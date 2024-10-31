package fuzz.grammar;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

import fuzz.Fuzzer;
import owl.ltl.Formula;

public class GrammarFuzzer implements Fuzzer {
	
	private Random random;
	
	private int maxNonTerminals;
	
	private LTLGrammar grammar;
	
	private double terminalChoiceProbability;
		
	public GrammarFuzzer(LTLGrammar grammar, Random random, double terminalChoiceProbability, int maxNonTerminals) {
		this.grammar = grammar;
		this.random = random;
		this.maxNonTerminals = maxNonTerminals;
		if (terminalChoiceProbability > 1) {
			this.terminalChoiceProbability = 1;
		} else if (terminalChoiceProbability < 0) {
			this.terminalChoiceProbability = 0;
		} else {
			this.terminalChoiceProbability = terminalChoiceProbability;
		}
	}
	
	public List<String> getAtomicPropositions() {
		List<String> lAtomicPropositions = new ArrayList<String>();
		for (int i = 0; i < this.grammar.getMaxLiteral(); ++i) {
			lAtomicPropositions.add("p" + i);
		}
		return lAtomicPropositions;
	}
	
	public Pair<Formula,List<String>> fuzz() {
		final int currentNonTerminals = 0;
		Pair<Formula,List<String>> result = null;
		try {
			result = Pair.of(search(nonTerminal(), currentNonTerminals),getAtomicPropositions());
		} catch (RuntimeException e) {
			result = fuzz();
			e.printStackTrace();
		}
		return result;
	}

	private Formula search(Class<?> instanciableFormulaClass, int currentNonTerminals) throws RuntimeException {
		Class<?> clasz = instanciableFormulaClass;
		Constructor<?>[] constructors = clasz.getConstructors();
		int limit = constructors.length;
		boolean alwaysTerminals = false;
		if (currentNonTerminals >= maxNonTerminals) {
			alwaysTerminals = true;
			//throw new RuntimeException("Exceeded maximum non terminal");
			//return null;
		} 
		if (limit == 0 && this.grammar.isTerminal(clasz)) {
			return this.grammar.instanciateTerminal(instanciableFormulaClass);
		} else {
			List<Formula> subformulas = new ArrayList<Formula>();
			if (limit == 0) {
				int size = random.nextInt(this.grammar.getMaxLiteral());
				if (size < 2) {
					size = 2;
				}
				for (int i = 0; i < size; ++ i) {
					double choice = this.random.nextDouble();
					Formula subFormula = null;
					if (choice < this.terminalChoiceProbability || alwaysTerminals) {
						subFormula = search(terminal(), currentNonTerminals);
					} else {
						subFormula = search(nonTerminal(), currentNonTerminals + 1);
					}
					if (subFormula == null) {
						return null;
					}
					subformulas.add(subFormula);
				}
				try {
					Method method = clasz.getDeclaredMethod("of", Formula[].class);
					Formula[] formulas = new Formula[subformulas.size()];
					int i = 0;
					for (Formula form : subformulas) {
						formulas[i] = form;
						++i;
					}
					return (Formula) method.invoke(clasz, new Object[]{formulas});
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
					//Method method = clasz.getMethod("of", subformulas.toArray());
							//clasz.getDeclaredMethod("of", subformulas.toArray());
					
					//return method.invoke(clasz, subformulas.toArray());
					
					//Formula formula = (Formula) .newInstance(subformulas.toArray());
					//return formula;
				return null;
			} else {
				int index = this.random.nextInt(limit);
				Constructor<?> constructor = constructors[index];
				Class<?>[] parameters = (Class<?>[]) constructor.getParameterTypes();
				for (int i = 0; i < parameters.length; ++i) {
					double choice = this.random.nextDouble();
					Formula subFormula = null;
					if (choice < this.terminalChoiceProbability || alwaysTerminals) {
						subFormula = search(terminal(), currentNonTerminals);
					} else {
						subFormula = search(nonTerminal(), currentNonTerminals + 1);
					}
					if (subFormula == null) {
						return null;
					}
					subformulas.add(subFormula);
				}
				try {
					Formula formula = (Formula) constructor.newInstance(subformulas.toArray());
					return formula;
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					e.printStackTrace();
					return null;
				}
			}
		}
	}
	
	private Class<?> terminal() {
		Class<?>[] terminals = this.grammar.terminals();
		int lenght = terminals.length;
		int index = this.random.nextInt(lenght);
		return terminals[index];
	}
	
	private Class<?> nonTerminal() {
		Class<?>[] nonTerminals = this.grammar.nonTerminals();
		int lenght = nonTerminals.length;
		int index = this.random.nextInt(lenght);
		return nonTerminals[index];
	}

}	
	/*
	public void fuzz(LTLGrammar grammar, Formula startSymbol, int maxNonTerminals) {
		Formula term = startSymbol;
		while (grammar.nonTerminals(term).length > 0) {
			Formula newTerm;
			Formula[] nonTerminals = grammar.nonTerminals(term);
			if (nonTerminals.length > 0) {
				//choice
				newTerm = nonTerminals[0];
			} else {
				Formula[] terminals = grammar.terminals(term);
				//choice;
			}
			Formula newTerm = grammar.nonTerminals(term)[0]; //random
			if () {
				
			}

		}
	}
	*/
	
	/*
	public Formula fuzz() {
		Class<Formula> instanciableFormulaClass = firstInstanciableFormulaClass();
		formulaGenerator(instanciableFormulaClass,this.maxNonTerminals);
	}
	
	private Class<Formula> firstInstanciableFormulaClass() {
		Class<Formula> clasz = Formula.class;
	}
	*/
	
	/*
	
	private Formula formulaGenerator(Class<Formula> instanciableFormulaClass, int maxNonTerminals) {
		Class<Formula> clasz = instanciableFormulaClass;
		Constructor<Formula>[] constructors = (Constructor<Formula>[]) clasz.getConstructors();
		int limit = constructors.length;
		if (limit == 0) {
			return terminals(instanciableFormulaClass);
		} else {
			return nonTerminals(constructors, maxNonTerminals);
		}
	}
	
	private Formula terminals(Class<Formula> terminalClass) {
		String name = terminalClass.getName();
		if (name.equals("Literal")) {
			
		} else if (name.equals("BooleanConstant")) {
			
		}
		return null;
	}
	
	private Formula nonTerminals(Constructor<Formula>[] constructors, int maxNonTerminals) {
		int limit = constructors.length;
		int index = this.random.nextInt(limit);
		Constructor<Formula> constructor = constructors[index];
		Class<Formula>[] parameters = (Class<Formula>[]) constructor.getParameterTypes();
		List<Formula> subformulas = new ArrayList<Formula>();
		for (Class<Formula> parameter : parameters) {
			Formula subFormula = formulaGenerator(parameter, maxNonTerminals);
			if (subFormula == null) {
				return null;
			}
			subformulas.add(subFormula);
		}
		try {
			Formula formula = constructor.newInstance(subformulas);
			return formula;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}
	*/