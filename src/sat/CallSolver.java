package sat;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.tuple.Pair;

import owl.ltl.Formula;

public class CallSolver extends Thread {

	private SolverResult result;
	private SATSolver solver;
	private int timeout;
	private int repeat;
	private Pair<Formula,List<String>> formulaAndVariables;
	private CountDownLatch latch;
	
	public CallSolver() {
		this.latch = null;
	}
	
	public CallSolver(CountDownLatch latch) {
		this.latch = latch;
	}
	
	public void setSolver(SATSolver solver, int timeout, int repeat) {
		this.solver = solver;
		this.timeout = timeout;
		this.repeat = repeat;
		this.result = SolverResult.ERROR();
	}
	
	public SolverResult getSolverResult() {
		SolverResult result = this.result;
		this.result = null;
		return result;
	}
	
	public void setFormulaAndVariables(Pair<Formula,List<String>> formulaAndVariables) {
		this.formulaAndVariables = formulaAndVariables;
	}
	
	public void run() {
		try {
			this.result = this.solver.isSAT(this.formulaAndVariables, this.timeout);
			/*
			System.out.println(this.solver.getName());
			System.out.println(this.result.formula());
			System.out.println(this.result.time());
			*/
		} catch (Exception e) {
			this.result = SolverResult.ERROR();
		}
		if (this.latch != null) {
			latch.countDown();
		}
	}

	public String getSolverName() {
		return this.solver.getName();
	}
	
}