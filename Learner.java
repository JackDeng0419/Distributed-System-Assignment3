import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/*
* The thread of learners
*/
class Learner implements Runnable {

    private int learnerPort;
    private int accepterCount;
    private String memberID;
    private boolean hasPresidentResult = false;
    private Map<String, Integer> voteRecord = new HashMap<>(); // stores the voteCount for each proposer

    /*
     * Input:
     * 1. learnerPort: the port of the learner
     * 2. accepterCount: the number of accepters
     * 3. memberID: the id of the council member, e.g. M1, M2 ...
     */
    public Learner(int learnerPort, int accepterCount, String memberID) {
        this.learnerPort = learnerPort;
        this.accepterCount = accepterCount;
        this.memberID = memberID;
    }

    /*
     * This thread receives the vote choice from accepters
     */
    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(learnerPort);
            while (!hasPresidentResult) {
                final Socket requestSocket = serverSocket.accept();
                DataInputStream dataInputStream = new DataInputStream(requestSocket.getInputStream());
                String messageType = SocketUtils.readString(dataInputStream);
                switch (messageType) {
                    case "vote": {
                        // handle the vote as a learner
                        String voteTo = SocketUtils.readString(dataInputStream);
                        recordVote(voteTo);
                        break;
                    }
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("[" + memberID + ":Learner]: failed to start");
            e.printStackTrace();
        }

    }

    /*
     * Input: the voted member
     * Desc: This method increase the vote count for voted member
     * If a member gets the majority vote, the result of the election will be
     * printed
     */
    private void recordVote(String voteTo) {
        if (voteRecord.containsKey(voteTo)) {
            int voteCount = voteRecord.get(voteTo);
            voteRecord.put(voteTo, voteCount + 1);
        } else {
            voteRecord.put(voteTo, 1);
        }

        if (voteRecord.get(voteTo) >= accepterCount / 2 + 1) {
            hasPresidentResult = true;
            System.out.println("[" + memberID + "]: " + voteTo + "is the new president.");
        }
    }

}
