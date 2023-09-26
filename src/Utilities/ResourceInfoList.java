/*
 * File name: ResourceInfoList.java
 * Main class: ResourceInfoList
 *
 * Introduction:
 * This file implements a data structure for storing file path and file's resourceGUID.
 *
 * Create time: 2021/10/21
 * Last modified time: 2021/11/19
 */

package Utilities;

import java.io.Serializable;
import java.util.Vector;

public class ResourceInfoList implements Serializable {
    /*
    Each fileInfo item stands for an item of file.
    Each item has two elements, first - file absolute path; second - file GUID.
     */
    public Vector<Vector<String>> fileInfo;

    public ResourceInfoList() {
        fileInfo = new Vector<>();
    }

    public Vector<Vector<String>> getFileInfoList() {
        return fileInfo;
    }

    public String getPath(String resourceGUID) {
        for (Vector<String> file : fileInfo) {
            if (file.contains(resourceGUID)) {
                return file.elementAt(0);
            }
        }

        return null;
    }
}
