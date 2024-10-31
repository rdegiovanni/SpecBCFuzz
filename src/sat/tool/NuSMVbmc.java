package sat.tool;

import java.io.File;

import owl.ltl.Formula;

public class NuSMVbmc extends NuSMV {

	public NuSMVbmc(File binPath, int maxAtomicPropositions) {
		super(binPath, maxAtomicPropositions);
	}
	
	public NuSMVbmc(File binPath, int maxAtomicPropositions, String version) {
		super(binPath, maxAtomicPropositions, version);
	}
	
	@Override
	protected String parameter(Formula formula) {
		return "-bmc -bmc_length 200 " + super.parameter(formula);
	}
	
	@Override
	public String getBaseName() {
		return "NUSMVbmc";
	}

}
