/*
 * File name: ClientThread.java
 * Main class: ClientThread
 *
 * Introduction:
 * Main workload of client.
 * Listening for connections, answering for requests and make requests.
 *
 * Create time: 2021/10/20
 * Last modified time: 2021/11/19
 */

package Client;

import Utilities.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/*
ClientThread first called on mode 0(Initialize mode) to exchange data(register) with server.
Then keep a socket listening for peer connection.
It create ChildThread for further process if a peer connection arrives.
ClientThread may called on other mode to satisfy different user action.
 */
public class ClientThread extends Thread {
    Socket clientSocket;
    static ServerSocket serverSocket;
    ClientMain userInterface;
    HashedResourceTable dHRT;
    ResourceInfoList fRIL;

    String sharingPath, receivingPath, nickName, peerGUID, resourceGUID;
    int port, mode;

    //For initialize, this method is the only way to offer enough objects.
    public ClientThread(Socket clientSocket, ClientMain userInterface, HashedResourceTable dHRT,
                        String sharingPath, String receivingPath, String nickName, int port, int mode) {
        this.clientSocket = clientSocket;
        this.userInterface = userInterface;
        this.dHRT = dHRT;
        this.sharingPath = sharingPath;
        this.receivingPath = receivingPath;
        this.nickName = nickName;
        this.port = port;
        this.mode = mode;
    }

    public ClientThread(Socket clientSocket, ClientMain userInterface, HashedResourceTable dHRT,
                        String resourceGUID, String receivingPath, int mode) {
        this.clientSocket = clientSocket;
        this.userInterface = userInterface;
        this.dHRT = dHRT;
        this.resourceGUID = resourceGUID;
        this.receivingPath = receivingPath;
        this.mode = mode;
    }

    public ClientThread(Socket clientSocket, ClientMain userInterface, HashedResourceTable dHRT,
                        int mode) {
        this.clientSocket = clientSocket;
        this.userInterface = userInterface;
        this.dHRT = dHRT;
        this.mode = mode;
    }

    public void run() {
        switch (mode) {
            case 0 -> InitializeConnection();
            case 1 -> FetchFile();
            case 2 -> UpdateDHRP();
            case 3 -> AdvertiseSharingFile();
        }
    }

    void InitializeConnection() {
        userInterface.Log("Connection established. Synchronizing...");
        try {
            //Listening on a system decided port
            serverSocket = new ServerSocket(0);
            userInterface.Log("System indicated port is " + serverSocket.getLocalPort());
            //Communicate with server
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            NetMessage temp = new NetMessage();
            temp.type = NetMessage.MessageType.REGISTER;
            temp.senderName = nickName;
            temp.port = serverSocket.getLocalPort();
            oos.writeObject(temp);
            oos.flush();

            //Send file information to server.
            oos.writeObject(ClientSupport.getDirectoryFiles(sharingPath));
            oos.flush();
            userInterface.Log("File information sent.");

            //Construct a local file table for peer-peer file transfer
            fRIL = ClientSupport.getFullDirectoryFiles(sharingPath);
            userInterface.Log("Local file information table constructed.");


            //Wait for server
            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());

            temp = (NetMessage) ois.readObject();
            peerGUID = temp.identifier;
            userInterface.Log("Got peerGUID: " + peerGUID);

            //Set DHRT
            dHRT.setResource((HashedResourceTable) ois.readObject());

            userInterface.Log("Connection established. Synchronized.");
            Date date = new Date();
            userInterface.StatusEdit("Last synchronized at " + ClientMain.formatter.format(date));

        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            userInterface.RefreshDHRT();
        }
        while (true) {
            if (!serverSocket.isClosed()) {
                try {
                    //Listening for peer connection.
                    userInterface.Log("Listening peer connection.");
                    Socket newClient = serverSocket.accept();
                    userInterface.Log("Peer connection detected.");
                    ChildThread childThread = new ChildThread(newClient, fRIL, userInterface);
                    Thread t = new Thread(childThread);
                    t.start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                return;
            }
        }
    }

