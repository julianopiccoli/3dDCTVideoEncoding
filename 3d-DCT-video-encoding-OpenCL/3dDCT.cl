__kernel void dct(__global float * input,
					__global float * output,
					__local float * localOutput) {

	float scale = 2.0f / sqrt(512.0f);
	float M_PI_8 = M_PI / 8.0f;
	uint globalAddr = get_global_id(0);
	uint localAddr = get_local_id(0);
	uint groupSize = get_local_size(0);
	
	uint n0 = localAddr / 64;
	uint n1 = (localAddr - (n0 * 64)) / 8;
	uint n2 = localAddr - (n0 * 64) - (n1 * 8);

	float value = input[globalAddr];
	
	for (uint k0 = 0; k0 < 8; k0++) {
		for (uint k1 = 0; k1 < 8; k1++) {
			for (uint k2 = 0; k2 < 8; k2++) {
	
				localOutput[localAddr] = value * cos(M_PI_8 * (n0 + 0.5f) * k0) * cos(M_PI_8 * (n1 + 0.5f) * k1) * cos(M_PI_8 * (n2 + 0.5f) * k2);
				for (uint stride = groupSize / 2; stride > 0; stride = stride / 2) {
				
					barrier(CLK_LOCAL_MEM_FENCE);
					if (localAddr < stride) {
						localOutput[localAddr] = localOutput[localAddr] + localOutput[localAddr + stride];
					}
				
				}
				
				if (localAddr == 0) {
					float c0 = 1;
					float c1 = 1;
					float c2 = 1;
					if (k0 == 0) {
						c0 = M_SQRT1_2;
					}
					if (k1 == 0) {
						c1 = M_SQRT1_2;
					}
					if (k2 == 0) {
						c2 = M_SQRT1_2;
					}
					uint outputAddr = globalAddr + (k0 * 64) + (k1 * 8) + k2;
					output[outputAddr] = localOutput[localAddr] * scale * c0 * c1 * c2;
				}

			}
		}
	}
	
}

__kernel void idct(__global float * input,
					__global float * output,
					__local float * localOutput) {

	float scale = 2.0f / sqrt(512.0f);
	float M_PI_8 = M_PI / 8.0f;
	uint globalAddr = get_global_id(0);
	uint localAddr = get_local_id(0);
	uint groupSize = get_local_size(0);
	
	uint k0 = localAddr / 64;
	uint k1 = (localAddr - (k0 * 64)) / 8;
	uint k2 = localAddr - (k0 * 64) - (k1 * 8);
	
	float value = input[globalAddr];
	float c0 = 1;
	float c1 = 1;
	float c2 = 1;
	if (k0 == 0) {
		c0 = M_SQRT1_2;
	}
	if (k1 == 0) {
		c1 = M_SQRT1_2;
	}
	if (k2 == 0) {
		c2 = M_SQRT1_2;
	}
	
	for (uint n0 = 0; n0 < 8; n0++) {
		for (uint n1 = 0; n1 < 8; n1++) {
			for (uint n2 = 0; n2 < 8; n2++) {
	
				localOutput[localAddr] = value * c0 * c1 * c2 * cos(M_PI_8 * (n0 + 0.5f) * k0) * cos(M_PI_8 * (n1 + 0.5f) * k1) * cos(M_PI_8 * (n2 + 0.5f) * k2);
				
				for (uint stride = groupSize / 2; stride > 0; stride = stride / 2) {
				
					barrier(CLK_LOCAL_MEM_FENCE);
					if (localAddr < stride) {
						localOutput[localAddr] = localOutput[localAddr] + localOutput[localAddr + stride];
					}
				
				}
				
				if (localAddr == 0) {
					uint outputAddr = globalAddr + (n0 * 64) + (n1 * 8) + n2;
					float outputValue = localOutput[localAddr] * scale;
					if (outputValue > 255) {
						outputValue = 255;
					} else if (outputValue < 0) {
						outputValue = 0;
					}
					output[outputAddr] = outputValue;
				}

			}
		}
	}
	
}