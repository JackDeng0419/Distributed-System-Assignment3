import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/*
 *  The thread of proposers 
 */
public class Proposer implements Runnable {

    private String memberID;
    private Map<String, AccepterInfo> accepterMap;

    private int accepterCount;
    private float proposalID;
    private String proposeValue;
    private float maxAcceptedID;
    private BlockingQueue<Socket> accepterRespondPrepare;
    private ConcurrentHashMap<String, Integer> voteRecord = new ConcurrentHashMap<>(); // stores the voteCount for each
    private int profile;
    private int respondAccepterCount = 0;
    private int noRespondRetry = 0;
    private CountDownLatch prepareCountDownLatch;
    private CountDownLatch acceptCountDownLatch;

    int acceptedCount = 0;
    private Object lock = new Object();

    /*
     * Input:
     * 1. proposerPort: the port number
     * 2. memberID: e.g. M1, M2, ...
     */
    public Proposer(int proposerPort, String memberID) {
        this.memberID = memberID;
        this.accepterMap = ConfigurationUtils.accepterMap;

        this.accepterCount = accepterMap.size();
        this.maxAcceptedID = 0;
        accepterRespondPrepare = new LinkedBlockingQueue<>();

        this.proposalID = Float.parseFloat("0.0" + memberID.substring(1));
        this.proposalID += ThreadLocalRandom.current().nextInt(0, 10) * 0.1;
        this.proposeValue = memberID; // vote to self

    }

    /*
     * This thread sends the propose message to the accepters, and then start a new
     * thread to receive the request of resending propose
     */
    @Override
    public void run() {

        System.out.println("[" + memberID + ":Proposer]: start with proposeID: " + proposalID);

        try {
            sendPropose();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(-1);
            e.printStackTrace();
        }

        System.out.println("[" + memberID + ":Proposer]: Done");

    }

    private void sendPropose() throws Exception {

        // randomly wait 0 ~ 1 second to reduce competition
        WaitUtils.sleepMillisecond(ThreadLocalRandom.current().nextInt(10, 40) * 100);
        // broadcast the prepare message to all accepters

        prepareCountDownLatch = new CountDownLatch(accepterCount);
        // ArrayList<Thread> prepareSenderThreadList = new ArrayList<>();
        for (String accepterMemberID : accepterMap.keySet()) {
            AccepterInfo accepterInfo = accepterMap.get(accepterMemberID);
            PrepareSender prepareSender = new PrepareSender(accepterInfo.ip, accepterInfo.port);
            Thread prepareSenderThread = new Thread(prepareSender);
            // prepareSenderThreadList.add(prepareSenderThread);
            prepareSenderThread.start();
        }

        prepareCountDownLatch.await(4, TimeUnit.SECONDS);

        // prepareSenderThreadList.forEach(prepareSenderThread -> {
        // try {
        // prepareSenderThread.join(4000);
        // } catch (InterruptedException e) {
        // System.out.println(
        // "[" + memberID + ":Proposer]: thread join fail");
        // }
        // });
        // prepareSenderThreadList = null;

        if (accepterRespondPrepare.size() > accepterCount / 2 + 1) {
            // wait until get enough accepters
            // send the accept message to accepters that responded prepare message

            acceptCountDownLatch = new CountDownLatch(accepterRespondPrepare.size());
            // ArrayList<Thread> acceptSenderThreadList = new ArrayList<>();
            for (Socket accepterSocket : accepterRespondPrepare) {
                AcceptSender acceptSender = new AcceptSender(accepterSocket);
                Thread acceptSenderThread = new Thread(acceptSender);
                // acceptSenderThreadList.add(acceptSenderThread);
                acceptSenderThread.start();

            }

            acceptCountDownLatch.await(4, TimeUnit.SECONDS);

            // acceptSenderThreadList.forEach(acceptSenderThread -> {
            // try {
            // acceptSenderThread.join(4000);
            // } catch (InterruptedException e) {
            // System.out.println(
            // "[" + memberID + ":Proposer]: thread join fail");
            // }
            // });

            // acceptSenderThreadList = null;

            if (acceptedCount < accepterCount / 2 + 1) {
                synchronized (lock) {
                    voteRecord.clear();
                    acceptedCount = 0;
                    proposalID++;
                }
                sendPropose();
            } else {
                for (String voteChoice : voteRecord.keySet()) {

                    if (voteRecord.get(voteChoice) > acceptedCount / 2 + 1) {
                        System.out
                                .println("[" + memberID + ":Proposer]: " + voteChoice + " is the new president. ");
                    }
                }
            }

        } else {
            /* fail to get majority promise for prepare */

            // if even less than the majority send back response
            if (respondAccepterCount < accepterCount / 2 + 1) {
                noRespondRetry++;
                if (noRespondRetry > 3) {
                    throw new Exception("no majority of accepters are running, retried three times, exit...");
                }
            }

            // reset the counter
            synchronized (lock) {
                respondAccepterCount = 0;
                accepterRespondPrepare.clear();
                proposalID++;
            }
            sendPropose();
        }

    }

