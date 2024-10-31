package sat.tool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import owl.ltl.Biconditional;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.Literal;
import owl.ltl.MOperator;
import owl.ltl.ROperator;
import owl.ltl.UOperator;
import owl.ltl.WOperator;
import owl.ltl.visitors.Visitor;
import sat.PrintVisitor;
import sat.SolverResult;
import sat.SyntaxOperatorReplacer;

public class PLTL extends LTLSolver {

	//private final File inputTmp;
	
	private PLTL_VERSION version;
	private PLTL_PARAMETER parameter;
	
	public static enum PLTL_VERSION {
		tableau_graph,
		tableau_tree,
		bdd,
		multipass;
	}
	
	public static enum PLTL_PARAMETER {
		tableau_graph {
			public String toString() {
				return "graph";
			}
		},
		tableau_tree {
			public String toString() {
				return "tree";
			}
		},
		bdd {
			public String toString() {
				return "-sat";
			}
		},
		multipass {
			public String toString() {
				return "";
			}
		};
	}
	
	public PLTL(File binPath, int maxAtomicPropositions, PLTL_VERSION version) {
		super(binPath, maxAtomicPropositions);
		this.version = version;
		if (version == PLTL_VERSION.tableau_graph) {
			this.parameter = PLTL_PARAMETER.tableau_graph;
		} else if (version == PLTL_VERSION.tableau_tree) {
			this.parameter = PLTL_PARAMETER.tableau_tree;
		} else if (version == PLTL_VERSION.bdd) {
			this.parameter = PLTL_PARAMETER.bdd;
		} else if (version == PLTL_VERSION.multipass) {
			this.parameter = PLTL_PARAMETER.multipass;
		}
	}
	
	public PLTL(File binPath, int maxAtomicPropositions) {
		super(binPath, maxAtomicPropositions);
		this.version = PLTL_VERSION.tableau_graph;
		this.parameter = PLTL_PARAMETER.tableau_graph;
		//this.inputTmp = new File(binPath.getParentFile(),"pltl.tmpin");
	}

	@Override
	protected String formulaToString(Formula formula) {
		List<String> lAtomicPropositions = getAtomicPropositions();
		PrintVisitor printVisitor = new PrintVisitor(true, lAtomicPropositions) {
			  @Override
			  public String visit(Literal literal) {
				    String name = super.variableMapping == null
				      ? "p" + literal.getAtom()
				      : super.variableMapping.get(literal.getAtom());
				    return literal.isNegated() ? '~' + name : name;
			};
		};
		String str = formula.accept(printVisitor);
		str = str.replace("false", "False");
		str = str.replace("true", "True");
		return str;
		//return printVisitor.toString(formula, lAtomicPropositions, true);
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
			
			@Override
			public Formula visit(Biconditional biConditionalOperator) {
				Formula left = biConditionalOperator.left.accept(this);
				Formula right = biConditionalOperator.right.accept(this);
//				return Biconditional.of(left, right);
//				return Disjunction.of(Conjunction.of(left,right), Conjunction.of(left.not(),right.not()));
				return Conjunction.of(Disjunction.of(left.not(),right), Disjunction.of(left,right.not()));
			}
		};
		Formula result = formula.accept(operatorReplacer);
		return result.accept(operatorReplacer);
	}
	
	@Override
	protected String parameter(Formula formula) {
		/*
		try {
			if (this.inputTmp.exists()) {
				FileUtils.delete(this.inputTmp);
			}
			this.inputTmp.createNewFile();			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		String exp = super.parameter(formula);
		try {
			FileUtils.write(this.inputTmp, exp, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return "graph < " + this.inputTmp.getAbsolutePath();
		*/
		return super.parameter(formula);
	}
	
	@Override
	protected SolverResult outputAnalysis(InputStream in) {
		InputStreamReader inread = new InputStreamReader(in);
    	BufferedReader bufferedreader = new BufferedReader(inread);
		String aux;
		SolverResult result = SolverResult.ERROR();
		try {
			while ((aux = bufferedreader.readLine()) != null) {
			    if (aux.contains("unsatisfiable") || aux.contains("not satisfiable")) {
			    	result = SolverResult.UNSAT();
			    	break;
			    } else if (aux.contains("satisfiable")) {
			    	result = SolverResult.SAT();
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
	}
	
	@Override
	protected Process runCommand(String cmd, String exp) throws IOException {
		super.startExecutionTime();
		Process p;
		if (super.isPerformance(Performance.POSIX)) {
			p = Runtime.getRuntime().exec(cmd + " " + this.parameter.toString());
			/*
			final int length = cmd.split(" ").length;
			String[] cmdAndParameters = new String[length];
			cmdAndParameters = cmd.split(" ");
			cmdAndParameters[length - 1] = this.parameter.toString();
			System.out.println("-1------");
			for (String c : cmdAndParameters) {
				System.out.println(c);
			}
			System.out.println("-2------");
			System.out.println(cmd);
			System.out.println("-3------");
			System.out.println(this.parameter);
			System.out.println("-4------");
			p = Runtime.getRuntime().exec(cmdAndParameters);
			*/
		} else {
			p = Runtime.getRuntime().exec(new String[]{cmd, this.parameter.toString()});
		}
		//Process p = Runtime.getRuntime().exec(new String[]{cmd, this.parameter.toString()});
		super.stopExecutionTime();
		OutputStream out = p.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		writer.write(exp);
		writer.close();
		return p;
	}
	
	public String getBaseName() {
		return "PLTL";
	}
	
	private String getComplementaryName() {
		return this.version.toString();
	}
	
	@Override
	public String getName() {
		return getBaseName() + "_" + getComplementaryName();
	}
	
}