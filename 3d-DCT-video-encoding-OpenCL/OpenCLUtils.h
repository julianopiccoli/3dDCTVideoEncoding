/*
 * OpenCLUtils.h
 *
 *  Created on: 15 de mar de 2020
 *      Author: Piccoli
 */

#ifndef OPENCLUTILS_H_
#define OPENCLUTILS_H_

#include <CL/cl.h>

void printAvailablePlatforms();

cl_platform_id getPlatformIdForIndex(int platformIndex);

cl_device_id getDeviceId(int platformIndex);

size_t getMaxWorkGroupSize(cl_device_id deviceId);

cl_program buildKernel(cl_context context, cl_device_id deviceId, const char * fileName);

#endif /* OPENCLUTILS_H_ */
