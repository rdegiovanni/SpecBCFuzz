package sat;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import owl.ltl.Formula;

public interface SATSolver {
	
	public SolverResult isSAT(Pair<Formula,List<String>> formulaAndVariables, int timeout);
	
	public SolverResult isSAT(Pair<Formula,List<String>> formulaAndVariables, int timeout, int repeatThresold);
	
	public SolverResult isSAT(Pair<Formula,List<String>> formulaAndVariables, int timeout, int repeatStart, int repeatThresold);
	
	public SolverResult isSAT(Formula formula, int timeout);
	
	public SolverResult isSAT(Formula formula, int timeout, int repeatThresold);
	
	public SolverResult isSAT(Formula formula, int timeout, int repeatStart, int repeatThresold);

	public double executionTime();
	
	public String getName();
	
}
