#include "CubeUtils.h"
#include <math.h>
#include <stdlib.h>

struct SlicesPositions * cubeUtils_diagonalSlices(int width, int height, int depth) {

	struct SlicesPositions * slicesPositions = (struct SlicesPositions *) malloc(sizeof(struct SlicesPositions));
	slicesPositions->length = width * depth * height;

	slicesPositions->positions = (struct ThreeDimensionalCoordinates *) malloc(slicesPositions->length * sizeof(struct ThreeDimensionalCoordinates));
	int index = 0;

	int maxSum = (width - 1) + (height - 1) + (depth - 1);

	int x, y, z;

	for (int targetSum = 0; targetSum <= maxSum; targetSum++) {

		int maxWidth = (int) fmin(width - 1, targetSum);
		int maxHeight = (int) fmin(height - 1, targetSum);
		int maxDepth = (int) fmin(depth - 1, targetSum);
		int minWidth = (int) fmax(0, targetSum - (maxHeight + maxDepth));
		int minHeight = (int) fmax(0, targetSum - (maxWidth + maxDepth));
		int minDepth = (int) fmax(0, targetSum - (maxHeight + maxWidth));

		int sum = 0;

		for (y = minHeight; y <= maxHeight; y++) {
			for (z = minDepth; z <= maxDepth; z++) {
				for (x = minWidth; x <= maxWidth; x++) {
					sum = x + y + z;
					if (sum == targetSum) {
						slicesPositions->positions[index].x = x;
						slicesPositions->positions[index].y = y;
						slicesPositions->positions[index].z = z;
						index++;
					}
				}
			}
		}

	}

	return slicesPositions;

}

void cubeUtils_deallocatePositions(struct SlicesPositions * slicesPositions) {

	free(slicesPositions->positions);
	free(slicesPositions);

}
