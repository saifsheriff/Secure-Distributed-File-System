/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dfs.server;

import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class FileClient {

    private static final int[] SERVER_PORTS = {1000, 2000, 3000};
    private static FileServerI leaderStub;
    private static String sessionToken = null;
    private static String localPath;

    public static void main(String[] args) {

        localPath = System.getProperty("user.dir") + "/client_files/";
        new File(localPath).mkdirs();

        System.out.println("============================================");
        System.out.println("   Distributed File System - Client CLI");
        System.out.println("   Local folder: " + localPath);
        System.out.println("============================================");

        // Wait 2 seconds to make sure all servers are fully started
        System.out.println("  Connecting to servers...");
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        leaderStub = connectToRandomNode();
        if (leaderStub == null) {
            System.out.println("ERROR: Cannot connect to any server.");
            System.out.println("Make sure all 3 nodes are running first.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        printHelp();

        while (true) {
            System.out.print("\n> Enter command: ");
            String cmd = scanner.nextLine().trim().toLowerCase();

            switch (cmd) {
                case "register": handleRegister(scanner); break;
                case "login":    handleLogin(scanner);    break;
                case "upload":   handleUpload(scanner);   break;
                case "download": handleDownload(scanner); break;
                case "delete":   handleDelete(scanner);   break;
                case "rename":   handleRename(scanner);   break;
                case "search":   handleSearch(scanner);   break;
                case "list":     handleList();             break;
                case "help":     printHelp();              break;
                case "exit":
                case "quit":
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("  Unknown command. Type 'help' for commands.");
            }
        }
    }

    // =========================================================================
    // AUTH
    // =========================================================================

    private static void handleRegister(Scanner sc) {
        System.out.print("  Username: ");
        String username = sc.nextLine().trim();
        System.out.print("  Password: ");
        String password = sc.nextLine().trim();
        try {
            String result = leaderStub.register(username, password);
            System.out.println("  -> " + result);
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
    }

    private static void handleLogin(Scanner sc) {
        System.out.print("  Username: ");
        String username = sc.nextLine().trim();
        System.out.print("  Password: ");
        String password = sc.nextLine().trim();
        try {
            String result = leaderStub.login(username, password);
            if (result.startsWith("ERROR")) {
                System.out.println("  -> " + result);
            } else {
                sessionToken = result;
                System.out.println("  -> Login successful!");
                System.out.println("  -> Token: " + sessionToken.substring(0, 8) + "...");
            }
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
    }

    // =========================================================================
    // WRITE OPERATIONS
    // =========================================================================

    private static void handleUpload(Scanner sc) {
        if (!checkLoggedIn()) return;

        System.out.print("  Filename to upload (must be inside client_files folder): ");
        String fileName = sc.nextLine().trim();

        File localFile = new File(localPath + fileName);
        if (!localFile.exists()) {
            System.out.println("  ERROR: File not found at: " + localFile.getAbsolutePath());
            System.out.println("  Tip: Copy your file into the client_files folder first.");
            return;
        }

        try {
            byte[] buf = new byte[(int) localFile.length()];
            FileInputStream fis = new FileInputStream(localFile);
            fis.read(buf);
            fis.close();

            FileSerializable fs = new FileSerializable();
            fs.setName(fileName);
            fs.setData(buf);
            fs.setLastModifiedDate(new Date(localFile.lastModified()));

            FileServerI node = leaderStub;
            if (node == null) {
                System.out.println("  ERROR: No servers available.");
                return;
            }

            boolean ok = node.uploadFile(sessionToken, fs);
            if (ok) {
                System.out.println("  -> Upload sent. All 3 replicas will sync via TO-Multicast.");
            } else {
                System.out.println("  -> Upload failed.");
            }

        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
    }

    private static void handleDelete(Scanner sc) {
        if (!checkLoggedIn()) return;

        System.out.print("  Filename to delete: ");
        String fileName = sc.nextLine().trim();

        try {
            FileServerI node = leaderStub;
            if (node == null) {
                System.out.println("  ERROR: No servers available.");
                return;
            }
            node.deleteFile(sessionToken, fileName);
            System.out.println("  -> Delete sent. All 3 replicas will sync via TO-Multicast.");
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
    }

    private static void handleRename(Scanner sc) {
        if (!checkLoggedIn()) return;

        System.out.print("  Current filename: ");
        String oldName = sc.nextLine().trim();
        System.out.print("  New filename:     ");
        String newName = sc.nextLine().trim();

        try {
           FileServerI node = leaderStub;
            if (node == null) {
                System.out.println("  ERROR: No servers available.");
                return;
            }
            node.renameFile(sessionToken, oldName, newName);
            System.out.println("  -> Rename sent. All 3 replicas will sync via TO-Multicast.");
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
    }

    // =========================================================================
    // READ OPERATIONS
    // =========================================================================

    private static void handleDownload(Scanner sc) {
        if (!checkLoggedIn()) return;

        System.out.print("  Filename to download: ");
        String fileName = sc.nextLine().trim();

        try {
            FileSerializable fs = tryReadWithFailover(
                stub -> stub.downloadFile(sessionToken, fileName)
            );

            if (fs == null) {
                System.out.println("  -> File not found on server.");
                return;
            }

            File outFile = new File(localPath + fileName);
            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(fs.getData());
            fos.close();
            System.out.println("  -> Downloaded to: " + outFile.getAbsolutePath());

        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
    }

    private static void handleSearch(Scanner sc) {
        if (!checkLoggedIn()) return;

        System.out.print("  Filename to search for: ");
        String fileName = sc.nextLine().trim();

        try {
            Boolean found = tryReadWithFailover(
                stub -> stub.searchFiles(sessionToken, fileName)
            );
            System.out.println(Boolean.TRUE.equals(found)
                ? "  -> File FOUND on leader."
                : "  -> File NOT found.");
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
    }

    private static void handleList() {
        if (!checkLoggedIn()) return;

        try {
            List<String> files = tryReadWithFailover(
                stub -> stub.listFiles(sessionToken)
            );
            if (files == null || files.isEmpty()) {
                System.out.println("  -> No files on the server.");
            } else {
                System.out.println("  -> Files on leader (" + files.size() + " total):");
                for (String f : files) {
                    System.out.println("       * " + f);
                }
            }
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
    }

    // =========================================================================
    // CONNECTION HELPERS
    // =========================================================================

    private static FileServerI connectToRandomNode() {
        List<Integer> ports = new ArrayList<>();
        for (int p : SERVER_PORTS) ports.add(p);
        Collections.shuffle(ports);

        for (int port : ports) {
            FileServerI stub = tryConnect(port);
            if (stub != null) {
                System.out.println("  Connected to node on port " + port + " (leader for reads)");
                return stub;
            }
        }
        return null;
    }

    private static FileServerI connectToAnyNode() {
        int startIdx = new Random().nextInt(SERVER_PORTS.length);
        for (int i = 0; i < SERVER_PORTS.length; i++) {
            int port = SERVER_PORTS[(startIdx + i) % SERVER_PORTS.length];
            FileServerI stub = tryConnect(port);
            if (stub != null) return stub;
        }
        return null;
    }

    private static FileServerI tryConnect(int port) {
        try {
            Registry reg = LocateRegistry.getRegistry("localhost", port);
            FileServerI stub = (FileServerI) reg.lookup(FileServerI.serviceName + port);
            return stub;
        } catch (Exception e) {
            System.out.println("  Cannot connect to port " + port + ": " + e.getMessage());
            return null;
        }
    }

    private static <T> T tryReadWithFailover(ReadOperation<T> op) throws Exception {
        try {
            return op.run(leaderStub);
        } catch (Exception e) {
            System.out.println("  Leader is down. Selecting a new leader...");
            leaderStub = connectToRandomNode();
            if (leaderStub == null) {
                throw new Exception("All servers are unreachable.");
            }
            return op.run(leaderStub);
        }
    }

    // =========================================================================
    // UTILITIES
    // =========================================================================

    private static boolean checkLoggedIn() {
        if (sessionToken == null) {
            System.out.println("  -> Not logged in. Use the 'login' command first.");
            return false;
        }
        return true;
    }

    private static void printHelp() {
        System.out.println("\n  Commands:");
        System.out.println("  -----------------------------------------");
        System.out.println("  register  - create a new account");
        System.out.println("  login     - login to get session token");
        System.out.println("  upload    - upload a file to all replicas");
        System.out.println("  download  - download a file from leader");
        System.out.println("  delete    - delete a file on all replicas");
        System.out.println("  rename    - rename a file on all replicas");
        System.out.println("  search    - check if a file exists");
        System.out.println("  list      - list all files on leader");
        System.out.println("  help      - show this menu");
        System.out.println("  exit      - quit");
        System.out.println("  -----------------------------------------");
    }

    @FunctionalInterface
    interface ReadOperation<T> {
        T run(FileServerI stub) throws Exception;
    }
}