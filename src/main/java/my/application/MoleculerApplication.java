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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import io.datatree.Tree;
import io.datatree.templates.SimpleHtmlMinifier;
import my.commands.HelloCommand;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.config.SpringRegistrator;
import services.moleculer.jmx.JmxService;
import services.moleculer.jmx.ObjectWatcher;
import services.moleculer.repl.LocalRepl;
import services.moleculer.repl.RemoteRepl;
import services.moleculer.web.ApiGateway;
import services.moleculer.web.middleware.CorsHeaders;
import services.moleculer.web.middleware.ErrorPage;
import services.moleculer.web.middleware.Favicon;
import services.moleculer.web.middleware.Redirector;
import services.moleculer.web.middleware.ServeStatic;
import services.moleculer.web.netty.NettyServer;
import services.moleculer.web.router.MappingPolicy;
import services.moleculer.web.router.Route;
import services.moleculer.web.template.DataTreeEngine;
import services.moleculer.web.template.languages.DefaultMessageLoader;

/**
 * Sample Spring Boot Application. It can run on a Netty Server and as a J2EE
 * Servlet. The Services are loaded from the package specified in
 * the @ComponentScan annotation.
 */
@SpringBootApplication
@ComponentScan("my.services")
public class MoleculerApplication {

	// --- CONFIGURATION OF THE WEB APPLICATION ---

	/**
	 * The contents of the Environment are loaded from the main application
	 * configuration file (from the "application.yml").
	 */
	@Autowired
	private Environment env;

	// --- CREATE AND CONFIGURE SERVICE BROKER ---

