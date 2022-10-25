import java.util.concurrent.ConcurrentHashMap;

public class CouncilVoter {

    private int accepterPort;
    private int learnerPort;
    private String memberID;
    private int accepterCount;
    private ConcurrentHashMap<String, String> finalRecord;

    /*
     * Input:
     * 1. accepterPort: the port number for the proposer
     * 2. learnerPort: the port number for the learner
     * 3. memberID: e.g. M1, M2, ...
     * 4. accepterCount: the number of accepters
     */
    public CouncilVoter(int accepterPort, int learnerPort, int accepterCount, String memberID,
            ConcurrentHashMap<String, String> finalRecord) {
        this.accepterPort = accepterPort;
        this.learnerPort = learnerPort;
        this.memberID = memberID;
        this.accepterCount = accepterCount;
        this.finalRecord = finalRecord;
    }

    /*
     * start the accepter thread and the learner thread
     */
    public void start() {
        System.out.println("[" + memberID + "]: start");

        // start the accepter thread
        Accepter accepter = new Accepter(accepterPort, accepterCount, memberID, finalRecord);
        Thread accepterThread = new Thread(accepter);
        accepterThread.start();
        // try {
        //     accepterThread.join();
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }

        // start the learner thread
        // Learner learner = new Learner(learnerPort, accepterCount, memberID);
        // new Thread(learner).start();
    }

}
