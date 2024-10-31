package fuzz.searchbased.acore.optimization.specification;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.uma.jmetal.operator.mutation.MutationOperator;

import fuzz.searchbased.acore.optimization.specification.SpecificationSolution.SPEC_STATUS;
import fuzz.searchbased.acore.ltl.owl.visitors.FormulaMutator;
import fuzz.searchbased.acore.ltl.owl.visitors.FormulaStrengthening;
import fuzz.searchbased.acore.ltl.owl.visitors.FormulaWeakening;
import fuzz.searchbased.acore.ltl.owl.visitors.GeneralFormulaMutator;
import fuzz.searchbased.acore.ltl.owl.visitors.SubformulaReplacer;
import fuzz.searchbased.acore.utils.Formula_Utils;
import ltl.gore.Spec;
import owl.ltl.BooleanConstant;
import owl.ltl.Formula;

public class SpecificationMutation implements MutationOperator<SpecificationSolution> {
	
	private Random random;
	
	private double mutationProbability;
	
	private int gaGuaranteesPreferenceFactor;
	
	private boolean onlyInputsInAssumptions;
	
	private int gaGeneNumOfMutations;
	
	public SpecificationMutation(double mutationProbability, Random random, int gaGuaranteesPreferenceFactor, boolean onlyInputsInAssumptions,
			int gaGeneNumOfMutations) {
		this.mutationProbability = mutationProbability;
		this.random = random;
		this.gaGuaranteesPreferenceFactor = gaGuaranteesPreferenceFactor;
		this.onlyInputsInAssumptions = onlyInputsInAssumptions;
		this.gaGeneNumOfMutations = gaGeneNumOfMutations;
	}

	public synchronized Spec mutate(Spec spec, SpecificationSolution.SPEC_STATUS status) {
		//System.out.println("MUTATE");
		
		//create empty specification
		//TODO - Change this to the NSGA-III
		//Spec new_spec = new Spec(spec.getAssume(), spec.getGoals(), spec.getIns(), spec.getOuts());
		Spec new_spec = spec;
		
		int random = this.random.nextInt(100);
		if (random >= this.gaGuaranteesPreferenceFactor) {
			// mutate assumptions
			List<Formula> assumptions = new LinkedList<Formula>(spec.getAssume());
			if (assumptions.isEmpty())
				assumptions.add(BooleanConstant.TRUE);
			int index_to_mutate = this.random.nextInt(assumptions.size());
			Formula assumption_to_mutate = assumptions.get(index_to_mutate);
			
			List<String> vars = new LinkedList<String>(spec.getVariables());
			if (this.onlyInputsInAssumptions)
				vars = vars.subList(0,spec.getNumberOfInputs());

			//select subformula to mutate
			Set<Formula> subformulas = Formula_Utils.subformulas(assumption_to_mutate);
			int n = subformulas.size();
			Formula to_mutate = (Formula) subformulas.toArray()[this.random.nextInt(n)];


			Formula mutated_subformula = BooleanConstant.TRUE;
			int modification =  this.random.nextInt(3);
			if (modification == 0) {
				// arbitrary mutation
				mutated_subformula = applyGeneralMutation(to_mutate,vars);
			}
			else if (modification == 1) {
				// weaken mutation
				mutated_subformula = weakenFormula(to_mutate, vars);
			}
			else {
				// strengthen mutation
				mutated_subformula = strengthenFormula(to_mutate, vars);
			}
			SubformulaReplacer visitor = new SubformulaReplacer(to_mutate,mutated_subformula);
			Formula new_assumption = assumption_to_mutate.accept(visitor);

			if (new_assumption != BooleanConstant.FALSE) {
				assumptions.remove(index_to_mutate);
				assumptions.add(index_to_mutate, new_assumption);
				new_spec.setAssume(assumptions);
			}
		}
		else {
			List<Formula> guarantees = Formula_Utils.splitConjunctions(spec.getGoals());
			if (guarantees.isEmpty())
				guarantees.add(BooleanConstant.TRUE);
			int index_to_mutate = this.random.nextInt(guarantees.size());
			Formula guarantee_to_mutate = guarantees.get(index_to_mutate);

			//select subformula to mutate
			Set<Formula> subformulas = Formula_Utils.subformulas(guarantee_to_mutate);
			int n = subformulas.size();
			Formula to_mutate = (Formula) subformulas.toArray()[this.random.nextInt(n)];

			Formula mutated_subformula = BooleanConstant.TRUE;
			int modification =  this.random.nextInt(3);
			if (modification == 0) {
				// arbitrary mutation
				mutated_subformula = strengthenFormula(to_mutate, spec.getVariables());
			}
			else if (modification == 1){
				// weaken mutation
				mutated_subformula = weakenFormula(to_mutate, spec.getVariables());
			}
			else {
				// weaken mutation
				mutated_subformula = applyGeneralMutation(to_mutate, spec.getVariables());
			}

			SubformulaReplacer visitor = new SubformulaReplacer(to_mutate,mutated_subformula);
			Formula new_guarantee = guarantee_to_mutate.accept(visitor);
			
			if (new_guarantee != BooleanConstant.FALSE) {
				guarantees.remove(index_to_mutate);
				guarantees.add(index_to_mutate, new_guarantee);
				new_spec.setGuarantees(guarantees);
			}
		}
		
		/**
		if (Killable.is(new_spec)) {
			return mutate(spec, status);
		}
		**/
		
		assert(spec.getAssume().size() == new_spec.getAssume().size());
		for (int i = 0; i < spec.getAssume().size(); ++i) {
			assert(spec.getAssume().get(i).equals(new_spec.getAssume().get(i)));
		}
		
		return new_spec;
	}

