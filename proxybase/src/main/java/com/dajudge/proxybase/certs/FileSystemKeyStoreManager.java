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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class FileSystemKeyStoreManager implements KeyStoreManager {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemKeyStoreManager.class);
    private final KeyStoreLoader loader;
    private final Supplier<Long> clock;
    private final long updateIntervalMsecs;
    private final Object keyStoreLock = new Object();
    private final Object clockLock = new Object();
    private long lastUpdate;
    private KeyStoreWrapper keyStore;
    private AtomicBoolean loading = new AtomicBoolean();

    public FileSystemKeyStoreManager(
            final KeyStoreLoader loader,
            final Supplier<Long> clock,
            final long updateIntervalMsecs
    ) {
        this.loader = loader;
        this.clock = clock;
        this.updateIntervalMsecs = updateIntervalMsecs;
    }

    public KeyStoreWrapper getKeyStore() {
        updateIfNecessary();
        synchronized (keyStoreLock) {
            return keyStore;
        }
    }

    private void updateIfNecessary() {
        if (updateIsNecessary()) {
            if (loading.getAndSet(true)) {
                // Update already in progress
                return;
            }
            try {
                final KeyStoreWrapper newKeyStore = loader.load();
                synchronized (keyStoreLock) {
                    keyStore = newKeyStore;
                }
                synchronized (clockLock) {
                    lastUpdate = clock.get();
                }
            } catch (final Exception e) {
                LOG.warn("Failed to reload keystore", e);
            }
            loading.set(false);
        }
    }

    private boolean updateIsNecessary() {
        synchronized (clockLock) {
            return clock.get() - lastUpdate > updateIntervalMsecs;
        }
    }

    public interface KeyStoreLoader {
        KeyStoreWrapper load() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException;
    }

    public static FileSystemKeyStoreManager upstreamKeyStoreManager(
            final UpstreamSslConfig config,
            final Supplier<Long> clock
    ) {
        return new FileSystemKeyStoreManager(
                new FileSystemKeyStoreLoader(
                        config.getKeyStore(),
                        config.getKeyStorePassword(),
                        config.getKeyStorePasswordFile(),
                        config.getKeyPassword(),
                        config.getKeyPasswordFile(),
                        config.getKeyStoreType()
                ),
                clock,
                config.getKeyStoreUpdateIntervalMsecs()
        );
    }

    public static FileSystemKeyStoreManager upstreamTrustStoreManager(
            final UpstreamSslConfig config,
            final Supplier<Long> clock
    ) {
        return new FileSystemKeyStoreManager(
                new FileSystemKeyStoreLoader(
                        config.getTrustStore(),
                        config.getTrustStorePassword(),
                        config.getTrustStorePasswordFile(),
                        null,
                        null,
                        config.getTrustStoreType()
                ),
                clock,
                config.getTrustStoreUpdateIntervalMsecs()
        );
    }

    public static FileSystemKeyStoreManager downstreamTrustStoreManager(
            final DownstreamSslConfig config,
            final Supplier<Long> clock
    ) {
        final KeyStoreConfig keyStore = config.getTrustStore();
        return new FileSystemKeyStoreManager(
                new FileSystemKeyStoreLoader(
                        keyStore.getKeyStorePath(),
                        keyStore.getKeyStorePassword(),
                        keyStore.getKeyStorePasswordPath(),
                        keyStore.getKeyPassword(),
                        keyStore.getKeyPasswordPath(),
                        keyStore.getKeyStoreType()
                ),
                clock,
                keyStore.getUpdateIntervalMsecs()
        );
    }
}
