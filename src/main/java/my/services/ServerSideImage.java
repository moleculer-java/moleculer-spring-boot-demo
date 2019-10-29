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

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalTime;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Controller;

import io.datatree.Tree;
import services.moleculer.service.Action;
import services.moleculer.service.Service;
import services.moleculer.stream.PacketStream;
import services.moleculer.util.CheckedTree;
import services.moleculer.web.common.HttpConstants;

/**
 * An example of how to generate an image on the server. This sample displays a
 * clock. URL of this sample (when running the example on a local Netty server):
 * <br>
 * <br>
 * http://localhost:3000/clock.html
 */
@Controller
public class ServerSideImage extends Service {

	// --- CONSTANTS ---

	private static final float DEGREES06 = (float) (PI / 30);
	private static final float DEGREES30 = DEGREES06 * 5;
	private static final float DEGREES90 = DEGREES30 * 3;

	private static final int SIZE = 590;
	private static final int SPACING = 40;
	private static final int DIAMETER = SIZE - 2 * SPACING;
	private static final int CX = DIAMETER / 2 + SPACING;
	private static final int CY = DIAMETER / 2 + SPACING;

	// --- PRIVATE PAINT METHODS ---

	Action getImage = ctx -> {

		// Generate PNG image
		Graphics2D g = null;
		byte[] bytes = null;
		try {
			BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
			g = (Graphics2D) image.getGraphics();
			paintClock(g);
			ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
			ImageIO.write(image, "png", output);
			bytes = output.toByteArray();
		} finally {
			if (g != null) {
				g.dispose();
			}
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

	// --- PRIVATE PAINT METHODS ---

	private void paintClock(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		drawFace(g);

		final LocalTime time = LocalTime.now();
		int hour = time.getHour();
		int minute = time.getMinute();
		int second = time.getSecond();

		float angle = DEGREES90 - (DEGREES06 * second);
		drawHand(g, angle, DIAMETER / 2 - 30, Color.red);

		float minsecs = (minute + second / 60.0F);
		angle = DEGREES90 - (DEGREES06 * minsecs);
		drawHand(g, angle, DIAMETER / 3 + 10, Color.black);

		float hourmins = (hour + minsecs / 60.0F);
		angle = DEGREES90 - (DEGREES30 * hourmins);
		drawHand(g, angle, DIAMETER / 4 + 10, Color.black);
	}

	private void drawFace(Graphics2D g) {
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(0, 0, SIZE, SIZE);
		g.setStroke(new BasicStroke(2));
		g.setColor(Color.WHITE);
		g.fillOval(SPACING, SPACING, DIAMETER, DIAMETER);
		g.setColor(Color.BLACK);
		g.drawOval(SPACING, SPACING, DIAMETER, DIAMETER);
	}

	private void drawHand(Graphics2D g, float angle, int radius, Color color) {
		int x = CX + (int) (radius * cos(angle));
		int y = CY - (int) (radius * sin(angle));
		g.setColor(color);
		g.drawLine(CX, CY, x, y);
	}

}