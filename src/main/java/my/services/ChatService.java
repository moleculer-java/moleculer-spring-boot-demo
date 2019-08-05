/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package my.services;

import org.springframework.stereotype.Controller;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Action;
import services.moleculer.service.Service;

/**
 * Simple chat service with history. URL of this sample (when
 * running the example on a local Netty server):<br>
 * <br>
 * http://localhost:3000/chat.html
 */
@Controller
public class ChatService extends Service {

	/**
	 * JSON object / message container of the history. Sample message history:
	 * 
	 * <pre>
	 * {
	 *   "history": [
	 *     {"id": "123", "message": "msg1"},
	 *     {"id": "456", "message": "msg2"},
	 *     {"id": "789", "message": "msg3"},
	 *     {"id": "123", "message": "msg4"}
	 *   ]
	 * }
	 * </pre>
	 * 
	 * ...where "id" is a client/sender ID, and the "message" is the text of the
	 * message.
	 */
	private Tree root = new Tree();

	/**
	 * History array( sub-structure of the "history" object.
	 */
	private Tree history;

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Init history array
		history = root.putList("history");
	}

	/**
	 * Send message to other users.
	 */
	public Action sendMessage = ctx -> {

		// Get and verify incoming paramters
		String id = ctx.params.get("id", "");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("Missing \"id\" parameter!");
		}
		String text = ctx.params.get("text", "");
		if (text == null || text.isEmpty()) {
			throw new IllegalArgumentException("Missing \"text\" parameter!");
		}

		// Crete chat message
		Tree message = new Tree();
		message.put("id", id);
		message.put("text", text);

		// Add to history
		synchronized (root) {
			history.addObject(message);
			while (history.size() > 10) {
				history.removeFirst();
			}
		}

		// Create WebSocket packet
		Tree packet = new Tree();

		// Target "path" of the WebSocket message.
		// Browers listen this logical "path".
		// In Atmosphere terminology "path" is a name of a Broadcaster.
		packet.put("path", "/ws/chat");

		// The outgoing "data" structure (payload of the websocket message)
		packet.putObject("data", message);

		// Send all ApiGateway of this clustered environment
		broker.broadcast("websocket.send", packet);

		// No response (just "200 Ok")
		return null;
	};

	public Action getHistory = ctx -> {

		// Create (deep) copy from history
		Tree copyOfRoot;
		synchronized (root) {
			copyOfRoot = root.clone();
		}
		return copyOfRoot;
	};

}