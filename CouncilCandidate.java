/* 
 * This class is for the member that wants to be the president
 */
public class CouncilCandidate {

    private int proposerPort;
    private int learnerPort;
    private String memberID;
    private int accepterCount;

    /*
     * Input:
     * 1. proposerPort: the port number for the proposer
     * 2. learnerPort: the port number for the learner
     * 3. accepterCount: the number of accepters
     * 4. memberID: e.g. M1, M2, ...
     */
    public CouncilCandidate(int proposerPort, int learnerPort, int accepterCount, String memberID) {
        this.proposerPort = proposerPort;
        this.learnerPort = learnerPort;
        this.accepterCount = accepterCount;
        this.memberID = memberID;
    }

    /*
     * start the proposer thread and the learner thread
     */
    public void start() {
        System.out.println("[" + memberID + "]: start");

        // start the proposer thread
        Proposer proposer = new Proposer(proposerPort, memberID);
        new Thread(proposer).start();

        // start the learner thread
        Learner learner = new Learner(learnerPort, accepterCount, memberID);
        new Thread(learner).start();
    }

}
