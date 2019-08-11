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

import alearis.malebolge.cpa.util.BigRational;
import alearis.malebolge.cpa.util.CountResult;
import alearis.malebolge.cpa.util.StatisticalCountResult;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;

@Options(prefix = "pcp")
public class PCPQuantifier implements ConstraintQuantifier {

  @Option(description = "ask pcp to simplify the constraint before counting")
  private boolean simplify = true;

  @Option(description = "URI for the PCP instance to be used (e.g. 'localhost:9001')")
  private String pcpAddress = "localhost:9001";

  @Option(description = "model counter to be used. defaults to barvinok")
  private Counter counter = Counter.BARVINOK;


  public enum Counter {
    BARVINOK,
    SHARPSAT;
  }

  private final PCPConnectionManager pcp;
  private final ShutdownNotifier shutdownNotifier;
  private final LoadingCache<Collection<Constraint>, CountResult> cache;
  private final LogManager logger;
  private final ExecutorService executor;
  private final Timer timer;

  public PCPQuantifier(
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      LogManager logger,
      Timer timer) throws InvalidConfigurationException {
    pConfig.inject(this);
    shutdownNotifier = pShutdownNotifier;
    this.logger = logger;
    this.timer = timer;

    try {
      String address;
      int port;
      int colonIndex = pcpAddress.indexOf(':');
      if (colonIndex > 0) {
        address = pcpAddress.substring(0, colonIndex);
        port = Integer.parseInt(pcpAddress.substring(colonIndex + 1));
      } else {
        address = "localhost";
        port = Integer.parseInt(pcpAddress);
      }
      this.pcp = new PCPConnectionManager(address, port, -1);
    } catch (NumberFormatException e) {
      throw new RuntimeException("Error connecting to PCP address: " + pcpAddress, e);
    }

    this.cache = CacheBuilder.newBuilder()
        .maximumSize(100000)
        .recordStats()
        .build(CacheLoader.from(this::count));
    executor = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setDaemon(true).build());
    shutdownNotifier.register(pS -> executor.shutdownNow());
  }

  @Override
  public BigRational quantify(Collection<Constraint> constraints) throws InterruptedException {
    Future<BigRational> future = executor.submit(() -> cache.get(constraints).getMean());
    while (true) {
      shutdownNotifier.shutdownIfNecessary();
      try {
        return future.get(2, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        throw e; //external interruption; likely from the timeout
      } catch (ExecutionException e) {
        // something went wrong with the counter
        throw new RuntimeException(e.getCause());
      } catch (TimeoutException e) {
        // next iteration;
      }
    }
  }

  private CountResult count(Collection<Constraint> pc) {
    Optional<String> query = translate(pc);
    if (!query.isPresent()) {
      return new StatisticalCountResult(BigRational.ONE);
    }

    logger.log(Level.FINEST, "PCP query: ", query.get());
    timer.start();
    try {
      CountResult result = pcp.count(query.get());
      logger.log(Level.FINE, "PCP result: ", result);
      return result;
    } finally {
      timer.stop();
    }
  }


  private Optional<String> translate(Collection<Constraint> pc) {
    if (pc.isEmpty()) {
      return Optional.empty();
    }

    PCPVisitor translator = new PCPVisitor(logger);

    StringBuilder header = new StringBuilder();
    StringBuilder body = new StringBuilder();

    String counterName;
    switch (counter) {
      case BARVINOK:
        counterName = "barvinok";
        break;
      case SHARPSAT:
        counterName = "sharpsat";
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + counter);
    }

    header.append("(clear)\n");
    header.append("(set-option :non-linear-counter \"qcoral\")\n" +
        "(set-option :partitioning true)\n" +
        "(set-option :inclusion-exclusion false)\n" +
        "(set-option :linear-counter \"" + counterName + "\")\n");

    if (this.simplify) {
      header.append("(set-option :simplify \"z3\")\n");
    }

    for (Constraint constraint : pc) {
      body.append("(assert ");
      body.append(constraint.accept(translator));
      body.append(")\n");
    }
    body.append("(count)\n");

    for (SymbolicIdentifier var : translator.getVarSet()) {
      //TODO SymbolicIdentifiers do not have any type information.
      // Gonna assume that everything is an int for now.

      header.append("(declare-var "
          + PCPVisitor.prepareUniqueNameForVar(var)
          + " (Int " + Integer.MIN_VALUE + " "
          + Integer.MAX_VALUE + "))\n");
    }
    header.append(body);

    return Optional.of(header.toString());
  }
}
