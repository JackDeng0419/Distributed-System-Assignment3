import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
* The thread of learners
*/
class Learner implements Runnable {

    private int learnerPort;
    private final int accepterCount;
    private String memberID;
    private Map<String, String> urlProposerMap;
    private boolean hasPresidentResult = false;
    private int voteReceived = 0;
    private int currentProposeNumber = 0;
    private ConcurrentHashMap<String, Integer> voteRecord = new ConcurrentHashMap<>(); // stores the voteCount for each
                                                                                       // proposer
    private ConcurrentHashMap<String, String> votedAccepter = new ConcurrentHashMap<>(); // stores the voteCount for
                                                                                         // each

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
        this.urlProposerMap = new UrlList().getUrlProposerMap();
    }

    /*
     * This thread receives the vote choice from accepters
     */
    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(learnerPort);
            while (true) {
                final Socket requestSocket = serverSocket.accept();
                DataInputStream dataInputStream = new DataInputStream(requestSocket.getInputStream());
                String messageType = SocketUtils.readString(dataInputStream);
                switch (messageType) {
                    case "accepted": {
                        // handle the vote as a learner
                        String voteFrom = SocketUtils.readString(dataInputStream);
                        String voteTo = SocketUtils.readString(dataInputStream);

                        // add accepter to the map
                        try {

                            if (votedAccepter.get(voteFrom) != null) {
                                if (!votedAccepter.get(voteFrom).equals(voteTo)) {
                                    throw new Exception("Accepter can not vote different proposer");
                                }
                            } else {
                                votedAccepter.put(voteFrom, voteTo);
                                System.out.println(
                                        "[" + memberID + ":Learner]: " + voteTo + " got a vote");

                                recordVote(voteTo);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // int proposeNumber =
                        // Integer.parseInt(SocketUtils.readString(dataInputStream));

                        // if (proposeNumber >= currentProposeNumber) {
                        // if (proposeNumber > currentProposeNumber) {
                        // System.out.println(
                        // "[" + memberID + ":Learner]: This is a new propose with proposeNumber:"
                        // + proposeNumber);
                        // currentProposeNumber = proposeNumber;
                        // voteRecord.clear();
                        // voteReceived = 0;
                        // }
                        // }
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
        voteReceived++;
        if (voteRecord.containsKey(voteTo)) {
            int voteCount = voteRecord.get(voteTo);
            voteRecord.put(voteTo, voteCount + 1);
        } else {
            voteRecord.put(voteTo, 1);
        }

        String currentResultString = "";
        for (Map.Entry<String, Integer> voteSet : voteRecord.entrySet()) {
            currentResultString = currentResultString + "(" + voteSet.getKey() + ":" +
                    voteSet.getValue() + ") ";
        }

        System.out.println("[" + memberID + ":Learner]: current vote result: " + currentResultString);

        // got the final result
        if (voteRecord.get(voteTo) >= accepterCount / 2 + 1) {
            hasPresidentResult = true;
            System.out
                    .println("[" + memberID + ":Learner]: " + voteTo + " is the new president. " + currentResultString);
            return;
        }

        if (!hasPresidentResult && voteReceived == accepterCount) {
            // no one gets majority vote, send request to the proposers to propose again
            String resultString = "";
            for (Map.Entry<String, Integer> voteSet : voteRecord.entrySet()) {
                resultString = resultString + "(" + voteSet.getKey() + ":" +
                        voteSet.getValue() + ") ";
            }
            System.out.println("[" + memberID + ":Learner]: no candidate gets the majority vote for proposeNumber="
                    + currentProposeNumber + ", vote result: " + resultString);
            System.out.println("[" + memberID + ":Learner]: request proposers to propose again");
            // requestProposeAgain();

        }
    }

    private void requestProposeAgain() {
        for (Map.Entry<String, String> proposer : urlProposerMap.entrySet()) {
            String[] strings = proposer.getValue().split(":", 2);
            String proposerDomain = strings[0];
            int proposerPort = Integer.parseInt(strings[1]);
            Socket proposerSocket;
            try {
                proposerSocket = new Socket(proposerDomain, proposerPort);
                DataOutputStream dataOutputStream = new DataOutputStream(proposerSocket.getOutputStream());

                SocketUtils.sendString(dataOutputStream, "re-propose");
                SocketUtils.sendString(dataOutputStream, String.valueOf(currentProposeNumber + 1));
            } catch (IOException e) {
                System.out.println("[" + memberID + ":Learner]: failed to send re-propose request");
                e.printStackTrace();
            }

        }
    }

}
