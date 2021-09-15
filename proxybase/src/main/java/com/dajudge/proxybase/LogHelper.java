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

import org.slf4j.MDC;

final class LogHelper {
    private static final String MDC_CHANNEL_ID = "CHANNEL_ID";
    private static final String MDC_CHANNEL_TYPE = "CHANNEL_TYPE";

    private LogHelper() {
    }

    static void withChannelId(final String channelId, final String channelType, final Runnable runnable) {
        MDC.put(MDC_CHANNEL_ID, channelId);
        MDC.put(MDC_CHANNEL_TYPE, channelType);
        try {
            runnable.run();
        } finally {
            MDC.remove(MDC_CHANNEL_ID);
            MDC.remove(MDC_CHANNEL_TYPE);
        }
    }
}
