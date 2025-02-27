/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package org.nd4j.parameterserver.distributed.v2.messages.history;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nd4j.common.tests.BaseND4JTest;
import org.nd4j.common.tests.tags.NativeTag;
import org.nd4j.common.tests.tags.TagNames;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@Tag(TagNames.FILE_IO)
@Tag(TagNames.DIST_SYSTEMS)
@NativeTag
public class HashHistoryHolderTest extends BaseND4JTest {

    @Test
    public void testBasicStuff_1() {
        val history = new HashHistoryHolder<String>(1024);

        val first = java.util.UUID.randomUUID().toString();

        // we assume that message is unknown
        assertFalse(history.storeIfUnknownMessageId(first));

        // we assume that message is unknown
        assertTrue(history.storeIfUnknownMessageId(first));

        for (int e = 0; e < 1000; e++) {
            assertFalse(history.storeIfUnknownMessageId(String.valueOf(e)));
        }

        // we still know this entity
        assertTrue(history.storeIfUnknownMessageId(first));
    }


    @Test
    public void testBasicStuff_2() {
        val history = new HashHistoryHolder<String>(2048);

        val iterations = 1000000;
        val timeStart = System.nanoTime();
        for (int e = 0; e < iterations; e++) {
            assertFalse(history.storeIfUnknownMessageId(String.valueOf(e)));
        }
        val timeStop= System.nanoTime();

        log.info("Average time per iteration: [{} us]", (timeStop - timeStart) / iterations);
    }
}