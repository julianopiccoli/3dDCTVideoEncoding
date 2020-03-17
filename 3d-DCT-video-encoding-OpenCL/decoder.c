#include "codec.h"
#include <stdlib.h>
#include <stdio.h>
#include <zlib.h>
#include <math.h>
#include "OpenCLUtils.h"
#include "CubeUtils.h"
#include "ExpGolomb.h"

void writeCubes(FILE * outputFile, float * data, int width, int height) {

	unsigned char * tempBuffer;
	size_t totalWritten;
	size_t writeResult;
	int frameSize = width * height;
	int size = width * height * DCT_BLOCK_SIZE;
	int y, x, i, j, k;
	int pixelPosition;
	int inputPixelPosition = 0;

	tempBuffer = (unsigned char *) malloc(size);

	for (y = 0; y < height; y += DCT_BLOCK_SIZE) {
		for (x = 0; x < width; x += DCT_BLOCK_SIZE) {
			for (k = 0; k < DCT_BLOCK_SIZE; k++) {
				for (i = 0; i < DCT_BLOCK_SIZE; i++) {
					for (j = 0; j < DCT_BLOCK_SIZE; j++) {
						pixelPosition = k * frameSize + (y + i) * width + x + j;
						tempBuffer[pixelPosition] = (unsigned char) data[inputPixelPosition];
						inputPixelPosition++;
					}
				}
			}
		}
	}

	writeResult = fwrite(tempBuffer, 1, size, outputFile);
	totalWritten = writeResult;
	while(totalWritten < size) {
		writeResult = fwrite(tempBuffer + totalWritten, 1, size - totalWritten, outputFile);
		totalWritten += writeResult;
	}

	free(tempBuffer);

}

void applyDequantization(float * dctCoeff, size_t bufferSize) {
	int frameSize = DCT_BLOCK_SIZE * DCT_BLOCK_SIZE;
	int cubeSize = DCT_BLOCK_SIZE * DCT_BLOCK_SIZE * DCT_BLOCK_SIZE;
	for (int offset = 0; offset < bufferSize; offset += cubeSize) {
		for (int z = 0; z < DCT_BLOCK_SIZE; z++) {
			for (int y = 0; y < DCT_BLOCK_SIZE; y++) {
				for (int x = 0; x < DCT_BLOCK_SIZE; x++) {
					int dctCoeffCubePosition = offset + (z * frameSize) + (y * DCT_BLOCK_SIZE) + x;
					dctCoeff[dctCoeffCubePosition] = round(dctCoeff[dctCoeffCubePosition] * fmax(1, 5 * (x + y + z)));
				}
			}
		}
	}
}

void reorderDctCoeffs(float * dctCoeff, size_t bufferSize, float * expGolombDecodedData, struct SlicesPositions * slicesPositions) {

	int frameSize = DCT_BLOCK_SIZE * DCT_BLOCK_SIZE;
	int cubeSize = DCT_BLOCK_SIZE * DCT_BLOCK_SIZE * DCT_BLOCK_SIZE;
	int expGolombCodedDataPosition = 0;
	for (int offset = 0; offset < bufferSize; offset += cubeSize) {
		for (int index = 0; index < slicesPositions->length; index++) {
			struct ThreeDimensionalCoordinates coordinates = slicesPositions->positions[index];
			dctCoeff[offset + coordinates.x + (coordinates.y * DCT_BLOCK_SIZE) + (coordinates.z * frameSize)] = expGolombDecodedData[expGolombCodedDataPosition];
			expGolombCodedDataPosition++;
		}
	}

}

int applyZlibDecompression(z_stream * zlibStream, char * inputBuffer, size_t inputBufferSize, char * outputBuffer, size_t outputBufferSize) {

	zlibStream->next_in = inputBuffer;
	zlibStream->avail_in = inputBufferSize;
	zlibStream->next_out = outputBuffer;
	zlibStream->avail_out = outputBufferSize;
	inflate(zlibStream, Z_NO_FLUSH);
	return outputBufferSize - zlibStream->avail_out;

}

