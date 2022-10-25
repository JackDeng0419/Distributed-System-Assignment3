import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TwoSendProposal {
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {

        ConcurrentHashMap<String, String> finalRecord = new ConcurrentHashMap<>();

        UrlList urlList = new UrlList();
        Map<String, String> urlLearnerMap = urlList.getUrlLearnerMap();
        Map<String, String> urlProposerMap = urlList.getUrlProposerMap();
        Map<String, String> urlAccepterMap = urlList.getUrlAccepterMap();

        CouncilVoter M4 = new CouncilVoter(9204, 9004, 9, "M4", finalRecord);
        CouncilVoter M5 = new CouncilVoter(9205, 9005, 9, "M5", finalRecord);
        CouncilVoter M6 = new CouncilVoter(9206, 9006, 9, "M6", finalRecord);
        CouncilVoter M7 = new CouncilVoter(9207, 9007, 9, "M7", finalRecord);
        CouncilVoter M8 = new CouncilVoter(9208, 9008, 9, "M8", finalRecord);
        CouncilVoter M9 = new CouncilVoter(9209, 9009, 9, "M9", finalRecord);

        M4.start();
        M5.start();
        M6.start();
        M7.start();
        M8.start();
        M9.start();

        CouncilCandidate M1 = new CouncilCandidate(9101, 9201, 9001, 9, "M1", finalRecord);
        CouncilCandidate M2 = new CouncilCandidate(9102, 9202, 9002, 9, "M2", finalRecord);
        CouncilCandidate M3 = new CouncilCandidate(9103, 9203, 9003, 9, "M3", finalRecord);

        M1.start();
        M2.start();
        M3.start();

        while (finalRecord.size() != 9) {

        }

        if (finalRecord.size() == 9) {
            PrintWriter writer = new PrintWriter("voteResult.txt", "UTF-8");
            for (String memberID : finalRecord.keySet()) {
                writer.println("[" + memberID + "]: the president is " + finalRecord.get(memberID));
            }
            writer.close();
        }
    }

}
