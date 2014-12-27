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
package org.sosy_lab.cpachecker.util.precondition.segkro.rules;

import static org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPatternBuilder.*;

import java.util.Collection;
import java.util.Map;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class ExistentialRule extends PatternBasedRule {

  public ExistentialRule(Solver pSolver, SmtAstMatcher pMatcher) {
    super(pSolver, pMatcher);
  }

  @Override
  protected void setupPatterns() {
    // ATTENTION:
    //  This definition is different from the Seghir/Kroenig paper!
    //  We had to swap i and j in the function application

    premises.add(new PatternBasedPremise(or(
        GenericPatterns.f_of_x("f", "j")
        )));

    premises.add(new PatternBasedPremise(or(
        matchBind("not", "nf",
            GenericPatterns.f_of_x("fx", "i"))
        )));
  }

  @Override
  protected boolean satisfiesConstraints(Map<String, Formula> pAssignment) throws SolverException, InterruptedException {
    final Formula i = pAssignment.get("i");
    final Formula j = pAssignment.get("j");

    assert i instanceof IntegerFormula;
    assert j instanceof IntegerFormula;

    BooleanFormula lt = ifm.lessThan((IntegerFormula) i, (IntegerFormula) j);

    return solver.isUnsat(fmv.makeNot(lt));
  }

  @Override
  protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {

    // There might be more than one valid (symbolic) assignment; this method gets called for every possible assignment

    final BooleanFormula f = (BooleanFormula) pAssignment.get("f");
    final BooleanFormula nf = (BooleanFormula) pAssignment.get("nf");
    final IntegerFormula i = (IntegerFormula) pAssignment.get("i");
    final IntegerFormula j = (IntegerFormula) pAssignment.get("j");

    final IntegerFormula x = ifm.makeVariable("x");

    Map<Formula, Formula> transformation = Maps.newHashMap();
    transformation.put(i, x);
    transformation.put(j, x);

    final BooleanFormula fPrime = matcher.substitute(f, transformation);
    final BooleanFormula nfPrime = matcher.substitute(nf, transformation);

    final BooleanFormula xConstraint =  bfm.and(
        ifm.greaterOrEquals(x, i),
        ifm.lessOrEquals(x, j));

    return Lists.newArrayList(
        qfm.exists(Lists.newArrayList(x), bfm.and(fPrime, xConstraint)),
        qfm.exists(Lists.newArrayList(x), bfm.and(nfPrime, xConstraint)));
  }

}
