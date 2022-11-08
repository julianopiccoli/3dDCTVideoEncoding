
/*
 * Computes the sum of the elements stored in the provided values vector.
 * The resulting sum will be stored in the first element of the input vector.
 * The contents of the input vector are NOT preserved.
 * 
 * @param values Input vector
 * @param valuesSize Quantity of elements on the input vector
 * @param localId Id of the work item in its local work group
 */
void parallelSumReduction(float * values, size_t valuesSize, const localId) {
	for (size_t stride = valuesSize / 2; stride > 0; stride = stride / 2, valuesSize = valuesSize / 2) {
		barrier(CLK_LOCAL_MEM_FENCE);
		if (localId < stride) {
			values[localId] = values[localId] + values[localId + stride];
		}
		// Dealing with odd quantities
		if (localId == 0 && valuesSize % 2 != 0) {
			values[localId] = values[localId] + values[valuesSize - 1];
		}
	}
}

/*
 * Calculates the partial sums for each resulting coefficient after applying
 * the 3D discrete cosine transform in the provided input vector.
 * The number of partial sum factors per DCT coefficient is given by the number of
 * work groups per cube ratio. For example, if the cube size is 1024 and the work
 * group size is 256, then this kernel will produce 4 partial sums for each resulting
 * DCT coefficient. This approach was chosen because there is no way for synchronizing
 * different threads of different work groups, so this kernel returns the partial
 * sums instead of accumulating them and returning the DCT coefficients directly.
 * 
 * @param cubeWidth Width of each cube contained in the input vector.
 * @param cubeHeight Height of each cube contained in the input vector.
 * @param cubeDepth Depth of each cube contained in the input vector.
 * @param input Vector containing multiple cubes data (an exact multiple is required).
 * @param partialSumsOutput Vector where the partial sums will be stored. Should
 * contain a number of elements equal to CUBE SIZE / WORK GROUP SIZE * input vector size.
 * @param localOutput Local memory for the work groups. Should contain a number of elements
 * equal to the work group size.
 */
__kernel void dct_calculate_partial_sums(int cubeWidth, int cubeHeight, int cubeDepth,
											__global float * input, __global float * partialSumsOutput,
											__local float * localOutput) {

	const int cubeFaceSize = cubeWidth * cubeHeight;
	const int cubeSize = cubeFaceSize * cubeDepth;
	const float piCoeffWidth = M_PI / ((float) cubeWidth);
	const float piCoeffHeight = M_PI / ((float) cubeHeight);
	const float piCoeffDepth = M_PI / ((float) cubeDepth);
	
	const size_t groupSize = get_local_size(0);
	const size_t globalAddr = get_global_id(0);
	const size_t localAddr = get_local_id(0);
	
	const uint cubeIndex = globalAddr / cubeSize;
	const uint itemIndexInCube = globalAddr % cubeSize;
	const uint cubeOffset = cubeIndex * cubeSize;
	const uint groupsPerCube = cubeSize / groupSize;
	const uint groupIndexInCube = itemIndexInCube / groupSize;
	
	const uint n0 = itemIndexInCube / cubeFaceSize;
	const uint n1 = (itemIndexInCube % cubeFaceSize) / cubeWidth;
	const uint n2 = itemIndexInCube % cubeWidth;

	const float value = input[globalAddr];
	
	for (uint k0 = 0; k0 < cubeDepth; k0++) {
		for (uint k1 = 0; k1 < cubeHeight; k1++) {
			for (uint k2 = 0; k2 < cubeWidth; k2++) {
				
				localOutput[localAddr] = value * native_cos(piCoeffDepth * (n0 + 0.5f) * k0) * native_cos(piCoeffHeight * (n1 + 0.5f) * k1) * native_cos(piCoeffWidth * (n2 + 0.5f) * k2);;
				
				parallelSumReduction(localOutput, groupSize, localAddr);
				
				if (localAddr == 0) {
					const uint coefficientIndex = cubeOffset + (k0 * cubeFaceSize) + (k1 * cubeWidth) + k2;
					const uint partialSumIndex = coefficientIndex * groupsPerCube + groupIndexInCube;
					partialSumsOutput[partialSumIndex] = localOutput[localAddr];
				}

			}
		}
	}
	
}

/*
 * Aggregates the partial sums into the final DCT coefficients.
 * The work group size should be equal to the number of partial sums
 * for each DCT coefficient.
 * 
 * @param cubeWidth Width of each cube contained in the output vector.
 * @param cubeHeight Height of each cube contained in the output vector.
 * @param cubeDepth Depth of each cube contained in the output vector.
 * @param partialSumsInput Vector containing the partial sums.
 * @param output Vector where the resulting DCT coefficients will be stored. Should
 * contain a number of elements equal to sizeof(partialSumsInput) / nr of partial sums per DCT coefficient.
 * @param localOutput Local memory for the work groups. Should contain a number of elements
 * equal to the work group size.
 */
__kernel void dct_aggregate_partial_sums(int cubeWidth, int cubeHeight, int cubeDepth,
											__global float * partialSumsInput, __global float * output,
											__local float * localOutput) {
	
	const int cubeFaceSize = cubeWidth * cubeHeight;
	const int cubeSize = cubeFaceSize * cubeDepth;
	const float scale = 2.0f / sqrt((float) cubeSize);
	
	const size_t groupSize = get_local_size(0);
	const size_t globalAddr = get_global_id(0);
	const size_t localAddr = get_local_id(0);
			
	localOutput[localAddr] = partialSumsInput[globalAddr];
	
	parallelSumReduction(localOutput, groupSize, localAddr);
	
	if (localAddr == 0) {
		const uint coefficientIndex = globalAddr / groupSize;
		const uint indexInCube = coefficientIndex % cubeSize;
		const uint k0 = indexInCube / cubeFaceSize;
		const uint k1 = (indexInCube % cubeFaceSize) / cubeWidth;
		const uint k2 = indexInCube % cubeWidth;
		
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
		
		const uint outputAddr = globalAddr / groupSize;
		output[outputAddr] = localOutput[localAddr] * scale * c0 * c1 * c2;
	}
	
}

