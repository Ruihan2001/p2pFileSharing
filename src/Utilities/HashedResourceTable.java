/*
 * File name: HashedResourceTable.java
 * Main class: HashedResourceTable
 *
 * Introduction:
 * Data structure for UHRT or DHRT. Made up of two tables.
 *
 * Create time: 2021/10/20
 * Last modified time: 2021/11/20
 */

package Utilities;

import java.io.Serializable;
import java.util.*;

public class HashedResourceTable implements Serializable {
    Hashtable<String, ArrayList<String>> peerTable; //Key-resourceGUID, value-peerGUIDs
    Hashtable<String, String> resourceTable;    //Key-resourceGUID, value-resourceName

    public HashedResourceTable() {
        peerTable = new Hashtable<>();
        resourceTable = new Hashtable<>();
    }

    //Serialize values for table displaying
    public String[][] getValues() {
        String[][] values = new String[resourceTable.size()][3];
        int i = 0;
        for (String key : resourceTable.keySet()) {
            values[i][0] = key;
            values[i][1] = resourceTable.get(key);
            values[i][2] = peerTable.get(key).toString();
            i++;
        }

        return values;
    }

    public ArrayList<String> getPeers(String resourceGUID) {
        return peerTable.get(resourceGUID);
    }

    public void addResource(String resourceName, String resourceGUID, String peerGUID) {
        //Check if resource already exists.
        if (resourceTable.containsKey(resourceGUID)) {
            //Remove repeat
            if (peerTable.get(resourceGUID).contains(peerGUID)) {
                return;
            }
            //Directly add peerGUID into UHRT
            ArrayList<String> list = peerTable.get(resourceGUID);
            list.add(peerGUID);
        } else {
            resourceTable.put(resourceGUID, resourceName);
            ArrayList<String> list = new ArrayList<>();
            list.add(peerGUID);
            peerTable.put(resourceGUID, list);
        }
    }

    public void setResource(HashedResourceTable src) {
        peerTable = src.peerTable;
        resourceTable = src.resourceTable;
    }

    public synchronized void deleteResource(String resourceGUID) {
        if (resourceTable.containsKey(resourceGUID)) {
            resourceTable.remove(resourceGUID);
            peerTable.remove(resourceGUID);
        }
    }

    public synchronized void deletePeer(String peerGUID) {
        //Store which GUID needs to be deleted.
        Vector<String> deleteResourceGUIDs = new Vector<>();

        for (Map.Entry<String, ArrayList<String>> element : peerTable.entrySet()) {
            //If peerGUID contains target.
            if (element.getValue().contains(peerGUID))
            {
                element.getValue().remove(peerGUID);
                //If only one peerGUID for the resource, then delete it.
                if (element.getValue().size() < 1) {
                    deleteResourceGUIDs.add(element.getKey());
                    //The following commented code would cause CONCURRENCY issues.
                    //deleteResource(element.getKey());
                    //Concurrency issues
                }
            }
        }
        //To avoid concurrency issue, delete this way.
        for (String rGUID : deleteResourceGUIDs) {
            deleteResource(rGUID);
        }
    }

}

