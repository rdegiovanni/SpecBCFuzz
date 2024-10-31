package fuzz.searchbased.acore.optimization.specification;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.uma.jmetal.problem.AbstractGenericProblem;

import com.google.common.collect.Sets;

import ch.obermuhlner.math.big.BigDecimalMath;
import experiment.ExperimentSetting;
import experiment.runner.Experiment;
import fuzz.searchbased.acore.ltl.owl.visitors.GeneralFormulaMutator;
import fuzz.searchbased.acore.ltl.owl.visitors.SolverSyntaxOperatorReplacer;
import fuzz.searchbased.acore.ltl.owl.visitors.SubformulaReplacer;
import fuzz.searchbased.acore.modelcounter.EmersonLeiAutomatonBasedModelCounting;
import fuzz.searchbased.acore.optimization.specification.SpecificationSolution.SPEC_STATUS;
import fuzz.searchbased.acore.utils.Formula_Utils;
import ltl.gore.Spec;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.FOperator;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.LabelledFormula;
import owl.ltl.Literal;
import owl.ltl.rewriter.SyntacticSimplifier;
//import fuzz.searchbased.acore.solvers.LTLSolver;
//import fuzz.searchbased.acore.solvers.LTLSolver.SolverResult;
import sat.SolverResult;

public class SpecificationProblem extends AbstractGenericProblem<SpecificationSolution> {
	
	public Spec originalSpecification = null;
	public SPEC_STATUS originalStatus = SPEC_STATUS.UNKNOWN;
	public BigInteger originalNumOfModels;
	public List<Formula> boundaryConditions = new LinkedList<Formula>();
	private static HashMap<Formula, Double> originalLikelihood = new HashMap<Formula, Double>();
	private static HashMap<Formula, Double> refinedLikelihood = new HashMap<Formula, Double>();

	private List<SpecificationSolution> newPopulation = new ArrayList<SpecificationSolution>();
	private int newPopulationIndex = 0;
	
	private Experiment exp;
	
	public int repairCount = 0;
	public int evalCount = 0;
	
	private SpecificationSetting setting;
	
	public SpecificationProblem(Spec originalSpecification, SpecificationSetting setting, Experiment exp) throws IOException, InterruptedException {
		this.setting = setting;
		this.exp = exp;
		this.originalSpecification = originalSpecification;
		if (this.setting == SpecificationSetting.SemSynStatus) {
			super.setNumberOfObjectives(3);
		} else {
			super.setNumberOfObjectives(4);
		}
		SpecificationSolution originalChromosome = new SpecificationSolution(originalSpecification,this.setting);
		compute_status(originalChromosome);
		this.originalStatus = originalChromosome.status;
		if (ExperimentSetting.LOST_MODELS_FACTOR > 0.0d)
			originalNumOfModels = countModels(originalSpecification.toFormula());
	}
	
	public SpecificationProblem(Spec originalSpecification, List<Formula> bcs, Experiment exp) throws IOException, InterruptedException {
		super.setNumberOfObjectives(4);
		this.setting = SpecificationSetting.SemSynStatusBc;
		this.exp = exp;
		this.originalSpecification = originalSpecification;
		this.boundaryConditions.addAll(bcs);
	
		SpecificationSolution originalChromosome = new SpecificationSolution(originalSpecification,this.setting);
		
		compute_status(originalChromosome);
		//compute_originalLikelihood(originalSpecification, bcs);
		this.originalStatus = originalChromosome.status;
		
		if (ExperimentSetting.LOST_MODELS_FACTOR > 0.0d)
			originalNumOfModels = countModels(originalSpecification.toFormula());
		
		System.out.println("Initial specification is: " + originalStatus);
	}
	
	@Override
	public synchronized SpecificationSolution createSolution() {	
		//System.out.println("CREATE-SOLUTION");
		if (newPopulationIndex == 0) {
			this.newPopulation = createInitialPopulation(originalSpecification);
		}
		int size = this.newPopulation.size();
		SpecificationSolution newSolution = this.newPopulation.get(this.newPopulationIndex);
		this.newPopulationIndex = (this.newPopulationIndex + 1) % size;
		return newSolution;
	}
	
