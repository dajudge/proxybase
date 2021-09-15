/*
 * Copyright 2019-2021 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.dajudge.proxybase.util;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
        return hasCertFor(() -> dn);
    }

    public static Consumer<Socket> hasCertFor(final Supplier<String> dnProvider) {
        return socket -> {
            try {
                final X509Certificate cert = (X509Certificate) ((SSLSocket) socket).getSession()
                        .getPeerCertificates()[0];
                assertEquals(dnProvider.get(), cert.getSubjectDN().getName());
            } catch (final SSLPeerUnverifiedException e) {
                throw new AssertionError(e);
            }
        };
    }

    private DownstreamSocketAssertions() {
    }
}
