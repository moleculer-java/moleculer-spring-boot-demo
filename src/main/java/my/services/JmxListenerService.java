package my.services;

import org.springframework.stereotype.Controller;

import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

/**
 * Event listener, which monitors events published by the JMXService (this
 * service is installed by the "moleculer.config.xml", at the following section:
 * &lt;import resource="jmx/local.xml"&gt;).
 */
@Name("jmxListener")
@Controller
public class JmxListenerService extends Service {

	/**
	 * This event is sent by JMXService's ObjectWatcher thread (see the
	 * configuration in the "cfg/jmx/local.xml" file):
	 */
	@Subscribe("java.classloader")
	public Listener setCounter = data -> {

		// Write to log
		logger.info("Number of loaded classes: " + data.get("LoadedClassCount", 0)
				+ ", number of total loaded classes: " + data.get("TotalLoadedClassCount", 0)
				+ ", number of unloaded classes: " + data.get("UnloadedClassCount", 0) + ".");
	};

}