	/**
	 * This section contains the most important settings. Creates the
	 * ServiceBroker, defines the HTTP Middlewares and assigns Aliases to REST
	 * services.
	 * 
	 * @return ServiceBroker instance
	 */
	@Bean(initMethod = "start", destroyMethod = "stop")
	public ServiceBroker getServiceBroker() {

		// Are we in DEVELOPMENT mode?
		boolean developmentMode = env.acceptsProfiles(Profiles.of("development"));

		// --- MAIN MOLECULER SETTINGS ---

		// Configure Service Broker
		ServiceBrokerConfig cfg = new ServiceBrokerConfig();

		// Define the underlaying JSON implementations (eg. "jackson", "boon",
		// "builtin", "gson", "fastjson", "genson", etc.)
		// See https://github.com/berkesa/datatree-adapters
		cfg.setJsonReaders("boon,jackson");
		cfg.setJsonWriters("jackson");

		// Define Transporter to connect other nodes
		// cfg.setTransporter(new RedisTransporter("redis://host"));

		// Supported servers:
		// - Redis Server
		// - Kafka Server
		// - NATS Server
		// - Servers with AMQP protocol (eg. RabbitMQ)
		// - TCP (serverless, direct connection between the nodes)

		// Create Service Broker and API Gateway
		ServiceBroker broker = new ServiceBroker(cfg);
		ApiGateway gateway = new ApiGateway();
		broker.createService("api-gw", gateway);

		gateway.setDebug(developmentMode);

		// Set default server-side template engine, it can be
		// - FreeMarker
		// - Jade for Java
		// - Mustache
		// - Handlebars
		// - Pebble
		// - Thymeleaf
		// - DataTree Engine
		// - Your implementation (see "services.moleculer.web.template" package)
		DataTreeEngine templateEngine = new DataTreeEngine();
		gateway.setTemplateEngine(templateEngine);
		if (!developmentMode) {

			// Minify in production mode
			templateEngine.getEngine().setTemplatePreProcessor(new SimpleHtmlMinifier());
		}

		// Enable template reloading in development mode
		templateEngine.setReloadable(developmentMode);
		templateEngine.setTemplatePath("/www");

		// Enable multilingualism and language file reloading in development
		// mode (language files can be in YAML or Java Properties format)
		templateEngine.setMessageLoader(new DefaultMessageLoader("languages/messages", "yml", developmentMode));

		// --- CONFIGURE ROUTES AND MIDDLEWARES ---

		// Create route for REST services
		Route restRoute = gateway.addRoute(new Route());

		// Add CORS headers to all REST responses
		restRoute.use(new CorsHeaders());

		// Custom error pages (for handle errors of Template Engine)
		restRoute.use(new ErrorPage());

		// Configure REST services. These services may be LOCAL or REMOTE. Even
		// NodeJS-based services can be provided via message broker (eg. NATS).
		// You can define Aliases by using Annotations (see in ChatService.java)
		restRoute.addAlias("api/hello/:name", "greeter.hello");
		restRoute.addAlias("api/add/:a/:b", "math.add");
		restRoute.addAlias("GET", "api/jmx", "jmxListener.getAll");
		restRoute.addAlias("POST", "api/drawing", "drawing.send");
		restRoute.addAlias("GET", "api/search/:query", "jmx.findObjects");

		// With this tricky solutions we cover the HTML pages with a Moleculer
		// Services
		restRoute.addAlias("ALL", "table.html", "tableService.render");
		restRoute.addAlias("ALL", "upload.html", "upload.receive");

		restRoute.addAlias("GET", "api/streamer", "mediaStreamer.getPacket");
		restRoute.addAlias("GET", "api/clock", "serverSideImage.getImage");

		// Create route for static content (HTML pages, images, CSS files)
		Route staticRoute = gateway.addRoute(new Route());
		staticRoute.setMappingPolicy(MappingPolicy.ALL);

		// Install middlewares (in REVERSED invocation order)
		ServeStatic serveStatic = new ServeStatic("/", "/www");
		serveStatic.setEnableReloading(developmentMode);

		// Last in the invocation chain is the static file handler
		staticRoute.use(serveStatic);

		// Second is the "favicon" handler
		staticRoute.use(new Favicon("/www/img/favicon.ico"));

		// First middleware redirects "/" path to "index.html"
		staticRoute.use(new Redirector("/", "index.html", 307));
		
		// --- CUSTOM BEFORE-CALL FUNCTION ---

		// Custom actions before/after the call (here you can copy headers,
		// session variables, remote IP address into the Meta block) - There is
		// also an "afterCall" function (for copy from JSON into the Response)
		gateway.setBeforeCall((currentRoute, req, rsp, data) -> {
			String path = req.getPath();
			if (path.startsWith("/api/upload")) {

				// Copy remote address into the "meta" structure
				Tree meta = data.getMeta();
				meta.put("address", req.getAddress());
				return;
			}
			if ("/api/streamer".equals(path)) {

				// Copy the "Range" header into the "meta" structure
				String range = req.getHeader("Range");
				if (range != null) {
					Tree meta = data.getMeta();
					meta.put("range", range);
				}
				return;
			}
		});

		// --- ADD JMX SERVICE ---

		// Create and install JMX service
		// (this example program uses JMX, but is not required)
		// See https://github.com/moleculer-java/moleculer-java-jmx
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

	/**
	 * See https://github.com/moleculer-java/moleculer-java-repl
	 * 
	 * @return local console instance
	 */
	@Bean
	@Profile("development")
	public LocalRepl getLocalConsole() {

		// Start local (standard input) console
		// (type "help" to list commands)
		LocalRepl repl = new LocalRepl();
		repl.addCommand(new HelloCommand());
		return repl;
	}

	/**
	 * See https://github.com/moleculer-java/moleculer-java-repl
	 * 
	 * @return telnet-based developer console instance
	 */
	@Bean
	@Profile("development")
	public RemoteRepl getRemoteConsole() {

		// Start remote (telnet-based) console
		// (mostly the Local Console is enough)
		RemoteRepl repl = new RemoteRepl();
		repl.setPort(env.getProperty("repl.port", Integer.class, 23));
		repl.addCommand(new HelloCommand());
		return repl;
	}

	// --- NETTY SERVER (RUNS ONLY IN STANDALONE MODE) ---

	/**
	 * Netty is used for standalone operation. This server is disabled in the
	 * web.xml. Netty and J2ee Servers/Servlet Containers provide the same
	 * capabilities for a Moleculer Application; WebSocket, SSL, REST, Template
	 * Engines, File Upload, Static File Serving, etc.
	 * 
	 * @return Netty server instance
	 */
	@Bean
	@ConditionalOnProperty(value = "netty.enabled", havingValue = "true", matchIfMissing = true)
	public NettyServer getNettyServer() {

		// Create Netty server
		NettyServer server = new NettyServer();
		server.setPort(env.getProperty("netty.port", Integer.class, 3000));

		// Set other webserver properties, eg:
		// server.setUseSSL(env.getProperty(...));
		// server.setKeyStoreFilePath("keytore.jks");

		return server;
	}

	// --- SPRING REGISTRATOR (REQUIRED) ---

	@Bean
	public SpringRegistrator getSpringRegistrator() {

		// The Registrator installs Spring Beans as Moleculer Services
		return new SpringRegistrator();
	}

}