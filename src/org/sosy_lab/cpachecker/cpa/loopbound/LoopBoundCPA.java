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
package org.sosy_lab.cpachecker.cpa.loopbound;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;
import static org.sosy_lab.cpachecker.util.AbstractStates.projectToType;

import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationBounding;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.loopbound")
public class LoopBoundCPA extends AbstractCPA
    implements ReachedSetAdjustingCPA, StatisticsProvider, Statistics, LoopIterationBounding {

  @Option(secure=true, description="enable stack-based tracking of loops")
  private boolean trackStack = false;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(LoopBoundCPA.class);
  }

  private final LoopStructure loopStructure;

  private final LoopBoundPrecisionAdjustment precisionAdjustment;

  LoopBoundCPA(Configuration pConfig, CFA pCFA, LogManager pLogger) throws InvalidConfigurationException, CPAException {
    super("sep", "sep", new LoopBoundTransferRelation(pConfig, pCFA));
    pConfig.inject(this);
    loopStructure = pCFA.getLoopStructure().orElseThrow();
    precisionAdjustment = new LoopBoundPrecisionAdjustment(pConfig, pLogger);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    LoopBoundState initialState = new LoopBoundState();
    for (Loop loop : loopStructure.getLoopsForLoopHead(pNode)) {
      initialState = initialState.visitLoopHead(new LoopEntry(pNode, loop));
    }
    return initialState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new LoopBoundPrecision(
        trackStack,
        precisionAdjustment.getMaxLoopIterations(),
        precisionAdjustment.getLoopIterationsBeforeAbstraction());
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment ;
  }

  @Override
  public boolean adjustPrecision() {
    return precisionAdjustment.nextState();
  }

  @Override
  public void adjustReachedSet(final ReachedSet pReachedSet) {
    Set<AbstractState> toRemove = new LinkedHashSet<>();
    for (AbstractState s : pReachedSet) {
      LoopBoundState loopBoundState = extractStateByType(s, LoopBoundState.class);
      if (loopBoundState != null && loopBoundState.mustDumpAssumptionForAvoidance()) {
        toRemove.add(s);
      }
    }

    // Never delete the first state
    if (toRemove.contains(pReachedSet.getFirstState())) {
      pReachedSet.clear();
      return;
    }

    Set<AbstractState> waitlist = new LinkedHashSet<>();
    for (ARGState s : projectToType(toRemove, ARGState.class)) {
      waitlist.addAll(s.getParents());
    }

    // Add the new waitlist
    waitlist.forEach(pReachedSet::reAddToWaitlist);

    pReachedSet.removeAll(toRemove);
    for (ARGState s : projectToType(toRemove, ARGState.class)) {
      s.removeFromARG();
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
    writer.put("Bound k", precisionAdjustment.getMaxLoopIterations());
    int maximumLoopIterationReached = 0;
    for (LoopBoundState state : projectToType(pReached, LoopBoundState.class)) {
      maximumLoopIterationReached =
          Math.max(maximumLoopIterationReached, state.getDeepestIteration());
    }
    writer.put("Maximum loop iteration reached", maximumLoopIterationReached);
    writer.spacer();
  }

  @Override
  public String getName() {
    return "Bounds CPA";
  }

  @Override
  public void setMaxLoopIterations(int pMaxLoopIterations) {
    precisionAdjustment.setMaxLoopIterations(pMaxLoopIterations);
  }

  @Override
  public int getMaxLoopIterations() {
    return precisionAdjustment.getMaxLoopIterations();
  }

  public void incrementLoopIterationsBeforeAbstraction() {
    precisionAdjustment.incrementLoopIterationsBeforeAbstraction();
  }
}