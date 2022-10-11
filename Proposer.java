import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * Proposer
 */
public class Proposer {

    private int port;
    private String memberID;
    private int acceptorCount;
    private boolean hasPresidentResult = false;

    /*
     * Input:
     * 1. port: the port number
     * 2. memberID: e.g. M1, M2, ...
     * 3. acceptorCount: the number of acceptors
     */
    public Proposer(int port, String memberID, int acceptorCount) {
        this.port = port;
        this.memberID = memberID;
        this.acceptorCount = acceptorCount;
    }

    public void start() {
        System.out.println("[" + memberID + "]: start");
        try {
            ServerSocket serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("[" + memberID + "]" + "failed to start.");
            e.printStackTrace();
        }

    }

    class Learner implements Runnable {

        /*
         * This thread keeps listening to the vote result from the acceptors.
         */
        @Override
        public void run() {
            DataInputStream dataInputStream = new DataInputStream(null)
        }

    }
}