package cnp2p;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class HandshakeMessage implements Serializable {
    private byte[] bytes;
    private final byte[] bytePrefix = {0x50, 0x32, 0x50, 0x46, 0x49, 0x4c, 0x45, 0x53, 0x48, 0x41, 0x52, 0x49, 0x4e,
            0x47, 0x50, 0x52, 0x4f, 0x4a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    public HandshakeMessage(int peerId) {
        bytes = new byte[32];
        System.arraycopy(bytePrefix, 0, bytes, 0, 28);
        System.arraycopy(ByteBuffer.allocate(4).putInt(peerId).array(), 0, bytes, 28, 4);
    }

    private HandshakeMessage(byte[] fromBytes) {
            bytes = fromBytes;
    }

    static HandshakeMessage parse(byte[] fromBytes) {
        if (fromBytes.length != 32) {
            return null;
        }

        HandshakeMessage obj = new HandshakeMessage(fromBytes);
        if(obj.validate()) {
            return obj;
        } else {
            return null;
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

    int getPeerId() {
        return ByteBuffer.wrap(Arrays.copyOfRange(bytes, 28, 32)).getInt();
    }

    @Override
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
