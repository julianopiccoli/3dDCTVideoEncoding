package br.jpiccoli.video;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;

public class RenderVideo {

	private static class ImageRender extends JComponent {
	
		private BufferedImage frame;
		private float aspectRatio;
		
		public ImageRender(int width, int height) {
			frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			aspectRatio = ((float) width) / ((float) height);
		}
		
		@Override
		public void paint(Graphics g) {
			int availableWidth = getWidth();
			int availableHeight = getHeight();
			int imageHeight = (int) (availableWidth / aspectRatio);
			if (imageHeight <= availableHeight) {
				g.drawImage(frame, 0, (availableHeight - imageHeight) / 2, availableWidth, imageHeight, null);
			} else {
				int imageWidth = (int) (availableHeight * aspectRatio);
				g.drawImage(frame, (availableWidth - imageWidth) / 2, 0, imageWidth, availableHeight, null);
			}
		}
		
	}
	
	private static class VideoFileReader extends Thread {
		
		private File file;
		private ImageRender render;
		private long frameTimeInMs;
		
		public VideoFileReader(File file, ImageRender render, long frameTimeInMs) {
			this.file = file;
			this.render = render;
			this.frameTimeInMs = frameTimeInMs;
		}
		
		@Override
		public void run() {
			
			BufferedImage frame = render.frame;
			byte[] inputBuffer = new byte[frame.getWidth() * frame.getHeight() * 3];
			int[] rgbBuffer = new int[frame.getWidth() * frame.getHeight()];
			long now = System.currentTimeMillis();
			long nextFrameTimestamp = now;
			
			try (DataInputStream input = new DataInputStream(new FileInputStream(file))) {
				
				while(true) {
					input.readFully(inputBuffer);
					now = System.currentTimeMillis();
					long timeToNextFrame = nextFrameTimestamp - now;
					if (timeToNextFrame > 0) {
						Thread.sleep(timeToNextFrame);
					}					
					for (int index = 0; index < rgbBuffer.length; index++) {
						int r = (inputBuffer[index * 3] & 0xFF) << 16;
						int g = (inputBuffer[index * 3 + 1] & 0xFF) << 8;
						int b = inputBuffer[index * 3 + 2] & 0xFF;
						rgbBuffer[index] = r | g | b;
					}
					frame.setRGB(0, 0, frame.getWidth(), frame.getHeight(), rgbBuffer, 0, frame.getWidth());
					render.repaint();
					nextFrameTimestamp = nextFrameTimestamp + frameTimeInMs;
					
				}
				
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	public static void main(String[] args) {
		
		if (args.length < 4) {
			System.out.println("Usage: java RenderVideo <input file> <width> <height> <framerate>");
			System.out.println("Parameters <input file>, <width>, <height> and <framerate> are mandatory");
			System.exit(-1);
		}
		
		File inputFile = new File(args[0]);
		int width = Integer.parseInt(args[1]);
		int height = Integer.parseInt(args[2]);
		float fps = Float.parseFloat(args[3]);
		long frameTime = (long) (1000.0f / fps);
		
		JFrame frame = new JFrame();
		ImageRender render = new ImageRender(width, height);
		JButton playButton = new JButton("Play");
		frame.setLayout(new BorderLayout());
		frame.add(render, BorderLayout.CENTER);
		frame.add(playButton, BorderLayout.SOUTH);
		playButton.addActionListener((e) -> {
			VideoFileReader videoReader = new VideoFileReader(inputFile, render, frameTime);
			Thread thread = new Thread(videoReader);
			thread.start();
		});
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
	}
	
}
