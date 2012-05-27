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
package org.esigate.http;

import java.net.MalformedURLException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.esigate.ResourceContext;
import org.esigate.UserContext;
import org.esigate.util.UriUtils;

public class RewriteUtils {

	private RewriteUtils() {
	}

	public final static String removeSessionId(String sessionId, String page) {
		String regexp = ";?jsessionid=" + Pattern.quote(sessionId);
		return page.replaceAll(regexp, "");
	}

	public final static String getSessionId(ResourceContext resourceContext) {
		UserContext userContext = resourceContext.getUserContext();
		String jsessionid = (userContext != null) ? userContext.getSessionId() : null;

		return jsessionid;
	}

	/**
	 * Translates an URL by replacing the beginning like in the example passed as parameters
	 * 
	 * @param sourceUrl
	 * @param sourceContext
	 * @param targetContext
	 * @return The translated URL
	 * @throws MalformedURLException
	 */
	public final static String translateUrl(String sourceUrl, String sourceContext, String targetContext) throws MalformedURLException {
		// Find what has been replaced at the beginning of sourceContext to transform it to targetContext
		String commonSuffix = StringUtils.reverse(StringUtils.getCommonPrefix(StringUtils.reverse(sourceContext), StringUtils.reverse(targetContext)));
		String sourcePrefix = StringUtils.removeEnd(sourceContext, commonSuffix);
		String targetPrefix = StringUtils.removeEnd(targetContext, commonSuffix);
		// Make the source url absolute
		String absoluteSourceUrl;
			absoluteSourceUrl = UriUtils.resolve(sourceContext, sourceUrl).toString();
		if (absoluteSourceUrl.startsWith(sourcePrefix))
			return targetPrefix + StringUtils.removeStart(absoluteSourceUrl, sourcePrefix);
		else
			return absoluteSourceUrl;
	}
}
