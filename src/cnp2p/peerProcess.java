package cnp2p;


import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class peerProcess {
    /*
    public peerProcess(int peerID) {
        Config config = Config.getInstance();
        for (Peer p : config.peerList) {
            if (p.getPeerID() == peerID) {
                peer = p;
            }
        }
        try {
            peer.setRequestSocket(new Socket(peer.getHostName(), peer.getPortNumber()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        peer.connectToPeers(peerID);
        connection = new ConnectionHandler(peer.requestSocket, peerID);
    }
    */

    public static void main(String[] args) {
        int peerId;
        ConnectionHandler connection;
        Peer peer;


        if (args.length == 0) {
            System.out.println("Peer ID is not specified. Exiting!");
        }

        try {
            peerId = Integer.parseInt(args[0]);


        } catch (NumberFormatException e) {
            System.out.println("Peer ID must be a number. Exiting!");
        }

        System.out.println("Preferred neighbour: " + Config.getInstance().getPreferredNeighbors());
        System.out.println("Unchoking interval: " + Config.getInstance().getUnchokingInterval());
        System.out.println("Optimistic unchoking interval: " + Config.getInstance().getOptimisticUnchokingInterval());
        System.out.println("File name: " + Config.getInstance().getFileName());
        System.out.println("File size: " + Config.getInstance().getFileSize());
        System.out.println("Piece size: " + Config.getInstance().getPieceSize());
    }


}


