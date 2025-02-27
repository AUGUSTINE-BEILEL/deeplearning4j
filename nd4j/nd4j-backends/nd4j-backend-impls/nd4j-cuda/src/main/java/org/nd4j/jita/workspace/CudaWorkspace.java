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

package org.nd4j.jita.workspace;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.RandomUtils;
import org.bytedeco.javacpp.Pointer;
import org.nd4j.jita.allocator.impl.AllocationShape;
import org.nd4j.jita.allocator.impl.AtomicAllocator;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.memory.AllocationsTracker;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.memory.enums.*;
import org.nd4j.linalg.api.memory.pointers.PagedPointer;
import org.nd4j.linalg.api.memory.pointers.PointersPair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.memory.abstracts.Nd4jWorkspace;
import org.nd4j.nativeblas.NativeOpsHolder;
import org.nd4j.linalg.api.memory.Deallocator;
import java.util.List;
import java.util.Queue;
import org.nd4j.allocator.impl.MemoryTracker;

import static org.nd4j.linalg.workspace.WorkspaceUtils.getAligned;


/**
 * CUDA-aware MemoryWorkspace implementation
 *
 * @author raver119@gmail.com
 */
@Slf4j
public class CudaWorkspace extends Nd4jWorkspace {

    public final static long BASE_CUDA_DATA_BUFFER_OFFSET = RandomUtils.nextLong();

    public CudaWorkspace(@NonNull WorkspaceConfiguration configuration) {
        super(configuration);
    }

    public CudaWorkspace(@NonNull WorkspaceConfiguration configuration, @NonNull String workspaceId) {
        super(configuration, workspaceId);
    }

    public CudaWorkspace(@NonNull WorkspaceConfiguration configuration, @NonNull String workspaceId, Integer deviceId) {
        super(configuration, workspaceId);
        this.deviceId = deviceId;
    }

    @Override
    protected void init() {
        if (workspaceConfiguration.getPolicyLocation() == LocationPolicy.MMAP) {
            throw new ND4JIllegalStateException("CUDA do not support MMAP workspaces yet");
        }

        super.init();

        if (currentSize.get() > 0) {
            isInit.set(true);

            long bytes = currentSize.get();

            if (isDebug.get())
                log.info("Allocating [{}] workspace on device_{}, {} bytes...", id, Nd4j.getAffinityManager().getDeviceForCurrentThread(), bytes);

            if (isDebug.get()) {
                Nd4j.getWorkspaceManager().printAllocationStatisticsForCurrentThread();
            }

            Pointer ptr = memoryManager.allocate((bytes + SAFETY_OFFSET), MemoryKind.HOST, false);
            if (ptr == null)
                throw new ND4JIllegalStateException("Can't allocate memory for workspace");

            workspace.setHostPointer(new PagedPointer(ptr));

            if (workspaceConfiguration.getPolicyMirroring() != MirroringPolicy.HOST_ONLY) {
                workspace.setDevicePointer(new PagedPointer(memoryManager.allocate((bytes + SAFETY_OFFSET), MemoryKind.DEVICE, false)));
                AllocationsTracker.getInstance().markAllocated(AllocationKind.GENERAL, Nd4j.getAffinityManager().getDeviceForCurrentThread(), bytes + SAFETY_OFFSET);

                MemoryTracker.getInstance().incrementWorkspaceAllocatedAmount(Nd4j.getAffinityManager().getDeviceForCurrentThread(), bytes + SAFETY_OFFSET);

                // if base pointer isn't aligned to 16 bytes (128 bits) - adjust the offset then
                val addr = workspace.getDevicePointer().address();
                val div = addr % alignmentBase;
                if (div != 0) {
                    deviceOffset.set(alignmentBase - div);
                    hostOffset.set(alignmentBase - div);
                }
            }
        }
    }

    @Override
    public PagedPointer alloc(long requiredMemory, DataType type, boolean initialize) {
        return this.alloc(requiredMemory, MemoryKind.DEVICE, type, initialize);
    }

    @Override
    public long requiredMemoryPerArray(INDArray arr) {
        long ret = getAligned(arr.length() * arr.dataType().width());
        return (int) ret;
    }


