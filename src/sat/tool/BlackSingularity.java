package sat.tool;

import java.io.File;

/*
 * 
 * BlackSingularity calls the black solver in a singularity container (binVmPath).
 * 
 */
public class BlackSingularity extends Black {

	private File blackBinInContainer;
	
	public BlackSingularity(File blackBinInContainer, File blackSifSingularity, int maxAtomicPropositions, SatSolver satSolver, String version, File tmpRunner) {
		super(blackSifSingularity, maxAtomicPropositions, satSolver, version, tmpRunner);
		this.blackBinInContainer = blackBinInContainer;
		super.setPerformance(Performance.POSIX);
	}
	
	@Override
	protected String getCallBin() {
		String prefix = "";
		String call = "singularity exec " + this.binPath.getName() + " time -p " + this.blackBinInContainer.getAbsolutePath();
		if (super.isPerformance(Performance.POSIX)) {
			//prefix = "time -p";
			//return prefix + " " + call;
			return call;
		}
		return call;
		//return prefix + " " + call;
		
		//---
		//return "time -p singularity exec " + this.binPath.getName() + " " + this.blackBinInContainer.getAbsolutePath();
		//return "singularity exec " + this.binPath.getName() + " " + this.blackBinInContainer.getAbsolutePath();
	}
}