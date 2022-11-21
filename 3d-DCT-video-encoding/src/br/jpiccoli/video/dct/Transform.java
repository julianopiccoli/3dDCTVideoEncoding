package br.jpiccoli.video.dct;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base class for both the DCT and the inverse DCT transforms.
 * 
 * @author Juliano Piccoli
 */
public abstract class Transform {

	/*
	 * The number 3.0 in the expression below represents the number of dimensions of the
	 * cubes processed by the DCT and inverse DCT transforms. This expression is used in the
	 * scale factor that is applied when calculating each frequency/pixel.
	 */
	protected static final double DIMENSIONAL_FACTOR = Math.sqrt(Math.pow(2.0f, 3.0f));
	protected static final double INVERSE_SQRT_2 = 1.0f / Math.sqrt(2.0f);
	
	protected final double[] input;
	protected final double[] output;
	protected final int frameWidth;
	protected final int frameHeight;
	protected final int cubeWidth;
	protected final int cubeHeight;
	protected final int cubeDepth;
	protected final int cubeFaceSize;
	protected final int cubeSize;
	protected final int frameSize;

	/**
	 * Constructor.
	 * @param input Input data vector.
	 * @param output Output data vector.
	 * @param frameWidth Width of each video frame.
	 * @param frameHeight Height of each video frame.
	 * @param cubeWidth Width of each transform block.
	 * @param cubeHeight Height of each transform block.
	 * @param cubeDepth Depth of each transform block.
	 */
	protected Transform(double[] input, double[] output, int frameWidth, int frameHeight, int cubeWidth, int cubeHeight,
			int cubeDepth) {
		this.input = input;
		this.output = output;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
		this.cubeWidth = cubeWidth;
		this.cubeHeight = cubeHeight;
		this.cubeDepth = cubeDepth;
		this.cubeFaceSize = cubeWidth * cubeHeight;
		this.cubeSize = cubeFaceSize * cubeDepth;
		this.frameSize = frameWidth * frameHeight;
	}

	/**
	 * Executes the transform using multiple threads.
	 * 
	 * @throws InterruptedException
	 */
	public void run() throws InterruptedException {
		run(Runtime.getRuntime().availableProcessors());
	}
	
	/**
	 * Executes the transform using the provided number of threads.
	 * 
	 * @param threads Number of threads that will execute the transform.
	 * 
	 * @throws InterruptedException
	 */
	public void run(int threads) throws InterruptedException {
		final ExecutorService executor = Executors.newFixedThreadPool(threads);
		run(executor);
		executor.shutdown();
	}
	
	/**
	 * Executes the transform using the given executor.
	 * This method blocks the calling thread until the computation is completed.
	 * 
	 * @param executor Executor that will be used to dispatch the transform tasks.
	 * 
	 * @throws InterruptedException 
	 */
	public void run(final Executor executor) throws InterruptedException {
		
		final int framesCount = input.length / frameSize;
		final int cubesCount = (frameSize * framesCount) / cubeSize;
		final CountDownLatch countDownLatch = new CountDownLatch(cubesCount);
		
		for (int z = 0; z < framesCount; z += cubeDepth) {
			for (int y = 0; y < frameHeight; y += cubeHeight) {
				for (int x = 0; x < frameWidth; x += cubeWidth) {
					executor.execute(new Work(this, x, y, z, countDownLatch));
				}
			}
		}
		
		countDownLatch.await();
		
	}
	
	/**
	 * Apply the transform to a single block.
	 * 
	 * @param x Horizontal coordinate of the block's first element.
	 * @param y Vertical coordinate of the block's first element.
	 * @param z Depth coordinate of the block's first element.
	 */
	protected abstract void apply(final int x, final int y, final int z);

}