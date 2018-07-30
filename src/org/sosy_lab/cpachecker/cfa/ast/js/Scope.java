/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast.js;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;

public class Scope {
  public static Scope GLOBAL = new Scope(Collections.emptyList());

  private List<JSFunctionDeclaration> declarationStack;

  private Scope(final List<JSFunctionDeclaration> pDeclarationStack) {
    declarationStack = pDeclarationStack;
  }

  public boolean isGlobalScope() {
    return declarationStack.isEmpty();
  }

  public Scope createChildScope(final JSFunctionDeclaration pChild) {
    return new Scope(
        ImmutableList.<JSFunctionDeclaration>builderWithExpectedSize(declarationStack.size() + 1)
            .addAll(declarationStack)
            .add(pChild)
            .build());
  }

  public int getNestingLevel() {
    return declarationStack.size() - 1;
  }

  public JSFunctionDeclaration getFunctionDeclaration() {
    return declarationStack.get(getNestingLevel());
  }
}