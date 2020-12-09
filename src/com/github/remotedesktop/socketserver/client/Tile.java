package com.github.remotedesktop.socketserver.client;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import com.github.remotedesktop.Config;

public class Tile {

	private static final Logger logger = Logger.getLogger(Tile.class.getName());
	private ByteArrayOutputStream stream;
	private BufferedImage image;
	private boolean dirty;
	private int width;
	private int height;

	public interface Observable {
		public void updateTile(int x, int y);

		public void updateTileFinish(String cursor);

		public void stop();

	}

	public Tile() {
		stream = new ByteArrayOutputStream();
		dirty = true;
		width = 0;
		height = 0;
	}

	private void writeImageToOutputStream(BufferedImage image, OutputStream outs) {
		this.image = image;
		try {
			width = image.getWidth();
			height = image.getHeight();
		
    	    ImageWriter imageWriter = ImageIO.getImageWritersByFormatName("jpg").next();
    	    ImageWriteParam param = imageWriter.getDefaultWriteParam();
    	    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(Config.jpeg_quality); // an integer between 0 and 1

    	    imageWriter.setOutput(new MemoryCacheImageOutputStream(outs));
		
    	    IIOImage out = new IIOImage(image, null, null);
    	    imageWriter.write(null, out, param);

    	    imageWriter.dispose();

		} catch (Exception e) {
			logger.log(Level.SEVERE, "write image", e);
		}
	}

	public int fileSize() {
		return stream.size();
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

	public boolean updateImage2(BufferedImage image) {
		if (this.image == null || !compareImage(this.image, image)) {
			stream.reset();
			writeImageToOutputStream(image, stream);
			dirty = true;
			return true;
		} else {
			return false;
		}
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
