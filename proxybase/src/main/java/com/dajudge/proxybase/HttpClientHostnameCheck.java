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

package com.dajudge.proxybase;

import org.apache.hc.client5.http.ssl.HttpsSupport;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static java.lang.String.format;

class HttpClientHostnameCheck implements HostnameCheck {
    public static final javax.net.ssl.HostnameVerifier VERIFIER = HttpsSupport.getDefaultHostnameVerifier();
    private final String hostname;

    HttpClientHostnameCheck(final String hostname) {
        this.hostname = hostname;
    }

    @Override
    public void verify(final X509Certificate cert) throws CertificateException {
        if (VERIFIER.verify(hostname, new DummySslSession(cert))) {
            return;
        }
        throw new CertificateException(format(
                "Certificate does not match hostname '%s': DN=%s, SANs=%s ",
                hostname,
                cert.getSubjectDN(),
                cert.getSubjectAlternativeNames()
        ));
    }

    private static class DummySslSession implements SSLSession {
        private final Certificate certificate;

        DummySslSession(final Certificate certificate) {
            this.certificate = certificate;
        }

        @Override
        public byte[] getId() {
            return new byte[0];
        }

        @Override
        public SSLSessionContext getSessionContext() {
            return null;
        }

        @Override
        public long getCreationTime() {
            return 0;
        }

        @Override
        public long getLastAccessedTime() {
            return 0;
        }

        @Override
        public void invalidate() {

        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public void putValue(final String name, final Object value) {

        }

        @Override
        public Object getValue(final String name) {
            return null;
        }

        @Override
        public void removeValue(final String name) {

        }

        @Override
        public String[] getValueNames() {
            return new String[0];
        }

        @Override
        public Certificate[] getPeerCertificates()  {
            return new Certificate[]{certificate};
        }

        @Override
        public Certificate[] getLocalCertificates() {
            return new Certificate[0];
        }

        @SuppressWarnings("deprecation") // Can't help it
        @Override
        public javax.security.cert.X509Certificate[] getPeerCertificateChain() {
            return new javax.security.cert.X509Certificate[0];
        }

        @Override
        public Principal getPeerPrincipal()  {
            return null;
        }

        @Override
        public Principal getLocalPrincipal() {
            return null;
        }

        @Override
        public String getCipherSuite() {
            return null;
        }

        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public String getPeerHost() {
            return null;
        }

        @Override
        public int getPeerPort() {
            return 0;
        }

        @Override
        public int getPacketBufferSize() {
            return 0;
        }

        @Override
        public int getApplicationBufferSize() {
            return 0;
        }
    }
}
