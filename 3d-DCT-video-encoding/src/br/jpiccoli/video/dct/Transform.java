package br.jpiccoli.video.dct;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base class for both the DCT and the inverse DCT transforms.
 * 
 * @author Juliano Piccoli
 */
public abstract class Transform {

	protected final double[] input;
	protected final double[] output;
	protected final int frameWidth;
	protected final int frameHeight;
	protected final int cubeWidth;
	protected final int cubeHeight;
	protected final int cubeDepth;
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
		this.frameSize = frameWidth * frameHeight;
	}

	/**
	 * Executes the transform using multiple threads.
	 * This method blocks the calling thread until the computation is completed.
	 */
	public void run() {
		
		final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		final int framesCount = input.length / frameSize;
	
		for (int frame = 0; frame < framesCount; frame += cubeDepth) {
			executor.execute(new Work(this, frame, frame + cubeDepth));
		}
		
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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