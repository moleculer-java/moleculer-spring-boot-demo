package my.services;

import org.springframework.stereotype.Controller;

import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

/**
 * Simple Moleculer Service. This service is called by the
 * "my.commands.HelloCommand.java" (a custom, user-defined console command).
 */
@Name("greeter")
@Controller
public class Greeter extends Service {

	/**
	 * Simple action. Sample client-side code:
	 * 
	 * <pre>
	 * broker.call("greeter.hello", "name", "Tom").then(rsp -&gt; {
	 * 	logger.info(rsp.asString());
	 * });
	 * </pre>
	 */
	@Name("hello")	
	public Action helloAction = ctx -> {
		return "Hello " + ctx.params.get("name", "Anonymous");
	};

}