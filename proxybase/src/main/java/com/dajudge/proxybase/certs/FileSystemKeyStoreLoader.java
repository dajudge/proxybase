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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static java.nio.file.Files.readAllBytes;

public class FileSystemKeyStoreLoader implements FileSystemKeyStoreManager.KeyStoreLoader {
    private final String keyStorePath;
    private final char[] keyStorePassword;
    private final String keyStorePasswordPath;
    private final char[] keyPassword;
    private final String keyPasswordPath;
    private final String keyStoreType;

    public FileSystemKeyStoreLoader(
            final String keyStorePath,
            final char[] keyStorePassword,
            final String keyStorePasswordPath,
            final char[] keyPassword,
            final String keyPasswordPath,
            final String keyStoreType
    ) {
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.keyStorePasswordPath = keyStorePasswordPath;
        this.keyPassword = keyPassword;
        this.keyPasswordPath = keyPasswordPath;
        this.keyStoreType = keyStoreType;
    }

    @Override
    public KeyStoreWrapper load()
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final char[] keyStorePassword = getEffectiveKeyStorePassword();
        final char[] keyPassword = getEffectiveKeyPassword();
        final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        try (final InputStream is = new FileInputStream(keyStorePath)) {
            keyStore.load(is, keyStorePassword);
        }
        return new KeyStoreWrapper(keyStore, keyPassword);
    }

    private char[] getEffectiveKeyPassword() throws IOException {
        return getEffectivePassword(keyPasswordPath, keyPassword);
    }

    private char[] getEffectiveKeyStorePassword() throws IOException {
        return getEffectivePassword(keyStorePasswordPath, keyStorePassword);
    }

    private char[] getEffectivePassword(final String path, final char[] password) throws IOException {
        if (path != null) {
            new String(readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
        }
        return password;
    }
}
