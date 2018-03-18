package cnp2p;


import java.util.*;

public class peerProcess {
	private List<Peer> peerList;
	private Scanner peerInfoScanner;
	public peerProcess(int peerID) {
		Config config = new Config();
		getPeerInfo();
		connectToPeers(peerID);
	}
	
	private void connectToPeers(int peerID) {
		for(Peer p : peerList) {
			if(p.getPeerID() != peerID) {
				p.connect();
			}
			else break;
		}
	}
	private void getPeerInfo() {
	    Scanner in;
        peerInfoScanner = new Scanner("PeerInfo.cfg");
        while(peerInfoScanner.hasNextLine()) {
        		in = new Scanner(peerInfoScanner.nextLine());
        		in.useDelimiter(" ");
        		Peer p = new Peer();
        		p.setPeerID(Integer.parseInt(in.next()));
        		p.setHostName(in.next());
        		p.setPortNumber(Integer.parseInt(in.next()));
        		p.setHasFile(Boolean.parseBoolean(in.next()));
        		peerList.add(p);
        }
	}
	public static void main(String[] args) {
    		
       //peerProcess p = new peerProcess();
    }

	public List<Peer> getPeerList() {
		return peerList;
	}
	

}


