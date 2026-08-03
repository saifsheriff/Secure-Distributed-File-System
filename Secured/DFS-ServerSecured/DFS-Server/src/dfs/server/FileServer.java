package dfs.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class FileServer {

    public static void main(String[] args) throws Exception {
        System.out.println("WORKING DIR: " + System.getProperty("user.dir"));

        // FIX 2: Disable dynamic/remote class loading
        System.setProperty("java.rmi.server.useCodebaseOnly", "true");
        System.setProperty("java.rmi.server.codebase", "");

        // ── CHANGE NODE ID HERE ───────────────────────────────────────────────
        int nodeId = 3;           // CHANGE THIS: 1, 2, or 3
        boolean simulateDelay = true; // true for node 1 only

        // ── CHANGE PORTS HERE ─────────────────────────────────────────────────
        int[] allPorts = {1000, 2000, 3000};

        // ── Port for this node ────────────────────────────────────────────────
        int myPort = allPorts[nodeId - 1];

        // ── Storage folder ────────────────────────────────────────────────────
        String storagePath = System.getProperty("user.dir") + "/repo_node" + nodeId + "/";

        // ── Build peer config ─────────────────────────────────────────────────
        List<int[]> peerConfig = new ArrayList<>();
        for (int i = 0; i < allPorts.length; i++) {
            if (allPorts[i] != myPort) {
                peerConfig.add(new int[]{allPorts[i], i + 1});
            }
        }

        // ── Delay target ──────────────────────────────────────────────────────
        int delayTargetPort = simulateDelay ? allPorts[2] : -1;

        // ── Start the server ──────────────────────────────────────────────────
        try {
            // FIX 3: Use SSL socket factories for mTLS instead of plain TCP
            RMISSLClientSocketFactory csf = new RMISSLClientSocketFactory();
            RMISSLServerSocketFactory ssf = new RMISSLServerSocketFactory();

            FileServant servant = new FileServant(nodeId, myPort, storagePath,
                                                  peerConfig, delayTargetPort,
                                                  csf, ssf);

            // FIX 3: Registry itself also uses SSL
            Registry registry = LocateRegistry.createRegistry(myPort, csf, ssf);
            registry.rebind(FileServerI.serviceName + myPort, servant);

            System.out.println("===========================================");
            System.out.println(" Node " + nodeId + " running on port " + myPort);
            System.out.println(" Storage : " + storagePath);
            System.out.println(" Delay to Node 3: " + simulateDelay);
            System.out.println(" mTLS: ENABLED");
            System.out.println("===========================================");

        } catch (Exception e) {
            System.err.println("Failed to start Node " + nodeId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}