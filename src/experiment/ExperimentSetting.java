package experiment;

import java.util.Random;

public class ExperimentSetting {

	public static boolean USE_DOCKER = true;
	public static Random RANDOM_GENERATOR = new Random(System.currentTimeMillis());

	//genetic algorithm setting
	public static int GA_GENERATIONS = 10;
	public static int GA_MAX_NUM_INDIVIDUALS = Integer.MAX_VALUE;
	public static int GA_POPULATION_SIZE = 100;
	public static int GA_CROSSOVER_RATE = 10; // Percentage of chromosomes that will be selected for crossover
	public static int GA_MUTATION_RATE = 100; // Probability with which the mutation is applied to each chromosome
	public static int GA_GENE_MUTATION_RATE = 0; // Probability with which the mutation is applied to each gene of the chromosome
												 // 0 means that the probability will be 1/size_of(formula)
	public static int GA_GENE_NUM_OF_MUTATIONS = 0; // Number of allowed genes to be mutated
													// 0 means that it will be allowed to apply size_of(formula) mutations
	
	public static int GA_EXECUTION_TIMEOUT = 0;//in seconds. No timeout by default.
	public static int GA_GUARANTEES_PREFERENCE_FACTOR = 100; // p is the probability to which the genetic operators will be applied to the guarantees.
															// (1-p) is the probability to which the genetic operators will be applied to the assumptions.
	public static boolean GA_RANDOM_SELECTOR = false;
	
	public static boolean GA_BOLTZMANN_SELECTOR = true;
	public static double GA_BOLTZMANN_FACTOR = 1;
	public static double GA_BOLTZMANN_DIVISION_FACTOR = 2;
	public static int GA_BOLTZMANN_ROUND_FACTOR = 4;
	
	public static boolean only_inputs_in_assumptions = false;
	public static double GA_THRESHOLD = 0.0d;
	public static int GA_SURVIVING_BCs = 0;

	public static boolean GA_CHECK_NEW_BCs = true;
	public static int GA_ALLOWED_NEW_BCs = 0;
	
	final public static int MAX_LITERAL = 3;
	public static boolean allowAssumptionAddition = false;
	public static boolean allowGuaranteeRemoval = false;
	public static double STATUS_FACTOR = 0.1d;

	public static double LOST_MODELS_FACTOR = 0.05d;
	public static double WON_MODELS_FACTOR = 0.05d;
	
	public static double SYNTACTIC_FACTOR = 0.1d;
	public static boolean LEVENSTEIN_ENABLE = true;

	
	public static double IMP_FACTOR = 0.7d;
	public static double BC_FACTOR = 0.0d;
	public static double LIKELIHOOD_FACTOR = 0.0d;
	public static double SEVERITY_FACTOR = 0.0d;
	
	//	public static final double SOLUTION = 0.8d;
	public static double MAX_FITNESS () {
		return STATUS_FACTOR + LIKELIHOOD_FACTOR + BC_FACTOR + LOST_MODELS_FACTOR 
				+ WON_MODELS_FACTOR + SYNTACTIC_FACTOR + IMP_FACTOR;
	}

	//parsing timeout
	//public static int PARSING_TIMEOUT = 60;
	public static int PARSING_TIMEOUT = 30;
	
	//model counting setting
	//public static int MC_BOUND = 10;
	public static int MC_BOUND = 10;
	//public static int MC_TIMEOUT = 300;
	public static int MC_TIMEOUT = 30;

	//SAT solver setting
	public static int SAT_TIMEOUT = 120;
	
	
	//path (file system) to save the fitness
	public static String FITNESS_RESULT_PATH;
}
