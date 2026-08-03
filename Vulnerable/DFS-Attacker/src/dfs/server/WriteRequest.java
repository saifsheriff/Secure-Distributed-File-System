package dfs.server;

import java.io.Serializable;
import java.util.UUID;

public class WriteRequest implements Serializable, Comparable<WriteRequest> {

    private static final long serialVersionUID = 1L;

    public enum OperationType {
        UPLOAD, DELETE, RENAME
    }

    private String txId;
    private OperationType opType;
    private String fileName;
    private String newFileName;
    private FileSerializable file;
    private long logicalClock;
    private int nodeId;

    // BONUS: replay attack prevention fields
    private String nonce;
    private long timestamp;

    // Full constructor (with nonce + timestamp)
    public WriteRequest(String txId, OperationType opType, String fileName,
                        String newFileName, FileSerializable file,
                        long logicalClock, int nodeId,
                        String nonce, long timestamp) {
        this.txId = txId;
        this.opType = opType;
        this.fileName = fileName;
        this.newFileName = newFileName;
        this.file = file;
        this.logicalClock = logicalClock;
        this.nodeId = nodeId;
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    // Short constructor — auto generates nonce + timestamp
    // keeps FileServant.java working without any changes
    public WriteRequest(String txId, OperationType opType, String fileName,
                        String newFileName, FileSerializable file,
                        long logicalClock, int nodeId) {
        this(txId, opType, fileName, newFileName, file,
             logicalClock, nodeId,
             UUID.randomUUID().toString(),
             System.currentTimeMillis());
    }

    @Override
    public int compareTo(WriteRequest other) {
        if (this.logicalClock != other.logicalClock)
            return Long.compare(this.logicalClock, other.logicalClock);
        return Integer.compare(this.nodeId, other.nodeId);
    }

    // Getters
    public String getTxId()           { return txId; }
    public OperationType getOpType()  { return opType; }
    public String getFileName()       { return fileName; }
    public String getNewFileName()    { return newFileName; }
    public FileSerializable getFile() { return file; }
    public long getLogicalClock()     { return logicalClock; }
    public int getNodeId()            { return nodeId; }
    public String getNonce()          { return nonce; }
    public long getTimestamp()        { return timestamp; }
}