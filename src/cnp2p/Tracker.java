package cnp2p;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

class Tracker {
    private volatile static Tracker instance;
    private static List<ConnectionHandler> connectionHandlerList;
    private int numPieces;
    private BitSet bitField;
    private BitSet reqBitField;
    private Hashtable<Integer, BitSet> peerBitField;
    private RandomAccessFile file;

    private Tracker() {
        numPieces = (Config.getInstance().getFileSize() - 1) / Config.getInstance().getPieceSize() + 1;
        bitField = new BitSet(numPieces);
        reqBitField = new BitSet(numPieces);
        peerBitField = new Hashtable<>();

    }

    static Tracker getInstance() {
        if (instance == null) {
            synchronized (Tracker.class) {
                if (instance == null) {
                    instance = new Tracker();
                }
            }
        }
        return instance;
    }

    void instantiateFile(int peerId, boolean hasFile) {
        Path dirPath = Paths.get(Config.getInstance().getCurrentDirectory(),
                "peer_" + peerId);
        String filePath = Paths.get(Config.getInstance().getCurrentDirectory(),
                "peer_" + peerId,
                Config.getInstance().getFileName()).toString();
        try {
            if (hasFile) {
                file = new RandomAccessFile(filePath, "r");
            } else {
                Files.createDirectories(dirPath);
                file = new RandomAccessFile(filePath, "rws");
            }
        } catch (IOException io) {
            System.out.println("Unable to open " + Config.getInstance().getFileName() + " file!");
        }
    }

    int getNumberPieces() {
        return bitField.cardinality();
    }

    byte[] getBitField() {
        byte[] output = new byte[(numPieces - 1) / 8 + 1];
        byte[] temp = bitField.toByteArray();
        System.arraycopy(temp, 0, output, 0, temp.length);
        return output;
    }

    List<ConnectionHandler> getConnectionHandlerList() {
        return connectionHandlerList;
    }

    void setConnectionHandlerList(List<ConnectionHandler> connectionHandlerList) {
        Tracker.connectionHandlerList = connectionHandlerList;
    }

    void setBit(int pieceIndex) {
        bitField.set(pieceIndex);
    }

    void setAllBits() {
        bitField.set(0, numPieces, true);
    }

    void setPeerBitField(int peerId, byte[] peerField) {
        peerBitField.put(peerId, BitSet.valueOf(peerField));
    }

    boolean isFileComplete() {
        return bitField.cardinality() == numPieces;
    }

    int getNewRandomPieceNumber(int peerId, boolean setRequested) {
        if (!peerBitField.containsKey(peerId)) {
            return -1;
        }

        BitSet diff = (BitSet) peerBitField.get(peerId).clone();
        diff.andNot(bitField);

        synchronized (this) {
            diff.andNot(reqBitField);
            int diffCard = diff.cardinality();
            if (diffCard == 0) {
                return -1;
            }
            int nextRand = new Random().nextInt(diffCard);
            int index = diff.stream().skip(nextRand).iterator().nextInt();

            if (setRequested) {
                reqBitField.set(index);
            }

            return index;
        }
    }

    void setPeerHasPiece(int peerId, int pieceIndex) {
        if (peerBitField.containsKey(peerId)) {
            peerBitField.get(peerId).set(pieceIndex);
        }
    }

    byte[] getPiece(int pieceIndex) {
        int size = Config.getInstance().getPieceSize();
        if (pieceIndex == numPieces - 1) {
            // Last piece
            int leftover = Config.getInstance().getFileSize() % Config.getInstance().getPieceSize();
            if (leftover != 0) {
                size = leftover;
            }
        }
        byte[] bytes = new byte[size];

        synchronized (this) {
            try {
                file.seek(pieceIndex * Config.getInstance().getPieceSize());
                file.readFully(bytes);
            } catch (IOException io) {
                System.out.println("Error getting piece at index " + pieceIndex);
                io.printStackTrace();
                return null;
            }
        }
        return bytes;
    }

    void putPiece(int pieceIndex, byte[] piece) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(piece.length);
        byteBuffer.put(piece);
        byteBuffer.flip();

        synchronized (this) {
            try {
                file.seek(pieceIndex * Config.getInstance().getPieceSize());
                file.write(piece);
            } catch (IOException io) {
                System.out.println("Error writing piece to index " + pieceIndex);
                io.printStackTrace();
            }
        }
    }
}