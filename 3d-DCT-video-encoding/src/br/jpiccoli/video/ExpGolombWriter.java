package br.jpiccoli.video;

public class ExpGolombWriter {

	private byte[] output;
	private int bitPosition;
	private int bufferPosition;
	
	public void setOutput(byte[] output) {
		this.output = output;
		bitPosition = 8;
		bufferPosition = 0;
	}
	
	public int getBufferPosition() {
		return bufferPosition;
	}
	
	public void writeValue(int value) {
		if (value <= 0) {
			value = -2 * value;
		} else {
			value = 2 * value - 1;
		}
		value += 1;
		int bitsCount = ExpGolomb.getBitsCount(value);
		int mask = ExpGolomb.getMask(bitsCount);
		int zeroes = bitsCount - 1;
		bitPosition -= zeroes;
		while (bitPosition <= 0) {
			bufferPosition += 1;
			bitPosition = bitPosition + 8;
		}
		while(bitsCount > 0) {
			if (bitPosition > bitsCount) {
				output[bufferPosition] = (byte) (output[bufferPosition] | (value << (bitPosition - bitsCount)));
				bitPosition -= bitsCount;
				bitsCount = 0;
			} else {
				int reducedValue = value >> (bitsCount - bitPosition);
				output[bufferPosition] = (byte) (output[bufferPosition] | reducedValue);
				bitsCount -= bitPosition;
				mask = mask >> bitPosition;
				bufferPosition += 1;
				bitPosition = 8;
				value = value & mask;
			}
		}
	}
	
}
