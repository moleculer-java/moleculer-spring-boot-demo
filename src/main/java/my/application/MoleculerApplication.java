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
		gateway.addRoute(restServices);
		StaticRoute staticContent = new StaticRoute("/static");
		gateway.addRoute(staticContent);

		// Configure REST services. These services may be LOCAL or REMOTE. Even
		// NodeJS-based services can be provided via message broker (eg. NATS).
		restServices.addAlias("hello/:name", "greeter.hello");
		restServices.addAlias("add/:a/:b", "math.add");
		restServices.addAlias("GET", "jmx/all", "jmxListener.getAll");
		restServices.addAlias("POST", "drawing", "drawing.send");
		restServices.addAlias("ALL", "render", "modelViewController.render");

		// Enable "chatService.sendMessage" and "chatService.getHistory"
		// services
		restServices.addToWhiteList("chatService.*");

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