package cnp2p;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class peerProcess {

    private static List<Peer> peerListTrim;
    private static List<ConnectionHandler> connectionHandlerList;

    public static void main(String[] args) {
        int peerId, indexPeers;
        Logger logger;
        final int listeningPortNumber;
        List<Peer> peerListComplete;
        Tracker tracker;

        if (args.length == 0) {
            System.out.println("Peer ID is not specified. Exiting!");
            return;
        }

        try {
            peerId = Integer.parseInt(args[0]);
            Logger.createInstance(peerId, Config.getInstance().getCurrentDirectory());
            logger = Logger.getInstance();

            peerListComplete = Config.getInstance().peerList;

            for(indexPeers = 0; indexPeers < peerListComplete.size() &&
                    peerListComplete.get(indexPeers).getPeerID() != peerId; indexPeers++)
            {
                peerListTrim.add(peerListComplete.get(indexPeers));
            }

            tracker = Tracker.getInstance();
            listeningPortNumber = peerListComplete.get(indexPeers).getPortNumber();
            if(peerListComplete.get(indexPeers).isHasFile())
                tracker.setAllBits();

            Thread listeningThread = new Thread(() -> {
                try {
                    ServerSocket serverSocket = new ServerSocket(listeningPortNumber);
                    while(true) {
                        Socket clientSocket = serverSocket.accept();
                        ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket, peerId);
                        connectionHandlerList.add(connectionHandler);
                        connectionHandler.run();
                    }
                }catch(IOException io)
                {
                    io.printStackTrace();
                }
            });
            listeningThread.run();

            for(Peer peer : peerListTrim)
            {
                Socket connectPeerSocket = new Socket(peer.getHostName(),
                        peer.getPortNumber());
                ConnectionHandler connectionHandler =
                        new ConnectionHandler(connectPeerSocket, peerId, peer.getPeerID());
                connectionHandlerList.add(connectionHandler);
                connectionHandler.run();
            }

        } catch (Exception e) {
            System.out.println("Peer ID must be a number. Exiting!");
        }

        //System.out.println("Preferred neighbour: " + Config.getInstance().getPreferredNeighbors());
        //System.out.println("Unchoking interval: " + Config.getInstance().getUnchokingInterval());
        //System.out.println("Optimistic unchoking interval: " + Config.getInstance().getOptimisticUnchokingInterval());
        //System.out.println("File name: " + Config.getInstance().getFileName());
        //System.out.println("File size: " + Config.getInstance().getFileSize());
        //System.out.println("Piece size: " + Config.getInstance().getPieceSize());
    }


}


