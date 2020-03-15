/*
 * codec.h
 *
 *  Created on: 15 de mar de 2020
 *      Author: Piccoli
 */

#ifndef CODEC_H_
#define CODEC_H_

#define DCT_BLOCK_SIZE 8

int encode(char * inputFileName, char * outputFileName, int width, int height, int framesToEncode);

int decode(char * inputFileName, char * outputFileName, int width, int height, int framesToDecode);

#endif /* CODEC_H_ */
