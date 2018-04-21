package cnp2p;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class Logger extends Thread {
    private static final int QUEUE_CAPACITY = 500;

    private static Logger loggerInstance;
    private final BlockingQueue<String> messageQueue;
    private int peerID;
    private DateTimeFormatter formatter;
    private RandomAccessFile file;

    private Logger(int peerID, String directoryPath) {
        String logFileName;
        this.peerID = peerID;
        messageQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        logFileName = "log_peer_" + this.peerID + ".log";
        Path logFilePath = Paths.get(directoryPath, logFileName);
        formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
        try {
            file = new RandomAccessFile(logFilePath.toString(), "rw");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    static void createInstance(int peerID, String directoryPath) {
        loggerInstance = new Logger(peerID, directoryPath);
    }

    static Logger getInstance() {
        return loggerInstance;
    }

    private void putToQueue(String message) {
        try {
            messageQueue.put(message);
        } catch (InterruptedException ie) {
            return;
        }
    }

    void tcpConnectionEstablishedTo(int peerID) {
        String message = LocalDateTime.now().format(formatter) + ": Peer " + this.peerID
                + " makes a connection to Peer " + peerID + ".";
        putToQueue(message);
    }

    void tcpConnectionEstablishedFrom(int peerID) {
        String message = LocalDateTime.now().format(formatter) + ": Peer " + this.peerID
                + " makes a connection from Peer " + peerID + ".";
        putToQueue(message);
    }

    void preferredNeighborsChanged(int[] peerIDs) {
        StringBuilder message = new StringBuilder();
        message.append(LocalDateTime.now().format(formatter)).append(": Peer ").append(this.peerID)
                .append(" has the preferred neighbors ");
        for (int i = 0; i < peerIDs.length - 1; i++) {
            message.append(peerIDs[i]).append(", ");
        }
        message.append(peerIDs[peerIDs.length - 1]).append(".");
        putToQueue(message.toString());
    }

    void optUnchokedNeighborChanged(int peerID) {
        String message = LocalDateTime.now().format(formatter) + ": Peer " + this.peerID
                + " has the optimistically unchoked neighbor " + peerID;
        putToQueue(message);
    }

    void unchokedBy(int peerID) {
        String message = LocalDateTime.now().format(formatter) + ": Peer " + this.peerID + " is unchoked by " + peerID
                + ".";
        putToQueue(message);
    }

    void chokedBy(int peerID) {
        String message = LocalDateTime.now().format(formatter) + ": Peer " + this.peerID + " is choked by " + peerID
                + ".";
        putToQueue(message);
    }

    void receivedHaveFrom(int peerID, int pieceIndex) {
        String message = LocalDateTime.now().format(formatter) + ": Peer " + this.peerID
                + " received the \'have\' message from " + peerID + " for the piece " + pieceIndex + ".";
        putToQueue(message);
    }

    void receivedInterestedFrom(int peerID) {
        String message = LocalDateTime.now().format(formatter) + ": Peer " + this.peerID
                + " received the \'interested\' message from " + peerID + ".";
        putToQueue(message);
    }

    void receivedNotInterestedFrom(int peerID) {
        String message = LocalDateTime.now().format(formatter) + ": Peer " + this.peerID
                + " recieved the \'not interested\' message from " + peerID + ".";
        putToQueue(message);
    }

    void downloadedPieceFrom(int peerID, int pieceIndex, int numberOfPieces) {
        String message = LocalDateTime.now().format(formatter) + ": Peer " + this.peerID + " has downloaded the piece "
                + pieceIndex + " from " + peerID + ". Now the number of pieces it has is " + numberOfPieces + ".";
        putToQueue(message);
    }

    void downloadedFile() {
        String message = LocalDateTime.now().format(formatter) + ": Peer " + this.peerID
                + " has downloaded the complete file.";
        putToQueue(message);
    }

    @Override
    public void run() {
        while (true) {
            try {
                String msg = messageQueue.take();
                file.writeBytes(msg + System.lineSeparator());
            } catch (InterruptedException | IOException ie) {
                continue;
            }
        }
    }

}
