package cnp2p;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    private static List<ConnectionHandler> connectionHandlerList;

    public static void main(String[] args) {
        int peerId, indexPeers;
        final int listeningPortNumber;
        List<Peer> peerListComplete;

        if (args.length == 0) {
            System.out.println("Peer ID is not specified. Exiting!");
            return;
        }

        try {
            peerId = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.out.println("Peer ID is not a number. Exiting!");
            return;
        }

        try {
            Logger.createInstance(peerId, Config.getInstance().getCurrentDirectory());
        } catch (Exception e) {
            System.out.println("Unable to read configuration. Exiting!");
            return;
        }

        peerListComplete = Config.getInstance().getPeerList();

        if (peerListComplete.isEmpty()) {
            System.out.println("No peers listed in peer configuration. Exiting!");
            return;
        }

        List<Peer> peerListTrim = new ArrayList<>();
        connectionHandlerList = new CopyOnWriteArrayList<>();
        Tracker.getInstance().setConnectionHandlerList(connectionHandlerList);

        for (indexPeers = 0; indexPeers < peerListComplete.size() &&
                peerListComplete.get(indexPeers).getPeerId() != peerId; indexPeers++) {
            peerListTrim.add(peerListComplete.get(indexPeers));
        }

        listeningPortNumber = peerListComplete.get(indexPeers).getPortNumber();
        if (peerListComplete.get(indexPeers).hasFile()) {
            Config.getInstance().setHasFile(true);
            Tracker.getInstance().setAllBits();
        }

        Thread listeningThread = new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(listeningPortNumber);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket, peerId);
                    connectionHandlerList.add(connectionHandler);
                    connectionHandler.start();
                }
            } catch (IOException io) {
                io.printStackTrace();
            }
        });

        listeningThread.start();

        for (Peer peer : peerListTrim) {
            try {
                Socket connectPeerSocket = new Socket(peer.getHostName(),
                        peer.getPortNumber());
                ConnectionHandler connectionHandler =
                        new ConnectionHandler(connectPeerSocket, peerId, peer.getPeerId());
                connectionHandlerList.add(connectionHandler);
                connectionHandler.start();
            } catch (Exception e) {
                System.out.println("Failed to initiate connection with " + peer.getHostName());
            }
        }

        TimerTask unchokeTask = new TimerTask() {
            public void run() {
                ArrayList<ConnectionHandler> currentList = new ArrayList<>(connectionHandlerList);
                currentList.sort(Collections.reverseOrder(Comparator.comparingDouble(ConnectionHandler::getDownloadRate)));
                int connectionsCount = currentList.size();
                int prefCount = Config.getInstance().getPreferredNeighbors();
                boolean neighboursChanged = false;
                int[] peerIds;

                if (connectionsCount == 0) {
                    return;
                } else if (prefCount > connectionsCount) {
                    prefCount = connectionsCount;
                }

                peerIds = new int[prefCount];

                for (ConnectionHandler connection : currentList) {
                    if (connection.isRemoteInterested() && prefCount != 0) {
                        if (connection.getRemoteStatus() != ChokeStatus.UNCHOKED) {
                            neighboursChanged = true;
                            Message unchoke = new Message(MessageType.UNCHOKE);
                            connection.addMessage(unchoke);
                        }
                        peerIds[--prefCount] = connection.getRemotePeerId();
                    } else if (connection.getRemoteStatus() != ChokeStatus.CHOKED && prefCount == 0) {
                        if (connection.getRemoteStatus() != ChokeStatus.UNKNOWN) {
                            neighboursChanged = true;
                        }
                        Message choke = new Message(MessageType.CHOKE);
                        connection.addMessage(choke);
                    }
                }

                if (neighboursChanged) {
                    Logger.getInstance().preferredNeighborsChanged(peerIds);
                }
            }
        };

        Timer unchokeTimer = new Timer("UnchokeAlgorithm");
        unchokeTimer.scheduleAtFixedRate(unchokeTask, 0, Config.getInstance().getUnchokingInterval() * 1000);

        TimerTask optUnchokeTask = new TimerTask() {
            public void run() {
                ArrayList<ConnectionHandler> chokedConnections = new ArrayList<>();
                for (ConnectionHandler connection : connectionHandlerList) {
                    if (connection.getRemoteStatus() != ChokeStatus.UNCHOKED && connection.isRemoteInterested()) {
                        chokedConnections.add(connection);
                    }
                }

                if (chokedConnections.isEmpty()) {
                    return;
                }

                Random rand = new Random();
                int randValue = rand.nextInt(chokedConnections.size());

                ConnectionHandler connection = chokedConnections.get(randValue);

                if (connection.getRemoteStatus() != ChokeStatus.UNCHOKED) {
                    Message unchoke = new Message(MessageType.UNCHOKE);
                    connection.addMessage(unchoke);
                    Logger.getInstance().optUnchokedNeighborChanged(connection.getRemotePeerId());
                }
            }
        };

        Timer optUnchokeTimer = new Timer("OptimisticallyUnchokeAlgorithm");
        optUnchokeTimer.scheduleAtFixedRate(optUnchokeTask, 0, Config.getInstance().getOptimisticUnchokingInterval() * 1000);
    }
}