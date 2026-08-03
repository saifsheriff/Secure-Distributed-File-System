package dfs.server;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileServant extends UnicastRemoteObject implements FileServerI {

    private static final long serialVersionUID = 1L;

    private final int nodeId;
    private final int port;
    private final String storagePath;
    private final List<int[]> peerConfig;
    private final List<FileServerI> peers = new ArrayList<>();
    private boolean peersConnected = false;

    private long logicalClock = 0;
    private final PriorityQueue<WriteRequest> queue = new PriorityQueue<>();
    private final Map<String, Integer> ackCounts = new HashMap<>();
    private final Object clockLock = new Object();

    private final Map<String, String> userStore = new ConcurrentHashMap<>();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final int delayTargetPort;

    // =========================================================================
    // Constructor
    // =========================================================================
    public FileServant(int nodeId, int port, String storagePath,
                       List<int[]> peerConfig, int delayTargetPort) throws RemoteException {
        super();
        this.nodeId = nodeId;
        this.port = port;
        this.storagePath = storagePath;
        this.peerConfig = peerConfig;
        this.delayTargetPort = delayTargetPort;

        new File(storagePath).mkdirs();
        startQueueProcessor();
        System.out.println("[Node " + nodeId + "] Started. Storage: " + storagePath);

        // Connect to peers at startup — retry every 2 seconds in background
        new Thread(() -> {
            for (int i = 0; i < 15; i++) {
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
                connectPeersIfNeeded();
                if (peersConnected) break;
            }
        }).start();
    }

    // =========================================================================
    // SECTION 1: Authentication
    // =========================================================================

    @Override
    public String register(String username, String password) throws RemoteException {
        if (userStore.containsKey(username))
            return "ERROR: Username '" + username + "' already exists.";
        userStore.put(username, password);
        System.out.println("[Node " + nodeId + "] Registered user: " + username);
        return "OK";
    }

    @Override
    public String login(String username, String password) throws RemoteException {
        String stored = userStore.get(username);
        if (stored != null && stored.equals(password)) {
            String token = UUID.randomUUID().toString();
            sessions.put(token, username);
            System.out.println("[Node " + nodeId + "] User logged in: " + username);
            return token;
        }
        return "ERROR: Invalid username or password.";
    }

    private void requireAuth(String token) throws RemoteException {
        if (token == null || !sessions.containsKey(token))
            throw new RemoteException("UNAUTHORIZED: Please login first.");
    }

    // =========================================================================
    // SECTION 2: Write Operations
    // =========================================================================

    @Override
    public boolean uploadFile(String sessionToken, FileSerializable f) throws RemoteException {
        requireAuth(sessionToken);
        System.out.println("[Node " + nodeId + "] Client requested UPLOAD: " + f.getName());
        return initiateWrite(new WriteRequest(UUID.randomUUID().toString(),
            WriteRequest.OperationType.UPLOAD, f.getName(), null, f, 0, nodeId));
    }

    @Override
    public boolean deleteFile(String sessionToken, String fileName) throws RemoteException {
        requireAuth(sessionToken);
        System.out.println("[Node " + nodeId + "] Client requested DELETE: " + fileName);
        return initiateWrite(new WriteRequest(UUID.randomUUID().toString(),
            WriteRequest.OperationType.DELETE, fileName, null, null, 0, nodeId));
    }

    @Override
    public boolean renameFile(String sessionToken, String oldName, String newName) throws RemoteException {
        requireAuth(sessionToken);
        System.out.println("[Node " + nodeId + "] Client requested RENAME: " + oldName + " -> " + newName);
        return initiateWrite(new WriteRequest(UUID.randomUUID().toString(),
            WriteRequest.OperationType.RENAME, oldName, newName, null, 0, nodeId));
    }

    private boolean initiateWrite(WriteRequest req) {
        connectPeersIfNeeded();
        WriteRequest stamped;
        synchronized (clockLock) {
            logicalClock++;
            stamped = new WriteRequest(req.getTxId(), req.getOpType(),
                req.getFileName(), req.getNewFileName(), req.getFile(),
                logicalClock, nodeId);
        }
        multicastWrite(stamped);
        return true;
    }

    private void multicastWrite(WriteRequest req) {
        try { receiveWriteRequest(req); } catch (RemoteException e) { e.printStackTrace(); }

        for (int i = 0; i < peers.size(); i++) {
            final FileServerI peer = peers.get(i);
            final int targetPort = peerConfig.get(i)[0];
            new Thread(() -> {
                try {
                    if (port == 1000 && targetPort == delayTargetPort) {
                        System.out.println("[Node " + nodeId + "] >>> Simulating 5s delay to port " + targetPort + " <<<");
                        Thread.sleep(5000);
                    }
                    peer.receiveWriteRequest(req);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RemoteException e) {
                    System.err.println("[Node " + nodeId + "] Could not reach peer on port " + targetPort);
                }
            }).start();
        }
    }

    // =========================================================================
    // SECTION 3: TO-Multicast
    // =========================================================================

        @Override
        public void receiveWriteRequest(WriteRequest req) throws RemoteException {
            synchronized (clockLock) {
                logicalClock = Math.max(logicalClock, req.getLogicalClock()) + 1;
                queue.add(req);
                ackCounts.put(req.getTxId(), 0);
            }

            System.out.println("[Node " + nodeId + "] Queued " + req.getOpType()
                + " txId=" + req.getTxId().substring(0, 8)
                + " clock=" + req.getLogicalClock()
                + " queue size=" + queue.size());

            connectPeersIfNeeded();

            // Send ACK for THIS request to all nodes
            sendAckToAll(req.getTxId());

            // Also re-send ACKs for ALL queued requests
            // This handles the case where this node received requests before peers connected
            List<String> allTxIds = new ArrayList<>();
            synchronized (clockLock) {
                for (WriteRequest r : queue) {
                    allTxIds.add(r.getTxId());
                }
            }
            for (String txId : allTxIds) {
                if (!txId.equals(req.getTxId())) { // avoid double ACK for current
                    sendAckToAll(txId);
                }
            }
        }

    @Override
    public void receiveAck(String txId, int fromNodeId) throws RemoteException {
        synchronized (clockLock) {
            ackCounts.merge(txId, 1, Integer::sum);
        }
    }
    
            private void sendAckToAll(String txId) {
            // ACK to self
            try { 
                receiveAck(txId, nodeId); 
            } catch (RemoteException e) { 
                e.printStackTrace(); 
            }

            // ACK to each peer
            List<FileServerI> currentPeers;
            synchronized (this) { 
                currentPeers = new ArrayList<>(peers); 
            }

            for (FileServerI peer : currentPeers) {
                new Thread(() -> {
                    try { 
                        peer.receiveAck(txId, nodeId); 
                    } catch (RemoteException e) { 
                        // peer might be down
                    }
                }).start();
            }
        }

    // =========================================================================
    // SECTION 4: Queue Processor
    // =========================================================================

    private void startQueueProcessor() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { processQueue(); }
        }, 500, 100);
    }

    private void processQueue() {
        int totalNodes = 1 + peerConfig.size();
        synchronized (clockLock) {
            while (!queue.isEmpty()) {
                WriteRequest head = queue.peek();
                int acks = ackCounts.getOrDefault(head.getTxId(), 0);
                if (acks >= totalNodes) {
                    queue.poll();
                    ackCounts.remove(head.getTxId());
                    executeLocally(head);
                } else {
                    break;
                }
            }
        }
    }

    private void executeLocally(WriteRequest req) {
        System.out.println("[Node " + nodeId + "] EXECUTING " + req.getOpType()
            + " | file=" + req.getFileName()
            + " | clock=" + req.getLogicalClock()
            + " | tx=" + req.getTxId().substring(0, 8));
        switch (req.getOpType()) {
            case UPLOAD: writeFileToStorage(req.getFile()); break;
            case DELETE: deleteFileFromStorage(req.getFileName()); break;
            case RENAME: renameFileInStorage(req.getFileName(), req.getNewFileName()); break;
        }
    }

    // =========================================================================
    // SECTION 5: File System Helpers
    // =========================================================================

    private void writeFileToStorage(FileSerializable fs) {
        try {
            File f = new File(storagePath + fs.getName());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
            out.write(fs.getData()); out.flush(); out.close();
            System.out.println("[Node " + nodeId + "] Saved: " + fs.getName());
        } catch (IOException e) {
            System.err.println("[Node " + nodeId + "] Failed to write: " + e.getMessage());
        }
    }

    private void deleteFileFromStorage(String fileName) {
        File f = new File(storagePath + fileName);
        if (f.delete()) System.out.println("[Node " + nodeId + "] Deleted: " + fileName);
        else System.out.println("[Node " + nodeId + "] Delete failed (not found?): " + fileName);
    }

    private void renameFileInStorage(String oldName, String newName) {
        File oldFile = new File(storagePath + oldName);
        File newFile = new File(storagePath + newName);
        if (oldFile.renameTo(newFile))
            System.out.println("[Node " + nodeId + "] Renamed: " + oldName + " -> " + newName);
        else
            System.out.println("[Node " + nodeId + "] Rename failed: " + oldName);
    }

    // =========================================================================
    // SECTION 6: Read Operations
    // =========================================================================

    @Override
    public FileSerializable downloadFile(String sessionToken, String fileName) throws RemoteException {
        requireAuth(sessionToken);
        System.out.println("[Node " + nodeId + "] DOWNLOAD: " + fileName);
        try {
            File f = new File(storagePath + fileName);
            if (!f.exists()) { System.out.println("[Node " + nodeId + "] File not found: " + fileName); return null; }
            byte[] buf = new byte[(int) f.length()];
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
            in.read(buf); in.close();
            FileSerializable fs = new FileSerializable();
            fs.setData(buf); fs.setName(fileName);
            fs.setLastModifiedDate(new Date(f.lastModified()));
            return fs;
        } catch (IOException e) { e.printStackTrace(); return null; }
    }

    @Override
    public boolean searchFiles(String sessionToken, String fileName) throws RemoteException {
        requireAuth(sessionToken);
        boolean found = new File(storagePath + fileName).exists();
        System.out.println("[Node " + nodeId + "] SEARCH: " + fileName + " -> " + (found ? "FOUND" : "NOT FOUND"));
        return found;
    }

    @Override
    public List<String> listFiles(String sessionToken) throws RemoteException {
        requireAuth(sessionToken);
        List<String> names = new ArrayList<>();
        File dir = new File(storagePath);
        String[] files = dir.list();
        if (files != null) Collections.addAll(names, files);
        System.out.println("[Node " + nodeId + "] LIST: " + names);
        return names;
    }

    // =========================================================================
    // SECTION 7: Peer Connection
    // =========================================================================

    private synchronized void connectPeersIfNeeded() {
        if (peersConnected) return;

        peers.clear();
        for (int[] cfg : peerConfig) {
            try {
                Registry reg = LocateRegistry.getRegistry("localhost", cfg[0]);
                FileServerI stub = (FileServerI) reg.lookup(FileServerI.serviceName + cfg[0]);
                peers.add(stub);
                System.out.println("[Node " + nodeId + "] Connected to peer on port " + cfg[0]);
            } catch (Exception e) {
                System.err.println("[Node " + nodeId + "] Peer not available on port " + cfg[0]);
            }
        }

        peersConnected = (peers.size() == peerConfig.size());
        if (peersConnected) {
            System.out.println("[Node " + nodeId + "] All peers connected.");

            // ── KEY FIX: Re-send ACKs OUTSIDE clockLock to avoid deadlock ────
            // Copy pending txIds first
            List<String> pendingTxIds = new ArrayList<>();
            synchronized (clockLock) {
                for (WriteRequest req : queue) {
                    pendingTxIds.add(req.getTxId());
                }
            }

            // Now send ACKs outside both locks
            for (String txId : pendingTxIds) {
                sendAckToAll(txId);
            }

            if (!pendingTxIds.isEmpty()) {
                System.out.println("[Node " + nodeId + "] Re-sent ACKs for "
                    + pendingTxIds.size() + " queued requests.");
            }
        }
    }
}