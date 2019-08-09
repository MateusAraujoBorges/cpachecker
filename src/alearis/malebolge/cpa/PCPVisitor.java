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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressOfExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.DivisionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ModuloExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.MultiplicationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.NegationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.PointerExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SubtractionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicIdentifierLocator;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class PCPVisitor implements SymbolicValueVisitor<String> {

  private final LogManager logger;

  private final Map<SymbolicIdentifier, Type> vars = new HashMap<>();

  public PCPVisitor(LogManager pLogger) {
    logger = pLogger;
  }

  public Set<SymbolicIdentifier> getVarSet() {
    return vars.keySet();
  }

  public Map<SymbolicIdentifier, Type> getVars() {
    return vars;
  }

  @Override
  public String visit(SymbolicIdentifier pValue) {
    throw new UnsupportedOperationException(
        "SymbolicIdentifier should be handled in handling of ConstantSymbolicExpression");
  }

  @Nonnull
  public static String prepareUniqueNameForVar(SymbolicIdentifier pValue) {
    String rep = pValue.getRepresentation();
    if (rep.charAt(0) == '_') {
      rep = "var@" + rep;
    }
    return rep.replace("::", "@")
        //.replace("/","@")
        + "~" + pValue.getId();
  }

  @Override
  public String visit(ConstantSymbolicExpression pExpression) {
    Value v = pExpression.getValue();
    Type t = pExpression.getType();

    if (v instanceof SymbolicIdentifier) {
      vars.put((SymbolicIdentifier) v, t);
      return prepareUniqueNameForVar((SymbolicIdentifier) v);
    } else if (v instanceof SymbolicValue) {
      return ((SymbolicValue) v).accept(this);
    } else if (v.isNumericValue()) {
      NumericValue num = v.asNumericValue();
      return "" + num.getNumber();
    } else {
      throw new UnsupportedOperationException("not implemented: ["
          + v.getClass() + "] " + v);
    }
  }

  public String handleBinary(String op, BinarySymbolicExpression expr) {
    String left = expr.getOperand1().accept(this);
    String right = expr.getOperand2().accept(this);
    return "(" + op + " " + left + " " + right + ")";
  }

  @Override
  public String visit(AdditionExpression pExpression) {
    return handleBinary("+", pExpression);
  }

  @Override
  public String visit(SubtractionExpression pExpression) {
    return handleBinary("-", pExpression);
  }

  @Override
  public String visit(MultiplicationExpression pExpression) {
    return handleBinary("*", pExpression);
  }

  @Override
  public String visit(DivisionExpression pExpression) {
    return handleBinary("/", pExpression);
  }

  @Override
  public String visit(ModuloExpression pExpression) {
    return handleBinary("mod", pExpression);
  }

  @Override
  public String visit(BinaryAndExpression pExpression) {
    return handleBinary("bvand", pExpression);
  }

  @Override
  public String visit(BinaryNotExpression pExpression) {
    return "(bvnot " + pExpression.getOperand().accept(this)
        + ")";
  }

  @Override
  public String visit(BinaryOrExpression pExpression) {
    return handleBinary("bvor", pExpression);
  }

  @Override
  public String visit(BinaryXorExpression pExpression) {
    return handleBinary("bvxor", pExpression);
  }

  @Override
  public String visit(ShiftRightExpression pExpression) {
    return handleBinary("bvshr", pExpression);
  }

  @Override
  public String visit(ShiftLeftExpression pExpression) {
    return handleBinary("bvshl", pExpression);
  }

  @Override
  public String visit(LogicalNotExpression pExpression) {
    return "(not " + pExpression.getOperand().accept(this) + ")";
  }

  @Override
  public String visit(LessThanOrEqualExpression pExpression) {
    return handleBinary("<=", pExpression);
  }

  @Override
  public String visit(LessThanExpression pExpression) {
    return handleBinary("<", pExpression);
  }

  @Override
  public String visit(EqualsExpression pExpression) {
    return handleBinary("=", pExpression);
  }

  @Override
  public String visit(LogicalOrExpression pExpression) {
    return handleBinary("or", pExpression);
  }

  @Override
  public String visit(LogicalAndExpression pExpression) {
    return handleBinary("and", pExpression);
  }

  @Override
  public String visit(CastExpression pExpression) {
    SymbolicExpression nested = pExpression.getOperand();
    Type original = nested.getType();
    Type toCast = pExpression.getType();

    if (!(original instanceof CType) || !(toCast instanceof CType)) {
      throw new UnsupportedOperationException("Only C types are supported for now!");
    }
    CType cOriginal = ((CType) original).getCanonicalType();
    CType cToCast = ((CType) toCast).getCanonicalType();

    //temporary implementation to investigate where/how casts appear
    logger.log(Level.WARNING, "cast ignored: ", cOriginal, " -> ", cToCast);
    logger.log(Level.WARNING, "casted expression: ", nested);
    logger.log(Level.WARNING, "casted expression: ", nested.getRepresentation());
    return nested.accept(this);
//
//    if (cToCast.canBeAssignedFrom(cOriginal)) {
//
//    }
//
//    if (original instanceof CSimpleType && toCast instanceof CSimpleType) {
//      return nested.accept(this);
//    } else { //just ignore for now
//      logger.log(Level.WARNING, "Non-trivial cast ignored: ", original, " -> ", toCast);
//      return nested.accept(this);
//    }
  }

  @Override
  public String visit(PointerExpression pExpression) {
    throw new UnsupportedOperationException("casts are not supported in PCP for now");
  }

  @Override
  public String visit(AddressOfExpression pExpression) {
    throw new UnsupportedOperationException("casts are not supported in PCP for now");
  }

  @Override
  public String visit(NegationExpression pExpression) {
    return "(not " + pExpression.getOperand().accept(this) + ")";
  }

}
