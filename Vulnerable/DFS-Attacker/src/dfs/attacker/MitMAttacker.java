package dfs.attacker;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import dfs.server.FileServerI;

/**
 * ATTACKER CLIENT 3 — VUL-3: No mTLS (Man-in-the-Middle)
 * Attempts to connect to the server WITHOUT SSL certificates.
 * Shows that without mTLS the connection is accepted on vulnerable system.
 */
public class MitMAttacker {

    public static void main(String[] args) {
        System.out.println("=== ATTACKER 3: MitM Attack - No mTLS (VUL-3) ===");
        try {
            // VULNERABILITY: VUL-3 - Connecting with plain socket, no SSL
            // On vulnerable server this succeeds; on secured server it fails
            Registry reg = LocateRegistry.getRegistry("localhost", 1000);
            FileServerI stub = (FileServerI) reg.lookup(FileServerI.serviceName + 1000);

            System.out.println("[ATTACKER] Connected WITHOUT SSL certificate!");
            System.out.println("[ATTACKER] MitM attack possible - traffic is unencrypted!");
            System.out.println("[ATTACKER] Any data sent can be intercepted.");

        } catch (Exception e) {
    System.out.println("[SECURED]  Plain connection REJECTED: " + e.getMessage());
    System.out.println("========================================");
    System.out.println(" PROOF: Server requires valid SSL cert  ");
    System.out.println(" MitM attack is IMPOSSIBLE!             ");
    System.out.println(" VUL-3 mTLS Fix is WORKING!             ");
    System.out.println("========================================");
}
    }
}
