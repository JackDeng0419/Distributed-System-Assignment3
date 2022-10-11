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
class Accepter implements Runnable {

    private int accepterPort;
    private String memberID;
    private String voteChoice;
    private Set<String> proposerList;
    private Map<String, String> urlLearnerMap;

    public Accepter(int accepterPort, String memberID) {
        this.accepterPort = accepterPort;
        this.memberID = memberID;
        this.proposerList = new HashSet<String>();
        this.urlLearnerMap = new UrlList().getUrlLearnerMap();
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
                if (!selectedID.equals("no proposer")) {
                    voteChoice = selectedID;
                    System.out.println("[" + memberID + "]: vote to " + selectedID);
                    // broadcast the vote result to all the learners
                    for (Map.Entry<String, String> urlLearnerSet : urlLearnerMap.entrySet()) {
                        String[] domainPort = urlLearnerSet.getValue().split(":", 2); // e.g. ["127.0.0.1",
                                                                                      // "9001"]
                        Socket learnerSocket = new Socket(domainPort[0], Integer.parseInt(domainPort[1]));
                        DataOutputStream dataOutputStream = new DataOutputStream(
                                learnerSocket.getOutputStream());
                        SocketUtils.sendString(dataOutputStream, "vote");
                        SocketUtils.sendString(dataOutputStream, voteChoice);
                        learnerSocket.close();
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
