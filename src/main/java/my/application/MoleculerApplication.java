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
package my.application;

import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import io.datatree.Tree;
import io.datatree.templates.SimpleHtmlMinifier;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.config.SpringRegistrator;
import services.moleculer.jmx.JmxService;
import services.moleculer.jmx.ObjectWatcher;
import services.moleculer.repl.LocalRepl;
import services.moleculer.repl.RemoteRepl;
import services.moleculer.web.ApiGateway;
import services.moleculer.web.middleware.CorsHeaders;
import services.moleculer.web.netty.NettyServer;
import services.moleculer.web.router.RestRoute;
import services.moleculer.web.router.StaticRoute;
import services.moleculer.web.template.DataTreeEngine;

@SpringBootApplication
@ComponentScan("my.services")
public class MoleculerApplication {

	// --- CONFIGURATION OF THE WEB APPLICATION ---

	@Autowired
	private Environment env;

	// --- CREATE AND CONFIGURE SERVICE BROKER ---

	@Bean(initMethod = "start", destroyMethod = "stop")
	public ServiceBroker getServiceBroker() {

		// Are we in DEVELOPMENT mode?
		boolean developmentMode = env.acceptsProfiles(Profiles.of("development"));

		// --- MAIN MOLECULER SETTINGS ---

		// Configure Service Broker
		ServiceBrokerConfig cfg = new ServiceBrokerConfig();
		cfg.setJsonReaders("boon,jackson");
		cfg.setJsonWriters("jackson");

		// Create Service Broker and API Gateway
		ServiceBroker broker = new ServiceBroker(cfg);
		ApiGateway gateway = new ApiGateway();
		gateway.setDebug(developmentMode);

		// Set default server-side template engine, it can be
		// - FreeMarker
		// - Jade for Java
		// - Mustache
		// - Pebble
		// - Thymeleaf
		// - DataTree Engine
		// - Your implementation (see "services.moleculer.web.template" package)
		DataTreeEngine renderer = new DataTreeEngine();
		if (!developmentMode) {

			// Minify in production mode
			renderer.getEngine().setTemplatePreProcessor(new SimpleHtmlMinifier());
		}

		// Enable template reloading in development mode
		renderer.setReloadable(developmentMode);
		renderer.setTemplatePath("/static");
		gateway.setTemplateEngine(renderer);
		
		// --- CONFIGURE WEB ROUTES ---

		// Create routes for REST services and static content
		RestRoute restServices = new RestRoute();
		StaticRoute staticContent = new StaticRoute("/static");

		// Custom actions before/after the call
		gateway.setBeforeCall((route, req, rsp, data) -> {
			if (route == restServices) {
				String path = req.getPath();
				if (path.startsWith("/upload")) {
					
					// Copy remote address into the "meta" structure
					Tree meta = data.getMeta();
					meta.put("address", req.getAddress());
					return;
				}
				if ("/streamer".equals(path)) {
					
					// Copy the "Range" header into the "meta" structure
					String range = req.getHeader("Range");
					if (range != null) {
						Tree meta = data.getMeta();
						meta.put("range", range);
					}
					return;					
				}
			}
		});

		gateway.addRoute(restServices);
		gateway.addRoute(staticContent);

		// Configure REST services. These services may be LOCAL or REMOTE. Even
		// NodeJS-based services can be provided via message broker (eg. NATS).
		
		restServices.addAlias("hello/:name", "greeter.hello");
		restServices.addAlias("add/:a/:b", "math.add");
		restServices.addAlias("GET", "jmx/all", "jmxListener.getAll");
		restServices.addAlias("POST", "drawing", "drawing.send");
		restServices.addAlias("ALL", "render", "modelViewController.render");

		restServices.addAlias("POST", "upload", "upload.receive");
		restServices.addAlias("GET", "thumbnail", "upload.getThumbnail");
		restServices.addAlias("GET", "uploadCount", "upload.getUploadCount");
		
		restServices.addAlias("GET", "streamer", "mediaStreamer.getPacket");
		restServices.addAlias("GET", "clock", "serverSideImage.getImage");
		
		// Enable "chatService.sendMessage" and "chatService.getHistory"
		// services
		restServices.addToWhiteList("chatService.*");

		// Add CORS headers to REST responses
		restServices.use(new CorsHeaders());
				
		// Configure static web content in server-independent way
		staticContent.setFaviconPath("favicon.ico");

		// Enable file reloading in development mode
		staticContent.setEnableReloading(developmentMode);

		// Install APIGateway Moleculer Service
		broker.createService("api-gw", gateway);

		// --- CONFIGURE CUSTOM SERVICES ---

		// Create and install JMX service (optional)
		JmxService jmx = new JmxService();
		broker.createService(jmx);

		// First watched property: used memory, checked every 5 sec
		// Event's name: "java.memory", the event's payload will be a number -->
		ObjectWatcher watcher1 = new ObjectWatcher();
		watcher1.setObjectName("java.lang:type=Memory");
		watcher1.setAttributeName("HeapMemoryUsage");
		watcher1.setPath("used");
		watcher1.setEvent("java.memory");
		watcher1.setBroadcast(true);

		// Second watched object: classloader's status, checked every 5 sec
		// Event's name: "java.classloader", the payload will be a JSON
		// structure
		ObjectWatcher watcher2 = new ObjectWatcher();
		watcher2.setObjectName("java.lang:type=ClassLoading");
		watcher2.setEvent("java.classloader");

		// Install JMX ObjectWatchers
		HashSet<ObjectWatcher> watchers = new HashSet<>();
		watchers.add(watcher1);
		watchers.add(watcher2);
		jmx.setObjectWatchers(watchers);

		// End of configuration
		return broker;
	}

	// --- INTERACTIVE DEVELOPER CONSOLES ---

	@Bean
	@Profile("development")
	public LocalRepl getLocalConsole() {

		// Start local (standard input) console
		LocalRepl repl = new LocalRepl();
		repl.setPackagesToScan("my.commands");
		return repl;
	}

	@Bean
	@Profile("development")
	public RemoteRepl getRemoteConsole() {

		// Start remote (telnet-based) console
		RemoteRepl repl = new RemoteRepl();
		repl.setPackagesToScan("my.commands");
		repl.setPort(env.getProperty("repl.port", Integer.class, 23));
		return repl;
	}

	// --- NETTY SERVER (IN STANDALONE MODE) ---

	@Bean
	@ConditionalOnNotWebApplication
	public NettyServer getNettyServer() {

		// It's not required when runs under a J2EE server
		NettyServer server = new NettyServer();
		server.setPort(env.getProperty("netty.port", Integer.class, 3000));

		// Set other webserver properties, eg:
		// server.setUseSSL(env.getProperty(...));

		return server;
	}

	// --- SPRING REGISTRATOR (REQUIRED) ---

	@Bean
	public SpringRegistrator getSpringRegistrator() {

		// The Registrator installs Spring Beans as Moleculer Services
		return new SpringRegistrator();
	}

}