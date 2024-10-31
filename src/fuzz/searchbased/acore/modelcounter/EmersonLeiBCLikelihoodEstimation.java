package fuzz.searchbased.acore.modelcounter;


import it.unimi.dsi.fastutil.ints.IntArrayList;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import experiment.ExperimentSetting;
import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.FieldMatrix;
import owl.automaton.Automaton;
import owl.automaton.acceptance.BuchiAcceptance;
import owl.automaton.acceptance.EmersonLeiAcceptance;
import owl.automaton.acceptance.ParityAcceptance;
import owl.automaton.edge.Edge;
import owl.automaton.output.HoaPrinter;
import owl.ltl.LabelledFormula;
import owl.run.DefaultEnvironment;
import owl.translations.delag.DelagBuilder;
import owl.translations.ltl2dpa.LTL2DPAFunction;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.IntConsumer;

public class EmersonLeiBCLikelihoodEstimation<S> {

	private FieldMatrix<BigFraction> T = null;
	private Automaton<S, EmersonLeiAcceptance> automaton = null;
	private LabelledFormula formula = null;
    private Object[] states = null;
	//public static int TIMEOUT = 300;

	public EmersonLeiBCLikelihoodEstimation(LabelledFormula formula) {
		this.formula = formula;
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		// Do the call in a separate thread, get a Future back
		Future<String> future = executorService.submit(this::parse);
		try {
			// Wait for at most TIMEOUT seconds until the result is returned
			String result = future.get(ExperimentSetting.PARSING_TIMEOUT, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			System.out.println("EmersonLeiAutomatonBasedModelCounting: TIMEOUT parsing.");
		}
		catch (InterruptedException | ExecutionException e) {
			System.err.println("EmersonLeiAutomatonBasedModelCounting: ERROR while parsing. " + e.getMessage());
		}
	}

	private String parse() {
		// Convert the ltl formula to an automaton with OWL
		DelagBuilder translator = new DelagBuilder(DefaultEnvironment.standard());
		automaton = (Automaton<S, EmersonLeiAcceptance>) translator.apply(formula);
//		var translator = new LTL2DPAFunction(DefaultEnvironment.standard(), LTL2DPAFunction.RECOMMENDED_SYMMETRIC_CONFIG);
//		automaton = (Automaton<S, ParityAcceptance>) translator.apply(formula);
//		System.out.println(HoaPrinter.toString(automaton));
//		var environment = DefaultEnvironment.standard();
//		var translator = new LTL2DPAFunction(environment, EnumSet.of(
////				OPTIMISE_INITIAL_STATE,
//				COMPLEMENT_CONSTRUCTION,
//				GREEDY,
//				COMPRESS_COLOURS));
////				LTL2DPAFunction.RECOMMENDED_SYMMETRIC_CONFIG);
//
//		automaton = (Automaton<S, ParityAcceptance>) translator.apply(formula);

		states = automaton.states().toArray();
		Arrays.sort(this.states, new Comparator<Object>() {
	        @Override
			public int compare(Object o1, Object o2) {
	            return (o1.toString()).compareTo(o2.toString());
	        }
		});
		return "OK";
	}


	public  BigInteger count(int bound) {
		//We compute uTkv, where u is the row vector such that ui = 1 if and only if i is the start state and 0 otherwise,
		// and v is the column vector where vi = 1 if and only if i is an accepting state and 0 otherwise.
		if (states == null)
			return BigInteger.ZERO;
		BOUND = bound;
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		// Do the call in a separate thread, get a Future back
		Future<BigInteger> future = executorService.submit(this::countModels);
		try {
			// Wait for at most TIMEOUT seconds until the result is returned
			BigInteger result = future.get(ExperimentSetting.MC_TIMEOUT, TimeUnit.SECONDS);
			return result;
		} catch (TimeoutException e) {
			System.out.println("EmersonLeiAutomatonBasedModelCounting::count TIMEOUT.");
		}
		catch (InterruptedException | ExecutionException e) {
			System.err.println("EmersonLeiAutomatonBasedModelCounting::count ERROR. " + e.getMessage());
		}
		return BigInteger.ZERO;
	}

	int BOUND = 0;
	private BigInteger countModels() {
		T = buildTransferMatrix();
//		printMatrix(T);
//		int n = T.getRowDimension();

		//set initial states
		FieldMatrix<BigFraction> u = buildInitialStates() ;
//		printMatrix(u);
		//set final states
		FieldMatrix<BigFraction> v =  buildFinalStates();
//		printMatrix(v);
		// count models
		FieldMatrix<BigFraction> T_res = T.power(BOUND);
//		printMatrix(T_res);
		FieldMatrix<BigFraction> reachable = u.multiply(T_res);
//		System.out.println("reachable: " + reachable.toString());
		FieldMatrix<BigFraction> result = reachable.multiply(v);
//		System.out.println("result: " + result.toString());
		BigFraction value = (BigFraction)result.getEntry(0,0);
		BigInteger count = value.getNumerator();
		return count;
	}

	  /**
	   * Build the Transfer Matrix for the given DFA
	   * @param automaton is the DFA
	   * @return a n x n matrix M where M[i,j] is the number of transitions from state si to state sj
	   */
	long final_transitions = 0;
	long no_final_transitions = 0;
	public  FieldMatrix<BigFraction> buildTransferMatrix() {
	  	  //state N+1 will be used a sink final state to count the first time the BC has been satisfied
		  int n = automaton.size();
		  BigFraction[][] pData = new BigFraction[n+1][n+1];
		  for (int i=n; i<n+1;i++) {
			  for (int j=0; j<n+1;j++)
				  pData[i][j] = BigFraction.ZERO;
		  }



		  for (int i = 0;i<n;i++) {
			  S si = (S) states[i];
			  final_transitions = 0;
			  for (int j = 0; j < n; j++) {
				  S sj = (S) states[j];
//				  int u = j;
				  no_final_transitions = 0;
				  automaton.factory().universe().forEach(valuation -> {
					  Set<Edge<S>> edges = automaton.edges(si, valuation);
					  for (Edge<S> edge : edges) {
						  if (edge.successor().equals(sj)) {
							  //check if it is an acceptance transition
							  IntArrayList acceptanceSets = new IntArrayList();
							  if (edge.acceptanceSetIterator().hasNext())
								  edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
							  if (accConditionIsSatisfied(automaton.acceptance().booleanExpression(), acceptanceSets)) {
								  final_transitions++;
							  }
							  else
								  no_final_transitions++;
						  }
					  }
				  });
				  pData[i][j] = new BigFraction(no_final_transitions);
			  }
			  pData[i][n] =  new BigFraction(final_transitions);
		  }

//		  pData[n][n] =  new BigFraction(1);
		  return new Array2DRowFieldMatrix<BigFraction>(pData, false);
	  }

	  public FieldMatrix<BigFraction> buildInitialStates() {
		  int n = T.getRowDimension();
		  //set initial states
		  FieldMatrix<BigFraction> u = createMatrix(1,n);
		  Set<S> initial_states = automaton.initialStates();
		  for(int j = 0; j < n-1; j++) {
			  if (initial_states.contains(states[j]))
				  u.addToEntry(0, j,new BigFraction(1));
//			else
//				u.addToEntry(0, j,new BigFraction(0));
		  }
		  return u;
	  }

	public FieldMatrix<BigFraction> buildFinalStates() {
		int n = T.getRowDimension();
		//state N is the final state
		FieldMatrix<BigFraction> v = createMatrix(n, 1);
//		for (int i = 0; i < n-1; i++) {
//			v.addToEntry(i, 0, new BigFraction(0));
//		}
		v.addToEntry(n-1, 0, new BigFraction(1));
		return v;
	}

	public FieldMatrix<BigFraction> createMatrix(int row, int column) {
		BigFraction[][] pData = new BigFraction[row][column];
		for (int i = 0; i<row; i++){
			for (int j = 0; j<column; j++){
				pData[i][j] = new BigFraction(0);
			}
		}
		return new Array2DRowFieldMatrix<BigFraction>(pData, false);
	}

	public void printMatrix(FieldMatrix<BigFraction> M) {
		int row = M.getRowDimension();
		int column = M.getColumnDimension();
		for (int i = 0; i<row; i++){
			for (int j = 0; j<column; j++){
				System.out.print(M.getEntry(i,j) + " ");
			}
			System.out.println();
		}
	}


    public boolean accConditionIsSatisfied(BooleanExpression<AtomAcceptance> acceptanceCondition, IntArrayList acceptanceSets) {
        boolean accConditionSatisfied = false;
        switch(acceptanceCondition.getType()) {
            case EXP_TRUE: { accConditionSatisfied = true; break; }
            case EXP_FALSE: break;
            case EXP_ATOM:
            {
                if (acceptanceCondition.getAtom().getType() == AtomAcceptance.Type.TEMPORAL_INF)
                    accConditionSatisfied = (acceptanceSets.contains(acceptanceCondition.getAtom().getAcceptanceSet()));
                else if (acceptanceCondition.getAtom().getType() == AtomAcceptance.Type.TEMPORAL_FIN) {
                    accConditionSatisfied = ! (acceptanceSets.contains(acceptanceCondition.getAtom().getAcceptanceSet()));
                }
                break;
            }
            case EXP_AND:
            {
                if (accConditionIsSatisfied(acceptanceCondition.getLeft(), acceptanceSets))
                    accConditionSatisfied = accConditionIsSatisfied(acceptanceCondition.getRight(), acceptanceSets);
                break;
            }
            case EXP_OR:
            {
                if (accConditionIsSatisfied(acceptanceCondition.getLeft(), acceptanceSets))
                    accConditionSatisfied = true;
                else
                    accConditionSatisfied = accConditionIsSatisfied(acceptanceCondition.getRight(), acceptanceSets);
                break;
            }
            case EXP_NOT: {
                accConditionSatisfied = !accConditionIsSatisfied(acceptanceCondition.getRight(), acceptanceSets);
                break;
            }
        }

        return accConditionSatisfied;
    }
	  
	 
}
