package dfs.server;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * FIX 3: Loads KeyStores and TrustStores to build SSL socket factories.
 * Server uses server. + server-trust.
 * Client uses client. + client-trust.
 */
public class SSLConfig {

    public static SSLServerSocketFactory getServerSocketFactory() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(new FileInputStream("server.jks"), "serverpass".toCharArray());

    KeyStore trustStore = KeyStore.getInstance("JKS");
    trustStore.load(new FileInputStream("server-trust.jks"), "servertrustpass".toCharArray());

    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, "serverpass".toCharArray());

    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    tmf.init(trustStore);

    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    return ctx.getServerSocketFactory();
}

    public static SSLSocketFactory getClientSocketFactory() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(new FileInputStream("client.jks"), "clientpass".toCharArray());

    KeyStore trustStore = KeyStore.getInstance("JKS");
    trustStore.load(new FileInputStream("client-trust.jks"), "clienttrustpass".toCharArray());

    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, "clientpass".toCharArray());
        
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    tmf.init(trustStore);

    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    return ctx.getSocketFactory();
}}