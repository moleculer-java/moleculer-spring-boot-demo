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

import java.util.Date;

import org.springframework.stereotype.Controller;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

/**
 * Two event listener Service, that monitor events published by the JMXService.
 * The Event Listeners send the received data through WebSocket protocol to the
 * browsers. URL of this sample (when running the example on a local Netty
 * server):<br>
 * <br>
 * http://localhost:3000/jmx.html
 */
@Name("jmxListener")
@Controller
public class JmxListener extends Service {

	// --- FIRST EVENT LISTENER ---

	/**
	 * This event is sent by JMXService's ObjectWatcher thread.
	 */
	@Subscribe("java.classloader")
	Listener classloaderListener = ctx -> {

		// Write to log
		// logger.info("Number of loaded classes: "
		// + in.get("LoadedClassCount", 0)
		// + ", number of total loaded classes: "
		// + in.get("TotalLoadedClassCount", 0)
		// + ", number of unloaded classes: "
		// + in.get("UnloadedClassCount", 0) + ".");

		// Create WebSocket packet
		Tree packet = new Tree();

		// Target "path" of the WebSocket message.
		// Browers listen this logical "path".
		// In Atmosphere terminology "path" is a name of a Broadcaster.
		packet.put("path", "ws/jmx");

		// Payload of WebSocket message
		Tree data = packet.putMap("data");
		data.put("type", "classloader");
		data.copyFrom(ctx.params);

		// Send all ApiGateway of this clustered environment
		broker.broadcast("websocket.send", packet);
	};

	// --- SECOND EVENT LISTENER ---

	/**
	 * This event is sent by JMXService's ObjectWatcher thread.
	 */
	@Subscribe("java.memory")
	Listener memoryListener = ctx -> {

		// Incoming "structure" is a simple number
		long memoryUsage = ctx.params.asLong();

		// Write to log
		// logger.info("Memory usage: " + memoryUsage + " bytes");

		// Create WebSocket packet
		Tree packet = new Tree();

		// Target "path" of the WebSocket message.
		// Browers listen this logical "path".
		// In Atmosphere terminology "path" is a name of a Broadcaster.
		packet.put("path", "ws/jmx");

		// Payload of WebSocket message
		// (the content of "data" can be arbitrary)
		Tree data = packet.putMap("data");
		data.put("type", "memory");
		data.put("data", memoryUsage);

		// Send all ApiGateway of this clustered environment
		broker.broadcast("websocket.send", packet);
	};

	// --- REST SERVICE TO GET THE INITIAL VALUES ---

	@Name("getAll")
	Action getAll = ctx -> {

		// First invocation (returns a Promise)
		Tree req1 = new Tree();
		req1.put("objectName", "java.lang:type=Memory");
		req1.put("attributeName", "HeapMemoryUsage");
		req1.put("path", "used");

		Promise promise1 = ctx.call("jmx.getObject", req1);

		// Second invocation (returns a Promise)
		Tree req2 = new Tree();
		req2.put("objectName", "java.lang:type=ClassLoading");

		Promise promise2 = ctx.call("jmx.getObject", req2);

		// Parallel invocation (returns a merged array)
		return Promise.all(promise1, promise2).then(array -> {
			array.addMap().put("data", new Date());
			return array;
		});
	};

}
