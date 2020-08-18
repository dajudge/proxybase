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

public class ReloadingKeyStoreManager implements KeyStoreManager {
    private static final Logger LOG = LoggerFactory.getLogger(ReloadingKeyStoreManager.class);
    private final KeyStoreLoader loader;
    private final Supplier<Long> clock;
    private final long updateIntervalMsecs;
    private final Object keyStoreLock = new Object();
    private final Object clockLock = new Object();
    private long lastUpdate;
    private final AtomicBoolean loading = new AtomicBoolean();
    private KeyStoreWrapper keyStore;

    public ReloadingKeyStoreManager(
            final KeyStoreLoader loader,
            final Supplier<Long> clock,
            final long updateIntervalMsecs
    ) {
        this.loader = loader;
        this.clock = clock;
        this.updateIntervalMsecs = updateIntervalMsecs;
    }

    public static ReloadingKeyStoreManager createReloader(
            final KeyStoreConfig keystore,
            final Supplier<Long> clock,
            final Filesystem filesystem
    ) {
        return new ReloadingKeyStoreManager(
                new FileSystemKeyStoreLoader(filesystem, keystore),
                clock,
                keystore.getUpdateIntervalMsecs()
        );
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
            } finally {
                loading.set(false);
            }
        }
    }

    private boolean updateIsNecessary() {
        synchronized (clockLock) {
            final long now = clock.get();
            final long timePassedSinceLastUpdate = now - lastUpdate;
            return timePassedSinceLastUpdate > updateIntervalMsecs;
        }
    }

    public interface KeyStoreLoader {
        KeyStoreWrapper load() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException;
    }
}
