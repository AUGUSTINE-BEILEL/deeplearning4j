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
package org.nd4j.samediff.frameworkimport.onnx.ir

import onnx.Onnx
import org.apache.commons.io.FileUtils
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.onnxruntime.runner.OnnxRuntimeRunner
import org.nd4j.samediff.frameworkimport.ir.IRGraph
import org.nd4j.samediff.frameworkimport.onnx.*
import org.nd4j.samediff.frameworkimport.runner.IRGraphRunner
import java.io.File
import java.util.*

class OnnxIRGraphRunner(graphDef: OnnxIRGraph, inputNames: List<String>, outputNames: List<String>,opSet: Long = 13L,irVersion: Long = 7L): IRGraphRunner<
        Onnx.GraphProto,
        Onnx.NodeProto,
        Onnx.NodeProto,
        Onnx.TensorProto, Onnx.AttributeProto, Onnx.AttributeProto, Onnx.TensorProto.DataType> {
    val graphDef = graphDef
    val inputNames = inputNames
    val outputNames = outputNames
    val graphRunner: OnnxRuntimeRunner

    init {
        val uuid = UUID.randomUUID().toString()
        val tempFile = File("tempFile-$uuid.proto")
        val graphDefBuilder = graphDef.graphDef.toBuilder()
        //onnx runtime doesn't allow any outputs that aren't defined
        //already in the model, we need to dynamically modify the model at runtime
        //to allow things like intermediate results
 /*       outputNames.forEach {
            if(!graphDef.outputList.contains(it))
                graphDefBuilder.addOutput(ValueInfoProto {
                    name = it
                })
        }*/

        val modelProto =  prepareGraphForExecAndExport(graphDef.graphDef,outputNames,opSet,irVersion)
        FileUtils.writeByteArrayToFile(tempFile, modelProto.toByteArray())
        graphRunner = OnnxRuntimeRunner.builder()
            .modelUri(tempFile.absolutePath)
            .build()
        tempFile.deleteOnExit()
    }

    override fun graph(): IRGraph<Onnx.GraphProto, Onnx.NodeProto, Onnx.NodeProto, Onnx.TensorProto, Onnx.AttributeProto, Onnx.AttributeProto, Onnx.TensorProto.DataType> {
        return graphDef
    }

    override fun run(inputs: Map<String, INDArray>): Map<String, INDArray> {
        return graphRunner.exec(inputs)
    }

    override fun runSequence(inputs: Map<String, Array<INDArray>>): Map<String, Array<INDArray>> {
        return graphRunner.execSequence(inputs)
    }

}