package cnp2p;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static cnp2p.ChokeStatus.*;

public class Main {
    private static List<ConnectionHandler> connectionHandlerList;
    private static boolean connectionInitiated;

    public static void main(String[] args) {
        int peerId, indexPeers;
        final int listeningPortNumber;
        List<Peer> peerListComplete;
        connectionInitiated = false;
        ServerSocket serverSocket;

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

        peerListComplete = Config.getInstance().getPeerList();

        if (peerListComplete.isEmpty()) {
            System.out.println("No peers listed in peer configuration. Exiting!");
            return;
        }

        List<Peer> peerListTrim = new ArrayList<>();
        connectionHandlerList = new CopyOnWriteArrayList<>();
        Tracker.getInstance().setConnectionHandlerList(connectionHandlerList);

        for (indexPeers = 0; indexPeers < peerListComplete.size()
                && peerListComplete.get(indexPeers).getPeerId() != peerId; indexPeers++) {
            peerListTrim.add(peerListComplete.get(indexPeers));
        }

        listeningPortNumber = peerListComplete.get(indexPeers).getPortNumber();
        if (peerListComplete.get(indexPeers).hasFile()) {
            Tracker.getInstance().setAllBits();
            Tracker.getInstance().instantiateFile(peerListComplete.get(indexPeers).getPeerId(), true);
        } else {
            Tracker.getInstance().instantiateFile(peerListComplete.get(indexPeers).getPeerId(), false);
        }

        try {
            serverSocket = new ServerSocket(listeningPortNumber);
        } catch (IOException e) {
            return;
        }

        try {
            Logger.createInstance(peerId, Config.getInstance().getCurrentDirectory());
            Logger.getInstance().setName("Logger");
            Logger.getInstance().start();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unable to read configuration. Exiting!");
            return;
        }

        Thread listeningThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket, peerId);
                    connectionHandlerList.add(connectionHandler);
                    connectionHandler.start();
                    connectionInitiated = true;
                } catch (IOException io) {
                    // Connection closed, probably
                }
            }
        }, "ListeningThread");

        listeningThread.start();

        for (Peer peer : peerListTrim) {
            try {
                Socket connectPeerSocket = new Socket(peer.getHostName(), peer.getPortNumber());
                ConnectionHandler connectionHandler = new ConnectionHandler(connectPeerSocket, peerId,
                        peer.getPeerId());
                connectionHandlerList.add(connectionHandler);
                connectionHandler.start();
                connectionInitiated = true;
            } catch (Exception e) {
                System.out.println(peerId + " Failed to initiate connection with " + peer.getHostName()
                        + " with peer ID " + peer.getPeerId());
            }
        }

        Timer unchokeTimer = new Timer("UnchokeAlgorithm");
        TimerTask unchokeTask = new TimerTask() {
            public void run() {
                connectionHandlerList.removeIf(ConnectionHandler::isFileTransferComplete);
                if (connectionInitiated && connectionHandlerList.isEmpty()) {
                    Logger.getInstance().interrupt();
                    try {
                        serverSocket.close();
                        listeningThread.interrupt();
                    } catch (IOException e) {
                        // Already closed, ignore
                    }
                    cancel();
                    unchokeTimer.cancel();
                    unchokeTimer.purge();
                    return;
                }
                ArrayList<ConnectionHandler> currentList = new ArrayList<>(connectionHandlerList);
                Collections.shuffle(currentList);
                currentList.sort(Collections.reverseOrder(Comparator.comparingInt(ConnectionHandler::getDownloadRate)));
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
                        if (connection.getRemoteStatus() != UNCHOKED) {
                            neighboursChanged = true;
                            Message unchoke = new Message(MessageType.UNCHOKE);
                            connection.addMessage(unchoke);
                            connection.setRemoteStatus(UNCHOKED);
                        }
                        peerIds[--prefCount] = connection.getRemotePeerId();
                    } else if (connection.getRemoteStatus() != CHOKED && prefCount == 0) {
                        if (connection.getRemoteStatus() != UNKNOWN) {
                            neighboursChanged = true;
                        }
                        Message choke = new Message(MessageType.CHOKE);
                        connection.addMessage(choke);
                        connection.setRemoteStatus(CHOKED);
                    }
                    connection.resetDownloadRate();
                }

                if (neighboursChanged) {
                    Logger.getInstance().preferredNeighborsChanged(peerIds);
                }
            }
        };

        unchokeTimer.scheduleAtFixedRate(unchokeTask, 0, Config.getInstance().getUnchokingInterval() * 1000);

        Timer optUnchokeTimer = new Timer("OptimisticallyUnchokeAlgorithm");
        TimerTask optUnchokeTask = new TimerTask() {
            public void run() {
                connectionHandlerList.removeIf(ConnectionHandler::isFileTransferComplete);
                if (connectionInitiated && connectionHandlerList.isEmpty()) {
                    try {
                        serverSocket.close();
                        listeningThread.interrupt();
                    } catch (IOException e) {
                        // Already closed, ignore
                    }
                    Logger.getInstance().interrupt();
                    cancel();
                    optUnchokeTimer.cancel();
                    optUnchokeTimer.purge();
                    return;
                }

                ArrayList<ConnectionHandler> chokedConnections = new ArrayList<>();
                for (ConnectionHandler connection : connectionHandlerList) {
                    if (connection.getRemoteStatus() != UNCHOKED && connection.isRemoteInterested()) {
                        chokedConnections.add(connection);
                    }
                }

                if (chokedConnections.isEmpty()) {
                    return;
                }

                Random rand = new Random();
                int randValue = rand.nextInt(chokedConnections.size());

                ConnectionHandler connection = chokedConnections.get(randValue);

                if (connection.getRemoteStatus() != UNCHOKED) {
                    Message unchoke = new Message(MessageType.UNCHOKE);
                    connection.addMessage(unchoke);
                    connection.setRemoteStatus(UNCHOKED);
                    Logger.getInstance().optUnchokedNeighborChanged(connection.getRemotePeerId());
                }
            }
        };

        optUnchokeTimer.scheduleAtFixedRate(optUnchokeTask, 100,
                Config.getInstance().getOptimisticUnchokingInterval() * 1000);

        try {
            Logger.getInstance().join();
            listeningThread.join();
            Tracker.getInstance().closeFile();
        } catch (InterruptedException ie) {
            System.exit(1);
        }
    }
}