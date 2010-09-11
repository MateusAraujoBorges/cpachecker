package org.sosy_lab.cpachecker.fllesh.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.sosy_lab.cpachecker.fllesh.FlleShResult;

public class Experiment {
  
  private PrintWriter mWriter;
  
  public Experiment(String pFileName) {
    File lLogFile = new File(pFileName);
    
    if (lLogFile.exists()) {
      throw new RuntimeException();
    }
    
    try {
      lLogFile.createNewFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    
    try {
      mWriter = new PrintWriter(new BufferedWriter(new FileWriter(lLogFile)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    
    mWriter.println("Experiment;Number of Test Goals;Number of Feasible Test Goals;Number of Infeasible Test Goals;Number of Test Cases;Number of Inprecise Test Cases;Time [s];Time for feasible test goals[s];Time for infeasible test goals[s]");
  }
  
  public void addExperiment(String pName, int pNumberOfTestGoals, int pNumberOfFeasibleTestGoals, int pNumberOfInfeasibleTestGoals, int pNumberOfTestCases, int pNumberOfInpreciseTestCases, double pTime, double pTimeForFeasibleTestGoals, double pTimeForInfeasibleTestGoals) {
    mWriter.println(pName + ";"+ pNumberOfTestGoals + ";" + pNumberOfFeasibleTestGoals + ";" + pNumberOfInfeasibleTestGoals + ";" + pNumberOfTestCases + ";" + pNumberOfInpreciseTestCases + ";" + pTime + ";" + pTimeForFeasibleTestGoals + ";" + pTimeForInfeasibleTestGoals);
  }
  
  public void addExperiment(String pName, FlleShResult pResult, double pTime) {
    addExperiment(pName, pResult.getTask().getNumberOfTestGoals(), pResult.getNumberOfFeasibleTestGoals(), pResult.getNumberOfInfeasibleTestGoals(), pResult.getNumberOfTestCases(), pResult.getNumberOfImpreciseTestCases(), pTime, pResult.getTimeForFeasibleTestGoals(), pResult.getTimeForInfeasibleTestGoals());
  }
  
  public void close() {
    mWriter.close();
  }
  
}
