################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
..\CubeUtils.c \
..\ExpGolomb.c \
..\OpenCLUtils.c \
..\decoder.c \
..\encoder.c \
..\main.c 

OBJS += \
.\CubeUtils.o \
.\ExpGolomb.o \
.\OpenCLUtils.o \
.\decoder.o \
.\encoder.o \
.\main.o 

C_DEPS += \
.\CubeUtils.d \
.\ExpGolomb.d \
.\OpenCLUtils.d \
.\decoder.d \
.\encoder.d \
.\main.d 


# Each subdirectory must supply rules for building sources it contributes
%.o: ../%.c
	@echo 'Building file: $<'
	@echo 'Invoking: C Compiler'
	gcc -I$(OPENCL_HEADERS_PATH) -O0 -g -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