/*
 * Calculates the partial sums for each resulting element after applying
 * the 3D inverse discrete cosine transform in the provided coefficients input vector.
 * The number of partial sum factors per element is given by the number of
 * work groups per cube ratio. For example, if the cube size is 1024 and the work
 * group size is 256, then this kernel will produce 4 partial sums for each resulting
 * element. This approach was chosen because there is no way for synchronizing
 * different threads of different work groups, so this kernel returns the partial
 * sums instead of accumulating them and returning the resulting elements directly.
 * 
 * @param cubeWidth Width of each cube contained in the input vector.
 * @param cubeHeight Height of each cube contained in the input vector.
 * @param cubeDepth Depth of each cube contained in the input vector.
 * @param input Vector containing multiple cubes data (an exact multiple is required).
 * @param partialSumsOutput Vector where the partial sums will be stored. Should
 * contain a number of elements equal to CUBE SIZE / WORK GROUP SIZE * input vector size.
 * @param localOutput Local memory for the work groups. Should contain a number of elements
 * equal to the work group size.
 */
__kernel void idct_calculate_partial_sums(int cubeWidth, int cubeHeight, int cubeDepth,
											__global float * input, __global float * partialSumsOutput,
											__local float * localOutput) {
	
	const int cubeFaceSize = cubeWidth * cubeHeight;
	const int cubeSize = cubeFaceSize * cubeDepth;
	const float piCoeffWidth = M_PI / ((float) cubeWidth);
	const float piCoeffHeight = M_PI / ((float) cubeHeight);
	const float piCoeffDepth = M_PI / ((float) cubeDepth);
	
	const size_t groupSize = get_local_size(0);
	const size_t globalAddr = get_global_id(0);
	const size_t localAddr = get_local_id(0);
	
	const uint cubeIndex = globalAddr / cubeSize;
	const uint itemIndexInCube = globalAddr % cubeSize;
	const uint cubeOffset = cubeIndex * cubeSize;
	const uint groupsPerCube = cubeSize / groupSize;
	const uint groupIndexInCube = itemIndexInCube / groupSize;
	
	const uint k0 = itemIndexInCube / cubeFaceSize;
	const uint k1 = (itemIndexInCube % cubeFaceSize) / cubeWidth;
	const uint k2 = itemIndexInCube % cubeWidth;

	const float value = input[globalAddr];
	
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
	
	for (uint n0 = 0; n0 < cubeDepth; n0++) {
		for (uint n1 = 0; n1 < cubeHeight; n1++) {
			for (uint n2 = 0; n2 < cubeWidth; n2++) {
					
				localOutput[localAddr] = value * c0 * c1 * c2 * native_cos(piCoeffDepth * (n0 + 0.5f) * k0) * native_cos(piCoeffHeight * (n1 + 0.5f) * k1) * native_cos(piCoeffWidth * (n2 + 0.5f) * k2);

				parallelSumReduction(localOutput, groupSize, localAddr);
				
				if (localAddr == 0) {
					const uint coefficientIndex = cubeOffset + (n0 * cubeFaceSize) + (n1 * cubeWidth) + n2;
					const uint partialSumIndex = coefficientIndex * groupsPerCube + groupIndexInCube;
					partialSumsOutput[partialSumIndex] = localOutput[localAddr];
				}

			}
		}
	}
	
}

/*
 * Aggregates the partial sums into the final elements values.
 * The work group size should be equal to the number of partial sums
 * for each element value.
 * 
 * @param cubeWidth Width of each cube contained in the output vector.
 * @param cubeHeight Height of each cube contained in the output vector.
 * @param cubeDepth Depth of each cube contained in the output vector.
 * @param partialSumsInput Vector containing the partial sums.
 * @param output Vector where the resulting element values will be stored. Should
 * contain a number of elements equal to sizeof(partialSumsInput) / nr of partial sums per element.
 * @param localOutput Local memory for the work groups. Should contain a number of elements
 * equal to the work group size.
 */
__kernel void idct_aggregate_partial_sums(int cubeWidth, int cubeHeight, int cubeDepth,
											__global float * partialSumsInput, __global float * output,
											__local float * localOutput) {

	const int cubeFaceSize = cubeWidth * cubeHeight;
	const int cubeSize = cubeFaceSize * cubeDepth;
	const float scale = 2.0f / sqrt((float) cubeSize);
	
	const size_t groupSize = get_local_size(0);
	const size_t globalAddr = get_global_id(0);
	const size_t localAddr = get_local_id(0);
	
	localOutput[localAddr] = partialSumsInput[globalAddr];
		
	parallelSumReduction(localOutput, groupSize, localAddr);
	
	if (localAddr == 0) {
		const uint outputAddr = globalAddr / groupSize;
		float outputValue = localOutput[localAddr] * scale;
		// Assuming that the max possible output value is 255 and the minimum is 0.
		if (outputValue > 255) {
			outputValue = 255;
		} else if (outputValue < 0) {
			outputValue = 0;
		}
		output[outputAddr] = outputValue;
	}
	
}