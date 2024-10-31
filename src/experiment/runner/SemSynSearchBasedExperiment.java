package experiment.runner;

import java.io.File;
import java.io.IOException;
import java.util.List;

import experiment.ExperimentLog;
import experiment.ExperimentRunner;
import fuzz.Seeds;
import fuzz.searchbased.SemSynSearchBasedFuzzer;
import ltl.gore.SpecReader;
import sat.SATSolver;

public class SemSynSearchBasedExperiment implements ExperimentRunner {

	private File specs;
	private SpecReader reader;
	private final int popSize;
	private final int maxNumOfInd;
	private boolean enableThreads;
	//private final int position;
	
	public SemSynSearchBasedExperiment(File specs, SpecReader reader, int popSize, int maxNumOfInd, boolean enableThreads) {
		this.specs = specs;
		this.reader = reader;
		this.popSize = popSize;
		this.maxNumOfInd = maxNumOfInd;
		this.enableThreads = enableThreads;
		//this.position = -1;
	}
	
	/**
	public SemSynSearchBasedExperiment(File specs, SpecReader reader, int popSize, int maxNumOfInd) {
		this.specs = specs;
		this.reader = reader;
		this.popSize = popSize;
		this.maxNumOfInd = maxNumOfInd;
		//this.position = position;
	}
	*/
	
	/**
	 * Search based on the semantic and syntax distance. Seed: LTL benchmark
	 */
	@Override
	public void run(List<SATSolver> solvers, int nRuns, int timeout, File log) {
		try {
			final boolean enableVariables = true;
			Seeds seeds = this.reader.search(this.specs);
			Experiment exp = new Experiment(solvers, null, timeout, ExperimentLog.build(log,enableVariables), this.enableThreads);
			SemSynSearchBasedFuzzer fuzzer = new SemSynSearchBasedFuzzer(seeds, exp, this.popSize, this.maxNumOfInd);
			if (nRuns < 0) {
				nRuns = seeds.size() * Math.abs(nRuns);
			}
			for (int i = 0; i < nRuns; ++i) {
				fuzzer.fuzz();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}