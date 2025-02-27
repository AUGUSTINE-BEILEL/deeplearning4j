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
//  @author Yurii Shyrma (iuriish@yahoo.com), created on 20.01.2018
//

#include <system/op_boilerplate.h>
#if NOT_EXCLUDED(OP_svd)

#include <ops/declarable/CustomOperations.h>
#include <ops/declarable/helpers/svd.h>

namespace sd {
namespace ops {

CUSTOM_OP_IMPL(svd, 1, 1, false, 0, 3) {
  auto x = INPUT_VARIABLE(0);

  const int rank = x->rankOf();
  REQUIRE_TRUE(rank >= 2, 0, "SVD OP: the rank of input array must be >=2, but got %i instead!", rank);

  bool fullUV = (bool)INT_ARG(0);
  const bool calcUV = (bool)INT_ARG(1);

  if (calcUV == false) fullUV = false;

  const int switchNum = INT_ARG(2);

  // #ifndef __CUDABLAS__
  helpers::svd(block.launchContext(), x,
               {OUTPUT_VARIABLE(0), calcUV ? OUTPUT_VARIABLE(1) : nullptr, calcUV ? OUTPUT_VARIABLE(2) : nullptr},
               fullUV, calcUV, switchNum);
  // #endif

  return sd::Status::OK;
  ;
}

DECLARE_TYPES(svd) {
  getOpDescriptor()->setAllowedInputTypes(0, {DataType::FLOAT32, DataType ::DOUBLE, DataType::HALF})->setSameMode(true);
}

DECLARE_SHAPE_FN(svd) {
  auto inShapeInfo = inputShape->at(0);
  bool fullUV = (bool)INT_ARG(0);
  bool calcUV = (bool)INT_ARG(1);

  const int rank = inShapeInfo[0];
  REQUIRE_TRUE(rank >= 2, 0, "SVD OP: the rank of input array must be >=2, but got %i instead!", rank);

  const int diagSize = inShapeInfo[rank] < inShapeInfo[rank - 1] ? inShapeInfo[rank] : inShapeInfo[rank - 1];

  sd::LongType* sShapeInfo(nullptr);
  if (rank == 2) {
    ALLOCATE(sShapeInfo, block.getWorkspace(), shape::shapeInfoLength(1), sd::LongType);
    sShapeInfo[0] = 1;
    sShapeInfo[1] = diagSize;
  } else {
    ALLOCATE(sShapeInfo, block.getWorkspace(), shape::shapeInfoLength(rank - 1), sd::LongType);
    sShapeInfo[0] = rank - 1;
    for (int i = 1; i <= rank - 2; ++i) sShapeInfo[i] = inShapeInfo[i];
    sShapeInfo[rank - 1] = diagSize;
  }

  ShapeUtils::updateStridesAndType(sShapeInfo, inShapeInfo, shape::order(inShapeInfo));

  if (calcUV) {
    sd::LongType *uShapeInfo(nullptr), *vShapeInfo(nullptr);
    COPY_SHAPE(inShapeInfo, uShapeInfo);
    COPY_SHAPE(inShapeInfo, vShapeInfo);

    if (fullUV) {
      uShapeInfo[rank] = uShapeInfo[rank - 1];
      vShapeInfo[rank - 1] = vShapeInfo[rank];
    } else {
      uShapeInfo[rank] = diagSize;
      vShapeInfo[rank - 1] = vShapeInfo[rank];
      vShapeInfo[rank] = diagSize;
    }

    shape::updateStrides(uShapeInfo, shape::order(inShapeInfo));
    shape::updateStrides(vShapeInfo, shape::order(inShapeInfo));
    auto desc1 = new ShapeDescriptor(sShapeInfo);
    auto desc2 = new ShapeDescriptor(uShapeInfo);
    auto desc3 = new ShapeDescriptor(vShapeInfo);
    auto result = SHAPELIST(ConstantShapeHelper::getInstance().createShapeInfo(desc1),
                            ConstantShapeHelper::getInstance().createShapeInfo(desc2),
                            ConstantShapeHelper::getInstance().createShapeInfo(desc3));
    RELEASE(sShapeInfo, block.workspace());
    RELEASE(uShapeInfo, block.workspace());
    RELEASE(vShapeInfo, block.workspace());
    delete desc1;
    delete desc2;
    delete desc3;
    return result;
  }

  return SHAPELIST(ConstantShapeHelper::getInstance().createFromExisting(sShapeInfo, block.workspace()));
}

}  // namespace ops
}  // namespace sd

#endif
