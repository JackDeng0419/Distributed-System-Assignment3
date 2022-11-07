import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

/* 
 * A map that stores the URL for each member
 */

public class ConfigurationUtils {

    public static HashMap<String, AccepterInfo> accepterMap = new HashMap<>();
    public static HashMap<String, ProposerInfo> proposerMap = new HashMap<>();

    /*
     * This method parses the configuration file into the map objects
     */
    public static void ConfigurationInit(String configFilename) {
        Scanner scanner;
        try {
            // construct the learner map
            scanner = new Scanner(new File(configFilename));
            while (scanner.hasNextLine()) {
                String[] strings = scanner.nextLine().split("-", 4);
                String memberID = strings[0];
                String profileString = strings[1];
                int profile = 0;

                switch (profileString) {
                    case "immediate":
                        profile = Constant.PROFILE_IMMEDIATE;
                        break;
                    case "medium":
                        profile = Constant.PROFILE_MEDIUM;
                        break;
                    case "late":
                        profile = Constant.PROFILE_LATE;
                        break;
                    case "never":
                        profile = Constant.PROFILE_NEVER;
                        break;
                    default:
                        break;
                }

                String[] accepterDomain = strings[2].split(":", 2);
                String accepterIp = accepterDomain[0];
                int accepterPort = Integer.parseInt(accepterDomain[1]);

                AccepterInfo accepterInfo = new AccepterInfo(accepterPort, accepterIp, memberID, profile);

                accepterMap.put(memberID, accepterInfo);

                if (strings.length == 4) {
                    // configure the proposer
                    String[] proposerDomain = strings[3].split(":", 2);
                    String proposerIp = proposerDomain[0];
                    int proposerPort = Integer.parseInt(proposerDomain[1]);

                    ProposerInfo proposerInfo = new ProposerInfo(proposerPort, proposerIp, memberID, profile);
                    proposerMap.put(memberID, proposerInfo);
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("Configuration failed.");
            e.printStackTrace();
        }
    }

}
