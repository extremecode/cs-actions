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
package io.cloudslang.content.ldap.actions;

import com.hp.oo.sdk.content.annotations.Action;
import com.hp.oo.sdk.content.annotations.Output;
import com.hp.oo.sdk.content.annotations.Param;
import com.hp.oo.sdk.content.annotations.Response;
import com.hp.oo.sdk.content.plugin.ActionMetadata.MatchType;
import com.hp.oo.sdk.content.plugin.ActionMetadata.ResponseType;
import io.cloudslang.content.constants.ResponseNames;
import io.cloudslang.content.constants.ReturnCodes;
import io.cloudslang.content.ldap.constants.InputNames;
import io.cloudslang.content.ldap.entities.ResetComputerAccountInput;
import io.cloudslang.content.ldap.services.ResetComputerAccountService;
import io.cloudslang.content.ldap.utils.ResultUtils;

import java.util.Map;

public class ResetComputerAccountAction {
    /**
     * Resets a computer account in Active Directory, by resetting the password to an initial password.
     *
     * @param host                       The domain controller to connect to.
     * @param computerDN                 The distinguished name of the computer account we want to reset.
     *                                   Example: CN=computer_name,DC=example,DC=com
     * @param username                   The user to connect to AD as.
     * @param password                   The password to connect to AD as.
     * @param useSSL                     If true, the operation uses the Secure Sockets Layer (SSL) or Transport Layer
     *                                   Security (TLS) protocol to establish a connection to the remote computer. By default,
     *                                   the operation tries to establish a secure connection over TLSv1.2. Default port
     *                                   for SSL/TLS is 636.
     *                                   Valid values: true, false.
     *                                   Default value: false.
     * @param trustAllRoots              Specifies whether to enable weak security over SSL. A SSL certificate is trusted
     *                                   even if no trusted certification authority issued it.
     *                                   Valid values: true, false.
     *                                   Default value: true.
     * @param keyStore                   The location of the KeyStore file.
     *                                   Example: %JAVA_HOME%/jre/lib/security/cacerts.
     * @param keyStorePassword           The password associated with the KeyStore file.
     * @param trustKeystore              The location of the TrustStore file.
     *                                   Example: %JAVA_HOME%/jre/lib/security/cacerts.
     * @param trustPassword              The password associated with the TrustStore file.
     *
     * @return a map containing the output of the operations. Keys present in the map are:
     * <br><b>returnResult</b> - The return result of the operation.
     * <br><b>returnCode</b> - The return code of the operation. 0 if the operation goes to success, -1 if the operation goes to failure.
     * <br><b>exception</b> - The exception message if the operation goes to failure.
     */
    @Action(name = "Reset Computer Account",
            outputs = {
                    @Output(io.cloudslang.content.constants.OutputNames.RETURN_RESULT),
                    @Output(io.cloudslang.content.constants.OutputNames.RETURN_CODE),
                    @Output(io.cloudslang.content.constants.OutputNames.EXCEPTION)
            },
            responses = {
                    @Response(text = ResponseNames.SUCCESS, field = io.cloudslang.content.constants.OutputNames.RETURN_CODE,
                            value = ReturnCodes.SUCCESS,
                            matchType = MatchType.COMPARE_EQUAL, responseType = ResponseType.RESOLVED),
                    @Response(text = ResponseNames.FAILURE, field = io.cloudslang.content.constants.OutputNames.RETURN_CODE,
                            value = ReturnCodes.FAILURE,
                            matchType = MatchType.COMPARE_EQUAL, responseType = ResponseType.ERROR)
            })
    public Map<String, String> execute(
            @Param(value = InputNames.HOST, required = true) String host,
            @Param(value = InputNames.COMPUTER_DN, required = true) String computerDN,
            @Param(value = InputNames.USERNAME) String username,
            @Param(value = InputNames.PASSWORD) String password,
            @Param(value = InputNames.USE_SSL) String useSSL,
            @Param(value = InputNames.TRUST_ALL_ROOTS) String trustAllRoots,
            @Param(value = InputNames.KEYSTORE) String keyStore,
            @Param(value = InputNames.KEYSTORE_PASSWORD) String keyStorePassword,
            @Param(value = InputNames.TRUST_KEYSTORE) String trustKeystore,
            @Param(value = InputNames.TRUST_PASSWORD) String trustPassword){
        ResetComputerAccountInput.Builder inputBuilder = new ResetComputerAccountInput.Builder()
                .host(host)
                .computerDN(computerDN)
                .username(username)
                .password(password)
                .useSSL(useSSL)
                .trustAllRoots(trustAllRoots)
                .keyStore(keyStore)
                .keyStorePassword(keyStorePassword)
                .trustKeystore(trustKeystore)
                .trustPassword(trustPassword);
        try {
            return new ResetComputerAccountService().execute(inputBuilder.build());
        } catch (Exception e) {
            return ResultUtils.fromException(e);
        }
    }
}
