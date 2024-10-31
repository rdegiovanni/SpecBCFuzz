package sat.tool;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.MOperator;
import owl.ltl.UOperator;
import owl.ltl.visitors.Visitor;
import sat.PrintVisitor;
import sat.SyntaxOperatorReplacer;

public class Black extends LTLSolver {

	protected SatSolver satSolver;
	
	protected File tmpRunner;
	
	protected String version;
	
	public enum SatSolver {
		z3,
		mathsat,
		cmsat,
		minisat,
		cvc5
	}
	
	public Black(File binPath, int maxAtomicPropositions, SatSolver satSolver, String version, File tmpRunner) {
		super(binPath, maxAtomicPropositions);
		this.tmpRunner = tmpRunner;
		this.satSolver = satSolver;
		this.version = version;
		super.setPerformance(Performance.POSIX);
	}

	@Override
	public String getName() {
		return "Black-" + this.satSolver + "_" + this.version;
	}
	
	@Override
	protected String getCommand() {
		return super.binPath.getParentFile().getAbsolutePath();
	}
	
	/*
	protected String getCallBin() {
		String prefix = "";
		String call = "./" + super.binPath.getName();
		if (super.isPerformance(Performance.POSIX)) {
			prefix = "time -p";
		}
		return prefix + " " + call;
	}
	*/
	
	protected String getCallBin() {
		String prefix = "";
		//String time = "";
		String call = super.binPath.getAbsolutePath();
		if (super.isPerformance(Performance.POSIX)) {
			prefix = "time -p ";
			//time = "/usr/bin/time -p";
		}
		String callBin = prefix + "ld.so --library-path " + libraryPath() + " " + call;
		//String callBin = "ld.so --library-path " + libraryPath() + " " + time + " " + call;
		//System.out.println(callBin);
		return callBin;
	}
	
	protected String libraryPath() {
		String basedir = getBaseDir();
		String path = basedir + File.pathSeparator
					+ basedir + File.separator + "lib" + File.pathSeparator
					+ basedir + File.separator + "external" + File.separator + "cryptominisat5" + File.separator + "lib" + File.pathSeparator
					+ basedir + File.separator + "external" + File.separator + "minisat" + File.separator + "lib" + File.pathSeparator
					+ basedir + File.separator + "external" + File.separator + "mathsat5-Linux-x86_64" + File.separator + "lib" + File.pathSeparator
					+ basedir + File.separator + "external" + File.separator + "CVC5" + File.separator + "bin" + File.pathSeparator
					+ "$LD_LIBRARY_PATH"
					;
		return path;
	}
	
	protected String getBaseDir() {
		return super.binPath.getParentFile().getAbsoluteFile().toString();
	}

	@Override
	protected synchronized Process runCommand(String cmd, String exp) throws IOException {
		try {
			if (this.tmpRunner.exists()) {
				FileUtils.delete(this.tmpRunner);
			}
			this.tmpRunner.createNewFile();
			String content = "#!/bin/bash -i \n";
			FileUtils.write(this.tmpRunner, content, "UTF-8", true);
			content = "cd " + cmd + " \n";
			FileUtils.write(this.tmpRunner, content, "UTF-8", true);
			content = getCallBin() + " " + exp;
			//content = "./" + super.binPath.getName() + " " + exp;
			FileUtils.write(this.tmpRunner, content, "UTF-8", true);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		/*
		Process turnExecutable = Runtime.getRuntime().exec("chmod +x " + this.tmpRunner.getAbsoluteFile().toString());
		try {
			System.out.println(Thread.currentThread().getId() + " >>> File: " + this.tmpRunner.getAbsolutePath());
			if (turnExecutable.waitFor((long) super.timeout(), TimeUnit.SECONDS)) {
				System.out.println(Thread.currentThread().getId() + " >>> TIMEOUT_IN");
				turnExecutable.getInputStream().close();
				turnExecutable.getOutputStream().close();
				turnExecutable.getErrorStream().close();
				turnExecutable.destroy();
				System.out.println(Thread.currentThread().getId() + " >>> TIMEOUT_IN-2");
			}
			System.out.println(Thread.currentThread().getId() + " >>> TIMEOUT_OUT");
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		*/
		
		//System.out.println(cmd + " " + "solve -f '((p1) W (G(p2)))' -B mathsat");
		//Process p = Runtime.getRuntime().exec(new String[]{cmd,exp});
		//Process p = Runtime.getRuntime().exec(new String[]{cmd,"solve", "-f","'" + exp + "'", "-B", this.satSolver.toString()});
		//Process p = Runtime.getRuntime().exec(new String[]{cmd,"--help"});
		//Process p = Runtime.getRuntime().exec(new String[]{cmd,"solve --formula 'F p'"});
		//Process p = Runtime.getRuntime().exec(cmd + " " + "solve -f '((p1) W (G(p2)))' -B mathsat");
		//Process p = Runtime.getRuntime().exec("/home/users/lude/fuzzing-ltl-solvers/call-black.sh " + "'" + exp + "' " + this.satSolver.toString() );
		//Process p = Runtime.getRuntime().exec(cmd + "'" + exp + "' " + this.satSolver.toString());
		
		/*
		int sleepTime = 1;
		while (!this.tmpRunner.canWrite() || !this.tmpRunner.canExecute()) {
			try {
				System.out.println(Thread.currentThread().getId() + " >>> sleepTime");
				Thread.sleep(sleepTime);
				sleepTime *= 2;
				if (sleepTime > 3000) {
					System.out.println(Thread.currentThread().getId() + " BREAK-Black");
					break;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		*/
		//System.out.println(Thread.currentThread().getId() + " >>> File: " + this.tmpRunner.getAbsolutePath());
		/*
		try {
		*/
			//System.out.println(Thread.currentThread().getId() + " >>> ID-init");
		Process p = Runtime.getRuntime().exec("bash " + this.tmpRunner.getAbsoluteFile().toString());
			//System.out.println(Thread.currentThread().getId() + " >>> ID-end");
		return p;
		/*
		} catch (IOException e) {
			System.out.println(Thread.currentThread().getId() + " >>> ID-exception");
		}
		*/
		//return null;
	}
	
	@Override
	protected String formulaToString(Formula formula) {
		List<String> lAtomicPropositions = getAtomicPropositions();
		String result = PrintVisitor.toString(formula, lAtomicPropositions, true);
		result = result.replace("false", "False");
		result = result.replace("true", "True");
		return result;
	}
	
	@Override
	protected String parameter(Formula formula) {
		String f = super.parameter(formula);
		String fAndParameters = "solve -f '" + f + "' -B " + this.satSolver;
		//String fAndParameters = "'" + f + "' " + this.satSolver.toString();
		return fAndParameters;
	}
	
	@Override
	protected Formula convertLTLFormat(Formula formula) {
		Visitor<Formula> operatorReplacer = (Visitor<Formula>) new SyntaxOperatorReplacer() {
			
			@Override
			public Formula visit(MOperator mOperator) {
				Formula left = mOperator.left.accept(this);
				Formula right = mOperator.right.accept(this);
				return UOperator.of(right,Conjunction.of(right,left));
			}
			
		};
		Formula result = formula.accept(operatorReplacer);
		result = result.accept(operatorReplacer);
		return result;
	}

}