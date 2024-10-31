package fuzz.benchmarks;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import fuzz.Fuzzer;
import fuzz.Seeds;
import ltl.gore.Spec;
import owl.ltl.Formula;

public class BenchmarksFuzz implements Fuzzer {

	public Seeds seeds;
	
	public BenchmarksFuzz(Seeds seeds) {
		this.seeds = seeds;
	}
	
	public Pair<Formula,List<String>> fuzz() {
		Triple<Spec,List<Formula>,String> seed = this.seeds.next();
		Spec spec = seed.getLeft();
		//return Pair.of(spec.getFormula(),spec.getVariables());
		if (spec != null) {
			return Pair.of(spec.getFormula(),spec.getVariables());
		} else {
			return null;
		}
	}
	
}