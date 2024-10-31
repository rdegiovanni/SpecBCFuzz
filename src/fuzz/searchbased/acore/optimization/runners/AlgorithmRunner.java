package fuzz.searchbased.acore.optimization.runners;

import java.util.List;

import experiment.runner.Experiment;
import fuzz.searchbased.acore.optimization.specification.SpecificationSolution;
import owl.ltl.Formula;
import ltl.gore.Spec;

public interface AlgorithmRunner {
	
	public List<SpecificationSolution> run(Spec originalSpec, Experiment exp);

	public List<SpecificationSolution> run(Spec originalSpec, List<Formula> boundaryConditions, Experiment exp);
	
	public List<SpecificationSolution> getSolutions();
	
	public List<Formula> getBoundaryConditions();
	
	public Spec getOriginalSpecification();
	
	public String printExecutionTime();
	
}
