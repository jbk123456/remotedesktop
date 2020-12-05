package com.github.remotedesktop.socketserver.client;

import java.awt.image.BufferedImage;
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
	private Adler32 checksum;
	private boolean dirty;
	private int width;
	private int height;

	public interface Observable {
		public void updateTile(int x, int y);

		public void updateTileFinish();

		public void stop();

	}

	public Tile() {
		stream = new ByteArrayOutputStream();
		checksum = new Adler32();
		dirty = true;
		width = 0;
		height = 0;
	}

	private IIOMetadata getIIOMetadata(BufferedImage image, ImageWriter imageWriter, ImageWriteParam param) {
		ImageTypeSpecifier spec = ImageTypeSpecifier.createFromRenderedImage(image);
		IIOMetadata metadata = imageWriter.getDefaultImageMetadata(spec, param);
		return metadata;
	}

	private void writeImage(BufferedImage image, OutputStream outs) {
		ImageWriter imgwriter = null;
		ImageOutputStream ios = null;
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

	public boolean updateImage2(BufferedImage image) {
		long oldsum;
		oldsum = checksum.getValue();
		calcChecksum2(image);
		if (oldsum != checksum.getValue()) {
			stream.reset();
			writeImage(image, stream);
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

	private void calcChecksum2(BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		int pixels[] = new int[w * h];
		PixelGrabber pg = new PixelGrabber(image, 0, 0, w, h, pixels, 0, w);
		checksum.reset();
		try {
			pg.grabPixels(1);
		} catch (InterruptedException e) {
			logger.info("interrupted waiting for pixels!");
			return;
		}
		if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
			logger.info("image fetch aborted or errored");
			return;
		}

		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
				if ((j * w + i) % 13 == 0)
					checksum.update(pixels[j * w + i]);
			}
		}

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
