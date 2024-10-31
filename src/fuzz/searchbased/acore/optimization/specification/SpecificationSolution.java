package fuzz.searchbased.acore.optimization.specification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.uma.jmetal.lab.visualization.html.impl.htmlTable.impl.MedianValuesTable.Objective;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.solutionattribute.impl.SpatialSpreadDeviation;

import ltl.gore.Spec;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.rewriter.SyntacticSimplifier;

public class SpecificationSolution implements DoubleSolution {
	
	private SpecificationSetting settings;
	
	public enum SPEC_STATUS {
		UNKNOWN, 		// UNKNOWN: the status of the specification has not been computed yet.
		BOTTOM,			// BOTTOM: both the assumptions and goals are unsatisfiable.
		ASSUMPTIONS, 	// ASSUMPTIONS: the assumptions are consistent, but not the goals.
		GUARANTEES, 	// GUARANTEES: the goals are consistent, but not the assumptions.
		CONTRADICTORY,	// CONTRADICTORY: the assumptions and goals become unsatisfiable when are putted together. 
		CONSISTENT; 	// CONSISTENT: the specification is satisfiable.
	
		public boolean compatible (SPEC_STATUS other) {
			if (this == UNKNOWN || other == UNKNOWN 
				|| this == BOTTOM || other == BOTTOM 
				|| (this == ASSUMPTIONS && other == ASSUMPTIONS) 
				|| (this == GUARANTEES && other == GUARANTEES)
				)
				return false;
			
			return true;
		}
		
		public boolean areAssumptionsSAT () {
			return (this == ASSUMPTIONS || this == CONTRADICTORY || this == CONSISTENT);
		}
		
		public boolean areGuaranteesSAT () {
			return (this == GUARANTEES || this == CONTRADICTORY || this == CONSISTENT);
		}
		
		public boolean isSpecificationConsistent () {
			return (this == CONSISTENT);
		}

		@Override
		public String toString(){
			switch (this) {
				case UNKNOWN : return "unknown";
				case BOTTOM : return "BOTTOM: both the assumptions and goals are unsatisfiable.";
				case ASSUMPTIONS: return "ASSUMPTIONS: the assumptions are consistent, but not the goals.";
				case GUARANTEES: return "GUARANTEES: the goals are consistent, but not the assumptions.";
				case CONTRADICTORY : return "CONTRADICTORY: the assumptions and goals become unsatisfiable when are putted together. ";
				case CONSISTENT:  return "CONSISTENT: the specification is satisfiable.";
			};
			return null;
		}
	};
	
	public Spec spec = null;
	
	public SPEC_STATUS status = SPEC_STATUS.UNKNOWN;
	//public double fitness = 0d;
	public List<Formula> survivalBC = new LinkedList<Formula>();
	private List<SpecificationSolution> parents = new ArrayList<SpecificationSolution>();
	
	/**
	 * Each element of the fitness.
	 */
	public double status_fitness = 1.0d;
	public double bcGradeOfImprovement = 1.0d;
	public double removedBCLikelihood = 1.0d;
	public double survivalBCLikelihood = 1.0d;
	public double lost_models_fitness = 1.0d;
	public double won_models_fitness = 1.0d;
	public double syntactic_distance = 1.0d;
	
	public SpecificationSolution() {
		spec = null;
		this.status = SPEC_STATUS.UNKNOWN;
	}

	public SpecificationSolution(Spec other) {
		this.spec = other;
		this.status = SPEC_STATUS.UNKNOWN;
	}

	public SpecificationSolution(Spec other, SpecificationSetting settings) {
		this.spec = other;
		this.status = SPEC_STATUS.UNKNOWN;
		this.settings = settings;
	}
	
	@Override
	public synchronized int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		double fitness = status_fitness + bcGradeOfImprovement + removedBCLikelihood + survivalBCLikelihood + lost_models_fitness + won_models_fitness + syntactic_distance;
		temp = Double.doubleToLongBits(fitness);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((spec == null) ? 0 : spec.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		return result;
	}

