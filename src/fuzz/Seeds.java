package fuzz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;

import ltl.gore.Spec;
import ltl.gore.SpecReader;
import owl.ltl.Formula;

public class Seeds {
	
	private List<Spec> specs;
	private List<List<Formula>> bcs;
	private List<String> names;
	private List<File> specsFiles;
	private SpecReader specReader;
	private int index = 0;
	private int size = 0;
	
	public Seeds() {
		this.specs = new LinkedList<Spec>();
		this.bcs = new ArrayList<List<Formula>>();
		this.names = new ArrayList<String>();
		this.specsFiles = new ArrayList<File>();
	}
	
	public Seeds(SpecReader specReader) {
		this.specs = new LinkedList<Spec>();
		this.bcs = new ArrayList<List<Formula>>();
		this.names = new ArrayList<String>();
		this.specsFiles = new ArrayList<File>();
		this.specReader = specReader;
	}

	public int size() {
		return this.size;
	}
	
	public List<Spec> getSpecs() {
		return new ArrayList<Spec>(this.specs);
	}

	public List<List<Formula>> getBCs() {
		List<List<Formula>> bcs = new ArrayList<List<Formula>>();
		for (List<Formula> bcSet: this.bcs) {
			bcs.add(new ArrayList<Formula>(bcSet));
		}	
		return bcs;
	}
	
	public void add(Spec spec) {
		add(spec,null,null);
	}
	
	public void add(Spec spec, List<Formula> bcs) {
		add(spec,bcs,null);
	}
	
	public void add(Spec spec, String name) {
		add(spec,null,name);
	}
	
	public void add(Spec spec, List<Formula> bcs, String name) {
		this.specs.add(spec);
		this.specsFiles.add(null);
		if (bcs != null) {
			this.bcs.add(bcs);
		} else {
			this.bcs.add(new ArrayList<Formula>());
		}
		if (name != null) { 
			this.names.add(name);
		} else {
			this.names.add("");
		}
		++this.size;
	}
	
	public void add(File specFile) {
		add(specFile,null,null);
	}
	
	public void add(File specFile, List<Formula> bcs) {
		add(specFile,bcs,null);
	}
	
	public void add(File specFile, String name) {
		add(specFile,null,name);
	}
	
	public void add(File specFile, List<Formula> bcs, String name) {
		this.specsFiles.add(specFile);
		this.specs.add(null);
		if (bcs != null) {
			this.bcs.add(bcs);
		} else {
			this.bcs.add(new ArrayList<Formula>());
		}
		if (name != null) { 
			this.names.add(name);
		} else {
			this.names.add("");
		}
		++this.size;
	}
	
	/*
	 * Spec, BCs and spec-name
	 */
	public Triple<Spec,List<Formula>,String> next() {
		int index = 0;
		if (this.index < this.size) {
			index = this.index;
			++this.index;
		} else {
			this.index = 0;
		}
		Spec spec = this.specs.get(index);
		if (spec == null) {
			try {
				File specFile = this.specsFiles.get(index);
				System.out.println("Reading " + specFile.getAbsolutePath());
				spec = this.specReader.read(specFile);
			} catch (IOException e) {
				e.printStackTrace();
				spec = null;
			}
		}
		return Triple.of(spec, this.bcs.get(index), this.names.get(index));
	}

}
