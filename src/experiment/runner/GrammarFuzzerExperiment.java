package experiment.runner;

import java.io.File;
import java.util.List;
import java.util.Random;

import experiment.ExperimentLog;
import experiment.ExperimentRunner;
import experiment.ExperimentSetting;
import fuzz.Fuzzer;
import fuzz.grammar.GrammarFuzzer;
import fuzz.grammar.LTLGrammar;
import sat.SATSolver;

public class GrammarFuzzerExperiment implements ExperimentRunner {
	
	//CONSTANTS - GRAMMAR
	private int MAX_LITERAL;
	private double TERMINAL_PROBABILITY;
	private double BOOLEAN_CONSTANT_PROBABILITY;
	private int MAX_NONTERMINAL;
	private boolean enableThreads;
	
	public GrammarFuzzerExperiment() {
		this.MAX_LITERAL = ExperimentSetting.MAX_LITERAL;
		this.TERMINAL_PROBABILITY = 0.4;
		this.BOOLEAN_CONSTANT_PROBABILITY = 0.3;
		this.MAX_NONTERMINAL = 30;
		this.enableThreads = false;
	}
	
	public GrammarFuzzerExperiment(int maxLiteral, int maxNonTerminal, double terminalChoiceProbability, double booleanConstantProbability, 
			boolean enableThreads) {
		this.MAX_LITERAL = maxLiteral;
		this.TERMINAL_PROBABILITY = terminalChoiceProbability;
		this.MAX_NONTERMINAL = maxNonTerminal;
		this.BOOLEAN_CONSTANT_PROBABILITY = booleanConstantProbability;
		this.enableThreads = enableThreads;
	}
	
	@Override
	public void run(List<SATSolver> solvers, int nRuns, int timeout, File log) {
		final boolean enableVariables = true;
		LTLGrammar grammar = new LTLGrammar(new Random(), BOOLEAN_CONSTANT_PROBABILITY, MAX_LITERAL);
		Fuzzer fuzzer = new GrammarFuzzer(grammar, new Random(), TERMINAL_PROBABILITY, MAX_NONTERMINAL);
		Experiment exp = new Experiment(solvers, fuzzer, timeout, ExperimentLog.build(log,enableVariables), this.enableThreads);
		if (nRuns < 0) {
			nRuns = Integer.MAX_VALUE;
		}
		exp.run(nRuns);
	}
	
}