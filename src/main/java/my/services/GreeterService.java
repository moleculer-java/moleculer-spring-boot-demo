package my.services;

import org.springframework.stereotype.Controller;

import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

@Name("greeter")
@Controller
public class GreeterService extends Service {

	/**
	 * Simple action. Use the following code to call it:
	 * <pre>
	 * broker.call("greeter.hello")
	 *       .then(rsp -&gt; {
	 *         logger.info(rsp.asString());
	 * });
	 * </pre>
	 */
	public Action hello = ctx -> {
		return "Hello Moleculer";
	};

	/**
	 * Simple action. Sample client-side code:
	 * <pre>
	 * broker.call("greeter.welcome", "name", "Tom")
	 *       .then(rsp -&gt; {
	 *         logger.info(rsp.asString());
	 * });
	 * </pre>
	 */
	public Action welcome = ctx -> {
		return "Hello " + ctx.params.get("name", "Anonymous");
	};

}