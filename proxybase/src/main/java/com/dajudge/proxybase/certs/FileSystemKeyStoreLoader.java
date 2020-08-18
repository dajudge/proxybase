/*
 * Copyright 2019-2020 Alex Stockinger
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

package com.dajudge.proxybase.certs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class FileSystemKeyStoreLoader implements ReloadingKeyStoreManager.KeyStoreLoader {
    private final Filesystem filesystem;
    private final KeyStoreConfig config;

    public FileSystemKeyStoreLoader(
            final Filesystem filesystem,
            final KeyStoreConfig config
    ) {
        this.filesystem = filesystem;
        this.config = config;
    }

    @Override
    public KeyStoreWrapper load()
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final char[] keyStorePassword = getEffectiveKeyStorePassword();
        final char[] keyPassword = getEffectiveKeyPassword();
        final KeyStore keyStore = KeyStore.getInstance(config.getKeyStoreType());
        try (final InputStream is = new ByteArrayInputStream(filesystem.readFile(config.getKeyStorePath()))) {
            keyStore.load(is, keyStorePassword);
        }
        return new KeyStoreWrapper(keyStore, keyPassword);
    }

    private char[] getEffectiveKeyPassword() throws IOException {
        return getEffectivePassword(config.getKeyPasswordPath(), config.getKeyPassword());
    }

    private char[] getEffectiveKeyStorePassword() throws IOException {
        return getEffectivePassword(config.getKeyStorePasswordPath(), config.getKeyStorePassword());
    }

    private char[] getEffectivePassword(final String path, final char[] password) throws IOException {
        if (path != null) {
            new String(filesystem.readFile(path), StandardCharsets.UTF_8);
        }
        return password;
    }

}
