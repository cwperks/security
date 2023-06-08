/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.security.authtoken.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.opensaml.xmlsec.encryption.P;

public class EncryptionDecryptionUtil {

    public static String encrypt(final String secret, final String data) {
        final Cipher cipher = createCipherFromSecret(secret);
        final byte[] cipherText = createCipherText(cipher, data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(cipherText);
    }

    public static String decrypt(final String secret, final String encryptedString) {
        final Cipher cipher = createCipherFromSecret(secret);
        final byte[] cipherText = createCipherText(cipher, Base64.getDecoder().decode(encryptedString));
        return new String(cipherText, StandardCharsets.UTF_8);
    }

    private static Cipher createCipherFromSecret(final String secret) {
        try {
            final byte[] decodedKey = Base64.getDecoder().decode(secret);
            final Cipher cipher = Cipher.getInstance("AES");
            final SecretKey originalKey = new SecretKeySpec(Arrays.copyOf(decodedKey, 16), "AES");
            cipher.init(Cipher.DECRYPT_MODE, originalKey);
            return cipher;
        } catch (final Exception e) {
            throw new RuntimeException("Error creating cipher from secret");
        }
    }
    
    private static byte[] createCipherText(final Cipher cipher, final byte[] data) {
        try {
            return cipher.doFinal(data);
        } catch (final Exception e) {
            throw new RuntimeException("The cipher was unable to perform pass over data");
        }
    }
}