public class CouncilVoter {

    private int accepterPort;
    private int learnerPort;
    private String memberID;
    private int accepterCount;

    /*
     * Input:
     * 1. accepterPort: the port number for the proposer
     * 2. learnerPort: the port number for the learner
     * 3. memberID: e.g. M1, M2, ...
     * 4. accepterCount: the number of accepters
     */
    public CouncilVoter(int accepterPort, int learnerPort, int accepterCount, String memberID) {
        this.accepterPort = accepterPort;
        this.learnerPort = learnerPort;
        this.memberID = memberID;
        this.accepterCount = accepterCount;
    }

    /*
     * start the accepter thread and the learner thread
     */
    public void start() {
        System.out.println("[" + memberID + "]: start");

        // start the accepter thread
        Accepter accepter = new Accepter(accepterPort, memberID);
        new Thread(accepter).start();

        // start the learner thread
        Learner learner = new Learner(learnerPort, accepterCount, memberID);
        new Thread(learner).start();
    }

}
