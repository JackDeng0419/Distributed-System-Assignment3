import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TwoSendProposal {

    private static int accepterCount;

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {

        if (args.length < 1) {
            System.out.println("Please input the configure filename");
            return;
        }

        String configureFilename = args[0];

        ConcurrentHashMap<String, String> finalRecord = new ConcurrentHashMap<>();

        ConfigurationUtils.ConfigurationInit(configureFilename);

        HashMap<String, AccepterInfo> accepterMap = ConfigurationUtils.accepterMap;
        HashMap<String, ProposerInfo> proposerMap = ConfigurationUtils.proposerMap;

        accepterCount = accepterMap.size();

        // start the accepters
        for (String accepterKey : accepterMap.keySet()) {
            AccepterInfo accepterInfo = accepterMap.get(accepterKey);
            Accepter accepter = new Accepter(accepterInfo.port, accepterMap.size(), accepterInfo.memberID,
                    finalRecord, accepterInfo.profile);
            new Thread(accepter).start();
        }

        WaitUtils.sleepMillisecond(3000);

        // start the proposers

        for (String proposerKey : proposerMap.keySet()) {
            ProposerInfo proposerInfo = proposerMap.get(proposerKey);
            Proposer proposer = new Proposer(proposerInfo.port, proposerInfo.memberID);
            new Thread(proposer).start();
        }

        // CouncilVoter M4 = new CouncilVoter(9204, 9004, accepterCount, "M4",
        // finalRecord, Constant.PROFILE_IMMEDIATE);
        // CouncilVoter M5 = new CouncilVoter(9205, 9005, accepterCount, "M5",
        // finalRecord, Constant.PROFILE_IMMEDIATE);
        // CouncilVoter M6 = new CouncilVoter(9206, 9006, accepterCount, "M6",
        // finalRecord, Constant.PROFILE_IMMEDIATE);
        // CouncilVoter M7 = new CouncilVoter(9207, 9007, accepterCount, "M7",
        // finalRecord, Constant.PROFILE_IMMEDIATE);
        // CouncilVoter M8 = new CouncilVoter(9208, 9008, accepterCount, "M8",
        // finalRecord, Constant.PROFILE_IMMEDIATE);
        // CouncilVoter M9 = new CouncilVoter(9209, 9009, accepterCount, "M9",
        // finalRecord, Constant.PROFILE_IMMEDIATE);
        // CouncilVoter M10 = new CouncilVoter(9210, 9010, accepterCount, "M10",
        // finalRecord, Constant.PROFILE_IMMEDIATE);
        // CouncilVoter M11 = new CouncilVoter(9211, 9011, accepterCount, "M11",
        // finalRecord, Constant.PROFILE_IMMEDIATE);
        // CouncilVoter M12 = new CouncilVoter(9212, 9012, accepterCount, "M12",
        // finalRecord, Constant.PROFILE_IMMEDIATE);
        // CouncilVoter M13 = new CouncilVoter(9213, 9013, accepterCount, "M13",
        // finalRecord, Constant.PROFILE_IMMEDIATE);
        // CouncilVoter M14 = new CouncilVoter(9214, 9014, accepterCount, "M14",
        // finalRecord, Constant.PROFILE_IMMEDIATE);

        // M4.start();
        // M5.start();
        // M6.start();
        // M7.start();
        // M8.start();
        // M9.start();
        // M10.start();
        // M11.start();
        // M12.start();
        // M13.start();
        // M14.start();

        // CouncilCandidate M1 = new CouncilCandidate(9101, 9201, 9001, accepterCount,
        // "M1", finalRecord,
        // Constant.PROFILE_IMMEDIATE);
        // CouncilCandidate M2 = new CouncilCandidate(9102, 9202, 9002, accepterCount,
        // "M2", finalRecord,
        // Constant.PROFILE_IMMEDIATE);
        // CouncilCandidate M3 = new CouncilCandidate(9103, 9203, 9003, accepterCount,
        // "M3", finalRecord,
        // Constant.PROFILE_IMMEDIATE);

        // M1.start();
        // M2.start();
        // M3.start();

        while (finalRecord.size() != accepterCount) {

        }

        if (finalRecord.size() == accepterCount) {
            PrintWriter writer = new PrintWriter("voteResult.txt", "UTF-8");
            for (String memberID : finalRecord.keySet()) {
                System.out.println("[" + memberID + "]: the president is " +
                        finalRecord.get(memberID));
                writer.println("[" + memberID + "]: the president is " +
                        finalRecord.get(memberID));
            }
            writer.close();
            System.exit(0);
        }
    }

}
