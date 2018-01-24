/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public abstract class SingleLocationFormulaInvariant implements CandidateInvariant {

  private final CFANode location;

  public SingleLocationFormulaInvariant(CFANode pLocation) {
    Preconditions.checkNotNull(pLocation);
    this.location = pLocation;
  }

  public final CFANode getLocation() {
    return location;
  }

  @Override
  public boolean appliesTo(CFANode pLocation) {
    return location.equals(pLocation);
  }

  @Override
  public final BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR)
      throws CPATransferException, InterruptedException {
    Iterable<AbstractState> locationStates = filterApplicable(pReachedSet);
    return BMCHelper.assertAt(locationStates, this, pFMGR, pPFMGR);
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    // Do nothing
  }

  @Override
  public Iterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates) {
    return AbstractStates.filterLocation(pStates, location);
  }

  public SingleLocationFormulaInvariant negate() {
    return new SingleLocationFormulaInvariant(location) {

      @Override
      public BooleanFormula getFormula(
          FormulaManagerView pFMGR, PathFormulaManager pPFMGR, @Nullable PathFormula pContext)
          throws CPATransferException, InterruptedException {
        return pFMGR
            .getBooleanFormulaManager()
            .not(SingleLocationFormulaInvariant.this.getFormula(pFMGR, pPFMGR, pContext));
      }

      @Override
      public SingleLocationFormulaInvariant negate() {
        return SingleLocationFormulaInvariant.this;
      }

      @Override
      public void assumeTruth(ReachedSet pReachedSet) {
        // Do nothing
      }

      @Override
      public String toString() {
        return "!" + SingleLocationFormulaInvariant.this;
      }
    };
  }

  public static SingleLocationFormulaInvariant makeBooleanInvariant(
      CFANode pLocation, final boolean pValue) {
    class SingleLocationBooleanInvariant extends SingleLocationFormulaInvariant {

      private SingleLocationBooleanInvariant() {
        super(pLocation);
      }

      private final boolean value = pValue;

      @Override
      public BooleanFormula getFormula(
          FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
          throws CPATransferException, InterruptedException {
        return pFMGR.getBooleanFormulaManager().makeBoolean(value);
      }

      @Override
      public SingleLocationFormulaInvariant negate() {
        return makeBooleanInvariant(pLocation, !pValue);
      }

      @Override
      public boolean equals(Object pOther) {
        if (this == pOther) {
          return true;
        }
        if (pOther instanceof SingleLocationBooleanInvariant) {
          SingleLocationBooleanInvariant other = (SingleLocationBooleanInvariant) pOther;
          return getLocation().equals(other.getLocation()) && value == other.value;
        }
        return false;
      }

      @Override
      public int hashCode() {
        return Objects.hash(pLocation, value);
      }

      @Override
      public String toString() {
        return Boolean.toString(value) + " at " + getLocation();
      }
    }
    return new SingleLocationBooleanInvariant();
  }

  public static SingleLocationFormulaInvariant makeLocationInvariant(
      CFANode pLocation, BooleanFormula pInvariant, FormulaManagerView pOriginalFormulaManager) {
    BooleanFormulaManager bfmgr = pOriginalFormulaManager.getBooleanFormulaManager();
    if (bfmgr.isTrue(pInvariant)) {
      return SingleLocationFormulaInvariant.makeBooleanInvariant(pLocation, true);
    }
    if (bfmgr.isFalse(pInvariant)) {
      return SingleLocationFormulaInvariant.makeBooleanInvariant(pLocation, false);
    }
    class SpecificSMTLibLocationFormulaInvariant extends SingleLocationFormulaInvariant {

      private final BooleanFormula invariant;

      private final SMTLibLocationFormulaInvariant delegate;

      public SpecificSMTLibLocationFormulaInvariant(BooleanFormula pInv) {
        super(pLocation);
        invariant = pInv;
        delegate =
            new SMTLibLocationFormulaInvariant(
                pLocation, pOriginalFormulaManager.dumpFormula(pInv).toString());
      }

      @Override
      public BooleanFormula getFormula(
          FormulaManagerView pFMGR, PathFormulaManager pPFMGR, @Nullable PathFormula pContext)
          throws CPATransferException, InterruptedException {
        return delegate.getFormula(pFMGR, pPFMGR, pContext);
      }

      @Override
      public void assumeTruth(ReachedSet pReachedSet) {
        delegate.assumeTruth(pReachedSet);
      }

      @Override
      public String toString() {
        return invariant + " at " + getLocation();
      }

      @Override
      public boolean equals(Object pOther) {
        if (this == pOther) {
          return true;
        }
        if (pOther instanceof SpecificSMTLibLocationFormulaInvariant) {
          SpecificSMTLibLocationFormulaInvariant other =
              (SpecificSMTLibLocationFormulaInvariant) pOther;
          return delegate.equals(other.delegate);
        }
        return false;
      }

      @Override
      public int hashCode() {
        return delegate.hashCode();
      }

      @Override
      public SingleLocationFormulaInvariant negate() {
        return new SpecificSMTLibLocationFormulaInvariant(bfmgr.not(invariant));
      }
    }
    return new SpecificSMTLibLocationFormulaInvariant(pInvariant);
  }

  public static CandidateInvariant makeLocationInvariant(
      final CFANode pLocation, final String pInvariant) {
    return new SMTLibLocationFormulaInvariant(pLocation, pInvariant);
  }

  private static final class SMTLibLocationFormulaInvariant
      extends SingleLocationFormulaInvariant {

    /** Is the invariant known to be the boolean constant 'false' */
    private boolean isDefinitelyBooleanFalse = false;

    private final LoadingCache<FormulaManagerView, BooleanFormula> cachedFormulas;

    private final String invariant;

    private SMTLibLocationFormulaInvariant(CFANode pLocation, String pInvariant) {
      super(pLocation);
      invariant = pInvariant;
      cachedFormulas =
          CacheBuilder.newBuilder()
              .weakKeys()
              .weakValues()
              .build(CacheLoader.from(fmgr -> fmgr.parse(invariant)));
    }

    @Override
    public BooleanFormula getFormula(
        FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
        throws CPATransferException, InterruptedException {

      if (isDefinitelyBooleanFalse) {
        return pFMGR.getBooleanFormulaManager().makeFalse();
      }

      BooleanFormula formula;
      try {
        formula = cachedFormulas.get(pFMGR);
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause != null) {
          Throwables.propagateIfPossible(
              cause, CPATransferException.class, InterruptedException.class);
          throw new UncheckedExecutionException(cause);
        }
        throw new UncheckedExecutionException(e);
      }

      if (!isDefinitelyBooleanFalse && pFMGR.getBooleanFormulaManager().isFalse(formula)) {
        isDefinitelyBooleanFalse = true;
      }

      return formula;
    }

    @Override
    public String toString() {
      return invariant;
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      if (pOther instanceof SMTLibLocationFormulaInvariant) {
        SMTLibLocationFormulaInvariant other = (SMTLibLocationFormulaInvariant) pOther;
        return getLocation().equals(other.getLocation()) && invariant.equals(other.invariant);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getLocation(), invariant.hashCode());
    }

    @Override
    public void assumeTruth(ReachedSet pReachedSet) {
      if (isDefinitelyBooleanFalse) {
        Iterable<AbstractState> targetStates = ImmutableList.copyOf(filterApplicable(pReachedSet));
        pReachedSet.removeAll(targetStates);
        for (ARGState s : from(targetStates).filter(ARGState.class)) {
          s.removeFromARG();
        }
      }
    }
  }
}