int decode(char * inputFileName, char * outputFileName, int width, int height, int framesToDecode) {

	float * inputData;
	float * outputData;
	char * zlibCompressedData;
	char * expGolombCodedData;
	float * expGolombDecodedData;

	int framesRead;

	cl_device_id device;
	cl_context context;
	cl_program program;
	cl_kernel kernel;
	cl_command_queue queue;
	cl_int clResult;
	cl_mem kernelInputData;
	cl_mem kernelOutputData;

	struct SlicesPositions * slicesPositions;
	struct ExpGolombStream * expGolombStream;
	z_stream zlibStream;

	size_t bufferSize;
	size_t localSize;

	FILE * inputFile;
	FILE * outputFile;

	int expGolombDecodedDataSize;
	int expGolombCodedDataSize;
	int zlibCompressedDataSize;
	int consumedZlibDataSize;
	int readResult;
	int eof;

	bufferSize = width * height * DCT_BLOCK_SIZE;
	localSize = DCT_BLOCK_SIZE * DCT_BLOCK_SIZE * DCT_BLOCK_SIZE;
	inputFile = fopen(inputFileName, "rb");
	outputFile = fopen(outputFileName, "wb");
	inputData = (float *) malloc(bufferSize * sizeof(float));
	outputData = (float *) malloc(bufferSize * sizeof(float));
	zlibCompressedData = (char *) malloc(bufferSize);
	expGolombCodedData = (char *) malloc(bufferSize);
	expGolombDecodedData = (float *) malloc(bufferSize * sizeof(float));
	zlibCompressedDataSize = 0;
	expGolombCodedDataSize = 0;
	expGolombDecodedDataSize = 0;
	eof = 0;

	zlibStream.zalloc = Z_NULL;
	zlibStream.zfree = Z_NULL;
	zlibStream.opaque = Z_NULL;
	inflateInit(&zlibStream);

	// The DCT Coefficients inside the cubes are listed in diagonal slices to maximize the lengths
	// of the zeroes sequences. This increases the efficiency of the deflate compressor.
	slicesPositions = cubeUtils_diagonalSlices(DCT_BLOCK_SIZE, DCT_BLOCK_SIZE, DCT_BLOCK_SIZE);

	expGolombStream = expGolomb_createStream(expGolombCodedData);

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
		kernel = clCreateKernel(program, "idct", &clResult);
		if (clResult != CL_SUCCESS) {
			printf("Error creating OpenCL kernel: %d", clResult);
			return 1;
		}

		framesRead = 0;
		while(framesRead < framesToDecode) {
			while(expGolombDecodedDataSize < bufferSize) {
				if (!eof) {
					readResult = fread(zlibCompressedData + zlibCompressedDataSize, 1, bufferSize - zlibCompressedDataSize, inputFile);
					if (readResult == 0) {
						eof = 1;
					}
				}
				zlibCompressedDataSize += readResult;
				if (zlibCompressedDataSize > 0) {
					expGolombCodedDataSize += applyZlibDecompression(&zlibStream, zlibCompressedData, zlibCompressedDataSize, expGolombCodedData + expGolombCodedDataSize, bufferSize - expGolombCodedDataSize);
					if (zlibStream.avail_in > 0) {
						consumedZlibDataSize = zlibCompressedDataSize - zlibStream.avail_in;
						memmove(zlibCompressedData, zlibCompressedData + consumedZlibDataSize, zlibStream.avail_in);
					}
					zlibCompressedDataSize = zlibStream.avail_in;
				}
				while (expGolombStream->bufferPosition < expGolombCodedDataSize && expGolombDecodedDataSize < bufferSize) {
					expGolombDecodedData[expGolombDecodedDataSize] = expGolomb_readValue(expGolombStream);
					expGolombDecodedDataSize++;
				}
				int consumedBytes = expGolombStream->bufferPosition;
				expGolomb_freeBuffer(expGolombStream, expGolombCodedDataSize, 0);
				expGolombCodedDataSize = expGolombCodedDataSize - consumedBytes;
			}
			reorderDctCoeffs(inputData, bufferSize, expGolombDecodedData, slicesPositions);
			expGolombDecodedDataSize -= bufferSize;
			if (expGolombDecodedDataSize > 0) {
				memmove(expGolombDecodedData, expGolombDecodedData + bufferSize, expGolombDecodedDataSize);
			}
			applyDequantization(inputData, bufferSize);

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
			writeCubes(outputFile, outputData, width, height);
			framesRead += DCT_BLOCK_SIZE;
		}

		fflush(outputFile);

	} else {
		printf("Error creating OpenCL context: %d", clResult);
		return 1;
	}

	return 0;

}
