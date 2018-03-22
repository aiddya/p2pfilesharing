package cnp2p;

import java.io.*;
import java.net.URL;
import java.util.*;

public class Config {
    private volatile static Config instance;
    private int preferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;
    private List<Peer> peerList;
    private String currentDirectory;
    private boolean hasFile;

    private Config() {
        Properties commonProp = new Properties();
        try {
            String commonConfig = "Config.cfg";
            ClassLoader classLoader = peerProcess.class.getClassLoader();
            URL res = Objects.requireNonNull(classLoader.getResource(commonConfig),
                    "Can't find configuration file Config.cfg");
            InputStream is = new FileInputStream(res.getFile());
            currentDirectory = System.getProperty("user.dir");
            commonProp.load(is);
            preferredNeighbors = Integer.parseInt(commonProp.getProperty("NumberOfPreferredNeighbors"));
            unchokingInterval = Integer.parseInt(commonProp.getProperty("UnchokingInterval"));
            optimisticUnchokingInterval = Integer.parseInt(commonProp.getProperty("OptimisticUnchokingInterval"));
            fileName = commonProp.getProperty("FileName");
            fileSize = Integer.parseInt(commonProp.getProperty("FileSize"));
            pieceSize = Integer.parseInt(commonProp.getProperty("PieceSize"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        readPeerInfo();
    }

    public static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config();
                }
            }
        }
        return instance;
    }

    public boolean getHasFile(){
        return hasFile;
    }

    public void setHasFile(boolean hasFile) {
        this.hasFile = hasFile;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    private void readPeerInfo() {
        File peerInfoConfig = new File("PeerInfo.cfg");
        peerList = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(peerInfoConfig));
            String line = reader.readLine();
            while (line != null) {
                StringTokenizer st = new StringTokenizer(line, " ");
                peerList.add(new Peer(Integer.parseInt(st.nextToken()), st.nextToken(),
                        Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()) == 1));
                line = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            return;
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

    public List<Peer> getPeerList() {
        return peerList;
    }
}
