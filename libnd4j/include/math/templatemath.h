/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * See the NOTICE file distributed with this work for additional
 *  * information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

/*
 * templatemath.h
 *
 *  Created on: Jan 1, 2016
 *      Author: agibsonccc
 */

#ifndef TEMPLATEMATH_H_
#define TEMPLATEMATH_H_

#include <system/common.h>
#include <math/platformmath.h>
#include <array/DataTypeUtils.h>

#define BFLOAT16_MAX_VALUE 32737.
#define HALF_MAX_VALUE 65504.
#define FLOAT_MAX_VALUE 3.4028235E38
#define DOUBLE_MAX_VALUE 1.7976931348623157E308
#define SD_FLOAT_MIN_NORMAL 1.17549435e-38

#ifndef M_E
#define M_E 2.718281828459
#endif

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

namespace sd {

    namespace math {
        template <typename T>
        SD_HOST_DEVICE inline T sd_abs(T value);

        template <typename T>
        SD_HOST_DEVICE inline void sd_swap(T& val1, T& val2);

        template <typename T>
        SD_HOST_DEVICE inline T sd_max(T val1, T val2);

        template <typename T>
        SD_HOST_DEVICE inline T sd_min(T val1, T val2);

        template <typename T>
        SD_HOST_DEVICE inline bool sd_eq(T val1, T val2, double eps);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_re(T val1, T val2);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_rint(T val1);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_copysign(T val1, T val2);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_softplus(T val);

        template <typename T>
        SD_HOST_DEVICE inline T sd_rotl(T val, T shift);

        template <typename T>
        SD_HOST_DEVICE inline T sd_rotr(T val, T shift);

//#ifndef __CUDACC__
        template <typename X, typename Y, typename Z>
        SD_HOST_DEVICE inline Z sd_dot(X* x, Y* y, int length);
//#endif

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_ceil(T val1);

        template <typename T>
        SD_HOST_DEVICE inline bool sd_isnan(T val1);

        template <typename T>
        SD_HOST_DEVICE inline bool sd_isinf(T val1);

        template <typename T>
        SD_HOST_DEVICE inline bool sd_isfin(T val1);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_cos(T val);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_cosh(T val);

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_exp(X val);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_floor(T val);

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_log(X val);

        template <typename X, typename Y, typename Z>
        SD_HOST_DEVICE inline Z sd_pow(X val, Y val2);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_round(T val);

        template <typename X, typename Y, typename Z>
        SD_HOST_DEVICE inline Z sd_remainder(X num, Y denom);

        template <typename X, typename Y, typename Z>
        SD_HOST_DEVICE inline Z sd_fmod(X num, Y denom);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_erf(T num);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_erfc(T num);

        SD_HOST_DEVICE inline int32_t floatToRawIntBits(float d) {
            union {
                float f;
                int32_t i;
            } tmp;
            tmp.f = d;
            return tmp.i;
        }

        SD_HOST_DEVICE inline float intBitsToFloat(int32_t i) {
            union {
                float f;
                int32_t i;
            } tmp;
            tmp.i = i;
            return tmp.f;
        }

        SD_HOST_DEVICE inline float mulsignf(float x, float y) {
            return intBitsToFloat(floatToRawIntBits(x) ^ (floatToRawIntBits(y) & (1 << 31)));
        }

