package com.github.remotedesktop.socketserver.client;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Adler32;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

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

	private IIOMetadata getIIOMetadata(BufferedImage image, ImageWriter imageWriter, ImageWriteParam param) {
		ImageTypeSpecifier spec = ImageTypeSpecifier.createFromRenderedImage(image);
		IIOMetadata metadata = imageWriter.getDefaultImageMetadata(spec, param);
		return metadata;
	}

	private void writeImageToOutputStream(BufferedImage image, OutputStream outs) {
		ImageWriter imgwriter = null;
		ImageOutputStream ios = null;
		this.image = image;
		try {
			/* ImageIO.write(image, "JPG", outs); */
			width = image.getWidth();
			height = image.getHeight();
			ios = ImageIO.createImageOutputStream(outs);

			// create image writer
			Iterator<?> iter = ImageIO.getImageWritersByFormatName("jpeg");
			imgwriter = (ImageWriter) iter.next();
			ImageWriteParam iwp = imgwriter.getDefaultWriteParam();
			iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			iwp.setCompressionQuality(Config.jpeg_quality); // an integer between 0 and 1

			imgwriter.setOutput(ios);
			IIOMetadata meta = getIIOMetadata(image, imgwriter, iwp);
			imgwriter.write(meta, new IIOImage(image, null, meta), iwp);
			if (ios != null) {
				ios.flush();
			}
		} catch (Exception e) {
			imgwriter.abort();
			logger.log(Level.SEVERE, "write image", e);
		} finally {
			if (ios != null)
				try {
					ios.close();
				} catch (Exception e) {
					/* ignore */}
			if (imgwriter != null)
				try {
					imgwriter.dispose();
				} catch (Exception e) {
					/* ignore */}
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
