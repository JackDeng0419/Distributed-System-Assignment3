import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/*
* The thread of accepters
*/
class Accepter extends Thread {

    private int accepterPort;
    private String memberID;
    private String voteChoice;
    private boolean proposalAccepted = false;
    private float acceptedID;
    private Set<String> proposerList;
    private Map<String, String> urlLearnerMap;
    private float maxProposalID = 0;
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
        try {
            ServerSocket serverSocket = new ServerSocket(accepterPort);
            while (true) {

                final Socket requestSocket = serverSocket.accept();
                DataInputStream dataInputStream = new DataInputStream(requestSocket.getInputStream());
                String messageType = SocketUtils.readString(dataInputStream);

                DataOutputStream dataOutputStream = new DataOutputStream(requestSocket.getOutputStream());

                switch (messageType) {
                    case "prepare": {
                        TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(0, 4));

                        // read the proposerID from the message

                        String proposerID = SocketUtils.readString(dataInputStream);
                        float proposalID = Float.parseFloat(SocketUtils.readString(dataInputStream));
                        System.out.println(
                                "[" + memberID + ":Accepter]: received prepare with proposeID: " + proposalID);
                        if (proposalID > maxProposalID) {
                            maxProposalID = proposalID;
                            if (proposalAccepted) {
                                // already accepted another proposal
                                // respond: PROMISE(ID, accepted_ID, accepted_VALUE)
                                SocketUtils.sendString(dataOutputStream, Float.toString(proposalID));
                                SocketUtils.sendString(dataOutputStream, Float.toString(acceptedID));
                                SocketUtils.sendString(dataOutputStream, voteChoice);
                            } else {
                                // send back promise to the proposer
                                // respond: PROMISE(ID)
                                SocketUtils.sendString(dataOutputStream, Float.toString(proposalID));
                                SocketUtils.sendString(dataOutputStream, "");
                            }
                            // if (proposalID > maxProposalID) {
                            // synchronized (lock) {
                            // // a totally new propose, reset the state
                            // maxProposalID = proposalID;
                            // proposerList.clear();
                            // voteChoice = null;
                            // }
                            // }

                            // System.out.println(
                            // "[" + memberID + ":Accepter]: promise " + proposerID
                            // + ", proposalID:" + proposalID);

                            // proposerList.add(proposerID);

                            // // send response to the proposer
                            // SocketUtils.sendString(dataOutputStream, "prepare received");
                            // SocketUtils.sendString(dataOutputStream, memberID);
                        } else {
                            System.out
                                    .println("[" + memberID
                                            + ":Accepter]: received prepare with a smaller proposalID from "
                                            + proposerID + ", proposalID:" + proposalID);

                            // send response to the proposer
                            SocketUtils.sendString(dataOutputStream, "fail");
                            // SocketUtils.sendString(dataOutputStream, memberID);
                            // SocketUtils.sendString(dataOutputStream, String.valueOf(maxProposalID));
                        }
                        break;
                    }
                    case "accept": {
                        String proposeValue = SocketUtils.readString(dataInputStream);
                        float proposalID = Float.parseFloat(SocketUtils.readString(dataInputStream));
                        System.out.println(
                                "[" + memberID + ":Accepter]: received accept with proposeID: " + proposalID);

                        if (proposalID == maxProposalID) {
                            System.out
                                    .println("[" + memberID + ":Accepter]: received accept message with value: "
                                            + proposeValue
                                            + ", proposalID:" + proposalID);

                            proposalAccepted = true;
                            acceptedID = proposalID;
                            voteChoice = proposeValue;

                            SocketUtils.sendString(dataOutputStream, "accepted");
                            SocketUtils.sendString(dataOutputStream, voteChoice);

                            for (Map.Entry<String, String> urlLearnerSet : urlLearnerMap.entrySet()) {
                                // System.out.println("[" + memberID + "]: send vote result to " +
                                // urlLearnerSet.getKey());
                                String[] domainPort = urlLearnerSet.getValue().split(":", 2); // e.g. ["127.0.0.1",
                                                                                              // "9001"]
                                Socket learnerSocket = new Socket(domainPort[0], Integer.parseInt(domainPort[1]));
                                DataOutputStream learnerDataOutputStream = new DataOutputStream(
                                        learnerSocket.getOutputStream());
                                SocketUtils.sendString(learnerDataOutputStream, "accepted");
                                SocketUtils.sendString(learnerDataOutputStream, memberID);
                                SocketUtils.sendString(learnerDataOutputStream, voteChoice);

                                // SocketUtils.sendString(learnerDataOutputStream,
                                // String.valueOf(maxProposalID));
                                learnerSocket.close();
                            }

                            // if (voteChoice == null) {
                            // /* wait 3 seconds and then return the vote choice */
                            // voteChoice = "considering";
                            // AcceptHandler acceptHandler = new AcceptHandler();
                            // new Thread(acceptHandler).start();
                            // } else {
                            // // still considering, add the request to the waiting queue
                            // // already get the voting choice, directly return the vote choice
                            // // SocketUtils.sendString(dataOutputStream, "accept message received by " +
                            // // memberID);
                            // }
                        } else {
                            // System.out.println(
                            // "[" + memberID + ":Accepter]: received an old accept message from " +
                            // proposerID
                            // + ", proposalID:" + proposalID);

                            // send response to the proposer
                            SocketUtils.sendString(dataOutputStream, "");
                            // SocketUtils.sendString(dataOutputStream, memberID);
                            // SocketUtils.sendString(dataOutputStream, String.valueOf(maxProposalID));
                        }

                        break;
                    }
                    default:
                        break;
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("[" + memberID + ":Accepter]: failed to start.");
            e.printStackTrace();
        }

    }

