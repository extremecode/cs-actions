package io.cloudslang.content.mail.constants;

public final class CipherSuites {
    public static final String TLS_DHE_RSA_WITH_AES_256_GCM_SHA384 = "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384";
    public static final String TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 = "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384";
    public static final String TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 = "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256";
    public static final String TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 = "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256";
    public static final String TLS_DHE_RSA_WITH_AES_128_CBC_SHA256 = "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256";
    public static final String TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384 = "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384";
    public static final String TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256 = "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256";
    public static final String TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256 = "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256";
    public static final String TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256 = "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256";
    public static final String TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384 = "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384";
    public static final String TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384 = "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384";
    public static final String TLS_RSA_WITH_AES_256_GCM_SHA384 = "TLS_RSA_WITH_AES_256_GCM_SHA384";
    public static final String TLS_RSA_WITH_AES_256_CBC_SHA256 = "TLS_RSA_WITH_AES_256_CBC_SHA256";
    public static final String TLS_RSA_WITH_AES_128_CBC_SHA256 = "TLS_RSA_WITH_AES_128_CBC_SHA256";

    public static boolean validate(String cipherSuite) throws IllegalAccessException {
       return true;
    }
}
