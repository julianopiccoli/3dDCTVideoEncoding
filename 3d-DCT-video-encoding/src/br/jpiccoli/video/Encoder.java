package br.jpiccoli.video;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.Deflater;

import br.jpiccoli.video.dct.DCT;

public class Encoder {

	public static void main(String[] args) throws IOException {
		
		if (args.length < 4) {
			System.out.println("Usage: java Encoder <input file> <output file> <frame width> <frame height> <number of frames to encode");
			System.out.println("Parameters <input file>, <output file>, <frame width>, <frame height> are mandatory");
			System.exit(-1);
		}
		
		System.out.println("Starting");
		
		File inputFile = new File(args[0]);
		int width = Integer.parseInt(args[2]);
		int height = Integer.parseInt(args[3]);
		int depth = 0;
		int blockSize = 8;
		int frameSize = width * height;
		if (args.length >= 4) {
			depth = Integer.parseInt(args[4]);
		} else {
			depth = (int) (inputFile.length() / frameSize);
		}
		
		// Depth must be a multiple of blockSize.
		int exceedingFrames = depth % blockSize;
		depth -= exceedingFrames;
		
		System.out.println("Reading input file");
		
		// Reading the input file. This is not exactly the best
		// approach since the full file is stored into memory, which
		// limits the maximum file size...
		byte[] buffer = new byte[width * height * depth];
		DataInputStream input = new DataInputStream(new FileInputStream(inputFile));
		input.readFully(buffer);
		input.close();
		double[] pixels = new double[buffer.length];
		double[] dctCoeff = new double[buffer.length];
		for (int i = 0; i < buffer.length; i++) {
			int pixelValue = (int) (buffer[i] & 0xFF);
			pixels[i] = pixelValue;
		}
		
		System.out.println("Applying DCT. This process may take some time to complete...");
		
		// Applying DCT. This call blocks until the DCT is completed.
		// The process is executed in multiple threads, but it is
		// still VERY slow.
		DCT dct = new DCT(pixels, dctCoeff, width, height, blockSize, blockSize, blockSize);
		dct.run();
		
		System.out.println("DCT complete. Applying quantization.");
		
		pixels = null;
		double[] quantizationOutput = new double[buffer.length];
		int quantizationOutputIndex = 0;
		
		// Quantization of the DCT output. This process divides each sample
		// of the DCT cube by five times the sum of its coordinates in the cube.
		// This turns most of the higher frequencies to zero.
		for (int z = 0; z < depth; z += blockSize) {
			for (int y = 0; y < height; y += blockSize) {
				for (int x = 0; x < width; x += blockSize) {
					for (int k = 0; k < blockSize; k++) {
						for (int i = 0; i < blockSize; i++) {
							for (int j = 0; j < blockSize; j++) {
								int dctCoeffCubePosition = (z + k) * frameSize + (y + i) * width + x + j;
								quantizationOutput[quantizationOutputIndex] = Math.round(dctCoeff[dctCoeffCubePosition] / Math.max(1, 5 * (i + j + k)));
								quantizationOutputIndex++;
							}
						}
					}
				}
			}
		}
		
		System.out.println("Compressing the resulting data");
		
		buffer = null;
		buffer = new byte[width * height * depth];
		
		// The DCT Coefficients inside the cubes are listed in diagonal slices to maximize the lengths
		// of the zeroes sequences. This increases the efficiency of the deflate compressor.
		List<int[]> positions = CubeUtils.diagonalSlices(blockSize, blockSize, blockSize);
		
		// Applying Exp-Golomb coding to the quantized data.
		ExpGolombWriter writer = new ExpGolombWriter();
		writer.setOutput(buffer);
		int blockLength = blockSize * blockSize * blockSize;
		for (int offset = 0; offset < quantizationOutput.length; offset += blockLength) {
			for (int index = 0; index < positions.size(); index++) {
				int[] position = positions.get(index);
				int dctCoeffInt = (int) quantizationOutput[offset + position[0] + (position[1] * blockSize) + (position[2] * blockSize * blockSize)];
				writer.writeValue(dctCoeffInt);
			}
		}
		
		// Deflating the Exp-Golomb coded data.
		FileOutputStream output = new FileOutputStream(args[1]);
		byte[] outputBuffer = new byte[width * height];
		Deflater deflater = new Deflater();
		deflater.setInput(buffer, 0, writer.getBufferPosition() + 1);
		deflater.finish();
		int deflatedLength = deflater.deflate(outputBuffer);
		while(deflatedLength != 0) {
			output.write(outputBuffer, 0, deflatedLength);
			deflatedLength = deflater.deflate(outputBuffer);
		}
		output.flush();
		output.close();
		
		System.out.println("Finished. Frames encoded: " + depth);
		
	}
	
}
