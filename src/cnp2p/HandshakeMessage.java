package cnp2p;

import java.io.Serializable;
import java.util.Arrays;

public class HandshakeMessage implements Serializable {
    byte[] bytes;
    final byte[] bytePrefix = {0x50, 0x32, 0x50, 0x46, 0x49, 0x4c, 0x45, 0x53, 0x48, 0x41, 0x52, 0x49, 0x4e, 0x47, 0x50,
            0x52, 0x4f, 0x4a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    public HandshakeMessage() {
        bytes = new byte[32];
    }

    public HandshakeMessage(byte[] fromBytes) {
        if (fromBytes.length == 32) {
            bytes = fromBytes;
        }
    }

    Boolean create(String peerId) {
        if (peerId.length() == 4) {
            System.arraycopy(bytePrefix, 0, bytes, 0, 28);
            System.arraycopy(peerId.getBytes(), 0, bytes, 28, 4);
            return true;
        } else {
            return false;
        }
    }

    Boolean validate() {
        if (bytes == null) {
            return false;
        }

        for (int i = 0; i < bytePrefix.length; i++) {
            if (bytes[i] != bytePrefix[i]) {
                return false;
            }
        }
        return true;
    }

    byte[] getBytes() {
        return bytes;
    }

    String getPeerId() {
        return new String(Arrays.copyOfRange(bytes, 28, 32));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Prefix: ");
        sb.append(new String(Arrays.copyOfRange(bytes, 0, 18)));
        sb.append(" Zero: ");
        sb.append(new String(Arrays.copyOfRange(bytes, 18, 28)));
        sb.append(" PeerID: ");
        sb.append(new String(Arrays.copyOfRange(bytes, 28, 32)));
        return sb.toString();
    }
}
