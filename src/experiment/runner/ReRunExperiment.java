package experiment.runner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import experiment.ExperimentLog;
import experiment.ExperimentRunner;
import ltl.gore.LogReader;
import owl.ltl.Formula;
import owl.ltl.parser.LtlParser;
import sat.SATSolver;

public class ReRunExperiment implements ExperimentRunner {

	private LogReader reader;
	private boolean enableThreads;
	
	public ReRunExperiment(LogReader reader, boolean enableThreads) {
		this.reader = reader;
		this.enableThreads = enableThreads;
	}
	
	public List<File> searchLogFiles(File folder) {
		List<File> logFiles = new ArrayList<File>();
		if (folder.isDirectory()) {
			String[] subFiles = folder.list();
			for (String strFile : subFiles) {
				File file = new File(folder.getAbsoluteFile() + File.separator + strFile);
				if (file.isFile()) {
					if (file.getName().equals("log_all.csv")) {
						logFiles.add(file);
					}
				} else if (file.isDirectory()) {
					List<File> logFilesFromDepth = searchLogFiles(file);
					if (!logFilesFromDepth.isEmpty()) {
						logFiles.addAll(logFilesFromDepth);
						//logFilesFromDepth.add(file);
					}
				}
			}
		} else if (folder.isFile() && folder.getName().startsWith("log_all.csv")) {
			logFiles.add(folder);
		}
		return logFiles;
	}
	
	public void runFromFolder(List<SATSolver> solvers, int nRuns, int timeout, File folder) {
		List<File> logFiles = searchLogFiles(folder);
		for (File l : logFiles) {
			System.out.println(l);
			run(solvers, nRuns, timeout, l);
		}
	}
	
	@Override
	public void run(List<SATSolver> solvers, int nRuns, int timeout, File log) {
		if (log.isDirectory()) {
			runFromFolder(solvers, nRuns, timeout, this.reader.getLog());
		} else {
			try {
				final boolean enableVariables = true;
				List<String> vars = new ArrayList<String>();
				for (int i = 0; i <= 19; ++i) {
					vars.add("p" + i);
				}
				String strFormula = reader.next();
				while (strFormula != null) {
					Formula formula = LtlParser.parse(strFormula,vars).formula();
					Experiment exp = new Experiment(solvers, null, timeout, ExperimentLog.build(log,enableVariables), this.enableThreads);
					exp.run(Pair.of(formula,vars));
					strFormula = reader.next();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}