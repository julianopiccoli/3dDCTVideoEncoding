#include <stdlib.h>
#include <stdio.h>
#include "codec.h"

void printUsage() {
	printf("Usage\n\n");
	printf("codec list_platforms -> List available OpenCL platforms\n");
	printf("codec encode|decode <input file> <output file> <width> <height> <nr of frames to encode/decode> <platform_index (optional)> -> Encode/Decode given file");
}

int main(int argc, char * argv[]) {

	char * inputFileName;
	char * outputFileName;
	int width;
	int height;
	int framesToProcess;
	int platformIndex;

	if (argc < 2) {
		printUsage();
		exit(0);
	}

	if (!strcmp(argv[1], "list_platforms")) {
		printAvailablePlatforms();
	} else if (argc >= 7) {
		inputFileName = argv[2];
		outputFileName = argv[3];
		width = atoi(argv[4]);
		height = atoi(argv[5]);
		framesToProcess = atoi(argv[6]);
		if (argc > 7) {
			platformIndex = atoi(argv[7]);
		} else {
			platformIndex = 1;
		}
		if (!strcmp(argv[1], "encode")) {
			encode(inputFileName, outputFileName, width, height, framesToProcess, platformIndex);
		} else if (!strcmp(argv[1], "decode")) {
			decode(inputFileName, outputFileName, width, height, framesToProcess, platformIndex);
		} else {
			printUsage();
		}
	} else {
		printUsage();
	}

}
