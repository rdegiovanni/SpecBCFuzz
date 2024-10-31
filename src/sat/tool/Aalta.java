package sat.tool;

import java.io.File;
import java.io.IOException;

import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.MOperator;
import owl.ltl.UOperator;
import owl.ltl.WOperator;
import owl.ltl.visitors.Visitor;
import sat.SyntaxOperatorReplacer;

public class Aalta extends LTLSolver {
	
	private String version = "";

	public Aalta(File binPath, int maxAtomicPropositions, String version) {
		super(binPath, maxAtomicPropositions);
		this.version = version;
		super.setPerformance(Performance.POSIX);
	}
	
	@Override
	protected Process runCommand(String cmd, String exp) throws IOException {
		super.startExecutionTime();
		Process p;
		//if (super.isPerformance(Performance.POSIX)) {
			final int length = cmd.split(" ").length + 1;
			String[] cmdAndParameters = new String[length];
			String[] tmpCmdAndParameters = cmd.split(" ");
			for (int i = 0; i < tmpCmdAndParameters.length; ++i) {
				cmdAndParameters[i] = tmpCmdAndParameters[i];
			}
			cmdAndParameters[length - 1] = exp;
			p = Runtime.getRuntime().exec(cmdAndParameters);
			//p = Runtime.getRuntime().exec(cmd + " \'" + exp + "\'");
		/*
		} else {
			p = Runtime.getRuntime().exec(new String[]{cmd,exp});
		}
		*/
		//Runtime.getRuntime().exec(new String[]{cmd,exp});
		super.stopExecutionTime();
		return p;
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
		};
		Formula result = formula.accept(operatorReplacer);
		return result.accept(operatorReplacer);
	}

	@Override
	public String getName() {
		return "Aalta_" + this.version;
	}

}