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
	
	public static void main(String[] args) {
		
		ExpGolombWriter writer = new ExpGolombWriter();
		byte[] buffer = new byte[20];
		writer.setOutput(buffer);
		writer.writeValue(0);
		writer.writeValue(1);
		writer.writeValue(2);
		writer.writeValue(3);
		writer.writeValue(4);
		writer.writeValue(5);
		writer.writeValue(6);
		writer.writeValue(7);
		writer.writeValue(8);
		writer.writeValue(9);
		writer.writeValue(10);
		writer.writeValue(11);
		writer.writeValue(12);
		writer.writeValue(13);
		writer.writeValue(14);
		writer.writeValue(15);
		writer.writeValue(16);
		writer.writeValue(17);
		writer.writeValue(18);
		
		System.out.println("Buffer position: " + writer.getBufferPosition());
		
		ExpGolombReader reader = new ExpGolombReader();
		reader.setInput(buffer);
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		System.out.println(reader.readValue());
		
		System.out.println(reader.getBufferPosition());
		
	}
	
}
