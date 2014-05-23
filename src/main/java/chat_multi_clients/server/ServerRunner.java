package chat_multi_clients.server;

/**
 * Author: Daniel
 */
public class ServerRunner {
    /**
     * The appplication main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        new Server();
    }
}
