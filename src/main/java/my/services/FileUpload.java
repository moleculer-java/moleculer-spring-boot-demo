/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package my.services;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Controller;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.stream.PacketStream;
import services.moleculer.util.CheckedTree;
import services.moleculer.web.common.HttpConstants;
import services.moleculer.web.router.HttpAlias;

/**
 * File upload based on Moleculer Streams. URL of this sample (when running the
 * example on a local Netty server):<br>
 * <br>
 * http://localhost:3000/upload.html
 */
@Name("upload")
@Controller
public class FileUpload extends Service {

	// --- THUMBNAIL SIZE ---

	/**
	 * Max upload size = 1024 * 1024 * 3 = 3 MBytes.
	 */
	private static final int MAX_UPLOAD_SIZE = 1024 * 1024 * 3;

	/**
	 * Max uploaded image width = 5000 pixels.
	 */
	private static final int MAX_IMAGE_WIDTH = 5000;

	/**
	 * Max uploaded image height = 4000 pixels.
	 */
	private static final int MAX_IMAGE_HEIGHT = 4000;

	/**
	 * Width of generated thumbnail image.
	 */
	private static final int THUMBNAIL_WIDTH = 500;

	/**
	 * Height of the generated thumbnail image.
	 */
	private static final int THUMBNAIL_HEIGHT = 500;

	// --- BYTES OF THUMBNAIL IMAGE ---

	private AtomicReference<byte[]> thumbnailBytes = new AtomicReference<>();

	// --- UPLOADED BYTES BY REMOTE HOST ---

	private ConcurrentHashMap<String, BytesPerAddress> uploadedBytes = new ConcurrentHashMap<>();