    @Override
    public synchronized void destroyWorkspace(boolean extended) {
        val size = currentSize.getAndSet(0);
        reset();

        if (extended)
            clearExternalAllocations();

        clearPinnedAllocations(extended);

        if (workspace.getHostPointer() != null)
            NativeOpsHolder.getInstance().getDeviceNativeOps().freeHost(workspace.getHostPointer());

        if (workspace.getDevicePointer() != null) {
            NativeOpsHolder.getInstance().getDeviceNativeOps().freeDevice(workspace.getDevicePointer(), 0);
            AllocationsTracker.getInstance().markReleased(AllocationKind.GENERAL, Nd4j.getAffinityManager().getDeviceForCurrentThread(), size + SAFETY_OFFSET);
            AllocationsTracker.getInstance().getTracker(id).deallocatePinned(MemoryKind.DEVICE,size + SAFETY_OFFSET);

            MemoryTracker.getInstance().decrementWorkspaceAmount(Nd4j.getAffinityManager().getDeviceForCurrentThread(), size + SAFETY_OFFSET);
        }

        workspace.setDevicePointer(null);
        workspace.setHostPointer(null);

    }


    @Override
    public PagedPointer alloc(long requiredMemory, MemoryKind kind, DataType type, boolean initialize) {
        long numElements = requiredMemory / Nd4j.sizeOfDataType(type);

        // alignment
        requiredMemory = alignMemory(requiredMemory);
        AllocationsTracker.getInstance().getTracker(id).allocate(type,kind,numElements,requiredMemory);

        if (!isUsed.get()) {
            if (disabledCounter.incrementAndGet() % 10 == 0)
                log.warn("Workspace was turned off, and wasn't enabled after {} allocations", disabledCounter.get());

            if (kind == MemoryKind.DEVICE) {
                val pointer = new PagedPointer(memoryManager.allocate(requiredMemory, MemoryKind.DEVICE, initialize), numElements);
                externalAllocations.add(new PointersPair(null, pointer));
                MemoryTracker.getInstance().incrementWorkspaceAllocatedAmount(Nd4j.getAffinityManager().getDeviceForCurrentThread(), requiredMemory);
                return pointer;
            } else {
                val pointer = new PagedPointer(memoryManager.allocate(requiredMemory, MemoryKind.HOST, initialize), numElements);
                externalAllocations.add(new PointersPair(pointer, null));
                return pointer;
            }


        }

        boolean trimmer = (workspaceConfiguration.getPolicyReset() == ResetPolicy.ENDOFBUFFER_REACHED && requiredMemory + cycleAllocations.get() > initialBlockSize.get() && initialBlockSize.get() > 0 && kind == MemoryKind.DEVICE) || trimmedMode.get();

        if (trimmer && workspaceConfiguration.getPolicySpill() == SpillPolicy.REALLOCATE && !trimmedMode.get()) {
            trimmedMode.set(true);
            trimmedStep.set(stepsCount.get());
        }

        if (kind == MemoryKind.DEVICE) {
            if (deviceOffset.get() + requiredMemory <= currentSize.get() && !trimmer && Nd4j.getWorkspaceManager().getDebugMode() != DebugMode.SPILL_EVERYTHING) {
                cycleAllocations.addAndGet(requiredMemory);
                long prevOffset = deviceOffset.getAndAdd(requiredMemory);
                if (workspaceConfiguration.getPolicyMirroring() == MirroringPolicy.HOST_ONLY)
                    return null;

                val ptr = workspace.getDevicePointer().withOffset(prevOffset, numElements);

                if (isDebug.get())
                    log.info("Workspace [{}] device_{}: alloc array of {} bytes, capacity of {} elements; prevOffset: {}; newOffset: {}; size: {}; address: {}", id, Nd4j.getAffinityManager().getDeviceForCurrentThread(), requiredMemory, numElements, prevOffset, deviceOffset.get(), currentSize.get(), ptr.address());

                if (initialize) {
                    val context = AtomicAllocator.getInstance().getDeviceContext();

                    int ret = NativeOpsHolder.getInstance().getDeviceNativeOps().memsetAsync(ptr, 0, requiredMemory, 0, context.getSpecialStream());
                    if (ret == 0)
                        throw new ND4JIllegalStateException("memset failed device_" + Nd4j.getAffinityManager().getDeviceForCurrentThread());

                    context.syncSpecialStream();
                }

                return ptr;
            } else {

                // spill
                if (workspaceConfiguration.getPolicyReset() == ResetPolicy.ENDOFBUFFER_REACHED && currentSize.get() > 0 && !trimmer && Nd4j.getWorkspaceManager().getDebugMode() != DebugMode.SPILL_EVERYTHING) {
                    //log.info("End of space reached. Current offset: {}; requiredMemory: {}", deviceOffset.get(), requiredMemory);
                    deviceOffset.set(0);
                    resetPlanned.set(true);
                    return alloc(requiredMemory, kind, type, initialize);
                }

                if (!trimmer) {
                    spilledAllocationsSize.addAndGet(requiredMemory);
                    AllocationsTracker.getInstance().getTracker(id).allocateSpilled(type,kind,numElements,requiredMemory);
                } else {
                    pinnedAllocationsSize.addAndGet(requiredMemory);
                    AllocationsTracker.getInstance().getTracker(id).allocatePinned(type,kind,numElements,requiredMemory);
                }
                if (isDebug.get()) {
                    log.info("Workspace [{}] device_{}: spilled DEVICE array of {} bytes, capacity of {} elements", id, Nd4j.getAffinityManager().getDeviceForCurrentThread(), requiredMemory, numElements);
                }

                val shape = new AllocationShape(requiredMemory / Nd4j.sizeOfDataType(type), Nd4j.sizeOfDataType(type), type);

                cycleAllocations.addAndGet(requiredMemory);

                if (workspaceConfiguration.getPolicyMirroring() == MirroringPolicy.HOST_ONLY)
                    return null;

                switch (workspaceConfiguration.getPolicySpill()) {
                    case REALLOCATE:
                    case EXTERNAL:
                        if (!trimmer) {
                            externalCount.incrementAndGet();
                            val pointer = new PagedPointer(memoryManager.allocate(requiredMemory, MemoryKind.DEVICE, initialize), numElements);
                            pointer.isLeaked();

                            val pp = new PointersPair(null, pointer);
                            pp.setRequiredMemory(requiredMemory);
                            externalAllocations.add(pp);
                            AllocationsTracker.getInstance()
                                    .getTracker(id).allocateExternal(type,kind,numElements,requiredMemory);
                            MemoryTracker.getInstance().incrementWorkspaceAllocatedAmount(Nd4j.getAffinityManager().getDeviceForCurrentThread(), requiredMemory);
                            return pointer;
                        } else {
                            pinnedCount.incrementAndGet();

                            val pointer = new PagedPointer(memoryManager.allocate(requiredMemory, MemoryKind.DEVICE, initialize), numElements);
                            pointer.isLeaked();

                            pinnedAllocations.add(new PointersPair(stepsCount.get(), requiredMemory, null, pointer));
                            MemoryTracker.getInstance().incrementWorkspaceAllocatedAmount(Nd4j.getAffinityManager().getDeviceForCurrentThread(), requiredMemory);
                            return pointer;
                        }
                    case FAIL:
                    default: {
                        throw new ND4JIllegalStateException("Can't allocate memory: Workspace is full");
                    }
                }
            }
        } else if (kind == MemoryKind.HOST) {
            if (hostOffset.get() + requiredMemory <= currentSize.get() && !trimmer && Nd4j.getWorkspaceManager().getDebugMode() != DebugMode.SPILL_EVERYTHING) {

                long prevOffset = hostOffset.getAndAdd(requiredMemory);

                val ptr = workspace.getHostPointer().withOffset(prevOffset, numElements);

                if (initialize)
                    Pointer.memset(ptr, 0, requiredMemory);
                return ptr;
            } else {
                //     log.info("Spilled HOST array of {} bytes, capacity of {} elements", requiredMemory, numElements);
                if (workspaceConfiguration.getPolicyReset() == ResetPolicy.ENDOFBUFFER_REACHED && currentSize.get() > 0 && !trimmer && Nd4j.getWorkspaceManager().getDebugMode() != DebugMode.SPILL_EVERYTHING) {
                    //log.info("End of space reached. Current offset: {}; requiredMemory: {}", deviceOffset.get(), requiredMemory);
                    hostOffset.set(0);
                    //resetPlanned.set(true);
                    return alloc(requiredMemory, kind, type, initialize);
                }


                switch (workspaceConfiguration.getPolicySpill()) {
                    case REALLOCATE:
                    case EXTERNAL:
                        if (!trimmer) {
                            PagedPointer pointer = new PagedPointer(memoryManager.allocate(requiredMemory, MemoryKind.HOST, initialize), numElements);
                            AllocationsTracker.getInstance()
                                    .getTracker(id).allocateExternal(type,kind,numElements,requiredMemory);

                            externalAllocations.add(new PointersPair(pointer, null));
                            return pointer;
                        } else {
                            PagedPointer pointer = new PagedPointer(memoryManager.allocate(requiredMemory, MemoryKind.HOST, initialize), numElements);
                            pointer.isLeaked();

                            pinnedAllocations.add(new PointersPair(stepsCount.get(), 0L, pointer, null));
                            return pointer;
                        }
                    case FAIL:
                    default: {
                        throw new ND4JIllegalStateException("Can't allocate memory: Workspace is full");
                    }
                }
            }
        } else throw new ND4JIllegalStateException("Unknown MemoryKind was passed in: " + kind);

    }

