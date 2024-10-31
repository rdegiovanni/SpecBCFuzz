package experiment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import owl.ltl.Formula;
import sat.SolverResult;

public class ExperimentLog {

	public enum CriticalityLevel {
		ALL,
		ALL_DIFFS,
		ONLY_SAT_AND_UNSAT_DIFFS;
	}
	
	private boolean logHeadWasPrinted;
	
	private CriticalityLevel level;
	
	private File logOutput;
	
	private boolean variableEnabled;
	
	public ExperimentLog(CriticalityLevel level, File logOutput) {
		new ExperimentLog(level,logOutput,false);
	}
	
	public ExperimentLog(CriticalityLevel level, File logOutput, boolean variable) {
		this.level = level;
		this.logHeadWasPrinted = false;
		this.logOutput = logOutput;
		this.variableEnabled = variable;
	}
	
	public static List<ExperimentLog> build(File log) {
		return build(log,false);
	}
	
	public static List<ExperimentLog> build(File log, boolean variableEnabled) {
		//LOG
		ExperimentLog all = new ExperimentLog(ExperimentLog.CriticalityLevel.ALL, 
				new File(log.getAbsoluteFile() + File.separator + "log_all.csv"),
				variableEnabled);
		ExperimentLog allDiffs = new ExperimentLog(ExperimentLog.CriticalityLevel.ALL_DIFFS, 
				new File(log.getAbsoluteFile() + File.separator + "log_all_diffs.csv"),
				variableEnabled);
		ExperimentLog onlySatAndUnsat = new ExperimentLog(ExperimentLog.CriticalityLevel.ONLY_SAT_AND_UNSAT_DIFFS, 
				new File(log.getAbsoluteFile() + File.separator + "log_only_sat_and_unsat_diffs.csv"),
				variableEnabled);
		List<ExperimentLog> logs = new ArrayList<ExperimentLog>();
		logs.add(all);
		logs.add(allDiffs);
		logs.add(onlySatAndUnsat);
		return logs;
	}

	public String getConsistencyStatus(List<Pair<String,SolverResult>> results) {
		String consistency = "";
		boolean sameResult = true;
		boolean firstLoop = true;
		SolverResult previousSolverResult = null;
		SolverResult[] status = new SolverResult[results.size()];
		int i = 0;
		for (Pair<String,SolverResult> currentResult : results) {
			status[i] = currentResult.getRight();
			++i;
			if (firstLoop) {
				firstLoop = false;
			} else {
				if (!currentResult.getRight().toString().equals(
						previousSolverResult.toString())) {
					sameResult = false;
				}
			}
			previousSolverResult = currentResult.getRight();
		}
		if (sameResult) {
			consistency = previousSolverResult.toString();
		} else {
			consistency = "DIFF";
			boolean sat = false;
			boolean unsat = false;
			for (int j = 0; j < status.length; ++j) {
				if (status[j].equals(SolverResult.SAT())) {
					sat = true;
				} else if (status[j].equals(SolverResult.UNSAT())) {
					unsat = true;
				}
			}
			if (sat && unsat) {
				consistency += "-SAT-UNSAT";
			}
		}
		return consistency;
	}
	
	/*
	public String getConsistencyStatus(List<Pair<String,SolverResult>> results) {
		String consistency = "";
		boolean sameResult = true;
		boolean firstLoop = true;
		SolverResult previousSolverResult = null;
		for (Pair<String,SolverResult> currentResult : results) {
			if (firstLoop) {
				previousSolverResult = currentResult.getRight();
				firstLoop = false;
			} else {
				if (currentResult.getRight().equals(previousSolverResult)) {
					previousSolverResult = currentResult.getRight();
				} else {
					sameResult = false;
					consistency = "DIFF";
					break;
				}
			}
		}
		if (sameResult) {
			consistency = previousSolverResult.toString();
		}
		return consistency;
	}*/
	
	private String getVariables(List<String> variables) {
		if (variables == null) {
			return "";
		}
		String vars = "";
		boolean first = false;
		for (String var : variables) {
			if (first) {
				first = true;
				vars += var;
			} else {
				vars += "#" + var;
			}
		}
		return vars;
	}
	
	private void printHead() throws IOException {
		if (!this.logHeadWasPrinted) {
			this.logHeadWasPrinted = true;
			String variables = this.variableEnabled ? "variables," : "";
			String head = "tool,formula," + variables + "formula_id,output,time";
			FileUtils.write(this.logOutput, head + System.lineSeparator(), "UTF-8", true);
		}
	}
	
	private void printSummary(Formula formula, List<String> variables, int id, List<Pair<String,SolverResult>> results, String consistency) throws IOException {
		FileUtils.write(this.logOutput, "all" + "," + formula + "," + getVariables(variables) + "," + id + "," + getConsistencyStatus(results) + "," + "no-time"
			+ System.lineSeparator(), 
				"UTF-8", true);
	}
	
	private void print(Formula formula, List<String> variables, int id, List<Pair<String,SolverResult>> results, String consistency) throws IOException {
		printSummary(formula,variables,id,results,consistency);
		for (Pair<String,SolverResult> result : results) {
			FileUtils.write(this.logOutput, result.getLeft() + "," + result.getRight().formula() + "," + getVariables(variables) + "," + id + "," + result.getRight() + "," + result.getRight().time()
				+ System.lineSeparator(), 
					"UTF-8", true);
		}
	}
	
	public boolean getVariableEnabled() {
		return this.variableEnabled;
	}
	
	public void persist(Formula formula, List<String> variables, int id, List<Pair<String,SolverResult>> results) throws IOException {
		if (this.variableEnabled) {
			if (variables == null || variables.isEmpty()) {
				throw new IOException("Inform the variables.");
			}
		} else {
			variables = null;
		}
		try {
			printHead();
			String consistency = getConsistencyStatus(results);
			if (this.level == CriticalityLevel.ALL) {
				print(formula,variables,id,results,consistency);
			} else if (this.level == CriticalityLevel.ALL_DIFFS) {
				if (consistency.startsWith("DIFF")) {
					print(formula,variables,id,results,consistency);
				}
			} else if (this.level == CriticalityLevel.ONLY_SAT_AND_UNSAT_DIFFS) {
				if (consistency.equals("DIFF-SAT-UNSAT")) {
					print(formula,variables,id,results,consistency);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void persist(Formula formula, int id, List<Pair<String,SolverResult>> results) throws IOException {
		if (this.variableEnabled) {
			throw new IOException("Log with variables was enabled.");
		}
		persist(formula, null, id, results);
	}
	
}
