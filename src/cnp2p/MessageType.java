package cnp2p;

public enum MessageType {
    CHOKE(0),
    UNCHOKE(1),
    INTERESTED(2),
    NOT_INTERESTED(3),
    HAVE(4),
    BITFIELD(5),
    REQUEST(6),
    PIECE(7);

    private int value;

    MessageType(int val) {
        value = val;
    }

    public static MessageType fromByte(byte val) {
        return fromInt((int) val);
    }

    public static MessageType fromInt(int val) {
        switch (val) {
            case 0:
                return CHOKE;
            case 1:
                return UNCHOKE;
            case 2:
                return INTERESTED;
            case 3:
                return NOT_INTERESTED;
            case 4:
                return HAVE;
            case 5:
                return BITFIELD;
            case 6:
                return REQUEST;
            case 7:
                return PIECE;
        }
        return null;
    }

    public int getValue() {
        return value;
    }

    public byte getByte() {
        return (byte) value;
    }

}
