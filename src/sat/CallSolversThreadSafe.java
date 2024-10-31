package sat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;

import owl.ltl.Formula;

public class CallSolversThreadSafe {
	
	private List<SATSolver> solvers;
	private int timeout;
	
	public CallSolversThreadSafe(List<SATSolver> solvers, int timeout) {
		this.solvers = solvers;
		this.timeout = timeout;
	}
	
	public List<Pair<String,SolverResult>> call(Pair<Formula,List<String>> formulaAndVariables) {
		KillAditionalProcess kill = new KillAditionalProcess();
		List<Pair<String,SolverResult>> results = new ArrayList<Pair<String,SolverResult>>();
		List<CallSolver> threads = new ArrayList<CallSolver>();
		CountDownLatch latch = new CountDownLatch(this.solvers.size());
		for (SATSolver solver : this.solvers) {
			//SolverResult result = solver.isSAT(formula, this.timeout, 0);
			final int noRepeat = 0;
			CallSolver call = new CallSolver(latch);
			call.setSolver(solver, this.timeout, noRepeat);
			call.setFormulaAndVariables(formulaAndVariables);
			threads.add(call);
			//System.out.println("Call: " + solver.getName());
			new Thread(call,solver.getName()).start();
		}
		boolean error = false;
		try {
			//System.out.println("Espera...");
			boolean await = latch.await((this.solvers.size() + 1) * this.timeout, TimeUnit.SECONDS);
			if (!await) {
				throw new InterruptedException("Call Solvers fall in await.");
			}
			//System.out.println("Colhe resultados e lanca novamente...");
		} catch (InterruptedException e) {
			e.printStackTrace();
			error = true;
		}
		kill.all();
		for (CallSolver caller : threads) {
			SolverResult solverResult = caller.getSolverResult();
			if (solverResult == null || error == true) {
				results.add(Pair.of(caller.getSolverName(),SolverResult.ERROR()));
			} else {
				results.add(Pair.of(caller.getSolverName(),solverResult));
			}
			//cleanUp
			caller = null;
		}
		return results;
	}
	
}