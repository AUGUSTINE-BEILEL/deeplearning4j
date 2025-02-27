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

package org.nd4j.linalg.workspace;

import lombok.NonNull;
import lombok.val;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.memory.abstracts.Nd4jWorkspace;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.memory.abstracts.DummyWorkspace;
import org.nd4j.linalg.factory.Nd4jBackend;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceUtils {

    private WorkspaceUtils() {
    }

    /**
     * Assert that no workspaces are currently open
     *
     * @param msg Message to include in the exception, if required
     */
    public static void assertNoWorkspacesOpen(String msg) throws ND4JWorkspaceException {
        assertNoWorkspacesOpen(msg, false);
    }

    /**
     * Assert that no workspaces are currently open
     *
     * @param msg Message to include in the exception, if required
     * @param allowScopedOut If true: don't fail if we have an open workspace but are currently scoped out
     */
    public static void assertNoWorkspacesOpen(String msg, boolean allowScopedOut) throws ND4JWorkspaceException {
        if (Nd4j.getWorkspaceManager().anyWorkspaceActiveForCurrentThread()) {

            MemoryWorkspace currWs = Nd4j.getMemoryManager().getCurrentWorkspace();
            if(allowScopedOut && (currWs == null || currWs instanceof DummyWorkspace))
                return; //Open WS but we've scoped out

            List<MemoryWorkspace> l = Nd4j.getWorkspaceManager().getAllWorkspacesForCurrentThread();
            List<String> workspaces = new ArrayList<>(l.size());
            for (MemoryWorkspace ws : l) {
                if(ws.isScopeActive()) {
                    workspaces.add(ws.getId());
                }
            }
            throw new ND4JWorkspaceException(msg + " - Open/active workspaces: " + workspaces);
        }
    }

    /**
     * Assert that the specified workspace is open and active
     *
     * @param ws       Name of the workspace to assert open and active
     * @param errorMsg Message to include in the exception, if required
     */
    public static void assertOpenAndActive(@NonNull String ws, @NonNull String errorMsg) throws ND4JWorkspaceException {
        if (!Nd4j.getWorkspaceManager().checkIfWorkspaceExistsAndActive(ws)) {
            throw new ND4JWorkspaceException(errorMsg);
        }
    }

    /**
     * Assert that the specified workspace is open, active, and is the current workspace
     *
     * @param ws       Name of the workspace to assert open/active/current
     * @param errorMsg Message to include in the exception, if required
     */
    public static void assertOpenActiveAndCurrent(@NonNull String ws, @NonNull String errorMsg) throws ND4JWorkspaceException {
        if (!Nd4j.getWorkspaceManager().checkIfWorkspaceExistsAndActive(ws)) {
            throw new ND4JWorkspaceException(errorMsg + " - workspace is not open and active");
        }
        MemoryWorkspace currWs = Nd4j.getMemoryManager().getCurrentWorkspace();
        if (currWs == null || !ws.equals(currWs.getId())) {
            throw new ND4JWorkspaceException(errorMsg + " - not the current workspace (current workspace: "
                    + (currWs == null ? null : currWs.getId()));
        }
    }

    /**
     * Assert that the specified array is valid, in terms of workspaces: i.e., if it is attached (and not in a circular
     * workspace), assert that the workspace is open, and that the data is not from an old generation.
     * @param array Array to check
     * @param msg   Message (prefix) to include in the exception, if required. May be null
     */
    public static void assertValidArray(INDArray array, String msg){
        if(array == null || !array.isAttached()){
            return;
        }

        val ws = array.data().getParentWorkspace();

        if (ws.getWorkspaceType() != MemoryWorkspace.Type.CIRCULAR) {

            if (!ws.isScopeActive()) {
                throw new ND4JWorkspaceException( (msg == null ? "" : msg + ": ") + "Array uses leaked workspace pointer " +
                        "from workspace " + ws.getId() + "\nAll open workspaces: " + allOpenWorkspaces());
            }

            if (ws.getGenerationId() != array.data().getGenerationId()) {
                throw new ND4JWorkspaceException( (msg == null ? "" : msg + ": ") + "Array outdated workspace pointer " +
                        "from workspace " + ws.getId() + " (array generation " + array.data().getGenerationId() +
                        ", current workspace generation " + ws.getGenerationId()  + ")\nAll open workspaces: " + allOpenWorkspaces());
            }
        }
    }

    private static List<String> allOpenWorkspaces() {
        List<MemoryWorkspace> l = Nd4j.getWorkspaceManager().getAllWorkspacesForCurrentThread();
        List<String> workspaces = new ArrayList<>(l.size());
        for( MemoryWorkspace ws : l){
            if(ws.isScopeActive()) {
                workspaces.add(ws.getId());
            }
        }
        return workspaces;
    }

    public static int getAligned(int requiredMemory) {
        long div = requiredMemory % Nd4jWorkspace.alignmentBase;
        if (div != 0) requiredMemory += (Nd4jWorkspace.alignmentBase - div);
        return requiredMemory;
    }

    public static int getAligned(long requiredMemory) {
        return  getAligned((int) requiredMemory);
    }

    public static int getShapeBufferRequireMemoryForWorkspace(INDArray arr) {
        return  getAligned(arr.shapeInfoJava().length * DataType.INT64.width());
    }




    /**
     * Returns the total amount of memory required per array for workspaces.
     * Typically for CPU it will be shape buffer + size of data type array
     * following:
     * getAligned(arr.length() * arr.dataType().width()) + getAligned(arr.shapeInfoJava().length * DataType.INT64.width())
     * where getAligned is {@link #getAligned(int)}
     * GPUS will only be:
     * etAligned(arr.length() * arr.dataType().width())
     *
     * This is due to shape buffers from cuda only being allocated
     * from a cache rather than workspaces itself.
     * @param arr the array to get the required memory for.
     * @return
     */
    public static int getTotalRequiredMemoryForWorkspace(INDArray arr) {
        if(!Nd4j.getBackend().getNDArrayClass().getName().toLowerCase().contains("cu")) {
            long ret =  getAligned(arr.length() * arr.dataType().width()) + getAligned(arr.shapeInfoJava().length * DataType.INT64.width());
            return (int) ret;
        } else {
            long ret = getAligned(arr.length() * arr.dataType().width());
            return (int) ret;
        }

    }
}
