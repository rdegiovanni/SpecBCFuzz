package fuzz.mutation;

import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

import fuzz.Fuzzer;
import fuzz.Seeds;
import ltl.gore.Spec;
import owl.ltl.Formula;

public class MutationFuzzer implements Fuzzer {

	private Seeds seeds;
	private double mutationProbability; 
	private int gaGuaranteesPreferenceFactor; 
	private boolean onlyInputsInAssumptions;
	private int gaGeneNumOfMutations;
	
	public MutationFuzzer(Seeds seeds, double mutationProbability, int gaGuaranteesPreferenceFactor, boolean onlyInputsInAssumptions, int gaGeneNumOfMutations) {
		this.seeds = seeds;
		this.mutationProbability = mutationProbability;
		this.gaGuaranteesPreferenceFactor = gaGuaranteesPreferenceFactor;
		this.onlyInputsInAssumptions = onlyInputsInAssumptions;
		this.gaGeneNumOfMutations = gaGeneNumOfMutations;
	}
	
	@Override
	public Pair<Formula,List<String>> fuzz() {
		Spec spec = seeds.next().getLeft().clone();
		SpecificationMutation mutation = new SpecificationMutation(this.mutationProbability, this.gaGuaranteesPreferenceFactor, 
				this.onlyInputsInAssumptions, this.gaGeneNumOfMutations, new Random());
		Pair<Formula,List<String>> pair = Pair.of(mutation.mutate(spec).toFormula().formula(),spec.getVariables());
		//spec = null;
		return pair;
	}

}
