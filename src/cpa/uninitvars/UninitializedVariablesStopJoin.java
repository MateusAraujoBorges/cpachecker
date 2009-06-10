/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.uninitvars;

import java.util.Collection;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

/**
 * @author Philipp Wendler
 */
public class UninitializedVariablesStopJoin implements StopOperator {

  @Override
  public <AE extends AbstractElement> boolean stop(AE element,
                                                   Collection<AE> reached,
                                                   Precision precision)
                                                   throws CPAException {
    // TODO implement stop-join
    return false;
  }

  @Override
  public boolean stop(AbstractElement element, AbstractElement reachedElement)
                      throws CPAException {
    throw new CPAException ("Cannot return element with location information");
  }
}
