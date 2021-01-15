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
		
		public ImageRender(int width, int height) {
			frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}
		
		@Override
		public void paint(Graphics g) {
			g.drawImage(frame, 0, 0, null);
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
			byte[] inputBuffer = new byte[frame.getWidth() * frame.getHeight()];
			int[] rgbBuffer = new int[frame.getWidth() * frame.getHeight()];
			long now = System.currentTimeMillis();
			long nextFrameTimestamp = now;
			
			try (DataInputStream input = new DataInputStream(new FileInputStream(file))) {
				
				while(true) {
					input.readFully(inputBuffer);
					for (int index = 0; index < inputBuffer.length; index++) {
						int r = (inputBuffer[index] & 0xFF) << 16;
						int g = (inputBuffer[index] & 0xFF) << 8;
						int b = inputBuffer[index] & 0xFF;
						rgbBuffer[index] = r | g | b;
					}
					frame.setRGB(0, 0, frame.getWidth(), frame.getHeight(), rgbBuffer, 0, frame.getWidth());
					render.repaint();
					now = System.currentTimeMillis();
					nextFrameTimestamp = nextFrameTimestamp + frameTimeInMs;
					long timeToNextFrame = nextFrameTimestamp - now;
					if (timeToNextFrame > 0) {
						Thread.sleep(timeToNextFrame);
					}
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
		frame.setSize(width, height);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
	}
	
}
