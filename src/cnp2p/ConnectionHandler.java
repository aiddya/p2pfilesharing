package cnp2p;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConnectionHandler extends Thread {
    final int BUFFER_SIZE = 32;
    private Socket connection;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private int remotePeerId;
    Boolean incomingConnection;

    ConnectionHandler(Socket connection) {
        this.connection = connection;
        this.remotePeerId = remotePeerId;
        incomingConnection = true;
    }

    ConnectionHandler(Socket connection, int remotePeerId) {
        this.connection = connection;
        this.remotePeerId = remotePeerId;
        incomingConnection = false;
    }

    public void run() {
        byte[] buf = new byte[BUFFER_SIZE];
        try {
            outputStream = new ObjectOutputStream(connection.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(connection.getInputStream());

            inputStream.read(buf);
            HandshakeMessage msg = HandshakeMessage.parse(buf);
            if (msg != null) {
                HandshakeMessage reply = new HandshakeMessage(remotePeerId);
            }
        } catch (Exception e) {

        }
    }
}
