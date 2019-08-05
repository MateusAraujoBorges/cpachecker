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

import alearis.malebolge.cpa.util.BigRational;
import com.google.common.base.Preconditions;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class ProbabilisticState extends AbstractSingleWrapperState {

  public enum Type {
    TRANSIENT,
    SUCCESS,
    FAILURE,
    GREY
  }

  private Optional<BigRational> probability;
  private Type type = Type.TRANSIENT;
  private final int depth;

  public ProbabilisticState(@Nullable AbstractState pWrappedState, int depth) {
    super(pWrappedState);
    this.depth = depth;
    this.probability = Optional.empty();
  }

  public Type getType() {
    return type;
  }

  public void setType(Type pType) {
    type = pType;
  }

  public Optional<BigRational> getProbability() {
    return probability;
  }

  public void setProbability(BigRational probability) {
    Preconditions.checkState(!this.probability.isPresent(),
        "Attempting to overwrite existing probability");
    this.probability = Optional.of(probability);
  }

  public int getDepth() {
    return depth;
  }

  @Override
  public String toString() {
    return "Probabilistic State (type: " + type + " depth: " + depth
        + " prob: " + (probability.isPresent() ? probability.get() : "-") + ") \n"
        + getWrappedState().toString();
  }
}
