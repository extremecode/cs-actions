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
package io.cloudslang.content.ldap.constants;

public final class Constants {
    public static final String FILE = "file:";
    public static final String HTTP = "http";
    public static final int ONE_SECOND = 1000;
    public static final String DEFAULT_JAVA_KEYSTORE = System.getProperty("java.home") + "/lib/security/cacerts";

    public static final String BOUNCY_CASTLE_PROVIDER = "BC";
    public static final String PKCS_KEYSTORE_TYPE = "PKCS12";
    public static final String ENCRYPTED_CONTENT_TYPE = "application/pkcs7-mime; name=\"smime.p7m\"; " +
            "smime-type=enveloped-data";
    public static final String ENCRYPT_RECID = "The encryption recId is: ";
    public static final String SSL = "SSL";
    public static final String DEFAULT_PASSWORD_FOR_STORE = "changeit";
    public static final String SSL_SOCKET_FACTORY = "javax.net.ssl.SSLSocketFactory";
    public static final String SECURE_SUFFIX = "s";
}
