package cnp2p;

public enum ChokeStatus {
    UNKNOWN(0),
    CHOKED(1),
    UNCHOKED(2);

    private int value;

    ChokeStatus(int val) {
        setValue(val);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}

