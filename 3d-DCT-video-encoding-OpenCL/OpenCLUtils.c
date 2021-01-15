#include "OpenCLUtils.h"
#include <stdio.h>
#include <stdlib.h>

#define MAX_KERNEL_SOURCE_SIZE 1000000

void printAvailablePlatforms() {

	cl_platform_id platformsIds[10];

	cl_uint platformsCount;
	cl_uint platformIndex;

	char platformName[100];
	char platformProfile[100];
	char platformVersion[100];
	char platformVendor[100];

	size_t platformNameSize;
	size_t platformProfileSize;
	size_t platformVersionSize;
	size_t platformVendorSize;

	cl_int result = clGetPlatformIDs(10, &platformsIds, &platformsCount);
	if (result == CL_SUCCESS) {
		if (platformsCount > 0) {
			for (platformIndex = 0; platformIndex < platformsCount; platformIndex++) {
				platformNameSize = 0;
				platformProfileSize = 0;
				platformVersionSize = 0;
				platformVendorSize = 0;
				clGetPlatformInfo(platformsIds[platformIndex], CL_PLATFORM_NAME, 100, platformName, &platformNameSize);
				clGetPlatformInfo(platformsIds[platformIndex], CL_PLATFORM_PROFILE, 100, platformProfile, &platformProfileSize);
				clGetPlatformInfo(platformsIds[platformIndex], CL_PLATFORM_VERSION, 100, platformVersion, &platformVersionSize);
				clGetPlatformInfo(platformsIds[platformIndex], CL_PLATFORM_VENDOR, 100, platformVendor, &platformVendorSize);
				printf("%d. Name: %s - Vendor: %s, Profile: %s, Version: %s\n", (platformIndex + 1), platformName, platformVendor, platformProfile, platformVersion);
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

cl_platform_id getPlatformIdForIndex(int platformIndex) {

	cl_platform_id platformsIds[platformIndex];
	cl_uint platformsCount;

	cl_int result = clGetPlatformIDs(platformIndex, platformsIds, &platformsCount);
	if (result == CL_SUCCESS) {
		if (platformsCount >= platformIndex) {
			return platformsIds[platformIndex - 1];
		} else {
			printf("clGetPlatformIDs returned 0 platform");
			exit(1);
		}
	} else {
		printf("Error getting platforms ids: %d", result);
		exit(1);
	}

}

cl_device_id getDeviceId(int platformIndex) {

	cl_platform_id platformId;
	cl_device_id deviceId;

	cl_uint platformsCount;
	cl_uint devicesCount;

	cl_int result;

	platformId = getPlatformIdForIndex(platformIndex);
	result = clGetDeviceIDs(platformId, CL_DEVICE_TYPE_GPU, 1, &deviceId, &devicesCount);
	if (result == CL_SUCCESS) {
		if (devicesCount > 0) {
			return deviceId;
		} else {
			printf("clGetDeviceIDs returned 0 devices for the selected platform. Try using another platform (available platforms can be obtained by issuing command 'codec list_platforms').");
			exit(1);
		}
	} else {
		printf("Error getting GPU devices ids for the selected platform. Try using another platform (available platforms can be obtained by issuing command 'codec list_platforms'). Error code: %d", result);
		exit(1);
	}

}

size_t getMaxWorkGroupSize(cl_device_id deviceId) {
	size_t size = 0;
	cl_int result = clGetDeviceInfo(deviceId, CL_DEVICE_MAX_WORK_GROUP_SIZE, sizeof(size_t), &size, NULL);
	if (result == CL_SUCCESS) {
		return size;
	} else {
		printf("Unable to retrieve CL_DEVICE_MAX_WORK_GROUP_SIZE from the specified device. Please try using another platform (you can list available platforms by issuing command 'codec list_platforms').");
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
