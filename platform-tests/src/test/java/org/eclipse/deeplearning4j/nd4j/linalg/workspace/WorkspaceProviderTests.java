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

package org.eclipse.deeplearning4j.nd4j.linalg.workspace;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.nd4j.common.tests.tags.NativeTag;
import org.nd4j.common.tests.tags.TagNames;
import org.nd4j.linalg.BaseNd4jTestWithBackends;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.memory.AllocationsTracker;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.memory.enums.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.factory.Nd4jBackend;
import org.nd4j.linalg.api.memory.abstracts.Nd4jWorkspace;
import org.nd4j.linalg.workspace.WorkspaceUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.nd4j.linalg.workspace.WorkspaceUtils.getTotalRequiredMemoryForWorkspace;

@Slf4j
@Tag(TagNames.WORKSPACES)
@NativeTag
@Execution(ExecutionMode.SAME_THREAD)
public class WorkspaceProviderTests extends BaseNd4jTestWithBackends {
    private static final WorkspaceConfiguration basicConfiguration = WorkspaceConfiguration.builder().initialSize(81920)
            .overallocationLimit(0.1).policySpill(SpillPolicy.EXTERNAL).policyLearning(LearningPolicy.NONE)
            .policyMirroring(MirroringPolicy.FULL).policyAllocation(AllocationPolicy.OVERALLOCATE).build();

    private static final WorkspaceConfiguration bigConfiguration = WorkspaceConfiguration.builder()
            .initialSize(20 * 1024 * 1024L).overallocationLimit(0.1).policySpill(SpillPolicy.EXTERNAL)
            .policyLearning(LearningPolicy.NONE).policyMirroring(MirroringPolicy.FULL)
            .policyAllocation(AllocationPolicy.OVERALLOCATE).build();

    private static final WorkspaceConfiguration loopConfiguration = WorkspaceConfiguration.builder().initialSize(0)
            .overallocationLimit(0.1).policySpill(SpillPolicy.EXTERNAL).policyLearning(LearningPolicy.OVER_TIME)
            .policyMirroring(MirroringPolicy.FULL).policyAllocation(AllocationPolicy.STRICT).build();


    private static final WorkspaceConfiguration delayedConfiguration = WorkspaceConfiguration.builder().initialSize(0)
            .overallocationLimit(0.1).policySpill(SpillPolicy.EXTERNAL).policyLearning(LearningPolicy.OVER_TIME)
            .policyMirroring(MirroringPolicy.FULL).cyclesBeforeInitialization(3)
            .policyAllocation(AllocationPolicy.STRICT).build();

    private static final WorkspaceConfiguration reallocateConfiguration = WorkspaceConfiguration.builder()
            .initialSize(0).overallocationLimit(0.1).policySpill(SpillPolicy.REALLOCATE)
            .policyLearning(LearningPolicy.OVER_TIME).policyMirroring(MirroringPolicy.FULL)
            .policyAllocation(AllocationPolicy.STRICT).build();

    private static final WorkspaceConfiguration reallocateDelayedConfiguration = WorkspaceConfiguration.builder()
            .initialSize(0).overallocationLimit(0.1).policySpill(SpillPolicy.REALLOCATE)
            .cyclesBeforeInitialization(3).policyLearning(LearningPolicy.OVER_TIME)
            .policyMirroring(MirroringPolicy.FULL).policyAllocation(AllocationPolicy.STRICT).build();


    private static final WorkspaceConfiguration reallocateUnspecifiedConfiguration = WorkspaceConfiguration.builder()
            .initialSize(0).overallocationLimit(0.0).policySpill(SpillPolicy.REALLOCATE)
            .policyLearning(LearningPolicy.OVER_TIME).policyMirroring(MirroringPolicy.FULL)
            .policyAllocation(AllocationPolicy.OVERALLOCATE).policyReset(ResetPolicy.BLOCK_LEFT).build();



    private static final WorkspaceConfiguration firstConfiguration = WorkspaceConfiguration.builder().initialSize(0)
            .overallocationLimit(0.1).policySpill(SpillPolicy.EXTERNAL)
            .policyLearning(LearningPolicy.FIRST_LOOP).policyMirroring(MirroringPolicy.FULL)
            .policyAllocation(AllocationPolicy.STRICT).build();


    private static final WorkspaceConfiguration circularConfiguration = WorkspaceConfiguration.builder()
            .minSize(10 * 1024L * 1024L).overallocationLimit(1.0).policySpill(SpillPolicy.EXTERNAL)
            .policyLearning(LearningPolicy.FIRST_LOOP).policyMirroring(MirroringPolicy.FULL)
            .policyAllocation(AllocationPolicy.STRICT).policyReset(ResetPolicy.ENDOFBUFFER_REACHED).build();


    private static final WorkspaceConfiguration adsiConfiguration =
            WorkspaceConfiguration.builder().overallocationLimit(3.0).policySpill(SpillPolicy.REALLOCATE)
                    .policyLearning(LearningPolicy.FIRST_LOOP).policyMirroring(MirroringPolicy.FULL)
                    .policyAllocation(AllocationPolicy.OVERALLOCATE)
                    .policyReset(ResetPolicy.ENDOFBUFFER_REACHED).build();

    DataType initialType = Nd4j.dataType();

