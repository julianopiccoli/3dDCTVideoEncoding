package br.jpiccoli.video;
import java.util.ArrayList;
import java.util.List;

public class DCT {

	private static void dct2d(final double[] input, final int offset, final int stride, final double[] output, final int width, final int height) {
		
		double scale = (double) (2.0f / Math.sqrt(width * height));
		
		for (int k1 = 0; k1 < height; k1++) {
			
			for (int k2 = 0; k2 < width; k2++) {

				int outputPosition = offset + k1 * stride + k2;
				
				for (int n1 = 0; n1 < height; n1++) {
					
					for (int n2 = 0; n2 < width; n2++) {
						
						int inputPosition = offset + n1 * stride + n2;
						double inputValue = input[inputPosition];
						output[outputPosition] += inputValue * Math.cos((Math.PI / (float) height) * (n1 + 0.5f) * k1) * Math.cos((Math.PI / (float) width) * (n2 + 0.5f) * k2);
						
					}
					
				}
				
				double c1 = 1;
				double c2 = 1;
				if (k1 == 0) {
					c1 = 1.0f / Math.sqrt(2.0);
				}
				if (k2 == 0) {
					c2 = 1.0f / Math.sqrt(2.0);
				}
				output[outputPosition] = scale * c1 * c2 * output[outputPosition];
				
			}
			
		}
		
	}
	
	private static void idct2d(final double[] input, final double[] output, final int offset, final int stride, final int width, final int height) {
		
		double scale = (double) (2.0f / Math.sqrt(width * height));
		
		for (int n1 = 0; n1 < height; n1++) {
			
			for (int n2 = 0; n2 < width; n2++) {
				
				int outputPosition = offset + n1 * stride + n2;
				
				for (int k1 = 0; k1 < height; k1++) {
					
					for (int k2 = 0; k2 < width; k2++) {
						
						double c1 = 1;
						double c2 = 1;
						if (k1 == 0) {
							c1 = 1.0f / Math.sqrt(2.0);
						}
						if (k2 == 0) {
							c2 = 1.0f / Math.sqrt(2.0);
						}
						int inputPosition = offset + k1 * stride + k2;
						double inputValue = input[inputPosition];
						output[outputPosition] += c1 * c2 * inputValue * Math.cos((Math.PI / (float) height) * (n1 + 0.5f) * k1) * Math.cos((Math.PI / (float) width) * (n2 + 0.5f) * k2);
						
					}
					
				}

				output[outputPosition] = scale * output[outputPosition];
				if (output[outputPosition] > 255) output[outputPosition] = 255.0f;
				if (output[outputPosition] < 0) output[outputPosition] = 0;
				
			}
			
		}
		
	}
	
	private static void dct3d(final double[] input, final double[] output, final int offset, final int stride, final int frameSize, final int width, final int height, final int depth) {
		
		double scale = (double) (2.0f / Math.sqrt(width * height * depth));
		
		for (int k0 = 0; k0 < depth; k0++) {
		
			for (int k1 = 0; k1 < height; k1++) {
				
				for (int k2 = 0; k2 < width; k2++) {
	
					int outputPosition = offset + k0 * frameSize + k1 * stride + k2;
					
					for (int n0 = 0; n0 < depth; n0++) {
					
						for (int n1 = 0; n1 < height; n1++) {
							
							for (int n2 = 0; n2 < width; n2++) {
								
								int inputPosition = offset + n0 * frameSize + n1 * stride + n2;
								double inputValue = input[inputPosition];
								output[outputPosition] += inputValue * Math.cos((Math.PI / (float) depth) * (n0 + 0.5f) * k0) * Math.cos((Math.PI / (float) height) * (n1 + 0.5f) * k1) * Math.cos((Math.PI / (float) width) * (n2 + 0.5f) * k2);
								
							}
							
						}
						
					}
					
					double c0 = 1;
					double c1 = 1;
					double c2 = 1;
					if (k0 == 0) {
						c0 = 1.0f / Math.sqrt(2.0);
					}
					if (k1 == 0) {
						c1 = 1.0f / Math.sqrt(2.0);
					}
					if (k2 == 0) {
						c2 = 1.0f / Math.sqrt(2.0);
					}
					output[outputPosition] = scale * c0 * c1 * c2 * output[outputPosition];
					
				}
				
			}
		
		}
		
	}
	
