import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/*
* The thread of accepters
*/
class Accepter extends Thread {

    private int accepterPort;
    private String memberID;
    private String voteChoice;
    private Set<String> proposerList;
    private Map<String, String> urlLearnerMap;
    private int currentProposeNumber = 0;
    private Object lock = new Object();

    public Accepter(int accepterPort, String memberID) {
        this.accepterPort = accepterPort;
        this.memberID = memberID;
        this.proposerList = new HashSet<String>();
        this.urlLearnerMap = new UrlList().getUrlLearnerMap();
        this.voteChoice = null;
    }

    /*
     * This thread receives the prepare and accept request
     */
    @Override
    public void run() {
        System.out.println(
                "[" + memberID + ":Accepter]: Thread ID:  " + Thread.currentThread().getName());
        try {
            ServerSocket serverSocket = new ServerSocket(accepterPort);
            while (true) {
                final Socket requestSocket = serverSocket.accept();
                DataInputStream dataInputStream = new DataInputStream(requestSocket.getInputStream());
                String messageType = SocketUtils.readString(dataInputStream);

                DataOutputStream dataOutputStream = new DataOutputStream(requestSocket.getOutputStream());

                switch (messageType) {
                    case "prepare": {
                        // read the proposerID from the message
                        String proposerID = SocketUtils.readString(dataInputStream);
                        int proposeNumber = Integer.parseInt(SocketUtils.readString(dataInputStream));
                        if (proposeNumber >= currentProposeNumber) {
                            if (proposeNumber > currentProposeNumber) {
                                synchronized (lock) {
                                    // a totally new propose, reset the state
                                    currentProposeNumber = proposeNumber;
                                    proposerList.clear();
                                    voteChoice = null;
                                }
                            }

                            System.out.println(
                                    "[" + memberID + ":Accepter]: received prepare message from " + proposerID
                                            + ", proposeNumber:" + proposeNumber);

                            proposerList.add(proposerID);

                            // send response to the proposer
                            SocketUtils.sendString(dataOutputStream, "prepare received");
                            SocketUtils.sendString(dataOutputStream, memberID);
                        } else {
                            System.out
                                    .println("[" + memberID + ":Accepter]: received an old prepare message from "
                                            + proposerID + ", proposeNumber:" + proposeNumber);

                            // send response to the proposer
                            SocketUtils.sendString(dataOutputStream, "your propose is old");
                            SocketUtils.sendString(dataOutputStream, memberID);
                            SocketUtils.sendString(dataOutputStream, String.valueOf(currentProposeNumber));
                        }
                        break;
                    }
                    case "accept": {
                        String proposerID = SocketUtils.readString(dataInputStream);
                        int proposeNumber = Integer.parseInt(SocketUtils.readString(dataInputStream));

                        if (proposeNumber >= currentProposeNumber) {
                            System.out
                                    .println("[" + memberID + ":Accepter]: received accept message from " + proposerID
                                            + ", proposeNumber:" + proposeNumber);
                            SocketUtils.sendString(dataOutputStream, "accept received");
                            SocketUtils.sendString(dataOutputStream, memberID);
                            if (voteChoice == null) {
                                /* wait 3 seconds and then return the vote choice */
                                voteChoice = "considering";
                                AcceptHandler acceptHandler = new AcceptHandler();
                                new Thread(acceptHandler).start();
                            } else {
                                // still considering, add the request to the waiting queue
                                // already get the voting choice, directly return the vote choice
                                // SocketUtils.sendString(dataOutputStream, "accept message received by " +
                                // memberID);
                            }
                        } else {
                            System.out.println(
                                    "[" + memberID + ":Accepter]: received an old accept message from " + proposerID
                                            + ", proposeNumber:" + proposeNumber);

                            // send response to the proposer
                            SocketUtils.sendString(dataOutputStream, "your propose is old");
                            SocketUtils.sendString(dataOutputStream, memberID);
                            SocketUtils.sendString(dataOutputStream, String.valueOf(currentProposeNumber));
                        }

                        break;
                    }
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("[" + memberID + ":Accepter]: failed to start.");
            e.printStackTrace();
        }

    }

    /*
     * The thread that decides the vote choice
     */
    class AcceptHandler implements Runnable {

        /*
         * This thread first waits 2 seconds, then randomly picks one proposer, and
         * finally broadcast the vote choice to all learners
         */
        @Override
        public void run() {
            try {
                Thread.sleep(2000);
                // decide the vote choice randomly
                String selectedID = getRandomProposer();
                synchronized (lock) {
                    if (!selectedID.equals("no proposer") && selectedID != null) {
                        voteChoice = selectedID;
                        System.out.println("[" + memberID + ":Accepter]: vote to " + selectedID + ", proposeNumber:"
                                + currentProposeNumber);
                        // broadcast the vote result to all the learners
                        for (Map.Entry<String, String> urlLearnerSet : urlLearnerMap.entrySet()) {
                            // System.out.println("[" + memberID + "]: send vote result to " +
                            // urlLearnerSet.getKey());
                            String[] domainPort = urlLearnerSet.getValue().split(":", 2); // e.g. ["127.0.0.1",
                                                                                          // "9001"]
                            Socket learnerSocket = new Socket(domainPort[0], Integer.parseInt(domainPort[1]));
                            DataOutputStream dataOutputStream = new DataOutputStream(
                                    learnerSocket.getOutputStream());
                            SocketUtils.sendString(dataOutputStream, "vote");
                            if (voteChoice == null) {
                                System.out.println("[" + memberID + ":Accepter]: VOTECHOICE IS NULL!!!!!!");
                            }
                            SocketUtils.sendString(dataOutputStream, voteChoice);
                            SocketUtils.sendString(dataOutputStream, String.valueOf(currentProposeNumber));
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
                    if (i == item && proposerID != null)
                        selectedID = proposerID;
                    i++;
                }

                return selectedID;
            }

        }

    }

}