	// --- CREATE INITIAL IMAGE ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Draw something...
		BufferedImage thumbnail = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = thumbnail.createGraphics();
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
		g.setColor(new Color(220, 220, 220));
		g.setFont(new Font("Dialog", Font.BOLD, 40));
		String text = "Image Preview";
		FontMetrics metrics = g.getFontMetrics();
		Rectangle2D r = metrics.getStringBounds(text, 0, text.length(), g);
		g.drawString(text, (int) (THUMBNAIL_WIDTH - r.getWidth()) / 2, (int) (THUMBNAIL_HEIGHT - r.getHeight()) / 2);
		g.dispose();
		setThumbnail(thumbnail);
	}

	// --- RECEIVE UPLOADED IMAGE ---

	@Name("receive")
	public Action receiveAction = ctx -> {
		
		// First load? (~= no stream)
		if (ctx.stream == null) {
			Tree rsp = new Tree();
			Tree meta = rsp.getMeta();
			meta.put("$template", "upload");
			return rsp;
		}
		
		// Get remote host
		logger.info("Receiving file...");
		String remoteAddress = ctx.params.getMeta().get("address", "");

		// Cleanup "uploadedBytes" map
		uploadedBytes.remove(remoteAddress);
		Iterator<BytesPerAddress> i = uploadedBytes.values().iterator();
		long now = System.currentTimeMillis();
		while (i.hasNext()) {
			if (now - i.next().timestamp > 30000) {
				i.remove();
			}
		}

		return new Promise(res -> {

			// Write incoming bytes from stream to temporary buffer
			ByteArrayOutputStream destination = new ByteArrayOutputStream(1024);
			ctx.stream.onPacket((bytes, cause, close) -> {

				// Data received
				if (bytes != null) {
					logger.info(bytes.length + " bytes received...");
					try {
						int newSize = destination.size() + bytes.length;
						if (destination.size() + bytes.length > MAX_UPLOAD_SIZE) {
							throw new IOException(
									"Too large image! Maximum image size is " + MAX_UPLOAD_SIZE + " bytes.");
						}
						uploadedBytes.put(remoteAddress, new BytesPerAddress(newSize));
						destination.write(bytes);
					} catch (Throwable err) {
						cause = err;
					}
				}

				// End of bytes (image received)
				if (close) {
					try {
						logger.info("Uploading finished.");
						ByteArrayInputStream source = new ByteArrayInputStream(destination.toByteArray());
						BufferedImage uploadedImage = ImageIO.read(source);
						if (uploadedImage == null) {
							throw new IOException("The uploaded image is not an image!");
						}
						if (uploadedImage.getWidth() > MAX_IMAGE_WIDTH) {
							throw new IOException(
									"Too wide image! Maximum image width is " + MAX_IMAGE_WIDTH + " pixels.");
						}
						if (uploadedImage.getHeight() > MAX_IMAGE_HEIGHT) {
							throw new IOException(
									"Too large image! Maximum image height is " + MAX_IMAGE_HEIGHT + " pixels.");
						}
						logger.info("Resizing image to " + THUMBNAIL_WIDTH + " x " + THUMBNAIL_HEIGHT + " pixels...");
						BufferedImage thumbnail = createThumbnail(uploadedImage);
						setThumbnail(thumbnail);
						logger.info("Done.");

						// IMPORTANT PART: This section instructs APIGateway not
						// to give a JSON
						// response, but to convert JSON to HTML. The
						// "$template" parameter
						// specifies the file name of the template (relative
						// path to template).
						// Technically, the answer here will be "upload.html"
						// (~= resend/reload).
						Tree rsp = new Tree();
						Tree meta = rsp.getMeta();
						meta.put("$template", "upload");

						res.resolve(rsp);

					} catch (Throwable error) {
						cause = error;
					} finally {
						if (cause == null) {
							res.resolve();
						}
					}
				}

				// Error occured
				if (cause != null) {
					res.reject(cause);
				}

			});
		});
	};

	// --- GET UPLOADED IMAGE ---

	@HttpAlias(method = "GET", path = "api/thumbnail")
	public Action getThumbnail = ctx -> {

		// Get image bytes
		byte[] bytes = null;
		for (int i = 0; i < 10; i++) {
			synchronized (thumbnailBytes) {
				bytes = thumbnailBytes.get();
				if (bytes != null) {
					break;
				}
				thumbnailBytes.wait(1000);
			}
		}
		if (bytes == null) {
			throw new IOException("Unable to get image!");
		}

		// Write bytes into the response stream
		PacketStream stream = broker.createStream();
		stream.sendData(bytes);
		stream.sendClose();

		// Set response headers
		Tree rsp = new CheckedTree(stream);
		Tree meta = rsp.getMeta();
		Tree headers = meta.putMap(HttpConstants.META_HEADERS);
		headers.put(HttpConstants.CONTENT_TYPE, "image/png");
		headers.put(HttpConstants.CONTENT_LENGTH, bytes.length);
		headers.put(HttpConstants.CACHE_CONTROL, HttpConstants.NO_CACHE);

		// Return response
		return rsp;
	};

	// --- GET UPLOADED BYTES ---

	@HttpAlias(method = "GET", path = "api/count")
	public Action getUploadCount = ctx -> {

		// Get remote host
		String remoteAddress = ctx.params.getMeta().get("address", "");

		// Get counter
		BytesPerAddress bytesPerAddress = uploadedBytes.get(remoteAddress);

		// Return counter
		return bytesPerAddress == null ? 0 : bytesPerAddress.uploadedBytes;
	};

	// --- PRIVATE UTILITIES ---

	private BufferedImage createThumbnail(BufferedImage sourceImage) throws IOException {
		BufferedImage thumbnail = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = null;
		try {
			g = thumbnail.createGraphics();
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
			int width = sourceImage.getWidth();
			int height = sourceImage.getHeight();
			if (width > height) {
				double percent = ((double) THUMBNAIL_WIDTH / width) * 100;
				double newHeight = ((double) height / 100) * percent;
				Image scaledImage = sourceImage.getScaledInstance(500, (int) newHeight, Image.SCALE_SMOOTH);
				g.drawImage(scaledImage, 0, (int) ((THUMBNAIL_HEIGHT - newHeight) / 2), null);
				return thumbnail;
			}
			double percent = ((double) THUMBNAIL_HEIGHT / height) * 100;
			double newWidth = ((double) width / 100) * percent;
			Image scaledImage = sourceImage.getScaledInstance((int) newWidth, THUMBNAIL_HEIGHT, Image.SCALE_SMOOTH);
			g.drawImage(scaledImage, (int) ((THUMBNAIL_WIDTH - newWidth) / 2), 0, null);
			return thumbnail;
		} finally {
			if (g != null) {
				g.dispose();
			}
		}
	}

	private void setThumbnail(BufferedImage image) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
		ImageIO.write(image, "png", output);
		byte[] bytes = output.toByteArray();
		synchronized (thumbnailBytes) {
			thumbnailBytes.set(bytes);
			thumbnailBytes.notifyAll();
		}
	}

	private static final class BytesPerAddress {

		private final int uploadedBytes;
		private final long timestamp = System.currentTimeMillis();

		private BytesPerAddress(int uploadedBytes) {
			this.uploadedBytes = uploadedBytes;
		}

	}

}