    @AfterEach
    public void shutUp() {
        Nd4j.getMemoryManager().setCurrentWorkspace(null);
        Nd4j.getWorkspaceManager().destroyAllWorkspacesForCurrentThread();
    }

    /**
     * This simple test checks for over-time learning with coefficient applied
     *
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testUnboundedLoop2(Nd4jBackend backend) {
        WorkspaceConfiguration configuration =
                WorkspaceConfiguration.builder().initialSize(0).policyReset(ResetPolicy.ENDOFBUFFER_REACHED)
                        .policyAllocation(AllocationPolicy.OVERALLOCATE).overallocationLimit(4.0)
                        .policyLearning(LearningPolicy.OVER_TIME).cyclesBeforeInitialization(5).build();

        Nd4jWorkspace ws1 =
                (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread(configuration, "ITER");

        long requiredMemory = getTotalRequiredMemoryForWorkspace(Nd4j.create(DataType.DOUBLE,100));
        //policy adds 30% to buffer size when reaching end of cycle
        long shiftedSize = ((long) (requiredMemory * 1.3)) + (8 - (((long) (requiredMemory * 1.3)) % 8));

        for (int x = 0; x < 100; x++) {
            try (Nd4jWorkspace wsI = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                    .getWorkspaceForCurrentThread(configuration, "ITER").notifyScopeEntered()) {
                INDArray array = Nd4j.create(DataType.DOUBLE,100);
                long bytes = getTotalRequiredMemoryForWorkspace(array);
            }

            // only checking after workspace is initialized
            if (x > 4) {
                assertEquals(shiftedSize, ws1.getInitialBlockSize());
                assertEquals(5 * shiftedSize, ws1.getCurrentSize());
            } else if (x < 4) {
                // we're making sure we're not initialize early
                assertEquals(0, ws1.getCurrentSize(),"Failed on iteration " + x);
            }
        }

        // maximum allocation amount is 100 elements during learning, and additional coefficient is 4.0. result is workspace of 500 elements
        assertEquals(5 * shiftedSize, ws1.getCurrentSize());

        assertNull(Nd4j.getMemoryManager().getCurrentWorkspace());
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testUnboundedLoop1(Nd4jBackend backend) {
        WorkspaceConfiguration configuration = WorkspaceConfiguration.builder()
                .initialSize(100 * 100 * DataType.DOUBLE.width()).policyReset(ResetPolicy.ENDOFBUFFER_REACHED)
                .policyAllocation(AllocationPolicy.STRICT).build();

        //end of buffer reached at 92, anything passed that does a reset
        int numArraysAllocated = 92;
        for (int x = 0; x < numArraysAllocated; x++) {
            try (Nd4jWorkspace ws1 = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                    .getWorkspaceForCurrentThread(configuration, "ITER").notifyScopeEntered()) {

                INDArray array = Nd4j.create(DataType.DOUBLE,100);
            }

            Nd4jWorkspace ws1 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread(configuration,
                    "ITER");
            INDArray array = Nd4j.create(DataType.DOUBLE, 100);
            assertEquals((x + 1) * getTotalRequiredMemoryForWorkspace(array), ws1.getPrimaryOffset(),"Failed equals at x " + x);
        }

        Nd4jWorkspace ws1 =
                (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread(configuration, "ITER");
        assertEquals(numArraysAllocated * getTotalRequiredMemoryForWorkspace( Nd4j.create(DataType.DOUBLE, 100)), ws1.getPrimaryOffset());

        // just to trigger reset
        ws1.notifyScopeEntered();

        // confirming reset
        //        assertEquals(0, ws1.getPrimaryOffset());

        ws1.notifyScopeLeft();

        assertNull(Nd4j.getMemoryManager().getCurrentWorkspace());
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testMultithreading1(Nd4jBackend backend) throws Exception {
        final List<MemoryWorkspace> workspaces = new CopyOnWriteArrayList<>();
        Nd4j.getWorkspaceManager().setDefaultWorkspaceConfiguration(basicConfiguration);

        Thread[] threads = new Thread[20];
        for (int x = 0; x < threads.length; x++) {
            threads[x] = new Thread(() -> {
                MemoryWorkspace workspace = Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread();
                workspaces.add(workspace);
            });

            threads[x].start();
        }

        for (int x = 0; x < threads.length; x++) {
            threads[x].join();
        }

        for (int x = 0; x < threads.length; x++) {
            for (int y = 0; y < threads.length; y++) {
                if (x == y)
                    continue;

                assertFalse(workspaces.get(x) == workspaces.get(y));
            }
        }

        assertNull(Nd4j.getMemoryManager().getCurrentWorkspace());
    }


    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testNestedWorkspacesOverlap2(Nd4jBackend backend) {
        Nd4j.getWorkspaceManager().setDefaultWorkspaceConfiguration(basicConfiguration);

        assertFalse(Nd4j.getWorkspaceManager().checkIfWorkspaceExists("WS1"));
        assertFalse(Nd4j.getWorkspaceManager().checkIfWorkspaceExists("WS2"));
        MemoryKind memoryKind = backend.getEnvironment().isCPU() ? MemoryKind.HOST : MemoryKind.DEVICE;
        try (Nd4jWorkspace ws1 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS1")
                .notifyScopeEntered()) {
            INDArray array = Nd4j.create(new double[] {6f, 3f, 1f, 9f, 21f});
            INDArray array3 = null;

            long reqMem = getTotalRequiredMemoryForWorkspace(array);
            assertEquals(reqMem + reqMem % 16, ws1.getPrimaryOffset());
            try (Nd4jWorkspace ws2 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS2")
                    .notifyScopeEntered()) {

                INDArray array2 = Nd4j.create(new double[] {1f, 2f, 3f, 4f, 5f});

                reqMem = getTotalRequiredMemoryForWorkspace(array2);
                assertEquals(reqMem + reqMem % 16, ws1.getPrimaryOffset());
                assertEquals(reqMem + reqMem % 16, ws2.getPrimaryOffset());

                try (Nd4jWorkspace ws3 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS1")
                        .notifyScopeBorrowed()) {
                    assertTrue(ws1 == ws3);
                    assertTrue(ws1 == Nd4j.getMemoryManager().getCurrentWorkspace());

                    array3 = array2.unsafeDuplication();
                    assertTrue(ws1 == array3.data().getParentWorkspace());
                    assertEquals(reqMem + reqMem % 16, ws2.getPrimaryOffset());
                    assertEquals(AllocationsTracker.getInstance().getTracker(ws1.getId()).currentBytes(memoryKind),
                            ws1.getPrimaryOffset());
                }

                log.info("Current workspace: {}", Nd4j.getMemoryManager().getCurrentWorkspace());
                assertTrue(ws2 == Nd4j.getMemoryManager().getCurrentWorkspace());

                assertEquals(reqMem + reqMem % 16, ws2.getPrimaryOffset());
                assertEquals((AllocationsTracker.getInstance().getTracker(ws1.getId())).currentBytes(memoryKind), ws1.getPrimaryOffset());

                assertEquals(15f, array3.sumNumber().floatValue(), 0.01f);
            }
        }

        log.info("------");

        assertNull(Nd4j.getMemoryManager().getCurrentWorkspace());
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    @Disabled
    @Tag(TagNames.NEEDS_VERIFY)
    public void testNestedWorkspacesOverlap1(Nd4jBackend backend) {
        Nd4j.getWorkspaceManager().setDefaultWorkspaceConfiguration(basicConfiguration);
        try (Nd4jWorkspace ws1 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS1").notifyScopeEntered()) {
            INDArray array = Nd4j.create(new float[] {1f, 2f, 3f, 4f, 5f});

            long reqMem = 5 * array.dataType().width();
            long add = ((Nd4jWorkspace.alignmentBase / 2) - reqMem % (Nd4jWorkspace.alignmentBase / 2));
            assertEquals(reqMem + add, ws1.getPrimaryOffset());
            try (Nd4jWorkspace ws2 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS2").notifyScopeEntered()) {

                INDArray array2 = Nd4j.create(new float[] {1f, 2f, 3f, 4f, 5f});

                reqMem = 5 * array2.dataType().width();
                assertEquals(reqMem + ((Nd4jWorkspace.alignmentBase / 2) - reqMem % (Nd4jWorkspace.alignmentBase / 2)), ws1.getPrimaryOffset());
                assertEquals(reqMem + ((Nd4jWorkspace.alignmentBase / 2) - reqMem % (Nd4jWorkspace.alignmentBase / 2)), ws2.getPrimaryOffset());

                try (Nd4jWorkspace ws3 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS1")
                        .notifyScopeBorrowed()) {
                    assertTrue(ws1 == ws3);

                    INDArray array3 = Nd4j.create(new float[] {1f, 2f, 3f, 4f, 5f});

                    assertEquals(reqMem + ((Nd4jWorkspace.alignmentBase / 2) - reqMem % (Nd4jWorkspace.alignmentBase / 2)), ws2.getPrimaryOffset());
                    assertEquals((reqMem + ((Nd4jWorkspace.alignmentBase / 2) - reqMem % (Nd4jWorkspace.alignmentBase / 2))) * 2, ws1.getPrimaryOffset());
                }
            }
        }

        assertNull(Nd4j.getMemoryManager().getCurrentWorkspace());
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testWorkspacesSerde3() throws Exception {
        INDArray array = Nd4j.create(DataType.DOUBLE,10).assign(1.0);
        INDArray restored = null;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        Nd4j.write(array, dos);

        try (Nd4jWorkspace workspace = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                .getAndActivateWorkspace(basicConfiguration, "WS_1")) {

            try (MemoryWorkspace wsO = Nd4j.getMemoryManager().scopeOutOfWorkspaces()) {
                workspace.enableDebug(true);

                ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                DataInputStream dis = new DataInputStream(bis);
                restored = Nd4j.read(dis);

                assertEquals(0, workspace.getPrimaryOffset());

                assertEquals(array.length(), restored.length());
                assertEquals(1.0f, restored.meanNumber().floatValue(), 1.0f);

                // we want to ensure it's the same cached shapeInfo used here
                assertEquals(array.shapeInfoDataBuffer(), restored.shapeInfoDataBuffer());
            }
        }
    }



    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testWorkspacesSerde2() throws Exception {
        INDArray array = Nd4j.create(10).assign(1.0).castTo(DataType.DOUBLE);
        INDArray restored = null;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        Nd4j.write(array, dos);

        try (Nd4jWorkspace workspace = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                .getAndActivateWorkspace(basicConfiguration, "WS_1")) {
            workspace.enableDebug(true);

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            DataInputStream dis = new DataInputStream(bis);
            restored = Nd4j.read(dis);

            long requiredMemory = getTotalRequiredMemoryForWorkspace(Nd4j.create(DataType.DOUBLE,10)) * 2;
            assertEquals(requiredMemory + requiredMemory % 8, workspace.getPrimaryOffset());

            assertEquals(array.length(), restored.length());
            assertEquals(1.0f, restored.meanNumber().floatValue(), 1.0f);

            // we want to ensure it's the same cached shapeInfo used here
            assertEquals(array.shapeInfoDataBuffer(), restored.shapeInfoDataBuffer());
        }
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testWorkspacesSerde1() throws Exception {
        int[] shape = new int[] {17, 57, 79};
        INDArray array = Nd4j.create(shape).assign(1.0);
        INDArray restored = null;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        Nd4j.write(array, dos);

        try (MemoryWorkspace workspace = Nd4j.getWorkspaceManager().getAndActivateWorkspace(bigConfiguration, "WS_1")) {
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            DataInputStream dis = new DataInputStream(bis);
            restored = Nd4j.read(dis);

            assertEquals(array.length(), restored.length());
            assertEquals(1.0f, restored.meanNumber().floatValue(), 1.0f);

            // we want to ensure it's the same cached shapeInfo used here
            assertEquals(array.shapeInfoDataBuffer(), restored.shapeInfoDataBuffer());
        }
    }


    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testCircularBufferReset1(Nd4jBackend backend) {
        Nd4jWorkspace workspace = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                .getWorkspaceForCurrentThread(circularConfiguration, "WSR_1");

        try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace("WSR_1")) {
            Nd4j.create(DataType.DOUBLE,10000);
            assertEquals(0, workspace.getCurrentSize());
            //note: 1 allocation of the array and a shape buffer should be the allocations here
            assertEquals(AllocationsTracker.getInstance()
                    .getTracker(workspace.getId()).totalExternalAllocationCount(), workspace.getNumberOfExternalAllocations());
        }

        assertEquals(10 * 1024L * 1024L, workspace.getCurrentSize());
        assertEquals(0, workspace.getPrimaryOffset());
        //note: 1 allocation of the array and a shape buffer should be the allocations here
        assertEquals(AllocationsTracker.getInstance()
                .getTracker(workspace.getId()).totalExternalAllocationCount(), workspace.getNumberOfExternalAllocations());

        for (int i = 0; i < 11 * 1024 * 1024; i += 10000 * DataType.DOUBLE.width()) {
            try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace("WSR_1")) {
                Nd4j.create(DataType.DOUBLE,10000);
            }


        }

        assertEquals(0, workspace.getNumberOfExternalAllocations());

    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testVariableInput1(Nd4jBackend backend) {
        //divide by 2 when since cuda doesn't allocate buffers from workspaces
        Nd4jWorkspace workspace = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                .getWorkspaceForCurrentThread(adsiConfiguration, "ADSI");

        INDArray array1 = null;
        INDArray array2 = null;

        try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(adsiConfiguration, "ADSI")) {
            // we allocate first element smaller then subsequent;
            array1 = Nd4j.create(DataType.DOUBLE, 8, 128, 100);
        }

        long requiredMemory = getTotalRequiredMemoryForWorkspace(Nd4j.create(DataType.DOUBLE, 8, 128, 100));
        long shiftedSize = ((long) (requiredMemory * 1.3)) + (8 - (((long) (requiredMemory * 1.3)) % 8));
        assertEquals( shiftedSize, workspace.getInitialBlockSize());
        assertEquals(shiftedSize * 4, workspace.getCurrentSize());
        assertEquals(0, workspace.getPrimaryOffset());
        assertEquals(0, workspace.getDeviceOffset());

        assertEquals(1, workspace.getCyclesCount());
        assertEquals(0, workspace.getStepNumber());


        try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(adsiConfiguration, "ADSI")) {
            // allocating same shape
            array1 = Nd4j.create(DataType.DOUBLE,8, 128, 100);
        }

        assertEquals(workspace.getInitialBlockSize(), workspace.getPrimaryOffset());
        assertEquals(workspace.getInitialBlockSize(), workspace.getDeviceOffset());

        assertEquals(2, workspace.getCyclesCount());
        assertEquals(0, workspace.getStepNumber());


        try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(adsiConfiguration, "ADSI")) {
            // allocating bigger shape
            array1 = Nd4j.create(DataType.DOUBLE, 8, 128, 200);
        }

        // offsets should be intact, allocation happened as pinned
        assertEquals(workspace.getInitialBlockSize(), workspace.getPrimaryOffset());
        assertEquals(workspace.getInitialBlockSize(), workspace.getDeviceOffset());
        //shape buffer + data buffer
        assertEquals(AllocationsTracker.getInstance().getTracker("ADSI").totalPinnedAllocationCount(), workspace.getNumberOfPinnedAllocations());

        assertEquals(3, workspace.getCyclesCount());
        assertEquals(0, workspace.getStepNumber());


        try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(adsiConfiguration, "ADSI")) {
            // allocating same shape
            array1 = Nd4j.create(DataType.DOUBLE, 8, 128, 100);
        }

        //shape buffer + data buffer * 2
        assertEquals(AllocationsTracker.getInstance().getTracker("ADSI").totalPinnedAllocationCount(), workspace.getNumberOfPinnedAllocations());
        assertEquals(0, workspace.getStepNumber());
        assertEquals(4, workspace.getCyclesCount());

        try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(adsiConfiguration, "ADSI")) {
            // allocating same shape
            array1 = Nd4j.create(DataType.DOUBLE, 8, 128, 100);
        }
        //shape buffer + data buffer * 3
        assertEquals(AllocationsTracker.getInstance().getTracker("ADSI").totalPinnedAllocationCount(), workspace.getNumberOfPinnedAllocations());
        assertEquals(1, workspace.getStepNumber());
        assertEquals(5, workspace.getCyclesCount());

        for (int i = 0; i < 12; i++) {
            try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(adsiConfiguration, "ADSI")) {
                // allocating same shape
                array1 = Nd4j.create(DataType.DOUBLE, 8, 128, 100);
            }
        }

        // Now we know that workspace was reallocated and offset was shifted to the end of workspace
        assertEquals(4, workspace.getStepNumber());

        requiredMemory = 8 * 128 * 200 * Nd4j.sizeOfDataType(DataType.DOUBLE);
        shiftedSize = ((long) (requiredMemory * 1.3)) + (8 - (((long) (requiredMemory * 1.3)) % 8));

        //assertEquals(shiftedSize * 4, workspace.getCurrentSize());
        assertEquals(workspace.getCurrentSize(), workspace.getPrimaryOffset());
        assertEquals(workspace.getCurrentSize(), workspace.getDeviceOffset());

    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testReallocate3(Nd4jBackend backend) {
        MemoryWorkspace workspace = Nd4j.getWorkspaceManager()
                .getWorkspaceForCurrentThread(reallocateUnspecifiedConfiguration, "WS_1");

        for (int i = 1; i <= 10; i++) {
            try (MemoryWorkspace ws = Nd4j.getWorkspaceManager()
                    .getAndActivateWorkspace(reallocateUnspecifiedConfiguration, "WS_1")) {
                INDArray array = Nd4j.create(DataType.DOUBLE,100 * i);
            }

            if (i == 3) {
                workspace.initializeWorkspace();
                assertEquals(getTotalRequiredMemoryForWorkspace(Nd4j.create(DataType.DOUBLE,100 * i)), workspace.getCurrentSize(),"Failed on iteration " + i);
            }
        }

        log.info("-----------------------------");

        for (int i = 10; i > 0; i--) {
            try (MemoryWorkspace ws = Nd4j.getWorkspaceManager()
                    .getAndActivateWorkspace(reallocateUnspecifiedConfiguration, "WS_1")) {
                INDArray array = Nd4j.create(DataType.DOUBLE,100 * i);
            }
        }

        workspace.initializeWorkspace();
        assertEquals(getTotalRequiredMemoryForWorkspace(Nd4j.create(DataType.DOUBLE,100 * 10)), workspace.getCurrentSize(),"Failed on final");
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testReallocate2(Nd4jBackend backend) {
        MemoryWorkspace workspace =
                Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread(reallocateDelayedConfiguration, "WS_1");

        for (int i = 1; i <= 10; i++) {
            try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(reallocateDelayedConfiguration,
                    "WS_1")) {
                INDArray array = Nd4j.create(DataType.DOUBLE,100 * i);
            }

            if (i >= 3)
                assertEquals(getTotalRequiredMemoryForWorkspace(Nd4j.create(DataType.DOUBLE,100 * i)), workspace.getCurrentSize(),"Failed on iteration " + i);
            else
                assertEquals(0, workspace.getCurrentSize());
        }
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testCircularLearning1(Nd4jBackend backend) {
        INDArray array1;
        INDArray array2;
        for (int i = 0; i < 2; i++) {
            try (MemoryWorkspace workspace =
                         Nd4j.getWorkspaceManager().getAndActivateWorkspace(circularConfiguration, "WSX")) {
                array1 = Nd4j.create(10).assign(1);
            }

            Nd4jWorkspace workspace = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                    .getWorkspaceForCurrentThread(circularConfiguration, "WSX");
            assertEquals(10 * 1024 * 1024L, workspace.getCurrentSize());
            log.info("Current step number: {}", workspace.getStepNumber());
            if (i == 0)
                assertEquals(0, workspace.getPrimaryOffset());
            else if (i == 1)
                assertEquals(workspace.getInitialBlockSize(), workspace.getPrimaryOffset());
        }

    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testReallocate1(Nd4jBackend backend) {
        try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(reallocateConfiguration, "WS_1")) {
            INDArray array = Nd4j.create(DataType.DOUBLE,100);
        }



        Nd4jWorkspace workspace = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                .getWorkspaceForCurrentThread(reallocateConfiguration, "WS_1");
        workspace.initializeWorkspace();

        assertEquals(getTotalRequiredMemoryForWorkspace(Nd4j.create(DataType.DOUBLE,100)), workspace.getCurrentSize());

        try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(reallocateConfiguration, "WS_1")) {
            INDArray array = Nd4j.create(DataType.DOUBLE,1000);
        }

        assertEquals(getTotalRequiredMemoryForWorkspace(Nd4j.create(DataType.DOUBLE,1000)), workspace.getMaxCycleAllocations());

        workspace.initializeWorkspace();

        assertEquals(getTotalRequiredMemoryForWorkspace(Nd4j.create(DataType.DOUBLE,1000)), workspace.getCurrentSize());

        // now we're working on reallocated array, that should be able to hold >100 elements
        try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(reallocateConfiguration, "WS_1")) {
            INDArray array = Nd4j.create(DataType.DOUBLE,500).assign(1.0);

            assertEquals(1.0, array.meanNumber().doubleValue(), 0.01);
        }
    }


    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testNestedWorkspaces10(Nd4jBackend backend) {
        for (int x = 1; x < 10; x++) {
            try (MemoryWorkspace ws1 = Nd4j.getWorkspaceManager().getAndActivateWorkspace(basicConfiguration, "WS_1")) {
                INDArray array1 = Nd4j.create(100 * x);
                try (MemoryWorkspace ws2 =
                             Nd4j.getWorkspaceManager().getAndActivateWorkspace(basicConfiguration, "WS_1")) {
                    INDArray array2 = Nd4j.create(100 * x);
                    try (MemoryWorkspace ws3 = Nd4j.getWorkspaceManager()
                            .getWorkspaceForCurrentThread(basicConfiguration, "WS_1").notifyScopeBorrowed()) {
                        INDArray array3 = Nd4j.create(DataType.DOUBLE,100 * x);
                    }

                }
            }
        }
    }


    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testNestedWorkspaces9(Nd4jBackend backend) {
        for (int x = 1; x < 10; x++) {
            try (MemoryWorkspace ws =
                         Nd4j.getWorkspaceManager().getAndActivateWorkspace(delayedConfiguration, "WS_1")) {
                INDArray array = Nd4j.create(DataType.DOUBLE,100 * x);
            }
        }

        Nd4jWorkspace workspace = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                .getWorkspaceForCurrentThread(delayedConfiguration, "WS_1");
        workspace.initializeWorkspace();
        INDArray array = Nd4j.create(DataType.DOUBLE,300);

        assertEquals(getTotalRequiredMemoryForWorkspace(array), workspace.getCurrentSize());
    }


    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testNestedWorkspaces8(Nd4jBackend backend) {
        try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(loopConfiguration, "WS_1")) {
            INDArray array = Nd4j.create(DataType.DOUBLE,100);
        }



        Nd4jWorkspace workspace = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                .getWorkspaceForCurrentThread(loopConfiguration, "WS_1");
        workspace.initializeWorkspace();

        INDArray array2 = Nd4j.create(DataType.DOUBLE,100);

        assertEquals(getTotalRequiredMemoryForWorkspace(array2), workspace.getCurrentSize());

        try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(loopConfiguration, "WS_1")) {
            INDArray array = Nd4j.create(DataType.DOUBLE,1000);
        }

        Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread(loopConfiguration, "WS_1").initializeWorkspace();

        assertEquals(getTotalRequiredMemoryForWorkspace(array2), workspace.getCurrentSize());
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testNestedWorkspaces7(Nd4jBackend backend) {
        try (Nd4jWorkspace wsExternal = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                .getAndActivateWorkspace(basicConfiguration, "External")) {
            INDArray array1 = Nd4j.create(10);
            INDArray array2 = null;
            INDArray array3 = null;
            INDArray array4 = null;
            INDArray array5 = null;


            try (Nd4jWorkspace wsFeedForward = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                    .getAndActivateWorkspace(basicConfiguration, "FeedForward")) {
                array2 = Nd4j.create(10);
                assertEquals(true, array2.isAttached());

                try (Nd4jWorkspace borrowed = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                        .getWorkspaceForCurrentThread("External").notifyScopeBorrowed()) {
                    array3 = Nd4j.create(10);

                    assertTrue(wsExternal == array3.data().getParentWorkspace());

                    try (MemoryWorkspace ws = Nd4j.getMemoryManager().scopeOutOfWorkspaces()) {
                        array4 = Nd4j.create(10);
                    }

                    array5 = Nd4j.create(10);
                    log.info("Workspace5: {}", array5.data().getParentWorkspace());
                    assertTrue(null == array4.data().getParentWorkspace());
                    assertFalse(array4.isAttached());
                    assertTrue(wsExternal == array5.data().getParentWorkspace());
                }

                assertEquals(true, array3.isAttached());
                assertEquals(false, array4.isAttached());
                assertEquals(true, array5.isAttached());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testNestedWorkspaces6(Nd4jBackend backend) {

        try (Nd4jWorkspace wsExternal = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                .getAndActivateWorkspace(firstConfiguration, "External")) {
            INDArray array1 = Nd4j.create(DataType.DOUBLE,10);
            INDArray array2 = null;
            INDArray array3 = null;
            INDArray array4 = null;


            try (Nd4jWorkspace wsFeedForward = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                    .getAndActivateWorkspace(firstConfiguration, "FeedForward")) {
                array2 = Nd4j.create(10);
                assertEquals(true, array2.isAttached());

                try (Nd4jWorkspace borrowed = (Nd4jWorkspace) Nd4j.getWorkspaceManager()
                        .getWorkspaceForCurrentThread("External").notifyScopeBorrowed()) {
                    array3 = Nd4j.create(DataType.DOUBLE,10);

                    assertTrue(wsExternal == array3.data().getParentWorkspace());
                }

                assertEquals(true, array3.isAttached());

                try (MemoryWorkspace ws = Nd4j.getMemoryManager().scopeOutOfWorkspaces()) {
                    array4 = Nd4j.create(DataType.DOUBLE,10);
                }

                assertEquals(false, array4.isAttached());
            }


            assertEquals(0, wsExternal.getCurrentSize());
            log.info("------");
        }
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testNestedWorkspaces5(Nd4jBackend backend) {
        Nd4j.getWorkspaceManager().setDefaultWorkspaceConfiguration(basicConfiguration);
        try (Nd4jWorkspace ws1 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS1")
                .notifyScopeEntered()) {

            INDArray array1 = Nd4j.create(DataType.DOUBLE,100);
            try (Nd4jWorkspace ws2 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS1")
                    .notifyScopeEntered()) {

                INDArray array2 = Nd4j.create(DataType.DOUBLE,100);
            }

            INDArray array3 = Nd4j.create(DataType.DOUBLE,100);

            long reqMem = getTotalRequiredMemoryForWorkspace(array3) * 3;
            assertEquals((int) (reqMem + reqMem % 8), ws1.getPrimaryOffset());


            reqMem = getTotalRequiredMemoryForWorkspace(array3) * 3;
            assertEquals(reqMem + reqMem % 8, ws1.getPrimaryOffset());
        }

        assertNull(Nd4j.getMemoryManager().getCurrentWorkspace());
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testNestedWorkspaces4(Nd4jBackend backend) {
        Nd4j.getWorkspaceManager().setDefaultWorkspaceConfiguration(basicConfiguration);

        try (Nd4jWorkspace ws1 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS1")
                .notifyScopeEntered()) {

            INDArray array1 = Nd4j.create(DataType.DOUBLE,100);

            try (Nd4jWorkspace ws2 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS2")
                    .notifyScopeEntered()) {
                INDArray array2 = Nd4j.create(DataType.DOUBLE,100);

                try (Nd4jWorkspace ws3 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS3")
                        .notifyScopeEntered()) {
                    INDArray array3 = Nd4j.create(DataType.DOUBLE,100);

                    assertEquals(getTotalRequiredMemoryForWorkspace(array3), ws1.getPrimaryOffset());
                    assertEquals(getTotalRequiredMemoryForWorkspace(array3), ws2.getPrimaryOffset());
                    assertEquals(getTotalRequiredMemoryForWorkspace(array3), ws3.getPrimaryOffset());
                }

                INDArray array2b = Nd4j.create(DataType.DOUBLE,100);

                assertEquals(getTotalRequiredMemoryForWorkspace(array2b) * 2, ws2.getPrimaryOffset());
            }

            INDArray array1b = Nd4j.create(DataType.DOUBLE,100);

            assertEquals(getTotalRequiredMemoryForWorkspace(array1b) * 2, ws1.getPrimaryOffset());
        }

        Nd4jWorkspace ws1 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS1");
        Nd4jWorkspace ws2 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS2");
        Nd4jWorkspace ws3 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS3");


        assertEquals(0 * DataType.DOUBLE.width(), ws1.getPrimaryOffset());
        assertEquals(0 * DataType.DOUBLE.width(), ws2.getPrimaryOffset());
        assertEquals(0 * DataType.DOUBLE.width(), ws3.getPrimaryOffset());

        assertNull(Nd4j.getMemoryManager().getCurrentWorkspace());
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testNestedWorkspaces3(Nd4jBackend backend) {
        Nd4j.getWorkspaceManager().setDefaultWorkspaceConfiguration(basicConfiguration);


        // We open top-level workspace
        try (Nd4jWorkspace ws1 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS1")
                .notifyScopeEntered()) {

            INDArray array1 = Nd4j.create(DataType.DOUBLE,100);

            assertEquals(getTotalRequiredMemoryForWorkspace(array1), ws1.getPrimaryOffset());

            // we open first nested workspace
            try (Nd4jWorkspace ws2 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS2")
                    .notifyScopeEntered()) {
                assertEquals(0 * DataType.DOUBLE.width(), ws2.getPrimaryOffset());

                INDArray array2 = Nd4j.create(DataType.DOUBLE,100);

                assertEquals(getTotalRequiredMemoryForWorkspace(array2), ws1.getPrimaryOffset());
                assertEquals(getTotalRequiredMemoryForWorkspace(array2), ws2.getPrimaryOffset());
            }

            // and second nexted workspace
            try (Nd4jWorkspace ws3 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS3")
                    .notifyScopeEntered()) {
                assertEquals(0 * DataType.DOUBLE.width(), ws3.getPrimaryOffset());

                INDArray array2 = Nd4j.create(DataType.DOUBLE,100);

                assertEquals(getTotalRequiredMemoryForWorkspace(array2), ws1.getPrimaryOffset());
                assertEquals(getTotalRequiredMemoryForWorkspace(array2), ws3.getPrimaryOffset());
            }

            // this allocation should happen within top-level workspace
            INDArray array1b = Nd4j.create(DataType.DOUBLE,100);

            //we allocated 2 arrays + 2 shape buffers
            assertEquals(getTotalRequiredMemoryForWorkspace(array1b) * 2, ws1.getPrimaryOffset());
        }

        assertEquals(null, Nd4j.getMemoryManager().getCurrentWorkspace());

        assertNull(Nd4j.getMemoryManager().getCurrentWorkspace());
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testNestedWorkspaces2(Nd4jBackend backend) {
        Nd4j.getWorkspaceManager().setDefaultWorkspaceConfiguration(basicConfiguration);

        try (Nd4jWorkspace ws1 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS1")
                .notifyScopeEntered()) {

            INDArray array1 = Nd4j.create(DataType.DOUBLE,100);


            assertEquals(getTotalRequiredMemoryForWorkspace(array1), ws1.getPrimaryOffset());

            for (int x = 1; x <= 100; x++) {
                try (Nd4jWorkspace ws2 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread(loopConfiguration, "WS2").notifyScopeEntered()) {
                    INDArray array2 = Nd4j.create(DataType.DOUBLE,x);
                }

                Nd4jWorkspace ws2 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS2");
                long reqMemory = getTotalRequiredMemoryForWorkspace(Nd4j.create(DataType.DOUBLE,x));
                assertEquals((int) (reqMemory + reqMemory % 16), ws2.getLastCycleAllocations());
            }

            Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS2").initializeWorkspace();
            long reqMemory = getTotalRequiredMemoryForWorkspace(Nd4j.create(DataType.DOUBLE,100));

            assertEquals(reqMemory, Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS2").getCurrentSize());
        }

        assertNull(Nd4j.getMemoryManager().getCurrentWorkspace());
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testNestedWorkspaces1(Nd4jBackend backend) {
        Nd4j.getWorkspaceManager().setDefaultWorkspaceConfiguration(basicConfiguration);


        try (Nd4jWorkspace ws1 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS1")
                .notifyScopeEntered()) {

            INDArray array1 = Nd4j.create(DataType.DOUBLE,100);

            assertEquals(getTotalRequiredMemoryForWorkspace(array1) , ws1.getPrimaryOffset());

            try (Nd4jWorkspace ws2 = (Nd4jWorkspace) Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread("WS2")
                    .notifyScopeEntered()) {
                assertEquals(0 * DataType.DOUBLE.width(), ws2.getPrimaryOffset());

                INDArray array2 = Nd4j.create(DataType.DOUBLE,100);

                assertEquals(getTotalRequiredMemoryForWorkspace(array2), ws1.getPrimaryOffset());
                assertEquals(getTotalRequiredMemoryForWorkspace(array2), ws2.getPrimaryOffset());
            }
        }

        assertNull(Nd4j.getMemoryManager().getCurrentWorkspace());
        log.info("---------------");
        Nd4j.getWorkspaceManager().destroyAllWorkspacesForCurrentThread();
    }




    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testNewWorkspace1(Nd4jBackend backend) {
        MemoryWorkspace workspace1 = Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread();

        assertNotEquals(null, workspace1);

        MemoryWorkspace workspace2 = Nd4j.getWorkspaceManager().getWorkspaceForCurrentThread();

        assertEquals(workspace1, workspace2);
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testWorkspaceGc_1() throws Exception {

        for (int e = 0; e < 10; e++) {
            val f = e;
            val t = new Thread(() -> {
                val wsConf = WorkspaceConfiguration.builder()
                        .initialSize(1000000).build();
                try (val ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(wsConf, "SomeRandomName999" + f)) {
                    val array = Nd4j.create(2, 2);
                }
                //Nd4j.getWorkspaceManager().destroyAllWorkspacesForCurrentThread();
            });
            t.start();
            t.join();

            System.gc();
            Thread.sleep(50);
        }

        System.gc();
        Thread.sleep(1000);
        System.gc();

        log.info("Done");
    }

    @Disabled
    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testMemcpy1(Nd4jBackend backend) {
        INDArray warmUp = Nd4j.create(100000);
        for (int x = 0; x < 5000; x++) {
            warmUp.addi(0.1);
        }

        WorkspaceConfiguration configuration =
                WorkspaceConfiguration.builder().policyMirroring(MirroringPolicy.HOST_ONLY)
                        .initialSize(1024L * 1024L * 1024L).policyLearning(LearningPolicy.NONE).build();

        INDArray array = Nd4j.createUninitialized(150000000);

        MemoryWorkspace workspace =
                Nd4j.getWorkspaceManager().createNewWorkspace(configuration, "HOST");
        workspace.notifyScopeEntered();


        INDArray memcpy = array.unsafeDuplication(false);


        workspace.notifyScopeLeft();

    }


    public int getAligned(int requiredMemory) {
        long div = requiredMemory % Nd4jWorkspace.alignmentBase;
        if (div != 0) requiredMemory += (Nd4jWorkspace.alignmentBase - div);
        return requiredMemory;
    }

    public int getAligned(long requiredMemory) {
        return  getAligned((int) requiredMemory);
    }


    @Override
    public char ordering() {
        return 'c';
    }
}
