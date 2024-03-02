import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    // Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is utxoPool.
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    // Returns true if the transaction is valid, false otherwise
    public boolean isValidTx(Transaction tx) {
        double inputSum = 0;
        double outputSum = 0;
        Set<UTXO> usedUTXOs = new HashSet<>();

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output prevOutput = utxoPool.getTxOutput(utxo);

            // Check if output claimed by tx is in the current UTXO pool
            if (!utxoPool.contains(utxo)) return false;

            // Check if the signature on the input is valid
            PublicKey pubKey = prevOutput.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = input.signature;
            if (!Crypto.verifySignature(pubKey, message, signature)) return false;

            // Check for double spending
            if (usedUTXOs.contains(utxo)) return false;
            usedUTXOs.add(utxo);

            inputSum += prevOutput.value;
        }

        for (Transaction.Output output : tx.getOutputs()) {
            // Check if all output values are non-negative
            if (output.value < 0) return false;
            outputSum += output.value;
        }

        // Check if the sum of input values is greater than or equal to the sum of output values
        return inputSum >= outputSum;
    }

    // Handles each epoch of transaction processing
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> validTxs = new ArrayList<>();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                validTxs.add(tx);

                // Remove spent outputs from the UTXO pool
                for (Transaction.Input input : tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }

                // Add new outputs to the UTXO pool
                byte[] txHash = tx.getHash();
                for (int i = 0; i < tx.numOutputs(); i++) {
                    UTXO utxo = new UTXO(txHash, i);
                    utxoPool.addUTXO(utxo, tx.getOutput(i));
                }
            }
        }

        return validTxs.toArray(new Transaction[0]);
    }
} 