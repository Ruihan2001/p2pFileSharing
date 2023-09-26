/*
 * File name: ClientMain.java
 * Main class: ClientMain
 *
 * Introduction:
 * Main implements for server GUI and interacts.
 *
 * Create time: 2021/10/20
 * Last modified time: 2021/10/23
 */

package Client;

import Utilities.HashedResourceTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientMain extends JFrame implements ActionListener {
    JButton connectButton, disconnectButton, sharingPathBrowse, receivingPathBrowse, refreshButton, downloadButton;
    JLabel statusText;
    JTextField IPAddress;
    JTextField sharingPath;
    JTextField receivingPath;
    JTextField nickName;
    JTextArea logArea;
    JTable resourceTable;
    DefaultTableModel resourceTableModel;

    HashedResourceTable dHRT;
    String[] dHRTHeader = {"Resource GUID", "Resource Name", "Peers"};

    Socket clientSocket;

    public static SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    public ClientMain(String title) {
        super(title);
        dHRT = new HashedResourceTable();

        this.setLayout(new BorderLayout());
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));

        //Operation panel
        JPanel operationPanel = new JPanel(new FlowLayout());
        operationPanel.add(new JLabel("IP address:"));
        IPAddress = new JTextField("127.0.0.1:5000", 15);
        operationPanel.add(IPAddress);

        connectButton = new JButton("Connect");
        connectButton.addActionListener(this);
        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(this);
        operationPanel.add(connectButton);
        operationPanel.add(disconnectButton);

        controlPanel.add(operationPanel, BorderLayout.CENTER);

        //Status panel
        JPanel statusPanel = new JPanel(new FlowLayout());

        statusText = new JLabel("No server connected");
        statusPanel.add(new JLabel("Status:"));
        statusPanel.add(statusText);
        controlPanel.add(statusPanel, BorderLayout.SOUTH);

        //File sharing settings
        JTabbedPane tabPane = new JTabbedPane();
        JPanel settingsPanel = new JPanel(new GridLayout(4, 0));

        JPanel sharingPanel = new JPanel(new FlowLayout());
        sharingPathBrowse = new JButton("Browse sharing path");
        sharingPathBrowse.addActionListener(this);
        sharingPath = new JTextField(15);
        sharingPanel.add(new JLabel("Sharing Path:"));
        sharingPanel.add(sharingPath);
        sharingPanel.add(sharingPathBrowse);

        JPanel receivingPanel = new JPanel(new FlowLayout());
        receivingPathBrowse = new JButton("Browse receiving path");
        receivingPathBrowse.addActionListener(this);
        receivingPath = new JTextField(15);
        receivingPanel.add(new JLabel("Receiving Path:"));
        receivingPanel.add(receivingPath);
        receivingPanel.add(receivingPathBrowse);

        JPanel nickNamePanel = new JPanel(new FlowLayout());
        nickName = new JTextField("Nobody's computer", 15);
        nickNamePanel.add(new JLabel("Nickname"), 0);
        nickNamePanel.add(nickName, 0);

        settingsPanel.add(nickNamePanel, 0);
        settingsPanel.add(sharingPanel, 1);
        settingsPanel.add(new JLabel(""), 2);
        settingsPanel.add(receivingPanel, 3);
        tabPane.add("Settings", settingsPanel);

        //Log area
        JScrollPane scrollPane = new JScrollPane();
        logArea = new JTextArea(10, 5);
        logArea.setEditable(false);
        logArea.setBackground(Color.WHITE);
        scrollPane.setViewportView(logArea);
        tabPane.add("Log", scrollPane);


        //Resource list
        JPanel fileBrowsing = new JPanel(new BorderLayout());
        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 10, 2));
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(this);
        downloadButton = new JButton("Download");
        downloadButton.addActionListener(this);
        actionPanel.add(refreshButton, 0);
        actionPanel.add(downloadButton, 1);
        fileBrowsing.add(actionPanel, BorderLayout.SOUTH);


        resourceTableModel = new DefaultTableModel();
        resourceTableModel.setDataVector(dHRT.getValues(), dHRTHeader);
        resourceTable = new JTable(resourceTableModel) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };  //Make table uneditable
        resourceTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resourceTable.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        JScrollPane resourceTablePane = new JScrollPane();
        resourceTablePane.setViewportView(resourceTable);
        fileBrowsing.add(resourceTablePane, BorderLayout.CENTER);
        tabPane.add("DHRT", fileBrowsing);

        this.add(controlPanel, BorderLayout.NORTH);
        this.add(tabPane, BorderLayout.CENTER);
        SwitchOffStatus(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            String str = e.getActionCommand();

            switch (str) {
                case "Connect" -> {
                    dHRT = new HashedResourceTable();
                    String[] IPAddressText = IPAddress.getText().split(":");
                    if (!IPAddressText[0].equals("") && !sharingPath.getText().equals("") &&
                            !receivingPath.getText().equals("")) {
                        try {
                            //Connect to server
                            InetAddress host = InetAddress.getByName(IPAddressText[0]);
                            clientSocket = new Socket(host, Integer.parseInt(IPAddressText[1]));
                            statusText.setText("Connecting to " + IPAddressText[0] + " at " + IPAddressText[1]);
                            IPAddress.setEnabled(false);

                            //Register to server
                            ClientThread clientThread = new ClientThread(clientSocket, this, dHRT,
                                    sharingPath.getText(), receivingPath.getText(), nickName.getText(),
                                    Integer.parseInt(IPAddressText[1]), 0);
                            clientThread.start();

                            //Switch button status
                            SwitchOffStatus(false);

                        } catch (Exception ex) {
                            statusText.setText("Failed in connecting to the server.");
                        }
                    } else
                        statusText.setText("IP address/ Sharing path/ Receiving path should not be empty.");
                }
                case "Disconnect" -> {
                    try {
                        //Close main workload socket to make it exit
                        clientSocket.close();
                    } catch (Exception ee) {
                        statusText.setText("Error closing client");
                    }
                    statusText.setText("Connection is closed");
                    SwitchOffStatus(true);
                    clientSocket = null;
                }
                case "Browse sharing path" -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                        sharingPath.setText(chooser.getSelectedFile().getPath());
                    }
                }
                case "Browse receiving path" -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                        receivingPath.setText(chooser.getSelectedFile().getPath());
                    }
                }
                case "Refresh" -> {
                    //Refresh DHRT
                    String[] IPAddressText = IPAddress.getText().split(":");
                    if (!IPAddressText[0].equals("") && !sharingPath.getText().equals("") &&
                            !receivingPath.getText().equals("")) {
                        try {
                            ClientThread clientThread = new ClientThread(clientSocket, this, dHRT, 2);
                            clientThread.start();
                        } catch (Exception ex) {
                            statusText.setText("Failed in connecting to the server.");
                        }
                    } else
                        statusText.setText("IP address/ Sharing path/ Receiving path should not be empty.");
                }
                case "Download" -> {
                    //Get target resourceGUID
                    String resourceGUID;
                    try {
                        resourceGUID = (String) resourceTable.getValueAt(resourceTable.getSelectedRow(), 0);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        Log("Wrong table selection. Make a selection in table before downloading.");
                        return;
                    }
                    String[] IPAddressText = IPAddress.getText().split(":");
                    if (!IPAddressText[0].equals("") && !sharingPath.getText().equals("") &&
                            !receivingPath.getText().equals("")) {
                        try {
                            //Contact to server to start fetching file process
                            ClientThread clientThread = new ClientThread(clientSocket, this,
                                    dHRT, resourceGUID, receivingPath.getText(), 1);
                            clientThread.start();


                        } catch (Exception ex) {
                            statusText.setText("Failed in connecting to the server.");
                        }
                    } else
                        statusText.setText("IP address/ Sharing path/ Receiving path should not be empty.");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //Refresh table view.
    public void RefreshDHRT() {
        SwingUtilities.invokeLater(() -> {
            resourceTableModel.setDataVector(dHRT.getValues(), dHRTHeader);
            resourceTable.setModel(resourceTableModel);
            AdjustTable();
        });
    }

    //Adjust table size by longest data.
    public void AdjustTable() {
        JTableHeader header = resourceTable.getTableHeader();
        int rowCount = resourceTable.getRowCount();
        TableColumnModel cm = resourceTable.getColumnModel();

        //Get longest size and set.
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn column = cm.getColumn(i);
            int width = (int) header.getDefaultRenderer().getTableCellRendererComponent(resourceTable,
                    column.getIdentifier(), false, false, -1, i).getPreferredSize().getWidth();

            for (int row = 0; row < rowCount; row++) {
                int preferredWidth = (int) resourceTable.getCellRenderer(row, i).
                        getTableCellRendererComponent(resourceTable, resourceTable.getValueAt(row, i),
                                false, false, row, i).getPreferredSize().getWidth();

                width = Math.max(width, preferredWidth);
            }
            column.setPreferredWidth(width + resourceTable.getIntercellSpacing().width);
        }
        //Apply changes
        resourceTable.doLayout();
    }

    //Set button status as system is offline
    public void SwitchOffStatus(boolean value) {
        IPAddress.setEnabled(value);
        connectButton.setEnabled(value);
        sharingPathBrowse.setEnabled(value);
        receivingPathBrowse.setEnabled(value);
        refreshButton.setEnabled(!value);
        downloadButton.setEnabled(!value);
        disconnectButton.setEnabled(!value);
    }

    public void StatusEdit(String message) {
        SwingUtilities.invokeLater(() -> statusText.setText(message));
    }

    //Log
    public void Log(String text) {
        SwingUtilities.invokeLater(() -> {
            Date date = new Date();
            logArea.append(formatter.format(date) + " " + text + "\n");
        });
    }
}
