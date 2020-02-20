package br.jpiccoli.video;

public class ExpGolombReader {

	private byte[] input;
	private int bitPosition;
	private int bufferPosition;
	
	public void setInput(byte[] input) {
		this.input = input;
		bitPosition = 8;
		bufferPosition = 0;
	}
	
	public int getBufferPosition() {
		return bufferPosition;
	}
	
	public int readValue() {
		int zeroesCount = 0;
		byte byteValue = input[bufferPosition];
		int bit = byteValue & (1 << (bitPosition - 1));
		while(bit == 0) {
			zeroesCount++;
			bitPosition--;
			if (bitPosition <= 0) {
				bufferPosition++;
				bitPosition = 8;
				byteValue = input[bufferPosition];
			}
			bit = byteValue & (1 << (bitPosition - 1));
		}
		int value = 0;
		int bitCount = zeroesCount + 1;
		while(bitCount > 0) {
			if (bitCount > bitPosition) {
				int mask = ExpGolomb.getMask(bitPosition);
				value = value | ((byteValue & mask) << (bitCount - bitPosition));
				bitCount -= bitPosition;
				bitPosition = 8;
				bufferPosition += 1;
				byteValue = input[bufferPosition];
			} else {
				int mask = ExpGolomb.getMask(bitCount);
				mask = mask << (bitPosition - bitCount);
				value = value | ((byteValue & mask) >> (bitPosition - bitCount));
				bitPosition -= bitCount;
				bitCount = 0;
				if (bitPosition <= 0) {
					bitPosition = 8;
					bufferPosition += 1;
					byteValue = input[bufferPosition];
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
	
}
