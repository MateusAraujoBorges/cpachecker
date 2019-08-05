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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PCPConnectionManager {

  private int pid;
  private final String serverAddress;
  private final int serverPort;


  public PCPConnectionManager(String serverAddress, int serverPort, int pid) {
    this.pid = pid;
    this.serverAddress = serverAddress;
    this.serverPort = serverPort;
  }

  public CountResult count(String query) {
    try (Socket countServer = new Socket(serverAddress, serverPort);
         OutputStreamWriter out = new OutputStreamWriter(countServer.getOutputStream(),
             StandardCharsets.UTF_8);
         BufferedReader in = new BufferedReader(
             new InputStreamReader(countServer.getInputStream()))) {

      out.write(query, 0, query.length());
      out.flush();
      String response = in.readLine();
      if (response == null || response.isEmpty()) {
        throw new RuntimeException("The count server returned an null/empty string");
      } else if (response.contains("error")) {
        throw new RuntimeException("The count server returned an error: " + response);
      } else {
        // merge multiple whitespaces
        response = response.substring(1, response.length() - 1)
            .replaceAll("\\s+", " ");
//				System.out.println("[PCPCounter] result:\"" + response + "\"");
        String[] toks = response.split(" ");
        BigRational prob;
        BigRational var;

        if (toks[0].equals("exact:")) {
          prob = new BigRational(toks[1]);
          var = BigRational.ZERO;
        } else if (toks[0].equals("statistical:")) {
          prob = new BigRational(toks[1]);
          var = new BigRational(toks[3]);
        } else {
          throw new RuntimeException("Unknown count type! " + response);
        }
        return new StatisticalCountResult(prob, var);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void close() {

  }

  public int getPID() {
    return pid;
  }
}
