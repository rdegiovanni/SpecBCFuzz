package experiment.runner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import experiment.ExperimentLog;
import fuzz.Fuzzer;
import owl.ltl.Formula;
import sat.CallSolver;
import sat.CallSolversThreadSafe;
import sat.SATSolver;
import sat.SolverResult;

public class Experiment {
	
	private List<SATSolver> solvers;
	private Fuzzer fuzzer;
	private int timeout;
	private List<ExperimentLog> logs;
	private int id;
	private boolean enableThread;
	
	public Experiment(List<SATSolver> solvers, Fuzzer fuzzer, int timeout, ExperimentLog log, boolean enableThread) {
		List<ExperimentLog> logs = new ArrayList<ExperimentLog>();
		logs.add(log);
		new Experiment(solvers,fuzzer,timeout,logs,enableThread);
	}
	
	public Experiment(List<SATSolver> solvers, Fuzzer fuzzer, int timeout, List<ExperimentLog> logs, boolean enableThread) {
		this.solvers = solvers;
		this.fuzzer = fuzzer;
		this.timeout = timeout;
		this.logs = logs;
		this.id = 0;
		this.enableThread = enableThread;
	}
	
	public List<Pair<String,SolverResult>> run(Pair<Formula,List<String>> formulaAndVariables) {
		List<Pair<String,SolverResult>> results = callSolvers(formulaAndVariables);
		saveLog(formulaAndVariables.getLeft(), formulaAndVariables.getRight(), results);
		++this.id;
		return results;
	}
	
	public void run(int threshold) {
		for (int i = 0; i < threshold; ++i) {
			this.id = i;
			Pair<Formula,List<String>> formulaAndVariables = fuzzer.fuzz();
			if (formulaAndVariables != null) {
				List<Pair<String,SolverResult>> results = callSolvers(formulaAndVariables);
				Formula f = formulaAndVariables.getLeft();
				List<String> vars = formulaAndVariables.getRight();
				saveLog(f, vars, results);
				//cleanUp
				f = null;
				vars.clear();
				vars = null;
				results = null;
			} else {
				--i;
			}
		}
	}
	
	public List<Pair<String,SolverResult>> callSolvers(Pair<Formula,List<String>> formulaAndVariables) {
		List<Pair<String,SolverResult>> results = new ArrayList<Pair<String,SolverResult>>();
		if (this.enableThread) {
			CallSolversThreadSafe callSolvers = new CallSolversThreadSafe(this.solvers, this.timeout);
			results = callSolvers.call(formulaAndVariables);
			//cleanUp
			callSolvers = null;
		} else {
			for (SATSolver solver : this.solvers) {
				String name = solver.getName();
				final int noRepeat = 0;
				CallSolver callSolver = new CallSolver();
				callSolver.setSolver(solver, this.timeout, noRepeat);
				callSolver.setFormulaAndVariables(formulaAndVariables);
				callSolver.run();
				
				results.add(Pair.of(name,callSolver.getSolverResult()));
				//cleanUp
				callSolver = null;
				
				//System.out.println(name);
				//System.out.println(callSolver.getSolverResult().toString());
				/*
				try {
					SolverResult result = solver.isSAT(formulaAndVariables, this.timeout);
					results.add(Pair.of(name,result));
				} catch (Exception e) {
					results.add(Pair.of(name,SolverResult.ERROR));
				}
				*/
			} 
		}
		return results;
	}
	
	public List<Pair<String,SolverResult>> callSolvers(Formula formula) {
		List<Pair<String,SolverResult>> results = new ArrayList<Pair<String,SolverResult>>();
		for (SATSolver solver : this.solvers) {
			String name = solver.getName();
			SolverResult result = solver.isSAT(formula, this.timeout, 0);
			results.add(Pair.of(name,result));
		}
		return results;
	}
	
	/*
	public void saveLog(Formula formula, List<Pair<String,SolverResult>> results) {
		for (ExperimentLog log : this.logs) {
			try {
				log.persist(formula, this.id, results);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	*/
	
	public void saveLog(Formula formula, List<String> variables, List<Pair<String,SolverResult>> results) {
		for (ExperimentLog log : this.logs) {
			try {
				if (log.getVariableEnabled()) {
					log.persist(formula, variables, this.id, results);
				} else {
					log.persist(formula, this.id, results);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}