#include <stdlib.h>
#include <stdio.h>
#include "codec.h"

int main(int argc, char * argv[]) {

	char * inputFileName;
	char * outputFileName;
	int width;
	int height;
	int framesToProcess;

	if (argc < 7) {
		printf("Usage: codec encode|decode <input file> <output file> <width> <height> <nr of frames to encode/decode>");
		exit(0);
	}

	inputFileName = argv[2];
	outputFileName = argv[3];
	width = atoi(argv[4]);
	height = atoi(argv[5]);
	framesToProcess = atoi(argv[6]);

	if (!strcmp(argv[1], "encode")) {
		encode(inputFileName, outputFileName, width, height, framesToProcess);
	} else if (!strcmp(argv[1], "decode")) {
		decode(inputFileName, outputFileName, width, height, framesToProcess);
	} else {
		printf("Usage: codec encode|decode <input file> <output file> <width> <height> <nr of frames to encode/decode>");
		exit(0);
	}

}
