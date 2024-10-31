package fuzz.searchbased;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import experiment.ExperimentSetting;
import experiment.runner.Experiment;
import fuzz.Fuzzer;
import fuzz.Seeds;
import fuzz.searchbased.acore.optimization.runners.AlgorithmRunner;
import fuzz.searchbased.acore.optimization.runners.NSGAIIIRunner;
import fuzz.searchbased.acore.optimization.specification.SpecificationSetting;
import fuzz.searchbased.acore.optimization.specification.SpecificationSolution;
import ltl.gore.Spec;
import owl.ltl.Formula;

public class SemSynSearchBasedFuzzer implements Fuzzer {

	private final int popSize;
	private final int maxNumOfInd;
	private final Seeds seeds;
	private List<SpecificationSolution> specs;
	private Experiment exp;
	
	public SemSynSearchBasedFuzzer(Seeds seeds, Experiment exp, int popSize, int maxNumOfInd) {
		this.seeds = seeds;
		this.exp = exp;
		this.popSize = popSize;
		this.maxNumOfInd = maxNumOfInd;
		this.specs = new ArrayList<SpecificationSolution>();
	}
	
	private void search() {
		Triple<Spec,List<Formula>,String> current = this.seeds.next();
		SpecificationSetting setting = SpecificationSetting.SemSynStatus;
		if (current.getMiddle() != null && !current.getMiddle().isEmpty()) {
			setting = SpecificationSetting.SemSynStatusBc;
		}
		System.out.println(setting.toString());
		AlgorithmRunner runner = new NSGAIIIRunner(this.popSize, this.maxNumOfInd, ExperimentSetting.GA_MUTATION_RATE, 
				ExperimentSetting.GA_CROSSOVER_RATE, ExperimentSetting.RANDOM_GENERATOR, ExperimentSetting.GA_GUARANTEES_PREFERENCE_FACTOR, 
				ExperimentSetting.only_inputs_in_assumptions, ExperimentSetting.GA_GENE_NUM_OF_MUTATIONS, 
				setting);
		this.specs = runner.run(current.getLeft().clone(), current.getMiddle(), this.exp);
	}
	
	private Spec getSpec() {
		if (this.specs == null || this.specs.isEmpty()) {
			search();
		}
		SpecificationSolution specSolution = this.specs.get(0);
		this.specs.remove(0);
		return specSolution.spec;
	}
	
	@Override
	public Pair<Formula, List<String>> fuzz() {
		Spec spec = getSpec();
		System.out.println("Fuzz()");
		return Pair.of(spec.toFormula().formula(), spec.getVariables());
	}

}