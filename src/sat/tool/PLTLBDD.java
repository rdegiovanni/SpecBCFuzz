package sat.tool;

import java.io.File;

public class PLTLBDD extends PLTL {

	public PLTLBDD(File binPath, int maxAtomicPropositions) {
		super(binPath, maxAtomicPropositions, PLTL_VERSION.bdd);
	}
	
	@Override
	protected String getCommand() {		
		String prefix = "";
		//String time = "";
		String call = this.binPath.getAbsolutePath();
		if (isPerformance(Performance.POSIX)) {
			prefix = "time -p ";
			//time = "/usr/bin/time -p";
		}
		String callBin = prefix + "ld.so --library-path " + libraryPath() + " " + call;
		//String callBin = "ld.so --library-path " + libraryPath() + " " + time + " " + call;		
		return callBin;
	}
	
	protected String libraryPath() {
		return this.binPath.getParentFile().getParentFile().getAbsolutePath() + File.separator + "pltl" + 
				File.pathSeparator + "$LD_LIBRARY_PATH";
	}
	
}