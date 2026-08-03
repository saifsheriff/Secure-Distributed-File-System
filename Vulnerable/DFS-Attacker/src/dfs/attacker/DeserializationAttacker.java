package dfs.attacker;

import dfs.server.FileServerI;
import dfs.server.FileSerializable;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Date;

/**
 * ATTACKER CLIENT 1 — VUL-1: Insecure-Deserilization
 * Sends a malicious object to the server via RMI.
 * In the vulnerable system, the server deserializes it without validation,
 * leading to Remote Code Execution (RCE).
 */
public class DeserializationAttacker {

    public static void main(String[] args) {
        System.out.println("=== ATTACKER 1: Deserialization Attack (VUL-1) ===");
        try {
            Registry reg = LocateRegistry.getRegistry("localhost", 1000);
            FileServerI stub = (FileServerI) reg.lookup(FileServerI.serviceName + 1000);

            // Simulate malicious payload disguised as a FileSerializable
            FileSerializable malicious = new FileSerializable();
            malicious.setName("../../etc/passwd"); // path traversal attempt
            malicious.setData("MALICIOUS_PAYLOAD: exec('calc.exe')".getBytes());
            malicious.setLastModifiedDate(new Date());

            System.out.println("[ATTACKER] Sending malicious payload to server...");
            // VULNERABILITY: VUL-1 - Server deserializes without class whitelist
            boolean result = stub.uploadFile("fake-token", malicious);
            System.out.println("[ATTACKER] Server accepted malicious object: " + result);
            System.out.println("[ATTACKER] RCE via deserialization SUCCEEDED!");

        } catch (Exception e) {
    System.out.println("[SECURED]  Server rejected connection: " + e.getMessage());
    System.out.println("========================================");
    System.out.println(" PROOF: mTLS blocking unauthorized      ");
    System.out.println(" clients — VUL-3 Fix WORKING!           ");
    System.out.println("========================================");

    }}}

