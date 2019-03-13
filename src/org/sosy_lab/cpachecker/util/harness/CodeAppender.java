/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.harness;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.cpachecker.cfa.types.IAFunctionType;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.java.JMethodType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.cpa.harness.HarnessState;

class CodeAppender implements Appendable {

  private Set<AFunctionDeclaration> isImplemented = new HashSet<>();

  private static final String RETVAL_NAME = "retval";

  private final Appendable appendable;
  private final Set<AFunctionDeclaration> unimplementedPointerTypeParameterFunctions;

  public CodeAppender(Appendable pAppendable, CFA pCFA) {
    appendable = Objects.requireNonNull(pAppendable);
    unimplementedPointerTypeParameterFunctions =
        PointerFunctionExtractor.getExternUnimplementedPointerTypeParameterFunctions(pCFA);
  }

  public CodeAppender(Appendable pAppendable) {
    appendable = Objects.requireNonNull(pAppendable);
    unimplementedPointerTypeParameterFunctions = new HashSet<>();
  }

  @Override
  public String toString() {
    return appendable.toString();
  }

  public CodeAppender appendIncludes() throws IOException {
    appendable.append("#include <stdlib.h>");
    appendln();
    appendable.append("#include <stdio.h>");
    appendln();
    return this;
  }

  private CodeAppender appendVectorIndexDeclaration(String pInputFunctionVectorIndexName) throws IOException {
    appendable.append("  static unsigned int ");
    appendable.append(pInputFunctionVectorIndexName);
    appendln(" = 0;");
    return this;
  }

  public CodeAppender appendDeclaration(Type pType, String pName)
      throws IOException {
    appendable.append(pType.toASTString(pName));
    appendln(";");
    return this;
  }

  public CodeAppender appendAssignment(String pRetvalName, ARightHandSide pValue)
      throws IOException {
    appendable.append(pRetvalName);
    appendable.append(" = ");
    appendable.append(pValue.toASTString());
    return this;
  }

  CodeAppender appendAssignment(String pRetvalName, TestValue pValue) throws IOException {
    return appendAssignment(pRetvalName, pValue, true);
  }

  private CodeAppender appendAssignment(String pRetvalName, TestValue pValue, boolean pEnclose)
      throws IOException {
    boolean hasAuxiliaryStatmenets = pValue.getAuxiliaryStatements().size() > 0;
    if (hasAuxiliaryStatmenets) {
      if (pEnclose) {
        appendable.append("{ ");
      }
      for (AAstNode auxiliaryStatement : pValue.getAuxiliaryStatements()) {
        appendable.append(auxiliaryStatement.toASTString());
        if (pEnclose) {
          appendable.append(" ");
        } else {
          appendln();
        }
      }
    }
    appendable.append(pRetvalName);
    appendable.append(" = ");
    appendable.append(pValue.getValue().toASTString());
    appendable.append(";");
    if (hasAuxiliaryStatmenets && pEnclose) {
      appendable.append(" }");
    }
    return this;
  }

  public CodeAppender appendln(String pLine) throws IOException {
    appendable.append(pLine);
    appendln();
    return this;
  }

  public Appendable appendln() throws IOException {
    appendable.append(System.lineSeparator());
    return this;
  }

  @Override
  public CodeAppender append(CharSequence pCsq) throws IOException {
    appendable.append(pCsq);
    return this;
  }

  @Override
  public CodeAppender append(char pChar) throws IOException {
    appendable.append(pChar);
    return this;
  }

  @Override
  public CodeAppender append(CharSequence pCsq, int pStart, int pEnd) throws IOException {
    appendable.append(pCsq, pStart, pEnd);
    return this;
  }


