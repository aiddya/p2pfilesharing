package cnp2p;

import javax.sound.midi.Track;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static cnp2p.ChokeStatus.CHOKED;
import static cnp2p.ChokeStatus.UNCHOKED;

public class ConnectionHandler extends Thread {
    public static final int QUEUE_CAPACITY = 50;

    private final int BUFFER_SIZE = 32;
    private Socket connection;
    private int localPeerId;
    private int remotePeerId;
    private boolean incomingConnection;
    private boolean remoteInterested;

    public ChokeStatus getMyStatus() {
        return myStatus;
    }

    public void setMyStatus(ChokeStatus myStatus) {
        this.myStatus = myStatus;
    }

    public ChokeStatus getPeerStatus() {
        return peerStatus;
    }

    public void setPeerStatus(ChokeStatus peerStatus) {
        this.peerStatus = peerStatus;
    }

    public double getDownloadRate() {
        return downloadRate;
    }

    public void setDownloadRate(double downloadRate) {
        this.downloadRate = downloadRate;
    }

    private ChokeStatus myStatus;
    private ChokeStatus peerStatus;
    private double downloadRate = 0;
    private final BlockingQueue<Message> messageQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

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

    void addMessage(Message msg){
        try {
            messageQueue.put(msg);
        }catch(InterruptedException ie){

        }
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

            Thread receiverThread = new Thread(() -> {
                Message msg, newMessage;
                int pieceIndex;
                while (true) {
                    try {
                        msg = (Message) inputStream.readObject();
                        switch (msg.getType()) {
                            case CHOKE:
                                myStatus = CHOKED;
                                break;
                            case UNCHOKE:
                                myStatus = ChokeStatus.UNCHOKED;
                                pieceIndex = Tracker.getInstance().getNewRandomPieceNumber(remotePeerId);
                                if(pieceIndex != -1) {
                                    Tracker.getInstance().setPieceRequested(pieceIndex);
                                    newMessage = new Message(MessageType.REQUEST, pieceIndex);
                                    messageQueue.put(newMessage);
                                }
                                break;
                            case HAVE:
                                Tracker.getInstance().setPeerHasPiece(remotePeerId, msg.getIndex());
                                if (Tracker.getInstance().getNewRandomPieceNumber(remotePeerId) != -1) {
                                    newMessage = new Message(MessageType.INTERESTED);
                                } else {
                                    newMessage = new Message(MessageType.NOT_INTERESTED);
                                }
                                messageQueue.put(newMessage);
                                break;
                            case BITFIELD:
                                Tracker.getInstance().setPeerBitField(remotePeerId, msg.getPayload());
                                if (Tracker.getInstance().getNewRandomPieceNumber(remotePeerId) != -1) {
                                    newMessage = new Message(MessageType.INTERESTED);
                                } else {
                                    newMessage = new Message(MessageType.NOT_INTERESTED);
                                }
                                messageQueue.put(newMessage);
                                break;
                            case INTERESTED:
                                remoteInterested = true;
                                break;
                            case NOT_INTERESTED:
                                remoteInterested = false;
                                break;
                            case REQUEST:
                                if(myStatus == UNCHOKED) {
                                    newMessage = new Message(MessageType.PIECE, Tracker.getInstance().getPiece(msg.getIndex()));
                                    messageQueue.put(newMessage);
                                }
                                break;
                            case PIECE:
                                Tracker.getInstance().putPiece(msg.getIndex(), msg.getPayload());
                                Tracker.getInstance().setBit(msg.getIndex());
                                for(ConnectionHandler connection : Tracker.getInstance().getConnectionHandlerList()){
                                    connection.addMessage(new Message(MessageType.HAVE, msg.getIndex()));
                                }
                                if(myStatus == UNCHOKED){
                                    pieceIndex = Tracker.getInstance().getNewRandomPieceNumber(remotePeerId);
                                    if(pieceIndex != -1) {
                                        Tracker.getInstance().setPieceRequested(pieceIndex);
                                        newMessage = new Message(MessageType.REQUEST, pieceIndex);
                                        messageQueue.put(newMessage);
                                    }
                                }
                                break;
                        }
                    }catch(Exception ex){

                    }
                }
            });

            receiverThread.start();

            Message message;
            while(true){
                message = messageQueue.take();
                switch(message.getType()){
                    case CHOKE:
                    case UNCHOKE:
                    case HAVE:
                    case BITFIELD:
                    case INTERESTED:
                    case NOT_INTERESTED:
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
