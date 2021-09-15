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

import com.dajudge.proxybase.ca.test.TestCertificationAuthority;
import com.dajudge.proxybase.certs.KeyStoreManager;
import com.dajudge.proxybase.certs.KeyStoreWrapper;

public class TestKeyStoreManager implements KeyStoreManager {
    private final TestCertificationAuthority ca;
    private final String keyStoreType;
    private String dn;

    public TestKeyStoreManager(
            final TestCertificationAuthority ca,
            final String dn,
            final String keyStoreType
    ) {
        this.ca = ca;
        this.dn = dn;
        this.keyStoreType = keyStoreType;
    }

    @Override
    public KeyStoreWrapper getKeyStore() {
        return ca.createNewKeyStore(dn, keyStoreType);
    }

    public void replaceDn(final String dn) {
        this.dn = dn;
    }
}
