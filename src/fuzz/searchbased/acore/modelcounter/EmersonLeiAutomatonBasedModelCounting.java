package fuzz.searchbased.acore.modelcounter;


import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntConsumer;

import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.FieldMatrix;

import experiment.ExperimentSetting;
import fuzz.searchbased.acore.ltl.owl.visitors.SolverSyntaxOperatorReplacer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import owl.automaton.Automaton;
import owl.automaton.acceptance.EmersonLeiAcceptance;
import owl.automaton.edge.Edge;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.FOperator;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.LabelledFormula;
import owl.ltl.ROperator;
import owl.ltl.UOperator;
import owl.ltl.WOperator;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.run.DefaultEnvironment;
import owl.translations.delag.DelagBuilder;

public class EmersonLeiAutomatonBasedModelCounting<S> {

	private FieldMatrix<BigFraction> T = null;
	private Automaton<S,EmersonLeiAcceptance> automaton = null;
	private LabelledFormula formula = null;
    private Object[] states = null;
	//public static int TIMEOUT = 300;

	public EmersonLeiAutomatonBasedModelCounting(LabelledFormula formula) {		
		SyntacticSimplifier simp = new SyntacticSimplifier();
		
		SolverSyntaxOperatorReplacer visitor  = new SolverSyntaxOperatorReplacer() {
			@Override
			public Formula visit(FOperator fOperator) {
				Formula operand = fOperator.operand;
				return UOperator.of(BooleanConstant.TRUE, operand);
			}
			
			@Override
			public Formula visit(GOperator gOperator) {
				Formula operand = gOperator.operand;
				return UOperator.of(BooleanConstant.TRUE, operand.not()).not();
			}
			
			@Override
			public Formula visit(ROperator rOperator) {
				// p R q" <-> q w (q && p) <-> (q U (q && p)) || G q 
				Formula left = rOperator.left.accept(this);
				Formula right = rOperator.right.accept(this);
				Formula uOperator = UOperator.of(right, Conjunction.of(right,left));
				Formula gOperator = UOperator.of(BooleanConstant.TRUE, right.not()).not();
				Formula disjunction = Disjunction.of(uOperator,gOperator);
				return disjunction;
				//Formula wformula = WOperator.of(right,Conjunction.of(right,left));
				//return wformula.accept(this);
			}
			
			@Override
			public Formula visit(WOperator wOperator) {
				Formula left = wOperator.left.accept(this);
				Formula right = wOperator.right.accept(this);
				//return ROperator.of(right, Disjunction.of(right,left));
				Formula gOperator = UOperator.of(BooleanConstant.TRUE, left.not()).not();
				return Disjunction.of(gOperator, UOperator.of(left, right));
			}
		};
		
		Formula simplified = formula.formula().accept(visitor).accept(simp);
		LabelledFormula simp_formula = LabelledFormula.of(simplified, formula.variables());
		this.formula = simp_formula;
		
		CountDownLatch latch = new CountDownLatch(1);
		Thread parseThread = new Thread() {
			public void run() {
				parse();
				latch.countDown();
			}
		};
		parseThread.start();
		try {
			boolean await = latch.await(ExperimentSetting.PARSING_TIMEOUT, TimeUnit.SECONDS);
			if (!await) {
				throw new TimeoutException();
			}
			//System.out.println(result);
		} catch (InterruptedException e) {
			System.err.println("EmersonLeiAutomatonBasedModelCounting: ERROR while parsing. " + e.getMessage());
		} catch (TimeoutException e) {
			System.err.println("EmersonLeiAutomatonBasedModelCounting: TIMEOUT parsing.");
		}
		if (parseThread.isAlive()) {
			parseThread.stop();
		}		
		/*
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		// Do the call in a separate thread, get a Future back
		Future<String> future = executorService.submit(this::parse);
		try {
			// Wait for at most TIMEOUT seconds until the result is returned
			String result = future.get(ExperimentSetting.PARSING_TIMEOUT, TimeUnit.SECONDS);
			
			//avoid out-of-memory
			future.cancel(true);
			executorService.shutdown();
		} catch (TimeoutException e) {
			System.out.println("EmersonLeiAutomatonBasedModelCounting: TIMEOUT parsing.");
		}
		catch (InterruptedException | ExecutionException e) {
			System.err.println("EmersonLeiAutomatonBasedModelCounting: ERROR while parsing. " + e.getMessage());
		}
		*/
	}

	private String parse() {
		// Convert the ltl formula to an automaton with OWL
		DelagBuilder translator = new DelagBuilder(DefaultEnvironment.standard());
		automaton = (Automaton<S, EmersonLeiAcceptance>) translator.apply(formula);
//		System.out.println(HoaPrinter.toString(automaton, EnumSet.of(SIMPLE_TRANSITION_LABELS)));
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

		return "OK";
	}


