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
package my.commands;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import services.moleculer.ServiceBroker;
import services.moleculer.repl.Command;
import services.moleculer.service.Name;

/**
 * REPL command to call the "greeter.hello" service.
 */
@Name("hello")
public class HelloCommand extends Command {

	public HelloCommand() {
		option("uppercase, -u", "uppercase the name");
	}
	
	@Override
	public String getDescription() {
		return "Call the greeter.welcome service with name";
	}

	@Override
	public String getUsage() {
		return "hello [options] <name>";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		
		// One parameter (the "name") is required
		return 1;
	}

	@Override
	public void onCommand(ServiceBroker broker, PrintWriter out, String[] parameters) throws Exception {
		
		// Parse parameters
		List<String> params = Arrays.asList(parameters);
		boolean uppercase = params.contains("--uppercase") || params.contains("-u");
		
		// Last parameter is the name
		String name = parameters[parameters.length - 1];
		if (uppercase) {
			name = name.toUpperCase();
		}
		
		// Call the "greeter.hello" service
		broker.call("greeter.hello", "name", name).then(rsp -> {
			
			// Print response
			out.println(rsp.asString());
			
		}).catchError(err -> {
			
			// Print error
			err.printStackTrace(out);
			
		});
	}

}
