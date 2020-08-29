################################################################################
# Automatically-generated file. Do not edit!
################################################################################

-include ../makefile.init

# Commands and variables which depend on the operating system
RM := del
EXECUTABLENAME := codec.exe
OPENCL_LIBRARY := C:\Windows\System32\OpenCL.dll

# Path to the OpenCL headers
OPENCL_HEADERS_PATH = ..\..\..\OpenCL-Headers

LIBS := -lz

# All of the sources participating in the build are defined here
-include sources.mk
-include subdir.mk
-include objects.mk

ifneq ($(MAKECMDGOALS),clean)
ifneq ($(strip $(C_DEPS)),)
-include $(C_DEPS)
endif
endif

-include ../makefile.defs

# Add inputs and outputs from these tool invocations to the build variables 

# All Target
all: codec

# Tool invocations
codec: $(OBJS) $(OPENCL_LIBRARY)
	@echo 'Building target: $@'
	@echo 'Invoking: C Linker'
	gcc  -o "codec" $(OBJS) $(OPENCL_LIBRARY) $(LIBS)
	@echo 'Finished building target: $@'
	@echo ' '

# Other Targets
clean:
	-$(RM) $(EXECUTABLES)$(OBJS)$(C_DEPS) $(EXECUTABLENAME)
	-@echo ' '

.PHONY: all clean dependents

-include ../makefile.targets
