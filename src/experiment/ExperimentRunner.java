package experiment;

import java.io.File;
import java.util.List;

import sat.SATSolver;

public interface ExperimentRunner {

	void run(List<SATSolver> solvers, int nRuns, int timeout, File log);

}
