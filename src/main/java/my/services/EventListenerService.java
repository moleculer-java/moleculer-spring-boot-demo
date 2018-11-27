package my.services;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Controller;

import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

@Name("listener")
@Controller
public class EventListenerService extends Service {

	// Global variable of the service instance
	private AtomicLong counter = new AtomicLong();

	// Sample action
	public Action getCounter = ctx -> {
		return counter.get();
	};

	// Sample event listener
	@Subscribe("listener.setCounter")
	public Listener listener = data -> {
		long delta = data.get("delta", 0);
		logger.info("event received. Received number: " + delta);
		counter.addAndGet(delta);
	};

}
