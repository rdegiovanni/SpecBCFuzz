package ltl.gore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Triple;

import fuzz.Seeds;
import ltl.owl.FormulaUtils;
import owl.ltl.Formula;
import owl.ltl.parser.LtlParser;

public class GOREReader implements SpecReader {

	/**
	 * Try to find some boundary conditions (bc) in the the provided folder.
	 * @param vars input and output variables
	 * @param bcFolder 
	 * @return boundary conditions
	 * @throws IOException
	 */
	public List<Formula> bc(List<String> vars, File bcFolder) throws IOException {
		List<Formula> bcs = null;
		if (bcFolder.exists()) {
			bcs = new ArrayList<Formula>();
			String[] files = bcFolder.list();
			for (String file : files) {
				File subFile = new File (bcFolder.getAbsoluteFile() + File.separator + file);
				Boolean bcPattern =  file.startsWith("BC") || file.startsWith("bc");
				if (subFile.isDirectory() && bcPattern) {
					List<Formula> newBcs = bc(vars,subFile);
					if (newBcs != null) {
						bcs.addAll(newBcs);
					}
				} else if (subFile.isFile() && bcPattern) {
					List<String> lines = FileUtils.readLines(subFile,"UTF-8");
					for (String line : lines) {
						try {
							bcs.add(LtlParser.parse(line, vars).formula());
						} catch (org.antlr.v4.runtime.misc.ParseCancellationException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return bcs;
	}
	
	@Override
	public Spec read(File specFile) throws IOException {
		System.out.println("Spec: " + specFile.getAbsolutePath());
		List<String> dom = new ArrayList<String>();
		List<String> goals = new ArrayList<String>();
		List<String> ins = new ArrayList<String>();
		List<String> outs = new ArrayList<String>();
		List<String> lines = FileUtils.readLines(specFile,"UTF-8");
 		for (String line : lines) {
			if (line.startsWith("-d=")) {
				dom.add(removeUnusefulCharacters(line.replace("-d=", "")));
			} else if (line.startsWith("-g=")) {
				goals.add(removeUnusefulCharacters(line.replace("-g=", "")));
			} else if (line.startsWith("-in=")) {
				ins.add(removeUnusefulCharacters(line.replace("-in=", "")));
			} else if (line.startsWith("-out=")) {
				outs.add(removeUnusefulCharacters(line.replace("-out=", "")));
			} else if (line.startsWith("-ins=")) {
				ins.addAll(splitVariables(line));
			} else if (line.startsWith("-outs=")) {
				outs.addAll(splitVariables(line));
			}
		}
		return new Spec(
				FormulaUtils.toFormulaList(dom, ins, outs),
				FormulaUtils.toFormulaList(goals, ins, outs), ins,outs);
	}
	
	private List<String> splitVariables(String line) {
		List<String> variables = new ArrayList<>();
		if (line.startsWith("-ins=")) {
			line = removeUnusefulCharacters(line.replace("-ins=", ""));
		} else if (line.startsWith("-outs=")) {
			line = removeUnusefulCharacters(line.replace("-outs=", ""));
		}
		if (line.contains(",")) {
			Arrays.asList(line.split(","));
			variables.addAll(Arrays.asList(line.split(",")));
		} else {
			variables.add(line);
		}
		return variables;
	}
	
	private String removeUnusefulCharacters(String formula) {
		return formula.replace("'", "").replace("\"", "");
	}

	private Triple<Spec,List<Formula>,String> specAndBCFolder(File specFolder) throws IOException {
		Triple<Spec,List<Formula>,String> seed = null;
		//if (specFolder.getName().startsWith(".")) continue;
		File specFile = new File(specFolder.getAbsoluteFile() + File.separator + "spec");
		File bcFolder = new File(specFolder.getAbsoluteFile() + File.separator);
		try {
			Spec spec = read(specFile);
			List<Formula> bcs = bc(spec.getVariables(), bcFolder);
			seed = Triple.of(spec, bcs, specFile.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return seed;
	}
	
	@Override
	public Seeds search(File folder) throws IOException {
		Seeds seeds = new Seeds();
		if (folder.isDirectory()) {
			File[] specsFolders = folder.listFiles();
			boolean noSubFolder = true;
			for (File file: specsFolders) {
				if (!file.getName().startsWith(".") && file.isDirectory()) {
					noSubFolder = false;
				}
			}
			if (noSubFolder) {
				Triple<Spec,List<Formula>,String> seed = specAndBCFolder(folder);
				seeds.add(seed.getLeft(), seed.getMiddle(), seed.getRight());
			} else {
				for (File specFolder : specsFolders) {
					if (specFolder.getName().startsWith(".")) continue;
					Triple<Spec,List<Formula>,String> seed = specAndBCFolder(specFolder);
					seeds.add(seed.getLeft(), seed.getMiddle(), seed.getRight());
					/*
					if (specFolder.getName().startsWith(".")) continue;
					File specFile = new File(specFolder.getAbsoluteFile() + File.separator + "spec");
					File bcFolder = new File(specFolder.getAbsoluteFile() + File.separator);
					try {
						Spec spec = read(specFile);
						List<Formula> bcs = bc(spec.getVariables(), bcFolder);
						seeds.add(spec, bcs, specFile.getName());
					} catch (IOException e) {
						e.printStackTrace();
					}
					*/
				}
			}
		} else {
			throw new IOException(folder.getAbsolutePath() + " is not a directory.");
		}
		return seeds;
	}

}