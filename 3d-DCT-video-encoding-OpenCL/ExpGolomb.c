#include "ExpGolomb.h"
#include <stdlib.h>
#include <string.h>

int expGolomb_getBitsCount(int value) {
	int count = 0;
	while(value != 0) {
		value = value >> 1;
		count++;
	}
	return count;
}

int expGolomb_getMask(int bitsCount) {
	int mask = 0;
	while(bitsCount > 0) {
		mask = mask << 1;
		mask |= 1;
		bitsCount -= 1;
	}
	return mask;
}

struct ExpGolombStream * expGolomb_createStream(char * buffer) {
	struct ExpGolombStream * stream = (struct ExpGolombStream *) malloc(sizeof(struct ExpGolombStream));
	stream->buffer = buffer;
	stream->bitPosition = 8;
	stream->bufferPosition = 0;
	return stream;
}

void expGolomb_writeValue(struct ExpGolombStream * stream, int value) {
	if (value <= 0) {
		value = -2 * value;
	} else {
		value = 2 * value - 1;
	}
	value += 1;
	int bitsCount = expGolomb_getBitsCount(value);
	int mask = expGolomb_getMask(bitsCount);
	int zeroes = bitsCount - 1;
	stream->bitPosition -= zeroes;
	while (stream->bitPosition <= 0) {
		stream->bufferPosition += 1;
		stream->bitPosition = stream->bitPosition + 8;
		stream->buffer[stream->bufferPosition] = 0;
	}
	while(bitsCount > 0) {
		if (stream->bitPosition > bitsCount) {
			stream->buffer[stream->bufferPosition] = (char) (stream->buffer[stream->bufferPosition] | (value << (stream->bitPosition - bitsCount)));
			stream->bitPosition -= bitsCount;
			bitsCount = 0;
		} else {
			int reducedValue = value >> (bitsCount - stream->bitPosition);
			stream->buffer[stream->bufferPosition] = (char) (stream->buffer[stream->bufferPosition] | reducedValue);
			bitsCount -= stream->bitPosition;
			mask = mask >> stream->bitPosition;
			stream->bufferPosition += 1;
			stream->bitPosition = 8;
			stream->buffer[stream->bufferPosition] = 0;
			value = value & mask;
		}
	}
}

int expGolomb_readValue(struct ExpGolombStream * stream) {
	int zeroesCount = 0;
	char byteValue = stream->buffer[stream->bufferPosition];
	int bit = byteValue & (1 << (stream->bitPosition - 1));
	while(bit == 0) {
		zeroesCount++;
		stream->bitPosition--;
		if (stream->bitPosition <= 0) {
			stream->bufferPosition++;
			stream->bitPosition = 8;
			byteValue = stream->buffer[stream->bufferPosition];
		}
		bit = byteValue & (1 << (stream->bitPosition - 1));
	}
	int value = 0;
	int bitCount = zeroesCount + 1;
	while(bitCount > 0) {
		if (bitCount > stream->bitPosition) {
			int mask = expGolomb_getMask(stream->bitPosition);
			value = value | ((byteValue & mask) << (bitCount - stream->bitPosition));
			bitCount -= stream->bitPosition;
			stream->bitPosition = 8;
			stream->bufferPosition += 1;
			byteValue = stream->buffer[stream->bufferPosition];
		} else {
			int mask = expGolomb_getMask(bitCount);
			mask = mask << (stream->bitPosition - bitCount);
			value = value | ((byteValue & mask) >> (stream->bitPosition - bitCount));
			stream->bitPosition -= bitCount;
			bitCount = 0;
			if (stream->bitPosition <= 0) {
				stream->bitPosition = 8;
				stream->bufferPosition += 1;
				byteValue = stream->buffer[stream->bufferPosition];
			}
		}
	}
	value -= 1;
	if (value % 2 != 0) {
		value = (value + 1) / 2;
	} else {
		value = -value / 2;
	}
	return value;
}

void expGolomb_freeBuffer(struct ExpGolombStream * stream, int position, int writing) {

	if (writing) {
		int bytesToMoveCount = stream->bufferPosition - position + 1;
		if (position <= stream->bufferPosition) {
			memmove(stream->buffer, stream->buffer + position, bytesToMoveCount);
			stream->bufferPosition -= position;
		} else {
			stream->bufferPosition = 0;
			stream->buffer[stream->bufferPosition] = 0;
		}
	} else {
		if (position > stream->bufferPosition) {
			memmove(stream->buffer, stream->buffer + stream->bufferPosition, position - stream->bufferPosition);
		}
		stream->bufferPosition = 0;
	}

}