	private synchronized List<SpecificationSolution> createInitialPopulation(Spec spec) {
		List<SpecificationSolution> population = new ArrayList<SpecificationSolution>();
		//SpecificationSolution init = new SpecificationSolution(spec);
		//population.add(init);

		if (ExperimentSetting.allowAssumptionAddition && ExperimentSetting.GA_GUARANTEES_PREFERENCE_FACTOR < 100) {
			//add simple assumptions G F input
			for (int i = 0; i < spec.getNumberOfInputs(); i++) {
				Literal input = Literal.of(i);
				if (ExperimentSetting.RANDOM_GENERATOR.nextBoolean())
					input = input.not();
				Formula new_assumption = GOperator.of(FOperator.of(input));
				List<Formula> assumes = new LinkedList<Formula>(spec.getAssume());
				assumes.add(new_assumption);
				Spec input_spec = new Spec(assumes, spec.getGoals(), spec.getIns(), spec.getOuts());
//				input_spec.setBoundaryConditions(spec.getBoundaryConditions());
				population.add(new SpecificationSolution(input_spec, this.setting));
			}
			//add simple assumptions: //G(!(i_1 & i_2))
			List<Literal> inputs = new LinkedList<Literal>();
			for (int i = 0; i < spec.getNumberOfInputs(); i++) {
				inputs.add(Literal.of(i));
			}

			Formula new_assumption = GOperator.of(Conjunction.of(inputs).not());
			List<Formula> assumes = new LinkedList<Formula>(spec.getAssume());
			assumes.add(new_assumption);
			Spec input_spec = new Spec(assumes, spec.getGoals(), spec.getIns(), spec.getOuts());
//			input_spec.setBoundaryConditions(spec.getBoundaryConditions());
			population.add(new SpecificationSolution(input_spec, this.setting));

			//add simple assumptions: //GF (i_1 & i_2)
			new_assumption = GOperator.of(FOperator.of(Conjunction.of(inputs)));
			assumes = new LinkedList<Formula>(spec.getAssume());
			assumes.add(new_assumption);
			input_spec = new Spec(assumes, spec.getGoals(), spec.getIns(), spec.getOuts());
//			input_spec.setBoundaryConditions(spec.getBoundaryConditions());
			population.add(new SpecificationSolution(input_spec, this.setting));
		}

		//combine or replace sub formulas by one input
		if (ExperimentSetting.GA_GUARANTEES_PREFERENCE_FACTOR < 100) {
			for (Formula as : spec.getAssume()) {
				int i = ExperimentSetting.RANDOM_GENERATOR.nextInt(spec.getNumberOfInputs());
				Literal input = Literal.of(i);
				if (ExperimentSetting.RANDOM_GENERATOR.nextBoolean())
					input = input.not();
				Formula new_assumption = null;
				if (ExperimentSetting.RANDOM_GENERATOR.nextBoolean())
					new_assumption = Formula_Utils.replaceSubformula(as,input);
				else
					new_assumption = Formula_Utils.combineSubformula(as,input);
				List<Formula> assumes = new LinkedList<Formula>(spec.getAssume());
				assumes.remove(as);
				assumes.add(new_assumption);
				Spec input_spec = new Spec(assumes, spec.getGoals(), spec.getIns(), spec.getOuts());
//				input_spec.setBoundaryConditions(spec.getBoundaryConditions());
				population.add(new SpecificationSolution(input_spec, this.setting));
			}
		}

		// weaken some sub formula
		if (ExperimentSetting.GA_GUARANTEES_PREFERENCE_FACTOR < 100) {
			for (Formula as : spec.getAssume()) {
				Set<Formula> subformulas = Formula_Utils.subformulas(as);
				int n = subformulas.size();
				Formula to_replace = (Formula) subformulas.toArray()[ExperimentSetting.RANDOM_GENERATOR.nextInt(n)];
				List<String> variables = spec.getVariables();
				if (ExperimentSetting.only_inputs_in_assumptions)
					variables = variables.subList(0,spec.getNumberOfInputs());
				GeneralFormulaMutator formVisitor = new GeneralFormulaMutator(variables, n, 1);
				Formula mutated_subformula = to_replace.nnf().accept(formVisitor);
				SubformulaReplacer visitor = new SubformulaReplacer(to_replace,mutated_subformula);
				Formula mutated_assumption = as.accept(visitor);
				List<Formula> assumes = new LinkedList<Formula>(spec.getAssume());
				assumes.remove(as);
				assumes.add(mutated_assumption);
				Spec input_spec = new Spec(assumes, spec.getGoals(), spec.getIns(), spec.getOuts());
//				input_spec.setBoundaryConditions(spec.getBoundaryConditions());
				population.add(new SpecificationSolution(input_spec, this.setting));
			}
		}

		if (ExperimentSetting.GA_GUARANTEES_PREFERENCE_FACTOR > 0) {
			for (Formula g : spec.getGoals()) {
				int i = ExperimentSetting.RANDOM_GENERATOR.nextInt(spec.getVariables().size());//spec.numberOfInputs() + ExperimentSetting.RANDOM_GENERATOR.nextInt(spec.variables().size()-spec.numberOfInputs());
				Literal output = Literal.of(i);
				if (ExperimentSetting.RANDOM_GENERATOR.nextBoolean())
					output = output.not();
				Formula new_guarantee = null;
				if (ExperimentSetting.RANDOM_GENERATOR.nextBoolean())
					new_guarantee = Formula_Utils.replaceSubformula(g,output);
				else
					new_guarantee = Formula_Utils.combineSubformula(g,output);
				List<Formula> guarantees = new LinkedList<>(spec.getGoals());
				guarantees.remove(g);
				guarantees.add(new_guarantee);
				Spec input_spec = new Spec(spec.getAssume(), guarantees, spec.getIns(), spec.getOuts());
//				input_spec.setBoundaryConditions(spec.getBoundaryConditions());
				population.add(new SpecificationSolution(input_spec, this.setting));
			}
		}

		if (ExperimentSetting.GA_GUARANTEES_PREFERENCE_FACTOR > 0) {
			for (Formula g : spec.getGoals()) {
				Set<Formula> subformulas = Formula_Utils.subformulas(g);
				int n = subformulas.size();
				Formula to_replace = (Formula) subformulas.toArray()[ExperimentSetting.RANDOM_GENERATOR.nextInt(n)];
				List<String> variables = spec.getVariables();
				GeneralFormulaMutator formVisitor = new GeneralFormulaMutator(variables, n, 1);
				Formula mutated_subformula = to_replace.nnf().accept(formVisitor);
				SubformulaReplacer visitor = new SubformulaReplacer(to_replace,mutated_subformula);
				Formula mutated_guarantee = g.accept(visitor);
				List<Formula> guarantees = new LinkedList<>(spec.getGoals());
				guarantees.remove(g);
				guarantees.add(mutated_guarantee);
				Spec input_spec = new Spec(spec.getAssume(), guarantees, spec.getIns(), spec.getOuts());
//				input_spec.setBoundaryConditions(spec.getBoundaryConditions());
				population.add(new SpecificationSolution(input_spec, this.setting));
			}
		}
		
		/*
		List<SpecificationSolution> filteredPop = new ArrayList<SpecificationSolution>();
		for (SpecificationSolution offspring : population) {
			if (!offspring.getSpec().equals(this.originalSpecification)) {
				filteredPop.add(offspring);
			}
		}
		return filteredPop;
		*/
		return population;
	}
	
	@Override
	public synchronized SpecificationSolution evaluate(SpecificationSolution solution) {
		this.evalCount++;
		calculate(solution);
		return solution;
	}

	private synchronized void killChromosome(SpecificationSolution chromosome) {
		chromosome.status_fitness = 1;
		chromosome.bcGradeOfImprovement = 1;
		chromosome.removedBCLikelihood = 1;
		chromosome.survivalBCLikelihood = 1;
		chromosome.lost_models_fitness = 1;
		chromosome.won_models_fitness = 1;
		chromosome.syntactic_distance = 1;
	}
	
