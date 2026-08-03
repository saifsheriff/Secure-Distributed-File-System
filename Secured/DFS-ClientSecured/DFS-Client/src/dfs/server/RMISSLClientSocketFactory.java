package dfs.server;

import java.io.*;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

public class RMISSLClientSocketFactory implements RMIClientSocketFactory, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        try {
            return SSLConfig.getClientSocketFactory().createSocket(host, port);
        } catch (Exception e) {
            throw new IOException("mTLS client socket failed: " + e.getMessage(), e);
        }
    }
}