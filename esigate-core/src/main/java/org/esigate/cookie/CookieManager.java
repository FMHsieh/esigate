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

package org.esigate.cookie;

import java.util.Date;
import java.util.List;

import org.apache.http.HttpRequest;
import org.apache.http.cookie.Cookie;
import org.esigate.extension.Extension;

public interface CookieManager extends Extension {

	public void addCookie(Cookie cookie, HttpRequest resourceContext);

	public List<Cookie> getCookies(HttpRequest resourceContext);

	public boolean clearExpired(Date date, HttpRequest resourceContext);

	public void clear(HttpRequest resourceContext);

}