    /*
     * The thread that decides the vote choice
     */
    // class AcceptHandler implements Runnable {

    // /*
    // * This thread first waits 2 seconds, then randomly picks one proposer, and
    // * finally broadcast the vote choice to all learners
    // */
    // @Override
    // public void run() {
    // try {
    // Thread.sleep(2000);
    // // decide the vote choice randomly
    // String selectedID = getRandomProposer();
    // synchronized (lock) {
    // if (!selectedID.equals("no proposer") && selectedID != null) {
    // voteChoice = selectedID;
    // System.out.println("[" + memberID + ":Accepter]: vote to " + selectedID + ",
    // proposeNumber:"
    // + maxProposalID);
    // // broadcast the vote result to all the learners
    // for (Map.Entry<String, String> urlLearnerSet : urlLearnerMap.entrySet()) {
    // // System.out.println("[" + memberID + "]: send vote result to " +
    // // urlLearnerSet.getKey());
    // String[] domainPort = urlLearnerSet.getValue().split(":", 2); // e.g.
    // ["127.0.0.1",
    // // "9001"]
    // Socket learnerSocket = new Socket(domainPort[0],
    // Integer.parseInt(domainPort[1]));
    // DataOutputStream dataOutputStream = new DataOutputStream(
    // learnerSocket.getOutputStream());
    // SocketUtils.sendString(dataOutputStream, "vote");
    // if (voteChoice == null) {
    // System.out.println("[" + memberID + ":Accepter]: VOTECHOICE IS NULL!!!!!!");
    // }
    // SocketUtils.sendString(dataOutputStream, voteChoice);
    // SocketUtils.sendString(dataOutputStream, String.valueOf(maxProposalID));
    // learnerSocket.close();
    // }
    // }
    // }
    // } catch (InterruptedException | IOException e) {
    // System.out.println("AcceptHandler error");
    // e.printStackTrace();
    // }
    // }

    // /*
    // * This method randomly get an item from the proposerList, return the selected
    // * proposerID
    // */
    // private String getRandomProposer() {
    // int size = proposerList.size();

    // if (size == 0) {
    // return "no proposer";
    // } else {
    // int item = new Random().nextInt(size);
    // int i = 0;
    // String selectedID = "no proposer";

    // for (String proposerID : proposerList) {
    // if (i == item && proposerID != null)
    // selectedID = proposerID;
    // i++;
    // }

    // return selectedID;
    // }

    // }

    // }

}
