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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.FQLSpecificationUtil;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.Edges;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.ClusteringCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering.InfeasibilityPropagation.Prediction;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton.State;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;

import com.google.common.collect.Sets;

public class TestGoalUtils {

  private final LogManager logger;

  private GuardedEdgeLabel mAlphaLabel;
  private GuardedEdgeLabel mOmegaLabel;
  private InverseGuardedEdgeLabel mInverseAlphaLabel;
  private Pattern variableWhitelist;

  public TestGoalUtils(LogManager pLogger, boolean pUseTigerAlgorithm_with_pc,
      NamedRegionManager pBddCpaNamedRegionManager, GuardedEdgeLabel pAlphaLabel,
      InverseGuardedEdgeLabel pInverseAlphaLabel, GuardedEdgeLabel pOmegaLabel, String bdpvwl) {
    logger = pLogger;

    mAlphaLabel = pAlphaLabel;
    mInverseAlphaLabel = pInverseAlphaLabel;
    mOmegaLabel = pOmegaLabel;

    if (bdpvwl == null) {
      bdpvwl = "";
    }
    bdpvwl = bdpvwl.replaceAll("%28", "(");
    bdpvwl = bdpvwl.replaceAll("%29", ")");
    bdpvwl = bdpvwl.replaceAll("%5C", "\\\\");
    bdpvwl = bdpvwl.replaceAll("%2A", "*");
    variableWhitelist = Pattern.compile(bdpvwl);
  }

  public FQLSpecification parseFQLQuery(String fqlQuery) throws InvalidConfigurationException {
    FQLSpecification fqlSpecification = null;

    logger.logf(Level.INFO, "FQL query string: %s", fqlQuery);

    fqlSpecification = FQLSpecificationUtil.getFQLSpecification(fqlQuery);

    logger.logf(Level.INFO, "FQL query: %s", fqlSpecification.toString());

    // TODO fix this restriction
    if (fqlSpecification.hasPassingClause()) {
      logger.logf(Level.SEVERE, "No PASSING clauses supported at the moment!");

      throw new InvalidConfigurationException("No PASSING clauses supported at the moment!");
    }

    // TODO fix this restriction
    if (fqlSpecification.hasPredicate()) {
      logger.logf(Level.SEVERE, "No predicates in FQL queries supported at the moment!");

      throw new InvalidConfigurationException(
          "No predicates in FQL queries supported at the moment!");
    }

    return fqlSpecification;
  }

  public Set<Goal> extractTestGoalPatterns(FQLSpecification pFqlSpecification,
      Prediction[] pGoalPrediction,
      Pair<Boolean, LinkedList<Edges>> pInfeasibilityPropagation,
      CoverageSpecificationTranslator pCoverageSpecificationTranslator,
      boolean pOptimizeGoalAutomata, boolean pUseOmegaLabel) {
    LinkedList<ElementaryCoveragePattern> goalPatterns;

    if (pInfeasibilityPropagation.getFirst()) {
      goalPatterns = extractTestGoalPatterns_InfeasibilityPropagation(pFqlSpecification,
          pInfeasibilityPropagation.getSecond(), pCoverageSpecificationTranslator);

      pGoalPrediction = new Prediction[goalPatterns.size()];

      for (int i = 0; i < goalPatterns.size(); i++) {
        pGoalPrediction[i] = Prediction.UNKNOWN;
      }
    } else {
      // (ii) translate query into set of test goals
      // I didn't move this operation to the constructor since it is a potentially expensive operation.
      goalPatterns =
          extractTestGoalPatterns(pFqlSpecification, pCoverageSpecificationTranslator);
      // each test goal needs to be covered in all (if possible) products.
      // Therefore we add a "todo" presence-condition TRUE to each test goal
      // it is the "maximum" set of products for which we try to cover this goal (could be useful to limit this set if we have feature models?)
      pGoalPrediction = null;
    }

    Set<Goal> goalsToCover = Sets.newLinkedHashSet();

    for (int i = 0; i < goalPatterns.size(); i++) {
      Goal lGoal =
          constructGoal(i + 1, goalPatterns.get(i), mAlphaLabel, mInverseAlphaLabel, mOmegaLabel,
              pOptimizeGoalAutomata, pUseOmegaLabel);
      if (lGoal != null) {
        goalsToCover.add(lGoal);
      }
    }

    return goalsToCover;
  }

