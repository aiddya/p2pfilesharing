package cnp2p;

import java.io.*;
import java.net.URL;
import java.util.*;

class Config {
    private volatile static Config instance;
    private int preferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;
    private List<Peer> peerList;
    private String currentDirectory;

    private Config() {
        Properties commonProp = new Properties();
        try {
            String commonConfig = "Common.cfg";
            ClassLoader classLoader = Main.class.getClassLoader();
            URL res = Objects.requireNonNull(classLoader.getResource(commonConfig),
                    "Can't find configuration file Common.cfg");
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
            System.out.println("Unable to read config file");
            e.printStackTrace();
        }
        readPeerInfo();
    }

    static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config();
                }
            }
        }
        return instance;
    }

    String getCurrentDirectory() {
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
            System.out.println("Unable to read peer config file");
            e.printStackTrace();
        }
    }

    int getPreferredNeighbors() {
        return preferredNeighbors;
    }

    int getUnchokingInterval() {
        return unchokingInterval;
    }

    int getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    String getFileName() {
        return fileName;
    }

    int getFileSize() {
        return fileSize;
    }

    int getPieceSize() {
        return pieceSize;
    }

    List<Peer> getPeerList() {
        return peerList;
    }
}
