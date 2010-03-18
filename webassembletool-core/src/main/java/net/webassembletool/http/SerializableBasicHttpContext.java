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
package net.webassembletool.http;

import java.io.Serializable;

import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/**
 * Serializable implementation of  {@link HttpContext}.
 * 
 * @author Nicolas Richeton 
 */
public class SerializableBasicHttpContext  extends BasicHttpContext implements Serializable{

	/**
	 * Serial Id
	 */
	private static final long serialVersionUID = -5003819783854207519L;

}