  public CodeAppender append(TestVector pVector) throws IOException {
    String externPointersArrayName = "HARNESS_externPointersArray";
    String arrayPushCounterName = "HARNESS_externPointersArrayCounter";
    append("long int ");
    append(arrayPushCounterName);
    appendln(" = 0;");

    for (AVariableDeclaration inputVariable : pVector.getInputVariables()) {
      InitializerTestValue inputValue = pVector.getInputValue(inputVariable);
      List<AAstNode> auxiliaryStatmenets = inputValue.getAuxiliaryStatements();
      Type type = PredefinedTypes.getCanonicalType(inputVariable.getType());
      boolean requiresInitialization = HarnessExporter.canInitialize(type);
      if (requiresInitialization && !auxiliaryStatmenets.isEmpty()) {
        for (AAstNode statement : inputValue.getAuxiliaryStatements()) {
          appendln(statement.toASTString());
        }
      }
      final AInitializer initializer;
      if (!requiresInitialization) {
        initializer = null;
      } else {
        initializer = inputValue.getValue();
      }
      final AVariableDeclaration internalDeclaration;
      if (inputVariable instanceof CVariableDeclaration) {
        internalDeclaration =
            new CVariableDeclaration(
                FileLocation.DUMMY,
                inputVariable.isGlobal(),
                CStorageClass.AUTO,
                (CType) inputVariable.getType(),
                inputVariable.getName(),
                inputVariable.getOrigName(),
                inputVariable.getQualifiedName(),
                (CInitializer) initializer);
      } else if (inputVariable instanceof JVariableDeclaration) {
        internalDeclaration =
            new JVariableDeclaration(
                FileLocation.DUMMY,
                (JType) inputVariable.getType(),
                inputVariable.getName(),
                inputVariable.getOrigName(),
                inputVariable.getQualifiedName(),
                initializer,
                ((JVariableDeclaration) inputVariable).isFinal());
      } else {
        throw new AssertionError("Unsupported declaration type: " + inputVariable);
      }
      appendln(internalDeclaration.toASTString());
    }
    int externPointersArrayLength = pVector.getExternPointersArrayLength();
    append("int* ");
    append(externPointersArrayName);
    append(" [");
    append(Integer.toString(externPointersArrayLength));
    appendln("];");
    for (ComparableFunctionDeclaration pointerFunction : pVector.getPointerFunctions()) {
      AFunctionDeclaration functionDeclaration = pointerFunction.getDeclaration();
      isImplemented.add(functionDeclaration);
      List<Integer> arrayIndices = pVector.getIndices(pointerFunction);
      String functionCounterName = functionDeclaration.getOrigName().concat("_ret_counter");

      append("unsigned long int ");
      append(functionCounterName);
      append(" = 0");
      appendln(";");

      append(functionDeclaration);

      appendln("{");

      append("  ");
      append("int* retval");
      appendln(";");
      int switchCaseCounter = 0;
      append("  switch(");
      append(functionCounterName);
      appendln(") {");
      for (int arrayIndex : arrayIndices) {
        append("        case ");
        append(Integer.toString(switchCaseCounter));
        append(": retval = ");
        append(externPointersArrayName);
        append("[");
        append(Integer.toString(arrayIndex));
        append("]; ");
        appendln("break;");
      }
      appendln("  }");
      append("  ++");
      append(functionCounterName);
      appendln(";");
      appendln("    return retval;");
      appendln("}");
    }
    for (AFunctionDeclaration inputFunction : pVector.getInputFunctions()) {
      if (isImplemented.contains(inputFunction)) {
        continue;
      }

      Iterable<AParameterDeclaration> parameterDeclarations =
          TestVector.upcast(
              FluentIterable.from(inputFunction.getParameters()),
              AParameterDeclaration.class);
      List<AParameterDeclaration> pointerParameterDeclarations = new LinkedList<>();
      for (AParameterDeclaration parameterDeclaration : parameterDeclarations) {
        if (parameterDeclaration.getType() instanceof CPointerType) {
          pointerParameterDeclarations.add(parameterDeclaration);
        }
      }

      List<ExpressionTestValue> inputValues = pVector.getInputValues(inputFunction);
      Type returnType = inputFunction.getType().getReturnType();
      append(inputFunction);
      appendln(" {");

      if (pointerParameterDeclarations.size() > 0
          && HarnessState.relevantFunctions.contains(inputFunction)) {
        for (AParameterDeclaration pointerParameterDeclaration : pointerParameterDeclarations) {
          String varName = pointerParameterDeclaration.getName();
          append(externPointersArrayName);
          append("[");
          append(arrayPushCounterName);
          append("++] = ");
          append("(int*) ");
          append(varName);
          appendln(";");
        }

      }


      if (!returnType.equals(CVoidType.VOID)) {
        String inputFunctionVectorIndexName = "test_vector_index";
        if (inputValues.size() > 1) {
          appendVectorIndexDeclaration(inputFunctionVectorIndexName);
        }
        append("  ");
        appendDeclaration(returnType, RETVAL_NAME);
        if (inputValues.size() == 1) {
          append("  ");
          appendAssignment(RETVAL_NAME, inputValues.iterator().next());
          appendln();
        } else if (inputValues.size() > 1) {
          append("  switch (");
          append(inputFunctionVectorIndexName);
          appendln(") {");
          int i = 0;
          for (TestValue value : inputValues) {
            append("    case ");
            append(Integer.toString(i));
            append(": ");
            appendAssignment(RETVAL_NAME, value);
            appendln(" break;");
            ++i;
          }
          appendln("  }");
          append("  ++");
          append(inputFunctionVectorIndexName);
          appendln(";");
        }
        append("  return ");
        append(RETVAL_NAME);
        appendln(";");
      }
      appendln("}");
    }
    return this;
  }

