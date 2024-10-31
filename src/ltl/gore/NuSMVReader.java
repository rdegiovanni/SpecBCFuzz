package ltl.gore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.io.FileUtils;

import fuzz.Seeds;
import ltl.owl.FormulaUtils;

public class NuSMVReader implements SpecReader {
	
	@Override
	public Spec read(File specFile) throws IOException {
		List<String> lines;
		try {
			lines = FileUtils.readLines(specFile,"UTF-8");
			Spec spec = null;
			try {
				//NuSMVReaderLoadMemory readerLoadMemory = new NuSMVReaderLoadMemory();
				//long loadMemory = readerLoadMemory.loadMemory(specFile);
				//System.out.println("LoadMemory: " + loadMemory);
				spec = readNuSMVFormat(lines);
			} catch (ParseCancellationException e) {
				e.printStackTrace();
			}
			lines = null;
			return spec;
			//return readNuSMVFormat(lines);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private Spec readNuSMVFormat(List<String> lines) {
		List<String> dom = new ArrayList<String>();
		List<String> goals = new ArrayList<String>();
		List<String> ins = new ArrayList<String>();
		List<String> outs = new ArrayList<String>();
		boolean varBlock = false;
		boolean specBlock = false;
		String goal = "";
		boolean firstSpec = false;
		for (String line : lines) {
			if (line.startsWith("VAR")) {
				varBlock = true;
				specBlock = false;
				continue;
			} else if (line.startsWith("LTLSPEC")) {
				varBlock = false;
				specBlock = true;
			}
			if (varBlock) {
				if (line.contains(":") && line.contains(";")) {
					String[] varAndType = line.split(":");
					final int varIndex = 0;
					String var = varAndType[varIndex];
					var = var.trim();
					ins.add(var);
				}
			} else if (specBlock) {
				String formula;
				if (line.startsWith("--")) {
					continue;
				}
				if (line.startsWith("LTLSPEC !")) {
					formula = line.replace("LTLSPEC !", "");
					if (formula.trim().length() != 0) {
						goal += changeConstants(formula) + " ";
						firstSpec = true;
						//goals.add(changeConstants(formula));
						//break;
					}
				} else if (line.startsWith("LTLSPEC")) {
					formula = line.replace("LTLSPEC", "");
					if (formula.trim().length() != 0) {
						goal += changeConstants("!" + formula) + " ";
						firstSpec = true;
						//goals.add(changeConstants("!" + formula));
						//break;
					}
				} else {
					if (line.startsWith("!")) {
						if (!firstSpec) {
							firstSpec = true;
							goal += changeConstants(line.replaceFirst("!", "")) + " ";
						} else {
							goal += changeConstants(line) + " ";
						}
						//goals.add(changeConstants(line.replaceFirst("!", "")));
						//break;
					} else {
						if (!firstSpec) {
							firstSpec = true;
							goal += changeConstants("!" + line) + " ";
						} else {
							goal += changeConstants(line) + " ";
						}
						//goals.add(changeConstants("!" + line));
						//break;
					}
				}
			}
		}
		goals.add(goal);
		Spec spec = null;
		try {
			spec = new Spec(
					FormulaUtils.toFormulaList(dom, ins, outs),
					FormulaUtils.toFormulaList(goals, ins, outs),
					ins,outs);
		} catch (org.antlr.v4.runtime.misc.ParseCancellationException e) {
			e.printStackTrace();
		}
		return spec;
	}
	
	private String changeConstants(String formula) {
		return formula.replaceAll("TRUE", "true").
				replaceAll("FALSE", "false").
				replaceAll("\\)V\\(", "\\)R\\(").
				replaceAll(" V ", " R ");
	}

	@Override
	public Seeds search(File folder) throws IOException {
		Seeds seeds = new Seeds(this);
		if (folder.isDirectory()) {
			File[] casesFolders = folder.listFiles();
			for (File caseFolder : casesFolders) {
				searchSpecs(caseFolder, seeds);
			}
		} else {
			searchSpecs(folder, seeds);
		}
		return seeds;
	}

	private void searchSpecs(File currentFile, Seeds seeds) {
		if (currentFile.isDirectory()) {
			File[] subDirs = currentFile.listFiles();
			for (File subDir : subDirs) {
				searchSpecs(subDir, seeds);
			}
		} else {
			File caseFile = currentFile;
			if (!caseFile.getName().equals("README") && !caseFile.getName().equals(".DS_Store")) {
				System.out.println("Reading the case: " + caseFile.getAbsolutePath());
				//seeds.add(reader.read(caseFile), null, 
				//		currentFile.getParentFile().getName() + "-" + currentFile.getName());
				seeds.add(caseFile, null, currentFile.getParentFile().getName() + "-" + currentFile.getName());
			}
		}
	}

}
