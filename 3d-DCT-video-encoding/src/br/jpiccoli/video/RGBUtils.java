package br.jpiccoli.video;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RGBUtils {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		if (args.length < 3) {
			printUsage();
		} else {
			if (args[0].equalsIgnoreCase("split")) {
				File inputFile = new File(args[1]);
				split(inputFile, args[2]);
			} else if (args[0].equalsIgnoreCase("mix")) {
				File outputFile = new File(args[2]);
				mix(args[1], outputFile);
			} else {
				printUsage();
			}
		}
		
	}
	
	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println();
		System.out.println("java RGBUtils split <input file> <output files names pattern> -> Split source raw RGB data file in three different files, each one storing data for a single color component");
		System.out.println("java RGBUtils mix <input files names pattern> <output file> -> Mix data from three different input files (each containing data for a single color) into a single file containing data in raw RGB format");
	}
	
	private static void split(File inputFile, String outputFileNamePattern) throws FileNotFoundException, IOException {
		
		File redChannelFile = new File(outputFileNamePattern + ".red");
		File greenChannelFile = new File(outputFileNamePattern + ".green");
		File blueChannelFile = new File(outputFileNamePattern + ".blue");
		
		try (FileInputStream input = new FileInputStream(inputFile);
				OutputStream redOutput = new BufferedOutputStream(new FileOutputStream(redChannelFile));
				OutputStream greenOutput = new BufferedOutputStream(new FileOutputStream(greenChannelFile));
				OutputStream blueOutput = new BufferedOutputStream(new FileOutputStream(blueChannelFile))) {
		
			byte[] buffer = new byte[1024 * 1024];
			byte[] redBuffer = new byte[1024 * 1024];
			byte[] greenBuffer = new byte[1024 * 1024];
			byte[] blueBuffer = new byte[1024 * 1024];
			
			byte[][] outputBuffers = new byte[][] {redBuffer, greenBuffer, blueBuffer};
			int[] outputBuffersPtrs;
			int bufferIndex = 0;
			
			int readResult = input.read(buffer);
			int xorResult = 0;
			int mask = 0;
			
			while(readResult > 0) {
				
				outputBuffersPtrs = new int[] {0, 0, 0};
				
				for (int index = 0; index < readResult; index++) {
					outputBuffers[bufferIndex][outputBuffersPtrs[bufferIndex]++] = buffer[index];
					bufferIndex++;
					// Little hack for creating a counter which loops from 0 to 2 without using any if statement.
					// Just for fun :)
					bufferIndex &= 3;
					xorResult = (bufferIndex & 1) ^ ((bufferIndex & 2) >> 1);
					mask = (xorResult << 1) | xorResult;
					bufferIndex &= mask;
				}
				
				redOutput.write(redBuffer, 0, outputBuffersPtrs[0]);
				greenOutput.write(greenBuffer, 0, outputBuffersPtrs[1]);
				blueOutput.write(blueBuffer, 0, outputBuffersPtrs[2]);
				readResult = input.read(buffer);
				
			}
			
			redOutput.flush();
			greenOutput.flush();
			blueOutput.flush();
			
		}
	}
	
	private static void mix(String inputFileNamePattern, File outputFile) throws FileNotFoundException, IOException {
		
		File redChannelFile = new File(inputFileNamePattern + ".red");
		File greenChannelFile = new File(inputFileNamePattern + ".green");
		File blueChannelFile = new File(inputFileNamePattern + ".blue");
		
		try (FileOutputStream output = new FileOutputStream(outputFile);
				InputStream redInput = new FileInputStream(redChannelFile);
				InputStream greenInput = new FileInputStream(greenChannelFile);
				InputStream blueInput = new FileInputStream(blueChannelFile)) {
		
			byte[] buffer = new byte[1024 * 1024 * 3];
			byte[] redBuffer = new byte[1024 * 1024];
			byte[] greenBuffer = new byte[1024 * 1024];
			byte[] blueBuffer = new byte[1024 * 1024];
			
			int readResult = readInput(redInput, redBuffer);
			readInput(greenInput, greenBuffer);
			readInput(blueInput, blueBuffer);
			
			while(readResult > 0) {
				
				for (int index = 0; index < readResult; index++) {
					buffer[index * 3] = redBuffer[index];
					buffer[index * 3 + 1] = greenBuffer[index];
					buffer[index * 3 + 2] = blueBuffer[index];
				}
				
				output.write(buffer, 0, readResult * 3);
				
				readResult = readInput(redInput, redBuffer);
				readInput(greenInput, greenBuffer);
				readInput(blueInput, blueBuffer);
				
			}
			
			output.flush();
			
		}
	}
	
	private static int readInput(InputStream input, byte[] buffer) throws IOException {
		
		int readResult = input.read(buffer);
		int totalRead = readResult > 0 ? readResult : 0;
		
		while(totalRead < buffer.length && readResult > 0) {
			readResult = input.read(buffer, totalRead, buffer.length - totalRead);
			totalRead += readResult > 0 ? readResult : 0;
		}
		
		return totalRead;
		
	}
	
}
