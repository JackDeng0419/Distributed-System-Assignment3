import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CouncilVoter {

    private int port;
    private String memberID;
    private int acceptorCount;
    private Set<String> proposerList;
    private String voteChoice;
    private Map<String, String> urlLearnerMap;
    private Map<String, Integer> voteRecord;
    private boolean hasPresidentResult = false;

    /*
     * Input:
     * 1. port: the port number
     * 2. memberID: e.g. M1, M2, ...
     * 3. acceptorCount: the number of acceptors
     */
    public CouncilVoter(int port, String memberID, int acceptorCount) {
        this.port = port;
        this.memberID = memberID;
        this.proposerList = new HashSet<String>();
        this.voteChoice = null;
        this.urlLearnerMap = new UrlList().getUrlLearnerMap();
        this.voteRecord = new HashMap<>();
        this.acceptorCount = acceptorCount;
    }

    /*
     * start to accept proposal and record voting
     */
    public void start() {
        System.out.println("[" + memberID + "]: start");
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (!hasPresidentResult) {
                final Socket requestSocket = serverSocket.accept();
                DataInputStream dataInputStream = new DataInputStream(requestSocket.getInputStream());
                String messageType = SocketUtils.readString(dataInputStream);

                DataOutputStream dataOutputStream = new DataOutputStream(requestSocket.getOutputStream());

                switch (messageType) {
                    case "prepare": {
                        // read the proposerID from the message
                        String proposerID = SocketUtils.readString(dataInputStream);
                        System.out.println("[" + memberID + "]: received prepare message from" + proposerID);

                        proposerList.add(proposerID);

                        // send response to the proposer
                        SocketUtils.sendString(dataOutputStream, "prepare received");
                        SocketUtils.sendString(dataOutputStream, memberID);
                        break;
                    }
                    case "accept": {
                        String proposerID = SocketUtils.readString(dataInputStream);
                        System.out.println("[" + memberID + "]: received accept message from" + proposerID);
                        if (voteChoice == null) {
                            /* wait 3 seconds and then return the vote choice */
                            voteChoice = "considering";
                            AcceptHandler acceptHandler = new AcceptHandler();
                            new Thread(acceptHandler).start();
                        } else {
                            // still considering, add the request to the waiting queue
                            // already get the voting choice, directly return the vote choice
                            SocketUtils.sendString(dataOutputStream, "accept message received by " + memberID);
                        }
                        break;
                    }
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
            System.out.println("[" + memberID + "]" + "failed to start.");
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

        if (voteRecord.get(voteTo) >= acceptorCount / 2 + 1) {
            hasPresidentResult = true;
            System.out.println("[" + memberID + "]: " + voteTo + "is the new president.");
        }
    }

    class AcceptHandler implements Runnable {

        // private Set<String> proposerList;
        // private String[] voteChoice;
        // private String memberID;

        // public AcceptHandler(Set<String> proposerList, String[] voteChoice, String
        // memberID) {
        // this.proposerList = proposerList;
        // this.voteChoice = voteChoice;
        // this.memberID = memberID;
        // }

        public AcceptHandler() {

        }

        /*
         * This thread waits 2 seconds, randomly pick a proposer from the
         * proposer list, and then broadcast the voting result to all the learners.
         */
        @Override
        public void run() {
            try {
                Thread.sleep(2000);
                // decide the vote choice randomly
                String selectedID = getRandomProposer();
                if (!selectedID.equals("no proposer")) {
                    voteChoice = selectedID;
                    System.out.println("[" + memberID + "]: vote to " + selectedID);
                    // record the vote choice
                    recordVote(selectedID);
                    // broadcast the vote result to all the learners except itself
                    for (Map.Entry<String, String> urlLearnerSet : urlLearnerMap.entrySet()) {
                        if (!urlLearnerSet.getKey().equals(memberID)) {
                            String[] domainPort = urlLearnerSet.getValue().split(":", 2); // e.g. ["127.0.0.1", "9001"]
                            Socket learnerSocket = new Socket(domainPort[0], Integer.parseInt(domainPort[1]));
                            DataOutputStream dataOutputStream = new DataOutputStream(learnerSocket.getOutputStream());
                            SocketUtils.sendString(dataOutputStream, "vote");
                            SocketUtils.sendString(dataOutputStream, voteChoice);
                            learnerSocket.close();
                        }
                    }

                }
            } catch (InterruptedException | IOException e) {
                System.out.println("AcceptHandler error");
                e.printStackTrace();
            }
        }

        /*
         * This method randomly get an item from the proposerList, return the selected
         * proposerID
         */
        private String getRandomProposer() {
            int size = proposerList.size();

            if (size == 0) {
                return "no proposer";
            } else {
                int item = new Random().nextInt(size);
                int i = 0;
                String selectedID = "no proposer";

                for (String proposerID : proposerList) {
                    if (i == item)
                        selectedID = proposerID;
                    i++;
                }
                return selectedID;
            }

        }

    }

}
