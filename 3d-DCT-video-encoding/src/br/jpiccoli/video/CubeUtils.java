package br.jpiccoli.video;
import java.util.ArrayList;
import java.util.List;

public class CubeUtils {

	public static List<int[]> diagonalSlices(int width, int height, int depth) {
		
		List<int[]> positions = new ArrayList<>();
		
		int maxSum = (width - 1) + (height - 1) + (depth - 1);
		
		int x, y, z;
		
		for (int targetSum = 0; targetSum <= maxSum; targetSum++) {
			
			int maxWidth = Math.min(width - 1, targetSum);
			int maxHeight = Math.min(height - 1, targetSum);
			int maxDepth = Math.min(depth - 1, targetSum);
			int minWidth = Math.max(0, targetSum - (maxHeight + maxDepth));
			int minHeight = Math.max(0, targetSum - (maxWidth + maxDepth));
			int minDepth = Math.max(0, targetSum - (maxHeight + maxWidth));
			
			int sum = 0;
			
			for (y = minHeight; y <= maxHeight; y++) {
				for (z = minDepth; z <= maxDepth; z++) {
					for (x = minWidth; x <= maxWidth; x++) {
						sum = x + y + z;
						if (sum == targetSum) {
							positions.add(new int[] {x, y, z});
						}
					}
				}
			}
			
		}
		
		return positions;
		
	}
	
}
