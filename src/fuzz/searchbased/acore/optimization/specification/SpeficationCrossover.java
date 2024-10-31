package fuzz.searchbased.acore.optimization.specification;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.util.errorchecking.JMetalException;

import experiment.ExperimentSetting;
import fuzz.searchbased.acore.ltl.owl.visitors.PropositionVariablesExtractor;
import fuzz.searchbased.acore.utils.Formula_Utils;
import ltl.gore.Spec;
import owl.ltl.BooleanConstant;
import owl.ltl.Formula;
import owl.ltl.Literal;

public class SpeficationCrossover implements CrossoverOperator<SpecificationSolution> {

	//private Random random = Settings.RANDOM_GENERATOR;
	private Random random;
		
	private int numberOfParents;
	
	private int numberOfChildren;
	
	private double crossoverProbability;
		
	private boolean onlyInputsInAssumptions;
	
	//private int gaGuaranteesPreferenceFactor = Settings.GA_GUARANTEES_PREFERENCE_FACTOR;
	private int gaGuaranteesPreferenceFactor;
	
	public SpeficationCrossover(double crossoverProbability, Random random, int gaGuaranteesPreferenceFactor, boolean onlyInputsInAssumptions) {
		/**
		if (numberOfChildren < 0) {
			throw new JMetalException("numberOfChildren is less then zero");
		}
		this.numberOfChildren = numberOfChildren;
		if (numberOfParents < 0) {
			throw new JMetalException("numberOfParents is less then zero");
		}
		this.numberOfParents = numberOfParents;
		**/
		if (crossoverProbability <= 0 && crossoverProbability >= 1) {
			throw new JMetalException("crossoverProbability is not [1]");
		}
		this.crossoverProbability = crossoverProbability;
		if (gaGuaranteesPreferenceFactor <= 0 && gaGuaranteesPreferenceFactor >= 100) {
			throw new JMetalException("gaGuaranteesPreferenceFactor is not [0,100]");
		}
		this.gaGuaranteesPreferenceFactor = gaGuaranteesPreferenceFactor;
		this.random = random;
		this.onlyInputsInAssumptions = onlyInputsInAssumptions;
	}
	
	private synchronized List<SpecificationSolution> clonePop(List<SpecificationSolution> source) {
		List<SpecificationSolution> clonedSource = new ArrayList<SpecificationSolution>();
		for (SpecificationSolution s: source) {
			clonedSource.add((SpecificationSolution) s.copy());
		}
		return clonedSource;
	}
	
	@Override
	public synchronized List<SpecificationSolution> execute(List<SpecificationSolution> source) {
		List<SpecificationSolution> result = new LinkedList<SpecificationSolution>();
		if (this.crossoverProbability <= (this.random.nextInt(100) + 1) ) {
			return clonePop(source);
		}
		// if the specifications will not lead us to a consistent specification, then do a random merge.
		int assumption_level = this.random.nextInt(3);
		int guarantee_level = this.random.nextInt(3);
		int random = this.random.nextInt(100);
		if (random >= this.gaGuaranteesPreferenceFactor)
			guarantee_level = 0;
		else
			assumption_level = 0;
		
		int numOfChromosomes = source.size();
		SpecificationSolution specSol = source.get(this.random.nextInt(numOfChromosomes));
		Spec spec = specSol.getSpec();
		Spec anotherSpec = source.get(this.random.nextInt(numOfChromosomes)).getSpec();

		List<Spec> mergedSpecs = apply(spec, anotherSpec, assumption_level,guarantee_level);
		mergedSpecs.addAll(apply(anotherSpec, spec, assumption_level,guarantee_level)); //TODO - new element
		for (Spec s : mergedSpecs) {
			result.add(new SpecificationSolution(s,specSol.getSetting()));
		}
		final int finalSize = 2;
		if (result.size() < finalSize) { //clean the set and does not apply the crossover.
			return clonePop(source);
		}
		assert(result.size() == 2);
		return result;
	}
	
