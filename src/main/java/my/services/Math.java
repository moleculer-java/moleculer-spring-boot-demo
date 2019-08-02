package my.services;

import org.springframework.stereotype.Controller;

import io.datatree.Tree;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

@Name("math")
@Controller
public class Math extends Service {

	/**
	 * Simple action. Use the following code to call it:
	 * <pre>
	 * broker.call("math.add", "a", 3, "b", 5)
	 *       .then(rsp -&gt; {
	 *         logger.info(rsp.get("c", 0));
	 * });
	 * </pre>
	 */
	@Name("add")
	public Action add = ctx -> {
		int a = ctx.params.get("a", 0);
		int b = ctx.params.get("b", 0);
		int c = a + b;
		return new Tree().put("a", a).put("b", b).put("c", c);
	};

}