/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BAMMergeOperator implements MergeOperator {

  private final MergeOperator wrappedMergeOp;
  private @Nullable BAMPCCManager bamPccManager;

  BAMMergeOperator(MergeOperator pWrappedMerge) {
    wrappedMergeOp = pWrappedMerge;
  }

  BAMMergeOperator withBAMPCCManager(BAMPCCManager pBAMPCCManager) {
    Preconditions.checkState(bamPccManager == null);
    bamPccManager = Preconditions.checkNotNull(pBAMPCCManager);
    return this;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {

    // do not merge at block starting states (initial states of a reached-set),
    // because we use this state to identify the block abstraction and the reached-set
    // in the BAMCache and other places.
    if (((ARGState) pState2).getParents().isEmpty()) {
      return pState2;
    }

    AbstractState out = wrappedMergeOp.merge(pState1, pState2, pPrecision);

    if (bamPccManager != null && bamPccManager.isPCCEnabled()) {
      return bamPccManager.attachAdditionalInfoToCallNode(out);
    }

    return out;
  }
}