	public synchronized List<Spec> apply(Spec spec0, Spec spec1, int assumption_level, int guarantee_level) {
		List<Spec> merged_specifications = new LinkedList<>();
		List<Formula> assumptionConjuncts = new LinkedList<Formula>();
		List<Formula> guaranteeConjuncts = new LinkedList<Formula>();
		List<Formula> assumesspec0 = new LinkedList<Formula>(spec0.getAssume());
		List<Formula> assumesspec1 = new LinkedList<Formula>(spec1.getAssume());
		
		if (assumption_level == 0) {
			// set assume
			if (this.random.nextBoolean())
				assumptionConjuncts.addAll(assumesspec0);
			else
				assumptionConjuncts.addAll(assumesspec1);
		}
		else if (assumption_level == 1) {
			// set assume
			//if the assumptions can be modified
			if (this.gaGuaranteesPreferenceFactor < 100) {
				assumptionConjuncts.addAll(selectRandomly(assumesspec0));
				for (Formula f : selectRandomly(assumesspec1))
					if (!assumptionConjuncts.contains(f))
						assumptionConjuncts.add(f);
			}
			else
				assumptionConjuncts.addAll(assumesspec0);
		}
		else { //level == 4 and by default
			// set assume
			//if assumptions can be modified
			if (this.gaGuaranteesPreferenceFactor < 100 && assumesspec0.size()>0 && assumesspec1.size()>0) {
				assumptionConjuncts.addAll(assumesspec0);
				int size = assumptionConjuncts.size();
				if (size >= 1) {
					Formula merge_ass0 = assumptionConjuncts.remove(this.random.nextInt(size ));
					Formula merge_ass1 = assumesspec1.get(this.random.nextInt(assumesspec1.size()));
					// merge ass0 and ass1
					if (merge_ass0 != null && merge_ass1 != null) {
						if (this.onlyInputsInAssumptions) {
							Set<Formula> subformulas = Formula_Utils.subformulas(merge_ass1);
							Set<Formula> to_remove = new LinkedHashSet<>();
							for (Formula f : subformulas) {
								PropositionVariablesExtractor prop_visitor = new PropositionVariablesExtractor();
								Set<Literal> props = f.accept(prop_visitor);
								for (Literal l : props) {
									if (l.getAtom() >= spec0.getNumberOfInputs()) {
										to_remove.add(f);
										break;
									}
								}
							}
							subformulas.removeAll(to_remove);
							if (!subformulas.isEmpty())
								merge_ass1 = (Formula)subformulas.toArray()[ExperimentSetting.RANDOM_GENERATOR.nextInt(subformulas.size())];
							else
								merge_ass1 = BooleanConstant.TRUE;
						}

						Formula merged_assumption = null;
						if (this.random.nextBoolean())
							merged_assumption = Formula_Utils.replaceSubformula(merge_ass0, merge_ass1);
						else {
							merged_assumption = Formula_Utils.combineSubformula(merge_ass0, merge_ass1);
						}
						if (merged_assumption != null && Formula_Utils.numOfTemporalOperators(merged_assumption) <= 2)
							assumptionConjuncts.add(merged_assumption);
					}
				}
			}
			else
				assumptionConjuncts.addAll(assumesspec0);
		}

		if (guarantee_level == 0) {
			// set guarantees
			if (this.random.nextBoolean())
				guaranteeConjuncts.addAll(spec0.getGoals());
			else
				guaranteeConjuncts.addAll(spec1.getGoals());
		}
		else if (guarantee_level == 1) {
			// set guarantee
			//if the guarantees can be modified
			if (this.gaGuaranteesPreferenceFactor > 0) {
				guaranteeConjuncts.addAll(selectRandomly(spec0.getGoals()));
				for (Formula f : selectRandomly(spec1.getGoals()))
					if (!guaranteeConjuncts.contains(f))
						guaranteeConjuncts.add(f);
			}
			else
				guaranteeConjuncts.addAll(spec0.getGoals());

		}
		else { //level == 4 and by default
			// set guarantee
			//if assumptions can be modified
			if (this.gaGuaranteesPreferenceFactor > 0 && spec0.getGoals().size()>0 && spec1.getGoals().size()>0) {
				guaranteeConjuncts.addAll(spec0.getGoals());
				int size_g = guaranteeConjuncts.size();
				if (size_g >= 1) {
					Formula merge_g0 = guaranteeConjuncts.remove(this.random.nextInt(size_g));
					Formula merge_g1 = spec1.getGoals().get(this.random.nextInt(spec1.getGoals().size()));
					// merge g0 and g1
					if (merge_g0 != null && merge_g1 != null) {
						Formula merged_g = null;
						if (this.random.nextBoolean())
							merged_g = Formula_Utils.replaceSubformula(merge_g0, merge_g1);
						else {
							merged_g = Formula_Utils.combineSubformula(merge_g0, merge_g1);
						}
						if (merged_g != null && Formula_Utils.numOfTemporalOperators(merged_g) <= 2)
							guaranteeConjuncts.add(merged_g);
					}
				}
			}
			else
				guaranteeConjuncts.addAll(spec0.getGoals());
		}
		if (!guaranteeConjuncts.isEmpty()) {
			Spec new_spec = new Spec(assumptionConjuncts, guaranteeConjuncts, spec0.getIns(), spec0.getOuts());
			merged_specifications.add(new_spec);
			
			assert(assumptionConjuncts.size() == spec0.getAssume().size() && 
					assumptionConjuncts.size() == spec1.getAssume().size());
			for (int i = 0; i < assumptionConjuncts.size(); ++i) {
				assert(assumptionConjuncts.get(i).equals(spec0.getAssume().get(i)) ||
						assumptionConjuncts.get(i).equals(spec1.getAssume().get(i)));
			}
			
		}
		
		/*
		boolean noone = false;
		List<Spec> notkillable_merged_spec = new ArrayList<Spec>();
		
		for (Spec merged_spec : merged_specifications) {
			if (!Killable.is(merged_spec)) {
				notkillable_merged_spec.add(merged_spec);
				noone = true;
				continue;
			}
		}
		if (noone) {
			return apply(spec0, spec1, assumption_level, (guarantee_level + 1) % 2); //killed crossover!
		}
		return notkillable_merged_spec;
		*/
		
		//assert(merged_specifications.size() != 0);
		
		return merged_specifications;
	}

	private synchronized List<Formula> selectRandomly(List<Formula> formulas) {
		List<Formula> selectedFormulas = new LinkedList<Formula>();
		for (Formula f : formulas) {
			if (this.random.nextBoolean() && f != BooleanConstant.TRUE && !selectedFormulas.contains(f))
				selectedFormulas.add(f);
		}
		return selectedFormulas;
	}
	
	@Override
	public synchronized double getCrossoverProbability() {
		return this.crossoverProbability;
	}

	@Override
	public synchronized int getNumberOfRequiredParents() {
		throw new JMetalException("numberOfRequiredParents was called");
		//return this.numberOfParents;
	}

	@Override
	public synchronized int getNumberOfGeneratedChildren() {
		throw new JMetalException("numberOfChildren was called");
		//return this.numberOfChildren;
	}

}
