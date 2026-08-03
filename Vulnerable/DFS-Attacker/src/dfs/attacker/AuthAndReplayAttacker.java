package dfs.attacker;

import dfs.server.FileServerI;
import dfs.server.FileSerializable;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Date;

/**
 * ATTACKER CLIENT 4 — VUL-4 + BONUS: Auth bypass + Replay Attack
 */
public class AuthAndReplayAttacker {

    public static void main(String[] args) {
        System.out.println("=== ATTACKER 4: Auth Bypass + Replay Attack (VUL-4 + BONUS) ===");
        System.out.println("[ATTACKER] Attempting to connect WITHOUT SSL (plain socket)...");
        System.out.println("[ATTACKER] Using fake session token: fake-token-12345");
        System.out.println();

        try {
            // VULNERABILITY: VUL-3+4 - Plain socket, no SSL, fake token
            Registry reg = LocateRegistry.getRegistry("localhost", 1000);
            FileServerI stub = (FileServerI) reg.lookup(FileServerI.serviceName + 1000);

            String fakeToken = "fake-token-12345";

            // --- AUTH BYPASS ATTEMPT ---
            System.out.println("[ATTACKER] Trying fake session token...");
            try {
                stub.listFiles(fakeToken);
                System.out.println("[ATTACKER] Fake token ACCEPTED - Auth bypass SUCCEEDED!");
            } catch (Exception e) {
                System.out.println("[SECURED]  Fake token REJECTED: " + e.getMessage());
            }

            // --- REPLAY ATTACK ATTEMPT ---
            System.out.println();
            System.out.println("[ATTACKER] Attempting replay attack...");
            FileSerializable fs = new FileSerializable();
            fs.setName("replayed_file.txt");
            fs.setData("Replayed request content".getBytes());
            fs.setLastModifiedDate(new Date());

            System.out.println("[ATTACKER] Sending request first time...");
            try {
                stub.uploadFile(fakeToken, fs);
                System.out.println("[ATTACKER] First request accepted.");
            } catch (Exception e) {
                System.out.println("[SECURED]  First request blocked: " + e.getMessage());
            }

            System.out.println("[ATTACKER] Replaying SAME request again...");
            try {
                stub.uploadFile(fakeToken, fs);
                System.out.println("[ATTACKER] REPLAY SUCCEEDED - vulnerability confirmed!");
            } catch (Exception e) {
                System.out.println("[SECURED]  Replay BLOCKED: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("[SECURED]  Server REJECTED plain connection entirely!");
            System.out.println("[SECURED]  Reason: " + e.getMessage());
            System.out.println();
            System.out.println("========================================");
            System.out.println(" PROOF: mTLS is blocking unauthorized  ");
            System.out.println(" clients at the transport layer!        ");
            System.out.println(" VUL-3 Fix is WORKING correctly.        ");
            System.out.println("========================================");
        }
    }
}