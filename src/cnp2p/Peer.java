package cnp2p;

public class Peer {
    private int peerId;
    private String hostName;
    private int portNumber;
    private boolean hasFile;
    private int downloadRate;

    Peer(int peerId, String hostName, int portNumber, boolean hasFile) {
        this.peerId = peerId;
        this.hostName = hostName;
        this.portNumber = portNumber;
        this.hasFile = hasFile;
    }

    public int getPeerId() {
        return peerId;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public boolean hasFile() {
        return hasFile;
    }

    public int getDownloadRate() {
        return downloadRate;
    }

    public int setDownloadRate() {
        return downloadRate;
    }
}

