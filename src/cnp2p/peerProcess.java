package cnp2p;


import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class peerProcess {
	private Scanner peerInfoScanner;
	ConnectionHandler connection;
	Peer peer;
	public peerProcess(int peerID) {
		Config config = Config.getInstance();
		for(Peer p : config.peerList) {
			if(p.getPeerID() == peerID) {
				peer = p;
			}
		}
		try {
			peer.setRequestSocket(new Socket(peer.getHostName(),peer.getPortNumber()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		peer.connectToPeers(peerID);
		connection = new ConnectionHandler(peer.requestSocket, peerID);
	}
	
	public static void main(String[] args) {
    		
       //peerProcess p = new peerProcess();
    }


}


