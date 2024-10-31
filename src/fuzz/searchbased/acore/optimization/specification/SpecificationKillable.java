package fuzz.searchbased.acore.optimization.specification;

import java.util.List;
import java.util.Set;

import ltl.gore.Spec;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.Literal;

public class SpecificationKillable {

	public synchronized static boolean is(Spec spec) {
		return isKillable(spec);
	}
	
	public synchronized static boolean isKillable(Spec spec) {
		return inputLiteralOnEachGoal(spec);
	}
	
	private synchronized static boolean inputLiteralOnEachGoal(Spec spec) {
		List<String> outs = spec.getOuts();
		List<Formula> goals = spec.getGoals();
		if (spec.getOuts().isEmpty()) {
			return false;
		}
		for (Formula f : goals) {
			boolean containOutLiteral = false;
			Set<Literal> literals = f.subformulas(Literal.class);
			for (Literal literal: literals) {
				LabelledFormula labelledLiteral = LabelledFormula.of(literal, spec.getVariables());
				for (String out : outs) {
					if (labelledLiteral.toString().contains(out) || labelledLiteral.not().toString().contains(out)) {
						containOutLiteral = true;
						continue;
					}
				}
				if (containOutLiteral) {
					continue;
				}
			}
			if (!containOutLiteral) {
				return true;
			}
		}
		return false;
	}
	
}
