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

import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Controller;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

/**
 * This Moleculer Service demonstrates how to easily create scheduled tasks. URL
 * of this sample (when running the example on a local Netty server):<br>
 * <br>
 * http://localhost:3000/jmx.html
 */
@Name("scheduledService")
@Controller
public class ScheduledService extends Service {

	// --- VARIABLES ---

	/**
	 * Cancelable timer. The schedule is interrupted by MessageBroker using this
	 * object.
	 */
	private ScheduledFuture<?> timer;

	// --- START MESSAGE BROKER / SCHEDULE TASK ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Start timer
		ScheduledExecutorService scheduler = broker.getConfig().getScheduler();
		timer = scheduler.scheduleAtFixedRate(this::scheduledMethod, 3, 3, TimeUnit.SECONDS);

		// Log
		logger.info("Scheduled Task started successfully.");
	}

	// --- THE SCHEDULED TASK ---

	/**
	 * This method runs every second. Sends a message to all connected browsers.
	 * The content of the message is the time on this server.
	 */
	private void scheduledMethod() {

		// Create WebSocket packet
		Tree packet = new Tree();

		// Target "path" of the WebSocket message.
		// Browers listen this logical "path".
		// In Atmosphere terminology "path" is a name of a Broadcaster.
		packet.put("path", "ws/jmx");

		// Payload of WebSocket message
		// (the content of "data" can be arbitrary)
		Tree data = packet.putMap("data");
		data.put("type", "date");
		data.put("data", new Date());

		// Send all ApiGateway of this clustered environment
		broker.broadcast("websocket.send", packet);
	}

	// --- STOP MESSAGE BROKER / CANCEL TASK ---

	@Override
	public void stopped() {
		if (timer != null) {
			timer.cancel(false);
			timer = null;

			// Log
			logger.info("Scheduled Task stopped.");
		}
	}

}