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
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.Negation;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransition.Builder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class StateManager {

  private final State initState;
  private final State itpState;
  private final State sinkState;
  private final State finalState;

  private final FormulaManagerView formulaManagerView;
  private final CParser parser;
  private final CProgramScope scope;

  private final String automatonName;
  private final String interpolant;
  private final String interpolantNegated;

  private State curState;

  StateManager(
      FormulaManagerView pFormulaMgrView,
      CParser pParser,
      CProgramScope pScope,
      ARGState pInitState,
      String pAutomatonName,
      String pInterpolant,
      String pInterpolantNegated) {
    formulaManagerView = checkNotNull(pFormulaMgrView);
    parser = checkNotNull(pParser);
    scope = checkNotNull(pScope);
    automatonName = checkNotNull(pAutomatonName);
    interpolant = pInterpolant;
    interpolantNegated = pInterpolantNegated;

    initState = new InitState(this, pInitState);
    itpState = new InterpolationState(this);
    sinkState = new SinkState(this);
    finalState = new FinalState(this);

    setCurrentState(initState);
  }

  void setNextState() {
    if (curState.equals(initState)) {
      itpState.setInitState(initState.getLastParentState());
      setCurrentState(itpState);
    } else if (curState.equals(itpState)) {
      finalState.setInitState(itpState.getLastParentState());
      setCurrentState(finalState);
    } else {
      throw new IllegalStateException("No next state available for " + curState);
    }
  }

  String getNameOfNextState() {
    if (curState.equals(initState)) {
      return itpState.getStateName();
    } else if (curState.equals(itpState)) {
      return finalState.getStateName();
    } else {
      throw new IllegalStateException("No next state available for " + curState);
    }
  }

  Automaton createAutomaton() throws InvalidAutomatonException {
    return new Automaton(
        automatonName,
        ImmutableMap.of(),
        ImmutableList.of(
            initState.buildInternalState(),
            itpState.buildInternalState(),
            sinkState.buildInternalState(),
            finalState.buildInternalState()),
        initState.getStateName());
  }

  private void setCurrentState(State pState) {
    curState = pState;
  }

  String getAutomatonName() {
    return automatonName;
  }

  String getInterpolant() {
    return interpolant;
  }

  String getNegatedInterpolant() {
    return interpolantNegated;
  }

  void addTransition(BooleanFormula pInterpolant, ARGState pCurrentState)
      throws InvalidAutomatonException, InterruptedException {
    curState.addNewST(pInterpolant, pCurrentState);
  }

  abstract class State {
    final StateManager stateMgr;
    List<ARGState> argStates;
    List<CFAEdge> cfaEdges;
    Set<AutomatonBoolExpr> boolExpressions;
    Set<AutomatonTransition> transitions;

    final private String stateName;

    ARGState lastParentState;

    State(StateManager pStateMgr, String pStateName) {
      stateMgr = pStateMgr;
      stateName = pStateName;
      argStates = new ArrayList<>();
      cfaEdges = new ArrayList<>();
      boolExpressions = new HashSet<>();
      transitions = new LinkedHashSet<>();
    }

    void setInitState(ARGState pInitState) {
      checkArgument(argStates.isEmpty());
      argStates.add(pInitState);
      lastParentState = pInitState;
    }

    void addNewST(BooleanFormula pInterpolant, ARGState pCurrentState)
        throws InvalidAutomatonException, InterruptedException {
      checkArgument(!argStates.isEmpty());

      setCurrentStateAsParent(pCurrentState);

      if (formulaManagerView.getBooleanFormulaManager().isTrue(pInterpolant)) {
        curState.createInvariantTrueTransition(pCurrentState);
      } else if (formulaManagerView.getBooleanFormulaManager().isFalse(pInterpolant)) {
        curState.createInvariantFalseTransition(pInterpolant, pCurrentState);
      } else {
        curState.createInterpolantTransition(pInterpolant, pCurrentState);
      }
    }

    abstract void createInvariantTrueTransition(ARGState pCurrentState)
        throws InvalidAutomatonException;

    abstract void createInterpolantTransition(BooleanFormula pInterpolant, ARGState pCurrentState)
        throws InvalidAutomatonException, InterruptedException;

    abstract void
        createInvariantFalseTransition(BooleanFormula pInterpolant, ARGState pCurrentState)
            throws InvalidAutomatonException, InterruptedException;

    private ARGState getLastParentState() {
      return lastParentState;
    }

    String getStateName() {
      return stateName;
    }

    private Optional<CFAEdge> handleSingleEdge(
        ARGState parentState,
        ARGState currentState,
        String pFollowStateName,
        Optional<String> pAssumptionOpt)
        throws InvalidAutomatonException {
      Optional<CFAEdge> singleEdgeOpt =
          Optional.ofNullable(parentState.getEdgeToChild(currentState));
      if (singleEdgeOpt.isPresent()) {
        addEdgeToTransition(pFollowStateName, singleEdgeOpt.get(), pAssumptionOpt);
      }
      return singleEdgeOpt;
    }

    List<CFAEdge>
        createTransitionToSameState(ARGState pCurrentState, Optional<String> pAssumptionOpt)
            throws InvalidAutomatonException {
      Optional<CFAEdge> singleEdgeOpt =
          handleSingleEdge(getLastParentState(), pCurrentState, getStateName(), pAssumptionOpt);
      if (singleEdgeOpt.isPresent()) {
        return ImmutableList.of(singleEdgeOpt.get());
      } else {
        // aggregateBasicBlocks is enabled!
        List<CFAEdge> edges = getLastParentState().getEdgesToChild(pCurrentState);
        checkArgument(!edges.isEmpty());
        addEdgesToTransition(getStateName(), edges, pAssumptionOpt);
        return edges;
      }
    }

    List<CFAEdge>
        createTransitionToNextState(ARGState pCurrentState, Optional<String> pAssumptionOpt)
            throws InvalidAutomatonException {
      Optional<CFAEdge> singleEdgeOpt =
          handleSingleEdge(
              getLastParentState(),
              pCurrentState,
              stateMgr.getNameOfNextState(),
              pAssumptionOpt);
      if (singleEdgeOpt.isPresent()) {
        return ImmutableList.of(singleEdgeOpt.get());
      } else {
        // aggregateBasicBlocks is enabled!
        List<CFAEdge> edges = getLastParentState().getEdgesToChild(pCurrentState);
        checkArgument(!edges.isEmpty());
        addEdgesToTransition(stateMgr.getNameOfNextState(), edges, pAssumptionOpt);
        return edges;
      }
    }

    // List<CFAEdge> createTransitionToNextState(
    // BooleanFormula pInterpolant,
    // ARGState pCurrentState,
    // Optional<String> pAssumptionOpt)
    // throws InvalidAutomatonException, InterruptedException {
    // Optional<CFAEdge> singleEdgeOpt =
    // handleSingleEdge(
    // getLastParentState(),
    // pCurrentState,
    // stateMgr.getNameOfNextState(),
    // pAssumptionOpt);
    //
    // if (singleEdgeOpt.isPresent()) {
    // setCurrentStateAsParent(pCurrentState);
    // stateMgr.setNextState();
    // return ImmutableList.of(singleEdgeOpt.get());
    // } else {
    // // aggregateBasicBlocks is enabled!
    // List<CFAEdge> edges = getLastParentState().getEdgesToChild(pCurrentState);
    // addEdgeToTransition(stateMgr.getNameOfNextState(), edges.iterator().next(), pAssumptionOpt);
    //
    // setCurrentStateAsParent(pCurrentState);
    // stateMgr.setNextState();
    // stateMgr.addEdgesToNextTransition(Iterables.skip(edges, 1), pInterpolant, pCurrentState);
    // return edges;
    // }
    // }

    private void
        addEdgeToTransition(String pFollowStateName, CFAEdge pEdge, Optional<String> pAssumptionOpt)
            throws InvalidAutomatonException {
      transitions.add(matchST(pFollowStateName, pEdge, pAssumptionOpt));
    }

    void addEdgesToTransition(
        String pFollowStateName,
        Iterable<CFAEdge> pEdges,
        Optional<String> pAssumptionOpt)
        throws InvalidAutomatonException {
      for (CFAEdge cfaEdge : pEdges) {
        addEdgeToTransition(pFollowStateName, cfaEdge, pAssumptionOpt);
      }
    }

    void setCurrentStateAsParent(ARGState pCurrentState) {
      lastParentState = Iterables.getLast(argStates);
      argStates.add(pCurrentState);
    }

    List<AutomatonTransition>
        matchST(String pStateName, Iterable<CFAEdge> pEdges, Optional<String> pAssumptionOpt)
            throws InvalidAutomatonException {
      List<AutomatonTransition> list = new ArrayList<>();
      for (CFAEdge cfaEdge : pEdges) {
        list.add(matchST(pStateName, cfaEdge, pAssumptionOpt));
      }
      return list;
    }

    private AutomatonTransition
        matchST(String pFollowStateName, CFAEdge pCFAEdge, Optional<String> pAssumptionOpt)
            throws InvalidAutomatonException {
      AutomatonBoolExpr.MatchCFAEdgeNodes trigger =
          new AutomatonBoolExpr.MatchCFAEdgeNodes(
              pCFAEdge.getPredecessor().getNodeNumber(),
              pCFAEdge.getSuccessor().getNodeNumber(),
              pCFAEdge.getRawStatement());
      boolExpressions.add(trigger);
      AutomatonTransition.Builder builder =
          new AutomatonTransition.Builder(trigger, pFollowStateName);
      addViolationPropertyToBuilder(builder);
      if (pAssumptionOpt.isPresent()) {
        builder.withAssumptions(assume(pAssumptionOpt.get()));
      }
      return builder.build();
    }

    abstract void addViolationPropertyToBuilder(AutomatonTransition.Builder pBuilder)
        throws InvalidAutomatonException;

    protected List<AExpression> assume(String pCExpression) throws InvalidAutomatonException {
      CAstNode sourceAST = CParserUtils.parseSingleStatement(pCExpression, parser, scope);
      AExpression expression = ((CExpressionStatement) sourceAST).getExpression();
      return ImmutableList.of(expression);
    }

    AutomatonInternalState buildInternalState() {
      return new AutomatonInternalState(getStateName(), buildTransitions(), isTargetState(), true);
    }

    abstract List<AutomatonTransition> buildTransitions();

    abstract boolean isTargetState();

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((stateName == null) ? 0 : stateName.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      State other = (State) obj;
      if (!getOuterType().equals(other.getOuterType())) {
        return false;
      }
      if (stateName == null) {
        if (other.stateName != null) {
          return false;
        }
      } else if (!stateName.equals(other.stateName)) {
        return false;
      }
      return true;
    }

    private StateManager getOuterType() {
      return StateManager.this;
    }

  }

  class InitState extends State {

    private static final String INIT_STATE = "init_State";

    private final StringExpression violatedPropertyDescription;

    InitState(StateManager pStateMgr, ARGState pInitState) {
      super(pStateMgr, INIT_STATE);
      violatedPropertyDescription = new StringExpression(stateMgr.getAutomatonName());

      setInitState(pInitState);
    }

    @Override
    void createInvariantTrueTransition(ARGState pCurrentState) throws InvalidAutomatonException {
      createTransitionToSameState(pCurrentState, Optional.of(stateMgr.getNegatedInterpolant()));
      List<CFAEdge> edges =
          createTransitionToNextState(pCurrentState, Optional.of(stateMgr.getInterpolant()));
      cfaEdges.addAll(edges);
    }

    @Override
    void createInterpolantTransition(BooleanFormula pInterpolant, ARGState pCurrentState)
        throws InvalidAutomatonException, InterruptedException {
      createTransitionToSameState(pCurrentState, Optional.of(stateMgr.getNegatedInterpolant()));
      List<CFAEdge> edges =
          createTransitionToNextState(pCurrentState, Optional.of(stateMgr.getInterpolant()));
      cfaEdges.addAll(edges);
      stateMgr.setNextState();
      stateMgr.addTransition(pInterpolant, pCurrentState);
      setCurrentStateAsParent(pCurrentState);
    }

    @Override
    void createInvariantFalseTransition(BooleanFormula pInterpolant, ARGState pCurrentState) {
      throw new IllegalStateException(String.format("Illegal state: %s", pCurrentState));
    }

    @Override
    void addViolationPropertyToBuilder(Builder pBuilder) {
      pBuilder.withViolatedPropertyDescription(violatedPropertyDescription);
    }

    @Override
    List<AutomatonTransition> buildTransitions() {
      Optional<AutomatonBoolExpr> boolExprOpt =
          boolExpressions.stream().reduce((x, y) -> new AutomatonBoolExpr.Or(x, y));
      checkArgument(boolExprOpt.isPresent());
      Negation negatedTrigger = new AutomatonBoolExpr.Negation(boolExprOpt.get());
      transitions.add(
          new AutomatonTransition.Builder(negatedTrigger, stateMgr.sinkState.getStateName())
              .build());
      return ImmutableList.copyOf(transitions);
    }

    @Override
    boolean isTargetState() {
      return true;
    }
  }

  class InterpolationState extends State {

    private static final String ITP_STATE = "itp_state";

    private final StringExpression violatedPropertyDescription;

    InterpolationState(StateManager pStateMgr) {
      super(pStateMgr, ITP_STATE);
      violatedPropertyDescription = new StringExpression(stateMgr.getAutomatonName());
    }

    @Override
    void createInvariantTrueTransition(ARGState pCurrentState) throws InvalidAutomatonException {
      throw new IllegalStateException(String.format("Illegal state: %s", pCurrentState));
    }

    @Override
    void createInterpolantTransition(BooleanFormula pInterpolant, ARGState pCurrentState)
        throws InvalidAutomatonException {
      createTransitionToSameState(pCurrentState, Optional.of(stateMgr.getInterpolant()));
      List<CFAEdge> edges =
          createTransitionToNextState(pCurrentState, Optional.of(stateMgr.getNegatedInterpolant()));
      cfaEdges.addAll(edges);
    }

    @Override
    void createInvariantFalseTransition(BooleanFormula pInterpolant, ARGState pCurrentState)
        throws InvalidAutomatonException, InterruptedException {

      createTransitionToSameState(pCurrentState, Optional.of(stateMgr.getInterpolant()));
      List<CFAEdge> edges =
          createTransitionToNextState(pCurrentState, Optional.of(stateMgr.getNegatedInterpolant()));
      cfaEdges.addAll(edges);
      stateMgr.setNextState();
      stateMgr.addTransition(pInterpolant, pCurrentState);
      setCurrentStateAsParent(pCurrentState);
    }

    @Override
    void addViolationPropertyToBuilder(Builder pBuilder) throws InvalidAutomatonException {
      pBuilder.withViolatedPropertyDescription(violatedPropertyDescription);
    }

    @Override
    List<AutomatonTransition> buildTransitions() {
      Optional<AutomatonBoolExpr> boolExprOpt =
          boolExpressions.stream().reduce((x, y) -> new AutomatonBoolExpr.Or(x, y));
      checkArgument(boolExprOpt.isPresent());
      Negation negatedTrigger = new AutomatonBoolExpr.Negation(boolExprOpt.get());
      transitions.add(
          new AutomatonTransition.Builder(negatedTrigger, stateMgr.sinkState.getStateName())
              .build());
      return ImmutableList.copyOf(transitions);
    }

    @Override
    boolean isTargetState() {
      return true;
    }
  }

  class FinalState extends State {

    private static final String FINAL_STATE = "final_state";

    FinalState(StateManager pStateMgr) {
      super(pStateMgr, FINAL_STATE);
    }

    @Override
    void createInvariantTrueTransition(ARGState pCurrentState) throws InvalidAutomatonException {
      throw new IllegalStateException(String.format("Illegal state: %s", pCurrentState));
    }

    @Override
    void createInterpolantTransition(BooleanFormula pInterpolant, ARGState pCurrentState) {
      throw new IllegalStateException(
          String.format("Illegal state: %s, %s", pInterpolant, pCurrentState));
    }

    @Override
    void createInvariantFalseTransition(BooleanFormula pInterpolant, ARGState pCurrentState)
        throws InvalidAutomatonException {
      // do nothing
    }

    @Override
    void addViolationPropertyToBuilder(Builder pBuilder) {
      // do nothing
    }

    @Override
    List<AutomatonTransition> buildTransitions() {
      transitions.add(
          new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, AutomatonInternalState.BOTTOM)
              .build());
      return ImmutableList.copyOf(transitions);
    }

    @Override
    boolean isTargetState() {
      return false;
    }
  }

  class SinkState extends State {

    private static final String SINK_STATE = "sink_state";

    private final StringExpression violatedPropertyDescription;

    SinkState(StateManager pStateMgr) {
      super(pStateMgr, SINK_STATE);
      violatedPropertyDescription = new StringExpression(stateMgr.getAutomatonName());
    }

    @Override
    void createInvariantTrueTransition(ARGState pCurrentState) throws InvalidAutomatonException {
      throw new IllegalStateException(String.format("Illegal state: %s", pCurrentState));
    }

    @Override
    void createInterpolantTransition(BooleanFormula pInterpolant, ARGState pCurrentState) {
      throw new IllegalStateException(
          String.format("Illegal state: %s, %s", pInterpolant, pCurrentState));
    }

    @Override
    void createInvariantFalseTransition(BooleanFormula pInterpolant, ARGState pCurrentState)
        throws InvalidAutomatonException {
      throw new IllegalStateException(
          String.format("Illegal state: %s, %s", pInterpolant, pCurrentState));
    }

    @Override
    void addViolationPropertyToBuilder(Builder pBuilder) {
      pBuilder.withViolatedPropertyDescription(violatedPropertyDescription);
    }

    @Override
    List<AutomatonTransition> buildTransitions() {
      transitions.add(new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, SINK_STATE).build());
      return ImmutableList.copyOf(transitions);
    }

    @Override
    boolean isTargetState() {
      return true;
    }
  }

}