    @Override
    protected void clearPinnedAllocations(boolean extended) {
        if (isDebug.get())
            log.info("Workspace [{}] device_{} threadId {} cycle {}: clearing pinned allocations...", id, Nd4j.getAffinityManager().getDeviceForCurrentThread(), Thread.currentThread().getId(), cyclesCount.get());

        while (!pinnedAllocations.isEmpty()) {
            val pair = pinnedAllocations.peek();
            if (pair == null)
                throw new RuntimeException();

            long stepNumber = pair.getAllocationCycle();
            long stepCurrent = stepsCount.get();

            if (isDebug.get())
                log.info("Allocation step: {}; Current step: {}", stepNumber, stepCurrent);

            if (stepNumber + 2 < stepCurrent || extended) {
                pinnedAllocations.remove();

                if (pair.getDevicePointer() != null) {
                    NativeOpsHolder.getInstance().getDeviceNativeOps().freeDevice(pair.getDevicePointer(), 0);
                    MemoryTracker.getInstance().decrementWorkspaceAmount(Nd4j.getAffinityManager().getDeviceForCurrentThread(), pair.getRequiredMemory());
                    pinnedCount.decrementAndGet();
                    AllocationsTracker.getInstance().getTracker(id).deallocatePinned(MemoryKind.DEVICE,pair.getRequiredMemory());

                    if (isDebug.get())
                        log.info("deleting external device allocation ");
                }

                if (pair.getHostPointer() != null) {
                    NativeOpsHolder.getInstance().getDeviceNativeOps().freeHost(pair.getHostPointer());

                    if (isDebug.get())
                        log.info("deleting external host allocation ");
                }

                val sizez = pair.getRequiredMemory() * -1;
                pinnedAllocationsSize.addAndGet(sizez);
                AllocationsTracker.getInstance().getTracker(id).deallocatePinned(MemoryKind.DEVICE,sizez);

            } else {
                break;
            }
        }
    }

