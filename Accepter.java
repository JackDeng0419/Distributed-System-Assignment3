import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

/*
* The thread of accepters
*/
class Accepter extends Thread {

    private int accepterPort;
    private String memberID;
    private String voteChoice;
    private boolean proposalAccepted = false;
    private float acceptedID;
    private Map<String, AccepterInfo> accepterMap;
    private float maxProposalID = 0;
    private Object lock = new Object();
    private ConcurrentHashMap<String, Integer> voteRecord = new ConcurrentHashMap<>(); // stores the voteCount for each
    private boolean hasResult = false;
    private int accepterCount;
    private ConcurrentHashMap<String, String> finalRecord;
    private int profile;
    private CountDownLatch sendLearnerCountDown;

    public Accepter(int accepterPort, int accepterCount, String memberID,
            ConcurrentHashMap<String, String> finalRecord, int profile) {
        this.accepterPort = accepterPort;
        this.memberID = memberID;
        this.accepterMap = ConfigurationUtils.accepterMap;
        this.voteChoice = null;
        this.accepterCount = accepterCount;
        this.finalRecord = finalRecord;
        this.profile = profile;
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
                MessageReceiver messageReceiver = new MessageReceiver(requestSocket);
                new Thread(messageReceiver).start();
            }
        } catch (IOException e) {
            System.out.println("[" + memberID + ":Accepter]: failed to start");
            e.printStackTrace();
        }

    }

    /* 
     * * Parameter: the dataInputStream and dataOutputStream of the proposer socket
     */
    private void prepareHandler(DataInputStream dataInputStream, DataOutputStream dataOutputStream) {

        // read the proposerID from the message
        try {

            String proposerID = SocketUtils.readString(dataInputStream);
            float proposalID = Float.parseFloat(SocketUtils.readString(dataInputStream));
            System.out.println(
                    "[" + memberID + ":Accepter]: received prepare with proposeID: " + proposalID);
            if (proposalID > maxProposalID) {

                synchronized (lock) {
                    maxProposalID = proposalID;
                }
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
            } else {
                System.out
                        .println("[" + memberID
                                + ":Accepter]: received prepare with a smaller proposalID from "
                                + proposerID + ", proposalID:" + proposalID);

                // send response to the proposer
                SocketUtils.sendString(dataOutputStream, "fail");
            }
        } catch (Exception e) {
            System.out.println("[" + memberID + ":Accepter]: fail to handler prepare");
        }

    }

    /* 
     * Parameter: the dataInputStream and dataOutputStream of the proposer socket
     */
    private void acceptHandler(DataInputStream dataInputStream, DataOutputStream dataOutputStream) {

        try {
            String proposeValue = SocketUtils.readString(dataInputStream);
            float proposalID = Float.parseFloat(SocketUtils.readString(dataInputStream));
            System.out.println(
                    "[" + memberID + ":Accepter]: received accept with proposeID: " + proposalID);

            if (proposalID == maxProposalID) {
                System.out
                        .println("[" + memberID + ":Accepter]: received accept message with value: "
                                + proposeValue
                                + ", proposalID:" + proposalID);

                synchronized (lock) {
                    proposalAccepted = true;
                    acceptedID = proposalID;
                    voteChoice = proposeValue;
                }

                SocketUtils.sendString(dataOutputStream, "accepted");
                SocketUtils.sendString(dataOutputStream, voteChoice);

                sendLearnerCountDown = new CountDownLatch(accepterMap.size());
                for (String learnerMemberID : accepterMap.keySet()) {

                    AccepterInfo accepterInfo = accepterMap.get(learnerMemberID);

                    SendVoteToLearner sendVoteToLearner = new SendVoteToLearner(accepterInfo.ip, accepterInfo.port);
                    Thread sendVoteToLearnerThread = new Thread(sendVoteToLearner);
                    sendVoteToLearnerThread.start();
                }
                sendLearnerCountDown.await();

            } else {

                // send response to the proposer
                SocketUtils.sendString(dataOutputStream, "");
            }
        } catch (Exception e) {
            System.out.println("[" + memberID + ":Accepter]: fail to handler accept");
        }

    }


    /* 
     * Parameter: the dataInputStream and dataOutputStream of the accepter socket that sends the vote choice
     */
    private void acceptedHandler(DataInputStream dataInputStream, DataOutputStream dataOutputStream) {

        try {
            String voteFrom = SocketUtils.readString(dataInputStream);
            String voteTo = SocketUtils.readString(dataInputStream);
            System.out.println(
                    "[" + memberID + ":Accepter]: accepted by the accepter");
            if (voteRecord.get(voteTo) != null) {
                voteRecord.put(voteTo, voteRecord.get(voteTo) + 1);
            } else {
                voteRecord.put(voteTo, 1);
            }
            if (!hasResult & voteRecord.get(voteTo) > accepterCount / 2 + 1) {

                synchronized (lock) {
                    hasResult = true;
                }
                System.out
                        .println("[" + memberID + ":Accepter]: " + voteTo + " is the new president. ");
                finalRecord.put(memberID, voteTo);
            }
        } catch (Exception e) {
            System.out.println("[" + memberID + ":Accepter]: fail to handler accepted");
        }
    }


    /* 
     * This thread accepts the socket request and distributes it to corresponding handler 
     */
    class MessageReceiver implements Runnable {

        private Socket requestSocket;

        public MessageReceiver(Socket requestSocket) {
            this.requestSocket = requestSocket;
        }

        @Override
        public void run() {
            try {
                DataInputStream dataInputStream = new DataInputStream(requestSocket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(requestSocket.getOutputStream());
                String messageType = SocketUtils.readString(dataInputStream);

                switch (messageType) {
                    case "prepare": {
                        WaitUtils.sleepMillisecond(profile == Constant.PROFILE_IMMEDIATE ? profile
                                : ThreadLocalRandom.current().nextInt(profile, profile + 3000));

                        prepareHandler(dataInputStream, dataOutputStream);
                        break;
                    }
                    case "accept": {
                        WaitUtils.sleepMillisecond(profile == Constant.PROFILE_IMMEDIATE ? profile
                                : ThreadLocalRandom.current().nextInt(profile, profile + 3000));

                        acceptHandler(dataInputStream, dataOutputStream);
                        break;
                    }
                    case "accepted": {
                        acceptedHandler(dataInputStream, dataOutputStream);
                        break;
                    }
                    default:
                        break;
                }
            } catch (SocketException e) {
                System.out.println("[" + memberID + ":Proposer]: proposer's socket closed");
            } catch (IOException e) {
                System.out.println("[" + memberID + ":Accepter]: fail to process request");
                e.printStackTrace();
            }
        }

    }


    /* 
     * This thread sends the vote choice to each accepter (learner)
     */
    class SendVoteToLearner implements Runnable {

        String learnerIp;
        int learnerPort;

        public SendVoteToLearner(String learnerIp, int learnerPort) {
            this.learnerIp = learnerIp;
            this.learnerPort = learnerPort;
        }

        @Override
        public void run() {

            try {
                Socket learnerSocket = new Socket(learnerIp, learnerPort);
                DataOutputStream learnerDataOutputStream = new DataOutputStream(
                        learnerSocket.getOutputStream());
                SocketUtils.sendString(learnerDataOutputStream, "accepted");
                SocketUtils.sendString(learnerDataOutputStream, memberID);
                SocketUtils.sendString(learnerDataOutputStream, voteChoice);
                sendLearnerCountDown.countDown();
            } catch (Exception e) {
                System.out.println("Fail to send vote to learner");
            }

        }

    }

}
