package fuzz.searchbased.acore.optimization.solvers;

public class LTLSolver {

	public static enum SolverResult {
		SAT,
		UNSAT,
		TIMEOUT,
		ERROR;
		
		public boolean inconclusive () { return this == TIMEOUT || this == ERROR; }
	}
	
}
