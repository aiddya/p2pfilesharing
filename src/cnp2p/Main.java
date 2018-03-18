package cnp2p;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Main {

    public static void main(String[] args) {
        byte[] inBuf;
        byte[] outBuf = new byte[32];

        try {
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream(256);
            ObjectOutputStream outStream = new ObjectOutputStream(outBytes);
            HandshakeMessage msg = new HandshakeMessage(1001);
            outStream.write(msg.getBytes());
            outStream.flush();
            inBuf = outBytes.toByteArray();
            ByteArrayInputStream inBytes = new ByteArrayInputStream(inBuf);
            ObjectInputStream inStream = new ObjectInputStream(inBytes);
            inStream.readFully(outBuf);
            HandshakeMessage rcvMsg = HandshakeMessage.parse(outBuf);
            if (rcvMsg.validate()) {
                rcvMsg.toString();
            }
            inStream.close();
            outStream.close();
            inBytes.close();
            outBytes.close();
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }
}
