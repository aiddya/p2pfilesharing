package cnp2p;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static cnp2p.ChokeStatus.*;
import static cnp2p.MessageType.*;

public class ConnectionHandler extends Thread {
    private static final int QUEUE_CAPACITY = 50;

    private final BlockingQueue<Message> messageQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private Socket connection;
    private int localPeerId;
    private int remotePeerId;
    private boolean incomingConnection;
    private boolean remoteInterested;
    private boolean localInterested;
    private ChokeStatus localStatus;
    private ChokeStatus remoteStatus;
    private int downloadRate;

    ConnectionHandler(Socket connection, int localPeerId) {
        this.connection = connection;
        this.localPeerId = localPeerId;
        incomingConnection = true;
        localStatus = UNKNOWN;
        remoteStatus = UNKNOWN;
        downloadRate = 0;
    }

    ConnectionHandler(Socket connection, int localPeerId, int remotePeerId) {
        this.connection = connection;
        this.localPeerId = localPeerId;
        this.remotePeerId = remotePeerId;
        incomingConnection = false;
        localStatus = UNKNOWN;
        remoteStatus = UNKNOWN;
        downloadRate = 0;
    }

    int getRemotePeerId() {
        return remotePeerId;
    }

    boolean isRemoteInterested() {
        return remoteInterested;
    }

    ChokeStatus getRemoteStatus() {
        return remoteStatus;
    }

    void setRemoteStatus(ChokeStatus newStatus) {
        remoteStatus = newStatus;
    }

    int getDownloadRate() {
        return downloadRate;
    }

    void resetDownloadRate() {
        this.downloadRate = 0;
    }

    void addMessage(Message msg) {
        try {
            messageQueue.put(msg);
        } catch (InterruptedException ie) {
            ie.getMessage();
        }
    }

    private Message updateInterestedStatus() {
        if (Tracker.getInstance().getNewRandomPieceNumber(remotePeerId, false) != -1) {
            if (!localInterested) {
                localInterested = true;
                return new Message(INTERESTED);
            }
        } else if (localInterested) {
            localInterested = false;
            return new Message(NOT_INTERESTED);
        }
        return null;
    }

    private Message sendInterestedStatus() {
        if (Tracker.getInstance().getNewRandomPieceNumber(remotePeerId, false) != -1) {
            localInterested = true;
            return new Message(INTERESTED);
        } else {
            localInterested = false;
            return new Message(NOT_INTERESTED);
        }
    }

    private void logInterested(MessageType interested) {
        if (interested == INTERESTED) {
            remoteInterested = true;
            Logger.getInstance().receivedInterestedFrom(remotePeerId);
        } else if (interested == NOT_INTERESTED) {
            remoteInterested = false;
            Logger.getInstance().receivedNotInterestedFrom(remotePeerId);
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
                Message outgoingBitField = new Message(BITFIELD, Tracker.getInstance().getBitField());
                outputStream.writeObject(outgoingBitField);
                outputStream.flush();

                Message iint = (Message) inputStream.readObject();
                logInterested(iint.getType());
                Message oint = sendInterestedStatus();
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
                Message outgoingBitField = new Message(BITFIELD, Tracker.getInstance().getBitField());
                outputStream.writeObject(outgoingBitField);
                outputStream.flush();
                Message incomingBitField = (Message) inputStream.readObject();
                Tracker.getInstance().setPeerBitField(remotePeerId, incomingBitField.getPayload());

                Message oint = sendInterestedStatus();
                outputStream.writeObject(oint);
                outputStream.flush();
                Message iint = (Message) inputStream.readObject();
                logInterested(iint.getType());
            }

            Thread receiverThread = new Thread(() -> {
                Message msg, newMessage;
                int pieceIndex;
                while (true) {
                    try {
                        msg = (Message) inputStream.readObject();
                    } catch (EOFException | SocketException e) {
                        System.out.println("Connection ended");
                        return;
                    } catch (Exception e) {
                        System.out.println("Encountered an error while receiving a message");
                        e.printStackTrace();
                        continue;
                    }

                    switch (msg.getType()) {
                        case CHOKE:
                            Logger.getInstance().chokedBy(remotePeerId);
                            localStatus = CHOKED;
                            Tracker.getInstance().clearReqBitField(remotePeerId);
                            break;
                        case UNCHOKE:
                            Logger.getInstance().unchokedBy(remotePeerId);
                            localStatus = UNCHOKED;
                            pieceIndex = Tracker.getInstance().getNewRandomPieceNumber(remotePeerId, true);
                            if (pieceIndex != -1) {
                                newMessage = new Message(REQUEST, pieceIndex);
                                addMessage(newMessage);
                            }
                            break;
                        case HAVE:
                            Logger.getInstance().receivedHaveFrom(remotePeerId, msg.getIndex());
                            Tracker.getInstance().setPeerHasPiece(remotePeerId, msg.getIndex());
                            newMessage = updateInterestedStatus();
                            if (newMessage != null) {
                                addMessage(newMessage);
                            }
                            break;
                        case BITFIELD:
                            Tracker.getInstance().setPeerBitField(remotePeerId, msg.getPayload());
                            newMessage = updateInterestedStatus();
                            if (newMessage != null) {
                                addMessage(newMessage);
                            }
                            break;
                        case INTERESTED:
                            remoteInterested = true;
                            Logger.getInstance().receivedInterestedFrom(remotePeerId);
                            break;
                        case NOT_INTERESTED:
                            remoteInterested = false;
                            Logger.getInstance().receivedNotInterestedFrom(remotePeerId);
                            break;
                        case REQUEST:
                            if (remoteStatus == UNCHOKED) {
                                byte[] piece = Tracker.getInstance().getPiece(msg.getIndex());
                                newMessage = new Message(PIECE, msg.pieceIndex, piece);
                                addMessage(newMessage);
                            }
                            break;
                        case PIECE:
                            Tracker.getInstance().putPiece(msg.getIndex(), msg.getPayload());
                            Tracker.getInstance().setBit(msg.getIndex());
                            Tracker.getInstance().unsetPieceRequested(msg.getIndex(), remotePeerId);
                            Logger.getInstance().downloadedPieceFrom(remotePeerId, msg.getIndex(),
                                    Tracker.getInstance().getNumberPieces());
                            if (Tracker.getInstance().isFileComplete()) {
                                Logger.getInstance().downloadedFile();
                            } else if (localStatus == UNCHOKED) {
                                pieceIndex = Tracker.getInstance().getNewRandomPieceNumber(remotePeerId, true);
                                if (pieceIndex != -1) {
                                    newMessage = new Message(REQUEST, pieceIndex);
                                    addMessage(newMessage);
                                }
                            }
                            for (ConnectionHandler connection : Tracker.getInstance().getConnectionHandlerList()) {
                                connection.addMessage(new Message(HAVE, msg.getIndex()));
                            }
                            downloadRate++;
                            break;
                    }
                }
            });

            receiverThread.start();
            Message message;
            boolean connectionAlive = true;
            while (connectionAlive) {
                message = messageQueue.take();

                try {
                    outputStream.writeObject(message);
                    outputStream.flush();
                } catch (SocketException se) {
                    connectionAlive = false;
                    System.out.println("Connection ended from peer " + localPeerId + " to " + remotePeerId + ": "
                            + se.getMessage());
                } catch (Exception e) {
                    System.out.println("Encountered an error while sending a message");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("Encountered an error while communicating with a peer");
            e.printStackTrace();
        }
    }
}
