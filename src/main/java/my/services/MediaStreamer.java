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

import static services.moleculer.web.common.GatewayUtils.getFileSize;
import static services.moleculer.web.common.GatewayUtils.getFileURL;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Controller;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.error.MoleculerClientError;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.stream.PacketStream;
import services.moleculer.util.CheckedTree;

/**
 * This example demonstrates how to stream large video (or audio) files.
 * Playback supports "partial content" headers. This means we can seek/search
 * the video without downloading the entire file. URL of this sample (when
 * running the example on a local Netty server):<br>
 * <br>
 * http://localhost:3000/video.html
 */
@Name("mediaStreamer")
@Controller
public class MediaStreamer extends Service {

	// --- CONSTANTS ---

	/**
	 * Source of video (in this example it's a file).
	 */
	private static final String VIDEO_FILE = "www/media/video.mp4";

	/**
	 * Size of one media packet.
	 */
	private static final int PACKET_SIZE = 1024 * 100;

	/**
	 * Delay between the media packets.
	 */
	private static final long PACKET_DELAY = 20;

	// --- VARIABLES ---

	private URL videoUrl;

	private long videoLength;

	// --- MOLECULER COMPONENTS ---

	private ScheduledExecutorService scheduler;

	// --- INIT VARIABLES ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		videoUrl = getFileURL(VIDEO_FILE);
		videoLength = getFileSize(VIDEO_FILE);

		scheduler = broker.getConfig().getScheduler();
	}

	// --- STREAM VIDEO ---

	public Action getPacket = ctx -> {

		// Is video available?
		if (videoUrl == null || videoLength < 1) {
			Tree rsp = new Tree();
			rsp.getMeta().put("$statusCode", 404);
			return rsp;
		}

		// Get range from request
		String range = ctx.params.getMeta().get("range", "");
		long startPosition = 0;
		long endPosition = -1;
		if (range != null) {
			int i = range.indexOf('=');
			if (i > -1) {
				range = range.substring(i + 1);
				i = range.indexOf('-');
				if (i > -1) {
					try {
						startPosition = Long.parseLong(range.substring(0, i));
					} catch (Exception ignored) {
					}
					if (i < range.length() - 1) {
						try {
							endPosition = Long.parseLong(range.substring(i + 1));
						} catch (Exception ignored) {
						}
					}
				}
			}
		}

		// Verify "start" position
		if (startPosition > videoLength - 1) {
			startPosition = videoLength - 1;
		} else if (startPosition < 0) {
			startPosition = 0;
		}

		// Verify "end" position
		if (endPosition < startPosition || endPosition > videoLength - 1) {
			endPosition = videoLength - 1;
		}

		// Calculate content length
		long contentLength = endPosition - startPosition + 1;

		// Create response stream
		PacketStream destination = broker.createStream();

		// Start sending the requested packet...
		final long bytesToSkip = startPosition;
		scheduler.schedule(() -> {
			logger.info("Starting to transfer " + contentLength + " bytes from position " + bytesToSkip + "...");
			InputStream source = null;
			try {
				source = videoUrl.openStream();
				if (bytesToSkip > 0) {
					int skipped = 0;
					while (skipped < bytesToSkip) {
						skipped += source.skip(bytesToSkip + skipped);
					}
				}
				sendPacket(source, destination, 0, contentLength);
			} catch (Throwable cause) {
				closeStreams(source, destination, cause);
			}
		}, PACKET_DELAY, TimeUnit.MILLISECONDS);

		// Set response headers
		Tree rsp = new CheckedTree(destination);
		Tree meta = rsp.getMeta();

		Tree headers = meta.putMap("$responseHeaders");
		headers.put("Content-Type", "video/mp4");
		headers.put("Content-Length", contentLength);
		headers.put("Pragma", "no-cache");
		headers.put("Expires", "-1");
		headers.put("Cache-Control", "no-store, no-cache, must-revalidate, " + "post-check=0, pre-check=0");
		headers.put("Accept-Ranges", "bytes");
		headers.put("Content-Range", "bytes " + startPosition + '-' + endPosition + '/' + videoLength);

		// Set HTTP status to "206 Partial Content"
		if (startPosition > 0 || endPosition < videoLength) {
			meta.put("$statusCode", 206);
		}

		// Return response
		return rsp;
	};

	private void sendPacket(InputStream source, PacketStream destination, long sentBytes, long totalBytes) {
		try {

			// Send one packet
			long packetLength = totalBytes - sentBytes;
			if (packetLength < 1 || destination.isClosed()) {
				destination.sendClose();
				source.close();
				logger.info("All bytes transfered.");
				return;
			}
			if (packetLength > PACKET_SIZE) {
				packetLength = PACKET_SIZE;
			}
			byte[] packet = new byte[(int) packetLength];
			int count = source.read(packet);
			if (count < packetLength) {
				byte[] smaller = new byte[count];
				System.arraycopy(packet, 0, smaller, 0, count);
				packet = smaller;
			}
			destination.sendData(packet);
			long newSentBytes = sentBytes + count;

			long percent = newSentBytes * 100 / totalBytes;
			logger.info(percent + "% (" + newSentBytes + " of " + totalBytes + ") transfered.");

			// Send next packet...
			scheduler.schedule(() -> {
				sendPacket(source, destination, newSentBytes, totalBytes);
			}, PACKET_DELAY, TimeUnit.MILLISECONDS);

		} catch (Throwable cause) {

			// Mostly this is not an error (eg. browser socket has closed)
			logger.info("Transfer interrupted.");
			closeStreams(source, destination, cause);
		}
	}

	private final void closeStreams(InputStream source, PacketStream destination, Throwable cause) {
		if (!destination.isClosed() && !(cause instanceof MoleculerClientError)) {
			destination.sendError(cause);
		}
		if (source != null) {
			try {
				source.close();
			} catch (Exception ignored) {
			}
		}
	}

}