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
package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * This is an abstract class for building CPAs. It uses the flat lattice domain
 * if no other domain is given, and the standard implementations for merge-(sep|join)
 * and stop-sep.
 */
public abstract class AbstractCPA implements ConfigurableProgramAnalysis {

  private final AbstractDomain abstractDomain;

  /* The operators can be overridden in sub-classes. Thus we allow Null as possible assignment. */
  private final @Nullable String mergeType;
  private final @Nullable String stopType;
  private final @Nullable TransferRelation transferRelation;

  protected AbstractCPA(String mergeType, String stopType, @Nullable TransferRelation transfer) {
    this(mergeType, stopType, new FlatLatticeDomain(), transfer);
  }

  /** When using this constructor, you have to override the methods for getting Merge- and StopOperator.
   * This can be useful for cases where the operators are configurable or are initialized lazily. */
  protected AbstractCPA(AbstractDomain domain, TransferRelation transfer) {
    this.abstractDomain = domain;
    this.mergeType = null;
    this.stopType = null;
    this.transferRelation = transfer;
  }

  /** Use this constructor, if Merge- and StopOperator are fixed. */
  protected AbstractCPA(String mergeType, String stopType, AbstractDomain domain, @Nullable TransferRelation transfer) {
    this.abstractDomain = domain;
    this.mergeType = mergeType;
    this.stopType = stopType;
    this.transferRelation = transfer;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(Preconditions.checkNotNull(mergeType));
  }

  protected MergeOperator buildMergeOperator(String pMergeType) {
    switch (pMergeType.toUpperCase()) {
      case "SEP":
        return MergeSepOperator.getInstance();

      case "JOIN":
        return new MergeJoinOperator(getAbstractDomain());

      default:
        throw new AssertionError("unknown merge operator");
    }
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(Preconditions.checkNotNull(stopType));
  }

  protected StopOperator buildStopOperator(String pStopType) throws AssertionError {
    switch (pStopType.toUpperCase()) {
      case "SEP": // state is LESS_OR_EQUAL to any reached state
        return new StopSepOperator(getAbstractDomain());

      case "JOIN": // state is LESS_OR_EQUAL to the union of all reached state
        return new StopJoinOperator(getAbstractDomain());

      case "NEVER": // always FALSE
        return new StopNeverOperator();

      case "ALWAYS": // always TRUE
        return new StopAlwaysOperator();

      case "EQUALS": // state is EQUAL to any reached state
        return new StopEqualsOperator();

      default:
        throw new AssertionError("unknown stop operator");
    }
  }

  @Override
  public TransferRelation getTransferRelation() {
    return Preconditions.checkNotNull(transferRelation);
  }
}
