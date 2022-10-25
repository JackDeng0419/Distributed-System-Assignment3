import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/*
 *  The thread of proposers 
 */
public class Proposer implements Runnable {

    private int proposerPort;
    private String memberID;
    private Map<String, String> urlAccepterMap;
    private int accepterCount;
    private int currentProposeNumber;
    private float proposalID;
    private String proposeValue;
    private float maxAcceptedID;
    private BlockingQueue<Socket> accepterRespondPrepare;
    private BlockingQueue<Socket> accepterRespondPrepare2;
    private Object lock = new Object();

    /*
     * Input:
     * 1. proposerPort: the port number
     * 2. memberID: e.g. M1, M2, ...
     */
    public Proposer(int proposerPort, String memberID) {
        this.proposerPort = proposerPort;
        this.memberID = memberID;
        this.urlAccepterMap = new UrlList().getUrlAccepterMap();
        this.accepterCount = urlAccepterMap.size();
        this.currentProposeNumber = 0;
        this.maxAcceptedID = 0;
        accepterRespondPrepare = new LinkedBlockingQueue<>();
        accepterRespondPrepare2 = new LinkedBlockingQueue<>();

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

        sendPropose();

        // start to listen to the request of resending propose
        // try {
        // ServerSocket serverSocket = new ServerSocket(proposerPort);
        // while (true) {
        // final Socket requestSocket = serverSocket.accept();
        // DataInputStream dataInputStream = new
        // DataInputStream(requestSocket.getInputStream());
        // String messageType = SocketUtils.readString(dataInputStream);

        // switch (messageType) {
        // case "re-propose":
        // System.out.println("[" + memberID + ":Proposer]: received re-propose");
        // int newProposeNumber =
        // Integer.parseInt(SocketUtils.readString(dataInputStream));
        // if (newProposeNumber > currentProposeNumber) {
        // currentProposeNumber = newProposeNumber;
        // System.out.println("[" + memberID + ":Proposer]: resend propose with new
        // propose number: " +
        // currentProposeNumber);
        // sendPropose();
        // }
        // break;
        // default:
        // break;
        // }
        // }
        // } catch (IOException e) {
        // System.out.println("[" + memberID + ":Proposer]: failed to start the server
        // socket");
        // e.printStackTrace();
        // }

    }

    private void sendPropose() {

        // broadcast the prepare message to all accepters
        for (Map.Entry<String, String> urlAccepterSet : urlAccepterMap.entrySet()) {
            PrepareSender prepareSender = new PrepareSender(urlAccepterSet);
            Thread prepareSenderThread = new Thread(prepareSender);
            prepareSenderThread.start();
            try {
                prepareSenderThread.join(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println(
                        "[" + memberID + ":Proposer]: didn't receive " + urlAccepterSet.getKey()
                                + "'s response after waiting 3 seconds");
            }
        }

        if (accepterRespondPrepare.size() > accepterCount / 2 + 1) {
            // wait until get enough accepters
            // send the accept message to accepters that responded prepare message
            for (Socket accepterSocket : accepterRespondPrepare) {
                AcceptSender acceptSender = new AcceptSender(accepterSocket);
                new Thread(acceptSender).start();
            }
        } else {
            proposalID++;
            sendPropose();
        }

    }

    /*
     * The thread to send prepare message to accepters
     */
    class PrepareSender implements Runnable {

        private Map.Entry<String, String> urlAccepterSet;

        public PrepareSender(Map.Entry<String, String> urlAccepterSet) {
            this.urlAccepterSet = urlAccepterSet;
        }

        @Override
        public void run() {
            // increase the proposeID
            // proposalID += 1;
            String[] domainPort = urlAccepterSet.getValue().split(":", 2);
            Socket accepterSocket;
            try {
                System.out.println("[" + memberID + ":Proposer]: send prepare with proposeID: " + proposalID);
                /* prepare message */
                // send prepare
                accepterSocket = new Socket(domainPort[0], Integer.parseInt(domainPort[1]));
                DataOutputStream dataOutputStream = new DataOutputStream(accepterSocket.getOutputStream());

                sendPrepare(dataOutputStream);

                // receive prepare response
                DataInputStream dataInputStream = new DataInputStream(accepterSocket.getInputStream());

                String promiseID = SocketUtils.readString(dataInputStream);
                String promiseAcceptedID = promiseID.equals("fail") ? "" : SocketUtils.readString(dataInputStream);
                String promiseVoteChoice = promiseAcceptedID.equals("") ? "" : SocketUtils.readString(dataInputStream);

                // if (responseMessage.equals("prepare received")) {
                // // add responding accepters to the queue
                // accepterRespondPrepare.add(responseAcceptorID);
                // } else if (responseMessage.equals("your propose is old")) {
                // // // update the current propose number and resend again
                // // int newProposeNumber =
                // // Integer.parseInt(SocketUtils.readString(dataInputStream));
                // // currentProposeNumber = newProposeNumber;
                // // sendPrepare(dataOutputStream);
                // }
                System.out.println("[" + memberID + ":Proposer]: received prepare respond");

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

            // this.accepterSocket = new Socket(accepterDomain, accepterPort);

            // String[] strings = this.accepterURL.split(":", 2);
            // this.accepterDomain = strings[0];
            // this.accepterPort = Integer.parseInt(strings[1]);
        }

        @Override
        public void run() {
            System.out.println("[" + memberID + ":Proposer]: send accept message with value: " + proposeValue);

            try {
                /* accept message */
                // send accept
                accepterSocket = new Socket(accepterDomain, accepterPort);

                DataOutputStream dataOutputStream = new DataOutputStream(accepterSocket.getOutputStream());
                SocketUtils.sendString(dataOutputStream, "accept");
                SocketUtils.sendString(dataOutputStream, proposeValue);
                SocketUtils.sendString(dataOutputStream, Float.toString(proposalID));

                // receive accept response
                DataInputStream dataInputStream = new DataInputStream(accepterSocket.getInputStream());
                String responseMessage = SocketUtils.readString(dataInputStream);
                String voteChoice = responseMessage.equals("") ? "" : SocketUtils.readString(dataInputStream);

                // String responseAcceptorID = SocketUtils.readString(dataInputStream);

                if (responseMessage.equals("accepted")) {
                    System.out.println(
                            "[" + memberID + ":Proposer]: accepted by the accepter");
                }
            } catch (NumberFormatException | IOException e) {
                System.out.println("[" + memberID + ":Proposer]: failed to send accept");
                e.printStackTrace();
            }
        }

    }
}