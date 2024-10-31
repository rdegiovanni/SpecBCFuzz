package sat.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;

import owl.ltl.Formula;
//import owl.ltl.visitors.PrintVisitor;
import sat.PrintVisitor;
import sat.SATSolver;
import sat.SolverResult;

public abstract class LTLSolver implements SATSolver {

	protected File binPath;
	
	protected int maxAtomicPropositions;
	
	protected List<String> atomicPropositions;
	
	private double timeout = 0;

	private double startExecutionTime = -1;

	//private long executionTime = -1;
		
	private Performance performance = Performance.JVM;
	
	protected enum Performance {
		JVM,POSIX;
		public double time = -1l;
		boolean JVM() { return this == JVM; }
		boolean POSIX() { return this == POSIX; }
	}
	
	public LTLSolver(File binPath, int maxAtomicPropositions) {
		this.binPath = binPath;
		this.maxAtomicPropositions = maxAtomicPropositions;
		this.setPerformance(Performance.POSIX);
	}
	
	public LTLSolver(File binPath, List<String> atomicPropositions) {
		this.binPath = binPath;
		this.atomicPropositions = atomicPropositions;
		this.setPerformance(Performance.POSIX);
	}
	
	protected boolean isPerformance(Performance performance) {
		return this.performance == performance;
	}
	
	protected void setPerformance(Performance performance) {
		this.performance = performance;
	}
	
	protected String getCommand() {
		//return this.binPath.getAbsolutePath();
		String prefix = "";
		String call = this.binPath.getAbsolutePath();
		if (isPerformance(Performance.POSIX)) {
			prefix = "time -p ";
		}
		return prefix + call;
	}
	
	protected String parameter(Formula formula) {
		return formulaToString(convertLTLFormat(formula));
	}
	
	protected void cleanExecution() { }
	
	public double timeout() {
		return this.timeout;
	}
	
	/*
	protected void performanceBasedInPosixAPIAnalysis(BufferedReader bufferedreader) {
		if (this.performance.POSIX()) {
			String aux;
			try {
				while ((aux = bufferedreader.readLine()) != null) {
					if(aux.startsWith("real ")) {
						String time = aux.replace("real ", "");
						this.performance.time = Long.parseLong(time);
						break;
					}
				}
		    	bufferedreader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	*/
	
	protected boolean checkPerformanceBasedInPosixAPIAnalysis(String line) {
		if(this.performance.POSIX() && line.startsWith("real ")) {
			String time = line.replace("real ", "");
			//this.performance.time = (long) Double.parseDouble(time);
			this.performance.time = Double.parseDouble(time);
			return true;
		}
		return false;
	}
	
	protected SolverResult outputAnalysis(InputStream out) {
    	InputStreamReader inread = new InputStreamReader(out);
    	BufferedReader bufferedreader = new BufferedReader(inread);
		String aux;
		SolverResult result = SolverResult.UNSAT();
		result.hashCode();
		try {
			while ((aux = bufferedreader.readLine()) != null) {
				if (this.performance.POSIX()) {
					if (checkPerformanceBasedInPosixAPIAnalysis(aux)) {
						continue;
					}
				}
				/*
				if (aux.contains("pltl") && aux.contains("ignored")) {
					continue;
				}
				*/
				if (aux.contains("cannot find")) {
					System.err.println("Error calling solvers");
					result = SolverResult.ERROR();
					break;
				}
				if (aux.contains("Cannot recognize the output")) {
					result = SolverResult.ERROR();
					break;
				}
				else {
					if (aux.contains("unsat") || aux.contains("UNSAT")) {
			    		result = SolverResult.UNSAT();
			    		break;
			    	} else if (aux.contains("sat") || aux.contains("SAT")) {
			    		result = SolverResult.SAT();
			    		break;
			    	}
				}
			}
	    	bufferedreader.close();
	    	inread.close();
	    	out.close();
		} catch (IOException e) {
			e.printStackTrace();
			return SolverResult.ERROR();
		}
		return result;
	}
	
	protected boolean errorAnalysis(InputStream err) {
    	InputStreamReader errread = new InputStreamReader(err);
    	BufferedReader errbufferedreader = new BufferedReader(errread);
    	String aux;
		Boolean result = false;
	    try {
			while ((aux = errbufferedreader.readLine()) != null) {
				if (this.performance.POSIX()) {
					if (checkPerformanceBasedInPosixAPIAnalysis(aux)) {
						continue;
					}
				}
				// Model checker uses error output to send warning message when formulae is a contradiction.
				if (aux.isEmpty()) continue;
				/*
				if (aux.contains("WARNING")) {
					result = true;
					*/
				if (aux.contains("ERROR") || aux.contains("Error") || aux.contains("error")) {
					result = true;
				}
			}
	   		errbufferedreader.close();
	   		errread.close();
	   		err.close();
		} catch (IOException e) {
			result = true;
			e.printStackTrace();
		}
	    return result;
	}
	
	abstract protected Formula convertLTLFormat(Formula formula);
	
	public void setMaxAtomicPropositions(int maxAtomicPropositions) {
		this.maxAtomicPropositions = maxAtomicPropositions;
	}
	
	public void setAtomicPropositions(List<String> atomicPropositions) {
		this.atomicPropositions = atomicPropositions;
	}
	
	protected List<String> getAtomicPropositions() {
		if (this.atomicPropositions == null || this.atomicPropositions.isEmpty()) {
			List<String> lAtomicPropositions = new ArrayList<String>();
			for (int i = 0; i < this.maxAtomicPropositions; ++i) {
				lAtomicPropositions.add("p" + i);
			}
			return lAtomicPropositions;
		} else {
			return this.atomicPropositions;
		}
	}
	
