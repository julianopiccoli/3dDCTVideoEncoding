#define DCT_BLOCK_WIDTH 8
#define DCT_BLOCK_HEIGHT 8
#define DCT_BLOCK_DEPTH 8

#define FACE_SIZE DCT_BLOCK_WIDTH * DCT_BLOCK_HEIGHT
#define CUBE_SIZE FACE_SIZE * DCT_BLOCK_DEPTH

__kernel void dct(__global float * input,
					__global float * output,
					__local float * localOutput) {

	float scale = 2.0f / sqrt((float) CUBE_SIZE);
	
	float piCoeffWidth = M_PI / ((float) DCT_BLOCK_WIDTH);
	float piCoeffHeight = M_PI / ((float) DCT_BLOCK_HEIGHT);
	float piCoeffDepth = M_PI / ((float) DCT_BLOCK_DEPTH);
	
	uint globalAddr = get_global_id(0);
	uint localAddr = get_local_id(0);
	uint groupSize = get_local_size(0);
	
	uint n0 = localAddr / ((int) FACE_SIZE);
	uint n1 = (localAddr - (n0 * ((int) FACE_SIZE))) / ((int) DCT_BLOCK_WIDTH);
	uint n2 = localAddr - (n0 * FACE_SIZE) - (n1 * ((int) DCT_BLOCK_WIDTH));

	float value = input[globalAddr];
	
	for (uint k0 = 0; k0 < DCT_BLOCK_DEPTH; k0++) {
		for (uint k1 = 0; k1 < DCT_BLOCK_HEIGHT; k1++) {
			for (uint k2 = 0; k2 < DCT_BLOCK_WIDTH; k2++) {
	
				localOutput[localAddr] = value * cos(piCoeffDepth * (n0 + 0.5f) * k0) * cos(piCoeffHeight * (n1 + 0.5f) * k1) * cos(piCoeffWidth * (n2 + 0.5f) * k2);
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
					uint outputAddr = globalAddr + (k0 * FACE_SIZE) + (k1 * DCT_BLOCK_WIDTH) + k2;
					output[outputAddr] = localOutput[localAddr] * scale * c0 * c1 * c2;
				}

			}
		}
	}
	
}

__kernel void idct(__global float * input,
					__global float * output,
					__local float * localOutput) {

	float scale = 2.0f / sqrt((float) CUBE_SIZE);

	float piCoeffWidth = M_PI / ((float) DCT_BLOCK_WIDTH);
	float piCoeffHeight = M_PI / ((float) DCT_BLOCK_HEIGHT);
	float piCoeffDepth = M_PI / ((float) DCT_BLOCK_DEPTH);
	
	uint globalAddr = get_global_id(0);
	uint localAddr = get_local_id(0);
	uint groupSize = get_local_size(0);
	
	uint k0 = localAddr / ((int) FACE_SIZE);
	uint k1 = (localAddr - (k0 * ((int) FACE_SIZE))) / ((int) DCT_BLOCK_WIDTH);
	uint k2 = localAddr - (k0 * FACE_SIZE) - (k1 * ((int) DCT_BLOCK_WIDTH));
	
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
	
	for (uint n0 = 0; n0 < DCT_BLOCK_DEPTH; n0++) {
		for (uint n1 = 0; n1 < DCT_BLOCK_HEIGHT; n1++) {
			for (uint n2 = 0; n2 < DCT_BLOCK_WIDTH; n2++) {
	
				localOutput[localAddr] = value * c0 * c1 * c2 * cos(piCoeffDepth * (n0 + 0.5f) * k0) * cos(piCoeffHeight * (n1 + 0.5f) * k1) * cos(piCoeffWidth * (n2 + 0.5f) * k2);
				
				for (uint stride = groupSize / 2; stride > 0; stride = stride / 2) {
				
					barrier(CLK_LOCAL_MEM_FENCE);
					if (localAddr < stride) {
						localOutput[localAddr] = localOutput[localAddr] + localOutput[localAddr + stride];
					}
				
				}
				
				if (localAddr == 0) {
					uint outputAddr = globalAddr + (n0 * FACE_SIZE) + (n1 * DCT_BLOCK_WIDTH) + n2;
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