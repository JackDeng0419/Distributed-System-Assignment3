import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/*
 *  The thread of proposers 
 */
public class Proposer implements Runnable {

    private int proposerPort;
    private String memberID;
    private Map<String, String> urlAccepterMap;
    private int accepterCount;
    private int currentProposeNumber;
    private BlockingQueue<String> acceptorRespondPrepare;

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
        acceptorRespondPrepare = new LinkedBlockingQueue<>();
    }

    /*
     * This thread sends the propose message to the accepters, and then start a new
     * thread to receive the request of resending propose
     */
    @Override
    public void run() {
        System.out.println("[" + memberID + "]: start");

        sendPropose();
    }

    private void sendPropose() {
        // broadcast the prepare message
        for (Map.Entry<String, String> urlAccepterSet : urlAccepterMap.entrySet()) {
            PrepareSender prepareSender = new PrepareSender(urlAccepterSet);
            new Thread(prepareSender).start();
        }
        while (acceptorRespondPrepare.size() < accepterCount / 2 + 1) {
            // wait until get enough accepters
        }

        // send the accept message to accepters that responded prepare message
        for (String accepterID : acceptorRespondPrepare) {
            String accepterURL = urlAccepterMap.get(accepterID);
            AcceptSender acceptSender = new AcceptSender(accepterURL);
            new Thread(acceptSender).start();
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
            String[] domainPort = urlAccepterSet.getValue().split(":", 2);
            Socket accepterSocket;
            try {
                /* prepare message */
                // send prepare
                accepterSocket = new Socket(domainPort[0], Integer.parseInt(domainPort[1]));
                DataOutputStream dataOutputStream = new DataOutputStream(accepterSocket.getOutputStream());
                SocketUtils.sendString(dataOutputStream, "prepare");
                SocketUtils.sendString(dataOutputStream, memberID);

                // receive prepare response
                DataInputStream dataInputStream = new DataInputStream(accepterSocket.getInputStream());
                String responseMessage = SocketUtils.readString(dataInputStream);
                String responseAcceptorID = SocketUtils.readString(dataInputStream);

                if (responseMessage.equals("prepare received")) {
                    // add responding accepters to the queue
                    acceptorRespondPrepare.add(responseAcceptorID);
                }
            } catch (NumberFormatException | IOException e) {
                System.out.println("[" + memberID + ":Proposer]: failed to send prepare");
                e.printStackTrace();
            }

            // receive prepare response

        }

    }

    /* The thread to send accept message to accepters */
    class AcceptSender implements Runnable {

        private String accepterURL;
        private String accepterDomain;
        private int accepterPort;

        public AcceptSender(String accepterURL) {
            this.accepterURL = accepterURL;
            String[] strings = this.accepterURL.split(":", 2);
            this.accepterDomain = strings[0];
            this.accepterPort = Integer.parseInt(strings[1]);
        }

        @Override
        public void run() {
            Socket accepterSocket;

            try {
                /* accept message */
                // send prepare
                accepterSocket = new Socket(accepterDomain, accepterPort);
                DataOutputStream dataOutputStream = new DataOutputStream(accepterSocket.getOutputStream());
                SocketUtils.sendString(dataOutputStream, "accept");
                SocketUtils.sendString(dataOutputStream, memberID);

                // receive accept response
                DataInputStream dataInputStream = new DataInputStream(accepterSocket.getInputStream());
                String responseMessage = SocketUtils.readString(dataInputStream);
                String responseAcceptorID = SocketUtils.readString(dataInputStream);

                if (responseMessage.equals("accept received")) {
                    System.out.println(
                            "[" + memberID + ":Proposer]: accept message was received by " + responseAcceptorID);

                }
            } catch (NumberFormatException | IOException e) {
                System.out.println("[" + memberID + ":Proposer]: failed to send prepare");
                e.printStackTrace();
            }
        }

    }
}