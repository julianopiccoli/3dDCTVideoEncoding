package br.jpiccoli.video.dct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Discrete Cosine Transform.
 * 
 * @author Juliano Piccoli
 */
public class DCT extends Transform {

	private int equivalentSumsPerCube;
	private List<List<Multiplication>> multiplications;
	
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
	public DCT(final double[] input, final double[] output, final int frameWidth, final int frameHeight, final int cubeWidth, final int cubeHeight, final int cubeDepth) {
		super(input, output, frameWidth, frameHeight, cubeWidth, cubeHeight, cubeDepth);
		initialize();
	}
	
	/**
	 * Apply the DCT to a single block.
	 * 
	 * @see Transform
	 */
	protected void apply(final int x, final int y, final int z) {
		final Iterator<List<Multiplication>> multiplicationListsIterator = multiplications.iterator();
		final Double[] sumCache = new Double[equivalentSumsPerCube];
		final int offset = z * frameSize + y * frameWidth + x;
		
		for (int k0 = 0, frameOffset = offset; k0 < cubeDepth; k0++, frameOffset += frameSize) {
			for (int k1 = 0, lineOffset = frameOffset; k1 < cubeHeight; k1++, lineOffset += frameWidth) {
				for (int k2 = 0, outputOffset = lineOffset; k2 < cubeWidth; k2++, outputOffset++) {
					
					List<Multiplication> multiplicationList = multiplicationListsIterator.next();
					
					for (Multiplication multiplication : multiplicationList) {
						output[outputOffset] += multiplication.sum.computeSum(sumCache, offset, input) * multiplication.coefficient;
					}
					
				}
			}
		}
	}
	
	/**
	 * Initialize the auxiliary structures for calculating the DCT.
	 * This method computes all the coefficients that will be used in the transform and aggregates
	 * similar coefficients to reduce the number of multiplications.
	 * 
	 * For example, the first DCT value is calculated using the following formula:
	 * 
	 * DCT[0] = input[0] * coefficients[0, 0] + input[1] * coefficients[0, 1] + ... + input[N] * coefficients[0, N]
	 * 
	 * The distributive property of multiplication is used to group equivalent coefficients. Assuming that
	 * coefficients[0] and coefficients[1] are equal, for example, this method will rewrite the equation
	 * as below:
	 * 
	 * DCT[0] = (input[0] + input[1]) * coefficients[0, 0] + input[2] * coefficients[0, 1] + ... + input[N] * coefficients[0, N]
	 * 
	 */
	private void initialize() {
		
		this.multiplications = new ArrayList<>(cubeWidth * cubeHeight * cubeDepth);
		
		final double scale = (double) (2.0f / Math.sqrt(cubeWidth * cubeHeight * cubeDepth));
		
		final double piOverWidth = Math.PI / (float) cubeWidth;
		final double piOverHeight = Math.PI / (float) cubeHeight;
		final double piOverDepth = Math.PI / (float) cubeDepth;
		
		for (int k0 = 0; k0 < cubeDepth; k0++) {	
			for (int k1 = 0; k1 < cubeHeight; k1++) {
				for (int k2 = 0; k2 < cubeWidth; k2++) {
					
					final Map<Long, Multiplication> butterflyMap = new HashMap<>();
					
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
					
					for (int n0 = 0; n0 < cubeDepth; n0++) {
						for (int n1 = 0; n1 < cubeHeight; n1++) {
							for (int n2 = 0; n2 < cubeWidth; n2++) {
								
								final int inputOffset = n0 * frameSize + n1 * frameWidth + n2;
								// Computing the coefficient
								final double coefficient = scale * c0 * c1 * c2 * Math.cos(piOverDepth * (n0 + 0.5f) * k0) * Math.cos(piOverHeight * (n1 + 0.5f) * k1) * Math.cos(piOverWidth * (n2 + 0.5f) * k2);
								// The double is converted to a long value for comparing the coefficients.
								// The first 9 decimal places are taken into account for the comparison.
								final long longCoefficient = (long) (coefficient * 1E9);
								if (longCoefficient == 0) continue;	// Removing zero coefficients.
								Multiplication butterflyElement = butterflyMap.get(longCoefficient);
								if (butterflyElement == null) {
									// If no other coefficient calculated so far is equivalent to this one,
									// a new Multiplication instance is created. See the description for class
									// Multiplication below.
									butterflyElement = new Multiplication(coefficient);
									butterflyMap.put(longCoefficient, butterflyElement);
								}
								butterflyElement.offsets.add(inputOffset);
								
							}
						}
					}
				
					multiplications.add(new LinkedList<DCT.Multiplication>(butterflyMap.values()));
					
				}
				
			}
		}
		
		createSums();
		
	}
	
	/**
	 * After grouping similar coefficients, this method is used to identify sums that are equivalent.
	 * These sums will be cached (memoization) to speed up the transform process.
	 * 
	 * Developing on the same example presented at method "initialize" above, let's suppose that the equations
	 * for the first two DCT values are:
	 * 
	 * DCT[0] = (input[0] + input[1]) * coefficients[0, 0] + input[2] * coefficients[0, 2] + ... + input[N] * coefficients[0, N]
	 * DCT[1] = (input[0] + input[1]) * coefficients[1, 0] + input[2] * coefficient[1, 2] + ... +  input[N] * coefficients[1, N]
	 * 
	 * In this case, both equations contain the sum input[0] + input[1]. The value of this sum can be cached during the calculation
	 * of the first DCT value to speed up the second one.
	 */
	private void createSums() {
		final Map<Set<Integer>, Sum> sumsMap = new HashMap<>();
		for (List<Multiplication> multiplicationsList : multiplications) {
			for (Multiplication multiplication : multiplicationsList) {
				Sum sum = sumsMap.computeIfAbsent(multiplication.offsets, offset -> new Sum(equivalentSumsPerCube++, offset));
				multiplication.sum = sum;
			}
		}
	}
	
	/**
	 * Utility class used to group equivalent coefficients.
	 * 
	 * @author Juliano Piccoli
	 *
	 */
	private static class Multiplication {
		
		private final double coefficient;
		private final Set<Integer> offsets;
		private Sum sum;
		
		private Multiplication(final double coefficient) {
			this.coefficient = coefficient;
			this.offsets = new HashSet<Integer>();
		}
		
	}
	
	private void dct3d(final int x, final int y, final int z) {
		
		final int offset = z * frameSize + y * frameWidth + x;
		double scale = (double) (2.0f / Math.sqrt(cubeWidth * cubeHeight * cubeDepth));
		
		for (int k0 = 0; k0 < cubeDepth; k0++) {
			for (int k1 = 0; k1 < cubeHeight; k1++) {
				for (int k2 = 0; k2 < cubeWidth; k2++) {
	
					int outputPosition = offset + k0 * frameSize + k1 * frameWidth + k2;
					
					for (int n0 = 0; n0 < cubeDepth; n0++) {
						for (int n1 = 0; n1 < cubeHeight; n1++) {
							for (int n2 = 0; n2 < cubeWidth; n2++) {
								
								int inputPosition = offset + n0 * frameSize + n1 * frameWidth + n2;
								double inputValue = input[inputPosition];
								output[outputPosition] += inputValue * Math.cos((Math.PI / (float) cubeDepth) * (n0 + 0.5f) * k0) * Math.cos((Math.PI / (float) cubeHeight) * (n1 + 0.5f) * k1) * Math.cos((Math.PI / (float) cubeWidth) * (n2 + 0.5f) * k2);
								
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
		
}
