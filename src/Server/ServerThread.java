/*
 * File name: ServerThread.java
 * Main class: ServerThread
 *
 * Introduction:
 * Main workload of server.
 * Listening for connections, answering for requests.
 *
 * Create time: 2021/10/20
 * Last modified time: 2021/11/19
 */

package Server;

import Utilities.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import java.security.*;

/*
ServerThread maintains serverSocket which listens for new client connections.
Once a new connection request detected, it starts a new ChildThread for further process.
 */
public class ServerThread extends Thread {
    ServerSocket serverSocket;
    ServerMain userInterface;
    HashedPeerTable uHPT;
    HashedResourceTable uHRT;

    public ServerThread(ServerSocket serverSocket, ServerMain userInterface, HashedPeerTable uHPT,
                        HashedResourceTable uHRT) {
        this.serverSocket = serverSocket;
        this.userInterface = userInterface;
        this.uHPT = uHPT;
        this.uHRT = uHRT;
    }

    public void run() {
        userInterface.Log("Server started.");
        while (true) {
            if (serverSocket.isClosed()) {
                userInterface.Log("Serversocket closed.");
                return;
            }
            try {
                Socket newClient = serverSocket.accept();
                userInterface.Log("New connection detected. From" + newClient.getInetAddress().toString()
                        + " at " + newClient.getPort());

                //Start a ChildThread for further process.
                ChildThread childThread = new ChildThread(newClient, userInterface, uHPT, uHRT);
                Thread t = new Thread(childThread);
                t.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /*
    ChildThread responds specific requests from clients.
     */
    class ChildThread implements Runnable {
        Socket client;
        ServerMain userInterface;
        HashedPeerTable uHPT;
        HashedResourceTable uHRT;
        NetMessage msg;
        String peerGUID;

        public ChildThread(Socket client, ServerMain userInterface, HashedPeerTable uHPT, HashedResourceTable uHRT) {
            this.client = client;
            this.userInterface = userInterface;
            this.uHPT = uHPT;
            this.uHRT = uHRT;
        }

        public void run() {
            while (!client.isClosed()) {
                try {
                    //Get request message.
                    ObjectInputStream obj = new ObjectInputStream(client.getInputStream());
                    userInterface.Log("Client " + client.getInetAddress().toString() + " connected.");
                    msg = (NetMessage) obj.readObject();
                    userInterface.Log("Client message" + msg.type.toString());

                    //Different message type for different actions.
                    if (msg.type == NetMessage.MessageType.REQUEST) {
                        //If client requests for DHRT
                        if (msg.identifier.equals("DHRT")) {
                            ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                            oos.writeObject(uHRT);
                            oos.flush();
                        }
                        //Get peer information for specified resource
                        else
                        {
                            String peerGUID = ServiceSupport.getPeer(msg.identifier, uHPT, uHRT);
                            ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                            NetMessage temp = new NetMessage();
                            temp.type = NetMessage.MessageType.REPLY;
                            temp.identifier = msg.identifier;
                            if (null == peerGUID) {
                                temp.data = "null";
                            } else {
                                temp.data = uHPT.getByGUID(peerGUID).peerIP + ":" + uHPT.getByGUID(peerGUID).peerPort;
                            }
                            oos.writeObject(temp);
                        }
                    }
                    //Client registry
                    else if (msg.type == NetMessage.MessageType.REGISTER) {
                        //Generate and store peer GUID
                        Random random = new Random();
                        peerGUID = UUID.randomUUID().toString();
                        uHPT.add(peerGUID, msg.senderName, client.getInetAddress().getHostAddress(), msg.port,
                                random.nextInt(50));    //We shall make routingMetric random

                        //Generate and store resource information advertised by client
                        ResourceInfoList resourceInfoList = (ResourceInfoList) obj.readObject();
                        for (Vector<String> file : resourceInfoList.getFileInfoList()) {
                            uHRT.addResource(file.get(0), SHA1Generator.getDigest(file.get(0) + file.get(1)),
                                    peerGUID);
                        }

                        //Reply client
                        userInterface.Log("Process complete, reply client.");
                        NetMessage temp = new NetMessage();
                        temp.type = NetMessage.MessageType.REPLY;
                        temp.senderName = msg.senderName;
                        temp.identifier = peerGUID;
                        userInterface.Log("Message constructed.");
                        ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                        oos.writeObject(temp);
                        oos.flush();
                        userInterface.Log("Message sent.");

                        //Deliver DHRT
                        oos.writeObject(uHRT);
                        oos.flush();
                        userInterface.Log("DHRT sent.");


                    }
                    //Update peer's resource list.
                    else if (msg.type == NetMessage.MessageType.UPDATE) {
                        uHPT.deleteByGUID(peerGUID);
                        ResourceInfoList resourceInfoList = (ResourceInfoList) obj.readObject();
                        for (Vector<String> file : resourceInfoList.getFileInfoList()) {
                            uHRT.addResource(file.get(0), SHA1Generator.getDigest(file.get(0) + file.get(1)),
                                    peerGUID);
                        }
                    }


                } catch (IOException e) {
                    //e.printStackTrace();
                    userInterface.Log(uHPT.getByGUID(peerGUID).peerName + "/" + client.getInetAddress().toString()
                            + " offline.");

                    //Clean peer data
                    uHPT.deleteByGUID(peerGUID);
                    uHRT.deletePeer(peerGUID);
                    return;
                } catch (ClassNotFoundException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    userInterface.Log("Internal error.");
                    return;
                } finally {
                    userInterface.RefreshUHPT();
                    userInterface.RefreshUHRT();
                }
            }
            userInterface.Log(uHPT.getByGUID(peerGUID).peerName + "/" + client.getInetAddress().toString()
                    + " offline.");
            //Clean peer data
            uHPT.deleteByGUID(peerGUID);
            uHRT.deletePeer(peerGUID);
        }
    }
}
