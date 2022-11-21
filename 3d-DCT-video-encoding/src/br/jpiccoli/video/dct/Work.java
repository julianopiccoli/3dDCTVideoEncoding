package br.jpiccoli.video.dct;

import java.util.concurrent.CountDownLatch;

/**
 * Runnable containing part of the DCT/Inverse DCT transform work.
 * 
 * @author Juliano Piccoli
 */
class Work implements Runnable {
	
	private final Transform dct;
	private final int x;
	private final int y;
	private final int z;
	private final CountDownLatch countDownLatch;

	Work(final Transform dct, final int x, final int y, final int z, final CountDownLatch countDownLatch) {
		this.dct = dct;
		this.x = x;
		this.y = y;
		this.z = z;
		this.countDownLatch = countDownLatch;
	}

	public void run() {
		dct.apply(x, y, z);
		countDownLatch.countDown();
	}

}
