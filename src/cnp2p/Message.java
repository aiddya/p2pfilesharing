package cnp2p;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Message implements Externalizable {
    private static final int BUFFER_SIZE = 8192;
    private MessageType messageType;
    private byte[] payload;

    public Message() {
        // Default constructor for Externalizable
    }

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
        if (type == MessageType.HAVE || type == MessageType.REQUEST) {
            if (payload.length == 4) {
                messageType = type;
                this.payload = payload;
            } else {
                throw new IllegalArgumentException("Message type requires a 4-byte payload");
            }
        } else if (type == MessageType.BITFIELD || type == MessageType.PIECE) {
            messageType = type;
            this.payload = payload;
        } else {
            throw new IllegalArgumentException("Message type should not have a payload");
        }
    }

    public Message(MessageType type, int index) {
        if (type == MessageType.HAVE || type == MessageType.REQUEST) {
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

    public void writeExternal(ObjectOutput out) throws IOException {
        int payloadLen = payload == null ? 1 : payload.length + 1;
        out.write(ByteBuffer.allocate(4).putInt(payloadLen).array());
        out.write(messageType.getByte());
        out.write(payload);
        out.flush();
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        byte[] len = new byte[4];
        in.read(len);

        int length = ByteBuffer.wrap(len).getInt();
        if (length < 1) {
            throw new ClassNotFoundException("Invalid length specified");
        }

        messageType = MessageType.fromInt(in.read());
        if (messageType == null) {
            throw new ClassNotFoundException("Invalid message type specified");
        } else if ((messageType == MessageType.CHOKE
                || messageType == MessageType.UNCHOKE
                || messageType == MessageType.INTERESTED
                || messageType == MessageType.NOT_INTERESTED) && length != 1) {
            throw new ClassNotFoundException("Invalid message length, expected 1");
        } else if ((messageType == MessageType.HAVE || messageType == MessageType.REQUEST) && length != 5) {
            throw new ClassNotFoundException("Invalid message length, expected 5");
        } else if (messageType == MessageType.PIECE && length < 6) {
            throw new ClassNotFoundException("Invalid message length, too short");
        }

        payload = new byte[length - 1];
        for (int i = 0; i <= payload.length / BUFFER_SIZE; i++) {
            int remaining = payload.length - (i * BUFFER_SIZE);
            in.read(payload, i * BUFFER_SIZE, remaining < BUFFER_SIZE ? remaining : BUFFER_SIZE);
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
