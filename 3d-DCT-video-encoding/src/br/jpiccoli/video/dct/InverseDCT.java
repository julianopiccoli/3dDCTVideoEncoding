package br.jpiccoli.video.dct;

/**
 * The inverse Discrete Cosine Transform
 * 
 * @author Juliano Piccoli
 *
 */
public class InverseDCT extends Transform {

	private double[][] coefficients;
	
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
	public InverseDCT(final double[] input, final double[] output, final int frameWidth, final int frameHeight, final int cubeWidth, final int cubeHeight, final int cubeDepth) {
		super(input, output, frameWidth, frameHeight, cubeWidth, cubeHeight, cubeDepth);
		initialize();
	}
	
	/**
	 * Apply the inverse DCT to a single block.
	 * 
	 * @see Transform
	 */
	protected void apply(final int x, final int y, final int z) {
		
		final int offset = z * frameSize + y * frameWidth + x;
		final int cubeFaceSize = cubeWidth * cubeHeight;
		
		/*
		 * Iterates over the list of input data for the block looking for non-zero values.
		 * Zero values are discarded because they don't contribute to the final result.
		 * Assuming that the input data was quantized and de-quantized, most of the values are going to be
		 * zero.
		 */		
		int nonZeroInputsQuantity = 0;
		final double[] nonZeroInputsVector = new double[cubeWidth * cubeHeight * cubeDepth];
		final int[] coefficientsIndex = new int[cubeWidth * cubeHeight * cubeDepth];

		for (int k0 = 0, frameOffset = offset; k0 < cubeDepth; k0++, frameOffset += frameSize) {
			for (int k1 = 0, lineOffset = frameOffset; k1 < cubeHeight; k1++, lineOffset += frameWidth) {
				for (int k2 = 0, inputOffset = lineOffset; k2 < cubeWidth; k2++, inputOffset++) {
					if (Math.abs(input[inputOffset]) > 1E-9) {
						nonZeroInputsVector[nonZeroInputsQuantity] = input[inputOffset];
						coefficientsIndex[nonZeroInputsQuantity++] = k0 * cubeFaceSize + k1 * cubeWidth + k2;
					}
				}
			}
		}
		
		for (int n0 = 0, frameOffset = offset; n0 < cubeDepth; n0++, frameOffset += frameSize) {
			for (int n1 = 0, lineOffset = frameOffset; n1 < cubeHeight; n1++, lineOffset += frameWidth) {
				for (int n2 = 0, outputOffset = lineOffset; n2 < cubeWidth; n2++, outputOffset++) {
					int outputIndex = n0 * cubeFaceSize + n1 * cubeWidth + n2;
					for (int index = 0; index < nonZeroInputsQuantity; index++) {
						output[outputOffset] += nonZeroInputsVector[index] * coefficients[outputIndex][coefficientsIndex[index]];
					}
				}
			}
		}
		
		/*
		 * Assuming that the output values are color intensities, the minimum possible value is
		 * zero and the maximum, 255.
		 */
		for (int n0 = 0, frameOffset = offset; n0 < cubeDepth; n0++, frameOffset += frameSize) {
			for (int n1 = 0, lineOffset = frameOffset; n1 < cubeHeight; n1++, lineOffset += frameWidth) {
				for (int n2 = 0, outputOffset = lineOffset; n2 < cubeWidth; n2++, outputOffset++) {
					output[outputOffset] = Math.max(0, Math.min(255.0d, output[outputOffset]));
				}
			}
		}
		
	}
	
	/**
	 * Computes the grid of coefficients that must be multiplied by the input data.
	 */
	private void initialize() {
		
		this.coefficients = new double[cubeWidth * cubeHeight * cubeDepth][cubeWidth * cubeHeight * cubeDepth];
		final double scale = (double) (2.0f / Math.sqrt(cubeWidth * cubeHeight * cubeDepth));
		
		final double piOverWidth = Math.PI / (float) cubeWidth;
		final double piOverHeight = Math.PI / (float) cubeHeight;
		final double piOverDepth = Math.PI / (float) cubeDepth;
		
		final int cubeFaceSize = cubeWidth * cubeHeight;
		
		for (int n0 = 0; n0 < cubeDepth; n0++) {	
			for (int n1 = 0; n1 < cubeHeight; n1++) {
				for (int n2 = 0; n2 < cubeWidth; n2++) {
					
					final int outputIndex = n0 * cubeFaceSize + n1 * cubeWidth + n2;
					
					for (int k0 = 0; k0 < cubeDepth; k0++) {
						for (int k1 = 0; k1 < cubeHeight; k1++) {
							for (int k2 = 0; k2 < cubeWidth; k2++) {
								
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
								
								final int inputIndex = k0 * cubeFaceSize + k1 * cubeWidth + k2;
								
								coefficients[outputIndex][inputIndex] = scale * c0 * c1 * c2 * Math.cos(piOverDepth * (n0 + 0.5f) * k0) * Math.cos(piOverHeight * (n1 + 0.5f) * k1) * Math.cos(piOverWidth * (n2 + 0.5f) * k2);
								
							}
						}
					}
					
				}
			}
		}
		
	}
	
	private void idct3d(final int x, final int y, final int z) {
		
		final int offset = z * frameSize + y * frameWidth + x;
		double scale = (double) (2.0f / Math.sqrt(cubeWidth * cubeHeight * cubeDepth));
		
		for (int n0 = 0; n0 < cubeDepth; n0++) {
			for (int n1 = 0; n1 < cubeHeight; n1++) {
				for (int n2 = 0; n2 < cubeWidth; n2++) {
					
					int outputPosition = offset + n0 * frameSize + n1 * frameWidth + n2;
					
					for (int k0 = 0; k0 < cubeDepth; k0++) {
						for (int k1 = 0; k1 < cubeHeight; k1++) {
							for (int k2 = 0; k2 < cubeWidth; k2++) {
								
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
								int inputPosition = offset + k0 * frameSize + k1 * frameWidth + k2;
								double inputValue = input[inputPosition];
								output[outputPosition] += c0 * c1 * c2 * inputValue * Math.cos((Math.PI / (float) cubeDepth) * (n0 + 0.5f) * k0) * Math.cos((Math.PI / (float) cubeHeight) * (n1 + 0.5f) * k1) * Math.cos((Math.PI / (float) cubeWidth) * (n2 + 0.5f) * k2);
								
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
	
}
