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
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix = "cpa.probabilistic")
public class ProbabilisticCPA extends AbstractSingleWrapperCPA implements StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ProbabilisticCPA.class);
  }

  @Option(description = "Maximum depth of the symbolic tree to be explored")
  private int depthLimit = 1000;

  @Option(description = "Quantify probability of gray paths")
  private boolean quantifyGray = true;

  private final ConstraintQuantifier counter;
  private final LogManager logger;
  private final ProbabilisticStatistics currentProbabilities;
  private final CFA cfa;
  private final ShutdownNotifier shutdownNotifier;

  protected ProbabilisticCPA(
      ConfigurableProgramAnalysis cpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(cpa);
    pConfig.inject(this);
    this.logger = pLogger;
    this.cfa = pCfa;
    currentProbabilities = new ProbabilisticStatistics();
    this.shutdownNotifier = pShutdownNotifier;
    counter = new PCPQuantifier(pConfig, shutdownNotifier, logger);
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return new ProbabilisticState(getWrappedCpa().getInitialState(node, partition), 0);
  }

  @Override
  public TransferRelation getTransferRelation() {

    return new AbstractSingleWrapperTransferRelation(getWrappedCpa().getTransferRelation()) {
      @Override
      public Collection<? extends AbstractState> getAbstractSuccessors(
          AbstractState state, Precision precision)
          throws CPATransferException, InterruptedException {
        ProbabilisticState pstate =
            AbstractStates.extractStateByType(state, ProbabilisticState.class);
        int depth = pstate.getDepth();
        Collection<? extends AbstractState> successors;

        if (depth >= depthLimit) {
          pstate.setType(Type.GREY);
          successors = Collections.emptyList();
        } else {
          successors = this.transferRelation.
              getAbstractSuccessors(pstate.getWrappedState(), precision);
          if (successors.isEmpty()) {
            Type endType = pstate.isTarget() ? Type.FAILURE : Type.SUCCESS;
            pstate.setType(endType);
          }
        }

        if (pstate.getType() != Type.TRANSIENT) {
          countAndUpdate(pstate);
        }

        return successors.stream()
            .map(s -> new ProbabilisticState(s, depth + 1))
            .collect(Collectors.toList());
      }

      //TODO refactor copy/paste
      @Override
      public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
          AbstractState state, Precision precision, CFAEdge cfaEdge)
          throws CPATransferException, InterruptedException {
        ProbabilisticState pstate =
            AbstractStates.extractStateByType(state, ProbabilisticState.class);
        int depth = pstate.getDepth();
        Collection<? extends AbstractState> successors;

        if (depth >= depthLimit) {
          pstate.setType(Type.GREY);
          successors = Collections.emptyList();
        } else {
          successors = this.transferRelation
              .getAbstractSuccessorsForEdge(pstate.getWrappedState(), precision, cfaEdge);
          if (successors.isEmpty()) {
            Type endType = pstate.isTarget() ? Type.FAILURE : Type.SUCCESS;
            pstate.setType(endType);
          }
        }

        if (pstate.getType() != Type.TRANSIENT) {
          countAndUpdate(pstate);
        }

        return successors.stream()
            .map(s -> new ProbabilisticState(s, depth + 1))
            .collect(Collectors.toList());
      }
    };
  }

  @Override
  public MergeOperator getMergeOperator() {
    return (state1, state2, precision) -> {
      AbstractState wrapped1 = AbstractStates.extractStateByType(state1, ProbabilisticState.class)
          .getWrappedState();
      AbstractState wrapped2 = AbstractStates.extractStateByType(state2, ProbabilisticState.class)
          .getWrappedState();
      int depth = AbstractStates.extractStateByType(state2, ProbabilisticState.class)
          .getDepth();

      AbstractState merged =
          getWrappedCpa().getMergeOperator().merge(wrapped1, wrapped2, precision);
      if (merged == wrapped2) {
        return state2;
      }
      return new ProbabilisticState(merged, depth);
    };
  }

  @Override
  public StopOperator getStopOperator() {
    return (state, reached, precision) -> {
      AbstractState wrapped = AbstractStates.extractStateByType(state, ProbabilisticState.class)
          .getWrappedState();
      return getWrappedCpa().getStopOperator()
          .stop(wrapped, unwrapCollection(reached), precision);
    };
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return (state, precision, states, stateProjection, fullState) -> {
      AbstractState wrapped = AbstractStates.extractStateByType(state, ProbabilisticState.class)
          .getWrappedState();
      AbstractState fullWrapped =
          AbstractStates.extractStateByType(fullState, ProbabilisticState.class)
              .getWrappedState();
      Optional<PrecisionAdjustmentResult>
          adj = getWrappedCpa().getPrecisionAdjustment()
          .prec(wrapped, precision, states, stateProjection, fullWrapped);
      if (adj.isPresent()) {
        return Optional.of(
            adj.get().withAbstractState(
                new ProbabilisticState(adj.get().abstractState(),
                    ((ProbabilisticState) state).getDepth())));
      }
      return Optional.empty();
    };
  }


  private void countAndUpdate(ProbabilisticState state) throws InterruptedException {
    if (state.getType() == Type.GREY && !quantifyGray) {
      return;
    }
    BigRational count =
        counter.quantify(AbstractStates.extractStateByType(state, ConstraintsState.class));
    state.setProbability(count);
    this.currentProbabilities.update(state);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    if (!quantifyGray) {
      currentProbabilities.setGreyToRemainingUnexplored();
    }
    statsCollection.add(currentProbabilities);
  }

  private Collection<AbstractState> unwrapCollection(Collection<AbstractState> states) {
    return states.stream()
        .map(c -> ((AbstractSingleWrapperState) c).getWrappedState())
        .collect(Collectors.toList());
  }
}
