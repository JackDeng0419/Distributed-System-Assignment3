import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/* 
 * A map that stores the URL for each member
 */

public class ConfigurationUtils {

    private final String UrlLearnerFilename = "URL_learner.txt";
    private final String UrlProposerFilename = "URL_proposer.txt";
    private final String UrlAcceptorFilename = "URL_accepter.txt";

    private Map<String, String> urlLearnerMap = new HashMap<>();
    private Map<String, String> urlProposerMap = new HashMap<>();
    private Map<String, String> urlAccepterMap = new HashMap<>();

    public static HashMap<String, AccepterInfo> accepterMap = new HashMap<>();
    public static HashMap<String, ProposerInfo> proposerMap = new HashMap<>();

    /*
     * This constructor reads the URL list file and construct the maps
     */
    public ConfigurationUtils() {
        Scanner scanner;
        try {
            // construct the learner map
            scanner = new Scanner(new File(UrlLearnerFilename));
            while (scanner.hasNextLine()) {
                String[] strings = scanner.nextLine().split("-", 2);
                urlLearnerMap.put(strings[0], strings[1]);
            }

            // construct the proposer map
            scanner = new Scanner(new File(UrlProposerFilename));
            while (scanner.hasNextLine()) {
                String[] strings = scanner.nextLine().split("-", 2);
                urlProposerMap.put(strings[0], strings[1]);
            }

            // construct the acceptor map
            scanner = new Scanner(new File(UrlAcceptorFilename));
            while (scanner.hasNextLine()) {
                String[] strings = scanner.nextLine().split("-", 2);
                urlAccepterMap.put(strings[0], strings[1]);
            }
        } catch (FileNotFoundException e) {
            System.out.println("URLList failed to construct.");
            e.printStackTrace();
        }
    }

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

            // // construct the proposer map
            // scanner = new Scanner(new File(UrlProposerFilename));
            // while (scanner.hasNextLine()) {
            // String[] strings = scanner.nextLine().split("-", 2);
            // urlProposerMap.put(strings[0], strings[1]);
            // }

            // // construct the acceptor map
            // scanner = new Scanner(new File(UrlAcceptorFilename));
            // while (scanner.hasNextLine()) {
            // String[] strings = scanner.nextLine().split("-", 2);
            // urlAccepterMap.put(strings[0], strings[1]);
            // }
        } catch (FileNotFoundException e) {
            System.out.println("Configuration failed.");
            e.printStackTrace();
        }
    }

    public Map<String, String> getUrlLearnerMap() {
        return urlLearnerMap;
    }

    public Map<String, String> getUrlProposerMap() {
        return urlProposerMap;
    }

    public Map<String, String> getUrlAccepterMap() {
        return urlAccepterMap;
    }

    public int getAccepterCount() {
        return urlAccepterMap.size();
    }

    // class AccepterInfo {
    // int port;
    // String ip;
    // String memberID;
    // int profile;

    // public AccepterInfo(int port, String ip, String memberID, int profile) {
    // this.port = port;
    // this.ip = ip;
    // this.memberID = memberID;
    // this.profile = profile;
    // }
    // }

    // class ProposerInfo {
    // int port;
    // String ip;
    // String memberID;
    // int profile;

    // public ProposerInfo(int port, String ip, String memberID, int profile) {
    // this.port = port;
    // this.ip = ip;
    // this.memberID = memberID;
    // this.profile = profile;
    // }
    // }
}
