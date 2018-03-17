package cnp2p;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;

public class Config {
	private static int preferredNeighbors;
	private static int unchokingInterval;
	private static int optimisticUnchokingInterval;
	private static String fileName;
	private static long fileSize;
	private static long pieceSize;

	public Config() {
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
            fileSize = Long.parseLong(commonProp.getProperty("FileSize"));
            pieceSize = Long.parseLong(commonProp.getProperty("PieceSize"));
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
	}
	
}