    @Override
    protected void clearExternalAllocations() {
        if (isDebug.get())
            log.info("Workspace [{}] device_{} threadId {} guid [{}]: clearing external allocations...", id, Nd4j.getAffinityManager().getDeviceForCurrentThread(), Thread.currentThread().getId(), guid);

        Nd4j.getExecutioner().commit();

        try {
            for (PointersPair pair : externalAllocations) {
                if (pair.getHostPointer() != null) {
                    NativeOpsHolder.getInstance().getDeviceNativeOps().freeHost(pair.getHostPointer());

                    if (isDebug.get())
                        log.info("deleting external host allocation... ");
                }

                if (pair.getDevicePointer() != null) {
                    NativeOpsHolder.getInstance().getDeviceNativeOps().freeDevice(pair.getDevicePointer(), 0);

                    if (isDebug.get())
                        log.info("deleting external device allocation... ");

                    val sizez = pair.getRequiredMemory();
                    if (sizez != null) {
                        AllocationsTracker.getInstance().markReleased(AllocationKind.GENERAL, Nd4j.getAffinityManager().getDeviceForCurrentThread(), sizez);
                        MemoryTracker.getInstance().decrementWorkspaceAmount(Nd4j.getAffinityManager().getDeviceForCurrentThread(), sizez);
                    }
                }
            }
        } catch (Exception e) {
            log.error("RC: Workspace [{}] device_{} threadId {} guid [{}]: clearing external allocations...", id, Nd4j.getAffinityManager().getDeviceForCurrentThread(), Thread.currentThread().getId(), guid);
            throw new RuntimeException(e);
        }

        spilledAllocationsSize.set(0);
        externalCount.set(0);
        externalAllocations.clear();
    }

    @Override
    protected void resetWorkspace() {
    }

    protected PointersPair workspace() {
        return workspace;
    }

    protected Queue<PointersPair> pinnedPointers() {
        return pinnedAllocations;
    }

    protected List<PointersPair> externalPointers() {
        return externalAllocations;
    }

    @Override
    public Deallocator deallocator() {
        return new CudaWorkspaceDeallocator(this);
    }

    @Override
    public long getUniqueId() {
        return BASE_CUDA_DATA_BUFFER_OFFSET + Nd4j.getDeallocatorService().nextValue();
    }

    @Override
    public int targetDevice() {
        return deviceId;
    }

    @Override
    public long getPrimaryOffset() {
        return getDeviceOffset();
    }
}
