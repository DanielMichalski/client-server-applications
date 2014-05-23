package bridge_game.server;

import bridge_game.model.MessageType;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Klasa serwera odpowiedzialna za komunikację z klientami
 * wykorzystująć Sockety i tokeny jako typy wiadomości
 */
public class Server {

    /**
     * Port serwera
     */
    public static final int PORT = 8002;

    /**
     * Maksymalna ilość graczy
     */
    public static final int MAX_PLAYERS = 4;

    /**
     * Obecnie najwyższa kwota licytacji
     */
    private int currentMaxPrice = 0;

    /**
     * Mapa zawierąca nazwę gracza oraz obiekt printWriter
     */
    private Map<String, PrintWriter> outputs = new TreeMap<String, PrintWriter>();

    /**
     * Gracze ktrzy jeszcze nie licytowali
     */
    private List<Player> playersWhoDidNotBet = new ArrayList<Player>();

    /**
     * Gracz który może obecnie licytować
     */
    Player currentPlayer;

    /**
     * Metoda startowa serwera
     */
    public static void main(String[] args) throws Exception {
        new Server();
    }

    /**
     * Kontruktor serwera uruchamiany przy starcie serwera
     */
    public Server() throws IOException {
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(PORT);
            System.out.println("Serwer uruchumiony na porcie " + PORT);

            while (true) {
                Player player1 = new Player(listener.accept(), Position.NORTH, "Gracz 1");
                Player player2 = new Player(listener.accept(), Position.EAST, "Gracz 2");
                Player player3 = new Player(listener.accept(), Position.SOUTH, "Gracz 3");
                Player player4 = new Player(listener.accept(), Position.WEST, "Gracz 4");

                currentPlayer = playersWhoDidNotBet.get(0);

                player1.start();
                player2.start();
                player3.start();
                player4.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (listener != null) {
                listener.close();
            }
        }
    }

    /**
     * Metoda sprawdzająca czy gracz podał prawidłową kwotę
     *
     * @param line   kwota podana przez gracza typu String
     * @param player gracz który licytuje
     * @return prawde jeśli kwota jest prawidłowa, fałsz
     * w przeciwnym przypadku
     */
    public synchronized boolean checkPrice(String line, Player player) {
        try {
            int price = Integer.parseInt(line);
            if (player == currentPlayer) {
                if (price <= currentMaxPrice) {
                    sendMessageToAll(player.getPlayerName() + " podał zbyt niską kwotę: " + price + " zł");
                    return false;
                }
                sendMessageToAll(player.getPlayerName() + " zalicytował za " + line + " zł");

                playersWhoDidNotBet.remove(0);

                if (playersWhoDidNotBet.size() > 0) {
                    currentPlayer = playersWhoDidNotBet.get(0);

                    currentMaxPrice = price;
                } else {
                    sendMessageToAll("Licytacja zakończona kwotą " + line + "zł");
                    sendMessageToAll("Koniec gry");
                    sendCommandToAll(MessageType.QUIT);
                }


            }
        } catch (NumberFormatException e) {
            sendMessageToAll((player.getPlayerName() + " podał błędną kwotę " + line + " zł"));
            return false;
        }

        return true;
    }

    /**
     * Wysyła wiadomość do podłączonych klientów
     *
     * @param msg wiadomość do wysłania
     */
    private void sendMessageToAll(String msg) {
        for (PrintWriter printWriter : outputs.values()) {
            printWriter.println(MessageType.MESSAGE + msg);
        }
    }

    /**
     * Wysyła komende do klientów
     *
     * @param command komenda do wysłania
     */
    private void sendCommandToAll(MessageType command) {
        for (PrintWriter printWriter : outputs.values()) {
            printWriter.println(command);
        }
    }

    /**
     * Pozycje graczy jakie mogą przyjąć przy stole
     */
    public enum Position {
        NORTH,
        EAST,
        SOUTH,
        WEST
    }

    /**
     * Klasa wątku, która reprezentuje pojedyńczego gracza.
     * Każdy z graczy ma swój socket, nazwę, czy chociażby
     * pozycję przy stole
     */
    class Player extends Thread {

        /**
         * Imię gracza
         */
        private String playerName;

        /**
         * Socket klienta
         */
        private Socket socket;

        /**
         * Odczyt buforowany
         */
        private BufferedReader input;

        /**
         * Strumień wyjścia do klienta
         */
        private PrintWriter output;

        /**
         * Pozycja gracza przy stole
         */
        private Position position;

        /**
         * Konstruktor gracza, który przygotowuje go do gry
         *
         * @param socket     socket klienta
         * @param position   pozycja przy stole
         * @param playerName imię gracza
         */
        public Player(Socket socket, Position position, String playerName) {
            this.socket = socket;
            this.position = position;
            this.playerName = playerName;

            try {
                input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                outputs.put(playerName, output);
                playersWhoDidNotBet.add(this);
                checkIsToMuchPlayers();
                output.println(MessageType.WELCOME + playerName);
                sendMessageToAll(playerName + " dołączył do gry i gra na pozycji " + position);
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            }
        }

        /**
         * Sprawdza czy nie podłączyło się za dużo klientów
         */
        private void checkIsToMuchPlayers() {
            if (outputs.size() > MAX_PLAYERS) {
                output.println(MessageType.FUll);
            }
        }

        /**
         * Glówna metoda wątku wykorzystywana w rozgrywce.
         * Tu jest zawarta logika rozgrywki
         */
        public void run() {
            try {
                output.println(MessageType.MESSAGE + "Wszyscy gracze podłączeni");

                if (currentPlayer == this) {
                    output.println(MessageType.MESSAGE + "Twoja kolej na licytację");
                }

                while (true) {
                    String line = input.readLine();

                    if (line.startsWith("MOVE")) {
                        if (currentPlayer == this) {
                            line = line.replaceFirst("MOVE", "");
                            checkPrice(line, this);
                        } else {
                            output.println(MessageType.MESSAGE + "Teraz nie jest Twój ruch, a przeciwnika");
                        }

                    }
                }
            } catch (IOException e) {
                sendMessageToAll(playerName + " opuścił grę");
                outputs.remove(playerName);
                playersWhoDidNotBet.remove(this);
                if (outputs.size() == 0) {
                    JOptionPane.showMessageDialog(null, "Wszyscy gracze wyszli z pokoju.");
                }
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        /**
         * @return zwraca imię gracza
         */
        public String getPlayerName() {
            return playerName;
        }
    }
}