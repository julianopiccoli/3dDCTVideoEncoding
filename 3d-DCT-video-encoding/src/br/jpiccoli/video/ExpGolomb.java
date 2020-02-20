package br.jpiccoli.video;
class ExpGolomb {

	static int getBitsCount(int value) {
		int count = 0;
		while(value != 0) {
			value = value >> 1;
			count++;
		}
		return count;
	}
	
	static int getMask(int bitsCount) {
		int mask = 0;
		while(bitsCount > 0) {
			mask = mask << 1;
			mask |= 1;
			bitsCount -= 1;
		}
		return mask;
	}
	
}
