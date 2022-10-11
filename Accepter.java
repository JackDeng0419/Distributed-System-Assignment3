/**
 * Accepter
 */
public interface Accepter {

    public void receivePrepare(String senderID, ProposalID proposalID);

    public void receiveAcceptRequest(String senderID, ProposalID proposalID, int value);

}