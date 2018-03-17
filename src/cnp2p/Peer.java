package cnp2p;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Peer {
	private int peerID;
	private String hostName;
	private int portNumber;
	private boolean hasFile;
	Socket requestSocket;
	
	public int getPeerID() {
		return peerID;
	}
	public void setPeerID(int peerID) {
		this.peerID = peerID;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public int getPortNumber() {
		return portNumber;
	}
	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}
	public boolean isHasFile() {
		return hasFile;
	}
	public void setHasFile(boolean hasFile) {
		this.hasFile = hasFile;
	}
	
	public void connect() {
		try{
			requestSocket = new Socket(hostName, portNumber);
		}
		catch (ConnectException e) {
    			System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	
	public void closeConnection() {
		try{
			requestSocket.close();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	

}