	@Override
	public synchronized boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SpecificationSolution other = (SpecificationSolution) obj;
		double fitness = status_fitness + bcGradeOfImprovement + removedBCLikelihood + survivalBCLikelihood + lost_models_fitness + won_models_fitness + syntactic_distance;
		double otherfitness = other.status_fitness + other.bcGradeOfImprovement + other.removedBCLikelihood + other.survivalBCLikelihood + other.lost_models_fitness + other.won_models_fitness + other.syntactic_distance;
		if (fitness > 0.0d && otherfitness > 0.0d && Double.doubleToLongBits(fitness) != Double.doubleToLongBits(otherfitness))
				return false;
		if (spec == null) {
			if (other.spec != null)
				return false;
		} else {
			SyntacticSimplifier simp = new SyntacticSimplifier();
			
			List<Formula> assumptions = new LinkedList<Formula>();
			List<Formula> guarantees = new LinkedList<Formula>();
			assumptions.addAll(spec.getAssume());
			guarantees.addAll(spec.getGoals());
			
			assumptions.stream().forEach((assumption) -> assumption.accept(simp));
			guarantees.stream().forEach((guarantee) -> guarantee.accept(simp));
			
			Formula left = Conjunction.of(assumptions);
			Formula right = Conjunction.of(guarantees);
			Formula thiz = Conjunction.of(left, right);
			
			List<Formula> assumptions2 = new LinkedList<Formula>();
			List<Formula> guarantees2 = new LinkedList<Formula>();
			assumptions2.addAll(other.spec.getAssume());
			guarantees2.addAll(other.spec.getGoals());
			
			assumptions2.stream().forEach((assumption) -> assumption.accept(simp));
			guarantees2.stream().forEach((guarantee) -> guarantee.accept(simp));
			
			Formula left2 = Conjunction.of(assumptions2);
			Formula right2 = Conjunction.of(guarantees2);
			Formula that = Conjunction.of(left2, right2);
			
			if (!thiz.equals(that))
				return false;
		}
		if (fitness > 0.0d && otherfitness > 0.0d && status != other.status)
			return false;
		return true;
	}
	
	public synchronized Spec getSpec() {
		return spec;
	}

	public synchronized void addParent(SpecificationSolution parentChromosome) {
		this.parents.add(parentChromosome);
	}

	public synchronized void clearParents() {
		this.parents.clear();
	}
	
	public synchronized List<SpecificationSolution> getParents() {
		return this.parents;
	}
	
	public synchronized SPEC_STATUS getStatus() {
		return this.status;
	}
	
	@Override
	public synchronized List<Double> variables() {
		throw new JMetalException("variables() was called");
	}

	@Override
	public synchronized double[] objectives() {
		//System.out.println("OBJECTIVES");
		int size = 4;
		if (this.settings == SpecificationSetting.SemSynStatusBc) {
			size = 4;
		} else if (this.settings == SpecificationSetting.SemSynStatus) {
			size = 3;
		}
		double[] objectives = new double[size];
		if (this.settings == SpecificationSetting.SemSynStatusBc) {
			objectives[0] = status_fitness;
			objectives[1] = bcGradeOfImprovement;
			objectives[2] = (lost_models_fitness + won_models_fitness) / 2d;
			objectives[3] = syntactic_distance;
		} else if (this.settings == SpecificationSetting.SemSynStatus) {
			objectives[0] = status_fitness;
			objectives[1] = (lost_models_fitness + won_models_fitness) / 2d;
			objectives[2] = syntactic_distance;
		}
		//System.out.println(objectives[0] + " " + objectives[1] + " " + objectives[2] + " " + objectives[3]);
		return objectives;
	}
	
	public synchronized SpecificationSetting getSetting() {
		return this.settings;
	}

	@Override
	public synchronized double[] constraints() {
		//throw new JMetalException("constraints() was called");
		return new double[0];
	}
	
	private Map<Object, Object> attributes = new HashMap<Object, Object>();
	
	@Override
	public synchronized Map<Object, Object> attributes() {
		//SpatialSpreadDeviation spatialSpreadDeviation = new SpatialSpreadDeviation<>();
		//Object key = spatialSpreadDeviation.getAttributeID();
		//attributes.put(key, key);
		return attributes;
	}
	
	public synchronized void setAttributes(Map<Object, Object> attributes) {
		this.attributes = new HashMap<Object, Object>(attributes);
	}
	
	@Override
	public synchronized Solution<Double> copy() {
		SpecificationSolution newSpec = new SpecificationSolution(this.spec.clone(), this.settings);
		newSpec.status = this.status;
		newSpec.survivalBC = new ArrayList<>();
		newSpec.survivalBC.addAll(this.survivalBC);
		newSpec.parents = new ArrayList<>();
		newSpec.parents.addAll(this.parents);
		newSpec.status_fitness = this.status_fitness;
		newSpec.bcGradeOfImprovement = this.bcGradeOfImprovement;
		newSpec.removedBCLikelihood = this.removedBCLikelihood;
		newSpec.survivalBCLikelihood = this.survivalBCLikelihood;
		newSpec.lost_models_fitness = this.lost_models_fitness;
		newSpec.won_models_fitness = this.won_models_fitness;
		newSpec.syntactic_distance = this.syntactic_distance;
		newSpec.setAttributes(this.attributes());
		return newSpec;
	}
	
	public synchronized void clearFitness() {
		this.status = SPEC_STATUS.UNKNOWN;
		this.survivalBC = new ArrayList<>();
		this.parents = new ArrayList<>();
		this.status_fitness = 1.0d;
		this.bcGradeOfImprovement = 1.0d;
		this.removedBCLikelihood = 1.0d;
		this.survivalBCLikelihood = 1.0d;
		this.lost_models_fitness = 1.0d;
		this.won_models_fitness = 1.0d;
		this.syntactic_distance = 1.0d;
		this.attributes = new HashMap<Object, Object>();
	}

	@Override
	public synchronized Double getLowerBound(int index) {
		throw new JMetalException("LowerBound() was called");
	}

	@Override
	public synchronized Double getUpperBound(int index) {
		throw new JMetalException("UpperBound() was called");
	}

}
