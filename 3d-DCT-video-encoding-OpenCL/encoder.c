#include "codec.h"
#include <stdlib.h>
#include <stdio.h>
#include <zlib.h>
#include <math.h>
#include "OpenCLUtils.h"
#include "CubeUtils.h"
#include "ExpGolomb.h"

void readCubes(FILE * inputFile, cl_float * data, int width, int height) {

	unsigned char * tempBuffer;
	size_t totalRead;
	size_t readResult;
	int frameSize = width * height;
	int size = width * height * DCT_BLOCK_DEPTH;
	int y, x, i, j, k;
	int pixelPosition;
	int outputPixelPosition = 0;

	tempBuffer = (unsigned char *) malloc(size);
	readResult = fread(tempBuffer, 1, size, inputFile);
	totalRead = readResult;
	while(readResult > 0 && totalRead < size) {
		readResult = fread(tempBuffer + totalRead, 1, size - totalRead, inputFile);
		totalRead += readResult;
	}

	for (y = 0; y < height; y += DCT_BLOCK_HEIGHT) {
		for (x = 0; x < width; x += DCT_BLOCK_WIDTH) {
			for (k = 0; k < DCT_BLOCK_DEPTH; k++) {
				for (i = 0; i < DCT_BLOCK_HEIGHT; i++) {
					for (j = 0; j < DCT_BLOCK_WIDTH; j++) {
						pixelPosition = k * frameSize + (y + i) * width + x + j;
						data[outputPixelPosition] = tempBuffer[pixelPosition];
						outputPixelPosition++;
					}
				}
			}
		}
	}

	free(tempBuffer);

}

void applyQuantization(cl_float * dctCoeff, size_t bufferSize) {
	for (int offset = 0; offset < bufferSize; offset += CUBE_SIZE) {
		for (int z = 0; z < DCT_BLOCK_DEPTH; z++) {
			for (int y = 0; y < DCT_BLOCK_HEIGHT; y++) {
				for (int x = 0; x < DCT_BLOCK_WIDTH; x++) {
					int dctCoeffCubePosition = offset + (z * FACE_SIZE) + (y * DCT_BLOCK_WIDTH) + x;
					dctCoeff[dctCoeffCubePosition] = round(dctCoeff[dctCoeffCubePosition] / fmax(1, 5 * (x + y + z)));
				}
			}
		}
	}
}

int applyExpGolombCoding(cl_float * dctCoeff, size_t bufferSize, struct SlicesPositions * slicesPositions, struct ExpGolombStream * expGolombStream) {

	for (int offset = 0; offset < bufferSize; offset += CUBE_SIZE) {
		for (int index = 0; index < slicesPositions->length; index++) {
			struct ThreeDimensionalCoordinates coordinates = slicesPositions->positions[index];
			int dctCoeffInt = (int) dctCoeff[offset + coordinates.x + (coordinates.y * DCT_BLOCK_WIDTH) + (coordinates.z * FACE_SIZE)];
			expGolomb_writeValue(expGolombStream, dctCoeffInt);
		}
	}
	return expGolombStream->bufferPosition;

}

int applyZlibCompression(z_stream * zlibStream, char * inputBuffer, size_t inputBufferSize, char * outputBuffer, size_t outputBufferSize, int flush) {

	zlibStream->next_in = inputBuffer;
	zlibStream->avail_in = inputBufferSize;
	zlibStream->next_out = outputBuffer;
	zlibStream->avail_out = outputBufferSize;
	if (flush) {
		deflate(zlibStream, Z_FINISH);
	} else {
		deflate(zlibStream, Z_NO_FLUSH);
	}
	return outputBufferSize - zlibStream->avail_out;

}

