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
package alearis.malebolge.cpa;

import alearis.malebolge.cpa.ProbabilisticState.Type;
import alearis.malebolge.cpa.util.BigRational;
import com.google.common.base.Preconditions;
import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class ProbabilisticStatistics implements Statistics {

  private final String name;

  private BigRational successProbability = BigRational.ZERO;
  private BigRational failureProbability = BigRational.ZERO;
  private BigRational greyProbability = BigRational.ZERO;
  private BigRational domainSize = BigRational.ONE;

  private int nSuccessPaths = 0;
  private int nFailurePaths = 0;
  private int nGreyPaths = 0;


  public ProbabilisticStatistics() {
    name = ProbabilisticCPA.class.getName();
  }

  public ProbabilisticStatistics(String name) {
    this.name = name;
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out)
        .spacer()
        .put("success probability", successProbability.div(domainSize).doubleValue())
        .beginLevel()
        .put("-", successProbability.div(domainSize))
        .endLevel()
        .put("failure probability", failureProbability.div(domainSize).doubleValue())
        .beginLevel()
        .put("-", failureProbability.div(domainSize))
        .endLevel()
        .put("grey probability", greyProbability.div(domainSize).doubleValue())
        .beginLevel()
        .put("-", greyProbability.div(domainSize))
        .endLevel()
        .put("domain size", domainSize.doubleValue())
        .beginLevel()
        .put("-", domainSize)
        .endLevel()
        .spacer()
        .put("#success paths", nSuccessPaths)
        .put("#failure paths", nFailurePaths)
        .put("#grey    paths", nGreyPaths);
  }

  public boolean repOk() {
    return successProbability.plus(failureProbability)
        .plus(greyProbability)
        .compareTo(BigRational.ONE) <= 0;
  }

  public void setGreyToRemainingUnexplored() {
    greyProbability = BigRational.ONE
        .minus(successProbability)
        .minus(failureProbability);
  }

  public void update(ProbabilisticState state) {
    Preconditions.checkArgument(state.getProbability().isPresent(),
        "State wasn't quantified yet!");
    Preconditions.checkArgument(state.getType() != Type.TRANSIENT,
        "Transient states shouldn't be tallied!");
    BigRational probability = state.getProbability().get();

    switch (state.getType()) {
      case SUCCESS:
        successProbability = successProbability.plus(probability);
        nSuccessPaths++;
        break;
      case FAILURE:
        failureProbability = failureProbability.plus(probability);
        nFailurePaths++;
        break;
      case GREY:
        greyProbability = greyProbability.plus(probability);
        nGreyPaths++;
        break;
      case TRANSIENT:
      default:
        throw new UnsupportedOperationException();
    }
    //more like postcondition
    Preconditions.checkState(repOk(), "Inconsistent probability totals");
  }


  @Override
  public @Nullable String getName() {
    return name;
  }
}
