package cnp2p;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;

import static cnp2p.MessageType.*;

public class Message implements Externalizable {
    int pieceIndex;
    private MessageType messageType;
    private byte[] payload;

    public Message() {
        // Default constructor for Externalizable
    }

    public Message(MessageType type) {
        if (type == CHOKE || type == UNCHOKE || type == INTERESTED || type == NOT_INTERESTED) {
            messageType = type;
        } else {
            throw new IllegalArgumentException("Message type requires an index or payload");
        }
    }

    public Message(MessageType type, byte[] payload) {
        if (type == BITFIELD) {
            messageType = type;
            this.payload = payload;
        } else {
            throw new IllegalArgumentException("Message type has to be bit field");
        }
    }

    public Message(MessageType type, int pieceIndex) {
        if (type == HAVE || type == REQUEST) {
            messageType = type;
            this.pieceIndex = pieceIndex;
        } else {
            throw new IllegalArgumentException("Message type cannot accept index or requires a payload");
        }
    }

    public Message(MessageType type, int pieceIndex, byte[] payload) {
        if (type == PIECE) {
            messageType = type;
            this.pieceIndex = pieceIndex;
            this.payload = payload;
        } else {
            throw new IllegalArgumentException("Message type has to be piece");
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        int payloadLen = 1;

        switch (messageType) {
            case BITFIELD:
                payloadLen = payload.length + 1;
                break;
            case PIECE:
                payloadLen = payload.length + 5;
                break;
            case HAVE:
            case REQUEST:
                payloadLen = 5;
                break;
        }

        out.write(ByteBuffer.allocate(4).putInt(payloadLen).array());
        out.write(messageType.getByte());

        if (messageType == HAVE || messageType == REQUEST || messageType == PIECE) {
            out.write(ByteBuffer.allocate(4).putInt(pieceIndex).array());
        }

        if (messageType == BITFIELD || messageType == PIECE) {
            out.write(payload);
        }

        out.flush();
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        byte[] len = new byte[4];
        in.read(len);

        int length = ByteBuffer.wrap(len).getInt();
        if (length < 1) {
            throw new ClassNotFoundException("Invalid length specified");
        }

        messageType = fromInt(in.read());
        if (messageType == null) {
            throw new ClassNotFoundException("Invalid message type specified");
        } else if ((messageType == CHOKE || messageType == UNCHOKE || messageType == INTERESTED
                || messageType == NOT_INTERESTED) && length != 1) {
            throw new ClassNotFoundException("Invalid message length, expected 1 got " + length);
        } else if ((messageType == HAVE || messageType == REQUEST) && length != 5) {
            throw new ClassNotFoundException("Invalid message length, expected 5 got " + length);
        } else if (messageType == PIECE && length < 6) {
            throw new ClassNotFoundException("Invalid message length, too short. Expected >=6, got " + length);
        }

        if (length > 1) {
            if (messageType != BITFIELD) {
                byte[] index = new byte[4];
                in.read(index);
                pieceIndex = ByteBuffer.wrap(index).getInt();
            }

            if (messageType == BITFIELD || messageType == PIECE) {
                payload = new byte[length - (messageType == BITFIELD ? 1 : 5)];
                in.readFully(payload);
            }
        }
    }

    byte[] getPayload() {
        return payload;
    }

    MessageType getType() {
        return messageType;
    }

    int getIndex() {
        if (messageType == HAVE || messageType == REQUEST || messageType == PIECE) {
            return pieceIndex;
        } else {
            return -1;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Length: ");
        if (payload != null) {
            sb.append(payload.length + (messageType == BITFIELD ? 1 : 5));
        } else if (messageType == HAVE || messageType == REQUEST || messageType == PIECE) {
            sb.append(5);
        } else {
            sb.append(1);
        }
        sb.append(" Message Type: ");
        sb.append(messageType.name());
        if (messageType == HAVE || messageType == REQUEST || messageType == PIECE) {
            byte[] index = new byte[4];
            System.arraycopy(payload, 0, index, 0, 4);
            sb.append(" Piece Index: ");
            sb.append(ByteBuffer.wrap(index).getInt());
        }
        return sb.toString();
    }
}
