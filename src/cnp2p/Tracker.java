package cnp2p;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;

public class Tracker {
    private volatile static Tracker instance;
    private int numPieces;
    private BitSet bitField;
    HashMap<Integer, BitSet> peerBitField;

    private Tracker() {
        numPieces = (Config.getInstance().getFileSize() - 1) / Config.getInstance().getPieceSize() + 1;
        bitField = new BitSet(numPieces);
        peerBitField = new HashMap<>();
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

    boolean isFileEmpty() {
        return bitField.cardinality() == 0;
    }

    byte[] getBitField() {
        byte[] output = new byte[(numPieces - 1) / 8 + 1];
        byte[] temp = bitField.toByteArray();
        System.arraycopy(temp, 0, output, 0, temp.length);
        return output;
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

    int getNewRandomPieceNumber(int peerId) {
        if (!peerBitField.containsKey(peerId)) {
            return -1;
        }

        BitSet diff = peerBitField.get(peerId);
        diff.andNot(bitField);
        int diffCard = diff.cardinality();
        if (diffCard == 0) {
            return -1;
        }

        int nextRand = new Random().nextInt(diffCard);
        return diff.stream().skip(nextRand).iterator().nextInt();
    }
}
