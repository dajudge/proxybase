package com.dajudge.proxybase.certs;

import com.dajudge.proxybase.Clock;
import com.dajudge.proxybase.ca.KeyStoreWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeyStoreManager {
    private static final Logger LOG = LoggerFactory.getLogger(KeyStoreManager.class);
    private final KeyStoreLoader loader;
    private final Clock clock;
    private final long updateIntervalMsecs;
    private final Object keyStoreLock = new Object();
    private final Object clockLock = new Object();
    private long lastUpdate;
    private KeyStoreWrapper keyStore;
    private AtomicBoolean loading = new AtomicBoolean();

    public KeyStoreManager(
            final KeyStoreLoader loader,
            final Clock clock,
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
                    lastUpdate = clock.now();
                }
            } catch (final Exception e) {
                LOG.warn("Failed to reload keystore", e);
            }
            loading.set(false);
        }
    }

    private boolean updateIsNecessary() {
        synchronized (clockLock) {
            return clock.now() - lastUpdate > updateIntervalMsecs;
        }
    }

    public interface KeyStoreLoader {
        KeyStoreWrapper load() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException;
    }

    public static KeyStoreManager upstreamKeyStoreManager(
            final UpstreamSslConfig config,
            final Clock clock
    ) {
        return new KeyStoreManager(
                new InputStreamKeyStoreLoader(
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

    public static KeyStoreManager upstreamTrustStoreManager(
            final UpstreamSslConfig config,
            final Clock clock
    ) {
        return new KeyStoreManager(
                new InputStreamKeyStoreLoader(
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

    public static KeyStoreManager downstreamTrustStoreManager(
            final DownstreamSslConfig config,
            final Clock clock
    ) {
        final KeyStoreConfig keyStore = config.getTrustStore();
        return new KeyStoreManager(
                new InputStreamKeyStoreLoader(
                        keyStore.getKeyStorePath(),
                        keyStore.getKeyStorePassword(),
                        keyStore.getKeyStorePasswordPath(),
                        keyStore.getKeyPassword(),
                        keyStore.getKeyPasswordPath(),
                        keyStore.getKeyStoreType()
                ),
                clock,
                keyStore.getUpdateIntercalMsecs()
        );
    }
}
