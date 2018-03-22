package cnp2p;

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
                    remotePeerId = ihs.getPeerId();
                    HandshakeMessage ohs = new HandshakeMessage(localPeerId);
                    outputStream.write(ohs.getBytes());
                    outputStream.flush();
                } else {
                    System.out.println("Malformed handshake message");
                    return;
                }

                Logger.getInstance().tcpConnectionEstablishedFrom(remotePeerId);
                Message incomingBitField;
                incomingBitField = (Message) inputStream.readObject();
                Tracker.getInstance().setPeerBitField(remotePeerId, incomingBitField.getPayload());
                Message outgoingBitField = new Message(MessageType.BITFIELD, Tracker.getInstance().getBitField());
                outputStream.writeObject(outgoingBitField);
                outputStream.flush();

                Message iint = (Message) inputStream.readObject();
                if (iint.getType() == MessageType.INTERESTED) {
                    Logger.getInstance().receivedInterestedFrom(remotePeerId);
                } else if (iint.getType() == MessageType.NOT_INTERESTED) {
                    Logger.getInstance().receivedNotInterestedFrom(remotePeerId);
                }
                Message oint;
                if (Tracker.getInstance().getNewRandomPieceNumber(remotePeerId) != -1) {
                    oint = new Message(MessageType.INTERESTED);
                } else {
                    oint = new Message(MessageType.NOT_INTERESTED);
                }
                outputStream.writeObject(oint);
                outputStream.flush();
            } else {
                HandshakeMessage ohs = new HandshakeMessage(localPeerId);
                outputStream.write(ohs.getBytes());
                outputStream.flush();
                inputStream.readFully(buf);
                HandshakeMessage ihs = HandshakeMessage.parse(buf);
                if (ihs == null || ihs.getPeerId() != remotePeerId) {
                    System.out.println("Malformed handshake message");
                    return;
                }

                Logger.getInstance().tcpConnectionEstablishedTo(remotePeerId);
                Message outgoingBitField = new Message(MessageType.BITFIELD, Tracker.getInstance().getBitField());
                outputStream.writeObject(outgoingBitField);
                outputStream.flush();
                Message incomingBitField = (Message) inputStream.readObject();
                Tracker.getInstance().setPeerBitField(remotePeerId, incomingBitField.getPayload());

                Message oint;
                if (Tracker.getInstance().getNewRandomPieceNumber(remotePeerId) != -1) {
                    oint = new Message(MessageType.INTERESTED);
                } else {
                    oint = new Message(MessageType.NOT_INTERESTED);
                }
                outputStream.writeObject(oint);
                outputStream.flush();
                Message iint = (Message) inputStream.readObject();
                if (iint.getType() == MessageType.INTERESTED) {
                    Logger.getInstance().receivedInterestedFrom(remotePeerId);
                } else if (iint.getType() == MessageType.NOT_INTERESTED) {
                    Logger.getInstance().receivedNotInterestedFrom(remotePeerId);
                }
            }
            Thread.sleep(600000);

        } catch (Exception e) {
            System.out.println("Encountered an error while communicating with a peer");
            e.printStackTrace();
        }
    }
}