    void FetchFile() {
        userInterface.Log("Start to fetch file.");
        try {
            //Request server for peer information
            NetMessage temp = new NetMessage();
            temp.type = NetMessage.MessageType.REQUEST;
            temp.identifier = resourceGUID;
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            oos.writeObject(temp);
            oos.flush();
            userInterface.Log("Fetch request sent");

            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
            temp = (NetMessage) ois.readObject();
            userInterface.Log("Resource located.");

            //Check message from server if file available
            if (temp.data.equals("null")) {
                userInterface.Log("File fetch failed: No such file on server UHRT");
            } else {
                //File available, establish connection
                userInterface.Log("Establishing peer connection..");
                String[] IPAddress = temp.data.split(":");
                Socket fileSocket = new Socket(InetAddress.getByName(IPAddress[0]), Integer.parseInt(IPAddress[1]));

                //Request peer for file information
                temp.type = NetMessage.MessageType.REQUEST;
                temp.identifier = resourceGUID;

                ObjectOutputStream foos = new ObjectOutputStream(fileSocket.getOutputStream());
                foos.writeObject(temp);
                foos.flush();

                userInterface.Log("Waiting for file info.");
                ObjectInputStream fois = new ObjectInputStream(fileSocket.getInputStream());
                temp = (NetMessage) fois.readObject();

                if (temp.type != NetMessage.MessageType.FILE) {
                    //Wrong server data
                    userInterface.Log("Failed in fetching file: peer reported no such file.");
                    return;
                }

                //Ready for trans
                //long fileSize = temp.fileSize;
                String fileName = temp.data;
                File file = new File(receivingPath + File.separatorChar + fileName);
                if (file.exists()) {
                    if (!file.delete()) {
                        userInterface.Log("Unable to access target file.");
                        return;
                    }
                }
                if (!file.createNewFile()) {
                    userInterface.Log("Unable to access target file.");
                    return;
                }

                //Ready to receive
                DataOutputStream fos = new DataOutputStream(new BufferedOutputStream(new BufferedOutputStream
                        (new FileOutputStream(file.getAbsolutePath()))));
                InputStream dis = fileSocket.getInputStream();

                int bufferSize = 8192;
                byte[] buf = new byte[bufferSize];
                while (true) {
                    int read = 0;
                    if (dis != null) {
                        read = dis.read(buf);
                    }
                    if (read == -1) {
                        break;
                    }

                    fos.write(buf, 0, read);
                }
                fos.close();

                userInterface.Log("Succeed in receiving file.");
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            UpdateDHRP();
        }
    }

    void UpdateDHRP() {
        try {
            //Request for DHRT
            NetMessage temp = new NetMessage();
            temp.type = NetMessage.MessageType.REQUEST;
            temp.identifier = "DHRT";
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            oos.writeObject(temp);

            //Get and set DHRT
            userInterface.Log("Synchronizing DHRT");
            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
            dHRT.setResource((HashedResourceTable) ois.readObject());

            //Display
            userInterface.RefreshDHRT();
            userInterface.Log("DHRT Synchronized");
            Date date = new Date();
            userInterface.StatusEdit("Last synchronized at " + ClientMain.formatter.format(date));
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            for (StackTraceElement s : e.getStackTrace()) {
                userInterface.Log(s.toString());
            }
            userInterface.Log("Unable to synchronize DHRT, you may offline now.");
        }
    }

    //Advertise server for new file list
    void AdvertiseSharingFile() {
        try {
            //Construct request
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            NetMessage temp = new NetMessage();
            temp.type = NetMessage.MessageType.UPDATE;
            temp.identifier = peerGUID;
            temp.port = serverSocket.getLocalPort();
            oos.writeObject(temp);
            oos.flush();

            userInterface.Log("Ready information sent.");
            oos.writeObject(ClientSupport.getDirectoryFiles(sharingPath));
            oos.flush();
            userInterface.Log("File information sent.");

            fRIL = ClientSupport.getFullDirectoryFiles(sharingPath);
            userInterface.Log("Local file information table constructed.");
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            UpdateDHRP();
        }
    }

    //Work thread for file sending
    class ChildThread implements Runnable {
        Socket client;
        ResourceInfoList fRIL;
        ClientMain userInterface;

        public ChildThread(Socket client, ResourceInfoList fRIL, ClientMain userInterface) {
            this.client = client;
            this.fRIL = fRIL;
            this.userInterface = userInterface;
        }

        @Override
        public void run() {
            try {
                //Get requested file GUID
                ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
                NetMessage temp = (NetMessage) ois.readObject();
                userInterface.Log("Peer connection message received.");
                if (temp.type == NetMessage.MessageType.REQUEST) {
                    String resourceGUID = temp.identifier;
                    String filePath = fRIL.getPath(resourceGUID);
                    ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());

                    //No such file
                    if (filePath == null) {
                        userInterface.Log("Reply eer connection: No such file.");
                        temp.type = NetMessage.MessageType.REPLY;
                        temp.data = "null";
                        oos.writeObject(temp);
                        oos.flush();
                        return;
                    }
                    //Reply peer of file information
                    File file = new File(filePath);
                    temp.type = NetMessage.MessageType.FILE;
                    temp.data = file.getName();
                    temp.fileSize = file.length();
                    oos.writeObject(temp);
                    oos.flush();

                    //Send file
                    userInterface.Log("Start sending file.");
                    DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                    DataInputStream fis = new DataInputStream(new BufferedInputStream(new
                            FileInputStream(file.getAbsolutePath())));
                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
                        dos.write(bytes, 0, length);
                        dos.flush();
                    }
                    userInterface.Log("File sent");

                    dos.flush();
                    dos.close();
                    fis.close();

                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
