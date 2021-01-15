/*
 * codec.h
 *
 *  Created on: 15 de mar de 2020
 *      Author: Piccoli
 */

#ifndef CODEC_H_
#define CODEC_H_

#define DCT_BLOCK_WIDTH 8
#define DCT_BLOCK_HEIGHT 8
#define DCT_BLOCK_DEPTH 8

#define FACE_SIZE DCT_BLOCK_WIDTH * DCT_BLOCK_HEIGHT
#define CUBE_SIZE FACE_SIZE * DCT_BLOCK_DEPTH

int encode(char * inputFileName, char * outputFileName, int width, int height, int framesToEncode, int platformIndex);

int decode(char * inputFileName, char * outputFileName, int width, int height, int framesToDecode, int platformIndex);

#endif /* CODEC_H_ */
