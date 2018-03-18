package cnp2p;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConnectionHandler extends Thread {
    private final int BUFFER_SIZE = 32;
    private Socket connection;
    private int localPeerId;
    private int remotePeerId;
    private boolean incomingConnection;

    ConnectionHandler(Socket connection, int localPeerId) {
        this.connection = connection;
        this.localPeerId = localPeerId;
        incomingConnection = true;
    }

    ConnectionHandler(Socket connection, int localPeerId, int remotePeerId) {
        this.connection = connection;
        this.localPeerId = localPeerId;
        this.remotePeerId = remotePeerId;
        incomingConnection = false;
    }

    public void run() {
        byte[] buf = new byte[BUFFER_SIZE];
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(connection.getOutputStream());
            ObjectInputStream inputStream = new ObjectInputStream(connection.getInputStream());
            outputStream.flush();

            if (incomingConnection) {
                inputStream.read(buf);
                HandshakeMessage ihs = HandshakeMessage.parse(buf);
                if (ihs != null) {
                    HandshakeMessage ohs = new HandshakeMessage(remotePeerId);
                    outputStream.write(ohs.getBytes());
                    outputStream.flush();
                }
            } else {
                HandshakeMessage ohs = new HandshakeMessage(localPeerId);
                outputStream.write(ohs.getBytes());
                outputStream.flush();
                inputStream.read(buf);
                HandshakeMessage ihs = HandshakeMessage.parse(buf);
                if (ihs == null || ihs.getPeerId() != remotePeerId) {
                    return;
                }
            }


        } catch (IOException e) {


        }
    }
}
