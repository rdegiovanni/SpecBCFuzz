package ltl.gore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;

public class Spec {
	
    private List<Formula> domain = new LinkedList<Formula>();
    private List<Formula> goals = new LinkedList<Formula>();
    private List<String> variables = new LinkedList<String>();
    private List<String> ins = new LinkedList<String>();
    private List<String> outs = new LinkedList<String>();
    private int numberOfInputs = 0;
    //private SyntacticSimplifier simp = new SyntacticSimplifier();
    //private SolverSyntaxOperatorReplacer visitor  = new SolverSyntaxOperatorReplacer();
    
    public Spec(List<Formula> domain, List<Formula> goals, List<String> ins, List<String> outs) {
        this.domain.addAll(domain);
        this.goals.addAll(goals);
        this.numberOfInputs = ins.size();
        this.ins.addAll(ins);
        this.outs.addAll(outs);
        this.variables.addAll(ins);
        this.variables.addAll(outs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Spec tuple2 = (Spec) o;

        if (domain != null ? !domain.equals(tuple2.domain) : tuple2.domain != null) return false;
        if (goals != null ? !goals.equals(tuple2.goals) : tuple2.goals != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = domain != null ? domain.hashCode() : 0;
        result = 31 * result + (goals != null ? goals.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "(" + domain + ',' + goals + ')';
    }
    
    public String toLabelledString() {
    	return "(" + LabelledFormula.of(Conjunction.of(this.domain), this.variables) + 
    			',' + LabelledFormula.of(Conjunction.of(this.goals), this.variables) + ')';
    }
    
    public LabelledFormula toFormula() {
		Formula left = Conjunction.of(this.domain);
		Formula right = Conjunction.of(this.goals);
		return LabelledFormula.of(Disjunction.of(left.not(), right), this.variables); // Assumptions -> Goals
    }
    
    public List<Formula> getAssume() {
    	return this.domain;
    }
    
    public List<Formula> getGoals() {
    	return this.goals;
    }

    public int getNumberOfInputs() {
    	return this.numberOfInputs;
    }
    
    public List<String> getVariables(){
    	return this.variables;
    }
    
    public void setAssume(List<Formula> assume) {
    	this.domain.clear();
    	this.domain.addAll(assume);
    }
    
    public void setGuarantees(List<Formula> goals) {
    	this.goals.clear();
    	this.goals.addAll(goals);
    }

	public List<String> getIns() {
		return this.ins;
	}

	public List<String> getOuts() {
		return this.outs;
	}
	
	public Formula getFormula() {
		Formula left = Conjunction.of(this.domain);
		Formula right = Conjunction.of(this.goals);
		return Disjunction.of(left.not(), right); // Assumptions -> Goals
	}
	
	public Spec clone() {
		List<Formula> domain = new ArrayList<Formula>(this.domain);
		List<Formula> goals = new ArrayList<Formula>(this.goals);
		List<String> ins = new ArrayList<String>(this.ins);
		List<String> outs = new ArrayList<String>(this.outs);
		Spec newSpec = new Spec(domain, goals, ins, outs);
		assert(newSpec.equals(this));
		return newSpec;
	}
	
	/*
	public boolean isBoundaryCondition(Formula bc) throws IOException, InterruptedException{
		// Check non-triviality
		if (isTrivial(bc)) return false;
		
		// Check logical inconsistency
		if (isConsistent(bc)) return false;
		
		// check minimality
		return isMinimal(bc);
	}
	
	public boolean isTrivial(Formula bc) {
		Formula guarantees = Conjunction.of(this.goals);
		return Formula_Utils.areEquivalent(bc, guarantees.not());
	}
	
	public boolean isConsistent(Formula bc) throws IOException, InterruptedException {
		Formula environment_bc = Conjunction.of(Conjunction.of(this.domain),bc); //Assum & bc
		Formula assumes_guarantees = Conjunction.of(Conjunction.of(this.goals), environment_bc); // assum & guar & bc
		Formula environment2 = assumes_guarantees.accept(visitor);
		return (LTLSolver.isSAT(Formula_Utils.toSolverSyntax(environment2))).equals(SolverResult.SAT);			
	}
	
	public boolean isMinimal(Formula bc) throws IOException, InterruptedException {
		Formula environment_bc = Conjunction.of(Conjunction.of(this.domain),bc); //Assum & bc
		Formula assumes_guarantees; // assum & guar & bc
		Formula environment2;
		
		List<Formula> gcopy = new LinkedList<Formula>();
		gcopy.addAll(this.goals);
		
		for (int i = 0; i < this.goals.size(); i++) {
			gcopy.remove(i);
			
			assumes_guarantees = Conjunction.of(Conjunction.of(gcopy), environment_bc);
			environment2 = assumes_guarantees.accept(visitor);
			//if (LTLSolver.isSAT(Formula_Utils.toSolverSyntax(environment2)).equals(SolverResult.UNSAT))
			if (!LTLSolver.isSAT(Formula_Utils.toSolverSyntax(environment2)).equals(SolverResult.SAT))
				return false;
			
			gcopy.add(i, this.goals.get(i));
		}
		return true;
	}
	*/
	
}
