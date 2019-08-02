package my.services;

import org.springframework.stereotype.Controller;

import io.datatree.Tree;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

/**
 * Distributed, websocket-based drawing board.<br>
 * Open the following URL in multiple browser windows:<br>
 * http://localhost:3000/drawing.html
 */
@Name("drawing")
@Controller
public class Drawing extends Service {

	/**
	 * Send drawing events to browsers.
	 */
	@Name("send")
	public Action send = ctx -> {
		
		// Create WebSocket packet
		Tree packet = new Tree();

		// Target "path" of the WebSocket message.
		// Browers listen this logical "path".
		// In Atmosphere terminology "path" is a name of a Broadcaster.
		packet.put("path", "/ws/drawing");
		
		// The incoming "params" structure is the outgoing "data"
		packet.putObject("data", ctx.params);

		// Send all ApiGateway of this clustered environment
		broker.broadcast("websocket.send", packet);
		
		// No response (just "200 Ok")
		return null;
	};

}