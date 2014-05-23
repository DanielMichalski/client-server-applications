package bridge_game.server;

import chat_multi_clients.model.ChatMessageType;
import bridge_game.model.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class Server {
    List<Player> players = new ArrayList<Player>();

    public static final int PORT = 8002;

    public static final int MAX_PLAYERS = 2;

    public int howManyConnectedPlayers = 0;

    private int currentPlayerNumber = 0;

    private int currentMaxPrice = 0;

    private List<PrintWriter> outputs = new ArrayList<PrintWriter>();

    Player currentPlayer;

    public static void main(String[] args) throws Exception {
        new Server();
    }

    public Server() throws IOException {
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(PORT);
            System.out.println("Serwer uruchumiony na porcie " + PORT);

            while (true) {
                Player player1 = new Player(listener.accept(), Position.NORTH, "Gracz 1");
                Player player2 = new Player(listener.accept(), Position.EAST, "Gracz 2");
//                Player player3 = new Player(listener.accept(), Position.SOUTH, "Gracz 3");
//                Player player4 = new Player(listener.accept(), Position.WEST, "Gracz 4");

                players.add(player1);
                players.add(player2);
//                players.add(player3);
//                players.add(player4);

                currentPlayer = player1;
                currentPlayerNumber = 0;

                player1.start();
                player2.start();
//                player3.start();
//                player4.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (listener != null) {
                listener.close();
            }
        }
    }

    public synchronized boolean checkPrice(String line, Player player) {
        try {
            int price = Integer.parseInt(line);
            if (player == currentPlayer) {
                if (price <= currentMaxPrice) {
                    sendMessageToAll(player.getPlayerName() + " podał zbyt niską kwotę: " + price + " zł");
                    return false;
                }
                sendMessageToAll(player.getPlayerName() + " zalicytował za " + line + " zł");
                currentMaxPrice = price;
                if (player.equals(players.get(players.size() - 1))) {
                    sendMessageToAll("Licytacja zakończona kwotą " + line + "zł");
                    sendMessageToAll("Koniec gry");
                    sendCommandToAll(MessageType.QUIT);
                } else {
                    currentPlayerNumber++;
                    currentPlayer = players.get(currentPlayerNumber);
                }
            }
        } catch (NumberFormatException e) {
            sendMessageToAll((player.getPlayerName() + " podał błędną kwotę " + line + " zł"));
            return false;
        }

        return true;
    }

    private void sendMessageToAll(String msg) {
        for (PrintWriter printWriter : outputs) {
            printWriter.println(ChatMessageType.MESSAGE + msg);
        }
    }

    private void sendCommandToAll(MessageType command) {
        for (PrintWriter printWriter : outputs) {
            printWriter.println(command);
        }
    }

    public enum Position {
        NORTH,
        EAST,
        SOUTH,
        WEST
    }

    /**
     * The class for the helper threads in this multithreaded server
     * application.  A Player is identified by a character mark
     * which is either 'X' or 'O'.  For communication with the
     * client the player has a socket with its input and output
     * streams.  Since only text is being communicated we use a
     * reader and a writer.
     */
    class Player extends Thread {

        String playerName;

        Socket socket;

        BufferedReader input;

        PrintWriter output;

        Position position;

        /**
         * Constructs a handler thread for a given socket and mark
         * initializes the stream fields, displays the first two
         * welcoming messages.
         */
        public Player(Socket socket, Position position, String playerName) {
            this.socket = socket;
            this.position = position;
            this.playerName = playerName;

            try {
                input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                outputs.add(output);
                checkIsToMuchPlayers();
                output.println(MessageType.WELCOME + playerName);
                output.println(ChatMessageType.MESSAGE + "Gracz podlaczony na pozycji " + position);
                howManyConnectedPlayers++;
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            }
        }

        private void checkIsToMuchPlayers() {
            if (howManyConnectedPlayers >= MAX_PLAYERS) {
                output.println(MessageType.FUll);
            }
        }

        /**
         * The run method of this thread.
         */
        public void run() {
            try {
                // The thread is only started after everyone connects.
                output.println(ChatMessageType.MESSAGE + "Wszyscy gracze podłączeni");

                if (currentPlayer == this) {
                    output.println(ChatMessageType.MESSAGE + "Twoja kolej na licytację");
                }

                // Repeatedly get commands from the client and process them.
                while (true) {

                    String line = input.readLine();
                    if (line.startsWith("MOVE")) {
                        if (currentPlayer == this) {
                            line = line.replaceFirst("MOVE", "");
                            checkPrice(line, this);
                        } else {
                            output.println(ChatMessageType.MESSAGE + "Teraz nie jest Twój ruch, a przeciwnika");
                        }

                    }
                }
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        public String getPlayerName() {
            return playerName;
        }
    }
}