/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dfs.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Shared RMI interface — copy this file into BOTH Server and Client projects.
 * Every node implements this interface.
 */
public interface FileServerI extends Remote {

    // The name used to bind/lookup in the RMI registry (port appended in FileServer.java)
    String serviceName = "FileServer";

    // ── Authentication ────────────────────────────────────────────────────────
    // Returns "OK" on success, "ERROR: ..." on failure
    String register(String username, String password) throws RemoteException;
    // Returns sessionToken on success, "ERROR: ..." on failure
    String login(String username, String password) throws RemoteException;

    // ── Write operations (multicast to ALL nodes) ─────────────────────────────
    boolean uploadFile(String sessionToken, FileSerializable f) throws RemoteException;
    boolean deleteFile(String sessionToken, String fileName) throws RemoteException;
    boolean renameFile(String sessionToken, String oldName, String newName) throws RemoteException;

    // ── Read operations (handled by leader only) ──────────────────────────────
    FileSerializable downloadFile(String sessionToken, String fileName) throws RemoteException;
    boolean searchFiles(String sessionToken, String fileName) throws RemoteException;
    List<String> listFiles(String sessionToken) throws RemoteException;

    // ── Inter-node TO-Multicast communication (server-to-server only) ─────────
    void receiveWriteRequest(WriteRequest req) throws RemoteException;
    void receiveAck(String txId, int fromNodeId) throws RemoteException;
}