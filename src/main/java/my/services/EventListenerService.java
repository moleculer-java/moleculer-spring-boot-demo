package my.services;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Controller;

import services.moleculer.eventbus.Group;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

/**
 * Simple event listener service.
 */
@Name("listener")
@Controller
public class EventListenerService extends Service {

	/**
	 * Global variable of the service instance.
	 */
	private AtomicLong counter = new AtomicLong();

	/**
	 * Sample action to get the counter's value. Sample client-side code:
	 * 
	 * <pre>
	 * broker.call("listener.getCounter").then(rsp -&gt; {
	 * 	logger.info(rsp.asLong());
	 * });
	 * </pre>
	 */
	public Action getCounter = ctx -> {
		return counter.get();
	};

	/**
	 * Set the counter on all nodes. Sample client-side code:
	 * 
	 * <pre>
	 * broker.broadcast("listener.setCounter", "delta", 3);
	 * </pre>
	 */
	@Subscribe("listener.setCounter")
	public Listener setCounter = data -> {
		long delta = data.get("delta", 0);
		logger.info("'SET COUNTER' event received. Received number: " + delta);
		counter.addAndGet(delta);
	};

	/**
	 * Reset the counter. Sample client-side code:
	 * 
	 * <pre>
	 * // On all nodes (and on all listeners):
	 * broker.broadcast("listener.reset");
	 * 
	 * // On all listeners in the "reset" group:
	 * broker.broadcast("listener.reset", Groups.of("reset"));
	 * 
	 * // On one (random) listener in all groups:
	 * broker.emit("listener.reset");
	 * 
	 * // On one (random) listener in the "reset" group:
	 * broker.emit("listener.reset", Groups.of("reset"));
	 * </pre>
	 */
	@Subscribe("listener.reset")
	@Group("reset")
	public Listener resetAll = data -> {
		logger.info("'RESET' event received.");
		counter.set(0);
	};

}