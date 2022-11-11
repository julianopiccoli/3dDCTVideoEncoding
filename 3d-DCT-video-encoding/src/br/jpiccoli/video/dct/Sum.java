package br.jpiccoli.video.dct;

import java.util.Collection;

/**
 * Utility class that caches the results of sums.
 * 
 * @author Juliano Piccoli
 */
class Sum {

	private final int sumIndex;
	private final int[] offsetsArray;
	
	/**
	 * Constructor.
	 * @param sumIndex Index of this sum.
	 * @param offsets List of offsets.
	 */
	Sum(final int sumIndex, final Collection<Integer> offsets) {
		this.sumIndex = sumIndex;
		offsetsArray = new int[offsets.size()];
		int offsetsIndex = 0;
		for (Integer offset : offsets) {
			offsetsArray[offsetsIndex++] = offset;
		}
	}
	
	/**
	 * Computes the sum of the values contained at the specified vector's offsets.
	 * If the specified cache contains a pre-computed value for this sum, it is returned
	 * and the calculation is skipped.
	 * 
	 * @param cache Vector used as cache. This method will store the computed sum at the vector's index
	 * equivalent to this object's sumIndex property.
	 * @param cubeOffset Offset for calculating the sum. This offset will be added to this sum's list of offsets
	 * while calculating the sum.
	 * @param vector Vector containing the input data.
	 * @return The calculated sum.
	 */
	double computeSum(final Double[] cache, final int cubeOffset, final double[] vector) {
		Double result = cache[sumIndex];
		if (result == null) {
			double value = 0;
			for (int index = 0; index < offsetsArray.length; index++) {
				value += vector[cubeOffset + offsetsArray[index]];
			}
			result = value;
			cache[sumIndex] = value;
		}
		return result;
	}
	
}