        SD_HOST_DEVICE inline float copysignfk(float x, float y) {
            return intBitsToFloat((floatToRawIntBits(x) & ~(1 << 31)) ^ (floatToRawIntBits(y) & (1 << 31)));
        }

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_sigmoid(T val) {
            return (Z)1.0f / ((Z)1.0f + sd_exp<T, Z>(-val));
        }

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_elu(T val, T alpha) {
            if (val >= (T)0.f) return val;
            return static_cast<Z>(alpha) * (sd_exp<T, Z>(val) - static_cast<Z>(1.0f));
        }

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_leakyrelu(T val, T alpha) {
            if (val < (T)0.0f)
                return alpha * val;
            else
                return val;
        }

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_eluderivative(T val, T alpha) {
            if (val >= static_cast<T>(0.0f)) return static_cast<Z>(1.0f);
            return static_cast<Z>(alpha) * sd_exp<T, Z>(val);
            // return val >= 0.0 ? 1.0 : sd_exp(val);
        }

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_sin(T val);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_sinh(T val);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_softplus(T val) {
            return sd_log<T, Z>((Z)1.0f + sd_exp<T, Z>(val));
        }

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_softsign(T val) {
            return val / ((T)1.0f + sd::math::sd_abs<T>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_sqrt(X val);

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_tanh(X val);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_tan(T val);

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_atan2(X val1, X val2);

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_atan2(X val1, X val2) {
            return p_atan2<Z>(static_cast<Z>(val1), static_cast<Z>(val2));
        }

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_tan(T tval) {
            return p_tan<Z>(static_cast<Z>(tval));
        }

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_tanhderivative(T val) {
            Z tanh = sd_tanh<T, Z>(val);
            return (Z)1.0f - tanh * tanh;
        }
        template <typename T, typename Z>
        SD_HOST_DEVICE inline T sd_sigmoidderivative(T val) {
            Z sigmoid = sd_sigmoid<T, Z>(val);
            return sigmoid * ((Z)1.0f - sigmoid);
        }

        template <typename T, typename Z>
        SD_HOST_DEVICE inline T sd_softsignderivative(T val) {
            T y = (T)1.0f + sd_abs(val);
            return (Z)1.0f / (y * y);
        }

        template <typename T, typename Z>
        SD_HOST_DEVICE inline T sd_sgn(T val) {
            return val < (T)0.0f ? (Z)-1.0f : val > (T)0.0f ? (Z)1.0f : (Z)0.0f;
        }

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_sign(T val) {
            return sd_sgn<T, Z>(val);
        }

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_signum(T val) {
            return sd_sgn<T, Z>(val);
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_gamma(X a);

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_lgamma(X x);

//#ifndef __CUDACC__
/*
        template<>
        SD_HOST_DEVICE inline float16 sd_dot<float16>(float16 *x, float16 *y, int length) {
            float16 dot = (float16) 0.0f;

            // TODO: since we can't use simd on unions, we might use something else here.
            for(int e = 0; e < length; e++) {
                dot += x[e] * y[e];
            }

            return dot;
        }
        */

        template <typename X, typename Y, typename Z>
        SD_HOST_DEVICE inline Z sd_dot(X* x, Y* y, int length) {
            Z dot = (Z)0.0f;

            for (int e = 0; e < length; e++) {
                dot += static_cast<Z>(x[e]) * static_cast<Z>(y[e]);
            }

            return dot;
        }
//#endif

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_acos(T val);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_sech(T val);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_acosh(T val);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_asin(T val);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_asinh(T val);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_asinh(T val) {
            // Math.log(Math.sqrt(Math.pow(x, 2) + 1) + x)
            return sd_log<Z, Z>(sd_sqrt<Z, Z>(sd_pow<T, T, Z>(val, (T)2) + (Z)1.f) + (Z)val);
        }

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_atan(T val);

        template <typename T, typename Z>
        SD_HOST_DEVICE inline Z sd_atanh(T val);

        template <>
        SD_HOST_DEVICE inline float16 sd_abs<float16>(float16 value) {
#ifdef SD_NATIVE_HALFS
            if (value < (float16)0.f) {
    return float16(__hneg(value.data));
  } else
    return value;
#else
            return (float16)fabsf((float)value);
#endif
        }
        template <>
        SD_HOST_DEVICE inline bfloat16 sd_abs<bfloat16>(bfloat16 value) {
            return (bfloat16)fabsf((float)value);
        }
        template <>
        SD_HOST_DEVICE inline float sd_abs<float>(float value) {
            return fabsf(value);
        }

        template <>
        SD_HOST_DEVICE inline double sd_abs<double>(double value) {
            return fabs(value);
        }

        template <>
        SD_HOST_DEVICE inline int sd_abs<int>(int value) {
            return abs(value);
        }

        template <>
        SD_HOST_DEVICE inline sd::LongType sd_abs<sd::LongType>(sd::LongType value) {
            return llabs(value);
        }

        template <>
        SD_HOST_DEVICE inline bool sd_abs<bool>(bool value) {
            return value;
        }

        template <>
        SD_HOST_DEVICE inline uint8_t sd_abs<uint8_t>(uint8_t value) {
            return value;
        }

        template <>
        SD_HOST_DEVICE inline uint16_t sd_abs<uint16_t>(uint16_t value) {
            return value;
        }

        template <>
        SD_HOST_DEVICE inline uint32_t sd_abs<uint32_t>(uint32_t value) {
            return value;
        }

        template <>
        SD_HOST_DEVICE inline sd::UnsignedLong sd_abs<sd::UnsignedLong>(sd::UnsignedLong value) {
            return value;
        }

        template <>
        SD_HOST_DEVICE inline int8_t sd_abs<int8_t>(int8_t value) {
            return value < 0 ? -value : value;
        }

        template <>
        SD_HOST_DEVICE inline int16_t sd_abs<int16_t>(int16_t value) {
            return value < 0 ? -value : value;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isnan<float16>(float16 value) {
            return *(value.data.getXP()) == 0x7fffU;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isnan<bfloat16>(bfloat16 value) {
            return value == bfloat16::nan();  // 0x7fffU;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isnan<float>(float value) {
            return value != value;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isnan<double>(double value) {
            return value != value;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isnan<int>(int value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isnan<uint32_t>(uint32_t value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isnan<uint16_t>(uint16_t value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isnan<uint8_t>(uint8_t value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isnan<int16_t>(int16_t value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isnan<int8_t>(int8_t value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isnan<bool>(bool value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isnan<sd::LongType>(sd::LongType value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isnan<sd::UnsignedLong>(sd::UnsignedLong value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isinf<float16>(float16 value) {
            return value < (float16)-HALF_MAX_VALUE || value > (float16)HALF_MAX_VALUE;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isinf<bfloat16>(bfloat16 value) {
            return value < (bfloat16)-BFLOAT16_MAX_VALUE || value > (bfloat16)BFLOAT16_MAX_VALUE;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isinf<float>(float value) {
#ifdef __CUDACC__
            return isinf(value);
#else
            return std::isinf(value);
#endif
            // return value < -FLOAT_MAX_VALUE || value > FLOAT_MAX_VALUE;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isinf<double>(double value) {
#ifdef __CUDACC__
            return isinf(value);
#else
            return std::isinf(value);
#endif
            // return value < -DOUBLE_MAX_VALUE || value > DOUBLE_MAX_VALUE;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isinf<int>(int value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isinf<uint32_t>(uint32_t value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isinf<uint16_t>(uint16_t value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isinf<uint8_t>(uint8_t value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isinf<int16_t>(int16_t value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isinf<int8_t>(int8_t value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isinf<bool>(bool value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isinf<sd::LongType>(sd::LongType value) {
            return false;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_isinf<sd::UnsignedLong>(sd::UnsignedLong value) {
            return false;
        }

        template <typename T>
        SD_HOST_DEVICE inline bool sd_isfin(T value) {
            return !sd_isnan<T>(value) && !sd_isinf<T>(value);
        }

        template <>
        SD_HOST_DEVICE inline float16 sd_copysign<float16>(float16 val1, float16 val2) {
            return (float16)copysignf((float)val1, (float)val2);
        }

        template <>
        SD_HOST_DEVICE inline float sd_copysign<float>(float val1, float val2) {
            return copysignf(val1, val2);
        }

        template <>
        SD_HOST_DEVICE inline double sd_copysign<double>(double val1, double val2) {
            return copysign(val1, val2);
        }

        template <>
        SD_HOST_DEVICE inline int sd_copysign<int>(int val1, int val2) {
            if (val2 < 0)
                return -(sd_abs<int>(val1));
            else
                return sd_abs<int>(val1);
        }

        template <>
        SD_HOST_DEVICE inline sd::LongType sd_copysign<sd::LongType>(sd::LongType val1, sd::LongType val2) {
            if (val2 < 0)
                return -(sd_abs<sd::LongType>(val1));
            else
                return sd_abs<sd::LongType>(val1);
        }

        template <>
        SD_HOST_DEVICE inline bool sd_max(bool val1, bool val2) {
            return (val1 || val2) ? true : false;
        }

        template <typename T>
        SD_HOST_DEVICE inline T sd_max(T val1, T val2) {
            return val1 > val2 ? val1 : val2;
        }

        template <>
        SD_HOST_DEVICE inline bool sd_min(bool val1, bool val2) {
            return (val1 && val2) ? true : false;
        }

        template <typename T>
        SD_HOST_DEVICE inline T sd_min(T val1, T val2) {
            return val1 < val2 ? val1 : val2;
        }

        template <typename T>
        SD_HOST_DEVICE inline bool sd_eq(T d1, T d2, double eps) {
            if (sd::math::sd_isinf<T>(d1) && sd::math::sd_isinf<T>(d2)) {
                if (d1 > 0 && d2 > 0)
                    return true;
                else if (d1 < 0 && d2 < 0)
                    return true;
                else
                    return false;
            }

            auto diff = static_cast<double>(sd::math::sd_abs<T>(d1 - d2));
            // works well except in the range of very large numbers
            if (diff <= eps) return true;

            // Knuth approach
            // works well except in the range of very small numbers
            if (diff <= sd::math::sd_max<double>(sd::math::sd_abs<double>(static_cast<double>(d1)),
                                                 sd::math::sd_abs<double>(static_cast<double>(d2))) *
                        eps)
                return true;

            return false;
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_ceil(X val) {
            return static_cast<Z>(p_ceil<X>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_round(X val) {
            return static_cast<Z>(p_round<X>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_asin(X val) {
            return p_asin<Z>(static_cast<Z>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_atan(X val) {
            return p_atan<Z>(static_cast<Z>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_atanh(X val) {
            return p_atanh<Z>(static_cast<Z>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_cosh(X val) {
            return p_cosh<Z>(static_cast<Z>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_rint(X val) {
            return p_rint<X>(val);
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_sinh(X val) {
            return p_sinh<Z>(static_cast<Z>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_acos(X val) {
            return p_acos<Z>(static_cast<Z>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_sech(X val) {
            return static_cast<Z>(1) / sd_cosh<X, Z>(val);
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_acosh(X val) {
            return p_acosh<Z>(static_cast<Z>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_cos(X val) {
            return p_cos<Z>(static_cast<Z>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_exp(X val) {
            return p_exp<X>(val);
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_floor(X val) {
            return static_cast<Z>(p_floor<X>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_log(X val) {
            return static_cast<Z>(p_log<X>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_log2(X val) {
            return static_cast<Z>(p_log2<X>(val));
        }

/**
 * This func is special case - it must return floating point value, and optionally Y arg can be floating point argument
 * @tparam X
 * @tparam Y
 * @tparam Z
 * @param val
 * @param val2
 * @return
 */
        template <>
        SD_HOST_DEVICE inline float sd_pow(float val, float val2) {
            return p_pow<float>(val, val2);
        }

        template <typename X, typename Y, typename Z>
        SD_HOST_DEVICE inline Z sd_pow(X val, Y val2) {
            return p_pow<Z>(static_cast<Z>(val), static_cast<Z>(val2));
        }

/**
 * LogGamma(a) - float point extension of ln(n!)
 **/
        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_lgamma(X x) {
            //            if (x <= X(0.0))
            //            {
            //                std::stringstream os;
            //                os << "Logarithm of Gamma has sence only for positive values, but " << x <<  " was given.";
            //                throw std::invalid_argument( os.str() );
            //            }

            if (x < X(12.0)) {
                return sd_log<Z, Z>(sd_gamma<X, Z>(x));
            }

            // Abramowitz and Stegun 6.1.41
            // Asymptotic series should be good to at least 11 or 12 figures
            // For error analysis, see Whittiker and Watson
            // A Course in Modern Analysis (1927), page 252

            static const double c[8] = {1.0 / 12.0,   -1.0 / 360.0,      1.0 / 1260.0, -1.0 / 1680.0,
                                        1.0 / 1188.0, -691.0 / 360360.0, 1.0 / 156.0,  -3617.0 / 122400.0};

            double z = Z(1.0 / Z(x * x));
            double sum = c[7];

            for (int i = 6; i >= 0; i--) {
                sum *= z;
                sum += c[i];
            }

            double series = sum / Z(x);

            static const double halfLogTwoPi = 0.91893853320467274178032973640562;

            return Z((double(x) - 0.5) * sd_log<X, double>(x) - double(x) + halfLogTwoPi + series);
        }

        template <typename T>
        SD_HOST_DEVICE inline T sd_re(T val1, T val2) {
            if (val1 == (T)0.0f && val2 == (T)0.0f) return (T)0.0f;

            return sd_abs<T>(val1 - val2) / (sd_abs<T>(val1) + sd_abs<T>(val2));
        }

        template <typename X, typename Y, typename Z>
        SD_HOST_DEVICE inline Z sd_remainder(X val, Y val2) {
            return p_remainder<Z>(static_cast<Z>(val), static_cast<Z>(val2));
        }

        template <typename X, typename Y, typename Z>
        SD_HOST_DEVICE inline Z sd_fmod(X val, Y val2) {
            return p_fmod<Z>(static_cast<Z>(val), static_cast<Z>(val2));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_sin(X val) {
            return p_sin<Z>(static_cast<Z>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_sqrt(X val) {
            return p_sqrt<Z>(static_cast<Z>(val));
        }

        template <typename X>
        SD_HOST_DEVICE inline X neg_tanh(X val) {
            X o = static_cast<X>(1.0f);
            X t = static_cast<X>(2.0f);
            X e = static_cast<X>(M_E);

            auto p = sd::math::sd_pow<X, X, X>(e, val * t);
            return (p - o) / (p + o);
        }

        template <typename X>
        SD_HOST_DEVICE inline X pos_tanh(X val) {
            X o = static_cast<X>(1.0f);
            X t = static_cast<X>(-2.0f);
            X e = static_cast<X>(M_E);

            auto p = sd::math::sd_pow<X, X, X>(e, val * t);
            return (o - p) / (o + p);
        }

        SD_HOST_DEVICE inline float neu_tanh(float val, float sign) {
            float e(M_E);
            float av = sign * val;
            auto p = sd::math::sd_pow<float, float, float>(e, -av * 2.f);
            return (1 - p) / (1 + p);
        }

        template <>
        SD_HOST_DEVICE inline float sd_tanh(float val) {
            float sign = copysignfk(1.0f, val);
            return sign * neu_tanh(val, sign);
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_tanh(X val) {
            return val <= 0 ? neg_tanh(val) : pos_tanh(val);
        }

        template <typename T>
        SD_HOST_DEVICE inline T sd_rotl(T val, T shift) {
            return p_rotl<T>(val, shift);
        }

        template <typename T>
        SD_HOST_DEVICE inline T sd_rotr(T val, T shift) {
            return p_rotr<T>(val, shift);
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_erf(X val) {
            return p_erf<Z>(static_cast<Z>(val));
        }

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_erfc(X val) {
            return p_erfc<Z>(static_cast<Z>(val));
        }

        template <typename T>
        SD_HOST_DEVICE inline void sd_swap(T& val1, T& val2) {
            T temp = val1;
            val1 = val2;
            val2 = temp;
        };

        template <typename X, typename Z>
        SD_HOST_DEVICE inline Z sd_gamma(X a) {
            //            sd_lgamma<X,Z>(a);
            //            return (Z)std::tgamma(a);
            // Split the function domain into three intervals:
            // (0, 0.001), [0.001, 12), and (12, infinity)

            ///////////////////////////////////////////////////////////////////////////
            // First interval: (0, 0.001)
            //
            // For small a, 1/Gamma(a) has power series a + gamma a^2  - ...
            // So in this range, 1/Gamma(a) = a + gamma a^2 with error on the order of a^3.
            // The relative error over this interval is less than 6e-7.

            const double eulerGamma = 0.577215664901532860606512090;  // Euler's gamma constant

            if (a < X(0.001)) return Z(1.0 / ((double)a * (1.0 + eulerGamma * (double)a)));

            ///////////////////////////////////////////////////////////////////////////
            // Second interval: [0.001, 12)

            if (a < X(12.0)) {
                // The algorithm directly approximates gamma over (1,2) and uses
                // reduction identities to reduce other arguments to this interval.

                double y = (double)a;
                int n = 0;
                bool argWasLessThanOne = y < 1.0;

                // Add or subtract integers as necessary to bring y into (1,2)
                // Will correct for this below
                if (argWasLessThanOne) {
                    y += 1.0;
                } else {
                    n = static_cast<int>(floor(y)) - 1;  // will use n later
                    y -= n;
                }

                // numerator coefficients for approximation over the interval (1,2)
                static const double p[] = {-1.71618513886549492533811E+0, 2.47656508055759199108314E+1,
                                           -3.79804256470945635097577E+2, 6.29331155312818442661052E+2,
                                           8.66966202790413211295064E+2,  -3.14512729688483675254357E+4,
                                           -3.61444134186911729807069E+4, 6.64561438202405440627855E+4};

                // denominator coefficients for approximation over the interval (1,2)
                static const double q[] = {-3.08402300119738975254353E+1, 3.15350626979604161529144E+2,
                                           -1.01515636749021914166146E+3, -3.10777167157231109440444E+3,
                                           2.25381184209801510330112E+4,  4.75584627752788110767815E+3,
                                           -1.34659959864969306392456E+5, -1.15132259675553483497211E+5};

                double num = 0.0;
                double den = 1.0;

                double z = y - 1;
                for (auto i = 0; i < 8; i++) {
                    num = (num + p[i]) * z;
                    den = den * z + q[i];
                }
                double result = num / den + 1.0;

                // Apply correction if argument was not initially in (1,2)
                if (argWasLessThanOne) {
                    // Use identity gamma(z) = gamma(z+1)/z
                    // The variable "result" now holds gamma of the original y + 1
                    // Thus we use y-1 to get back the orginal y.
                    result /= (y - 1.0);
                } else {
                    // Use the identity gamma(z+n) = z*(z+1)* ... *(z+n-1)*gamma(z)
                    for (auto i = 0; i < n; i++) result *= y++;
                }

                return Z(result);
            }

            ///////////////////////////////////////////////////////////////////////////
            // Third interval: [12, infinity)

            if (a > 171.624) {
                // Correct answer too large to display. Force +infinity.
                return Z(DOUBLE_MAX_VALUE);
                //                return DataTypeUtils::infOrMax<Z>();
            }

            return sd::math::sd_exp<Z, Z>(sd::math::sd_lgamma<X, Z>(a));
        }

        template <typename X, typename Y, typename Z>
        SD_HOST_DEVICE inline Z sd_igamma(X a, Y x) {
            Z aim = sd_pow<X, X, Z>(x, a) / (sd_exp<X, Z>(x) * sd_gamma<Y, Z>(a));
            auto sum = Z(0.);
            auto denom = Z(1.);
            if (a <= X(0.000001))
                // throw std::runtime_error("Cannot calculate gamma for a zero val.");
                return Z(0);

            for (int i = 0; Z(1. / denom) > Z(1.0e-12); i++) {
                denom *= (a + i);
                sum += sd_pow<X, int, Z>(x, i) / denom;
            }
            return aim * sum;
        }

        template <typename X, typename Y, typename Z>
        SD_HOST_DEVICE inline Z sd_igammac(X a, Y x) {
            return Z(1.) - sd_igamma<X, Y, Z>(a, x);
        }

#ifdef __CUDACC__
        namespace atomics {
template <typename T>
inline SD_DEVICE T sd_atomicAdd(T* address, T val);

template <typename T>
inline SD_DEVICE T sd_atomicSub(T* address, T val);
template <typename T>
inline SD_DEVICE T sd_atomicMul(T* address, T val);
template <typename T>
inline SD_DEVICE T sd_atomicDiv(T* address, T val);

template <typename T>
inline SD_DEVICE T sd_atomicMin(T* address, T val);
template <typename T>
inline SD_DEVICE T sd_atomicMax(T* address, T val);

template <>
inline SD_DEVICE int32_t sd_atomicMin<int32_t>(int32_t* address, int32_t val) {
  return atomicMin(address, val);
}

template <>
inline SD_DEVICE uint32_t sd_atomicMin<uint32_t>(uint32_t* address, uint32_t val) {
  return atomicMin(address, val);
}
template <>
inline SD_DEVICE float sd_atomicMin<float>(float* address, float val) {
  int* address_as_ull = (int*)address;
  int old = __float_as_int(val), assumed;
  do {
    assumed = old;
    old = atomicCAS(address_as_ull, assumed, __float_as_int(math::sd_min(val, __int_as_float(assumed))));
  } while (assumed != old);
  return __int_as_float(old);
}
template <>
inline SD_DEVICE double sd_atomicMin<double>(double* address, double val) {
  unsigned long long int* address_as_ull = (unsigned long long int*)address;
  unsigned long long int old = __double_as_longlong(val), assumed;
  do {
    assumed = old;
    old = atomicCAS(address_as_ull, assumed, __double_as_longlong(math::sd_min(val, __longlong_as_double(assumed))));
  } while (assumed != old);
  return __longlong_as_double(old);
}
template <>
inline SD_DEVICE uint64_t sd_atomicMin<uint64_t>(uint64_t* address, uint64_t val) {
#if __CUDA_ARCH__ >= 350
  return atomicMin((unsigned long long*)address, (unsigned long long)val);
#else
  unsigned long long int* address_as_ull = (unsigned long long int*)address;
  unsigned long long int old = __double_as_longlong(val), assumed;
  do {
    assumed = old;
    old = atomicCAS(address_as_ull, assumed, math::sd_min((unsigned long long)val, assumed));
  } while (assumed != old);
  return old;
#endif
}
template <>
inline SD_DEVICE sd::LongType sd_atomicMin<sd::LongType>(sd::LongType* address, sd::LongType val) {
#if __CUDA_ARCH__ >= 350
  return atomicMin((unsigned long long*)address, (unsigned long long)val);
#else
  unsigned long long int* address_as_ull = (unsigned long long int*)address;
  unsigned long long int old = (unsigned long long)val, assumed;
  do {
    assumed = old;
    old = atomicCAS(address_as_ull, assumed, math::sd_min(val, (sd::LongType)assumed));
  } while (assumed != old);
  return old;
#endif
}
template <>
inline SD_DEVICE int16_t sd_atomicMin<int16_t>(int16_t* address, int16_t val) {
  int32_t temp = *address;
  *address = atomicMin(&temp, (int)val);
  return *address;
}
template <>
inline SD_DEVICE bfloat16 sd_atomicMin<bfloat16>(bfloat16* address, bfloat16 val) {
  return bfloat16(sd_atomicMin<int16_t>(&address->_data, val._data));
}
template <>
inline SD_DEVICE float16 sd_atomicMin<float16>(float16* address, float16 val) {
  return float16(sd_atomicMin<int16_t>(reinterpret_cast<int16_t*>(&address->data), (int16_t)val.data));
}
template <>
inline SD_DEVICE int32_t sd_atomicMax<int32_t>(int32_t* address, int32_t val) {
  return atomicMax(address, val);
}

template <>
inline SD_DEVICE uint32_t sd_atomicMax<uint32_t>(uint32_t* address, uint32_t val) {
  return atomicMax(address, val);
}

template <>
inline SD_DEVICE double sd_atomicMax<double>(double* address, double val) {
  unsigned long long int* address_as_ull = (unsigned long long int*)address;
  unsigned long long int old = __double_as_longlong(val), assumed;
  do {
    assumed = old;
    old = atomicCAS(address_as_ull, assumed, __double_as_longlong(math::sd_max(val, __longlong_as_double(assumed))));
  } while (assumed != old);
  return __longlong_as_double(old);
}
template <>
inline SD_DEVICE float sd_atomicMax<float>(float* address, float val) {
  int* address_as_ull = (int*)address;
  int old = __float_as_int(val), assumed;
  do {
    assumed = old;
    old = atomicCAS(address_as_ull, assumed, __float_as_int(math::sd_max(val, __int_as_float(assumed))));
  } while (assumed != old);
  return __int_as_float(old);
}
template <>
inline SD_DEVICE uint8_t sd_atomicMin<uint8_t>(uint8_t* address, uint8_t val) {
  uint32_t temp = *address;
  *address = atomicMin(&temp, (uint32_t)val);
  return *address;
}

template <>
inline SD_DEVICE int8_t sd_atomicMin<int8_t>(int8_t* address, int8_t val) {
  int32_t temp = *address;
  *address = atomicMin(&temp, (int)val);
  return *address;
}

template <>
inline SD_DEVICE uint16_t sd_atomicMin<uint16_t>(uint16_t* address, uint16_t val) {
  uint32_t temp = *address;
  *address = atomicMin(&temp, (uint32_t)val);
  return *address;
}

template <>
inline SD_DEVICE uint8_t sd_atomicMax<uint8_t>(uint8_t* address, uint8_t val) {
  uint32_t temp = *address;
  *address = atomicMax(&temp, (uint32_t)val);
  return *address;
}

template <>
inline SD_DEVICE int8_t sd_atomicMax<int8_t>(int8_t* address, int8_t val) {
  int32_t temp = *address;
  *address = atomicMax(&temp, (int)val);
  return *address;
}

template <>
inline SD_DEVICE uint16_t sd_atomicMax<uint16_t>(uint16_t* address, uint16_t val) {
  uint32_t temp = *address;
  *address = atomicMax(&temp, (uint32_t)val);
  return *address;
}

template <>
inline SD_DEVICE int16_t sd_atomicMax<int16_t>(int16_t* address, int16_t val) {
  int32_t temp = *address;
  *address = atomicMax(&temp, (int32_t)val);
  return *address;
}

template <>
inline SD_DEVICE float16 sd_atomicMax<float16>(float16* address, float16 val) {
  auto address_as_ull = (int*)address;

  long addr = (long)address;
  bool misaligned = addr & 0x3;

  if (misaligned) address_as_ull = (int*)(address - 1);

  PAIR old, assumed, fresh;

  old.W = *address_as_ull;
  do {
    if (!misaligned) {
      float16 res = sd_max((float16)old.B.H, val);
      fresh.B.H = res.data;
      fresh.B.L = old.B.L;
    } else {
      float16 res = sd_max((float16)old.B.L, val);
      fresh.B.L = res.data;
      fresh.B.H = old.B.H;
    }

    assumed.W = old.W;
    old.W = atomicCAS(address_as_ull, assumed.W, fresh.W);
  } while (assumed.W != old.W);

  if (!misaligned)
    return old.B.H;
  else
    return old.B.L;
}

template <>
inline SD_DEVICE bfloat16 sd_atomicMax<bfloat16>(bfloat16* address, bfloat16 val) {
  auto address_as_ull = (int*)address;

  long addr = (long)(address);
  bool misaligned = addr & 0x3;

  if (misaligned) address_as_ull = (int*)(address - 1);

  BPAIR old, assumed, fresh;

  old.W = *address_as_ull;
  do {
    if (!misaligned) {
      bfloat16 res = sd_max(old.B.H, val);
      fresh.B.H = res;
      fresh.B.L = old.B.L;
    } else {
      bfloat16 res = sd_max(old.B.L, val);
      fresh.B.L = res;
      fresh.B.H = old.B.H;
    }

    assumed.W = old.W;
    old.W = atomicCAS(address_as_ull, assumed.W, fresh.W);
  } while (assumed.W != old.W);

  if (!misaligned)
    return old.B.H;
  else
    return old.B.L;
}

template <>
inline SD_DEVICE uint64_t sd_atomicMax<uint64_t>(uint64_t* address, uint64_t val) {
#if __CUDA_ARCH__ >= 350
  return atomicMax((unsigned long long*)address, (unsigned long long)val);
#else
  unsigned long long int* address_as_ull = (unsigned long long int*)address;
  unsigned long long int old = __double_as_longlong(val), assumed;
  do {
    assumed = old;
    old = atomicCAS(address_as_ull, assumed, math::sd_max((unsigned long long)val, assumed));
  } while (assumed != old);
  return old;
#endif
}

template <>
inline SD_DEVICE sd::LongType sd_atomicMax<sd::LongType>(sd::LongType* address, sd::LongType val) {
  unsigned long long int* address_as_ull = (unsigned long long int*)address;

  // return (sd::LongType) atomicAdd(address_as_ull, (unsigned long long int) val);
  unsigned long long int old = *address_as_ull, assumed;
  do {
    assumed = old;
    old = atomicCAS(address_as_ull, assumed, (unsigned long long)sd_max(val, (sd::LongType)assumed));
  } while (assumed != old);
  return old;
}

template <>
inline SD_DEVICE double sd_atomicAdd<double>(double* address, double val) {
  unsigned long long int* address_as_ull = (unsigned long long int*)address;
  unsigned long long int old = *address_as_ull, assumed;
  do {
    assumed = old;
    old = atomicCAS(address_as_ull, assumed, __double_as_longlong(val + __longlong_as_double(assumed)));
  } while (assumed != old);
  return __longlong_as_double(old);
}

template <>
inline SD_DEVICE sd::LongType sd_atomicAdd<sd::LongType>(sd::LongType* address, sd::LongType val) {
  unsigned long long int* address_as_ull = (unsigned long long int*)address;

  // return (sd::LongType) atomicAdd(address_as_ull, (unsigned long long int) val);
  unsigned long long int old = *address_as_ull, assumed;
  do {
    assumed = old;
    old = atomicCAS(address_as_ull, assumed, val + assumed);
  } while (assumed != old);
  return old;
}

template <>
inline SD_DEVICE long sd_atomicAdd<long>(long* address, long val) {
  unsigned long long* address_as_ull = (unsigned long long int*)address;

  //    return atomicAdd(address, val);
  unsigned long int old = *address_as_ull, assumed;
  do {
    assumed = old;
    old = atomicCAS(address_as_ull, assumed, val + assumed);
  } while (assumed != old);
  return old;
}

template <>
inline SD_DEVICE uint32_t sd_atomicAdd<uint32_t>(uint32_t* address, uint32_t val) {
  return atomicAdd(address, val);
}

template <>
inline SD_DEVICE uint64_t sd_atomicAdd<uint64_t>(uint64_t* address, uint64_t val) {
  //    unsigned long long* address_as_ull = (unsigned long long int *) address;
  //
  ////    return atomicAdd(address, val);
  //    unsigned long int old = *address_as_ull, assumed;
  //    do {
  //        assumed = old;
  //        old = atomicCAS(address_as_ull, assumed, val + assumed);
  //    } while (assumed != old);
  //    return old;
  return (uint64_t)atomicAdd((unsigned long long*)address, (unsigned long long)val);
}

template <>
inline SD_DEVICE float16 sd_atomicAdd<float16>(float16* address, float16 val) {
#if __CUDA_ARCH__ >= 700 && CUDA_VERSION_MAJOR >= 10
  atomicAdd(reinterpret_cast<__half*>(address), val.data);
#else
  auto address_as_ull = (int*)address;

  long addr = (long)address;
  bool misaligned = addr & 0x3;

  if (misaligned) address_as_ull = (int*)(address - 1);

  PAIR old, assumed, fresh;

  old.W = *address_as_ull;
  do {
    if (!misaligned) {
      float16 res = ((float16)old.B.H) + val;
      fresh.B.H = res.data;
      fresh.B.L = old.B.L;
    } else {
      float16 res = ((float16)old.B.L) + val;
      fresh.B.L = res.data;
      fresh.B.H = old.B.H;
    }

    assumed.W = old.W;
    old.W = atomicCAS(address_as_ull, assumed.W, fresh.W);
  } while (assumed.W != old.W);

  if (!misaligned)
    return old.B.H;
  else
    return old.B.L;
#endif
}

template <>
inline SD_DEVICE bfloat16 sd_atomicAdd<bfloat16>(bfloat16* address, bfloat16 val) {
  auto address_as_ull = (int*)address;

  auto addr = (long)(address);
  bool misaligned = addr & 0x3;

  if (misaligned) address_as_ull = (int*)(address - 1);

  BPAIR old, assumed, fresh;

  old.W = *address_as_ull;
  do {
    if (!misaligned) {
      bfloat16 res = old.B.H + val;
      fresh.B.H = res;
      fresh.B.L = old.B.L;
    } else {
      bfloat16 res = old.B.L + val;
      fresh.B.L = res;
      fresh.B.H = old.B.H;
    }

    assumed.W = old.W;
    old.W = atomicCAS(address_as_ull, assumed.W, fresh.W);
  } while (assumed.W != old.W);

  if (!misaligned)
    return old.B.H;
  else
    return old.B.L;
}

template <typename T>
static SD_INLINE SD_DEVICE T internal_16bit_atomicAdd(T* address, T val) {
  size_t shift = ((size_t)address & 2);
  int* base_address = (int*)((char*)address - shift);

  union I16PAIR {
    struct {
      T H;
      T L;
    } B;
    int W;

    SD_HOST_DEVICE
    I16PAIR(){};

    SD_HOST_DEVICE
    ~I16PAIR(){};
  };

  I16PAIR pairNew, pairOld, pairAssumed;

  if (reinterpret_cast<int*>(address) == base_address) {
    pairOld.B.L = val;
    do {
      pairNew.B.L = pairOld.B.L;
      pairNew.B.H = pairOld.B.H + val;
      pairAssumed.W = pairOld.W;

      pairOld.W = atomicCAS(base_address, pairAssumed.W, pairNew.W);
    } while (pairAssumed.W != pairOld.W);

    return (T)pairOld.B.H;
  } else {
    pairOld.B.H = val;
    do {
      pairNew.B.H = pairOld.B.H;
      pairNew.B.L = pairOld.B.L + val;
      pairAssumed.W = pairOld.W;
      pairOld.W = atomicCAS(base_address, pairAssumed.W, pairNew.W);

    } while (pairAssumed.W != pairOld.W);

    return (T)pairOld.B.L;
  }
}

template <>
inline SD_DEVICE int16_t sd_atomicAdd<int16_t>(int16_t* address, int16_t val) {
  return internal_16bit_atomicAdd<int16_t>(address, val);
}

template <>
inline SD_DEVICE uint16_t sd_atomicAdd<uint16_t>(uint16_t* address, uint16_t val) {
  return internal_16bit_atomicAdd<uint16_t>(address, val);
}

template <>
inline SD_DEVICE int8_t sd_atomicAdd<int8_t>(int8_t* address, int8_t val) {
  int res = *address;
  atomicAdd(&res, (int)val);
  *address = res;
  return *address;
}

template <>
inline SD_DEVICE uint8_t sd_atomicAdd<uint8_t>(uint8_t* address, uint8_t val) {
  int res = *address;
  atomicAdd(&res, (int)val);
  *address = res;
  return *address;
}

template <>
inline SD_DEVICE bool sd_atomicAdd<bool>(bool* address, bool val) {
  *address += (val);
  return *address;
}

template <>
inline SD_DEVICE double sd_atomicSub<double>(double* address, double val) {
  return sd_atomicAdd<double>(address, -val);
}

template <>
inline SD_DEVICE double sd_atomicMul<double>(double* address, double val) {
  unsigned long long int* address_as_ull = (unsigned long long int*)address;
  unsigned long long int old = *address_as_ull, assumed;
  do {
    assumed = old;
    old = atomicCAS(address_as_ull, assumed, __double_as_longlong(val * __longlong_as_double(assumed)));
  } while (assumed != old);
  return __longlong_as_double(old);
}

template <>
inline SD_DEVICE double sd_atomicDiv<double>(double* address, double val) {
  return sd_atomicMul<double>(address, 1. / val);
}

template <>
inline SD_DEVICE float sd_atomicAdd<float>(float* address, float val) {
  return atomicAdd(address, val);
}
// template <>
// inline SD_DEVICE int sd_atomicAdd<int>(int* address, int val)  {
//    return atomicAdd(address, val);
//}
template <>
inline SD_DEVICE int32_t sd_atomicAdd<int32_t>(int32_t* address, int32_t val) {
  return (int32_t)atomicAdd((int*)address, (int)val);
}

template <>
inline SD_DEVICE float sd_atomicSub<float>(float* address, float val) {
  return sd_atomicAdd<float>(address, -val);
}

template <>
inline SD_DEVICE float16 sd_atomicSub<float16>(float16* address, float16 val) {
  return sd_atomicAdd<float16>(address, -val);
}
template <>
inline SD_DEVICE bfloat16 sd_atomicSub<bfloat16>(bfloat16* address, bfloat16 val) {
  return sd_atomicAdd<bfloat16>(address, -val);
}

template <>
inline SD_DEVICE float sd_atomicMul<float>(float* address, float val) {
  int* address_as_ull = (int*)address;
  int old = *address_as_ull, assumed;
  do {
    assumed = old;
    old = atomicCAS(address_as_ull, assumed, __float_as_int(val * __int_as_float(assumed)));
  } while (assumed != old);
  return __int_as_float(old);
}

template <>
inline SD_DEVICE int8_t sd_atomicMul<int8_t>(int8_t* address, int8_t val) {
  unsigned int* base_address = (unsigned int*)((size_t)address & ~3);
  unsigned int selectors[] = {0x3214, 0x3240, 0x3410, 0x4210};
  unsigned int sel = selectors[(size_t)address & 3];
  unsigned int old, assumed, mul, new_;

  old = *base_address;

  do {
    assumed = old;
    mul = val * (int8_t)__byte_perm(old, 0, ((size_t)address & 3) | 0x4440);
    new_ = __byte_perm(old, mul, sel);

    if (new_ == old) break;

    old = atomicCAS(base_address, assumed, new_);
  } while (assumed != old);
  return (int8_t)old;
}

template <>
inline SD_DEVICE unsigned char sd_atomicMul<unsigned char>(unsigned char* address, unsigned char val) {
  unsigned int* base_address = (unsigned int*)((size_t)address & ~3);
  unsigned int selectors[] = {0x3214, 0x3240, 0x3410, 0x4210};
  unsigned int sel = selectors[(size_t)address & 3];
  unsigned int old, assumed, mul, new_;

  old = *base_address;

  do {
    assumed = old;
    mul = val * (uint8_t)__byte_perm(old, 0, ((size_t)address & 3) | 0x4440);
    new_ = __byte_perm(old, mul, sel);

    if (new_ == old) break;

    old = atomicCAS(base_address, assumed, new_);
  } while (assumed != old);
  return (uint8_t)old;
}

template <typename T>
static SD_INLINE SD_DEVICE T internal_16bit_atomicMul(T* address, T val) {
  size_t shift = ((size_t)address & 2);
  int* base_address = (int*)((char*)address - shift);

  union I16PAIR {
    struct {
      T H;
      T L;
    } B;
    int W;

    SD_HOST_DEVICE
    I16PAIR(){};

    SD_HOST_DEVICE
    ~I16PAIR(){};
  };

  I16PAIR pairNew, pairOld, pairAssumed;

  if (reinterpret_cast<int*>(address) == base_address) {
    pairOld.B.L = val;
    do {
      pairNew.B.L = pairOld.B.L;
      pairNew.B.H = pairOld.B.H * val;
      pairAssumed.W = pairOld.W;

      pairOld.W = atomicCAS(base_address, pairAssumed.W, pairNew.W);
    } while (pairAssumed.W != pairOld.W);

    return (T)pairOld.B.H;
  } else {
    pairOld.B.H = val;
    do {
      pairNew.B.H = pairOld.B.H;
      pairNew.B.L = pairOld.B.L * val;
      pairAssumed.W = pairOld.W;
      pairOld.W = atomicCAS(base_address, pairAssumed.W, pairNew.W);

    } while (pairAssumed.W != pairOld.W);

    return (T)pairOld.B.L;
  }
}

template <>
inline SD_DEVICE int16_t sd_atomicMul<int16_t>(int16_t* address, int16_t val) {
  return internal_16bit_atomicMul<int16_t>(address, val);
}

template <>
inline SD_DEVICE uint16_t sd_atomicMul<uint16_t>(uint16_t* address, uint16_t val) {
  return internal_16bit_atomicMul<uint16_t>(address, val);
}

template <>
inline SD_DEVICE int sd_atomicMul<int>(int* address, int val) {
  int* res_address = address;
  int old = *res_address, assumed;
  do {
    assumed = old;
    old = atomicCAS(res_address, assumed, val * assumed);
  } while (assumed != old);
  return old;
}

template <>
inline SD_DEVICE unsigned int sd_atomicMul<unsigned int>(unsigned int* address, unsigned int val) {
  unsigned int* res_address = address;
  unsigned int old = *res_address, assumed;
  do {
    assumed = old;
    old = atomicCAS(res_address, assumed, val * assumed);
  } while (assumed != old);
  return old;
}

template <>
inline SD_DEVICE int64_t sd_atomicMul<int64_t>(int64_t* address, int64_t val) {
  unsigned long long int* res_address = (unsigned long long int*)address;
  unsigned long long int old = *res_address, assumed;
  do {
    assumed = old;
    old = atomicCAS(res_address, assumed, val * assumed);
  } while (assumed != old);
  return (int64_t)old;
}

template <>
inline SD_DEVICE uint64_t sd_atomicMul<uint64_t>(uint64_t* address, uint64_t val) {
  unsigned long long int* res_address = (unsigned long long int*)address;
  unsigned long long int old = *res_address, assumed;
  do {
    assumed = old;
    old = atomicCAS(res_address, assumed, val * assumed);
  } while (assumed != old);
  return (uint64_t)old;
}

#if !defined(_WIN32) && !defined(_WIN64)
template <>
inline SD_DEVICE sd::LongType sd_atomicMul<sd::LongType>(sd::LongType* address, sd::LongType val) {
  unsigned long long int* res_address = (unsigned long long*)address;
  unsigned long long int old = *res_address, assumed;
  do {
    assumed = old;
    old = atomicCAS(res_address, assumed, val * assumed);
  } while (assumed != old);
  return (sd::LongType)old;
}
#endif

template <>
inline SD_DEVICE bfloat16 sd_atomicMul<bfloat16>(bfloat16* address, bfloat16 val) {
  return internal_16bit_atomicMul<bfloat16>(address, val);
}

template <>
inline SD_DEVICE float16 sd_atomicMul<float16>(float16* address, float16 val) {
  return internal_16bit_atomicMul<float16>(address, val);
}

template <>
inline SD_DEVICE float sd_atomicDiv<float>(float* address, float val) {
  return sd_atomicMul<float>(address, 1.f / val);
}

template <>
inline SD_DEVICE float16 sd_atomicDiv<float16>(float16* address, float16 val) {
  return internal_16bit_atomicMul<float16>(address, (float16)1.f / val);
}

template <>
inline SD_DEVICE bfloat16 sd_atomicDiv<bfloat16>(bfloat16* address, bfloat16 val) {
  return internal_16bit_atomicMul<bfloat16>(address, (bfloat16)1 / val);
}
}  // namespace atomics
#endif
    }  // namespace math
}  // namespace sd

#ifdef _OPENMP

#ifndef SD_MAX_FLOAT
#define SD_MAX_FLOAT 1e37
#endif

#pragma omp declare reduction(maxTF                              \
                              : float, double, float16, bfloat16 \
                              : omp_out = sd::math::sd_max(omp_in, omp_out)) initializer(omp_priv = -SD_MAX_FLOAT)

#pragma omp declare reduction(minTF                              \
                              : float, double, float16, bfloat16 \
                              : omp_out = sd::math::sd_min(omp_in, omp_out)) initializer(omp_priv = SD_MAX_FLOAT)

#pragma omp declare reduction(maxT                                                                             \
                              : float, double, float16, bfloat16, int, sd::LongType, sd::UnsignedLong, int8_t, \
                                uint8_t, bool, int16_t, uint16_t, uint32_t                                     \
                              : omp_out = sd::math::sd_max(omp_in, omp_out)) initializer(omp_priv = 0)

#pragma omp declare reduction(minT                                                                             \
                              : float, double, float16, bfloat16, int, sd::LongType, sd::UnsignedLong, int8_t, \
                                uint8_t, bool, int16_t, uint16_t, uint32_t                                     \
                              : omp_out = sd::math::sd_min(omp_in, omp_out)) initializer(omp_priv = 0)

#pragma omp declare reduction(amaxT                                                                            \
                              : float, double, float16, bfloat16, int, sd::LongType, sd::UnsignedLong, int8_t, \
                                uint8_t, bool, int16_t, uint16_t, uint32_t                                     \
                              : omp_out = sd::math::sd_max(sd::math::sd_abs(omp_in), sd::math::sd_abs(omp_out)))

#pragma omp declare reduction(aminT                                                                            \
                              : float, double, float16, bfloat16, int, sd::LongType, sd::UnsignedLong, int8_t, \
                                uint8_t, bool, int16_t, uint16_t, uint32_t                                     \
                              : omp_out = sd::math::sd_min(sd::math::sd_abs(omp_in), sd::math::sd_abs(omp_out)))

#pragma omp declare reduction(asumT                                                                            \
                              : float, double, float16, bfloat16, int, sd::LongType, sd::UnsignedLong, int8_t, \
                                uint8_t, bool, int16_t, uint16_t, uint32_t                                     \
                              : omp_out = sd::math::sd_abs(omp_in) + sd::math::sd_abs(omp_out))                \
    initializer(omp_priv = 0)

#pragma omp declare reduction(sumT                                                                             \
                              : float, double, float16, bfloat16, int, sd::LongType, sd::UnsignedLong, int8_t, \
                                uint8_t, bool, int16_t, uint16_t, uint32_t                                     \
                              : omp_out = omp_in + omp_out) initializer(omp_priv = 0)

#pragma omp declare reduction(prodT                                                                            \
                              : float, double, float16, bfloat16, int, sd::LongType, sd::UnsignedLong, int8_t, \
                                uint8_t, bool, int16_t, uint16_t, uint32_t                                     \
                              : omp_out = omp_in * omp_out) initializer(omp_priv = 1)

#endif

#endif /* TEMPLATEMATH_H_ */