	public synchronized Formula applyGeneralMutation (Formula f, List<String> variables) {
		int n = Formula_Utils.formulaSize(f);
		int MR = Math.max(1, (int) (((100 - this.gaGeneNumOfMutations) / 100) * n));
		int num_of_mut = n;
		if (this.gaGeneNumOfMutations>0)
			num_of_mut = Math.min(n, this.gaGeneNumOfMutations);
		GeneralFormulaMutator formVisitor = new GeneralFormulaMutator(variables, MR, num_of_mut);
		Formula m = f.nnf().accept(formVisitor);
		return m;
	}

	public synchronized Formula mutateFormula (Formula f, List<String> variables) {
		int n = Formula_Utils.formulaSize(f);
		int MR = Math.max(1, (int) (((100 - this.gaGeneNumOfMutations) / 100) * n));
		int num_of_mut = n;
		if (this.gaGeneNumOfMutations >0)
			num_of_mut = Math.min(n, this.gaGeneNumOfMutations);
		FormulaMutator formVisitor = new FormulaMutator(variables, MR, num_of_mut);
		Formula m = f.nnf().accept(formVisitor);
		return m;
	}

	public synchronized Formula weakenFormula (Formula f, List<String> variables) {
		int n = Formula_Utils.formulaSize(f);
		int MR = Math.max(1, (int) (((100 - this.gaGeneNumOfMutations) / 100) * n));
		int num_of_mut = n;
		if (this.gaGeneNumOfMutations>0)
			num_of_mut = Math.min(n, this.gaGeneNumOfMutations);
		FormulaWeakening formVisitor = new FormulaWeakening(variables, MR, num_of_mut);
		Formula m = f.nnf().accept(formVisitor);
		return m;
	}

	public synchronized Formula strengthenFormula (Formula f, List<String> variables) {
		int n = Formula_Utils.formulaSize(f);
		int MR = Math.max(1, (int) (((100 - this.gaGeneNumOfMutations) / 100) * n));
		int num_of_mut = n;
		if (this.gaGeneNumOfMutations>0)
			num_of_mut = Math.min(n, this.gaGeneNumOfMutations);
		FormulaStrengthening formVisitor = new FormulaStrengthening(variables, MR, num_of_mut);
		Formula m = f.nnf().accept(formVisitor);
		return m;
	}
	
	@Override
	public synchronized SpecificationSolution execute(SpecificationSolution source) {
		//clone the current specification
		Spec mutated_spec = mutate(source.spec, source.getStatus());
		if (mutated_spec == null)
			return null;
		SpecificationSolution mutated_chromosome = new SpecificationSolution(mutated_spec,source.getSetting());
		//TODO - do better
		source.spec = mutated_chromosome.spec;
		source.clearFitness(); //consider remove this when change the algorithm.
		mutated_chromosome.clearFitness();
		return mutated_chromosome;
	}

	@Override
	public synchronized double getMutationProbability() {
		return this.mutationProbability;
	}

}