  private LinkedList<ElementaryCoveragePattern> extractTestGoalPatterns_InfeasibilityPropagation(
      FQLSpecification pFQLQuery, LinkedList<Edges> pEdges,
      CoverageSpecificationTranslator pCoverageSpecificationTranslator) {
    logger.logf(Level.INFO, "Extracting test goals.");

    CFANode lInitialNode = this.mAlphaLabel.getEdgeSet().iterator().next().getSuccessor();
    ClusteringCoverageSpecificationTranslator lTranslator =
        new ClusteringCoverageSpecificationTranslator(
            pCoverageSpecificationTranslator.mPathPatternTranslator, pEdges,
            lInitialNode);

    ElementaryCoveragePattern[] lGoalPatterns =
        lTranslator.createElementaryCoveragePatternsAndClusters();

    LinkedList<ElementaryCoveragePattern> goalPatterns = new LinkedList<>();

    for (int lGoalIndex = 0; lGoalIndex < lGoalPatterns.length; lGoalIndex++) {
      goalPatterns.add(lGoalPatterns[lGoalIndex]);
    }

    return goalPatterns;
  }

  private LinkedList<ElementaryCoveragePattern> extractTestGoalPatterns(FQLSpecification pFQLQuery,
      CoverageSpecificationTranslator pCoverageSpecificationTranslator) {
    logger.logf(Level.INFO, "Extracting test goals.");

    // TODO check for (temporarily) unsupported features

    // TODO enable use of infeasibility propagation

    IncrementalCoverageSpecificationTranslator lTranslator =
        new IncrementalCoverageSpecificationTranslator(
            pCoverageSpecificationTranslator.mPathPatternTranslator);

    Iterator<ElementaryCoveragePattern> lGoalIterator =
        lTranslator.translate(pFQLQuery.getCoverageSpecification());
    LinkedList<ElementaryCoveragePattern> lGoalPatterns = new LinkedList<>();

    int numberOfTestGoals = lTranslator.getNumberOfTestGoals(pFQLQuery.getCoverageSpecification());
    for (int lGoalIndex = 0; lGoalIndex < numberOfTestGoals; lGoalIndex++) {
      lGoalPatterns.add(lGoalIterator.next());
    }

    return lGoalPatterns;
  }

  /**
   * Constructs a test goal from the given pattern.
   * @param pGoalPattern
   * @param pAlphaLabel
   * @param pInverseAlphaLabel
   * @param pOmegaLabel
   * @param pUseAutomatonOptimization
   * @param pUseOmegaLabel
   * @return
   */
  public Goal constructGoal(int pIndex, ElementaryCoveragePattern pGoalPattern,
      GuardedEdgeLabel pAlphaLabel,
      GuardedEdgeLabel pInverseAlphaLabel, GuardedLabel pOmegaLabel,
      boolean pUseAutomatonOptimization, boolean pUseOmegaLabel) {

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton =
        ToGuardedAutomatonTranslator.toAutomaton(pGoalPattern, pAlphaLabel, pInverseAlphaLabel,
            pOmegaLabel, pUseOmegaLabel);

    if (isFeatureAutomaton(automaton)) {
      return null;
    }

    automaton = FQLSpecificationUtil.optimizeAutomaton(automaton, pUseAutomatonOptimization);

    Goal lGoal = new Goal(pIndex, pGoalPattern, automaton);

    return lGoal;
  }

  private boolean isFeatureAutomaton(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton) {
    for (State state : pAutomaton.getStates()) {
      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge edge : pAutomaton
          .getIncomingEdges(state)) {
        String label = edge.getLabel().toString();

        if (variableWhitelist.matcher(label).find()) { return true; }
      }
    }

    return false;
  }

}
