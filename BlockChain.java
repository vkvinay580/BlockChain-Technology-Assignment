
import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {

    private class BlockNode {
        public Block b;
        public BlockNode parent;
        public ArrayList<BlockNode> children;
        public int height;
        private UTXOPool uPool;

        public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
            this.b = b;
            this.parent = parent;
            children = new ArrayList<>();
            this.uPool = uPool;
            if (parent != null) {
                height = parent.height + 1;
                parent.children.add(this);
            } else {
                height = 1;
            }
        }

        public UTXOPool getUTXOPoolCopy() {
            return new UTXOPool(uPool);
        }
    }

    private HashMap<ByteArrayWrapper, BlockNode> blockChain;
    private BlockNode maxHeightNode;
    private TransactionPool txPool;

    public static final int CUT_OFF_AGE = 10;


    public BlockChain(Block genesisBlock) {
        blockChain = new HashMap<>();
        UTXOPool utxoPool = new UTXOPool();
        addCoinbaseToUTXOPool(genesisBlock, utxoPool);
        BlockNode genesisNode = new BlockNode(genesisBlock, null, utxoPool);
        blockChain.put(wrap(genesisBlock.getHash()), genesisNode);
        txPool = new TransactionPool();
        maxHeightNode = genesisNode;
    }

    /**
     * Get the maximum height blocks
     */
    public Block getMaxHeightBlock() {
        return maxHeightNode.b;
    }

    /**
     * Get the UTXOPool for mining a new block on top of max height blocks
     */
    public UTXOPool getMaxHeightUTXOPool() {
        return maxHeightNode.getUTXOPoolCopy();
    }

    /**
     * Get the transaction pool to mine a new blocks
     */
    public TransactionPool getTransactionPool() {
        return txPool;
    }


    public boolean addBlock(Block block) {
        byte[] prevBlockHash = block.getPrevBlockHash();
        if (prevBlockHash == null)
            return false;
        BlockNode parentBlockNode = blockChain.get(wrap(prevBlockHash));
        if (parentBlockNode == null) {
            return false;
        }
        TxHandler handler = new TxHandler(parentBlockNode.getUTXOPoolCopy());
        Transaction[] txs = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTxs = handler.handleTxs(txs);
        if (validTxs.length != txs.length) {
            return false;
        }
        int proposedHeight = parentBlockNode.height + 1;
        if (proposedHeight <= maxHeightNode.height - CUT_OFF_AGE) {
            return false;
        }
        UTXOPool utxoPool = handler.getUTXOPool();
        addCoinbaseToUTXOPool(block, utxoPool);
        BlockNode node = new BlockNode(block, parentBlockNode, utxoPool);
        blockChain.put(wrap(block.getHash()), node);
        if (proposedHeight > maxHeightNode.height) {
            maxHeightNode = node;
        }
        return true;
    }

    /**
     * Add a transaction to the transaction pool
     */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }

    private void addCoinbaseToUTXOPool(Block block, UTXOPool utxoPool) {
        Transaction coinbase = block.getCoinbase();
        for (int i = 0; i < coinbase.numOutputs(); i++) {
            Transaction.Output out = coinbase.getOutput(i);
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            utxoPool.addUTXO(utxo, out);
        }
    }

    private static ByteArrayWrapper wrap(byte[] arr) {
        return new ByteArrayWrapper(arr);
		//Blockchain lol
    }
}
