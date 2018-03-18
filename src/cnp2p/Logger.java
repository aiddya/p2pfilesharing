package cnp2p;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {

    private int peerID;
    private Path logFilePath;
    private Calendar calendar;
    private SimpleDateFormat simpleDateFormat;
    private PrintWriter printWriter;

    Logger(int peerID, String directoryPath){
        String logFileName;
        this.peerID = peerID;
        logFileName = "log_peer_" + this.peerID + ".log";
        logFilePath = Paths.get(directoryPath, logFileName);
        calendar = Calendar.getInstance();
        simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        try {
            OpenOption[] options = new OpenOption[]{StandardOpenOption.APPEND, StandardOpenOption.SYNC};
            BufferedWriter bufferedWriter = Files.newBufferedWriter(logFilePath,
                    StandardCharsets.UTF_8, options);
            printWriter = new PrintWriter(bufferedWriter);
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    private void writeToFile(String message){
            printWriter.println(message);
    }

    void tcpConnectionEstablishedTo(int peerID){
        String message = simpleDateFormat.format(calendar.getTime()) + ": Peer " + this.peerID
                + " makes a connection to Peer " + peerID + ".";
        writeToFile(message);
    }

    void tcpConnectionEstablishedFrom(int peerID){
        String message = simpleDateFormat.format(calendar.getTime()) + ": Peer " + this.peerID
                +" makes a connection from Peer "+ peerID + ".";
        writeToFile(message);
    }

    void preferredNeighborsChanged(int[] peerIDs){
        StringBuilder message = new StringBuilder();
        message.append(simpleDateFormat.format(calendar.getTime())).append(": Peer ").append(this.peerID)
                .append(" has the preferred neighbors ");
        for(int i = 0; i < peerIDs.length - 1; i++) {
            message.append(peerIDs[i]).append(", ");
        }
        message.append(peerIDs[peerIDs.length - 1]).append(".");
        writeToFile(message.toString());
    }

    void optUnchokedNeighborChanged(int peerID){
        String message = simpleDateFormat.format(calendar.getTime()) + ": Peer " + this.peerID
                + "has the optimistically unchoked neighbor" + peerID;
        writeToFile(message);
    }

    void unchokedBy(int peerID){
        String message = simpleDateFormat.format(calendar.getTime()) + ": Peer " + this.peerID
                + " is unchoked by " + peerID + ".";
        writeToFile(message);
    }

    void chokedBy(int peerID){
        String message = simpleDateFormat.format(calendar.getTime()) + ": Peer " + this.peerID
                + " is choked by " + peerID + ".";
        writeToFile(message);
    }

    void receivedHaveFrom(int peerID, int pieceIndex){
        String message = simpleDateFormat.format(calendar.getTime()) + ": Peer " + this.peerID
                + " received the \'have\' message from " + peerID + " for the piece " + pieceIndex + ".";
        writeToFile(message);
    }

    void receivedInterestedFrom(int peerID){
        String message = simpleDateFormat.format(calendar.getTime()) + ": Peer " + this.peerID
                + " received the \'interested\' message from " + peerID + ".";
        writeToFile(message);
    }

    void receivedNotInterestedFrom(int peerID){
        String message = simpleDateFormat.format(calendar.getTime()) + ": Peer " + this.peerID
                + " recieved the \'not interested\' message from " + peerID + ".";
        writeToFile(message);
    }

    void downloadedPieceFrom(int peerID, int pieceIndex, int numberOfPieces){
        String message = simpleDateFormat.format(calendar.getTime()) + ": Peer " + this.peerID
                + " has downloaded the piece " + pieceIndex + " from " + peerID
                + ". Now the number of pieces it has is " + numberOfPieces + ".";
        writeToFile(message);
    }

    void downloadedFile(){
        String message = simpleDateFormat.format(calendar.getTime()) + ": Peer " + this.peerID
                + "has downloaded the complete file.";
        writeToFile(message);
    }

}
