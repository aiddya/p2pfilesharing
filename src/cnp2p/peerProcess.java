package cnp2p;

import java.io.*;
import java.net.*;
import java.util.*;

public class peerProcess {
	private static int preferredNeighbors;
	private static int unchokingInterval;
	private static int optimisticUnchokingInterval;
	private static String fileName;
	private static long fileSize;
	private static long pieceSize;
	private List<Peer> peerList;
	private Scanner peerInfoScanner;
//	private int port = 65380;
	public peerProcess(int peerID) {
		getConfig();
		getPeerInfo();
	}
	/*
    private void writePeerInfo(int peerID) throws IOException {
        RandomAccessFile stream = new RandomAccessFile(fileName, "rw");
        FileChannel channel = stream.getChannel();
        FileLock lock = null;
        try {
        		lock = channel.tryLock();
            if(lock != null) {
            		InetSocketAddress host = new InetSocketAddress(port);
                stream.writeChars(peerID + " " + host.getAddress());
            }
            else {
            		TimeUnit.SECONDS.sleep(1);
            		writePeerInfo(peerID);
            }
        } catch (final OverlappingFileLockException | InterruptedException e) {
            stream.close();
            channel.close();
        }
        lock.release();
     
        stream.close();
        channel.close();
	}
	*/
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
	private void getConfig() {
    		Properties commonProp = new Properties();
        try {
            String commonConfig = "Config.cfg";
            ClassLoader classLoader = peerProcess.class.getClassLoader();
            URL res = Objects.requireNonNull(classLoader.getResource(commonConfig),
                "Can't find configuration file Config.cfg");
            InputStream is = new FileInputStream(res.getFile());
            commonProp.load(is);
            setPreferredNeighbors(Integer.parseInt(commonProp.getProperty("NumberOfPreferredNeighbors")));
            setUnchokingInterval(Integer.parseInt(commonProp.getProperty("UnchokingInterval")));
            setOptimisticUnchokingInterval(Integer.parseInt(commonProp.getProperty("OptimisticUnchokingInterval")));
            setFileName(commonProp.getProperty("FileName"));
            setFileSize(Long.parseLong(commonProp.getProperty("FileSize")));
            setPieceSize(Long.parseLong(commonProp.getProperty("PieceSize")));
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
	}
	public static void main(String[] args) {
    		
       //peerProcess p = new peerProcess();
    }
	public static int getOptimisticUnchokingInterval() {
		return optimisticUnchokingInterval;
	}
	public static void setOptimisticUnchokingInterval(int optimisticUnchokingInterval) {
		peerProcess.optimisticUnchokingInterval = optimisticUnchokingInterval;
	}
	public static int getUnchokingInterval() {
		return unchokingInterval;
	}
	public static void setUnchokingInterval(int unchokingInterval) {
		peerProcess.unchokingInterval = unchokingInterval;
	}
	public static int getPreferredNeighbors() {
		return preferredNeighbors;
	}
	public static void setPreferredNeighbors(int preferredNeighbors) {
		peerProcess.preferredNeighbors = preferredNeighbors;
	}
	public static long getFileSize() {
		return fileSize;
	}
	public static void setFileSize(long fileSize) {
		peerProcess.fileSize = fileSize;
	}
	public static long getPieceSize() {
		return pieceSize;
	}
	public static void setPieceSize(long pieceSize) {
		peerProcess.pieceSize = pieceSize;
	}
	public static String getFileName() {
		return fileName;
	}
	public static void setFileName(String fileName) {
		peerProcess.fileName = fileName;
	}
	public List<Peer> getPeerList() {
		return peerList;
	}
	

	public class Peer {
		private int peerID;
		private String hostName;
		private int portNumber;
		private boolean hasFile;
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
		

	}
}