  public CodeAppender append(AFunctionDeclaration pInputFunction) throws IOException {
    return append(enforceParameterNames(pInputFunction).toASTString(pInputFunction.getName()));
  }

  private static AFunctionType enforceParameterNames(AFunctionDeclaration pInputFunction) {
    IAFunctionType functionType = pInputFunction.getType();
    if (functionType instanceof CFunctionType) {
      CFunctionType cFunctionType = (CFunctionType) functionType;
      return new CFunctionTypeWithNames(
          cFunctionType.getReturnType(),
          FluentIterable.from(enforceParameterNames(pInputFunction.getParameters()))
              .filter(CParameterDeclaration.class)
              .toList(),
          functionType.takesVarArgs());
    }
    if (functionType instanceof JMethodType) {
      JMethodType methodType = (JMethodType) functionType;
      return new JMethodType(
          methodType.getReturnType(),
          FluentIterable.from(enforceParameterNames(pInputFunction.getParameters()))
              .filter(JType.class)
              .toList(),
          functionType.takesVarArgs());
    }
    throw new AssertionError("Unsupported function type: " + functionType.getClass());
  }

  private static List<AParameterDeclaration> enforceParameterNames(
      List<? extends AParameterDeclaration> pParameters) {
    Set<String> usedNames = Sets.newHashSetWithExpectedSize(pParameters.size());
    int i = 0;
    List<AParameterDeclaration> result = Lists.newArrayListWithCapacity(pParameters.size());
    for (AParameterDeclaration parameter : pParameters) {
      AParameterDeclaration declaration = parameter;
      if (!declaration.getName().isEmpty()) {
        usedNames.add(declaration.getName());
      } else {
        String name;
        while (!usedNames.add(name = "p" + i)) {
          ++i;
        }
        if (declaration instanceof CParameterDeclaration) {
          declaration =
              new CParameterDeclaration(FileLocation.DUMMY, (CType) declaration.getType(), name);
        } else if (declaration instanceof JParameterDeclaration) {
          JParameterDeclaration jDecl = (JParameterDeclaration) declaration;
          declaration =
              new JParameterDeclaration(
                  FileLocation.DUMMY,
                  jDecl.getType(),
                  name,
                  jDecl.getQualifiedName() + name,
                  jDecl.isFinal());
        } else {
          throw new AssertionError(
              "Unsupported parameter declaration type: " + declaration.getClass());
        }
      }
      result.add(declaration);
    }
    return result;
  }

  public void append(ArrayList<CTypeDeclaration> pPredefinedTypes) throws IOException {
    for (CTypeDeclaration typeDeclaration : pPredefinedTypes) {
      appendable.append(typeDeclaration.toASTString());

    }
  }

}
