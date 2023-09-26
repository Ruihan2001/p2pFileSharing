/*
 * File name: ServiceSupport.java
 * Main class: ServiceSupport
 *
 * Introduction:
 * Some tool class for server.
 *
 * Create time: 2021/10/20
 * Last modified time: 2021/11/19
 */

package Server;

import Utilities.HashedPeerTable;
import Utilities.HashedResourceTable;


public class ServiceSupport {
    //Choose a suitable peer for file transfer.(minimum routingMetric)
    public static String getPeer(String resourceGUID, HashedPeerTable uHPT, HashedResourceTable uHRT) {
        int minPeer = -1;
        String finalPeerGUID = null;
        for (String peerGUID : uHRT.getPeers(resourceGUID)) {
            if (minPeer == -1 || uHPT.getByGUID(peerGUID).routingMetric < minPeer) {
                minPeer = uHPT.getByGUID(peerGUID).routingMetric;
                finalPeerGUID = peerGUID;
            }
        }

        return finalPeerGUID;
    }
}
