/*
 * File name: ServerMain.java
 * Main class: ServerMain
 *
 * Introduction:
 * Main implements for server GUI and interacts.
 *
 * Create time: 2021/10/20
 * Last modified time: 2021/11/19
 */

package Server;

import Utilities.HashedPeerTable;
import Utilities.HashedResourceTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerMain extends JFrame implements ActionListener {
    JButton startButton, stopButton;
    JLabel statusText;
    JTextField portNumber;
    JTextArea logArea;
    JTable peerList, resourceTable;
    DefaultTableModel peerListModel, resourceTableModel;
    HashedPeerTable uHPT;
    HashedResourceTable uHRT;

    String[] uHRTHeader = {"Resource GUID", "Resource Name", "Peers"};
    String[] uHPTHeader = {"Peer GUID", "Peer Name", "Routing Metric"};

    ServerSocket serverSocket;
    ServerThread serverThread;

    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    public ServerMain(String title) {
        super(title);
        uHPT = new HashedPeerTable();
        uHRT = new HashedResourceTable();

        this.setLayout(new BorderLayout());

        //Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        controlPanel.add(new JLabel("Port:"));
        portNumber = new JTextField("5000", 5);
        controlPanel.add(portNumber);

        startButton = new JButton("Start");
        startButton.addActionListener(this);
        stopButton = new JButton("Stop");
        stopButton.addActionListener(this);
        stopButton.setEnabled(false);
        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        statusText = new JLabel("Server stopped.");
        controlPanel.add(new JLabel("Status"));
        controlPanel.add(statusText);

        //Log area
        JTabbedPane tabPane = new JTabbedPane();
        JScrollPane scrollPane = new JScrollPane();
        logArea = new JTextArea(10, 5);
        logArea.setEditable(false);
        logArea.setBackground(Color.WHITE);
        scrollPane.setViewportView(logArea);
        tabPane.add("Log", scrollPane);

        //Peer list
        peerListModel = new DefaultTableModel();
        peerListModel.setDataVector(uHPT.valueSet(), uHPTHeader);
        peerList = new JTable(peerListModel) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };  //Make the table uneditable.
        JScrollPane peerListPane = new JScrollPane();
        peerListPane.setViewportView(peerList);
        tabPane.add("UHPT", peerListPane);

        //Resource list
        resourceTableModel = new DefaultTableModel();
        resourceTableModel.setDataVector(uHRT.getValues(), uHRTHeader);
        resourceTable = new JTable(resourceTableModel) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };  //Make the table uneditable.
        resourceTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane resourceTablePane = new JScrollPane();
        resourceTablePane.setViewportView(resourceTable);
        tabPane.add("UHRT", resourceTablePane);

        this.add(controlPanel, BorderLayout.NORTH);
        this.add(tabPane, BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            String str = e.getActionCommand();

            if (str.equals("Start")) {
                String portNumberText = portNumber.getText();
                if (!portNumberText.equals("")) {
                    try {
                        //Start listening socket.
                        serverSocket = new ServerSocket(Integer.parseInt(portNumberText));
                        statusText.setText("Server is running....");
                        portNumber.setEnabled(false);

                        startButton.setEnabled(false);
                        stopButton.setEnabled(true);

                        serverThread = new ServerThread(serverSocket, this, uHPT, uHRT);
                        serverThread.start();

                    } catch (Exception ex) {
                        statusText.setText("Invalid port number.");
                    }
                } else
                    statusText.setText("Port number should not be empty.");

            }
            if (str.equals("Stop")) {
                try {
                    //Stop listening socket, then main workload thread would exit.
                    serverSocket.close();
                } catch (Exception ee) {
                    statusText.setText("Error closing server");
                }
                statusText.setText("Server is closed");
                portNumber.setEnabled(true);
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                serverSocket = null;
                serverThread = null;

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //Refresh tables view.
    public void RefreshUHRT() {
        SwingUtilities.invokeLater(() -> {
            resourceTableModel.setDataVector(uHRT.getValues(), uHRTHeader);
            resourceTable.setModel(resourceTableModel);
            AdjustResourceTable();
        });
    }

    public void RefreshUHPT() {
        SwingUtilities.invokeLater(() -> {
            peerListModel.setDataVector(uHPT.valueSet(), uHPTHeader);
            peerList.setModel(peerListModel);
            AdjustPeerTable();
        });
    }

    //Adjust table size by longest data.
    public void AdjustResourceTable() {
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

        //Apply changes.
        resourceTable.doLayout();
    }

    public void AdjustPeerTable() {
        JTableHeader header = peerList.getTableHeader();
        int rowCount = peerList.getRowCount();
        TableColumnModel cm = peerList.getColumnModel();

        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn column = cm.getColumn(i);
            int width = (int) header.getDefaultRenderer().getTableCellRendererComponent(peerList,
                    column.getIdentifier(), false, false, -1, i).getPreferredSize().getWidth();

            for (int row = 0; row < rowCount; row++) {
                int preferredWidth = (int) peerList.getCellRenderer(row, i).
                        getTableCellRendererComponent(peerList, peerList.getValueAt(row, i),
                                false, false, row, i).getPreferredSize().getWidth();

                width = Math.max(width, preferredWidth);
            }
            column.setPreferredWidth(width + peerList.getIntercellSpacing().width);
        }

        peerList.doLayout();
    }

    //Log output
    public void Log(String text) {
        SwingUtilities.invokeLater(() -> {
            Date date = new Date();
            logArea.append(formatter.format(date) + "  " + text + "\n");
        });
    }
}
