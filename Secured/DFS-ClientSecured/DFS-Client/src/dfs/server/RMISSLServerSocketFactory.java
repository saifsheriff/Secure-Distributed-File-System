package dfs.server;

import java.io.*;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;
import javax.net.ssl.SSLServerSocket;

public class RMISSLServerSocketFactory implements RMIServerSocketFactory {

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        try {
            SSLServerSocket ss = (SSLServerSocket)
                SSLConfig.getServerSocketFactory().createServerSocket(port);
            ss.setNeedClientAuth(true);
            return ss;
        } catch (Exception e) {
            throw new IOException("mTLS server socket failed: " + e.getMessage(), e);
        }
    }
}