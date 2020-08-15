package com.dajudge.proxybase;

import org.junit.Test;

public class SslReloadTest extends BaseProxyTest {
    @Test
    public void works_without_proxy() {
        assertRoundtripWorksWithoutProxy();
    }

    @Test
    public void works_with_proxy() {
        assertRoundtripWorksWithProxy(
                upstream -> {
                },
                downstream -> {
                }
        );
    }
}
