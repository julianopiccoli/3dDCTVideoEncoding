/*
 * CubeUtils.h
 *
 *  Created on: 4 de mar de 2020
 *      Author: Piccoli
 */

#ifndef CUBEUTILS_H_
#define CUBEUTILS_H_

struct ThreeDimensionalCoordinates {
	int x;
	int y;
	int z;
};

struct SlicesPositions {
	struct ThreeDimensionalCoordinates * positions;
	int length;
};

struct SlicesPositions * cubeUtils_diagonalSlices(int width, int height, int depth);

void cubeUtils_deallocatePositions(struct SlicesPositions * slicesPositions);

#endif /* CUBEUTILS_H_ */
