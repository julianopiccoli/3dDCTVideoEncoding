#ifndef EXPGOLOMB_H_
#define EXPGOLOMB_H_

struct ExpGolombStream {
	char * buffer;
	int bitPosition;
	int bufferPosition;
};

struct ExpGolombStream * expGolomb_createStream(char * buffer);

void expGolomb_writeValue(struct ExpGolombStream * stream, int value);

int expGolomb_readValue(struct ExpGolombStream * stream);

void expGolomb_freeBuffer(struct ExpGolombStream * stream, int position, int writing);

#endif /* EXPGOLOMB_H_ */
