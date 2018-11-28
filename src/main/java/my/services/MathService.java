package my.services;

import org.springframework.stereotype.Controller;

import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

@Name("math")
@Controller
public class MathService extends Service {

	/**
	 * Simple action. Use the following code to call it:
	 * <pre>
	 * broker.call("math.add", "a", 3, "b", 5)
	 *       .then(rsp -&gt; {
	 *         logger.info(rsp.asInteger());
	 * });
	 * </pre>
	 */
	public Action add = ctx -> {
		return ctx.params.get("a", 0) + ctx.params.get("b", 0);
	};

}