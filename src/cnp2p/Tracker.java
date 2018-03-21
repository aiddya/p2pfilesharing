package cnp2p;

public class Tracker {
    private volatile static Tracker instance;
    byte[] bitField;

    private Tracker() {
        bitField = new byte[(Config.getInstance().getFileSize() - 1) / Config.getInstance().getPieceSize() + 1];
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

    byte[] getBitField() {
        return bitField;
    }

    void setBit(int pieceIndex) {
        int byteIndex = pieceIndex / 8;
        int byteOffset = pieceIndex % 8;
        bitField[byteIndex] |= (1 << byteOffset);
    }

    void setAllBits(){
        for(int i = 0; i < bitField.length; i++)
        {
            bitField[i] = (byte)(-128);
        }
    }

}
