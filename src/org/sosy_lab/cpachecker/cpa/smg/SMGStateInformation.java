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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;

public class SMGStateInformation {

  private static final SMGStateInformation EMPTY = new SMGStateInformation();

  private final ImmutableSet<SMGEdgeHasValue> hvEdges;
  private final ImmutableSet<SMGEdgePointsTo> ptEdges;
  private final boolean valid;
  private final boolean external;

  private SMGStateInformation() {
    hvEdges = ImmutableSet.of();
    ptEdges = ImmutableSet.of();
    valid = false;
    external = false;
  }

  private SMGStateInformation(
      Set<SMGEdgeHasValue> pHves,
      Set<SMGEdgePointsTo> pPtes,
      boolean pIsRegionValid,
      boolean pIsRegionExternallyAllocated) {
    hvEdges = ImmutableSet.copyOf(pHves);
    ptEdges = ImmutableSet.copyOf(pPtes);
    valid = pIsRegionValid;
    external = pIsRegionExternallyAllocated;
  }

  public static SMGStateInformation of() {
    return EMPTY;
  }

  public ImmutableSet<SMGEdgePointsTo> getPtEdges() {
    return ptEdges;
  }

  public ImmutableSet<SMGEdgeHasValue> getHvEdges() {
    return hvEdges;
  }

  public boolean isValid() {
    return valid;
  }

  public boolean isExternal() {
    return external;
  }

  @Override
  public String toString() {
    return hvEdges + "\n" + ptEdges;
  }

  public static SMGStateInformation of(
      Set<SMGEdgeHasValue> pHves,
      Set<SMGEdgePointsTo> ptes,
      boolean pIsRegionValid,
      boolean pIsRegionExternallyAllocated) {
    return new SMGStateInformation(pHves, ptes, pIsRegionValid, pIsRegionExternallyAllocated);
  }
}
