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

package com.dajudge.proxybase.certs;

import com.dajudge.proxybase.certs.ReloadingKeyStoreManager.KeyStoreLoader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

public class ReloadingKeyStoreManagerTest {
    private static final long START_TIME = 42;
    private static final int UPDATE_INTERVAL_MSECS = 10;
    @SuppressWarnings("unchecked")
    private Supplier<Long> clock = (Supplier<Long>) mock(Supplier.class);
    private KeyStoreLoader loader = mock(KeyStoreLoader.class);
    private final ReloadingKeyStoreManager subject = new ReloadingKeyStoreManager(
            loader,
            clock,
            UPDATE_INTERVAL_MSECS
    );

    @Before
    public void setup() {
        when(clock.get()).thenReturn(START_TIME);
    }

    @Test
    public void returns_initial_keystore()
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        final KeyStoreWrapper keyStore1 = new KeyStoreWrapper(null, null);
        when(loader.load()).thenReturn(keyStore1);
        assertSame(keyStore1, subject.getKeyStore());
    }

    @Test
    public void does_not_reload_when_not_expired()
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        final KeyStoreWrapper keyStore1 = new KeyStoreWrapper(null, null);
        when(loader.load()).thenReturn(keyStore1);
        subject.getKeyStore();
        when(clock.get()).thenReturn((long) (UPDATE_INTERVAL_MSECS / 2));
        subject.getKeyStore();
        verify(loader, times(1)).load();
    }

    @Test
    public void reloads_when_time_expired()
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        final KeyStoreWrapper keyStore1 = new KeyStoreWrapper(null, null);
        final KeyStoreWrapper keyStore2 = new KeyStoreWrapper(null, null);

        when(loader.load()).thenReturn(keyStore1);
        assertSame(keyStore1, subject.getKeyStore());

        when(loader.load()).thenReturn(keyStore2);
        when(clock.get()).thenReturn(START_TIME + UPDATE_INTERVAL_MSECS + 1);
        assertSame(keyStore2, subject.getKeyStore());
        assertSame(keyStore2, subject.getKeyStore());

        verify(loader, times(2)).load();
    }

    @Test
    public void returns_old_keystore_while_loading()
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
            IOException, InterruptedException {
        final CountDownLatch continueLoadLatch = new CountDownLatch(1);
        final CountDownLatch beganLoadLatch = new CountDownLatch(1);

        final KeyStoreWrapper keyStore1 = new KeyStoreWrapper(null, null);
        final KeyStoreWrapper keyStore2 = new KeyStoreWrapper(null, null);

        // Load old keystore
        when(loader.load()).thenReturn(keyStore1);
        assertSame(keyStore1, subject.getKeyStore());

        // Advance to a time where a reload is required
        when(clock.get()).thenReturn(START_TIME + UPDATE_INTERVAL_MSECS + 1);

        // Start blocking reload
        when(loader.load()).thenAnswer((Answer<KeyStoreWrapper>) invocationOnMock -> {
            beganLoadLatch.countDown();
            synchronized (continueLoadLatch) {
                continueLoadLatch.await();
            }
            return keyStore2;
        });
        final Thread updateThread = new Thread(() -> {
            // Blocking thread must already see new keystore when released
            assertSame(keyStore2, subject.getKeyStore());
        });
        updateThread.start();

        // Wait until blocking reload is in progress
        synchronized (beganLoadLatch) {
            beganLoadLatch.await();
        }

        // Make sure the old keystore is returned while blocked
        assertSame(keyStore1, subject.getKeyStore());

        // Continue blocking load
        continueLoadLatch.countDown();

        // Await completion of blocking load
        updateThread.join();

        // New keystore must be returned now
        assertSame(keyStore2, subject.getKeyStore());
    }
}