/*
 * File name: ClientSupport.java
 * Main class: ClientSupport
 *
 * Introduction:
 * Some tool class for server.
 *
 * Create time: 2021/10/20
 * Last modified time: 2021/11/19
 */

package Client;

import Utilities.ResourceInfoList;
import Utilities.SHA1Generator;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Vector;

public class ClientSupport {
    //Construct local file information table
    public static ResourceInfoList getDirectoryFiles(String path) {
        ResourceInfoList list = new ResourceInfoList();
        getFiles(new File(path), list.getFileInfoList());

        return list;
    }

    //Construct local file information table with full file address
    public static ResourceInfoList getFullDirectoryFiles(String path) throws NoSuchAlgorithmException {
        ResourceInfoList list = new ResourceInfoList();
        getFullFiles(new File(path), list.getFileInfoList());

        return list;
    }

    //Recursively get file information
    public static void getFiles(File file, Vector<Vector<String>> list) {
        for (File f : Objects.requireNonNull(file.listFiles())) {
            if (f.isDirectory()) {
                getFiles(f, list);
            } else if (f.isFile()) {
                Vector<String> info = new Vector<>();
                info.add(f.getName());
                info.add(getFileMD5(f));
                list.add(info);
            }
        }
    }

    //Recursively get file information with full file address
    public static void getFullFiles(File file, Vector<Vector<String>> list) throws NoSuchAlgorithmException {
        for (File f : Objects.requireNonNull(file.listFiles())) {
            if (f.isDirectory()) {
                getFiles(f, list);
            } else if (f.isFile()) {
                Vector<String> info = new Vector<>();
                info.add(f.getAbsolutePath());
                info.add(SHA1Generator.getDigest(f.getName() + getFileMD5(f)));
                list.add(info);
            }
        }
    }

    //Calculate file MD5
    public static String getFileMD5(File f) {
        BigInteger bi = null;
        try {
            byte[] buffer = new byte[8192];
            int len;
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(f);
            while ((len = fis.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            fis.close();
            byte[] b = md.digest();
            bi = new BigInteger(1, b);
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

        return bi == null ? "" : bi.toString(16);
    }
}
