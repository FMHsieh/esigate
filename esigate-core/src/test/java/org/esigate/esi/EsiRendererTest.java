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

package org.esigate.esi;

import junit.framework.TestCase;

import org.apache.commons.io.output.StringBuilderWriter;
import org.esigate.Driver;
import org.esigate.MockRequestExecutor;
import org.esigate.impl.DriverRequest;
import org.esigate.test.TestUtils;

public class EsiRendererTest extends TestCase {
    private DriverRequest request;
    private EsiRenderer tested;

    @Override
    protected void setUp() throws Exception {
        Driver provider = MockRequestExecutor.createDriver();
        request = TestUtils.createRequest(provider);
        tested = new EsiRenderer();
    }

    public void testFragmentTagsShouldBeRemoved() throws Exception {
        String page = "begin <esi:fragment name=\"test\">content</esi:fragment> end";
        StringBuilderWriter out = new StringBuilderWriter();
        tested.render(request, page, out);
        assertEquals("begin content end", out.toString());
    }
}
