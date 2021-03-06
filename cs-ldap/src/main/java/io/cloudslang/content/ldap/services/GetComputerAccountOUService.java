/*
 * (c) Copyright 2020 Micro Focus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0 which accompany this distribution.
 *
 * The Apache License is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cloudslang.content.ldap.services;

import io.cloudslang.content.ldap.entities.GetComputerAccountOUInput;
import io.cloudslang.content.ldap.utils.LDAPQuery;
import io.cloudslang.content.ldap.utils.MySSLSocketFactory;
import io.cloudslang.content.ldap.utils.ResultUtils;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Map;

import static io.cloudslang.content.constants.OutputNames.*;
import static io.cloudslang.content.ldap.constants.OutputNames.RESULT_OU_DN;
import static io.cloudslang.content.ldap.utils.ResultUtils.replaceInvalidXMLCharacters;

public class GetComputerAccountOUService {

    public Map<String, String> execute(GetComputerAccountOUInput input) {

        Map<String, String> results = ResultUtils.createNewEmptyMap();

        try {
            LDAPQuery ldap = new LDAPQuery();
            String compCN = input.getComputerCommonName();
            DirContext ctx;

            if (input.getUseSSL()) {
                if (Boolean.valueOf(input.getTrustAllRoots())) {
                    ctx = ldap.MakeDummySSLLDAPConnection(input.getHost(), input.getUsername(), input.getPassword());
                } else {
                    ctx = ldap.MakeSSLLDAPConnection(input.getHost(), input.getUsername(), input.getPassword(), "false",
                            input.getKeyStore(), input.getKeyStorePassword(), input.getTrustKeystore(), input.getTrustPassword());
                }

            } else {
                ctx = ldap.MakeLDAPConnection(input.getHost(), input.getUsername(), input.getPassword());
            }
//         Specify the ids of the attributes to return
            String[] attrIDs = {"ou"};

            SearchControls ctls = new SearchControls();
            ctls.setReturningAttributes(attrIDs);
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration<?> result = ctx.search(input.getRootDN(), "cn=" + compCN, ctls);
            if (result.hasMore()) {
                String ouDN = ((SearchResult) result.next()).getAttributes().get("ou").get(0).toString();
                String name = ouDN.substring(0, ouDN.indexOf(","));
                results.put(RETURN_RESULT, name);
                results.put(RESULT_OU_DN, ouDN);
                results.put(RETURN_CODE, "0");
            } else {
                results.put(RETURN_RESULT, "LDAP object doesn't exist");
                results.put(RETURN_CODE, "-1");
            }
            ctx.close();

        } catch (NamingException e) {
            Exception exception = MySSLSocketFactory.getException();
            if (exception == null)
                exception = e;
            results.put(EXCEPTION, String.valueOf(exception));
            results.put(RETURN_RESULT, replaceInvalidXMLCharacters(exception.getMessage()));
            results.put(RETURN_CODE, "-1");
        }
        return results;
    }
}

