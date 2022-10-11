/**
 * Learner
 */
public interface Learner {
    public void receiveAccept(String senderID, ProposalID proposalID, int value);
}