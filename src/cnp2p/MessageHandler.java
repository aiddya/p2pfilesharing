package cnp2p;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class MessageHandler extends Thread {
    Socket connection;
    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;
    int connIndex;

    MessageHandler(Socket connection, int connIndex) {
        this.connection = connection;
        this.connIndex = connIndex;
    }

    public void run() {
        try {
            outputStream = new ObjectOutputStream(connection.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(connection.getInputStream());
        } catch (Exception e) {
            
        }
    }
}