	private static void idct3d(final double[] output, final double[] input, final int offset, final int stride, final int frameSize, final int width, final int height, final int depth) {
		
		double scale = (double) (2.0f / Math.sqrt(width * height * depth));
		
		for (int n0 = 0; n0 < depth; n0++) {
		
			for (int n1 = 0; n1 < height; n1++) {
				
				for (int n2 = 0; n2 < width; n2++) {
					
					int outputPosition = offset + n0 * frameSize + n1 * stride + n2;
					
					for (int k0 = 0; k0 < depth; k0++) {
					
						for (int k1 = 0; k1 < height; k1++) {
							
							for (int k2 = 0; k2 < width; k2++) {
								
								double c0 = 1;
								double c1 = 1;
								double c2 = 1;
								if (k0 == 0) {
									c0 = 1.0f / Math.sqrt(2.0);
								}
								if (k1 == 0) {
									c1 = 1.0f / Math.sqrt(2.0);
								}
								if (k2 == 0) {
									c2 = 1.0f / Math.sqrt(2.0);
								}
								int inputPosition = offset + k0 * frameSize + k1 * stride + k2;
								double inputValue = input[inputPosition];
								output[outputPosition] += c0 * c1 * c2 * inputValue * Math.cos((Math.PI / (float) depth) * (n0 + 0.5f) * k0) * Math.cos((Math.PI / (float) height) * (n1 + 0.5f) * k1) * Math.cos((Math.PI / (float) width) * (n2 + 0.5f) * k2);
								
							}
							
						}
					
					}
	
					output[outputPosition] = scale * output[outputPosition];
					if (output[outputPosition] > 255) output[outputPosition] = 255.0f;
					if (output[outputPosition] < 0) output[outputPosition] = 0;
					
				}
				
			}
			
		}
		
	}

	public static void applyDctInBlocks(int width, int height, int blockSize, double[] pixels, double[] dctCoeff) {
		runDctTask(width, height, blockSize, pixels, dctCoeff, DCT::dct3d);
	}
	
	public static void applyInverseDctInBlocks(int width, int height, int blockSize, double[] pixels, double[] dctCoeff) {
		runDctTask(width, height, blockSize, pixels, dctCoeff, DCT::idct3d);
	}
	
	private static void runDctTask(int width, int height, int blockSize, double[] pixels, double[] dctCoeff, DctFunction function) {
		
		int targetThreadsCount = Runtime.getRuntime().availableProcessors();
		int framesCount = pixels.length / (width * height);
		
		List<Thread> dctThreads = new ArrayList<>();
		int processedFrames = 0;
		while(processedFrames < framesCount) {
			for (; processedFrames < framesCount && dctThreads.size() < targetThreadsCount; processedFrames += blockSize) {
				dctThreads.add(new DctTask(processedFrames, processedFrames + blockSize, blockSize, width, height, pixels, dctCoeff, function));
			}
			for (Thread thread : dctThreads) {
				thread.start();
			}
			for (Thread thread : dctThreads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			dctThreads.clear();
		}
		
	}
	
	@FunctionalInterface
	private static interface DctFunction {
		void dctFunction(final double[] input, final double[] output, final int offset, final int stride, final int frameSize, final int width, final int height, final int depth);
	}
	
	private static class DctTask extends Thread {
		
		private int initialZ;
		private int finalZ;
		private int blockSize;
		private int width;
		private int height;
		private double[] pixels;
		private double[] dctCoeff;
		private DctFunction function;

		private DctTask(int initialZ, int finalZ, int blockSize, int width, int height, double[] pixels, double[] dctCoeff, DctFunction function) {
			this.initialZ = initialZ;
			this.finalZ = finalZ;
			this.blockSize = blockSize;
			this.width = width;
			this.height = height;
			this.pixels = pixels;
			this.dctCoeff = dctCoeff;
			this.function = function;
		}

		public void run() {
			
			int frameSize = width * height;
			for (int z = initialZ; z < finalZ; z += blockSize) {
				for (int y = 0; y < height; y += blockSize) {
					for (int x = 0; x < width; x += blockSize) {
						function.dctFunction(pixels, dctCoeff, z * frameSize + y * width + x, width, frameSize, blockSize, blockSize, blockSize);
					}
				}
			}
			
		}
		
	}
	
}
