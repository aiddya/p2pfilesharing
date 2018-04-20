package cnp2p;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static cnp2p.ChokeStatus.CHOKED;
import static cnp2p.ChokeStatus.UNCHOKED;

public class ConnectionHandler extends Thread {
    private static final int QUEUE_CAPACITY = 50;

    private final BlockingQueue<Message> messageQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private Socket connection;
    private int localPeerId;
    private int remotePeerId;
    private boolean incomingConnection;
    private boolean remoteInterested;
    private ChokeStatus localStatus;
    private ChokeStatus remoteStatus;
    private double downloadRate = 0;

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

    public int getRemotePeerId() {
        return remotePeerId;
    }

    boolean isRemoteInterested() {
        return remoteInterested;
    }

    ChokeStatus getRemoteStatus() {
        return remoteStatus;
    }

    double getDownloadRate() {
        return downloadRate;
    }

    void addMessage(Message msg) {
        try {
            messageQueue.put(msg);
        } catch (InterruptedException ie) {

        }
    }

    public void run() {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(connection.getOutputStream());
            ObjectInputStream inputStream = new ObjectInputStream(connection.getInputStream());
            outputStream.flush();

            if (incomingConnection) {
                HandshakeMessage ihs = (HandshakeMessage) inputStream.readObject();
                if (ihs != null) {
                    remotePeerId = ihs.getPeerId();
                    HandshakeMessage ohs = new HandshakeMessage(localPeerId);
                    outputStream.writeObject(ohs);
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
                if (Tracker.getInstance().getNewRandomPieceNumber(remotePeerId, false) != -1) {
                    oint = new Message(MessageType.INTERESTED);
                } else {
                    oint = new Message(MessageType.NOT_INTERESTED);
                }
                outputStream.writeObject(oint);
                outputStream.flush();
            } else {
                HandshakeMessage ohs = new HandshakeMessage(localPeerId);
                outputStream.writeObject(ohs);
                outputStream.flush();
                HandshakeMessage ihs = (HandshakeMessage) inputStream.readObject();
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
                if (Tracker.getInstance().getNewRandomPieceNumber(remotePeerId, false) != -1) {
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

            Thread receiverThread = new Thread(() -> {
                Message msg, newMessage;
                int pieceIndex;
                while (true) {
                    try {
                        msg = (Message) inputStream.readObject();
                        switch (msg.getType()) {
                            case CHOKE:
                                Logger.getInstance().chokedBy(remotePeerId);
                                localStatus = CHOKED;
                                break;
                            case UNCHOKE:
                                Logger.getInstance().unchokedBy(remotePeerId);
                                localStatus = ChokeStatus.UNCHOKED;
                                pieceIndex = Tracker.getInstance().getNewRandomPieceNumber(remotePeerId, true);
                                if (pieceIndex != -1) {
                                    newMessage = new Message(MessageType.REQUEST, pieceIndex);
                                    messageQueue.put(newMessage);
                                }
                                break;
                            case HAVE:
                                Logger.getInstance().receivedHaveFrom(remotePeerId, msg.getIndex());
                                Tracker.getInstance().setPeerHasPiece(remotePeerId, msg.getIndex());
                                if (Tracker.getInstance().getNewRandomPieceNumber(remotePeerId, false) != -1) {
                                    newMessage = new Message(MessageType.INTERESTED);
                                } else {
                                    newMessage = new Message(MessageType.NOT_INTERESTED);
                                }
                                messageQueue.put(newMessage);
                                break;
                            case BITFIELD:
                                Tracker.getInstance().setPeerBitField(remotePeerId, msg.getPayload());
                                if (Tracker.getInstance().getNewRandomPieceNumber(remotePeerId, false) != -1) {
                                    newMessage = new Message(MessageType.INTERESTED);
                                } else {
                                    newMessage = new Message(MessageType.NOT_INTERESTED);
                                }
                                messageQueue.put(newMessage);
                                break;
                            case INTERESTED:
                                Logger.getInstance().receivedInterestedFrom(remotePeerId);
                                remoteInterested = true;
                                break;
                            case NOT_INTERESTED:
                                Logger.getInstance().receivedNotInterestedFrom(remotePeerId);
                                remoteInterested = false;
                                break;
                            case REQUEST:
                                if (localStatus == UNCHOKED) {
                                    newMessage = new Message(MessageType.PIECE, Tracker.getInstance().getPiece(msg.getIndex()));
                                    messageQueue.put(newMessage);
                                }
                                break;
                            case PIECE:
                                Tracker.getInstance().putPiece(msg.getIndex(), msg.getPayload());
                                Tracker.getInstance().setBit(msg.getIndex());
                                for (ConnectionHandler connection : Tracker.getInstance().getConnectionHandlerList()) {
                                    connection.addMessage(new Message(MessageType.HAVE, msg.getIndex()));
                                }
                                downloadRate++;
                                Logger.getInstance().downloadedPieceFrom(remotePeerId, msg.getIndex(), Tracker.getInstance().getNumberPieces());
                                if (localStatus == UNCHOKED) {
                                    pieceIndex = Tracker.getInstance().getNewRandomPieceNumber(remotePeerId, true);
                                    if (pieceIndex != -1) {
                                        newMessage = new Message(MessageType.REQUEST, pieceIndex);
                                        messageQueue.put(newMessage);
                                    }
                                }
                                break;
                        }
                    } catch (Exception ex) {

                    }
                }
            });

            receiverThread.start();

            Message message;
            while (true) {
                message = messageQueue.take();
                switch (message.getType()) {
                    case CHOKE:
                        remoteStatus = CHOKED;
                        outputStream.writeObject(message);
                        break;
                    case UNCHOKE:
                        remoteStatus = UNCHOKED;
                        outputStream.writeObject(message);
                        break;
                    case INTERESTED:
                        outputStream.writeObject(message);
                        break;
                    case NOT_INTERESTED:
                        outputStream.writeObject(message);
                        break;
                    case HAVE:
                    case BITFIELD:
                    case REQUEST:
                    case PIECE:
                        outputStream.writeObject(message);
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Encountered an error while communicating with a peer");
            e.printStackTrace();
        }
    }
}