    /*
     * The thread to send prepare message to accepters
     */
    class PrepareSender implements Runnable {

        private String accepterIp;
        private int accepterPort;

        public PrepareSender(String accepterIp, int accepterPort) {
            this.accepterIp = accepterIp;
            this.accepterPort = accepterPort;
        }

        @Override
        public void run() {
            // increase the proposeID
            // proposalID += 1;
            Socket accepterSocket;
            try {

                System.out.println("[" + memberID + ":Proposer]: send prepare with proposeID: " + proposalID);
                /* prepare message */
                // send prepare
                accepterSocket = new Socket(accepterIp, accepterPort);
                accepterSocket.setSoTimeout(6000);
                DataOutputStream dataOutputStream = new DataOutputStream(accepterSocket.getOutputStream());

                sendPrepare(dataOutputStream);

                // receive prepare response
                DataInputStream dataInputStream = new DataInputStream(accepterSocket.getInputStream());

                String promiseID = SocketUtils.readString(dataInputStream);
                String promiseAcceptedID = promiseID.equals("fail") ? "" : SocketUtils.readString(dataInputStream);
                String promiseVoteChoice = promiseAcceptedID.equals("") ? ""
                        : SocketUtils.readString(dataInputStream);

                System.out.println("[" + memberID + ":Proposer]: received prepare respond");

                synchronized (lock) {
                    respondAccepterCount++;
                }

                if (promiseID.equals("fail")) {
                    // resend the prepare with a higher promiseID
                    accepterSocket.close();
                    // run();
                } else {

                    // add responding accepters to the queue

                    accepterRespondPrepare.add(accepterSocket);
                    if (promiseAcceptedID.equals("")) {
                        /* received the promise from the accepter */

                    } else {
                        /* accepter already accept another proposal */
                        float promiseAcceptedIDFloat = Float.parseFloat(promiseAcceptedID);

                        if (promiseAcceptedIDFloat > maxAcceptedID) {
                            // change the value to the accepted value with highest accepted ID
                            maxAcceptedID = promiseAcceptedIDFloat;
                            proposeValue = promiseVoteChoice;
                        }
                    }
                }
                prepareCountDownLatch.countDown();
            } catch (SocketException e) {
                System.out.println("[" + memberID + ":Proposer]: accepter's socket closed");
            } catch (SocketTimeoutException e) {
                System.out.println("[" + memberID + ":Proposer]: exceed max prepare waiting time");
            } catch (NumberFormatException | IOException e) {
                System.out.println("[" + memberID + ":Proposer]: failed to send prepare");
                e.printStackTrace();
            }
        }

        private void sendPrepare(DataOutputStream dataOutputStream) throws IOException {
            SocketUtils.sendString(dataOutputStream, "prepare");
            SocketUtils.sendString(dataOutputStream, memberID);
            SocketUtils.sendString(dataOutputStream, Float.toString(proposalID));
        }

    }

    /* The thread to send accept message to accepters */
    class AcceptSender implements Runnable {

        // private String accepterURL;
        private InetAddress accepterDomain;
        private int accepterPort;

        private Socket accepterSocket;

        public AcceptSender(Socket accepterSocket) {
            this.accepterPort = accepterSocket.getPort();
            this.accepterDomain = accepterSocket.getInetAddress();
        }

        @Override
        public void run() {
            System.out.println("[" + memberID + ":Proposer]: send accept message with value: " + proposeValue);

            try {
                /* accept message */
                // send accept
                accepterSocket = new Socket(accepterDomain, accepterPort);

                accepterSocket.setSoTimeout(6000);
                DataOutputStream dataOutputStream = new DataOutputStream(accepterSocket.getOutputStream());
                SocketUtils.sendString(dataOutputStream, "accept");
                SocketUtils.sendString(dataOutputStream, proposeValue);
                SocketUtils.sendString(dataOutputStream, Float.toString(proposalID));

                // receive accept response
                DataInputStream dataInputStream = new DataInputStream(accepterSocket.getInputStream());
                String responseMessage = SocketUtils.readString(dataInputStream);
                String voteChoice = responseMessage.equals("") ? "" : SocketUtils.readString(dataInputStream);

                if (responseMessage.equals("accepted")) {
                    System.out.println(
                            "[" + memberID + ":Proposer]: accepted by the accepter");
                    if (voteRecord.get(voteChoice) != null) {
                        voteRecord.put(voteChoice, voteRecord.get(voteChoice) + 1);
                    } else {
                        voteRecord.put(voteChoice, 1);
                    }

                    synchronized (lock) {
                        acceptedCount++;
                    }
                }
                acceptCountDownLatch.countDown();
            } catch (SocketException e) {
                System.out.println("[" + memberID + ":Proposer]: accepter's socket closed");
            } catch (SocketTimeoutException e) {
                System.out.println("[" + memberID + ":Proposer]: exceed max accept waiting time");
            } catch (NumberFormatException | IOException e) {
                System.out.println("[" + memberID + ":Proposer]: failed to send accept");
                e.printStackTrace();
            }
        }

    }
}