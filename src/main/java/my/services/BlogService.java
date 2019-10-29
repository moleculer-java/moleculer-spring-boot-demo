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

import java.util.Random;

import org.springframework.stereotype.Controller;

import io.datatree.Tree;
import services.moleculer.cacher.Cacher;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

/**
 * Using a server-side template engine, with "Top Level Cache" middleware. URL
 * of this sample (when running the example on a local Netty server):<br>
 * <br>
 * http://localhost:3000/blog/0<br>
 * <br>
 * Moleculer supports several template engine:
 * <ul>
 * <li>FreeMarker
 * <li>Jade for Java
 * <li>Mustache
 * <li>Handlebars
 * <li>Pebble
 * <li>Thymeleaf
 * <li>DataTree Engine (this example demonstrates this)
 * <li>Your implementation (see "services.moleculer.web.template" package)
 */
@Name("blog")
@Controller
public class BlogService extends Service {

	// --- RENDER PAGE ---

	/**
	 * Render HTML page. This function produces a JSON structure that contains
	 * one page of a Blog. After the JSON structure has been created, the
	 * function tells APIGateway to convert JSON to HTML with the "$template"
	 * parameter.
	 */
	Action render = ctx -> {

		// Create page model (~=JSON)
		Tree model = new Tree();

		// Get pageID
		int pageID = ctx.params.get("id", 0);
		logger.info("Generating content for page #" + pageID + " (response is cached by the Top-level Cache)...");

		// Set title
		model.put("page", "Page-" + pageID);

		// Generate random blog content
		Random rnd = new Random();
		int max = rnd.nextInt(5) + 7;

		Tree posts = model.putList("posts");
		for (int i = 0; i < max; i++) {
			Tree entry = posts.addMap();
			entry.put("title", generateRandomSentences(rnd, rnd.nextInt(5) + 5));
			entry.put("content", generateRandomSentences(rnd, rnd.nextInt(170) + 30));
		}

		// Page IDs
		Tree links = model.putList("links");
		for (int i = 0; i < 20; i++) {
			links.add(i);
		}

		// IMPORTANT PART: This section instructs APIGateway not to give a JSON
		// response, but to convert JSON to HTML. The "$template" parameter
		// specifies the file name of the template (relative path to template).
		Tree meta = model.getMeta();
		meta.put("$template", "blog");

		// Return the "raw" data with template name
		return model;
	};

	Action clear = ctx -> {

		// Clear cache
		Cacher cacher = broker.getConfig().getCacher();
		return cacher.clean("toplevel.**");
	};

	private String generateRandomSentences(Random rnd, int words) {
		StringBuilder tmp = new StringBuilder(1024);
		boolean startSentence = true;
		boolean upper = false;
		for (int i = 0; i < words; i++) {
			if (i % 10 == 9) {
				startSentence = true;
				tmp.setLength(tmp.length() - 1);
				switch (rnd.nextInt(4)) {
				case 0:
					tmp.append("? ");
					break;
				case 1:
					tmp.append("! ");
					break;
				default:
					tmp.append(". ");
					break;
				}
			}
			upper = false;
			if (startSentence) {
				upper = true;
				startSentence = false;
			}
			generateRandomWord(tmp, rnd, rnd.nextInt(5) + 3, upper);
			tmp.append(' ');
		}
		tmp.setLength(tmp.length() - 1);
		if (!upper) {
			switch (rnd.nextInt(4)) {
			case 0:
				tmp.append('?');
				break;
			case 1:
				tmp.append('!');
				break;
			default:
				tmp.append('.');
				break;
			}
		}
		return tmp.toString();
	}

	private void generateRandomWord(StringBuilder tmp, Random rnd, int len, boolean upper) {
		char c;
		for (int i = 0; i < len; i++) {
			c = (char) (rnd.nextInt(26) + 'a');
			if (i == 0 && upper) {
				c = Character.toUpperCase(c);
			}
			tmp.append(c);
		}
	}

}