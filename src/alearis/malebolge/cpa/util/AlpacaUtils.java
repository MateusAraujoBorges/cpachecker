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
package alearis.malebolge.cpa.util;

import alearis.malebolge.cpa.PCPVisitor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicIdentifierLocator;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class AlpacaUtils {
  public static void dumpPcForAlpaca(ARGState pLastState, LogManager logger) throws IOException {
    ConstraintsState cstate = AbstractStates.extractStateByType(pLastState, ConstraintsState.class);

    StringBuilder sb = new StringBuilder();
    PCPVisitor visitor = new PCPVisitor(logger);
    if (cstate.size() > 1) {
      sb.append("(and ");
    }
    for (Constraint c : cstate) {
      sb.append(c.accept(visitor));
      sb.append(" ");
    }
    if (cstate.size() > 1) {
      sb.append(")");
    }
    sb.append("\n##\n");

    // list all symbolic variables produced in this trace
    Set<SymbolicIdentifier> traceVars = extractAllSymbolicVarsFromTrace(pLastState);
    for (SymbolicIdentifier symVar : traceVars) {
      sb.append(PCPVisitor.prepareUniqueNameForVar(symVar));
      sb.append("\n");
    }
    sb.append("\n##\n");

    // list all symbolic variables, together with their types, that are present in the constraint
    visitor.getVars().forEach((var, type) -> {
      sb.append(PCPVisitor.prepareUniqueNameForVar(var));
      sb.append(" -> ");
      if (type instanceof CSimpleType) {
        CSimpleType ctype = (CSimpleType) type;
        sb.append(ctype.toASTString(""));
      }
      sb.append("\n");
    });

    Path output = Files.write(Paths.get("output", "alpaca.pc"),
        Collections.singletonList(sb.toString()), StandardOpenOption.CREATE,
        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    System.out.println("[alpaca-writer] pc written to: " + output);
  }

  //FIXME Slow as hell; probably will have to create a CPA for this since I can't (or don't know how)
  // to get only the diff between consecutive ValueAnalysisStates
  private static Set<SymbolicIdentifier> extractAllSymbolicVarsFromTrace(ARGState pLastState) {
    Deque<ARGState> worklist = new ArrayDeque<>();
    Set<ARGState> covered = new HashSet<>();
    Set<SymbolicIdentifier> traceVars = new TreeSet<>();
    worklist.add(pLastState);

    while (!worklist.isEmpty()) {
      ARGState current = worklist.pop();
      if (!covered.add(current)) {
        continue;
      }
      worklist.addAll(current.getParents());
      ValueAnalysisState vaState =
          AbstractStates.extractStateByType(current, ValueAnalysisState.class);
      vaState.getConstants().stream()
          .forEach(e -> {
            Value v = e.getValue().getValue();
            if (v instanceof SymbolicValue) {
              SymbolicValue sv = (SymbolicValue) v;
              SymbolicIdentifierLocator varExtractor = SymbolicIdentifierLocator.getInstance();
              traceVars.addAll(sv.accept(varExtractor));
            }
          });
    }

    return traceVars;
  }
}
