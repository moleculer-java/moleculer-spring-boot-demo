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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.lang.Math;

import org.springframework.stereotype.Controller;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

/**
 * Using a server-side template engine. URL of this sample (when
 * running the example on a local Netty server):<br>
 * <br>
 * http://localhost:3000/render<br>
 * <br>
 * Moleculer supports several template engine:
 * <ul>
 * <li>FreeMarker
 * <li>Jade for Java
 * <li>Mustache
 * <li>Pebble
 * <li>Thymeleaf
 * <li>DataTree Engine (this example demonstrates this)
 * <li>Your implementation (see "services.moleculer.web.template" package)
 * </ul>
 */
@Name("modelViewController")
@Controller
public class ModelViewController extends Service {

	// --- DATABASE EMULATION ---

	/**
	 * JSON structure what contains ALL of the rows.
	 */
	private Tree database = new Tree();

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Setup the time formatter
		DateFormat formatter = new SimpleDateFormat("HH:mm");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

		// Fill out "database"
		Tree rows = database.putList("rows");
		String[] ids = TimeZone.getAvailableIDs();
		Arrays.sort(ids, String.CASE_INSENSITIVE_ORDER);
		for (String id : ids) {
			TimeZone timeZone = TimeZone.getTimeZone(id);

			Tree row = rows.addMap();
			row.put("id", timeZone.getID());
			row.put("name", timeZone.getDisplayName());
			
			int offset = timeZone.getRawOffset();
			String formatted = formatter.format(new Date(Math.abs(offset)));
			if (offset < 0) {
				formatted = "-" + formatted;
			}
			row.put("offset", formatted);
		}
	}

	// --- RENDER PAGE ---

	/**
	 * Render HTML page. This function produces a JSON structure that contains
	 * one page of a "database" table. After the JSON structure has been
	 * created, the function tells APIGateway to convert JSON to HTML with the
	 * "$template" parameter.
	 */
	public Action render = ctx -> {

		// Log incoming data
		// logger.info("Incoming request:\r\n" + ctx.params);

		// Create page model (~=JSON)
		Tree model = new Tree();

		// Copy table rows from page offset (copy one page only)
		int rowsPerPage = ctx.params.get("rowsPerPage", 20);
		int index = ctx.params.get("index", 0) / rowsPerPage * rowsPerPage;
		Tree rows = database.get("rows");
		int maxIndex = rows.size() / rowsPerPage * rowsPerPage;
		if (ctx.params.get("firstButton") != null) {

			// First page
			index = 0;

		} else if (ctx.params.get("nextButton") != null) {

			// Next page...
			index += rowsPerPage;

		} else if (ctx.params.get("jump") != null) {

			// Jump to page
			int pageIndex = ctx.params.get("jump", 0) - 1;
			if (pageIndex > 0) {
				index = pageIndex * rowsPerPage;
			} else {
				index = 0;
			}
		} else if (ctx.params.get("prevButton") != null) {

			// Previous page...
			index -= rowsPerPage;

		} else if (ctx.params.get("lastButton") != null) {

			// Last page
			index = maxIndex;
		}

		// Verify offset
		if (index >= maxIndex) {
			index = maxIndex;
			model.put("nextDisabled", true);
		}
		if (index <= 0) {
			index = 0;
			model.put("prevDisabled", true);
		}

		// Copy page from offset
		Tree page = model.putList("page");
		for (int i = index; i < index + rowsPerPage; i++) {
			if (i >= rows.size()) {
				break;
			}
			Tree row = rows.get(i);
			page.addObject(row);
		}
		model.put("index", index);

		// Create direct links (to page 1, 2, 3, ...)
		int startIndex = index - (5 * rowsPerPage);
		if (startIndex < 0) {
			startIndex = 0;
		}
		int endIndex = startIndex + (11 * rowsPerPage);
		if (endIndex > maxIndex + 1) {
			int dif = (endIndex - maxIndex - 1) / rowsPerPage * rowsPerPage;
			endIndex = maxIndex + 1;
			startIndex -= dif;
			if (startIndex < 0) {
				startIndex = 0;
			}
		}
		Tree links = model.putList("links");
		int pageIdx = (startIndex / rowsPerPage) + 1;
		int currentPage = index / rowsPerPage;
		for (int i = startIndex; i < endIndex; i += rowsPerPage) {
			Tree link = links.addMap();
			link.put("page", pageIdx++);
			link.put("current", i / rowsPerPage == currentPage);
		}

		// Fill out the "rowsPerPage" combo
		Tree rowsPerPageList = model.putList("rowsPerPage");
		for (int i = 10; i <= 50; i += 10) {
			Tree value = rowsPerPageList.addMap();
			value.put("number", i);
			value.put("selected", rowsPerPage == i);
		}

		// IMPORTANT PART: This section instructs APIGateway not to give a JSON
		// response, but to convert JSON to HTML. The "$template" parameter
		// specifies the file name of the template (relative path to template).
		Tree meta = model.getMeta();
		meta.put("$template", "view");

		// Other special meta fields:
		// - $statusCode = Status code (eg. 200, 404) of the HTTP response message.
		// - $responseType = Content-Type header's value of the HTTP response message.
		// - $responseHeaders = Set of response headers.
		// - $location = Location in header for redirects.
		
		// Write the page mode into the log file
		// logger.info("Page model:\r\n" + model);

		// Return the "raw" data with template name
		return model;
	};

}