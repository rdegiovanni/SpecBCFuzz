package fuzz.grammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import owl.ltl.Biconditional;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.FOperator;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.Literal;
import owl.ltl.MOperator;
import owl.ltl.ROperator;
import owl.ltl.UOperator;
import owl.ltl.WOperator;
import owl.ltl.XOperator;

public class LTLGrammar {

	private Random random;
	
	private int maxLiteral;
	
	List<Class<?>> lNonTerminals = new ArrayList<Class<?>>();
	
	List<Class<?>> lTerminals = new ArrayList<Class<?>>();
	
	private double booleanConstantProbability;
	
	public LTLGrammar(Random literalChoice, double booleanConstantProbability, int maxLiteral) { 
		this.random = literalChoice;
		this.maxLiteral = maxLiteral;
		this.initTerminals();
		this.initNonTerminals();
		if (booleanConstantProbability > 1) {
			this.booleanConstantProbability = 1;
		} else if (booleanConstantProbability < 0) {
			this.booleanConstantProbability = 0;
		} else {
			this.booleanConstantProbability = booleanConstantProbability;
		}
	}
	
	public int getMaxLiteral() {
		return maxLiteral;
	}
	
	public void addTerminal(Class<?> terminal) {
		this.addTerminal(terminal);
	}
	
	public void addNonTerminal(Class<?> nonTerminal) {
		this.addNonTerminal(nonTerminal);
	}
	
	private void initTerminals() {
		this.lTerminals.add(Literal.class);
		this.lTerminals.add(BooleanConstant.class);
	}
	
	private void initNonTerminals() {
		this.lNonTerminals.add(MOperator.class);
		this.lNonTerminals.add(XOperator.class);
		this.lNonTerminals.add(Biconditional.class);
		this.lNonTerminals.add(Conjunction.class);
		this.lNonTerminals.add(Disjunction.class);
		this.lNonTerminals.add(FOperator.class);
		this.lNonTerminals.add(GOperator.class);
		this.lNonTerminals.add(MOperator.class);
		this.lNonTerminals.add(ROperator.class);
		this.lNonTerminals.add(UOperator.class);
		this.lNonTerminals.add(WOperator.class);
		this.lNonTerminals.add(XOperator.class);
	}
	
	public void cleanNonTerminals() {
		this.lNonTerminals.clear();
	}
	
	public void cleanTerminal() {
		this.lTerminals.clear();
	}

	public Class<?>[] nonTerminals() {
		Class<?>[] nonTerminals = new Class<?>[lNonTerminals.size()];
		int i = 0;
		for (Class<?> clasz : lNonTerminals) {
			nonTerminals[i++] = clasz;
		}
		return nonTerminals;
	}

	public Class<?>[] terminals() {
		Class<?>[] terminals = new Class<?>[lTerminals.size()];
		int i = 0;
		for (Class<?> clasz : lTerminals) {
			if (clasz.getName().endsWith("BooleanConstant")) {
				double p = random.nextDouble(); // The Fuzzer will perform a random choice too, so the double of the probability.
				if (p > 1) { p = 1; }
				if (p < this.booleanConstantProbability) {
					//terminals[i++] = clasz;
					terminals[i] = clasz;
					i++;
				}
			} else {
				terminals[i] = clasz;
				i++;
			}
		}
		return Arrays.copyOf(terminals, i);
		//return terminals;
	}

	public Formula instanciateTerminal(Class<?> instanciableFormulaClass) {
		//return Literal.of(this.random.nextInt(this.maxLiteral));
		//assert(instanciableFormulaClass.equals(Literal.class) || 
		//		instanciableFormulaClass.equals(BooleanConstant.class));
		if (instanciableFormulaClass.getName().equals("owl.ltl.Literal")) {
			return Literal.of(this.random.nextInt(this.maxLiteral));
		} else if (instanciableFormulaClass.getName().equals("owl.ltl.BooleanConstant")) {
			Boolean choice = this.random.nextBoolean();
			if (choice) {
				return BooleanConstant.TRUE;
			} else {
				return BooleanConstant.FALSE;
			}
		} else {
			return null;
		}
	}

	public boolean isTerminal(Class<?> clasz) {
		for (Class<?> terminal : this.lTerminals) {
			if (terminal.getName().equals(clasz.getName())) {
				return true;
			}
		}
		return false;
	}
	
}
