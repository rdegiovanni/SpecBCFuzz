package experiment.runner;

import java.io.File;
import java.io.IOException;
import java.util.List;

import experiment.ExperimentLog;
import experiment.ExperimentRunner;
import fuzz.Seeds;
import fuzz.mutation.MutationFuzzer;
import ltl.gore.SpecReader;
import sat.SATSolver;

public class MutationFuzzerExperiment implements ExperimentRunner {

	private File specs;
	private SpecReader reader;
	private final int GA_MUTATION_RATE = 100;
	private final int GA_GUARANTEES_PREFERENCE_FACTOR = 100;
	private final boolean only_inputs_in_assumptions = false;
	private final int GA_GENE_NUM_OF_MUTATIONS = 0;
	
	public MutationFuzzerExperiment(File specs, SpecReader reader) {
		this.specs = specs;
		this.reader = reader;
	}
	
	/*
	private Seeds readSpecsFromGORE() {
		Seeds seeds = new Seeds();
		if (this.specs.isDirectory()) {
			File[] specsFolders = this.specs.listFiles();
			for (File specFolder : specsFolders) {
				File specFile = new File(specFolder.getAbsoluteFile() + File.pathSeparator + "spec");
				try {
					Spec spec = reader.read(specFile);
					seeds.add(spec, null, specFile.getName());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			throw new RuntimeException(this.specs.getAbsolutePath() + " is not a directory.");
		}
		return seeds;
	}
	
	private Seeds readSpecsFromNuSMV() {
		Seeds seeds = new Seeds();
		if (this.specs.isDirectory()) {
			File[] specsFolders = this.specs.listFiles();
			for (File specFolder : specsFolders) {
				File specFile = new File(specFolder.getAbsoluteFile() + File.pathSeparator + "spec");
				try {
					Spec spec = reader.read(specFile);
					seeds.add(spec, null, specFile.getName());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			throw new RuntimeException(this.specs.getAbsolutePath() + " is not a directory.");
		}
		return seeds;
	}
	*/
	
	@Override
	public void run(List<SATSolver> solvers, int nRuns, int timeout, File log) {
		try {
			final boolean enableVariables = true;
			Seeds seeds = this.reader.search(this.specs);
			MutationFuzzer fuzzer = new MutationFuzzer(seeds, GA_MUTATION_RATE, GA_GUARANTEES_PREFERENCE_FACTOR, 
					only_inputs_in_assumptions, GA_GENE_NUM_OF_MUTATIONS);
			Experiment exp = new Experiment(solvers, fuzzer, timeout, ExperimentLog.build(log,enableVariables), false);
			if (nRuns < 0) {
				nRuns = seeds.size() * Math.abs(nRuns);
			}
			exp.run(nRuns);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
