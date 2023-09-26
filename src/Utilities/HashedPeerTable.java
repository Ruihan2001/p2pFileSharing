/*
 * File name: HashedPeerTable.java
 * Main class: HashedPeerTable, peerInfo
 *
 * Introduction:
 * Data structure for UHPT.
 *
 * Create time: 2021/10/20
 * Last modified time: 2021/11/19
 */

package Utilities;

import java.io.Serializable;
import java.util.Hashtable;

public class HashedPeerTable implements Serializable {
    Hashtable<String, peerInfo> peerList;   //Key-PeerGUID; Value-peer details.

    public HashedPeerTable() {
        peerList = new Hashtable<>();
    }

    public void add(String peerGUID, String peerName, String peerIP, int peerPort, int routingMetric) {
        peerList.put(peerGUID, new peerInfo(peerName, peerIP, peerPort, routingMetric));
    }

    //Serialize values for table displaying
    public Object[][] valueSet() {
        String[][] values = new String[peerList.size()][3];
        int i = 0;
        for (String key : peerList.keySet()) {
            values[i][0] = key;
            values[i][1] = peerList.get(key).peerName;
            values[i][2] = Integer.toString(peerList.get(key).routingMetric);
            i++;
        }

        return values;
    }

    public peerInfo getByGUID(String peerGUID) {
        return peerList.get(peerGUID);
    }

    public void deleteByGUID(String peerGUID) {
        peerList.remove(peerGUID);
    }

    public static class peerInfo {
        public String peerName;
        public String peerIP;
        public int peerPort;    //Peer client listening port. NOT the port connected to server.
        public int routingMetric;

        peerInfo(String peerName, String peerIP, int peerPort, int routingMetric) {
            this.peerName = peerName;
            this.peerIP = peerIP;
            this.peerPort = peerPort;
            this.routingMetric = routingMetric;
        }
    }
}