	/*
	 * It keeps the same order of Labelled formula.
	 */
	protected String formulaToString(Formula formula) {
		List<String> lAtomicPropositions = getAtomicPropositions();
		String str = PrintVisitor.toString(formula, lAtomicPropositions, true);
		return str;
		//return PrintVisitor.toString(formula, lAtomicPropositions, false);
	}
	
	protected void startExecutionTime() {
		if (!this.performance.JVM()) {
			return;
		}
		//this.executionTime = -1;
		this.performance.time = -1;
		this.startExecutionTime = System.currentTimeMillis();
	}
	
	protected void stopExecutionTime() {
		if (!this.performance.JVM()) {
			return;
		}
		if (this.startExecutionTime == -1) {
			throw new RuntimeException("You need to call startExecutionTime before the stopExecutionTime.");
		}
		//this.executionTime = System.currentTimeMillis() - this.startExecutionTime;
		this.performance.time = System.currentTimeMillis() - this.startExecutionTime;
		this.startExecutionTime = -1;
	}
	
	public double executionTime() {
		return this.performance.time;
	}
	
	protected double executionTimeBasedInJVM() {
		if (!this.performance.JVM()) {
			throw new RuntimeException("Please, enable the execution time based in JVM.");
		}
		if (this.performance.time == -1) {
			throw new RuntimeException("You need to call stopExecutionTime or isSAT before the executionTime.");
		}
		//long executionTime = this.executionTime;
		//this.performance.time = this.executionTime;
		//this.executionTime = -1;
		return this.performance.time;
	}
	
	protected double executionTimeBasedInPosix() {
		if (!this.performance.POSIX()) {
			throw new RuntimeException("You need enable the execution time based in POSIX API.");
		}
		return this.performance.time;
	}
 	
	protected Process runCommand(String cmd, String exp) throws IOException {
		String run = cmd + " " + exp;
		//p = Runtime.getRuntime().exec(new String[]{getCommand(),exp});
		Process p = Runtime.getRuntime().exec(run);
		return p;
	}

	@Override
	public SolverResult isSAT(Pair<Formula, List<String>> formulaAndVariables, int timeout) {
		this.atomicPropositions = formulaAndVariables.getRight();
		return isSAT(formulaAndVariables.getLeft(), timeout);
	}

	@Override
	public SolverResult isSAT(Pair<Formula, List<String>> formulaAndVariables, int timeout, int repeatThresold) {
		this.atomicPropositions = formulaAndVariables.getRight();
		return isSAT(formulaAndVariables.getLeft(), timeout, repeatThresold);
	}

	@Override
	public SolverResult isSAT(Pair<Formula, List<String>> formulaAndVariables, int timeout, int repeatStart,
			int repeatThresold) {
		this.atomicPropositions = formulaAndVariables.getRight();
		return isSAT(formulaAndVariables.getLeft(), timeout, repeatStart, repeatThresold);
	}
	
	@Override
	public final SolverResult isSAT(Formula formula, int timeout) {
		return isSAT(formula, timeout, 0);
	}
	
	@Override
	public final SolverResult isSAT(Formula formula, int timeout, int repeatThresold) {
		return isSAT(formula, timeout, 0, repeatThresold);
	}
	
	@Override
	public final SolverResult isSAT(Formula formula, int timeout, int repeatStart, int repeatThresold) {
		Process p = null;
		//Imprimir a HASHCODE aqui;
		//Criar varios e imprimir os resultados;
		SolverResult result = SolverResult.ERROR();
		double time = -1;
		String exp = parameter(formula);
		this.timeout = timeout;
		if (exp != null && repeatStart >= 0) {
			try {
				p = runCommand(getCommand(), exp);
				long pid = p.pid();
				//System.out.println("pid-here-init: " + pid);
				startExecutionTime();
				if(!p.waitFor(timeout, TimeUnit.SECONDS)) {
					stopExecutionTime();
					pid = p.pid();
					//System.out.println("pid-here-end: " + pid);
					p.destroy();
					p.destroyForcibly();
					result = SolverResult.TIMEOUT();
					time = timeout;
				} else {
					pid = p.pid();
					//System.out.println("pid-here-ok: " + pid);
					stopExecutionTime();
					/*
					if (this.performance.JVM()) {
						time = executionTime();
					}
					*/
					//Input stream
					InputStream out = p.getInputStream();
					result = outputAnalysis(out);
			    	//Error stream
					InputStream err = p.getErrorStream();
			    	if (errorAnalysis(err) == true) {
			    		result = SolverResult.ERROR();
			    	}
				    //Process failure
					if (p.waitFor() != 0) {
						result = SolverResult.ERROR();
					}
					time = executionTime();
				}
				if (p!=null) {
		  			OutputStream os = p.getOutputStream();
					if (os!=null) os.close();
		   		}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				result = SolverResult.ERROR();
				long pid = p.pid();
				//System.out.println("pid-here-interrupted: " + pid);
			}
    	}
		if (repeatStart < repeatThresold) {
			if (result.equals(SolverResult.ERROR()) || result.equals(SolverResult.TIMEOUT())) {
				return isSAT(formula, timeout, repeatStart+1, repeatThresold);
			}
		}
		cleanExecution();
		result.formula(formulaToString(convertLTLFormat(formula)));
		result.time(time);
		return result;
	}
	
}