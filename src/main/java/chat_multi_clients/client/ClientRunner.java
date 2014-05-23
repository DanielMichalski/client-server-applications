package chat_multi_clients.client;

import java.awt.*;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

/**
 * Author: Daniel
 */
public class ClientRunner {
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ClientFrame frame = new ClientFrame();
                frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
                frame.setVisible(true);
                frame.start();
            }
        });
    }
}
