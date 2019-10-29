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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Controller;

import io.datatree.Tree;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.stream.PacketStream;
import services.moleculer.util.CheckedTree;
import services.moleculer.web.common.HttpConstants;

/**
 * File download based on Moleculer Streams. URL of this sample (when running
 * the example on a local Netty server):<br>
 * <br>
 * http://localhost:3000/download.html
 */
@Name("download")
@Controller
public class FileDownload extends Service {

	// --- RECEIVE UPLOADED IMAGE ---

	@Name("getFile")
	Action getFile = ctx -> {

		// Create temp file
		File file = generateRandomFile();

		// Create packet stream
		PacketStream stream = broker.createStream();

		// Set response headers
		Tree rsp = new CheckedTree(stream);
		Tree meta = rsp.getMeta();
		Tree headers = meta.putMap(HttpConstants.META_HEADERS);

		headers.put(HttpConstants.CONTENT_TYPE, "application/zip");
		headers.put(HttpConstants.CONTENT_LENGTH, file.length());
		headers.put(HttpConstants.CACHE_CONTROL, HttpConstants.NO_CACHE);
		headers.put("Content-Disposition", "attachment; filename=\"content.zip\"");

		// Start streaming
		stream.transferFrom(file).then(ok -> {

			// Finished
			logger.info("File transfer finished successfully.");

		}).catchError(err -> {

			// Failed
			logger.info("File transfer failed!", err);

		}).then(last -> {

			// ...then finally...
			if (file != null && file.delete()) {
				logger.info("Temporary file deleted.");
			}

		});

		// Return response
		return rsp;
	};

	private File generateRandomFile() throws IOException {
		File file = null;
		FileOutputStream fos = null;
		try {
			file = File.createTempFile("tmp", ".zip");
			fos = new FileOutputStream(file);
			ZipOutputStream zos = new ZipOutputStream(fos);
			ZipEntry entry = new ZipEntry("content.txt");
			zos.putNextEntry(entry);

			StringBuilder tmp = new StringBuilder();
			for (int rows = 0; rows < 100; rows++) {
				tmp.setLength(0);
				for (int cols = 0; cols < 20; cols++) {
					tmp.append(System.nanoTime());
					tmp.append(' ');
				}
				tmp.append("\r\n");
				zos.write(tmp.toString().getBytes(StandardCharsets.UTF_8));
			}

			zos.closeEntry();
			zos.finish();
			zos.close();
		} catch (IOException e) {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception ignored) {
				}
				fos = null;
			}
			if (file != null) {
				file.delete();
			}
			throw e;
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception ignored) {
				}
			}
		}
		return file;
	}

}