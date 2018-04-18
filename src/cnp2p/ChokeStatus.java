package cnp2p;

public enum ChokeStatus {
    CHOKED(0),
    UNCHOKED(1),
    OPT_UNCHOKED(2);

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

