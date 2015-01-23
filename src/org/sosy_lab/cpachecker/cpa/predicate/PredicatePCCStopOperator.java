/*
i *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

public class PredicatePCCStopOperator implements StopOperator {

  private final PredicateAbstractionManager paMgr;
  private final FormulaManagerView fMgr;

  private final AbstractionFormula trueAbs;

  public PredicatePCCStopOperator(final PredicateCPA pCPA, final Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    paMgr = pCPA.getPredicateManager();
    fMgr = GlobalInfo.getInstance().getFormulaManager();

    trueAbs = paMgr.makeTrueAbstractionFormula(null);
  }

  @Override
  public boolean stop(AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    PredicateAbstractState e1 = (PredicateAbstractState) pState;

    for (AbstractState reachedState : pReached) {
      PredicateAbstractState e2 = (PredicateAbstractState) reachedState;

      if (isCoveredBy(e1, e2)) { return true; }

    }
    return false;

  }

  private boolean isCoveredBy(final PredicateAbstractState e1, final PredicateAbstractState e2) throws CPAException,
      InterruptedException {
    if (e1.isAbstractionState() && e2.isAbstractionState()) {
      return paMgr.checkCoverage(e1.getAbstractionFormula(), e2.getAbstractionFormula());

    } else if (e2.isAbstractionState()) {
        return paMgr.checkCoverage(e1.getAbstractionFormula(), e1.getPathFormula(), e2.getAbstractionFormula());

    } else if (e1.isAbstractionState()) {
      return false;

    } else {
      if (e1.getAbstractionFormula() == e2.getAbstractionFormula()) {
        PathFormula pF = e1.getPathFormula();
        BooleanFormula bF = e2.getPathFormula().getFormula();
        bF = fMgr.makeNot(bF);
        bF = fMgr.makeAnd(e1.getPathFormula().getFormula(), bF);
        return paMgr.unsat(trueAbs, new PathFormula(bF, pF.getSsa(), pF.getPointerTargetSet(), pF.getLength()));
      }
      return false;
    }
  }

}
