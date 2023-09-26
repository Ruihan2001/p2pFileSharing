/*
 * File name: NetMessage.java
 * Main class: NetMessage
 *
 * Introduction:
 * The data structure for message exchange between server/client.
 *
 * Create time: 2021/10/20
 * Last modified time: 2021/11/19
 */

package Utilities;

import java.io.Serializable;

public class NetMessage implements Serializable {
    /*
    Hosts rely on MessageType to recognize information and react.
    REGISTER: When a client first connect to the server.
    REQUEST: Request for resources(UHRT, FILE and so on...).
    REPLY: Reply from other host.
    UPDATE: Request for resource list update.
    FILE: Before a file from transmitting.
     */
    public enum MessageType {REGISTER, REQUEST, REPLY, UPDATE, FILE}

    public MessageType type;
    public String senderName;
    public String identifier;
    public String data;
    public int port;
    public long fileSize;
}
