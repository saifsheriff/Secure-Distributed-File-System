/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dfs.server;

import java.io.Serializable;

/**
 * Represents a write operation sent between nodes during TO-Multicast.
 * Copy this file into BOTH Server and Client projects.
 *
 * Implements Comparable so PriorityQueue auto-sorts by:
 *   1. logicalClock (lower = higher priority)
 *   2. nodeId as tie-breaker (lower nodeId wins)
 */
public class WriteRequest implements Serializable, Comparable<WriteRequest> {

    private static final long serialVersionUID = 1L;

    // The three write operation types
    public enum OperationType {
        UPLOAD, DELETE, RENAME
    }

    private String txId;            // Unique transaction ID (UUID)
    private OperationType opType;   // What kind of write
    private String fileName;        // Target file name
    private String newFileName;     // Only used for RENAME
    private FileSerializable file;  // Only used for UPLOAD (contains byte data)
    private long logicalClock;      // Lamport clock value at time of send
    private int nodeId;             // Which node originated this request

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

    /**
     * Priority queue ordering:
     * Lower clock = processed first.
     * Same clock → lower nodeId processed first (deterministic tie-break).
     */
    @Override
    public int compareTo(WriteRequest other) {
        if (this.logicalClock != other.logicalClock) {
            return Long.compare(this.logicalClock, other.logicalClock);
        }
        return Integer.compare(this.nodeId, other.nodeId);
    }

    // Getters
    public String getTxId()            { return txId; }
    public OperationType getOpType()   { return opType; }
    public String getFileName()        { return fileName; }
    public String getNewFileName()     { return newFileName; }
    public FileSerializable getFile()  { return file; }
    public long getLogicalClock()      { return logicalClock; }
    public int getNodeId()             { return nodeId; }
}