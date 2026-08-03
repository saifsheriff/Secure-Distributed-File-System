package dfs.server;

import java.io.*;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;
import javax.net.ssl.SSLServerSocket;

/**
 * FIX 3: mTLS server socket factory plugged into RMI.
 * setNeedClientAuth(true) enforces mutual TLS — client must present a certificate.
 */
public class RMISSLServerSocketFactory implements RMIServerSocketFactory {

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        try {
            SSLServerSocket ss = (SSLServerSocket)
                SSLConfig.getServerSocketFactory().createServerSocket(port);
            ss.setNeedClientAuth(true); // FIX 3: enforce mutual authentication
            return ss;
        } catch (Exception e) {
            throw new IOException("mTLS server socket failed: " + e.getMessage(), e);
        }
    }
}
