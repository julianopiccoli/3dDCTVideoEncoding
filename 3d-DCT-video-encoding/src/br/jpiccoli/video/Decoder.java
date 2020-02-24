package br.jpiccoli.video;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Decoder {
	
	public static void main(String[] args) throws IOException, DataFormatException {
		
		if (args.length < 5) {
			System.out.println("Usage: java Decoder <input file> <output file> <frame width> <frame height> <number of frames to decode>");
			System.out.println("All parameters are mandatory");
			System.exit(-1);
		}
		
		System.out.println("Starting");
		
		File inputFile = new File(args[0]);
		int width = Integer.parseInt(args[2]);
		int height = Integer.parseInt(args[3]);
		int depth = Integer.parseInt(args[4]);
		int blockSize = 8;
		int frameSize = width * height;
		
		// Depth must be a multiple of blockSize.
		int exceedingFrames = depth % blockSize;
		depth -= exceedingFrames;
		
		// Read and inflate the input file contents. The inflated
		// data will be fully stored in memory.
		System.out.println("Reading and inflating the input file");
		FileInputStream inputStream = new FileInputStream(inputFile);
		ByteArrayOutputStream memoryStream = new ByteArrayOutputStream();
		byte[] inputBuffer = new byte[1024 * 1024];
		byte[] inflatedDataBuffer = new byte[width * height];
		Inflater inflater = new Inflater();
		int readResult = inputStream.read(inputBuffer);
		while(readResult > 0) {
			inflater.setInput(inputBuffer, 0, readResult);
			int inflatedLength = inflater.inflate(inflatedDataBuffer);
			while(inflatedLength != 0) {
				memoryStream.write(inflatedDataBuffer, 0, inflatedLength);
				inflatedLength = inflater.inflate(inflatedDataBuffer);
			}
			readResult = inputStream.read(inputBuffer);
		}
		inputStream.close();
		inflater.end();
		inflatedDataBuffer = memoryStream.toByteArray();
		memoryStream.close();
		
		// Read the Exp-Golomb coded data.
		System.out.println("Decoding the Exp-Golomb encoded data");
		ExpGolombReader expGolombReader = new ExpGolombReader();
		expGolombReader.setInput(inflatedDataBuffer);
		
		List<int[]> positions = CubeUtils.diagonalSlices(blockSize, blockSize, blockSize);
		
		double[] quantizationInput = new double[width * height * depth];
		int blockLength = blockSize * blockSize * blockSize;
		for (int offset = 0; offset < quantizationInput.length; offset += blockLength) {
			for (int index = 0; index < positions.size(); index++) {
				int[] position = positions.get(index);
				quantizationInput[offset + position[0] + (position[1] * blockSize) + (position[2] * blockSize * blockSize)] = expGolombReader.readValue();
			}
		}

		double[] dctCoeffMatrix = new double[width * height * depth];
		int quantizationInputPosition = 0;
		// Dequantize the DCT cubes
		System.out.println("Dequantizing cubes");
		for (int z = 0; z < depth; z += blockSize) {
			for (int y = 0; y < height; y += blockSize) {
				for (int x = 0; x < width; x += blockSize) {
					for (int k = 0; k < blockSize; k++) {
						for (int i = 0; i < blockSize; i++) {
							for (int j = 0; j < blockSize; j++) {
								int dctCoeffCubePosition = (z + k) * frameSize + (y + i) * width + x + j;
								dctCoeffMatrix[dctCoeffCubePosition] = Math.round(quantizationInput[quantizationInputPosition] * Math.max(1, 5 * (i + j + k)));
								quantizationInputPosition++;
							}
						}
					}
				}
			}
		}

		// Apply the Inverse DCT to the dequantized data. This call is blocking and
		// the process is slow.
		System.out.println("Applying inverse DCT. This process may take some time to complete...");
		double[] videoPixels = new double[dctCoeffMatrix.length];
		DCT.applyInverseDctInBlocks(width, height, blockSize, videoPixels, dctCoeffMatrix);
		
		// Writing decoded data to output file
		System.out.println("Writing video to output file");
		File outputFile = new File(args[1]);
		try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
			byte[] outputData = new byte[width * height];
			for (int offset = 0; offset < videoPixels.length; offset += outputData.length) {
				for (int index = 0; index < outputData.length; index++) {
					outputData[index] = (byte) videoPixels[offset + index];
				}
				outputStream.write(outputData);
			}
			outputStream.flush();
		}
		
		System.out.println("Complete!");
		
	}

}
