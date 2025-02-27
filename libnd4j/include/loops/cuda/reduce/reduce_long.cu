/* ******************************************************************************
 *
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

//
//  @author raver119@gmail.com
//  @author Yurii Shyrma (iuriish@yahoo.com)
//
#include <exceptions/cuda_exception.h>
#include <execution/LaunchContext.h>
#include <helpers/DebugHelper.h>
#include <loops/legacy_ops.h>
#include <loops/reduce_long.h>
#include <loops/scalar.h>
#include <system/op_boilerplate.h>
#include <types/types.h>

using namespace simdOps;

////////////////////////////////////////////////////////////////////////
template <typename X, typename Z, typename OpType>
SD_KERNEL void simpleReduce(const void *x, const sd::LongType *outerXTadShapeInfo,
                            const sd::LongType *innerXTadShapeInfo, void *extraParams, void *vreductionBuffer, void *z,
                            const sd::LongType *zShapeInfo) {
  functions::reduce::ReduceLongFunction<X, Z>::template transformCudaXD<OpType>(
      x, outerXTadShapeInfo, innerXTadShapeInfo, extraParams, vreductionBuffer, z, zShapeInfo);
}

////////////////////////////////////////////////////////////////////////
template <typename X, typename Z, typename OpType>
SD_DEVICE void reduceScalarGeneric(const void *x, const sd::LongType *xShapeInfo, void *extraParams, void *z,
                                   const sd::LongType *zShapeInfo, int *dimension, int dimensionLength,
                                   void *reductionBuffer, const sd::LongType *tadOnlyShapeInfo) {
  functions::reduce::ReduceLongFunction<X, Z>::template execScalarCuda<OpType>(
      x, xShapeInfo, extraParams, z, zShapeInfo, reductionBuffer, tadOnlyShapeInfo);
}

////////////////////////////////////////////////////////////////////////
template <typename X, typename Z, typename OpType>
SD_KERNEL void simpleScalar(const void *x, const sd::LongType *xShapeInfo, void *extraParams, void *z,
                            const sd::LongType *zShapeInfo, int *dimension, int dimensionLength, void *reductionBuffer,
                            const sd::LongType *tadOnlyShapeInfo) {
  reduceScalarGeneric<X, Z, OpType>(x, xShapeInfo, extraParams, z, zShapeInfo, dimension, dimensionLength,
                                    reductionBuffer, tadOnlyShapeInfo);
}

namespace functions {
namespace reduce {

////////////////////////////////////////////////////////////////////////
template <typename X, typename Z>
template <typename OpType>
SD_DEVICE void ReduceLongFunction<X, Z>::aggregatePartials(void *vsPartials, sd::LongType tid, sd::LongType numItems,
                                                           void *vextraParams) {
  // start the shared memory loop on the next power of 2 less
  // than the block size.  If block size is not a power of 2,
  // accumulate the intermediate sums in the remainder range.

  auto sPartials = reinterpret_cast<Z *>(vsPartials);
  auto extraParams = reinterpret_cast<X *>(vextraParams);

  sd::LongType floorPow2 = numItems;

  if (floorPow2 & (floorPow2 - 1)) {
    while (floorPow2 & (floorPow2 - 1)) floorPow2 &= floorPow2 - 1;

    if (tid >= floorPow2)
      sPartials[tid - floorPow2] = OpType::update(sPartials[tid - floorPow2], sPartials[tid], extraParams);

    __syncthreads();
  }

  for (sd::LongType activeThreads = floorPow2 >> 1; activeThreads; activeThreads >>= 1) {
    if (tid < activeThreads && tid + activeThreads < numItems)
      sPartials[tid] = OpType::update(sPartials[tid], sPartials[tid + activeThreads], extraParams);

    __syncthreads();
  }
}

////////////////////////////////////////////////////////////////////////
template <typename X, typename Z>
template <typename OpType>
SD_DEVICE void ReduceLongFunction<X, Z>::transformCudaXD(const void *vx, const sd::LongType *outerXTadShapeInfo,
                                                         const sd::LongType *innerXTadShapeInfo, void *vextraParams,
                                                         void *vreductionBuffer, void *vz,
                                                         const sd::LongType *zShapeInfo) {
  auto x = reinterpret_cast<const X *>(vx);
  auto z = reinterpret_cast<Z *>(vz);
  auto extraParams = reinterpret_cast<X *>(vextraParams);

  // shared memory space for storing intermediate results
  __shared__ Z sPartials[SD_CUDA_BLOCK_SIZE];
  __shared__ int tadLen, numTads;
  __shared__ bool sameOffsets;

  if (threadIdx.x == 0) {
    sameOffsets = shape::haveSameShapeAndStrides(zShapeInfo, outerXTadShapeInfo);

    tadLen = shape::length(innerXTadShapeInfo);
    numTads = shape::length(outerXTadShapeInfo);
  }
  __syncthreads();

  int coords[SD_MAX_RANK];

  for (int r = blockIdx.x; r < numTads; r += gridDim.x) {
    shape::index2coords(r, outerXTadShapeInfo, coords);
    const auto outerOffset = shape::getOffset(outerXTadShapeInfo, coords);
    const auto zOffset = sameOffsets ? outerOffset : shape::getOffset(zShapeInfo, coords);

    const X *xTad = x + outerOffset;
    sPartials[threadIdx.x] = OpType::startingValue(xTad);

    for (int i = threadIdx.x; i < tadLen; i += blockDim.x)
      sPartials[threadIdx.x] =
          OpType::update(sPartials[threadIdx.x],
                         OpType::op(xTad[shape::getIndexOffset(i, innerXTadShapeInfo)], extraParams), extraParams);
    __syncthreads();

    // aggregate. do NOT reduce for elements > tadLen
    aggregatePartials<OpType>(sPartials, threadIdx.x, sd::math::sd_min<int>(blockDim.x, tadLen), extraParams);
    __syncthreads();

    if (threadIdx.x == 0) z[zOffset] = OpType::postProcess(sPartials[threadIdx.x], tadLen, extraParams);
  }
}

////////////////////////////////////////////////////////////////////////
template <typename X, typename Z>
template <typename OpType>
SD_DEVICE void ReduceLongFunction<X, Z>::execScalarCuda(const void *vx, const sd::LongType *xShapeInfo,
                                                        void *vextraParams, void *vz, const sd::LongType *zShapeInfo,
                                                        void *vreductionBuffer, const sd::LongType *tadOnlyShapeInfo) {
  auto x = reinterpret_cast<const X *>(vx);
  auto z = reinterpret_cast<Z *>(vz);
  auto extraParams = reinterpret_cast<X *>(vextraParams);
  auto reductionBuffer = reinterpret_cast<Z *>(vreductionBuffer);

  auto tid = blockDim.x * blockIdx.x + threadIdx.x;

  // shared memory space for storing intermediate results
  __shared__ Z sPartials[SD_CUDA_BLOCK_SIZE];
  __shared__ sd::LongType xEws;
  __shared__ sd::LongType len;

  if (threadIdx.x == 0) {
    xEws = shape::elementWiseStride(xShapeInfo);
    len = shape::length(xShapeInfo);
  }
  __syncthreads();

  sPartials[threadIdx.x] = OpType::startingValue(x);

  if (xEws > 0)
    for (int i = tid; i < len; i += (blockDim.x * gridDim.x))
      sPartials[threadIdx.x] =
          OpType::update(sPartials[threadIdx.x], OpType::op(x[i * xEws], extraParams), extraParams);
  else
    for (int i = tid; i < len; i += blockDim.x * gridDim.x)
      sPartials[threadIdx.x] = OpType::update(
          sPartials[threadIdx.x], OpType::op(x[shape::getIndexOffset(i, xShapeInfo)], extraParams), extraParams);

  __syncthreads();
  aggregatePartials<OpType>(sPartials, threadIdx.x, sd::math::sd_min<int>(blockDim.x, len), extraParams);
  __syncthreads();

  if (gridDim.x > 1) {
    auto tc = reinterpret_cast<unsigned int *>(reductionBuffer);
    __shared__ bool amLast;

    tid = threadIdx.x;
    if (threadIdx.x == 0)
      reductionBuffer[blockIdx.x] = sPartials[0];  // this->postProcess(sPartials[0],len,extraParams);

    __threadfence();
    __syncthreads();

    if (threadIdx.x == 0) {
      unsigned int ticket = atomicInc(&tc[16384], gridDim.x);
      amLast = (ticket == gridDim.x - 1);
    }

    __syncthreads();

    if (amLast) {
      tc[16384] = 0;
      sPartials[threadIdx.x] = OpType::startingValue(x);

      for (int i = threadIdx.x; i < gridDim.x; i += blockDim.x)
        sPartials[threadIdx.x] = OpType::update(sPartials[threadIdx.x], reductionBuffer[i], extraParams);

      __syncthreads();
      aggregatePartials<OpType>(sPartials, threadIdx.x, sd::math::sd_min<int>(gridDim.x, blockDim.x), extraParams);
      __syncthreads();

      if (threadIdx.x == 0) {
        z[0] = OpType::postProcess(sPartials[0], len, extraParams);
      }
    }
  } else {
    if (threadIdx.x == 0) {
      auto tc = reinterpret_cast<unsigned int *>(reductionBuffer);
      tc[16384] = 0;
      z[0] = OpType::postProcess(sPartials[0], len, extraParams);
    }
  }
}

////////////////////////////////////////////////////////////////////////
template <typename X, typename Z>
template <typename OpType>
SD_HOST void ReduceLongFunction<X, Z>::intermediateXD(dim3 launchDims, cudaStream_t *stream, const void *x,
                                                      const sd::LongType *dXShapeInfo, const sd::LongType *hXShapeInfo,
                                                      void *extraParams, void *vreductionBuffer, void *z,
                                                      const sd::LongType *dZShapeInfo, const sd::LongType *hZShapeInfo,
                                                      const int *dims) {
  if (shape::isEmpty(hXShapeInfo)) {
    if (shape::isEmpty(hZShapeInfo)) return;

    const auto startingVal = static_cast<Z>(OpType::startingValue(reinterpret_cast<const X *>(x)));

    auto res = cudaMemcpyAsync(sd::LaunchContext::defaultContext()->getScalarPointer(), &startingVal, sizeof(Z),
                               cudaMemcpyHostToDevice, *stream);
    if (res != 0)
      throw sd::cuda_exception::build("ReduceLongFunction<X,Z>::intermediateXD: failed to copy temporary scalar", res);

    auto ptr = sd::LaunchContext::defaultContext()->getScalarPointer();

    // scalar assign
    functions::scalar::ScalarTransform<Z, Z, Z>::executeCudaShaped(launchDims, stream, 14, z, dZShapeInfo, hXShapeInfo,
                                                                   z, dZShapeInfo, hZShapeInfo, ptr, nullptr);
  } else {
    const int zRank = shape::rank(hZShapeInfo);
    const int tadRank = shape::rank(hXShapeInfo) - zRank;

    auto outerPack = sd::ConstantShapeHelper::getInstance().createSubArrShapeInfo(hXShapeInfo, dims, zRank);
    auto innerPack = sd::ConstantShapeHelper::getInstance().createSubArrShapeInfo(hXShapeInfo, dims + zRank, tadRank);

    simpleReduce<X, Z, OpType><<<launchDims.x, launchDims.y, launchDims.z, *stream>>>(
        x, reinterpret_cast<sd::LongType const *>(outerPack->special()),
        reinterpret_cast<sd::LongType const *>(innerPack->special()), extraParams, vreductionBuffer, z, dZShapeInfo);
  }
}

////////////////////////////////////////////////////////////////////////
template <typename X, typename Z>
template <typename OpType>
SD_HOST void ReduceLongFunction<X, Z>::intermediateScalar(dim3 launchDims, cudaStream_t *stream, const void *x,
                                                          const sd::LongType *xShapeInfo,
                                                          const sd::LongType *hXShapeInfo, void *extraParams, void *z,
                                                          const sd::LongType *zShapeInfo,
                                                          const sd::LongType *hZShapeInfo, int *dimension,
                                                          int dimensionLength, void *reductionBuffer,
                                                          const sd::LongType *tadOnlyShapeInfo) {
  if (shape::isEmpty(hXShapeInfo)) {
    if (shape::isEmpty(hZShapeInfo)) return;

    const auto startingVal = static_cast<Z>(OpType::startingValue(reinterpret_cast<const X *>(x)));

    auto res = cudaMemcpyAsync(z, &startingVal, sizeof(Z), cudaMemcpyHostToDevice, *stream);
    if (res != 0)
      throw sd::cuda_exception::build("ReduceLongFunction<X,Z>::intermediateScalar: failed to copy resulting scalar",
                                      res);
  } else {
    simpleScalar<X, Z, OpType><<<launchDims.x, launchDims.y, launchDims.z, *stream>>>(
        x, xShapeInfo, extraParams, z, zShapeInfo, dimension, dimensionLength, reductionBuffer, tadOnlyShapeInfo);
  }
}

////////////////////////////////////////////////////////////////////////
template <typename X, typename Y>
SD_HOST void ReduceLongFunction<X, Y>::execReduceScalar(dim3 launchDims, cudaStream_t *stream, const int opNum,
                                                        const void *x, const sd::LongType *xShapeInfo,
                                                        const sd::LongType *hXShapeInfo, void *extraParams, void *z,
                                                        const sd::LongType *zShapeInfo, const sd::LongType *hZShapeInfo,
                                                        int *dimension, int dimensionLength, void *reductionBuffer,
                                                        const sd::LongType *tadOnlyShapeInfo) {
  DISPATCH_BY_OPNUM_TT(intermediateScalar,
                       PARAMS(launchDims, stream, x, xShapeInfo, hXShapeInfo, extraParams, z, zShapeInfo, hZShapeInfo,
                              dimension, dimensionLength, reductionBuffer, tadOnlyShapeInfo),
                       OPS_A(REDUCE_LONG_OPS));
  sd::DebugHelper::checkErrorCode(stream, "execReduceScalarFloat(...) failed");
}

////////////////////////////////////////////////////////////////////////
template <typename X, typename Y>
SD_HOST void ReduceLongFunction<X, Y>::execReduceXD(dim3 launchDims, cudaStream_t *stream, const int opNum,
                                                    const void *x, const sd::LongType *dXShapeInfo,
                                                    const sd::LongType *hXShapeInfo, void *extraParams,
                                                    void *vreductionBuffer, void *z, const sd::LongType *dZShapeInfo,
                                                    const sd::LongType *hZShapeInfo, const int *dims) {
  if (shape::length(hZShapeInfo) == 1) {
    ReduceLongFunction<X, Y>::execReduceScalar(launchDims, stream, opNum, x, dXShapeInfo, hXShapeInfo, extraParams, z,
                                               dZShapeInfo, hZShapeInfo, nullptr, 0, vreductionBuffer, nullptr);
  } else {
    DISPATCH_BY_OPNUM_TT(intermediateXD,
                         PARAMS(launchDims, stream, x, dXShapeInfo, hXShapeInfo, extraParams, vreductionBuffer, z,
                                dZShapeInfo, hZShapeInfo, dims),
                         OPS_A(REDUCE_LONG_OPS));
  }
  DEBUG_KERNEL(stream, opNum);
}

////////////////////////////////////////////////////////////////////////
template <typename X>
SD_DEVICE void initializeShared(X *extraParams, X **sPartials, int sMemSize) {
  int sPartialsLength = sMemSize / sizeof(X);
  X *sPartialsDeref = (X *)*sPartials;
  for (int i = 0; i < sPartialsLength; i++) sPartialsDeref[i] = extraParams[0];
}

BUILD_DOUBLE_TEMPLATE(template class ReduceLongFunction, , SD_COMMON_TYPES, SD_LONG_TYPES);

}  // namespace reduce
}  // namespace functions
