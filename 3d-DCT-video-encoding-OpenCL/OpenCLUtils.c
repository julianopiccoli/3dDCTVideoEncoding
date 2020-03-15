#include "OpenCLUtils.h"
#include <stdio.h>

#define MAX_KERNEL_SOURCE_SIZE 1000000

cl_device_id getDeviceId() {

	cl_platform_id platformId;
	cl_device_id deviceId;

	cl_uint platformsCount;
	cl_uint devicesCount;

	cl_int result = clGetPlatformIDs(1, &platformId, &platformsCount);
	if (result == CL_SUCCESS) {
		if (platformsCount > 0) {
			result = clGetDeviceIDs(platformId, CL_DEVICE_TYPE_GPU, 1, &deviceId, &devicesCount);
			if (result == CL_SUCCESS) {
				if (devicesCount > 0) {
					size_t size = 0;
					clGetDeviceInfo(deviceId, CL_DEVICE_MAX_WORK_GROUP_SIZE, sizeof(size_t), &size, NULL);
					return deviceId;
				} else {
					printf("clGetDeviceIDs returned 0 device");
					exit(1);
				}
			} else {
				printf("Error getting devices ids: %d", result);
				exit(1);
			}
		} else {
			printf("clGetPlatformIDs returned 0 platform");
			exit(1);
		}
	} else {
		printf("Error getting platforms ids: %d", result);
		exit(1);
	}

}

cl_program buildKernel(cl_context context, cl_device_id deviceId, const char * fileName) {

	FILE * inputFile;
	char * buffer;
	size_t dataLength = 0;
	size_t readResult = 0;
	cl_program program;
	cl_int clResult;

	buffer = (char *) malloc(MAX_KERNEL_SOURCE_SIZE);
	inputFile = fopen(fileName, "rb");
	if (inputFile != NULL) {

		readResult = fread(buffer, 1, MAX_KERNEL_SOURCE_SIZE - 1, inputFile);
		dataLength += readResult;
		while(readResult > 0) {
			readResult = fread(buffer + dataLength, 1, MAX_KERNEL_SOURCE_SIZE - 1 - dataLength, inputFile);
			dataLength += readResult;
		}
		fclose(inputFile);
		buffer[dataLength] = '\0';
		dataLength += 1;

		program = clCreateProgramWithSource(context, 1, (const char **) &buffer, &dataLength, &clResult);
		if (clResult == CL_SUCCESS) {
			clResult = clBuildProgram(program, 0, NULL, NULL, NULL, NULL);
			if (clResult == CL_SUCCESS) {
				free(buffer);
				return program;
			} else {
				clResult = clGetProgramBuildInfo(program, deviceId, CL_PROGRAM_BUILD_LOG, 0, NULL, &dataLength);
				if (clResult == CL_SUCCESS) {
					if (dataLength >= MAX_KERNEL_SOURCE_SIZE) {
						dataLength = MAX_KERNEL_SOURCE_SIZE - 1;
					}
					buffer[dataLength + 1] = '\0';
					clResult = clGetProgramBuildInfo(program, deviceId, CL_PROGRAM_BUILD_LOG, dataLength + 1, buffer, NULL);
					if (clResult == CL_SUCCESS) {
						printf("Error building program. Logs: %s", buffer);
						exit(1);
					} else {
						printf("Error building program. The build logs could not be retrieved. Error code: %d", clResult);
						exit(1);
					}
				} else {
					printf("Error building program. The build logs could not be retrieved. Error code: %d", clResult);
					exit(1);
				}
			}
		} else {
			printf("Error creating program from source: %d", clResult);
			exit(1);
		}

	} else {
		printf("Error opening source file");
		exit(1);
	}

}
