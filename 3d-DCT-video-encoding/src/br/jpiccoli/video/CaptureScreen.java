package br.jpiccoli.video;
import java.awt.AWTException;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;

public class CaptureScreen {

	public static void main(String[] args) throws AWTException, InterruptedException, IOException {
		
		if (args.length == 0) {
			System.out.println("Usage: java CaptureScreen <output file name> <frame rate> <horizontal scale down factor> <vertical scale down factor>");
			System.out.println("Parameter <output file name> is mandatory");
			System.exit(-1);
		}

		int fps = args.length > 1 ? Integer.parseInt(args[1]) : 24;
		long frameTime = (long) (1000.0f / fps);
		int horizontalScaleFactor = args.length > 2 ? Integer.parseInt(args[2]) : 2;
		int verticalScaleFactor = args.length > 3 ? Integer.parseInt(args[3]) : 2;
		
		GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		DisplayMode displayMode = environment.getDefaultScreenDevice().getDisplayMode();
		Rectangle rectangle = new Rectangle(0, 0, displayMode.getWidth(), displayMode.getHeight());
		Robot robot = new Robot();

		FileOutputStream stream = new FileOutputStream(args[0]);
		int width = displayMode.getWidth() / horizontalScaleFactor;
		int height = displayMode.getHeight() / verticalScaleFactor;
		int imageWidth = width;
		int imageHeight = height;
		
		// Frame dimensions are rounded to a multiple of 8 because the encoder
		// uses blocks with 8x8x8 pixels
		while(imageWidth % 8 != 0) {
			imageWidth++;
		}
		while(imageHeight % 8 != 0) {
			imageHeight++;
		}
		int rgb[] = new int[imageWidth * imageHeight];
		byte[] output = new byte[imageWidth * imageHeight];
		long start = System.currentTimeMillis();
		long now = System.currentTimeMillis();
		long nextFrame = now;
		
		System.out.println("Screen capture will start in 5 seconds and will last for 10 seconds");
		
		Thread.sleep(5000);
		
		System.out.println("Screen capture started");
		
		int frameCount = 0;
		
		while(10000 > (now - start)) {
			
			BufferedImage image = robot.createScreenCapture(rectangle);
			BufferedImage scaled = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics graphics = scaled.createGraphics();
			graphics.drawImage(image, 0, 0, width, height, null);
			graphics.dispose();
			scaled.getRGB(0, 0, width, height, rgb, 0, width);
			for (int y = 0; y < scaled.getHeight(); y++) {
				for (int x = 0; x < scaled.getWidth(); x++) {
					int index = y * scaled.getWidth() + x;
					int pixel = rgb[index];
					int r = (pixel & 0xFF0000) >> 16;
					int g = (pixel & 0xFF00) >> 8;
					int b = pixel & 0xFF;
					int avg = (int) ((r + g + b) / 3.0f);
					output[index] = (byte) ((avg << 16) | (avg << 8) | avg);
				}
			}
			stream.write(output);
			frameCount++;
			nextFrame = nextFrame + frameTime;
			now = System.currentTimeMillis();
			long timeToNextFrame = nextFrame - now;
			if (timeToNextFrame > 0) {
				Thread.sleep(timeToNextFrame);
			}
			
		}

		System.out.println("Screen capture finished");
		System.out.println("The output data has a resolution of " + imageWidth + "x" + imageHeight);
		System.out.println(frameCount + " frames were captured");
		
		stream.flush();
		stream.close();
		
	}
	
}
