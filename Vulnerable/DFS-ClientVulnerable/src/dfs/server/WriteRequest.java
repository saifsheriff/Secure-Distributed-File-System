/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dfs.server;

import java.io.Serializable;

public class WriteRequest implements Serializable, Comparable<WriteRequest> {

    private static final long serialVersionUID = 1L;

    public enum OperationType { UPLOAD, DELETE, RENAME }

    private String txId;
    private OperationType opType;
    private String fileName;
    private String newFileName;
    private FileSerializable file;
    private long logicalClock;
    private int nodeId;

    public WriteRequest(String txId, OperationType opType, String fileName,
                        String newFileName, FileSerializable file,
                        long logicalClock, int nodeId) {
        this.txId = txId;
        this.opType = opType;
        this.fileName = fileName;
        this.newFileName = newFileName;
        this.file = file;
        this.logicalClock = logicalClock;
        this.nodeId = nodeId;
    }

    @Override
    public int compareTo(WriteRequest other) {
        if (this.logicalClock != other.logicalClock) {
            return Long.compare(this.logicalClock, other.logicalClock);
        }
        return Integer.compare(this.nodeId, other.nodeId);
    }

    public String getTxId()           { return txId; }
    public OperationType getOpType()  { return opType; }
    public String getFileName()       { return fileName; }
    public String getNewFileName()    { return newFileName; }
    public FileSerializable getFile() { return file; }
    public long getLogicalClock()     { return logicalClock; }
    public int getNodeId()            { return nodeId; }
}