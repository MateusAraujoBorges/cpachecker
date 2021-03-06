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

import java.io.Serializable;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;

public class SingletonPrecision implements AdjustablePrecision, Serializable {

  private static final long serialVersionUID = 1L;

  private final static SingletonPrecision mInstance = new SingletonPrecision();

  public static SingletonPrecision getInstance() {
    return mInstance;
  }

  private SingletonPrecision() {

  }

  @Override
  public String toString() {
    return "no precision";
  }

  @Override
  public AdjustablePrecision add(AdjustablePrecision pOtherPrecision) {
    return this;
  }

  @Override
  public AdjustablePrecision subtract(AdjustablePrecision pOtherPrecision) {
    return this;
  }

  protected Object readResolve() {
    return getInstance();
  }

  @Override
  public boolean isEmpty() {
    return true;
  }
}
