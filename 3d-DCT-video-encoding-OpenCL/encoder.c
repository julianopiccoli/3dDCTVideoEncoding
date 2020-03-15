#include "codec.h"
#include <stdlib.h>
#include <stdio.h>
#include <zlib.h>
#include <math.h>
#include "OpenCLUtils.h"
#include "CubeUtils.h"
#include "ExpGolomb.h"

void readCubes(FILE * inputFile, float * data, int width, int height) {

	unsigned char * tempBuffer;
	size_t totalRead;
	size_t readResult;
	int frameSize = width * height;
	int size = width * height * DCT_BLOCK_SIZE;
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

	for (y = 0; y < height; y += DCT_BLOCK_SIZE) {
		for (x = 0; x < width; x += DCT_BLOCK_SIZE) {
			for (k = 0; k < DCT_BLOCK_SIZE; k++) {
				for (i = 0; i < DCT_BLOCK_SIZE; i++) {
					for (j = 0; j < DCT_BLOCK_SIZE; j++) {
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

void applyQuantization(float * dctCoeff, size_t bufferSize) {
	int frameSize = DCT_BLOCK_SIZE * DCT_BLOCK_SIZE;
	int cubeSize = DCT_BLOCK_SIZE * DCT_BLOCK_SIZE * DCT_BLOCK_SIZE;
	for (int offset = 0; offset < bufferSize; offset += cubeSize) {
		for (int z = 0; z < DCT_BLOCK_SIZE; z++) {
			for (int y = 0; y < DCT_BLOCK_SIZE; y++) {
				for (int x = 0; x < DCT_BLOCK_SIZE; x++) {
					int dctCoeffCubePosition = offset + (z * frameSize) + (y * DCT_BLOCK_SIZE) + x;
					dctCoeff[dctCoeffCubePosition] = round(dctCoeff[dctCoeffCubePosition] / fmax(1, 5 * (x + y + z)));
				}
			}
		}
	}
}

int applyExpGolombCoding(float * dctCoeff, size_t bufferSize, struct SlicesPositions * slicesPositions, struct ExpGolombStream * expGolombStream) {

	int frameSize = DCT_BLOCK_SIZE * DCT_BLOCK_SIZE;
	int cubeSize = DCT_BLOCK_SIZE * DCT_BLOCK_SIZE * DCT_BLOCK_SIZE;
	for (int offset = 0; offset < bufferSize; offset += cubeSize) {
		for (int index = 0; index < slicesPositions->length; index++) {
			struct ThreeDimensionalCoordinates coordinates = slicesPositions->positions[index];
			int dctCoeffInt = (int) dctCoeff[offset + coordinates.x + (coordinates.y * DCT_BLOCK_SIZE) + (coordinates.z * frameSize)];
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

int encode(char * inputFileName, char * outputFileName, int width, int height, int framesToEncode) {

	float * inputData;
	float * outputData;
	char * expGolombBuffer;
	char * zlibCompressedBuffer;

	int framesRead;

	cl_device_id device;
	cl_context context;
	cl_program program;
	cl_kernel kernel;
	cl_command_queue queue;
	cl_int clResult;
	cl_mem kernelInputData;
	cl_mem kernelOutputData;

	size_t bufferSize;
	size_t localSize;

	FILE * inputFile;
	FILE * outputFile;

	struct SlicesPositions * slicesPositions;
	struct ExpGolombStream * expGolombStream;
	z_stream zlibStream;

	int expGolombCodedDataSize;
	int zlibCompressedDataSize;

	bufferSize = width * height * DCT_BLOCK_SIZE;
	localSize = DCT_BLOCK_SIZE * DCT_BLOCK_SIZE * DCT_BLOCK_SIZE;
	inputFile = fopen(inputFileName, "rb");
	outputFile = fopen(outputFileName, "wb");
	inputData = (float *) malloc(bufferSize * sizeof(float));
	outputData = (float *) malloc(bufferSize * sizeof(float));

	expGolombBuffer = (char *) malloc(bufferSize * sizeof(char));
	zlibCompressedBuffer = (char *) malloc(bufferSize * sizeof(char));

	zlibStream.zalloc = Z_NULL;
	zlibStream.zfree = Z_NULL;
	zlibStream.opaque = Z_NULL;
	deflateInit(&zlibStream, Z_BEST_COMPRESSION);

	// The DCT Coefficients inside the cubes are listed in diagonal slices to maximize the lengths
	// of the zeroes sequences. This increases the efficiency of the deflate compressor.
	slicesPositions = cubeUtils_diagonalSlices(DCT_BLOCK_SIZE, DCT_BLOCK_SIZE, DCT_BLOCK_SIZE);

	expGolombStream = expGolomb_createStream(expGolombBuffer);

	device = getDeviceId();
	context = clCreateContext(NULL, 1, &device, NULL, NULL, &clResult);
	if (clResult == CL_SUCCESS) {
		program = buildKernel(context, device, "3dDCT.cl");
		kernelInputData = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, bufferSize * sizeof(float), inputData, &clResult);
		if (clResult != CL_SUCCESS) {
			printf("Error creating OpenCL input data buffer: %d", clResult);
			return 1;
		}
		kernelOutputData = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, bufferSize * sizeof(float), outputData, &clResult);
		if (clResult != CL_SUCCESS) {
			printf("Error creating OpenCL output data buffer: %d", clResult);
			return 1;
		}
		queue = clCreateCommandQueue(context, device, NULL, &clResult);
		if (clResult != CL_SUCCESS) {
			printf("Error creating OpenCL command queue: %d", clResult);
			return 1;
		}
		kernel = clCreateKernel(program, "dct", &clResult);
		if (clResult != CL_SUCCESS) {
			printf("Error creating OpenCL kernel: %d", clResult);
			return 1;
		}

		framesRead = 0;
		while(framesRead < framesToEncode) {
			readCubes(inputFile, inputData, width, height);

			clEnqueueWriteBuffer(queue, kernelInputData, CL_TRUE, 0, bufferSize * sizeof(float), inputData, 0, NULL, NULL);

			clResult = clSetKernelArg(kernel, 0, sizeof(cl_mem), &kernelInputData);
			clResult |= clSetKernelArg(kernel, 1, sizeof(cl_mem), &kernelOutputData);
			clResult |= clSetKernelArg(kernel, 2, localSize * sizeof(float), NULL);

			if (clResult != CL_SUCCESS) {
				printf("Error setting OpenCL kernel args: %d", clResult);
				return 1;
			}

			clResult = clEnqueueNDRangeKernel(queue, kernel, 1, NULL, &bufferSize, &localSize, 0, NULL, NULL);
			if (clResult != CL_SUCCESS) {
				printf("Error sending command to OpenCL queue: %d", clResult);
				return 1;
			}

			clResult = clEnqueueReadBuffer(queue, kernelOutputData, CL_TRUE, 0, bufferSize * sizeof(float), outputData, 0, NULL, NULL);
			framesRead += DCT_BLOCK_SIZE;

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

			fwrite(zlibCompressedBuffer, 1, zlibCompressedDataSize, outputFile);

		}

		fflush(outputFile);
		fclose(outputFile);

	} else {
		printf("Error creating OpenCL context: %d", clResult);
		return 1;
	}

	return 0;

}
