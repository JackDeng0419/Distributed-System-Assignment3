import java.util.concurrent.ConcurrentHashMap;

/* 
 * This class is for the member that wants to be the president
 */
public class CouncilCandidate {

    private int proposerPort;
    private int learnerPort;
    private int accepterPort;
    private String memberID;
    private int accepterCount;
    private ConcurrentHashMap<String, String> finalRecord;
    private int profile;

    /*
     * Input:
     * 1. proposerPort: the port number for the proposer
     * 2. learnerPort: the port number for the learner
     * 3. accepterCount: the number of accepters
     * 4. memberID: e.g. M1, M2, ...
     */
    public CouncilCandidate(int proposerPort, int accepterPort, int learnerPort, int accepterCount, String memberID, ConcurrentHashMap<String, String> finalRecord, int profile) {
        this.proposerPort = proposerPort;
        this.learnerPort = learnerPort;
        this.accepterPort = accepterPort;
        this.accepterCount = accepterCount;
        this.memberID = memberID;
        this.finalRecord = finalRecord;
    }

    /*
     * start the proposer thread and the learner thread
     */
    public void start() {
        System.out.println("[" + memberID + "]: start");

        // start the proposer thread
        Proposer proposer = new Proposer(proposerPort, memberID);
        Thread proposeThread = new Thread(proposer);
        proposeThread.start();
        
        // start the accepter thread
        Accepter accepter = new Accepter(accepterPort, accepterCount, memberID, finalRecord, profile);
        Thread accepterThread = new Thread(accepter);
        accepterThread.start();
        
        // try {
        //     proposeThread.join();
        //     accepterThread.join();
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }
        // // start the learner thread
        // Learner learner = new Learner(learnerPort, accepterCount, memberID);
        // new Thread(learner).start();
    }

}
