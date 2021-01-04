package com.github.remotedesktop.socketserver.client;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import com.github.remotedesktop.Config;


public class Tile implements Runnable {

	static final Logger LOGGER = Logger.getLogger(Tile.class.getName());
	private ByteArrayOutputStream stream;
	private BufferedImage prevImage;
	private BufferedImage actImage;
	private boolean dirty;
	private int width;
	private int height;
	private ImageWriter imageWriter;
	ImageWriteParam param;
	private Object workerLock = new Object();
	private boolean isFinish = true;
	private ThreadPoolExecutor runner;

	public Tile(ThreadPoolExecutor pool) {
		this.runner = pool;
		
		stream = new ByteArrayOutputStream();
		dirty = false;
		width = 0;
		height = 0;

		imageWriter = ImageIO.getImageWritersByFormatName("jpg").next();
		param = imageWriter.getDefaultWriteParam();
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		param.setCompressionQuality(Config.jpeg_quality); // an integer between 0 and 1
	
	}

	public void updateQuality(float quality) {
		synchronized (param) {
			param.setCompressionQuality(quality);
			LOGGER.fine(String.format("set compression quality to %f", quality));
		}
	}
	
	private void writeImageToOutputStream(BufferedImage image, OutputStream outs) {
		this.prevImage = image;
		try {
			width = image.getWidth();
			height = image.getHeight();

			imageWriter.setOutput(new MemoryCacheImageOutputStream(outs));

			IIOImage out = new IIOImage(image, null, null);

			synchronized(param) {
				imageWriter.write(null, out, param);
			}
			
			imageWriter.reset();

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "write image", e);
		}
	}

	public int fileSize() {
		return stream.size();
	}

	public void updateImage2(BufferedImage image) {
		synchronized (workerLock) {
			assert (isFinish);

			isFinish = false;
			actImage = image;
			
			runner.execute(this);
		}
	}

	public void run() {
		try {
			processImage();
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "could not create tile", e);
		} finally {
			synchronized (workerLock) {
				isFinish = true;
				workerLock.notifyAll();
			}
		}
	}

	public void waitForFinish() throws InterruptedException {
		synchronized (workerLock) {
			while (!isFinish) {
				workerLock.wait();
			}
		}
	}

	private void processImage() {
		if (prevImage == null || !compareImage(prevImage, actImage)) {
			stream.reset();
			writeImageToOutputStream(actImage, stream);
			dirty = true;
		}
	}

	private static boolean compareImage(BufferedImage biA, BufferedImage biB) {
		DataBuffer dbA = biA.getData().getDataBuffer();
		DataBuffer dbB = biB.getData().getDataBuffer();

		int sizeA = dbA.getSize();
		for (int i = 0; i < sizeA; i++) {
			if (dbA.getElem(i) != dbB.getElem(i)) {
				return false;
			}
		}
		return true;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void clearDirty() {
		dirty = false;
	}

	public void setDirty() {
		dirty = true;
	}

	public byte[] getData() {
		return stream.toByteArray();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
