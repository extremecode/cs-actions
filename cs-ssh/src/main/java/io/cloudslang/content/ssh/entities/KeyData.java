package io.cloudslang.content.ssh.entities;


import io.cloudslang.content.ssh.utils.IdentityKeyUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * User: sacalosb
 * Date: 07.01.2016
 */
public class KeyData extends IdentityKey {
    public static final String MD_5 = "MD5";
    public static final int SIGNUM_POSITIVE = 1;
    public static final int RADIX_HEXA = 16;
    private byte[] privateKeyData;
    private String keyName;

    public KeyData(String privateKeyData) {
        this.setPrivateKeyData(privateKeyData);
        this.passPhrase = null;
        this.setKeyName();
    }

    public KeyData(String privateKeyData, String passPhrase) {
        this.setPrivateKeyData(privateKeyData);
        this.setPassPhrase(passPhrase);
        this.setKeyName();
    }

    public byte[] getPrivateKeyData() {
        return (privateKeyData == null) ? null : Arrays.copyOf(privateKeyData, privateKeyData.length);
    }

    private void setPrivateKeyData(String privateKeyData) {
        String fixedPrivateKey = IdentityKeyUtils.fixPrivateKeyFormat(privateKeyData);
        this.privateKeyData = fixedPrivateKey.getBytes(KEY_ENCODING);
    }

    public String getKeyName() {
        return keyName;
    }

    private void setKeyName() {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(MD_5);
            keyName = new BigInteger(SIGNUM_POSITIVE, messageDigest.digest(privateKeyData)).toString(RADIX_HEXA);
        } catch (NoSuchAlgorithmException e) {
            keyName = Integer.toHexString(Arrays.hashCode(privateKeyData));
        }
    }
}
