import java.util.HashSet;
import java.util.Set;

public class CompliantNode implements Node {

    private boolean[] followees;
    private Set<Transaction> pendingTransactions;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        // Constructor implementation, if needed
    }

    @Override
    public void setFollowees(boolean[] followees) {
        this.followees = followees.clone(); // Copy the followees array
    }

    @Override
    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = new HashSet<>(pendingTransactions); // Copy the pending transactions
    }

    @Override
    public Set<Transaction> sendToFollowers() {
        return new HashSet<>(pendingTransactions); // Return a copy of pending transactions to followers
    }

    @Override
    public void receiveFromFollowees(Set<Candidate> candidates) {
        for (Candidate candidate : candidates) {
            if (followees[candidate.sender]) {
                pendingTransactions.add(candidate.tx); // Add transactions received from followees
            }
        }
    }
}