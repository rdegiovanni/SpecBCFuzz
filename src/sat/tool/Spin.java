package sat.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.commons.io.FileUtils;

import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.MOperator;
import owl.ltl.ROperator;
import owl.ltl.UOperator;
import owl.ltl.WOperator;
//import owl.ltl.visitors.PrintVisitor;
import owl.ltl.visitors.Visitor;
import sat.PrintVisitor;
import sat.SolverResult;
import sat.SyntaxOperatorReplacer;

public class Spin extends LTLSolver {

	private final File inputTmp;
	private String version;
	//private final File outputTmp;

	public Spin(File binPath, int maxAtomicPropositions) {
		super(binPath, maxAtomicPropositions);
		this.inputTmp = new File(binPath.getParentFile(),"tmpin");
		this.version = "";
		//this.outputTmp = new File(binPath.getParentFile(),"tmpout");
	}
	
	public Spin(File binPath, int maxAtomicPropositions, String version) {
		this(binPath, maxAtomicPropositions);
		this.version = version;
		//this.outputTmp = new File(binPath.getParentFile(),"tmpout");
	}
	
	@Override
	protected String formulaToString(Formula formula) {
		List<String> lAtomicPropositions = getAtomicPropositions();
		String result = PrintVisitor.toString(formula, lAtomicPropositions, true);
		result = result.replace("false", "FALSE");
		result = result.replace("true", "TRUE");
		//System.out.println(result);
		return result;
	}

	@Override
	protected Formula convertLTLFormat(Formula formula) {
		//System.out.println(formula);
		//SyntacticSimplifier simp = new SyntacticSimplifier();
		Visitor<Formula> operatorReplacer = (Visitor<Formula>) new SyntaxOperatorReplacer() {
			
			@Override
			public Formula visit(MOperator mOperator) {
				Formula left = mOperator.left.accept(this);
				Formula right = mOperator.right.accept(this);
				return UOperator.of(right,Conjunction.of(right,left));
			}
			
			@Override
			public Formula visit(WOperator wOperator) {
				Formula left = wOperator.left.accept(this);
				Formula right = wOperator.right.accept(this);
				return Disjunction.of(GOperator.of(left), UOperator.of(left, right));
			}
			
			@Override
			public Formula visit(ROperator rOperator) {
				Formula left = rOperator.left.accept(this);
				Formula right = rOperator.right.accept(this);
				Formula uOperator = UOperator.of(right, Conjunction.of(right,left));
				Formula disjunction = Disjunction.of(uOperator, GOperator.of(right));
				return disjunction;
			}
		};
		Formula result = formula.accept(operatorReplacer);
		//System.out.println(result);
		result = result.accept(operatorReplacer);
		//System.out.println(result);
		//assert(result.toString().equals(formula.accept(operatorReplacer).toString()));
		/*
		if (result.toString().contains("R(")) {
			System.out.println("R(");
		}
		System.out.println("Medium formula:" + result);
		*/
		return result;
	}
	
	@Override
	protected String getCommand() {
		String prefix = "";
		//String time = "";
		String call = this.binPath.getAbsolutePath();
		if (isPerformance(Performance.POSIX)) {
			//time = "/usr/bin/time -p";
			prefix = "time -p ";
		}
		//Linux
		//String callBin = prefix + "ld.so --library-path " + libraryPath() + " " + call;
		//MacOS
		String callBin = prefix + " " + call;
		return callBin;
	}
	
	protected String libraryPath() {
		return "$LD_LIBRARY_PATH";
	}
	
	@Override
	protected String parameter(Formula formula) {
		try {
			if (this.inputTmp.exists()) {
				FileUtils.delete(this.inputTmp);
			}
			this.inputTmp.createNewFile();	
			//FileUtils.delete(this.outputTmp);
			//this.outputTmp.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		//System.out.println("old expression: " + formula.toString());
		String exp = super.parameter(formula);
		//System.out.println("New expression: " + exp);
		try {
			String content = "bool ";
			List<String> lAtomicPropositions = super.getAtomicPropositions();
			boolean first = true;
			for (String atomic : lAtomicPropositions) {
				if (!first)
					content += ", ";
				else
					first = false;
				content += atomic;
			}
			content += ";\n";

			content += "init\n" +
					"{\n" +
					"\tdo\n" +
					"\t:: atomic { \n";
			for (String atomic : lAtomicPropositions) {
				content += "if\n" +
						"\t\t:: true -> "+atomic+" = 0;\n" +
						"\t\t:: true -> "+atomic+" = 1; \n" +
						"\t\tfi;\n";
			}
			content += "\t}\n" +
					"\tod;\n" +
					"}\n";
			content += "ltl {!(" + exp + ")}\n";
			FileUtils.write(this.inputTmp, content, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		//return this.inputTmp.getAbsolutePath() + " > " + this.outputTmp.getAbsolutePath();
		return this.inputTmp.getAbsolutePath();
	}

	@Override
	protected void cleanExecution() {
		/*
		try {
			FileUtils.delete(this.inputTmp);
			//FileUtils.delete(this.outputTmp);
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
	}
	
	@Override
	protected SolverResult outputAnalysis(InputStream in) {
		InputStreamReader inread = new InputStreamReader(in);
    	BufferedReader bufferedreader = new BufferedReader(inread);
		String aux;
		SolverResult result = SolverResult.UNSAT();
		try {
			while ((aux = bufferedreader.readLine()) != null) {
			    if (aux.contains("is false")) {
			    	result = SolverResult.SAT();
			    	break;
			    } else if (aux.contains("is true")) {
			    	result = SolverResult.UNSAT();
			    	break;
			    }
			}
	    	bufferedreader.close();
	    	inread.close();
	    	in.close();
		} catch (IOException e) {
			e.printStackTrace();
			return SolverResult.ERROR();
		}
		return result;
		/*
		try {
			List<String> lines = FileUtils.readLines(this.outputTmp, "UTF-8");
			for (String line : lines) {
				if (line.contains("is false")) {
					return SolverResult.SAT;
				} else if (line.contains("is true")) {
					return SolverResult.UNSAT;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return SolverResult.ERROR;
		*/
	}

	public String getBaseName() {
		return "NUSMV";
	}
	
	@Override
	public String getName() {
		String baseName = getBaseName();
		if (this.version != "") {
			return baseName + "_" + this.version;
		}
		return baseName;
	}
	
	/**
	@Override
	protected boolean errorAnalysis(InputStream err) {
		boolean error = super.errorAnalysis(err);
		if (error) {
			return error;
		}
		return error;
	}
	**/
}
	
