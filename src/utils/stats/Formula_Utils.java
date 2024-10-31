package utils.stats;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.Formulas;
import owl.ltl.LabelledFormula;
import owl.ltl.Literal;
import owl.ltl.XOperator;
import owl.ltl.parser.LtlParser;

public class Formula_Utils {

	public static List<LabelledFormula> subformulas (LabelledFormula f) {//, List<String> variables) {
		List<LabelledFormula> s = new LinkedList<LabelledFormula>();
		
		for (Formula c : f.formula().children()) {
			LabelledFormula sf = LabelledFormula.of(c, f.variables());
			for(LabelledFormula e : subformulas(sf))
				s.add(e);		
		}
		s.add(LabelledFormula.of(f.formula(),f.variables()));
		return s;
	}

	public static Set<Formula> subformulas (Formula f) {//, List<String> variables) {
		Set<Formula> s = new HashSet<>();

		for (Formula c : f.children()) {
			for(Formula e : subformulas(c))
				s.add(e);
		}
		s.add(f);
		return s;
	}

	public static int compare(Formula f0, Formula f1) {
		return Formulas.compare(subformulas(f0), subformulas(f1));
	}
	
	public static int formulaSize (Formula f) {//, List<String> variables) {
		int size = 1;
		for (Formula c : f.children())
			size += formulaSize(c);		
		return size;
	}
	
	public static List<LabelledFormula> splitConjunction (LabelledFormula f){
		List<LabelledFormula> conjuncts = new LinkedList<>();
		if (f.formula() instanceof Conjunction) {
		      Conjunction conjunction = (Conjunction) f.formula();
		      for (Formula c : conjunction.children)
		    	  conjuncts.add(LabelledFormula.of(c, f.variables()));
		      
	    }
		else
			conjuncts.add(f);
		return conjuncts;
	}
	
	public static List<Formula> splitConjunction (Formula f){
		List<Formula> conjuncts = new LinkedList<>();
		if (f instanceof Conjunction) {
		      Conjunction conjunction = (Conjunction) f;
		      for (Formula c : conjunction.children)
		      	if (c != BooleanConstant.TRUE)
		      		conjuncts.addAll(splitConjunction(c));
	    }
		else if (f != BooleanConstant.TRUE)
			conjuncts.add(f);
		return conjuncts;
	}

	public static List<Formula> splitConjunctions(List<Formula> formulas){
		List<Formula> conjuncts = new LinkedList<>();
		for (Formula f : formulas) {
			if (f instanceof Conjunction) {
				Conjunction conjunction = (Conjunction) f;
				for (Formula c : conjunction.children)
					if (c != BooleanConstant.TRUE)
						conjuncts.addAll(splitConjunction(c));
			} else if (f != BooleanConstant.TRUE)
				conjuncts.add(f);
		}
		return conjuncts;
	}

	public static int numOfTemporalOperators(Formula formula) {
		if (formula == null || formula instanceof Literal)
			return 0;
		if (formula instanceof Formula.TemporalOperator && !(formula instanceof  XOperator)) {
			int max = 0;
			for (Formula c : formula.children()) {
				int aux = numOfTemporalOperators(c);
				if (max < aux)
					max = aux;
			}
			return max + 1;
		}
		if (formula instanceof Formula.LogicalOperator) {
			int max = 0;
			for (Formula c : formula.children()) {
				int aux = numOfTemporalOperators(c);
				if (max < aux)
					max = aux;
			}
			return max;
		}
		return 0;
	}

	public static List<Formula> toFormulaList(List<String> lst, List<String> ins, List<String> outs) {
		List<Formula> res = new LinkedList<Formula>();
		List<String> vars = new LinkedList<String>();
		vars.addAll(ins); vars.addAll(outs);
		for (String bc : lst) {
			res.add(LtlParser.parse(bc, vars).formula());
		}
		return res;
	}

	public static String toSolverSyntax(Formula f) {
		String LTLFormula = f.toString();
		LTLFormula = LTLFormula.replaceAll("\\!", "~");
		LTLFormula = LTLFormula.replaceAll("([A-Z])", " $1 ");
		LTLFormula = LTLFormula.replaceAll("_", "");
		return new String(LTLFormula); 
	}
	
	public static String toGAConflictIdentificationSyntax(Formula f) {
		String LTLFormula = f.toString();
		return toGAConflictIdentificationSyntax(LTLFormula);
	}

	public static String toGAConflictIdentificationSyntax(String LTLFormula) {
		LTLFormula = LTLFormula.replaceAll("\\!", "! ");
		LTLFormula = LTLFormula.replaceAll("([A-Z])", " $1 ");
		LTLFormula = LTLFormula.replaceAll("_", "");
		LTLFormula = LTLFormula.replaceAll("G", "[]");
		LTLFormula = LTLFormula.replaceAll("F", "<>");
		LTLFormula = LTLFormula.replaceAll("&", " && ");
		LTLFormula = LTLFormula.replaceAll("\\|", " || ");
		return new String(LTLFormula);
	}

	public static String translatorBCLearnLanguateToOWL(String str) {
		String result = "";
		char[] strArray = str.toCharArray();
		int lastPoint = 0;
		int i = 0;
		while (i < str.length()) {
			if (strArray[i] == '<' && strArray[i+1] == '>') {
				result += new String(strArray, lastPoint, i - lastPoint) + "F";
				i = i + 2;
				lastPoint = i;
			} else if (strArray[i] == '[') {
				result += new String(strArray, lastPoint, i - lastPoint) + "G";
				i = i + 2;
				lastPoint = i;
			} else if (strArray[i] == '/') {
				result += new String(strArray, lastPoint, i - lastPoint) + "&&";
				i = i + 2;
				lastPoint = i;
			} else if (strArray[i] == '\\') {
				result += new String(strArray, lastPoint, i - lastPoint) + "||";
				i = i + 2;
				lastPoint = i;
			} else if (strArray[i] == 'V') {
				result += new String(strArray, lastPoint, i - lastPoint) + "R";
				i = i + 1;
				lastPoint = i;
			} else {
				++i;
			}
		}
		if (i != lastPoint) {
			result += new String(strArray, lastPoint, i - lastPoint);
		}
		return result;
	}
}
