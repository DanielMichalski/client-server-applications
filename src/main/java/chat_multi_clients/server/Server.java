package chat_multi_clients.server;

import chat_multi_clients.model.ChatMessageType;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.TreeMap;

/**
 * A multithreaded chat_multi_clients room server.  When a client connects the
 * server requests a screen playerName by sending the client the
 * text "SUBMITNAME", and keeps requesting a playerName until
 * a unique one is received.  After a client submits a unique
 * playerName, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen playerName.  The
 * broadcast messages are prefixed with "MESSAGE ".
 * <p/>
 * Because this is just a teaching example to illustrate a simple
 * chat_multi_clients server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 * <p/>
 * 1. The protocol should be enhanced so that the client can
 * send clean disconnect messages to the server.
 * <p/>
 * 2. The server should do some logging.
 */
public class Server {

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 9001;

    /**
     * Max players
     */
    private static final int MAX_PLAYERS = 2;

    /**
     * The set of all names of clients in the chat_multi_clients room.  Maintained
     * so that we can check that new clients are not registering playerName
     * already in use.
     */
    private static final Map<String, PrintWriter> players = new TreeMap<String, PrintWriter>();

    private static final Map<String, Integer> playersPrices = new TreeMap<String, Integer>();

    private static boolean isGameStarted = false;

//    private static final HashSet<String> names = new HashSet<String>();

    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
//    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */

    public Server() {
        System.out.println("Serwer został uruchomiony na porcie: " + PORT);

        ServerSocket socket = null;
        try {
            socket = new ServerSocket(PORT);
            //noinspection InfiniteLoopStatement
            while (true) {
                new Handler(socket.accept()).start();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Wybrany port jest już zajęty. Nr portu: " + PORT);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class Handler extends Thread {

        private Socket socket;

        private BufferedReader in;

        private PrintWriter out;


        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen playerName until a unique one has been submitted, then
         * acknowledges the playerName and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a playerName from this client.  Keep requesting until
                // a playerName is submitted that is not already used.  Note that
                // checking for the existence of a playerName and adding the playerName
                // must be done while locking the set of names.

                waitForPlayer();

                while (players.size() < MAX_PLAYERS) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (!isGameStarted) {
                    sendMessageToAll("Gra rozpoczęta");
                    isGameStarted = true;
                }

                startGame();


//                AcceptMessagesAndBroadcast();

            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its playerName and its print
                // writer from the sets, and close its socket.
//                if (playerName != null) {
//                    players.remove(playerName);
//                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        private void waitForPlayer() throws IOException {
            String playerName;

            while (true) {
                out.println(ChatMessageType.SUBMIT_NAME);
                playerName = in.readLine();
                if (playerName == null) {
                    return;
                }
                synchronized (players) {
                    if (!players.containsKey(playerName)) {
                        out.println(ChatMessageType.NAME_ACCEPTED);
                        players.put(playerName, out);
                        break;
                    }
                }
            }

            if (players.size() > MAX_PLAYERS) {
                showMessageToMuchPlayers();
                out.println(ChatMessageType.EXIT);
                return;
            }

            sendMessagePlayerConnected(playerName, players.size() - 1);

        }

        private void startGame() {
            synchronized (players) {
                for (String name : players.keySet()) {
                    playersPrices.put(name, getPlayerPrice(name));
                    System.out.println(name + " " +playersPrices);
                }
            }

            System.out.println("Koniec");
        }

        private int getPlayerPrice(String playerName) {
            String line;
            int price;

            sendMessageToAll("Teraz licytuje gracz: " + playerName);

            while (true) {
                try {
                    players.get(playerName).println(playerName);

                    line = in.readLine();
                    if (line == null) {
                        continue;
                    }
                    if (line.startsWith(playerName)) {
                        price = Integer.parseInt(line);
                        playersPrices.put(playerName, price);
                    } else {
                        out.println(ChatMessageType.MESSAGE + "Teraz nie jest Twoja kolej. Powinien licytować gracz: " + playerName);
                        continue;
                    }

                } catch (IOException e) {
                    continue;
                }

                for (PrintWriter writer : players.values()) {
                    writer.println(ChatMessageType.MESSAGE + "Gracz " + playerName + " podał kwotę: " + line.replaceFirst(playerName, ""));
                }

                break;
            }

            return price;
        }

        private void sendCommandToAll(ChatMessageType startGame) {
            for (PrintWriter writer : players.values()) {
                writer.println(ChatMessageType.START_GAME);
            }
        }

        public void sendMessageToAll(String text) {
            for (PrintWriter writer : players.values()) {
                writer.println(ChatMessageType.MESSAGE + text);
            }
        }


//        private void AcceptMessagesAndBroadcast() throws IOException {
//            // Accept messages from this client and broadcast them.
//            // Ignore other clients that cannot be broadcasted to.
//            while (true) {
//                String input = in.readLine();
//                if (input == null) {
//                    return;
//                }
//                for (PrintWriter writer : players.values()) {
//                    writer.println(MessageType.MESSAGE + playerName + ": " + input);
//                }
//            }
//        }


        private void sendMessagePlayerConnected(String playerName, int position) {
            for (PrintWriter writer : players.values()) {
                Position[] positions = Position.values();
                writer.println(ChatMessageType.MESSAGE
                        + "Gracz "
                        + playerName
                        + " podłączył się do gry i gra na pozycji "
                        + positions[position]);
            }
        }


        private void showMessageToMuchPlayers() {
            JOptionPane.showMessageDialog(null, "Stół jest już pełny");
        }
    }

    enum Position {
        NORTH,
        EAST,
        SOUTH,
        WEST
    }
}