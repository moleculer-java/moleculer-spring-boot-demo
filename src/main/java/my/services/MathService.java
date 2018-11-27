package my.services;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

@Name("math")
public class MathService extends Service {

	private Random rand = new Random();

	private ScheduledExecutorService scheduler;
	
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		
		scheduler = broker.getConfig().getScheduler();
	}

	public Action add = ctx -> {
		return new Promise(resolver -> {

			Tree res = new Tree();
			res.put("count", ctx.params.get("count", 0));
			res.put("res", ctx.params.get("a", 0) + ctx.params.get("b", 0));
			
			scheduler.schedule(() -> {
				resolver.resolve(res);	
			}, rand.nextInt(1000) + 500, TimeUnit.MILLISECONDS);
			
		});
		
	};
	
	@Subscribe("echo.event")
	public Listener listener = data -> {
		logger.info("MATH: Echo event received. Counter: " + data.get("counter", -1) + ". Send reply...");
		broker.emit("reply.event", data);
	};
}
