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

/**
 * Klasa reprezentująca ogko dla pojedyńczego gracza
 */
public class ClientFrame extends JFrame {

    /**
     * Pole tekstowe z ceną do licytacji
     */
    private JTextField messageField;

    /**
     * Obszar w którym będą wszystkie wiadomości od serwera
     */
    private JTextArea messageArea;

    /**
     * Port klienta
     */
    private static int PORT = 8002;

    /**
     * Socket klienta
     */
    private Socket socket;

    /**
     * Strumień wejściowy klienta
     */
    private BufferedReader in;

    /**
     * Strumień wyjściowy klienta
     */
    private PrintWriter out;

    /**
     * Kontruktor oknta klienta, który przyjmuje jako
     * parametr adres IP serwera
     */
    public ClientFrame(String serverAddress) throws Exception {
        setupFrame(serverAddress);
        initialize();
    }

    /**
     * Metoda ustawiająca ramkę
     *
     * @param serverAddress adres serwera
     * @throws IOException Może wyrzucić wyjątek jeśli
     *                     nie będzie mogła się podłaczyć do serwera
     */
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

    /**
     * Metoda inicjalizująca komponenty na oknie
     */
    private void initialize() {
        messageField = createTextField();
        messageArea = createMessageArea();

        getContentPane().add(messageField, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
    }

    /**
     * Metoda tworząca pole tekstowe z licytacją
     *
     * @return pole tekstowe z licytacją
     */
    private JTextField createTextField() {
        messageField = new JTextField(40);
        messageField.addActionListener(new TextFieldListener());
        return messageField;
    }

    /**
     * Tworzy obszar w którym będą wyświetlane wiadomości z serwera
     *
     * @return obszar na wiadomości z serwera
     */
    private JTextArea createMessageArea() {
        messageArea = new JTextArea(8, 40);
        messageArea.setEditable(false);
        return messageArea;
    }

    /**
     * Listener pola tekstoweg, który wykonuje akcję
     * po wciśnięciu klawisza ENTER
     */
    class TextFieldListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            out.println(MessageType.MOVE + messageField.getText());
            messageField.setText("");
        }
    }

    /**
     * W tej metodzie klient komunikuje się z serwerem
     * oraz przetwarza otrzymywane wyniki
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
                } else if (response.startsWith(MessageType.MESSAGE + "")) {
                    messageArea.append(response.replaceFirst("MESSAGE", "") + "\n");
                } else if (response.startsWith(MessageType.QUIT + "")) {
                    showEndGameMessageAndeExit();
                }
            }

        } finally {
            socket.close();
        }
    }

    private void showEndGameMessageAndeExit() {
        JOptionPane.showMessageDialog(
                null,
                "Gra się zakończyła");

        dispose();
    }

    /**
     * Główna metoda startowa klienta
     */
    public static void main(String[] args) throws Exception {
        String serverAddress = "localhost";
        ClientFrame frame = new ClientFrame(serverAddress);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.play();
    }
}