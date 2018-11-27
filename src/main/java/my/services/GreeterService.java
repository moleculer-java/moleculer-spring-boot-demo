package my.services;

import org.springframework.stereotype.Controller;

import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

@Name("greeter")
@Controller
public class GreeterService extends Service {

	public Action hello = ctx -> {
		return "Hello Moleculer";
	};

	public Action welcome = ctx -> {
		return "Hello " + ctx.params.get("name", "Anonymous");
	};

}