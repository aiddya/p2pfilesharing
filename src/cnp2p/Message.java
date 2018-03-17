package cnp2p;

import java.nio.*;
import java.util.Arrays;

public class Message {
    private MessageType messageType;
    private byte[] payload;

    public Message(MessageType type) {
        if (type == MessageType.CHOKE
                || type == MessageType.UNCHOKE
                || type == MessageType.INTERESTED
                || type == MessageType.NOT_INTERESTED) {
            messageType = type;
        } else {
            throw new IllegalArgumentException("Message type requires a payload");
        }
    }

    public Message(MessageType type, byte[] payload) {
        if (type == MessageType.HAVE
                || type == MessageType.REQUEST
                || type == MessageType.PIECE) {
            if (payload.length == 4) {
                messageType = type;
                this.payload = payload;
            } else {
                throw new IllegalArgumentException("Message type requires a 4-byte payload");
            }
        } else if (type == MessageType.BITFIELD){
            messageType = type;
            this.payload = payload;
        } else {
            throw new IllegalArgumentException("Message type should not have a payload");
        }
    }

    public Message(MessageType type, int index) {
        if (type == MessageType.HAVE
                || type == MessageType.REQUEST
                || type == MessageType.PIECE) {
                messageType = type;
                this.payload = ByteBuffer.allocate(4).putInt(index).array();
        } else {
            throw new IllegalArgumentException("Message type cannot accept integer payload");
        }
    }

    static Message parse(byte[] fromBytes) {
        if (fromBytes.length < 5) {
            return null;
        }

        byte[] len = Arrays.copyOfRange(fromBytes, 0, 4);
        int msgLen = ByteBuffer.wrap(len).getInt();

        if (fromBytes.length != msgLen + 4) {
            return null;
        }

        MessageType type = MessageType.fromByte(fromBytes[4]);
        if (msgLen > 1) {
            byte[] payload = new byte[msgLen - 1];
            System.arraycopy(fromBytes, 5, payload, 0, msgLen - 1);
            try {
                return new Message(type, payload);
            } catch (Exception e) {
                return null;
            }
        } else {
            try {
                return new Message(type);
            } catch (Exception e) {
                return null;
            }
        }
    }

    byte[] getBytes() {
        int payloadLen = payload == null ? 1 : payload.length + 1;
        byte[] output = new byte[payloadLen + 4];
        System.arraycopy(ByteBuffer.allocate(4).putInt(payloadLen).array(), 0, output, 0, 4);
        output[4] = messageType.getByte();
        System.arraycopy(payload, 0, output, 5, payload.length);
        return output;
    }

    byte[] getPayload() {
        return payload;
    }

    MessageType getType() {
        return messageType;
    }

    int getIndex() {
        if (messageType == MessageType.HAVE
                || messageType == MessageType.REQUEST
                || messageType == MessageType.PIECE) {
            return ByteBuffer.wrap(payload).getInt();
        } else {
            return -1;
        }
    }
}
