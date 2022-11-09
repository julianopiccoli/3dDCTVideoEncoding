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
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CaptureScreen {

	public static void main(String[] args) throws AWTException, InterruptedException, IOException {
		
		if (args.length == 0) {
			System.out.println("Usage: java CaptureScreen <output file name> <frame rate> <horizontal scale down factor> <vertical scale down factor>");
			System.out.println("Parameter <output file name> is mandatory");
			System.exit(-1);
		}

		int fps = args.length > 1 ? Integer.parseInt(args[1]) : 24;
		long frameTime = (long) (1000.0f / fps);
		int horizontalScaleFactor = args.length > 2 ? Integer.parseInt(args[2]) : 1;
		int verticalScaleFactor = args.length > 3 ? Integer.parseInt(args[3]) : 1;
		
		GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		DisplayMode displayMode = environment.getDefaultScreenDevice().getDisplayMode();
		Rectangle rectangle = new Rectangle(0, 0, displayMode.getWidth(), displayMode.getHeight());
		Robot robot = new Robot();

		BlockingQueue<BufferedImage> imagesQueue = new LinkedBlockingQueue<BufferedImage>();
		Thread imageProcessingThread = new Thread(new ImageProcessor(args[0], displayMode.getWidth(), displayMode.getHeight(), horizontalScaleFactor,
				verticalScaleFactor, imagesQueue));
		imageProcessingThread.start();
		
		int frameCount = 0;
		long start;
		long now;
		long nextFrameTimestamp;
		long runTime;
		
		System.out.println("Screen capture will start in 5 seconds and will last for 10 seconds");
		
		Thread.sleep(5000);
		
		System.out.println("Screen capture started");
		
		start = System.currentTimeMillis();
		now = start;
		nextFrameTimestamp = now;
		runTime = 0;
		
		while(10000 > runTime) {
			
			long timeToNextFrame = nextFrameTimestamp - now;
			if (timeToNextFrame > 0) {
				System.out.println("Sleeping: " + timeToNextFrame);
				Thread.sleep(timeToNextFrame);
			}
			BufferedImage image = robot.createScreenCapture(rectangle);
			imagesQueue.put(image);
			frameCount++;
			nextFrameTimestamp = nextFrameTimestamp + frameTime;
			now = System.currentTimeMillis();
			runTime = now - start;
			
		}

		imageProcessingThread.interrupt();
		imageProcessingThread.join();
		System.out.println("Screen capture finished");
		System.out.println(frameCount + " frames were captured");
		System.out.println("Frame rate is " + (frameCount / 10.0f) + " fps");
				
	}
	
	private static final class ImageProcessor implements Runnable {
		
		private final String outputFilePath;
		private final int originalVideoWidth;
		private final int originalVideoHeight;
		private final int horizontalScaleFactor;
		private final int verticalScaleFactor;
		private final BlockingQueue<BufferedImage> imagesQueue;
		
		private ImageProcessor(String outputFilePath, int originalVideoWidth, int originalVideoHeight, int horizontalScaleFactor,
				int verticalScaleFactor, final BlockingQueue<BufferedImage> imagesQueue) {
			this.outputFilePath = outputFilePath;
			this.originalVideoWidth = originalVideoWidth;
			this.originalVideoHeight = originalVideoHeight;
			this.horizontalScaleFactor = horizontalScaleFactor;
			this.verticalScaleFactor = verticalScaleFactor;
			this.imagesQueue = imagesQueue;
		}

		@Override
		public void run() {
			
			try (OutputStream stream = new FileOutputStream(outputFilePath)) {
				int width = originalVideoWidth / horizontalScaleFactor;
				int height = originalVideoHeight / verticalScaleFactor;
				int adaptedWidth = width;
				int adaptedHeight = height;
				
				// Frame dimensions are rounded to a multiple of 8 because the encoder
				// uses blocks with 8x8x8 pixels
				while(adaptedWidth % 8 != 0) {
					adaptedWidth++;
				}
				while(adaptedHeight % 8 != 0) {
					adaptedHeight++;
				}
				int rgb[] = new int[adaptedWidth * adaptedHeight];
				byte[] output = new byte[adaptedWidth * adaptedHeight * 3];
				
				while(!Thread.interrupted() || !imagesQueue.isEmpty()) {
					try {
						BufferedImage image;
						if (Thread.interrupted()) {
							image = imagesQueue.remove();
						} else {
							image = imagesQueue.take();
						}
						BufferedImage scaled = new BufferedImage(adaptedWidth, adaptedHeight, BufferedImage.TYPE_INT_ARGB);
						Graphics graphics = scaled.createGraphics();
						graphics.drawImage(image, 0, 0, width, height, null);
						graphics.dispose();
						scaled.getRGB(0, 0, width, height, rgb, 0, width);
						for (int y = 0; y < scaled.getHeight(); y++) {
							for (int x = 0; x < scaled.getWidth(); x++) {
								int index = y * scaled.getWidth() + x;
								int pixel = rgb[index];
								byte r = (byte) ((pixel & 0xFF0000) >> 16);
								byte g = (byte) ((pixel & 0xFF00) >> 8);
								byte b = (byte) (pixel & 0xFF);
								output[index * 3] = r;
								output[index * 3 + 1] = g;
								output[index * 3 + 2] = b;
							}
						}
						stream.write(output);
					} catch (InterruptedException e) {
						//
					} catch (NoSuchElementException e) {
						//
					}
				}
				
				stream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
}
