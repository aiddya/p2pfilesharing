package cnp2p;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

public class Tracker {
    private volatile static Tracker instance;
    private static List<ConnectionHandler> connectionHandlerList;
    private int numPieces;
    private BitSet bitField;
    private BitSet reqBitField;
    private Hashtable<Integer, BitSet> peerBitField;
    private Path filePath;
    private FileChannel fileChannel;

    private Tracker() {
        numPieces = (Config.getInstance().getFileSize() - 1) / Config.getInstance().getPieceSize() + 1;
        bitField = new BitSet(numPieces);
        reqBitField = new BitSet(numPieces);
        peerBitField = new Hashtable<>();
        filePath = Paths.get(Config.getInstance().getCurrentDirectory(), Config.getInstance().getFileName());
        try {
            if (Config.getInstance().getHasFile()) {
                OpenOption[] options = new OpenOption[] { StandardOpenOption.READ, StandardOpenOption.SYNC };
                fileChannel = FileChannel.open(filePath, options);
            } else {
                OpenOption[] options = new OpenOption[] { StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                        StandardOpenOption.SYNC };
                fileChannel = FileChannel.open(filePath, options);
            }
        } catch (IOException io) {
            System.out.println("Unable to open " + Config.getInstance().getFileName() + " file!");
        }
    }

    public static Tracker getInstance() {
        if (instance == null) {
            synchronized (Tracker.class) {
                if (instance == null) {
                    instance = new Tracker();
                }
            }
        }
        return instance;
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
        ByteBuffer byteBuffer = ByteBuffer.allocate(Config.getInstance().getPieceSize());
        byte[] piece = new byte[Config.getInstance().getPieceSize()];
        try {
            FileLock fileLock = fileChannel.lock();
            fileChannel.position(pieceIndex * Config.getInstance().getPieceSize());
            while (byteBuffer.hasRemaining()) {
                if (fileChannel.read(byteBuffer) == -1) {
                    break;
                }
            }
            fileLock.release();
            byteBuffer.flip();
            piece = byteBuffer.array();
        } catch (IOException io) {
            System.out.println("Error getting piece at index " + pieceIndex);
        }
        return piece;
    }

    void putPiece(int pieceIndex, byte[] piece) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Config.getInstance().getPieceSize());
        byteBuffer.put(piece);
        byteBuffer.flip();
        try {
            FileLock fileLock = fileChannel.lock();
            fileChannel.position(pieceIndex * Config.getInstance().getPieceSize());
            while (byteBuffer.hasRemaining()) {
                fileChannel.write(byteBuffer);
            }
            fileLock.release();
        } catch (IOException io) {
            System.out.println("Error writing piece to index " + pieceIndex);
        }
    }
}