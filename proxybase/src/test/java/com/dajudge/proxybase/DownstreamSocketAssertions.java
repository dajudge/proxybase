package com.dajudge.proxybase;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public final class DownstreamSocketAssertions {

    public static Consumer<Socket> isNotSsl() {
        return socket -> assertFalse(socket instanceof SSLSocket);
    }

    public static Consumer<Socket> isSsl() {
        return socket -> assertTrue(socket instanceof SSLSocket);
    }

    public static Consumer<Socket> hasNoCert() {
        return socket -> {
            try {
                ((SSLSocket) socket).getSession().getPeerCertificates();
            } catch (final SSLPeerUnverifiedException e) {
                // Happy case
            }
        };
    }

    public static Consumer<Socket> hasCertFor(final String dn) {
        return socket -> {
            try {
                final X509Certificate cert = (X509Certificate) ((SSLSocket) socket).getSession()
                        .getPeerCertificates()[0];
                assertEquals(dn, cert.getSubjectDN().getName());
            } catch (final SSLPeerUnverifiedException e) {
                throw new AssertionError(e);
            }
        };
    }

    private DownstreamSocketAssertions() {
    }
}
