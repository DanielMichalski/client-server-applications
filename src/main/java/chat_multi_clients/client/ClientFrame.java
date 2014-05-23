package chat_multi_clients.client;

import chat_multi_clients.model.ChatMessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

/**
 * A simple Swing-based client for the chat_multi_clients server.  Graphically
 * it is a frame with a text field for entering messages and a
 * textarea to see the whole dialog.
 * <p/>
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */
public class ClientFrame extends JFrame implements Runnable {
    private PrintWriter out;

    private JTextField messageField;

    private JLabel playerNameLabel;

    private JTextArea messageArea;

    private String playerName;

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public ClientFrame() {
        setupFrame();
        initializeComponents();
    }

    private void setupFrame() {
        setTitle("Rozgrywka");
        setLocationRelativeTo(null);
        setSize(500, 250);
    }

    private void initializeComponents() {
        messageField = createTextField();
        messageArea = createMessageArea();
        playerNameLabel = new JLabel();

        add(messageField, BorderLayout.NORTH);
        add(new JScrollPane(messageArea), BorderLayout.CENTER);
        add(playerNameLabel, BorderLayout.SOUTH);
    }

    private JTextField createTextField() {
        messageField = new JTextField(40);
        messageField.setEditable(false);
        messageField.addActionListener(new TextFieldListener());
        return messageField;
    }

    private JTextArea createMessageArea() {
        messageArea = new JTextArea(8, 40);
        messageArea.setEditable(false);
        return messageArea;
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
                null,
                "Enter IP Address of the Server:",
                "127.0.0.1"
        );
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getPlayerName() {
        String name = null;
        boolean isOk = false;

        while (!isOk) {
            name = JOptionPane.showInputDialog(
                    null,
                    "Podaj imię gracza");

            if (name == null || name.equals("")) {
                JOptionPane.showMessageDialog(null, "Podano niepawidłową nazwę");
            } else {
                isOk = true;
            }
        }

        return name;
    }

    public void start() {
        new Thread(this).start();
    }

    /**
     * Responds to pressing the enter key in the textfield by sending
     * the contents of the text field to the server.    Then clear
     * the text area in preparation for the next message.
     */
    class TextFieldListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            out.println(playerName + messageField.getText());
            messageField.setText("");
        }
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    public void run() {

        try {
            // Make connection and initialize streams
            String serverAddress = getServerAddress();
            Socket socket = new Socket(serverAddress, 9001);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Process all messages from server, according to the protocol.
            while (true) {
                String line = in.readLine();
                if (line == null) continue;
                if (line.startsWith(ChatMessageType.SUBMIT_NAME + "")) {
                    playerName = getPlayerName();
                    out.println(playerName);
                } else if (line.startsWith(ChatMessageType.NAME_ACCEPTED + "")) {
                    playerNameLabel.setText("Login gracza: " + playerName);
                } else if (line.startsWith(ChatMessageType.MESSAGE + "")) {
                    messageArea.append(line.replace("MESSAGE", "") + "\n");
                } else if (line.startsWith(playerName +"")) {
                    messageField.setEditable(true);
                } else if (line.startsWith(ChatMessageType.EXIT + "")) {
                    dispose();
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Wystąpił problem z podłączeniem do serwera. " +
                            "Aplikacja zostanie zamknięta."
            );
            System.exit(-1);
        }
    }
}