int encode(char * inputFileName, char * outputFileName, int width, int height, int framesToEncode, int platformIndex) {

	cl_float * inputData;
	cl_float * outputData;
	char * expGolombBuffer;
	char * zlibCompressedBuffer;

	int framesRead;

	cl_device_id device;
	cl_context context;
	cl_program program;
	cl_kernel partialSumsCalcKernel;
	cl_kernel partialSumsAggregationKernel;
	cl_command_queue queue;
	cl_int clResult;
	cl_mem kernelInputData;
	cl_mem kernelOutputData;
	cl_mem partialSums;
	size_t maxWorkGroupSize;

	size_t bufferSize;
	size_t localSize;
	size_t partialSumsSize;
	size_t partialSumsPerCubeSize;

	cl_event event;

	FILE * inputFile;
	FILE * outputFile;

	struct SlicesPositions * slicesPositions;
	struct ExpGolombStream * expGolombStream;
	z_stream zlibStream;

	int expGolombCodedDataSize;
	int zlibCompressedDataSize;

	bufferSize = width * height * DCT_BLOCK_DEPTH;
	localSize = CUBE_SIZE;
	inputFile = fopen(inputFileName, "rb");
	outputFile = fopen(outputFileName, "wb");
	inputData = (cl_float *) malloc(bufferSize * sizeof(cl_float));
	outputData = (cl_float *) malloc(bufferSize * sizeof(cl_float));

	expGolombBuffer = (char *) malloc(bufferSize * sizeof(char));
	zlibCompressedBuffer = (char *) malloc(bufferSize * sizeof(char));

	zlibStream.zalloc = Z_NULL;
	zlibStream.zfree = Z_NULL;
	zlibStream.opaque = Z_NULL;
	deflateInit(&zlibStream, Z_BEST_COMPRESSION);

	// The DCT Coefficients inside the cubes are listed in diagonal slices to maximize the lengths
	// of the zeroes sequences. This increases the efficiency of the deflate compressor.
	slicesPositions = cubeUtils_diagonalSlices(DCT_BLOCK_WIDTH, DCT_BLOCK_HEIGHT, DCT_BLOCK_DEPTH);

	expGolombStream = expGolomb_createStream(expGolombBuffer);

	printf("Getting device id\n");
	device = getDeviceId(platformIndex);
	printf("Checking CL_DEVICE_MAX_WORK_GROUP_SIZE\n");
	maxWorkGroupSize = getMaxWorkGroupSize(device);
	while(localSize > maxWorkGroupSize) {
		localSize = localSize / 2;
	}
	printf("Using group size with %d work items\n", localSize);
	partialSumsPerCubeSize = CUBE_SIZE / localSize;
	partialSumsSize = bufferSize * partialSumsPerCubeSize;
	printf("Creating OpenCL context\n");
	context = clCreateContext(NULL, 1, &device, NULL, NULL, &clResult);
	if (clResult == CL_SUCCESS) {

		printf("Building the kernel\n");
		// Loading the kernel and creating the needed structures.
		program = buildKernel(context, device, "3dDCT.cl");
		printf("Creating buffers\n");
		kernelInputData = clCreateBuffer(context, CL_MEM_READ_ONLY, bufferSize * sizeof(cl_float), NULL, &clResult);
		if (clResult != CL_SUCCESS) {
			printf("Error creating OpenCL input data buffer: %d", clResult);
			return 1;
		}
		kernelOutputData = clCreateBuffer(context, CL_MEM_WRITE_ONLY, bufferSize * sizeof(cl_float), NULL, &clResult);
		if (clResult != CL_SUCCESS) {
			printf("Error creating OpenCL output data buffer: %d", clResult);
			return 1;
		}
		partialSums = clCreateBuffer(context, CL_MEM_READ_WRITE, partialSumsSize * sizeof(cl_float), NULL, &clResult);
		if (clResult != CL_SUCCESS) {
			printf("Error creating OpenCL partial sums data buffer: %d", clResult);
			return 1;
		}
		printf("Creating command queue\n");
		queue = clCreateCommandQueue(context, device, NULL, &clResult);
		if (clResult != CL_SUCCESS) {
			printf("Error creating OpenCL command queue: %d", clResult);
			return 1;
		}
		printf("Creating kernels\n");
		partialSumsCalcKernel = clCreateKernel(program, "dct_calculate_partial_sums", &clResult);
		if (clResult != CL_SUCCESS) {
			printf("Error creating OpenCL kernel 1: %d", clResult);
			return 1;
		}

		partialSumsAggregationKernel = clCreateKernel(program, "dct_aggregate_partial_sums", &clResult);
		if (clResult != CL_SUCCESS) {
			printf("Error creating OpenCL kernel 2: %d", clResult);
			return 1;
		}

		// Entering the encoding loop: blocks with DCT_BLOCK_DEPTH frames are read from the input stream and encoded each
		// iteration.
		printf("Starting encoding process\n");
		framesRead = 0;
		while(framesRead < framesToEncode) {

			// Reading frames
			readCubes(inputFile, inputData, width, height);

			// Sending pixels to the device memory
			clEnqueueWriteBuffer(queue, kernelInputData, CL_TRUE, 0, bufferSize * sizeof(cl_float), inputData, 0, NULL, NULL);

			const cl_int cubeWidth = DCT_BLOCK_WIDTH;
			const cl_int cubeHeight = DCT_BLOCK_HEIGHT;
			const cl_int cubeDepth = DCT_BLOCK_DEPTH;
			clResult = clSetKernelArg(partialSumsCalcKernel, 0, sizeof(cl_int), &cubeWidth);
			clResult |= clSetKernelArg(partialSumsCalcKernel, 1, sizeof(cl_int), &cubeHeight);
			clResult |= clSetKernelArg(partialSumsCalcKernel, 2, sizeof(cl_int), &cubeDepth);
			clResult |= clSetKernelArg(partialSumsCalcKernel, 3, sizeof(cl_mem), &kernelInputData);
			clResult |= clSetKernelArg(partialSumsCalcKernel, 4, sizeof(cl_mem), &partialSums);
			clResult |= clSetKernelArg(partialSumsCalcKernel, 5, localSize * sizeof(cl_float), NULL);

			if (clResult != CL_SUCCESS) {
				printf("Error setting OpenCL kernel 1 args: %d", clResult);
				return 1;
			}

			// Async running the kernel
			clResult = clEnqueueNDRangeKernel(queue, partialSumsCalcKernel, 1, NULL, &bufferSize, &localSize, 0, NULL, &event);
			if (clResult != CL_SUCCESS) {
				printf("Error sending kernel 1 execution command to OpenCL queue: %d", clResult);
				return 1;
			}

			clResult = clSetKernelArg(partialSumsAggregationKernel, 0, sizeof(cl_int), &cubeWidth);
			clResult |= clSetKernelArg(partialSumsAggregationKernel, 1, sizeof(cl_int), &cubeHeight);
			clResult |= clSetKernelArg(partialSumsAggregationKernel, 2, sizeof(cl_int), &cubeDepth);
			clResult |= clSetKernelArg(partialSumsAggregationKernel, 3, sizeof(cl_mem), &partialSums);
			clResult |= clSetKernelArg(partialSumsAggregationKernel, 4, sizeof(cl_mem), &kernelOutputData);
			clResult |= clSetKernelArg(partialSumsAggregationKernel, 5, partialSumsPerCubeSize * sizeof(cl_float), NULL);

			if (clResult != CL_SUCCESS) {
				printf("Error setting OpenCL kernel 2 args: %d", clResult);
				return 1;
			}

			// Async running the kernel
			clResult = clEnqueueNDRangeKernel(queue, partialSumsAggregationKernel, 1, NULL, &partialSumsSize, &partialSumsPerCubeSize, 1, &event, NULL);
			if (clResult != CL_SUCCESS) {
				printf("Error sending kernel 2 execution command to OpenCL queue:", clResult);
				return 1;
			}

			// Reading the resulting DCT coefficients. This operation blocks until the kernel execution is
			// completed.
			clResult = clEnqueueReadBuffer(queue, kernelOutputData, CL_TRUE, 0, bufferSize * sizeof(cl_float), outputData, 0, NULL, NULL);
			framesRead += DCT_BLOCK_DEPTH;

			// Quantization of the DCT output. This process divides each sample
			// of the DCT cube by five times the sum of its coordinates in the cube.
			// This turns most of the higher frequencies to zero.
			applyQuantization(outputData, bufferSize);

			// Applying Exp-Golomb coding to the quantized data.
			expGolombCodedDataSize = applyExpGolombCoding(outputData, bufferSize, slicesPositions, expGolombStream);

			// Deflating the Exp-Golomb coded data.
			if (framesToEncode > framesRead) {
				zlibCompressedDataSize = applyZlibCompression(&zlibStream, expGolombBuffer, expGolombCodedDataSize, zlibCompressedBuffer, bufferSize, 0);
				expGolomb_freeBuffer(expGolombStream, expGolombCodedDataSize, 1);
			} else {
				zlibCompressedDataSize = applyZlibCompression(&zlibStream, expGolombBuffer, expGolombCodedDataSize + 1, zlibCompressedBuffer, bufferSize, 1);
			}

			// Writing the encoded data to the output file.
			fwrite(zlibCompressedBuffer, 1, zlibCompressedDataSize, outputFile);

			printf("Frames processed: %d\n", framesRead);

		}

		// Flushing and closing the output file.
		fflush(outputFile);
		fclose(outputFile);

		printf("Encoding process completed");

	} else {
		printf("Error creating OpenCL context: %d", clResult);
		return 1;
	}

	return 0;

}
