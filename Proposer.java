import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;

/*
 *  The thread of proposers 
 */
public class Proposer implements Runnable {

    private int proposerPort;
    private String memberID;
    private Map<String, String> urlAccepterMap;
    private int currentProposeNumber;

    /*
     * Input:
     * 1. proposerPort: the port number
     * 2. memberID: e.g. M1, M2, ...
     */
    public Proposer(int proposerPort, String memberID) {
        this.proposerPort = proposerPort;
        this.memberID = memberID;
        this.urlAccepterMap = new UrlList().getUrlAccepterMap();
        this.currentProposeNumber = 0;
    }

    /*
     * This thread sends the propose message to the accepters, and then start a new
     * thread to receive the request of resending propose
     */
    @Override
    public void run() {
        System.out.println("[" + memberID + "]: start");

        // send the propose for the first time

        // after the first propose, start a thread to receive the resend request of
        // resending the propose

        // try {
        // // ServerSocket serverSocket = new ServerSocket(proposerPort);

        // } catch (IOException e) {
        // System.out.println("[" + memberID + ":Proposer] failed to start.");
        // e.printStackTrace();
        // }
    }

    private void sendPropose() {
        // send prepare
        sendPrepare();

        // send accept
        sendAccept();
    }

    /*
     * Sending prepare message to every accepter
     */
    private void sendPrepare() throws NumberFormatException, UnknownHostException, IOException {
        for (Map.Entry<String, String> urlAccepterSet : urlAccepterMap.entrySet()) {
            String[] domainPort = urlAccepterSet.getValue().split(":", 2);
            Socket accepterSocket = new Socket(domainPort[0], Integer.parseInt(domainPort[1]));
            DataOutputStream dataOutputStream = new DataOutputStream(accepterSocket.getOutputStream());
            SocketUtils.sendString(dataOutputStream, "prepare");
            SocketUtils.sendString(dataOutputStream, memberID);
            accepterSocket.close();
        }
    }

    private void sendAccept() {
        
    }
}