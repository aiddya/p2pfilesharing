package cnp2p;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;

public class Config {
	private int preferredNeighbors;
	private int unchokingInterval;
	private int optimisticUnchokingInterval;
	private String fileName;
	private int fileSize;
	private int pieceSize;
	private volatile static Config instance;
	List<Peer> peerList;
	private Scanner peerInfoScanner;

	private Config() {
    		Properties commonProp = new Properties();
        try {
            String commonConfig = "Config.cfg";
            ClassLoader classLoader = peerProcess.class.getClassLoader();
            URL res = Objects.requireNonNull(classLoader.getResource(commonConfig),
                "Can't find configuration file Config.cfg");
            InputStream is = new FileInputStream(res.getFile());
            commonProp.load(is);
            preferredNeighbors = Integer.parseInt(commonProp.getProperty("NumberOfPreferredNeighbors"));
            unchokingInterval = Integer.parseInt(commonProp.getProperty("UnchokingInterval"));
            optimisticUnchokingInterval = Integer.parseInt(commonProp.getProperty("OptimisticUnchokingInterval"));
            fileName = commonProp.getProperty("FileName");
            fileSize = Integer.parseInt(commonProp.getProperty("FileSize"));
            pieceSize = Integer.parseInt(commonProp.getProperty("PieceSize"));
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        getPeerInfo();
        instance = this;
	}
	
	public static Config getInstance() {
		if(instance == null) {
		    synchronized (Config.class) {
		        if (instance == null) {
		            instance = new Config();
                }
            }
        }
        return instance;
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

    public int getPreferredNeighbors() {
        return preferredNeighbors;
    }

    public int getUnchokingInterval() {
        return unchokingInterval;
    }

    public int getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getPieceSize() {
        return pieceSize;
    }
}
