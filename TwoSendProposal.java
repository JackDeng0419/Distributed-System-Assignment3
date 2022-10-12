import java.util.Map;

public class TwoSendProposal {
    public static void main(String[] args) {
        UrlList urlList = new UrlList();
        Map<String, String> urlLearnerMap = urlList.getUrlLearnerMap();
        Map<String, String> urlProposerMap = urlList.getUrlProposerMap();
        Map<String, String> urlAccepterMap = urlList.getUrlAccepterMap();

        CouncilVoter M4 = new CouncilVoter(9204, 9004, 6, "M4");
        CouncilVoter M5 = new CouncilVoter(9205, 9005, 6, "M5");
        CouncilVoter M6 = new CouncilVoter(9206, 9006, 6, "M6");
        CouncilVoter M7 = new CouncilVoter(9207, 9007, 6, "M7");
        CouncilVoter M8 = new CouncilVoter(9208, 9008, 6, "M8");
        CouncilVoter M9 = new CouncilVoter(9209, 9009, 6, "M9");

        M4.start();
        M5.start();
        M6.start();
        M7.start();
        M8.start();
        M9.start();

        CouncilCandidate M1 = new CouncilCandidate(9101, 9001, 6, "M1");
        CouncilCandidate M2 = new CouncilCandidate(9102, 9002, 6, "M2");
        CouncilCandidate M3 = new CouncilCandidate(9103, 9003, 6, "M3");

        M1.start();
        M2.start();
        M3.start();
    }

}
