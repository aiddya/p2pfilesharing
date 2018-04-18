package cnp2p;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import cnp2p.ChokeStatus;
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


        Thread unchokingThread = new Thread(() -> {
            try {
                while (true) {
            			Collections.sort(connectionHandlerList, Collections.reverseOrder(Comparator.comparingDouble(ConnectionHandler :: getDownloadRate)));
            			int i=0;
            			for(ConnectionHandler preferred : connectionHandlerList) {
            				if((preferred.getPeerStatus() == ChokeStatus.CHOKED || preferred.getPeerStatus() == null || preferred.getPeerStatus() == ChokeStatus.OPT_UNCHOKED) && i < Config.getInstance().getPreferredNeighbors()) {
	                        Message unchoke = new Message(MessageType.UNCHOKE);
	                        preferred.addMessage(unchoke);
            				}
            				i++;
            			}
            			for(int j= Config.getInstance().getPreferredNeighbors(); j < connectionHandlerList.size(); j++) {
            				if(connectionHandlerList.get(j).getMyStatus() != ChokeStatus.OPT_UNCHOKED) {
	                        Message choke = new Message(MessageType.CHOKE);
	                        connectionHandlerList.get(j).addMessage(choke);
            				}
            			}
            			Thread.sleep(Config.getInstance().getUnchokingInterval() * 1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        unchokingThread.start();
        
        Thread opt_unchokingThread = new Thread(() -> {
            try {
                while (true) {
                		Message opt_unchoke = new Message(MessageType.UNCHOKE);
            			ConnectionHandler optUnchokedHandler = connectionHandlerList.get(ThreadLocalRandom.current().nextInt(Config.getInstance().getPreferredNeighbors(), connectionHandlerList.size()));
            			optUnchokedHandler.addMessage(opt_unchoke);
            			optUnchokedHandler.setMyStatus(ChokeStatus.OPT_UNCHOKED);
            			Thread.sleep(Config.getInstance().getOptimisticUnchokingInterval() * 1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        opt_unchokingThread.start();
        
        
    }
}