package br.jpiccoli.video.dct;

/**
 * Runnable containing part of the DCT/Inverse DCT transform work.
 * 
 * @author Juliano Piccoli
 */
class Work implements Runnable {
	
	private final Transform dct;
	private final int initialZ;
	private final int finalZ;

	Work(final Transform dct, final int initialZ, final int finalZ) {
		this.dct = dct;
		this.initialZ = initialZ;
		this.finalZ = finalZ;
	}

	public void run() {
		for (int z = initialZ; z < finalZ; z += dct.cubeDepth) {
			for (int y = 0; y < dct.frameHeight; y += dct.cubeHeight) {
				for (int x = 0; x < dct.frameWidth; x += dct.cubeWidth) {
					dct.apply(x, y, z);
				}
			}
		}
	}

}
