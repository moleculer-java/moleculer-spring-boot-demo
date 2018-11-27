package my.services;

import org.springframework.stereotype.Controller;

import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

@Name("math")
@Controller
public class MathService extends Service {

	// Simple service
	public Action add = ctx -> {
		return ctx.params.get("a", 0) + ctx.params.get("b", 0);
	};

}