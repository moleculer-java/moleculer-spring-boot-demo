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
 * This Moleculer Service demonstrates how to easily create scheduled tasks.
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
		packet.put("path", "/ws/jmx");

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