	public  BigInteger count(int bound) {
		//We compute uTkv, where u is the row vector such that ui = 1 if and only if i is the start state and 0 otherwise,
		// and v is the column vector where vi = 1 if and only if i is an accepting state and 0 otherwise.
		if (states == null)
			return BigInteger.ZERO;
		BOUND = bound;
		COUNT = BigInteger.ZERO;
		BigInteger result = null;
		//System.out.println("count()");
		CountDownLatch latch = new CountDownLatch(1);
		Thread countModelThread = new Thread() {
			public void run() {
				try {
					countModels();
				} catch (NoDataException e) {
					COUNT = BigInteger.ZERO;
					System.err.println(e.getMessage());
				}
				latch.countDown();
			}
		};
		countModelThread.start();
		try {
			//System.out.println("waiting model couting...");
			boolean await = latch.await(ExperimentSetting.MC_TIMEOUT, TimeUnit.SECONDS);
			//System.out.println("go!");
			if (await) {
				result = COUNT;
			} else {
				result = BigInteger.ZERO;
				throw new TimeoutException();
			}
			//System.out.println(result);
		} catch (InterruptedException e) {
			System.err.println("EmersonLeiAutomatonBasedModelCounting::count ERROR. " + e.getMessage());
		} catch (TimeoutException e) {
			System.err.println("EmersonLeiAutomatonBasedModelCounting::count TIMEOUT.");
		}
		if (countModelThread.isAlive()) {
			countModelThread.stop();
		}
		return result;
		/*
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		// Do the call in a separate thread, get a Future back
		Future<BigInteger> future = executorService.submit(this::countModels);
		try {
			// Wait for at most TIMEOUT seconds until the result is returned
			BigInteger result = future.get(ExperimentSetting.MC_TIMEOUT, TimeUnit.SECONDS);
			//avoid out-of-memory
			shutdownAndAwaitTermination(executorService);
			return result;
		} catch (TimeoutException e) {
			System.out.println("EmersonLeiAutomatonBasedModelCounting::count TIMEOUT.");
		}
		catch (InterruptedException | ExecutionException e) {
			System.err.println("EmersonLeiAutomatonBasedModelCounting::count ERROR. " + e.getMessage());
		}
		return BigInteger.ZERO;
		*/
	}
	
	private int BOUND = 0;
	private BigInteger COUNT = BigInteger.ZERO; //share count result.
	private BigInteger countModels() {
		COUNT = BigInteger.ZERO;
		T = buildTransferMatrix();
//		printMatrix(T);
//		int n = T.getRowDimension();

		//set initial states
		FieldMatrix<BigFraction> u = buildInitialStates() ;

		//set final states
		FieldMatrix<BigFraction> v =  buildFinalStates();

		// count models
		FieldMatrix<BigFraction> T_res = T.power(BOUND);
//		printMatrix(T_res);
		FieldMatrix<BigFraction> reachable = u.multiply(T_res);
//		System.out.println("reachable: " + reachable.toString());
		FieldMatrix<BigFraction> result = reachable.multiply(v);
//		System.out.println("result: " + result.toString());
		BigFraction value = (BigFraction)result.getEntry(0,0);
		BigInteger count = value.getNumerator();
		//new code to avoid out-of-memory
		COUNT = new BigInteger(count.toString()); //count-copy
		return COUNT;
		//return count;
	}

	  /**
	   * Build the Transfer Matrix for the given DFA
	   * @param automaton is the DFA
	   * @return a n x n matrix M where M[i,j] is the number of transitions from state si to state sj
	   */
	  long transitions = 0;
	  public  FieldMatrix<BigFraction> buildTransferMatrix() {

		  int n = automaton.size();
		  BigFraction[][] pData = new BigFraction[n][n];
		  for (int i = 0;i<n;i++) {
			  S si = (S) states[i];
			  for (int j = 0; j < n; j++) {
				  S sj = (S) states[j];
				  transitions = 0;
				  automaton.factory().universe().forEach(valuation -> {
						  Set<Edge<S>> edges = automaton.edges(si, valuation);
						  for (Edge<S> edge : edges) {
							  if (edge.successor().equals(sj))
								transitions++;
						  }
					  });
				  BigFraction v = new BigFraction(transitions);
				  pData[i][j] = v;
			  }
		  }
		  return new Array2DRowFieldMatrix<BigFraction>(pData, false);
	  }

	  public FieldMatrix<BigFraction> buildInitialStates() {
		  int n = T.getRowDimension();
		  //set initial states
		  FieldMatrix<BigFraction> u = createMatrix(1,n);
		  Set<S> initial_states = automaton.initialStates();
		  for(int j = 0; j < n; j++) {
			  if (initial_states.contains(states[j]))
				  u.addToEntry(0, j,new BigFraction(1));
//			else
//				u.addToEntry(0, j,new BigFraction(0));
		  }
		  return u;
	  }

	public FieldMatrix<BigFraction> buildFinalStates() {
		int n = T.getRowDimension();
		//set final states
		Set<S> final_states = new HashSet<>();
		for (S s : automaton.states()) {
			Set<Edge<S>> edges = automaton.edges(s);
			for (Edge<S> edge : edges) {
				//check if it is an acceptance transition
				IntArrayList acceptanceSets = new IntArrayList();
				if (edge.acceptanceSetIterator().hasNext())
					edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
				if (accConditionIsSatisfied(automaton.acceptance().booleanExpression(), acceptanceSets)) {
					final_states.add(edge.successor());
				}
			}
		}

		FieldMatrix<BigFraction> v = createMatrix(n, 1);
		for (int i = 0; i < n; i++) {
			if (final_states.contains(states[i])) {
				v.addToEntry(i, 0, new BigFraction(1));
			}
		}
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

