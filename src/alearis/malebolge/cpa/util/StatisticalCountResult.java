/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package alearis.malebolge.cpa.util;

import com.google.common.base.Preconditions;

public class StatisticalCountResult implements CountResult {

  public static final StatisticalCountResult ZERO = new StatisticalCountResult(BigRational.ZERO);

  private final BigRational mean;
  private final BigRational variance;

  public StatisticalCountResult(BigRational mean, BigRational variance) {
    Preconditions.checkArgument(!variance.isNegative());
    Preconditions.checkArgument(!mean.isNegative());

    this.mean = mean;
    this.variance = variance;
  }

  public StatisticalCountResult(BigRational val) {
    Preconditions.checkArgument(!val.isNegative());
    this.mean = val;
    this.variance = BigRational.ZERO;
  }

  @Override
  public StatisticalCountResult multiply(BigRational val) {
    return new StatisticalCountResult(mean.mul(val), variance.mul(val).mul(val));
  }

  @Override
  public StatisticalCountResult multiply(CountResult count) {
    // qcoral approach
    if (this.variance.isPositive() || count.getVariance().isPositive()) {
      BigRational ex = this.getMean();
      BigRational ey = count.getMean();
      BigRational vx = this.getVariance();
      BigRational vy = count.getVariance();

      BigRational exy = ex.mul(ey);
      BigRational vxy = ex.pow(2).mul(vy).plus(ey.pow(2).mul(vx)).plus(vx.mul(vy));
      // can't find a way to take the square root of a BigRational

      return new StatisticalCountResult(exy, vxy);
    } else {
      return new StatisticalCountResult(this.getMean().mul(
          count.getMean()), BigRational.ZERO);
    }
  }

  @Override
  public StatisticalCountResult plus(CountResult other) {
    return new StatisticalCountResult(
        this.getMean().plus(other.getMean()),
        this.getVariance().plus(other.getVariance()));
  }

  @Override
  public BigRational getMean() {
    return mean;
  }

  @Override
  public BigRational getVariance() {
    return variance;
  }

  @Override
  public String toString() {
    return "StatisticalCountResult [mean="
        + mean + ", variance=" + variance + "]";
  }
}