	public synchronized void calculate(SpecificationSolution chromosome) {
		// compute multi-objective fitness function
		if (this.setting == SpecificationSetting.SemSynStatusBc) {
			if (chromosome.status != SPEC_STATUS.UNKNOWN) {
				killChromosome(chromosome);
				return;
			}
			// remove trivial specifications
			if (chromosome.spec.getGoals().isEmpty()) {
				killChromosome(chromosome);
				return;
			}
			//if (originalSpecification.equals(chromosome.spec)) {
			//	killChromosome(chromosome);
			//	return;
			//}
		}
		
		if (Conjunction.of(chromosome.spec.getAssume()) == BooleanConstant.FALSE) {
			killChromosome(chromosome);
			return;
		}
		if (Conjunction.of(chromosome.spec.getGoals()) == BooleanConstant.TRUE) {
			killChromosome(chromosome);
			return;
		}
		if (SpecificationKillable.is(chromosome.spec)) {
			killChromosome(chromosome);
			return;
		}
		
		if (this.setting == SpecificationSetting.SemSynStatusBc) {
			if (!ExperimentSetting.allowGuaranteeRemoval || !ExperimentSetting.allowAssumptionAddition) {
				boolean somethingRemoved = somethingHasBeenRemoved(originalSpecification, chromosome.spec);
				if (somethingRemoved) {
					killChromosome(chromosome);
					return;
				}
			}
		}
//		System.out.println("this: "+ LabelledFormula.of(originalSpecification.getFormula(), originalSpecification.getVariables()));
//		System.out.println("that: "+ LabelledFormula.of(chromosome.spec.getFormula(), originalSpecification.getVariables()));

		try {

			compute_status(chromosome);
				
			double status_fitness = 0.0d;
			if (chromosome.status == SPEC_STATUS.UNKNOWN || chromosome.status == SPEC_STATUS.BOTTOM)
				status_fitness = 0.0d;
			else if (chromosome.status == SPEC_STATUS.GUARANTEES)
				status_fitness = 0.1d;
			else if (chromosome.status == SPEC_STATUS.ASSUMPTIONS)
				status_fitness = 0.2d;
			else if (chromosome.status == SPEC_STATUS.CONTRADICTORY)
				status_fitness = 0.5d;
			else if (chromosome.status == SPEC_STATUS.CONSISTENT)
				status_fitness = 1.0d;
				
			status_fitness = 1d - status_fitness;
			chromosome.status_fitness = status_fitness;

			// Compute syntactic distance
			//double syntactic_distance = 0.0d;
			double syntactic_distance = 1.0d;
			if (ExperimentSetting.SYNTACTIC_FACTOR > 0.0d && originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
				if (ExperimentSetting.LEVENSTEIN_ENABLE) {
					//syntactic_distance = compute_levenshtein_distance(originalSpecification, chromosome.spec);
					syntactic_distance = 1d - compute_levenshtein_distance(originalSpecification, chromosome.spec);
				} else {
					//syntactic_distance = compute_syntactic_distance(originalSpecification, chromosome.spec);
					syntactic_distance = 1d - compute_syntactic_distance(originalSpecification, chromosome.spec);
				}
				System.out.printf("s%.2f ", syntactic_distance);
			}

			// Second, compute the portion of loosing models with respect to the original specification
			double lost_models_fitness = 0.0d;  // if the current specification is inconsistent, then it looses all the models (it maintains 0% of models of the original specification)
			if (syntactic_distance < 1.0d &&  ExperimentSetting.LOST_MODELS_FACTOR > 0.0d && originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
				// if both specifications are consistent, then we will compute the percentage of models that are maintained after the refinement
				//lost_models_fitness = compute_lost_models_porcentage(originalSpecification, chromosome.spec);
				lost_models_fitness = 1d - compute_lost_models_porcentage(originalSpecification, chromosome.spec);
				System.out.print(String.format("%.2f ",lost_models_fitness));
			}

			// Third, compute the portion of winning models with respect to the original specification
			double won_models_fitness = 0.0d;
			if (syntactic_distance < 1.0d &&  ExperimentSetting.WON_MODELS_FACTOR > 0.0d && originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
				// if both specifications are consistent, then we will compute the percentage of models that are added after the refinement (or removed from the complement of the original specifiction)
				//won_models_fitness = compute_won_models_porcentage(originalSpecification, chromosome.spec);
				won_models_fitness = 1d - compute_won_models_porcentage(originalSpecification, chromosome.spec);
				System.out.print(String.format("%.2f ",won_models_fitness));
			}
			
			if (this.setting == SpecificationSetting.SemSynStatusBc) {
				// First, check the BC improvement percentage, which will guide the GA to delete all BCs
				double bcGradeOfImprovement = 1.0d;
				// Also compute the removed BC likelihood to remove the most likely ones. 
				double removedBCLikelihood = 1.0d;
				// if the domain has changed, the likelihood of the survival BC must be less or equal to the old ones. 
				double survivalBCLikelihood = 1.0d;
				
				if (originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
					computeSurvivalBC(chromosome);
					if (ExperimentSetting.IMP_FACTOR > 0.0d) {
						int amountOfOriginalBc = this.boundaryConditions.size();
						bcGradeOfImprovement = 1d - ((double) (amountOfOriginalBc - amountOfSurvivalBoundaryConditions(chromosome)) / amountOfOriginalBc);
						if (bcGradeOfImprovement < 0.01) {
							++this.repairCount;
							System.out.println("Repair count: " + this.repairCount + "(" + this.evalCount + ")");
						}
						//bcGradeOfImprovement = ((double) (amountOfOriginalBc - amountOfSurvivalBoundaryConditions(chromosome)) / amountOfOriginalBc);
						System.out.print(String.format("i%.2f ", bcGradeOfImprovement));
					}
					if (ExperimentSetting.BC_FACTOR > 0.0d) {
						removedBCLikelihood = 1d - likelihoodOfRemovedBC(chromosome);
						//removedBCLikelihood = likelihoodOfRemovedBC(chromosome);
						System.out.print(String.format("rl%.2f ", removedBCLikelihood));
					}
					// Secondly, if the domain has been changed, so the new BC must be less likely
					/*
					Formula thisAssumptions = Conjunction.of(originalSpecification.getAssume());
					Formula thatAssumptions = Conjunction.of(chromosome.spec.getAssume());
					if (ExperimentSetting.LIKELIHOOD_FACTOR  > 0.0d && !Formula_Utils.areEquivalent(thisAssumptions, thatAssumptions)) {
						//Compute the improvement likelihood difference
						survivalBCLikelihood = 1d - likelihoodDifference2(chromosome);
						//survivalBCLikelihood = likelihoodDifference2(chromosome);
						System.out.print(String.format("sl%.2f ", survivalBCLikelihood));
					}
					*/
			}
			
//			double survivalBCSeverity = severityDifference2(originalSpecification, chromosome.spec);
//			double sublen = Formula_Utils.subformulas(Conjunction.of(Conjunction.of(chromosome.spec.getAssume()), Conjunction.of(chromosome.spec.getGoals()))).size();
//			System.out.printf("%.3f %.3f %.3f %.3f ",bcGradeOfImprovement,survivalBCLikelihood,survivalBCSeverity,status_fitness );

			/**
			fitness = (ExperimentSetting.STATUS_FACTOR * status_fitness)
					+ (ExperimentSetting.IMP_FACTOR * bcGradeOfImprovement)
					+ (ExperimentSetting.BC_FACTOR * removedBCLikelihood)
//					+ 0.2d * removedBCLikelihood
//					- (ExperimentSetting.LIKELIHOOD_FACTOR * survivalBCLikelihood + ExperimentSetting.SEVERITY_FACTOR * survivalBCSeverity)
					+ (ExperimentSetting.LIKELIHOOD_FACTOR * survivalBCLikelihood)
					+ (ExperimentSetting.LOST_MODELS_FACTOR * lost_models_fitness) 
					+ (ExperimentSetting.WON_MODELS_FACTOR * won_models_fitness)
					+ (ExperimentSetting.SYNTACTIC_FACTOR * syntactic_distance);
			**/
			
				chromosome.bcGradeOfImprovement = bcGradeOfImprovement;
				chromosome.removedBCLikelihood = removedBCLikelihood;
				chromosome.survivalBCLikelihood = survivalBCLikelihood;
			}
			chromosome.lost_models_fitness = lost_models_fitness;
			chromosome.won_models_fitness = won_models_fitness;
			chromosome.syntactic_distance = syntactic_distance;
			
			double [] objectives = chromosome.objectives();
			
			//System.out.println(objectives[0] + " " + objectives[1] + " " + objectives[2] + " " + objectives[3]);
			
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		//chromosome.fitness = fitness;
		//return fitness;
	}
	

	public synchronized void _calculate(SpecificationSolution chromosome) {
		//System.out.println("CALCULATE");
		// compute multi-objective fitness function
		if (chromosome.status != SPEC_STATUS.UNKNOWN) {
			killChromosome(chromosome);
			return;
		}
		// remove trivial specifications
		if (chromosome.spec.getGoals().isEmpty()) {
			killChromosome(chromosome);
			return;
		}
		//if (originalSpecification.equals(chromosome.spec)) {
		//	killChromosome(chromosome);
		//	return;
		//}
		if (Conjunction.of(chromosome.spec.getAssume()) == BooleanConstant.FALSE) {
			killChromosome(chromosome);
			return;
		}
		if (Conjunction.of(chromosome.spec.getGoals()) == BooleanConstant.TRUE) {
			killChromosome(chromosome);
			return;
		}
		if (SpecificationKillable.is(chromosome.spec)) {
			killChromosome(chromosome);
			return;
		}
		
		if (!ExperimentSetting.allowGuaranteeRemoval || !ExperimentSetting.allowAssumptionAddition) {
			boolean somethingRemoved = somethingHasBeenRemoved(originalSpecification, chromosome.spec);
			if (somethingRemoved) {
				killChromosome(chromosome);
				return;
			}
		}
		
//		System.out.println("this: "+ LabelledFormula.of(originalSpecification.getFormula(), originalSpecification.getVariables()));
//		System.out.println("that: "+ LabelledFormula.of(chromosome.spec.getFormula(), originalSpecification.getVariables()));

		try {

			compute_status(chromosome);
			
			double status_fitness = 0.0d;
			if (chromosome.status == SPEC_STATUS.UNKNOWN || chromosome.status == SPEC_STATUS.BOTTOM)
				status_fitness = 0.0d;
			else if (chromosome.status == SPEC_STATUS.GUARANTEES)
				status_fitness = 0.1d;
			else if (chromosome.status == SPEC_STATUS.ASSUMPTIONS)
				status_fitness = 0.2d;
			else if (chromosome.status == SPEC_STATUS.CONTRADICTORY)
				status_fitness = 0.5d;
			else if (chromosome.status == SPEC_STATUS.CONSISTENT)
				status_fitness = 1.0d;
			
			status_fitness = 1d - status_fitness;
			
			// Compute syntactic distance
			//double syntactic_distance = 0.0d;
			double syntactic_distance = 1.0d;
			if (ExperimentSetting.SYNTACTIC_FACTOR > 0.0d && originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
				if (ExperimentSetting.LEVENSTEIN_ENABLE) {
					//syntactic_distance = compute_levenshtein_distance(originalSpecification, chromosome.spec);
					syntactic_distance = 1d - compute_levenshtein_distance(originalSpecification, chromosome.spec);
				} else {
					//syntactic_distance = compute_syntactic_distance(originalSpecification, chromosome.spec);
					syntactic_distance = 1d - compute_syntactic_distance(originalSpecification, chromosome.spec);
				}
				System.out.printf("s%.2f ", syntactic_distance);
			}

			// Second, compute the portion of loosing models with respect to the original specification
			double lost_models_fitness = 0.0d;  // if the current specification is inconsistent, then it looses all the models (it maintains 0% of models of the original specification)
			if (syntactic_distance < 1.0d &&  ExperimentSetting.LOST_MODELS_FACTOR > 0.0d && originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
				// if both specifications are consistent, then we will compute the percentage of models that are maintained after the refinement
				//lost_models_fitness = compute_lost_models_porcentage(originalSpecification, chromosome.spec);
				lost_models_fitness = 1d - compute_lost_models_porcentage(originalSpecification, chromosome.spec);
				System.out.print(String.format("%.2f ",lost_models_fitness));
			}

			// Third, compute the portion of winning models with respect to the original specification
			double won_models_fitness = 0.0d;
			if (syntactic_distance < 1.0d &&  ExperimentSetting.WON_MODELS_FACTOR > 0.0d && originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
				// if both specifications are consistent, then we will compute the percentage of models that are added after the refinement (or removed from the complement of the original specifiction)
				//won_models_fitness = compute_won_models_porcentage(originalSpecification, chromosome.spec);
				won_models_fitness = 1d - compute_won_models_porcentage(originalSpecification, chromosome.spec);
				System.out.print(String.format("%.2f ",won_models_fitness));
			}
			
			// First, check the BC improvement percentage, which will guide the GA to delete all BCs
			double bcGradeOfImprovement = 1.0d;
			// Also compute the removed BC likelihood to remove the most likely ones. 
			double removedBCLikelihood = 1.0d;
			// if the domain has changed, the likelihood of the survival BC must be less or equal to the old ones. 
			double survivalBCLikelihood = 1.0d;
			
			if (originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
				//computeSurvivalBC(chromosome);
				if (ExperimentSetting.IMP_FACTOR > 0.0d) {
					int amountOfOriginalBc = this.boundaryConditions.size();
					bcGradeOfImprovement = 1d - ((double) (amountOfOriginalBc - amountOfSurvivalBoundaryConditions(chromosome)) / amountOfOriginalBc);
					if (bcGradeOfImprovement < 0.01) {
						++this.repairCount;
						System.out.println("Repair count: " + this.repairCount + "(" + this.evalCount + ")");
					}
					//bcGradeOfImprovement = ((double) (amountOfOriginalBc - amountOfSurvivalBoundaryConditions(chromosome)) / amountOfOriginalBc);
					System.out.print(String.format("i%.2f ", bcGradeOfImprovement));
				}
				if (ExperimentSetting.BC_FACTOR > 0.0d) {
					removedBCLikelihood = 1d - likelihoodOfRemovedBC(chromosome);
					//removedBCLikelihood = likelihoodOfRemovedBC(chromosome);
					System.out.print(String.format("rl%.2f ", removedBCLikelihood));
				}
				// Secondly, if the domain has been changed, so the new BC must be less likely
				/*
				Formula thisAssumptions = Conjunction.of(originalSpecification.getAssume());
				Formula thatAssumptions = Conjunction.of(chromosome.spec.getAssume());
				if (ExperimentSetting.LIKELIHOOD_FACTOR  > 0.0d && !Formula_Utils.areEquivalent(thisAssumptions, thatAssumptions)) {
					//Compute the improvement likelihood difference
					survivalBCLikelihood = 1d - likelihoodDifference2(chromosome);
					//survivalBCLikelihood = likelihoodDifference2(chromosome);
					System.out.print(String.format("sl%.2f ", survivalBCLikelihood));
				}
				*/
			}
			
//			double survivalBCSeverity = severityDifference2(originalSpecification, chromosome.spec);
//			double sublen = Formula_Utils.subformulas(Conjunction.of(Conjunction.of(chromosome.spec.getAssume()), Conjunction.of(chromosome.spec.getGoals()))).size();
//			System.out.printf("%.3f %.3f %.3f %.3f ",bcGradeOfImprovement,survivalBCLikelihood,survivalBCSeverity,status_fitness );

			/**
			fitness = (ExperimentSetting.STATUS_FACTOR * status_fitness)
					+ (ExperimentSetting.IMP_FACTOR * bcGradeOfImprovement)
					+ (ExperimentSetting.BC_FACTOR * removedBCLikelihood)
//					+ 0.2d * removedBCLikelihood
//					- (ExperimentSetting.LIKELIHOOD_FACTOR * survivalBCLikelihood + ExperimentSetting.SEVERITY_FACTOR * survivalBCSeverity)
					+ (ExperimentSetting.LIKELIHOOD_FACTOR * survivalBCLikelihood)
					+ (ExperimentSetting.LOST_MODELS_FACTOR * lost_models_fitness) 
					+ (ExperimentSetting.WON_MODELS_FACTOR * won_models_fitness)
					+ (ExperimentSetting.SYNTACTIC_FACTOR * syntactic_distance);
			**/
			
			chromosome.status_fitness = status_fitness;
			chromosome.bcGradeOfImprovement = bcGradeOfImprovement;
			chromosome.removedBCLikelihood = removedBCLikelihood;
			chromosome.survivalBCLikelihood = survivalBCLikelihood;
			chromosome.lost_models_fitness = lost_models_fitness;
			chromosome.won_models_fitness = won_models_fitness;
			chromosome.syntactic_distance = syntactic_distance;
			
			double [] objectives = chromosome.objectives();
			
			//System.out.println(objectives[0] + " " + objectives[1] + " " + objectives[2] + " " + objectives[3]);
			
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		//chromosome.fitness = fitness;
		//return fitness;
	}
	
	public synchronized int amountOfSurvivalBoundaryConditions(SpecificationSolution spec) {
		return spec.survivalBC.size();
	}
	
	/*
	private synchronized double severityDifference2(Spec originalspec, Spec spec) throws IOException, InterruptedException {
		double refined_severity = 0.0d;
		double original_severity = 0.0d;
		double cumulative_difference = 0.0d;
		
		List<Formula> refined_goals = new LinkedList<Formula>();
		List<Formula> original_goals = new LinkedList<Formula>();
		refined_goals.addAll(spec.getGoals());
		original_goals.addAll(originalspec.getGoals());
		// Compute survival boundary conditions
		List<Formula> survivalBC = new LinkedList<Formula>();
		for (Formula bc : this.boundaryConditions) {
			if (spec.isBoundaryCondition(bc))
				survivalBC.add(bc);
		}
		// Calculate the cumulative severity in original specification
		for (Formula bc : survivalBC) {
			for (Formula g : originalspec.getGoals()) 
				original_severity += (this.likelihood(Conjunction.of(Conjunction.of(originalspec.getAssume()), g), bc));
		}
		
		// Calculate the cumulative severity in refined specification
		for (Formula bc : survivalBC) {
			for (Formula g : spec.getGoals()) 
				refined_severity += (this.likelihood(Conjunction.of(Conjunction.of(originalspec.getAssume()), g), bc));
		}
		
		// The new severity should be greater than the older, because regarding severity the smaller the number the worse...
		cumulative_difference = refined_severity - original_severity;
//		System.out.println("SV: "+ refined_severity + " " + original_severity);
		// Calculating the improvement percentage 
		if (cumulative_difference <= 0.0d) {
			return 0.0d; // don't have any improvement 
		}
		else {
			//if (!(original_severity == 0.0d)) 
				return cumulative_difference/original_severity;
			//else return 1.0d;	//all improvement
		}
	}
	*/
		
	public synchronized double likelihoodDifference2(SpecificationSolution spec) throws IOException, InterruptedException {
		double refined_likelihood = 0.0d;
		double original_likelihood = 0.0d;
		double cumulative_difference = 0.0d;
		
		for (Formula bc : spec.survivalBC) {
			refined_likelihood +=(this.likelihood(Conjunction.of(spec.spec.getAssume()), bc));
			original_likelihood += originalLikelihood.get(bc);
		}
		
		// The new likelihood should be lower than the older one, because regarding likelihood the smaller the number the better...
		cumulative_difference = original_likelihood - refined_likelihood;
//		System.out.println("LK: "+ original_likelihood + " " + refined_likelihood);
		
		// Calculating the improvement percentage 
		if (cumulative_difference <= 0.0d) {
			return 0.0d; // don't have any improvement 
		}
		else {
			//if (!(original_likelihood == 0.0d)) 
				return cumulative_difference/original_likelihood;
			//else return 1.0d;	//all improvement
		}
	}
		

	/**
	 * Method to compute the boundary condition likelihood in a certain domain.
	 * @param domain
	 * @param boundary_condition
	 * @return a double value representing the probability that a boundary condition appears in some domain is returned.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private synchronized double likelihood(Formula domain, Formula boundary_condition) throws IOException, InterruptedException {
		BigInteger domain_models = countModels(LabelledFormula.of(domain, originalSpecification.getVariables()));
		BigInteger BC_models = countModels(LabelledFormula.of(Conjunction.of(domain,boundary_condition), originalSpecification.getVariables()));
//		System.out.println("likelihood" + BC_models.doubleValue() +" / " + domain_models.doubleValue() + " " + new BigDecimal(BC_models).divide(new BigDecimal(domain_models), 3, RoundingMode.HALF_UP));
		if (domain_models.equals(BigInteger.ZERO))
			return 0.0d;
		else {
			BigDecimal quotient = new BigDecimal(BC_models).divide(new BigDecimal(domain_models), 3, RoundingMode.HALF_UP);
//			System.out.println("q: " + quotient.doubleValue());
			return quotient.doubleValue();
		}
	}

	private synchronized BigInteger countModels (LabelledFormula formula) throws IOException, InterruptedException {
		SyntacticSimplifier simp = new SyntacticSimplifier();
		SolverSyntaxOperatorReplacer visitor  = new SolverSyntaxOperatorReplacer();
		Formula simplified = formula.formula().accept(visitor).accept(simp);
		if (simplified == BooleanConstant.FALSE)
			return BigInteger.ZERO;
		LabelledFormula simp_formula = LabelledFormula.of(simplified, formula.variables());
		EmersonLeiAutomatonBasedModelCounting<Object> counter = new EmersonLeiAutomatonBasedModelCounting<Object>(simp_formula);
		BigInteger numOfModels = counter.count(ExperimentSetting.MC_BOUND);
		//return new BigInteger(numOfModels.toString());
		return numOfModels;
	}
				
	public synchronized void print_config() {
		System.out.println(String.format("status: %s, addA: %s, remG: %s", ExperimentSetting.STATUS_FACTOR, ExperimentSetting.allowAssumptionAddition,ExperimentSetting.allowGuaranteeRemoval));
	}
	
	private synchronized SolverResult solvers(Formula form, List<String> vars) {
		List<Pair<String,SolverResult>> results = this.exp.run(Pair.of(form,vars));
		SolverResult env_sat = results.get(0).getRight();
		return env_sat;
	}
	
	public synchronized void compute_status(SpecificationSolution chromosome) throws IOException, InterruptedException {
		Spec spec = chromosome.spec;
		SolverSyntaxOperatorReplacer visitor  = new SolverSyntaxOperatorReplacer();
		
//		if (chromosome.spec.getAssume().isEmpty() || chromosome.spec.getGoals().isEmpty()) {
//			chromosome.status = SPEC_STATUS.UNKNOWN;
//			return;
//		}
		
//		Formula assume =  Conjunction.of(chromosome.spec.getAssume());
//		Formula guarantee = Conjunction.of(chromosome.spec.getGoals());
//		
//		if (assume.equals(BooleanConstant.TRUE) || guarantee.equals(BooleanConstant.TRUE)) {
//			chromosome.status = SPEC_STATUS.UNKNOWN;
//			return;
//		}
			
		// check satisfiability in assumes
		Formula environment = Conjunction.of(spec.getAssume()).accept(visitor);
		//SolverResult env_sat = LTLSolver.isSAT(Formula_Utils.toSolverSyntax(environment));
		
		//SolverResult env_sat = LTLSolver.isSAT(Formula_Utils.toSolverSyntax(environment));
		SolverResult env_sat = solvers(environment, spec.getVariables());
		SPEC_STATUS status = SPEC_STATUS.UNKNOWN;
		
		if (!env_sat.inconclusive()) {
			// check sat in guarantees
			Formula system = Conjunction.of(spec.getGoals()).accept(visitor);
			SolverResult sys_sat = solvers(system, spec.getVariables());
			//SolverResult sys_sat = LTLSolver.isSAT(Formula_Utils.toSolverSyntax(system));
			
			if (!sys_sat.inconclusive()) {
				if (env_sat.equals(SolverResult.UNSAT()) && sys_sat.equals(SolverResult.UNSAT())) {
					status = SPEC_STATUS.BOTTOM;
				}
				else if (env_sat.equals(SolverResult.UNSAT())) {
					status = SPEC_STATUS.GUARANTEES;
				}
				else if (sys_sat.equals(SolverResult.UNSAT())) {
					status = SPEC_STATUS.ASSUMPTIONS;
				}
				else { 
					//env_sat == SolverResult.SAT && sys_sat == SolverResult.SAT

					//check if initial states and safety properties are consistent
					Formula env_sys = Conjunction.of(Conjunction.of(spec.getAssume()), Conjunction.of(spec.getGoals()));
					Formula env_sys2 = env_sys.accept(visitor);
					//SolverResult sat = LTLSolver.isSAT(Formula_Utils.toSolverSyntax(env_sys2));
					SolverResult sat = solvers(env_sys2,spec.getVariables());
					
					if (!sat.inconclusive()) {
						if (sat.equals(SolverResult.UNSAT()))
							status = SPEC_STATUS.CONTRADICTORY;
						else
							status = SPEC_STATUS.CONSISTENT;
					}
				}
			}
		}
		chromosome.status = status;			
	}
	
	public synchronized double compute_levenshtein_distance(Spec original, Spec refined) {
		LevenshteinDistance leve = new LevenshteinDistance();
		String originalStr = original.toFormula().toString();
		String refinedStr = refined.toFormula().toString();
		double dist = (double) leve.apply(originalStr, refinedStr);
		double max = 0;
		if (originalStr.length() > refinedStr.length()) {
			max = (double) originalStr.length();
		} else {
			max = (double) refinedStr.length();
		}
		return (max - dist) / max;
	}
	
	public synchronized double compute_syntactic_distance(Spec original, Spec refined) {
		List<LabelledFormula> sub_original = Formula_Utils.subformulas(original.toFormula());
		List<LabelledFormula> sub_refined = Formula_Utils.subformulas(refined.toFormula());
		Set<LabelledFormula> commonSubs = Sets.intersection(Sets.newHashSet(sub_original), Sets.newHashSet(sub_refined));
		
		double lost = ((double) commonSubs.size()) / ((double) sub_original.size());
		double won = ((double) commonSubs.size()) / ((double) sub_refined.size());

		double syntactic_distance = 0.5d * lost + 0.5d * won;
		return syntactic_distance;
	}
	
	public synchronized double compute_lost_models_porcentage(Spec original, Spec refined) throws IOException, InterruptedException {
		System.out.print("-");
		
		if (originalNumOfModels == BigInteger.ZERO)
			return 0.0d;
		
		List<Formula> domainAndBcs = new ArrayList<Formula>();
		System.out.println(setting.toString());
		if (this.setting == SpecificationSetting.SemSynStatusBc) {
			System.out.println(setting.toString());
			Formula bcs = Conjunction.of(boundaryConditions.get(0).not());
			for (int i = 1; i < boundaryConditions.size(); ++i) {
				bcs = Conjunction.of(bcs, boundaryConditions.get(i).not());
			}
			if (refined.getAssume().size() > 0) {
				domainAndBcs.add(bcs);
				domainAndBcs.addAll(refined.getAssume());
			} else {
				domainAndBcs.add(bcs);
			}
		}
		
		//refined = new Spec(domainAndBcs, original.getGoals(), original.getIns(), original.getOuts());
		
		Formula refined_formula = refined.toFormula().formula();
		if (refined_formula == BooleanConstant.TRUE)
			return 1.0d;
		if (refined_formula == BooleanConstant.FALSE)
			return 0.0d;
		//Formula lostModels = Conjunction.of(original.toFormula().formula(), refined_formula.not());
		Formula lostModels = Conjunction.of(domainAndBcs);
		Formula tmp = Conjunction.of(original.getGoals());
		Formula tmp1 = Conjunction.of(refined.getGoals()).not();
		lostModels = Conjunction.of(lostModels, tmp);
		lostModels = Conjunction.of(lostModels, tmp1);
		
		LabelledFormula formula = LabelledFormula.of(lostModels, original.getVariables());
		BigDecimal numOfLostModels = new BigDecimal(countModels(formula));
		//patch to avoid computing again this value;
		BigDecimal numOfModels = new BigDecimal(originalNumOfModels);
		//BigDecimal res = numOfLostModels.divide(numOfModels, 2, RoundingMode.HALF_UP);
		BigDecimal res;
		if (numOfLostModels.longValue() == 0) {
			res = new BigDecimal(0);
		} else {
			//res = new BigDecimal(Math.log(numOfLostModels.longValue()) / Math.log(numOfModels.longValue()));
			final int iprecision = 9;
			MathContext precision = new MathContext(iprecision);
			BigDecimal numerator = BigDecimalMath.log10(numOfLostModels, precision);
			BigDecimal denominator = BigDecimalMath.log10(numOfModels, precision);
			if (denominator.compareTo(new BigDecimal(1)) <= 0) {
				res = new BigDecimal(1);
			} else {
				res = numerator.divide(denominator, iprecision, RoundingMode.HALF_UP);
			}
		}
		
		/*
		System.out.println("----");
		System.out.println("res: " + res);
		Formula dom = Conjunction.of(original.getAssume());
		Formula goals = Conjunction.of(original.getGoals());
		Formula repair = Conjunction.of(refined.getGoals()).not();
		Formula total = Conjunction.of(dom, goals, repair);
		numOfLostModels = new BigDecimal(countModels(LabelledFormula.of(total, original.getVariables())));
		System.out.println(numOfLostModels);
		System.out.println(numOfModels);
		res = numOfLostModels.divide(numOfModels, 9, RoundingMode.HALF_UP);
		if (numOfLostModels.longValue() == 0) {
			res = numOfLostModels;
		} else {
			System.out.println("res-log:" + Math.log(numOfLostModels.longValue()) / Math.log(numOfModels.longValue()) );
			res = new BigDecimal(Math.log(numOfLostModels.longValue()) / Math.log(numOfModels.longValue()));
		}
		System.out.println("res: " + res);
		
		System.out.println("----");
		*/
		
		double value = 1.0d - res.doubleValue();

		if (res.doubleValue() > 1.0d) {
			System.out.println("\nWARNING: increase the bound. ");
			return 1.0d;
		}

		return value;
	}

	public synchronized double compute_won_models_porcentage(Spec original, Spec refined) throws IOException, InterruptedException {
		System.out.print("+");

		BigInteger refinedNumOfModels = countModels(refined.toFormula());
		if (refinedNumOfModels == BigInteger.ZERO)
			return 0.0d;

		List<Formula> domainAndBcs = new ArrayList<Formula>();
		if (this.setting == SpecificationSetting.SemSynStatusBc) {
			Formula bcs = Conjunction.of(boundaryConditions.get(0).not());
			domainAndBcs = new ArrayList<Formula>();
			for (int i = 1; i < boundaryConditions.size(); ++i) {
				bcs = Conjunction.of(bcs, boundaryConditions.get(i).not());
			}
			if (original.getAssume().size() > 0) {
				//domainAndBcs.add(bcs);
				domainAndBcs.addAll(original.getAssume());
			} else {
				//domainAndBcs.add(bcs);
			}
		}
		//original = new Spec(domainAndBcs, original.getGoals(), original.getIns(), original.getOuts());
		
		
		Formula original_formula = original.toFormula().formula();
		//Formula wonModels = Conjunction.of(original_formula.not(), refined.toFormula().formula());

		Formula wonModels = Conjunction.of(domainAndBcs);
		Formula tmp = Conjunction.of(original.getGoals()).not();
		Formula tmp1 = Conjunction.of(refined.getGoals());
		wonModels = Conjunction.of(wonModels, tmp);
		wonModels = Conjunction.of(wonModels, tmp1);		
		
		LabelledFormula formula = LabelledFormula.of(wonModels, original.getVariables());
		BigDecimal numOfWonModels = null;
		numOfWonModels = new BigDecimal(countModels(formula));

		BigDecimal numOfRefinedModels = new BigDecimal(refinedNumOfModels);
		//BigDecimal res = numOfWonModels.divide(numOfRefinedModels, 2, RoundingMode.HALF_UP);
		
		BigDecimal res;
		if (numOfWonModels.longValue() == 0) {
			res = new BigDecimal(0);
		} else {
			//res = new BigDecimal(Math.log(numOfWonModels.longValue()) / Math.log(numOfRefinedModels.longValue()));
			final int iprecision = 9;
			MathContext precision = new MathContext(iprecision);
			BigDecimal numerator = BigDecimalMath.log10(numOfWonModels, precision);
			BigDecimal denominator = BigDecimalMath.log10(numOfRefinedModels, precision);
			if (denominator.compareTo(new BigDecimal(1)) <= 0) {
				res = new BigDecimal(1);
			} else {
				res = numerator.divide(denominator, iprecision, RoundingMode.HALF_UP);
			}
			//res = new BigDecimal(BigDecimalMath.log(numOfWonModels, precision) / Math.log(numOfRefinedModels.longValue()));
		}
		
		/*
		System.out.println("res: " + res);
		
		System.out.println("----");
		Formula dom = Conjunction.of(original.getAssume());
		Formula goals = Conjunction.of(original.getGoals());
		Formula repair = Conjunction.of(refined.getGoals()).not();
		Formula total = Conjunction.of(dom, goals, repair);
		numOfWonModels = new BigDecimal(countModels(LabelledFormula.of(total, original.getVariables())));
		System.out.println(numOfWonModels);
		System.out.println(numOfRefinedModels);
		res = numOfWonModels.divide(numOfRefinedModels, 9, RoundingMode.HALF_UP);
		System.out.println("res: " + res);
		
		System.out.println("----");
		
//		System.out.print("WON:" + numOfWonModels + " " + numOfRefinedModels + " ");
		*/
		double value = 1.0d - res.doubleValue();

		if (res.doubleValue() > 1.0d) {
			System.out.println("\nWARNING: increase the bound. ");
			return 1.0d;
		}
		return value;
	}
	
	private synchronized void compute_originalLikelihood(Spec originalSpecification2, List<Formula> bcs) throws IOException, InterruptedException {
		for (Formula bc : bcs)
			originalLikelihood.put(bc, this.likelihood(Conjunction.of(originalSpecification2.getAssume()), bc));
	}
	
/*
	public void computeSurvivalBC(SpecificationChromosome chromosome) throws IOException, InterruptedException {
		// Compute survival boundary conditions
		List<Formula> survivalBCs = new LinkedList<Formula>();
		for (Formula bc : this.boundaryConditions) {
			if (chromosome.spec.isBoundaryCondition(bc))
				survivalBCs.add(bc);
		}
		chromosome.survivalBC.clear();
		chromosome.survivalBC.addAll(survivalBCs);
	}
	*/
	public boolean isConsistent(Spec spec, Formula bc) throws IOException, InterruptedException {
		Formula environmentAndbc = Conjunction.of(Conjunction.of(spec.getAssume()),bc); //Assum & bc
		Formula assumesAndguarantees = Conjunction.of(Conjunction.of(spec.getGoals()), environmentAndbc); // assum & guar & bc
		return (solvers(assumesAndguarantees,spec.getVariables()).equals(SolverResult.SAT()));
		//return (solvers(Formula_Utils.toSolverSyntax(assumesAndguarantees))).equals(SolverResult.SAT);			
	}
	
	public synchronized void computeSurvivalBC(SpecificationSolution chromosome) throws IOException, InterruptedException {
		// Compute survival boundary conditions
		List<Formula> survivalBCs = new LinkedList<Formula>();
		for (Formula bc : this.boundaryConditions) {
			//if (!chromosome.spec.isConsistent(bc) && chromosome.spec.isMinimal(bc))
			if (!isConsistent(chromosome.spec, bc))
				survivalBCs.add(bc);
		}
		chromosome.survivalBC.clear();
		chromosome.survivalBC.addAll(survivalBCs);
	}

	
	public synchronized double likelihoodOfRemovedBC(SpecificationSolution spec) throws IOException, InterruptedException {
		List<Formula> removed = new LinkedList<Formula>();
		double all_likelihood = 0.0d;
		for (Formula bc : this.boundaryConditions) {
			if (!spec.survivalBC.contains(bc))
				removed.add(bc);
			all_likelihood += originalLikelihood.get(bc);
		}
		
//		System.out.println(removed);
		
		double cumulated_likelihood = 0.0d;
		for (Formula bc : removed) 
			cumulated_likelihood += originalLikelihood.get(bc);
		return (all_likelihood==0.0d)? 0.0d : cumulated_likelihood/all_likelihood;
	}
	
	public synchronized boolean somethingHasBeenRemoved(Spec original, Spec refined) {
		boolean assumptionAdded = !ExperimentSetting.allowAssumptionAddition && Formula_Utils.splitConjunction(Conjunction.of(original.getAssume())).size() < Formula_Utils.splitConjunction(Conjunction.of(refined.getAssume())).size() ;
		boolean guaranteeRemoved =  !ExperimentSetting.allowGuaranteeRemoval && Formula_Utils.splitConjunctions(original.getGoals()).size() > Formula_Utils.splitConjunctions(refined.getGoals()).size();
		return assumptionAdded || guaranteeRemoved;
	}

}
