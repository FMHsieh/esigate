/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.esigate.wicket.container;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Response;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.esigate.Driver;
import org.esigate.DriverFactory;
import org.esigate.wicket.utils.WATNullResponse;
import org.esigate.wicket.utils.WATWicketConfiguration;

/**
 * A container for a template parameter. It encloses a block which will be
 * inserted into the template.
 * 
 * <p>
 * Usage :
 * </p>
 * 
 * <p>
 * Page :
 * </p>
 * <code>
 * WATTemplate template = new WATTemplate( "template" );
 * add( template);
 * </code>
 * 
 * <p>
 * HTML :
 * </p>
 * <code>
 * <div wicket:id='template'>
 *  content here is ignored
 *     <div wicket:id='block1'>
 *         Block content using WATParam. Can be pure html or other wicket components.
 *     </div>
 *  </div>
 *  </code>
 * 
 * @author Nicolas Richeton
 * @see WATParam
 * 
 */
public abstract class AbstractWatDriverContainer extends WebMarkupContainer {
	private static final long serialVersionUID = 1L;

	public static final String WAT_ERROR_PREFIX = "org.esigate.error";

	private final Map<String, String> params = new HashMap<String, String>();
	private String provider = null;
	private final Map<String, String> replaceRules = new HashMap<String, String>();

	/**
	 * Create a template block
	 * 
	 * @param id
	 */
	public AbstractWatDriverContainer(String id) {
		super(id);
	}

	/**
	 * Add replace rule.
	 * 
	 * @param regexp
	 *            The regex to match in the template
	 * @param replacement
	 *            The content wich will replace the matched value.
	 */
	public void addReplaceRule(String regexp, String replacement) {
		replaceRules.put(regexp, replacement);
	}

	/**
	 * Get current driver, according to provider.
	 * 
	 * @see AbstractWatDriverContainer#setProvider(String)
	 * @return current WAT Driver
	 */
	protected Driver getDriver() {
		if (provider == null) {
			return DriverFactory.getInstance();
		} else {
			return DriverFactory.getInstance(provider);
		}
	}

	public String getProvider() {
		return provider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.wicket.MarkupContainer#onComponentTagBody(org.apache.wicket
	 * .markup.MarkupStream, org.apache.wicket.markup.ComponentTag)
	 */
	@Override
	protected void onComponentTagBody(MarkupStream markupStream,
			ComponentTag openTag) {

		// For unit tests, WAT can be disabled. This component will then behave
		// like a standard MarkupContainer.
		if (WATWicketConfiguration.isDisableHttpRequests()) {
			super.onComponentTagBody(markupStream, openTag);
			return;
		}

		Response originalResponse = getRequestCycle().getResponse();
		WATNullResponse watResponse = new WATNullResponse();
		getRequestCycle().setResponse(watResponse);
		super.onComponentTagBody(markupStream, openTag);
		getRequestCycle().setResponse(originalResponse);

		process(watResponse.getBlocks(), params, replaceRules);

	}

	/**
	 * This method is called when the component is rendered. This is the place
	 * to do the specific processing and call Driver.
	 * 
	 * @param blocks
	 * @param params
	 * @param replaceRules
	 */
	public abstract void process(Map<String, String> blocks,
			Map<String, String> params, Map<String, String> replaceRules);

	/**
	 * Write error content according to the error.
	 * 
	 * @param blocks
	 * @param response
	 * @param httpStatusCode
	 */
	protected void sendErrorContent(Map<String, String> blocks,
			Response response, Integer httpStatusCode) {

		String errorContent = blocks.get(WAT_ERROR_PREFIX + httpStatusCode);
		if (errorContent == null) {
			errorContent = blocks.get(WAT_ERROR_PREFIX);
		}
		if (errorContent != null) {
			response.write(errorContent);
		}
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

}
