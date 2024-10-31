package experiment.runner;

import java.io.File;
import java.util.List;

import experiment.ExperimentLog;
import experiment.ExperimentRunner;
import fuzz.Seeds;
import fuzz.benchmarks.BenchmarksFuzz;
import ltl.gore.SpecReader;
import sat.SATSolver;

public class BenchmarksExperiment implements ExperimentRunner {

	private File cases;
	private SpecReader reader;
	private boolean enableThreads;
	
	public BenchmarksExperiment(File cases, SpecReader reader, boolean enableThreads) {
		this.cases = cases;
		this.reader = reader;
		this.enableThreads = enableThreads;
	}

	private void searchSpecs(File currentFile, Seeds seeds) {
		if (currentFile.isDirectory()) {
			File[] subDirs = currentFile.listFiles();
			for (File subDir : subDirs) {
				searchSpecs(subDir, seeds);
			}
		} else {
			File caseFile = currentFile;
			if (!caseFile.getName().equals("README")) {
				System.out.println("Reading the case: " + caseFile.getAbsolutePath());
				//seeds.add(reader.read(caseFile), null, 
				//		currentFile.getParentFile().getName() + "-" + currentFile.getName());
				seeds.add(caseFile, null, currentFile.getParentFile().getName() + "-" + currentFile.getName());
			}
		}
	}
	
	private Seeds readCasesFromNuSMVFormat(File cases) {
		Seeds seeds = new Seeds(this.reader);
		if (cases.isDirectory()) {
			File[] casesFolders = cases.listFiles();
			for (File caseFolder : casesFolders) {
				searchSpecs(caseFolder, seeds);
				/*
				File caseFile = new File(caseFolder.getAbsoluteFile() + File.pathSeparator + "spec");
				try {
 					List<String> lines = FileUtils.readLines(caseFile,"UTF-8");
					seeds.add(readNuSMVFormat(lines), null, caseFolder.getName());
				} catch (IOException e) {
					e.printStackTrace();
				}
				*/
			}
		}
		return seeds;
	}
	
	@Override
	public void run(List<SATSolver> solvers, int nRuns, int timeout, File log) {
		final boolean enableVariables = true;
		Seeds seeds = readCasesFromNuSMVFormat(this.cases);
		BenchmarksFuzz fuzzer = new BenchmarksFuzz(seeds);
		Experiment exp = new Experiment(solvers, fuzzer, timeout, ExperimentLog.build(log,enableVariables), this.enableThreads);
		if (nRuns < 0) {
			nRuns = seeds.size() * (Math.abs(nRuns));
		}
		System.out.println("nRuns: " + nRuns);
		exp.run(nRuns);
	}

}