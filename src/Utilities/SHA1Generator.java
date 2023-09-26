/*
 * File name: SHA1Generator.java
 * Main class: SHA1Generator
 *
 * Introduction:
 * Generate a hex-digest string for given value by SHA-1 algorithm
 *
 * Create time: 2021/10/20
 * Last modified time: 2021/11/19
 */

package Utilities;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class SHA1Generator {
    public SHA1Generator() {

    }

    public static String getDigest(String value) throws NoSuchAlgorithmException{
        //Get digest instance ready
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();

        //Generate string
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder sha1 = new StringBuilder();
        for (byte c : digest.digest()) {
            sha1.append(Integer.toHexString(c));
        }

        return sha1.toString();
    }
}
