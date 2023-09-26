/*
 * File name: Entry.java
 * Main class: Entry
 *
 * Introduction:
 * Program entry class, this class draw the initial UI to choose server mode or client mode
 *
 * Create time: 2021/10/20
 * Last modified time: 2021/2021/11/17
 */

import Client.ClientMain;
import Server.ServerMain;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;

public class Entry extends JFrame implements ActionListener {
    //Program entry
    public static void main(String[] args) {
        Entry selectWindow = new Entry("Start as ...");
        selectWindow.setSize(300, 150);
        selectWindow.setResizable(false);
        selectWindow.setLocationRelativeTo(null);
        selectWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        selectWindow.setVisible(true);
    }

    JButton startButton, cancelButton;
    JComboBox<String> modeBox;
    JPanel selectionArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
    JPanel buttonArea = new JPanel(new FlowLayout());

    Entry(String title) {
        super(title);

        //ComboBox
        selectionArea.add(new JLabel("Mode: "));
        String[] modes = {"Server", "Client"};
        modeBox = new JComboBox<>(modes);
        modeBox.setSize(150, 30);
        selectionArea.add(modeBox);

        //Button
        startButton = new JButton("Start");
        cancelButton = new JButton("Cancel");

        startButton.addActionListener(this);
        cancelButton.addActionListener(this);

        buttonArea.add(startButton);
        buttonArea.add(cancelButton);

        this.add(selectionArea, BorderLayout.NORTH);
        this.add(buttonArea, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Start")) {
            this.setVisible(false);
            //If start as server mode
            if (Objects.equals(modeBox.getSelectedItem(), "Server")) {
                ServerMain mainWindow = new ServerMain("Server");
                mainWindow.setSize(500, 350);
                mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainWindow.setLocationRelativeTo(null);
                mainWindow.setResizable(false);
                mainWindow.setVisible(true);
            }
            //Start as client mode
            else {
                ClientMain mainWindow = new ClientMain("Server");
                mainWindow.setSize(500, 350);
                mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainWindow.setLocationRelativeTo(null);
                mainWindow.setResizable(false);
                mainWindow.setVisible(true);
            }
        }
        //Exit
        else if (e.getActionCommand().equals("Cancel")) {
            System.exit(0);
        }
    }

}




