package bridge_game.client;

import bridge_game.model.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientFrame extends JFrame {

    private JTextField messageField;

    private JTextArea messageArea;

    private static int PORT = 8002;

    private Socket socket;

    private BufferedReader in;

    private PrintWriter out;

    /**
     * Constructs the client by connecting to a server, laying out the
     * GUI and registering GUI listeners.
     */
    public ClientFrame(String serverAddress) throws Exception {
        setupFrame(serverAddress);
        initialize();
    }

    private void setupFrame(String serverAddress) throws IOException {
        setTitle("Rozgrywka");
        setLocationRelativeTo(null);
        setSize(500, 250);

        // Setup networking
        socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    private void initialize() {
        messageField = createTextField();
        messageArea = createMessageArea();

        getContentPane().add(messageField, BorderLayout.NORTH);
        getContentPane().add(messageArea, BorderLayout.CENTER);
    }

    private JTextField createTextField() {
        messageField = new JTextField(40);
        messageField.addActionListener(new TextFieldListener());
        return messageField;
    }

    private JTextArea createMessageArea() {
        messageArea = new JTextArea(8, 40);
        messageArea.setEditable(false);
        return messageArea;
    }


    class TextFieldListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            out.println(MessageType.MOVE + messageField.getText());
            messageField.setText("");
        }
    }

    /**
     * The main thread of the client will listen for messages
     * from the server.  The first message will be a "WELCOME"
     * message in which we receive our mark.  Then we go into a
     * loop listening for "VALID_MOVE", "OPPONENT_MOVED", "VICTORY",
     * "DEFEAT", "TIE", "OPPONENT_QUIT or "MESSAGE" messages,
     * and handling each message appropriately.  The "VICTORY",
     * "DEFEAT" and "TIE" ask the user whether or not to play
     * another game.  If the answer is no, the loop is exited and
     * the server is sent a "QUIT" message.  If an OPPONENT_QUIT
     * message is recevied then the loop will exit and the server
     * will be sent a "QUIT" message also.
     */
    public void play() throws Exception {
        String response;
        try {
            while (true) {
                response = in.readLine();
                if (response == null) continue;

                if (response.startsWith(MessageType.FUll + "")) {
                    JOptionPane.showMessageDialog(null, "Stół jest pełny");
                    dispose();
                } else if (response.startsWith(MessageType.WELCOME + "")) {
                    String playerName = response.replaceFirst("WELCOME", "");
                    setTitle("Gra w brydża - " + playerName);
                } else if (response.startsWith(MessageType.VALID_PRICE + "")) {
                    messageArea.append(response.replaceFirst("VALID_PRICE", "" + "\n"));
                } else if (response.startsWith(MessageType.MESSAGE + "")) {
                    messageArea.append(response.replaceFirst("MESSAGE", "") + "\n");
                } else if (response.startsWith(MessageType.QUIT + "")) {
                    messageField.setEditable(false);
                }
            }
        } finally {
            socket.close();
        }
    }

    /**
     * Runs the client as an application.
     */
    public static void main(String[] args) throws Exception {
        while (true) {
            String serverAddress = "localhost";
            ClientFrame frame = new ClientFrame(serverAddress);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setVisible(true);
            frame.setResizable(false);
            frame.play();
        }
    }
}