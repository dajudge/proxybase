package com.dajudge.proxybase.certs;

import com.dajudge.proxybase.ca.KeyStoreWrapper;
import com.dajudge.proxybase.certs.KeyStoreManager.KeyStoreLoader;

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

public class InputStreamKeyStoreLoader implements KeyStoreLoader {
    private final String keyStorePath;
    private final char[] keyStorePassword;
    private final String keyStorePasswordPath;
    private final char[] keyPassword;
    private final String keyPasswordPath;
    private final String keyStoreType;

    public InputStreamKeyStoreLoader(
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
