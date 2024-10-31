package fuzz;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import owl.ltl.Formula;

public interface Fuzzer {

	/*
	public Formula fuzz();
	*/
	
	public Pair<Formula,List<String>> fuzz();
		
}
