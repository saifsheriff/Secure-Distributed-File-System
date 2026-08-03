/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dfs.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class FileServer {

    public static void main(String[] args) throws Exception {

        // ── CHANGE NODE ID HERE ───────────────────────────────────────────────
        int nodeId = 3;          // CHANGE THIS: 1, 2, or 3
        boolean simulateDelay = false; // CHANGE THIS: true for node 1 only, false for 2 and 3

        // ── CHANGE PORTS HERE ─────────────────────────────────────────────────
        int[] allPorts = {1000, 2000, 3000}; // CHANGE THIS: must match FileServant and FileClient

        // ── Port for this node ────────────────────────────────────────────────
        int myPort = allPorts[nodeId - 1];

        // ── Storage folder ────────────────────────────────────────────────────
        String storagePath = System.getProperty("user.dir") + "/repo_node" + nodeId + "/";

        // ── Build peer config (the other two nodes) ───────────────────────────
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
            FileServant servant = new FileServant(nodeId, myPort, storagePath, peerConfig, delayTargetPort);
            
            // VULNERABILITY 2: Dynamic Class Loading — useCodebaseOnly not set
           // VULNERABILITY 3: Plaintext Transport — no SSL socket factory
            Registry registry = LocateRegistry.createRegistry(myPort);
            registry.rebind(FileServerI.serviceName + myPort, servant);

            System.out.println("===========================================");
            System.out.println(" Node " + nodeId + " running on port " + myPort);
            System.out.println(" Storage : " + storagePath);
            System.out.println(" Delay to Node 3: " + simulateDelay);
            System.out.println("===========================================");

        } catch (Exception e) {
            System.err.println("Failed to start Node " + nodeId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}