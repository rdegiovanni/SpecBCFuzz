package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import experiment.ExperimentRunner;
import experiment.ExperimentSetting;
import experiment.runner.BenchmarksExperiment;
import experiment.runner.GrammarFuzzerExperiment;
import experiment.runner.MutationFuzzerExperiment;
import experiment.runner.ReRunExperiment;
import experiment.runner.SemSynSearchBasedExperiment;
import ltl.gore.GOREReader;
import ltl.gore.LogReader;
import ltl.gore.NuSMVReader;
import sat.SATSolver;
import sat.tool.Aalta;
import sat.tool.Black;
import sat.tool.BlackSingularity;
import sat.tool.NuSMV;
import sat.tool.NuSMVbmc;
import sat.tool.PLTL;
import sat.tool.PLTL.PLTL_VERSION;
import sat.tool.PLTLBDD;
import sat.tool.Spin;
import sat.tool.kAllLTLBinary;
import sat.tool.kAllLTL.KAllLTL;

public class Main {
	
	public static File target = null;
	
	public static File copyLib(File source, int id) {
		System.out.println("Source: " + source);
		System.out.println("id: " + id);
		//System.out.println(source.getAbsoluteFile().getParent());
		target = new File(source.getAbsoluteFile().getParent() + File.separator + "lib_tmp" + File.separator + "lib_"  + id);
		try {
			String cmd = "rm -rf " + target.getAbsolutePath();
			Process deleteCmd = Runtime.getRuntime().exec(cmd, null, null);
			deleteCmd.waitFor();
		} catch (IOException | InterruptedException e1) {
			e1.printStackTrace();
		}
		
		target.getParentFile().mkdir();
		boolean exists = target.mkdir();
		
		if (exists) {
			try {
				String cmd = "cp -R ." + " " + target.getAbsolutePath();
				System.out.println("Execute: " + cmd);
				Process p = Runtime.getRuntime().exec(cmd, null, source);
				p.waitFor();
				return target;
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static void deleteLib() {
		deleteLib(target);
	}
	
	public static void deleteLib(File rootFile) {
		File[] subFiles = rootFile.listFiles();
		for (File file : subFiles) {
			if (file.isDirectory()) {
				deleteLib(file);
			} else {
				file.delete();
			}
		}
		rootFile.delete();
	}
	
	public static void main(String[] args) {
		File log = null;
		File lib = null;
		int run = 0;
		int jobId = 999999;
		int satTimeout = 1000;
		String experiment = "";
		File specs = null;
		File reRun = null;
		String os = "linux";
		int popSize = 50;
		int maxNumOfInd = 200;
		boolean enableThreads = false;
		int maxLiteral = -1;
		int maxNonTerminals = -1;
		double terminalChoiceProbability = -1;
		double booleanConstantProbability = -1;
		boolean kAllLTL = false;
		int K = -1;
		File kLib = null;
		for (int i = 0; i < args.length; ++i) {                        
			String currentArg = args[i];
			if (currentArg.startsWith("-log=")) {
				currentArg = currentArg.replace("-log=","");
				log = new File(currentArg);
			} else if (currentArg.startsWith("-lib=")) {
				currentArg = currentArg.replace("-lib=","");
				lib = new File(currentArg);
			} else if (currentArg.startsWith("-run=")) {
				currentArg = currentArg.replace("-run=","");
				run = Integer.parseInt(currentArg);
			} else if (currentArg.startsWith("-id=")) {
				currentArg = currentArg.replace("-id=","");
				jobId = Integer.parseInt(currentArg);
			} else if (currentArg.startsWith("-sattimeout=")) {
				currentArg = currentArg.replace("-sattimeout=","");
				satTimeout = Integer.parseInt(currentArg);
			} else if (currentArg.startsWith("-experiment=")) {
				currentArg = currentArg.replace("-experiment=","");
				experiment = currentArg;
			} else if (currentArg.startsWith("-maxLiteral=")) {
				currentArg = currentArg.replace("-maxLiteral=","");
				maxLiteral = Integer.parseInt(currentArg);
			} else if (currentArg.startsWith("-maxNonTerminals=")) {
				currentArg = currentArg.replace("-maxNonTerminals=","");
				maxNonTerminals = Integer.parseInt(currentArg);
			} else if (currentArg.startsWith("-terminalChoiceProbability=")) {
				currentArg = currentArg.replace("-terminalChoiceProbability=","");
				terminalChoiceProbability = Double.parseDouble(currentArg);
			} else if (currentArg.startsWith("-booleanConstantProbability=")) {
				currentArg = currentArg.replace("-booleanConstantProbability=","");
				booleanConstantProbability = Double.parseDouble(currentArg);
			} else if (currentArg.startsWith("-popSize=")) {
				currentArg = currentArg.replace("-popSize=","");
				popSize = Integer.parseInt(currentArg);
			} else if (currentArg.startsWith("-maxNumOfInd=")) {
				currentArg = currentArg.replace("-maxNumOfInd=","");
				maxNumOfInd = Integer.parseInt(currentArg);
			} else if (currentArg.startsWith("-specs=")) {
				currentArg = currentArg.replace("-specs=","");
				specs = new File(currentArg);
			} else if (currentArg.startsWith("-reRunLog=")) {
				currentArg = currentArg.replace("-reRunLog=","");
				reRun = new File(currentArg);
			} else if (currentArg.startsWith("-os=")) {
				currentArg = currentArg.replace("-os=","");
				os = currentArg;
			} else if (currentArg.startsWith("-enableThreads")) {
				currentArg = currentArg.replace("-enableThreads", "");
				enableThreads = true;
			} else if (currentArg.startsWith("-KAllLTL")) {
				currentArg = currentArg.replace("-KAllLTL", "");
				kAllLTL = true;
			} else if (currentArg.startsWith("-K=")) {
				currentArg = currentArg.replace("-K=", "");
				K = Integer.parseInt(currentArg);
			} else if (currentArg.startsWith("-Klib=")) {
				currentArg = currentArg.replace("-Klib=", "");
				kLib = new File(currentArg);
			}
		}
		ExperimentRunner exp = null;
		if (experiment.equals("grammar")) {
			if (maxLiteral >= 0 || maxNonTerminals >= 0 || terminalChoiceProbability >= 0 || booleanConstantProbability >= 0) {
				exp = new GrammarFuzzerExperiment(maxLiteral, maxNonTerminals, terminalChoiceProbability, 
						booleanConstantProbability, enableThreads);
			} else {
				exp = new GrammarFuzzerExperiment();
			}
		} else if (experiment.equals("benchmarks")) {
			exp = new BenchmarksExperiment(specs, new NuSMVReader(), enableThreads);
		} else if (experiment.equals("mutation-format:benchmarks")) {
			exp = new MutationFuzzerExperiment(specs, new NuSMVReader());
		} else if (experiment.equals("mutation-format:gore")) {
			exp = new MutationFuzzerExperiment(specs, new GOREReader());
		} else if (experiment.equals("searchbased:SemSyn-format:benchmarks")) {
			exp = new SemSynSearchBasedExperiment(specs, new NuSMVReader(), popSize, maxNumOfInd, enableThreads);
		} else if (experiment.equals("searchbased:SemSyn-format:gore")) {
			exp = new SemSynSearchBasedExperiment(specs, new GOREReader(), popSize, maxNumOfInd, enableThreads);
		} else if (experiment.equals("reRun")) {
			exp = new ReRunExperiment(new LogReader(reRun), enableThreads);
		} else {
			System.err.println("Please, select the experiment type (e.g., -experiment=grammar)");
			return;
		}
		
		//MOVE-LIB
		lib = copyLib(lib, jobId);
		if (lib == null) {
			return;
		}
		
		//CONSTANTS - GRAMMAR
		int MAX_LITERAL = ExperimentSetting.MAX_LITERAL;
		if (maxLiteral > 0) {
			MAX_LITERAL = maxLiteral;
		}
		//final int MAX_LITERAL = 3;
		//final double TERMINAL_PROBABILITY = 0.4;
		//final double BOOLEAN_CONSTANT_PROBABILITY = 0.01;
		//final int MAX_NONTERMINAL = 30;

		File aaltaBin_v2 = null;
		File aaltaBin_v1 = null;
		
		File nuSMVBin_v260 = null;
		File nuSMVbmcBin_v260 = null;
		
		File nuSMVBin_v254 = null;
		File nuSMVbmcBin_v254 = null;
		
		File nuSMVBin_v243 = null;
		File nuSMVbmcBin_v243 = null;
		
		File nuSMVBin_v225 = null;
		File nuSMVbmcBin_v225 = null;
		
		File nuSMVBin_v203 = null;
		File nuSMVbmcBin_v203 = null;
		
		File nuSMVBin_v11 = null;
		File nuSMVbmcBin_v11 = null;
		
		File PLTLBin_tableau = null;
		File PLTLBin_bdd = null;
		File PLTLBin_multipass = null;

		File blackBin_v074;
		File blackMathsatBin_v074;
		File blackCryptominisatBin_v074;
		File blackTmpRunner_v074;
		File blackMathsatTmpRunner_v074;
		File blackCryptominisatTmpRunner_v074;

		File blackSifSingularity_v092_z3;
		File blackSifSingularity_v092_mathsat;
		File blackSifSingularity_v092_cmsat;
		File blackSifSingularity_v092_cvc5;

		File blackBinInContainer_v092;
		File blackZ3TmpRunner_v092;
		File blackMathsatTmpRunner_v092;
		File blackCryptominisatTmpRunner_v092;
		File blackCVC5TmpRunner_v092;

		File blackSifSingularity_v052_z3;
		File blackSifSingularity_v052_mathsat;
		File blackSifSingularity_v052_cmsat;
		File blackBinInContainer_v052;
		File blackZ3TmpRunner_v052;
		File blackMathsatTmpRunner_v052;
		File blackCryptominisatTmpRunner_v052;

		File ltl2ba_v13;
		
		SATSolver aalta = null;
		SATSolver aalta_v1 = null;
					
		SATSolver nuSMV_v260 = null;
		SATSolver nuSMVbmc_v260 = null;
					
		SATSolver nuSMV_v254 = null;
		SATSolver nuSMVbmc_v254 = null;
					
		SATSolver nuSMV_v243 = null;
		SATSolver nuSMVbmc_v243 = null;
					
		SATSolver nuSMV_v225 = null;
		SATSolver nuSMVbmc_v225 = null;
					
		SATSolver nuSMV_v203 = null;
		SATSolver nuSMVbmc_v203 = null;
					
		SATSolver nuSMV_v11 = null;
		SATSolver nuSMVbmc_v11 = null;
					
		SATSolver PLTL_tableau = null;
		SATSolver PLTL_BDD = null;
		SATSolver PLTL_multipass = null;

		SATSolver blackZ3_v092 = null;
		SATSolver blackMathSAT_v092 = null;
		SATSolver blackCryptoMiniSAT_v092 = null;
		SATSolver blackCVC5_v092 = null;
					
		SATSolver blackZ3_v074 = null;
		SATSolver blackMathSAT_v074 = null;
		SATSolver blackCryptoMiniSAT_v074 = null;
					
		SATSolver blackZ3_v052 = null;
		SATSolver blackMathSAT_v052 = null;
		SATSolver blackCryptoMiniSAT_v052 = null;
				
		if (os.equals("mac")) {
			//CONSTANT - SOLVERS
			aaltaBin_v2 = new File(lib.getAbsoluteFile() + File.separator + "aalta" + File.separator + "aalta_mac");
			nuSMVBin_v260 = new File(lib.getAbsoluteFile() + File.separator + "NuSMV_mac" + File.separator + "bin" + File.separator + "NuSMV_2_6_0");
			nuSMVbmcBin_v260 = new File(lib.getAbsoluteFile() + File.separator + "NuSMV_mac_bmc" + File.separator + "bin" + File.separator + "NuSMV_2_6_0");
			//ltl2ba_v13 = new File("/Users/luiz.carvalho/Downloads/ltl2ba-1.3/ltl2ba");
			//PLTLBin_tableau = new File(lib.getAbsoluteFile() + File.separator + "pltl" + File.separator + "pltl_mac");
			aalta = new Aalta(aaltaBin_v2, MAX_LITERAL, "v2");
			nuSMV_v260 = new NuSMV(nuSMVBin_v260, MAX_LITERAL, "v260");
			nuSMVbmc_v260 = new NuSMVbmc(nuSMVbmcBin_v260, MAX_LITERAL, "v260");
			//ltl2ba13 = new Spin(ltl2ba_v13, MAX_LITERAL, "v1.3", new File("/Users/luiz.carvalho/Downloads/ltl2ba-1.3/input"));
			//PLTL_tableau = new PLTL(PLTLBin_tableau, MAX_LITERAL, PLTL_VERSION.tableau_graph);
		} else if (os.equals("linux")) {
			/*
			 * Aalta
			 */
			//aaltaBin_v2 = new File(lib.getAbsoluteFile() + File.separator + "aalta" + File.separator + "aalta_linux_new");
			aaltaBin_v2 = new File(lib.getAbsoluteFile() + File.separator + "aalta" + File.separator + "aalta_linux");
			aaltaBin_v1 = new File(lib.getAbsoluteFile() + File.separator + "aalta_v1" + File.separator + "aalta");
			
			/*
			 * NuSMV
			 */
			nuSMVBin_v260 = new File(lib.getAbsoluteFile() + File.separator + "NuSMV-2.6.0-Linux" + File.separator + "bin" + File.separator + "NuSMV");
			nuSMVbmcBin_v260 = new File(lib.getAbsoluteFile() + File.separator + "NuSMV-2.6.0-Linux_bmc" + File.separator + "bin" + File.separator + "NuSMV");
			
			nuSMVBin_v254 = new File(lib.getAbsoluteFile() + File.separator + "NuSMV-2.5.4-x86_64-unknown-linux-gnu" + File.separator + "bin" + File.separator + "NuSMV");
			nuSMVbmcBin_v254 = new File(lib.getAbsoluteFile() + File.separator + "NuSMV-2.5.4-x86_64-unknown-linux-gnu_bmc" + File.separator + "bin" + File.separator + "NuSMV");
			
			nuSMVBin_v243 = new File(lib.getAbsoluteFile() + File.separator + "NuSMV-2.4.3-x86_64-linux-gnu" + File.separator + "bin" + File.separator + "NuSMV");
			nuSMVbmcBin_v243 = new File(lib.getAbsoluteFile() + File.separator + "NuSMV-2.4.3-x86_64-linux-gnu_bmc" + File.separator + "bin" + File.separator + "NuSMV");
			
			nuSMVBin_v225 = new File(lib.getAbsoluteFile() + File.separator + "NuSMV-2.2.5-i686-pc-linux-gnu" + File.separator + "bin" + File.separator + "NuSMV");
			nuSMVbmcBin_v225 = new File(lib.getAbsoluteFile() + File.separator + "NuSMV-2.2.5-i686-pc-linux-gnu_bmc" + File.separator + "bin" + File.separator + "NuSMV");
			
			nuSMVBin_v203 = new File(lib.getAbsoluteFile() + File.separator + "nusmv-2.0.3" + File.separator + "bin" + File.separator + "NuSMV");
			nuSMVbmcBin_v203 = new File(lib.getAbsoluteFile() + File.separator + "nusmv-2.0.3_bmc" + File.separator + "bin" + File.separator + "NuSMV");

			nuSMVBin_v11 = new File(lib.getAbsoluteFile() + File.separator + "NuSMV-1.1_Linux-i686" + File.separator + "bin" + File.separator + "NuSMV");
			nuSMVbmcBin_v11 = new File(lib.getAbsoluteFile() + File.separator + "NuSMV-1.1_Linux-i686_bmc" + File.separator + "bin" + File.separator + "NuSMV");
			
			/*
			 * PLTL
			 */
			PLTLBin_tableau = new File(lib.getAbsoluteFile() + File.separator + "pltl_tableau" + File.separator + "pltl");
			PLTLBin_bdd = new File(lib.getAbsoluteFile() + File.separator + "pltl_bdd" + File.separator + "pltl");
			PLTLBin_multipass = new File(lib.getAbsoluteFile() + File.separator + "pltl_multipass" + File.separator + "pltl");
			
			//File PLTLBin = new File(lib.getAbsoluteFile() + File.separator + "pltl" + File.separator + "pltl_mac");
			//File aaltaBin = new File(lib.getAbsoluteFile() + File.separator + "aalta" + File.separator + "aalta_linux_new");
			//File nuSMVBin = new File(lib.getAbsoluteFile() + File.separator + "NuSMV-2.6.0-Linux" + File.separator + "bin" + File.separator + "NuSMV");
			//File PLTLBin = new File(lib.getAbsoluteFile() + File.separator + "pltl" + File.separator + "pltl");
			blackBin_v074 = new File(lib.getAbsoluteFile() + File.separator + "black_linux" + File.separator + "black");
			blackMathsatBin_v074 = new File(lib.getAbsoluteFile() + File.separator + "black_linux" + File.separator + "black");
			blackCryptominisatBin_v074 = new File(lib.getAbsoluteFile() + File.separator + "black_linux" + File.separator + "black");
			//File blackBin = new File(lib.getAbsoluteFile() + File.separator + "black_linux" + File.separator);
			blackTmpRunner_v074 = new File(lib.getAbsoluteFile() + File.separator + "black_linux" + File.separator + "black.sh");
			blackMathsatTmpRunner_v074 = new File(lib.getAbsoluteFile() + File.separator + "black_linux" + File.separator + "black_mathsat.sh");
			blackCryptominisatTmpRunner_v074 = new File(lib.getAbsoluteFile() + File.separator + "black_linux" + File.separator + "black_cryptominisat.sh");

			blackSifSingularity_v092_z3 = new File(lib.getAbsoluteFile() + File.separator + "black_v92" + File.separator + "black_v92.sif");
			blackSifSingularity_v092_mathsat = new File(lib.getAbsoluteFile() + File.separator + "black_v92_mathsat" + File.separator + "black_v92.sif");
			blackSifSingularity_v092_cmsat = new File(lib.getAbsoluteFile() + File.separator + "black_v92_cmsat" + File.separator + "black_v92.sif");
			blackSifSingularity_v092_cvc5 = new File(lib.getAbsoluteFile() + File.separator + "black_v92_cvc5" + File.separator + "black_v92.sif");
			
			blackBinInContainer_v092 = new File("/home/black/build/black");
			blackZ3TmpRunner_v092 = new File(lib.getAbsoluteFile() + File.separator + "black_v92" + File.separator + "black_z3.sh");
			blackMathsatTmpRunner_v092 = new File(lib.getAbsoluteFile() + File.separator + "black_v92" + File.separator + "black_mathsat.sh");
			blackCryptominisatTmpRunner_v092 = new File(lib.getAbsoluteFile() + File.separator + "black_v92" + File.separator + "black_cryptominisat.sh");
			blackCVC5TmpRunner_v092 = new File(lib.getAbsoluteFile() + File.separator + "black_v92" + File.separator + "black_cvc5.sh");

			blackSifSingularity_v052_z3 = new File(lib.getAbsoluteFile() + File.separator + "black_v52" + File.separator + "black_v52.sif");
			blackSifSingularity_v052_mathsat = new File(lib.getAbsoluteFile() + File.separator + "black_v52" + File.separator + "black_v52.sif");
			blackSifSingularity_v052_cmsat = new File(lib.getAbsoluteFile() + File.separator + "black_v52" + File.separator + "black_v52.sif");
			blackBinInContainer_v052 = new File("/home/black/build/black");
			blackZ3TmpRunner_v052 = new File(lib.getAbsoluteFile() + File.separator + "black_v52" + File.separator + "black_z3.sh");
			blackMathsatTmpRunner_v052 = new File(lib.getAbsoluteFile() + File.separator + "black_v52" + File.separator + "black_mathsat.sh");		
			blackCryptominisatTmpRunner_v052 = new File(lib.getAbsoluteFile() + File.separator + "black_v52" + File.separator + "black_cryptominisat.sh");

			//CONSTANTS - FUZZER
			//LTLGrammar grammar = new LTLGrammar(new Random(), BOOLEAN_CONSTANT_PROBABILITY, MAX_LITERAL);
			//Fuzzer fuzzer = new GrammarFuzzer(grammar, new Random(), TERMINAL_PROBABILITY, MAX_NONTERMINAL);

			//SOLVERS
			aalta = new Aalta(aaltaBin_v2, MAX_LITERAL, "v2");
			aalta_v1 = new Aalta(aaltaBin_v1, MAX_LITERAL, "v1");
			
			nuSMV_v260 = new NuSMV(nuSMVBin_v260, MAX_LITERAL, "v260");
			nuSMVbmc_v260 = new NuSMVbmc(nuSMVbmcBin_v260, MAX_LITERAL, "v260");
			
			nuSMV_v254 = new NuSMV(nuSMVBin_v254, MAX_LITERAL, "v254");
			nuSMVbmc_v254 = new NuSMVbmc(nuSMVbmcBin_v254, MAX_LITERAL, "v254");
			
			nuSMV_v243 = new NuSMV(nuSMVBin_v243, MAX_LITERAL, "v243");
			nuSMVbmc_v243 = new NuSMVbmc(nuSMVbmcBin_v243, MAX_LITERAL, "v243");
			
			nuSMV_v225 = new NuSMV(nuSMVBin_v225, MAX_LITERAL, "v225");
			nuSMVbmc_v225 = new NuSMVbmc(nuSMVbmcBin_v225, MAX_LITERAL, "v225");
			
			nuSMV_v203 = new NuSMV(nuSMVBin_v203, MAX_LITERAL, "v203");
			nuSMVbmc_v203 = new NuSMVbmc(nuSMVbmcBin_v203, MAX_LITERAL, "v203");
			
			nuSMV_v11 = new NuSMV(nuSMVBin_v11, MAX_LITERAL, "v11");
			nuSMVbmc_v11 = new NuSMVbmc(nuSMVbmcBin_v11, MAX_LITERAL, "v11");
			
			PLTL_tableau = new PLTL(PLTLBin_tableau, MAX_LITERAL, PLTL_VERSION.tableau_graph);
			PLTL_BDD = new PLTLBDD(PLTLBin_bdd, MAX_LITERAL);
			PLTL_multipass = new PLTL(PLTLBin_multipass, MAX_LITERAL, PLTL_VERSION.multipass);

			/*
			File blackSifSingularity_v092 = new File(lib.getAbsoluteFile() + File.separator + "black_v92" + File.separator + "black.sif");
			File blackBinInContainer_v092 = new File("/home/black/build/black");
			
			File blackSifSingularity_v052 = new File(lib.getAbsoluteFile() + File.separator + "black_v52" + File.separator + "black.sif");
			File blackBinInContainer_v052 = new File("/home/black/build/black");
			 */
			
			/*
			 * Black
			 */
			blackZ3_v092 = new BlackSingularity(blackBinInContainer_v092, blackSifSingularity_v092_z3, MAX_LITERAL, Black.SatSolver.z3, "v092", blackZ3TmpRunner_v092);
			blackMathSAT_v092 = new BlackSingularity(blackBinInContainer_v092, blackSifSingularity_v092_mathsat, MAX_LITERAL, Black.SatSolver.mathsat, "v092", blackMathsatTmpRunner_v092);
			blackCryptoMiniSAT_v092 = new BlackSingularity(blackBinInContainer_v092, blackSifSingularity_v092_cmsat, MAX_LITERAL, Black.SatSolver.cmsat, "v092", blackCryptominisatTmpRunner_v092);
			blackCVC5_v092 = new BlackSingularity(blackBinInContainer_v092, blackSifSingularity_v092_cvc5, MAX_LITERAL, Black.SatSolver.cvc5, "v092", blackCVC5TmpRunner_v092);
			
			blackZ3_v074 = new Black(blackBin_v074, MAX_LITERAL, Black.SatSolver.z3, "v074", blackTmpRunner_v074);
			blackMathSAT_v074 = new Black(blackMathsatBin_v074, MAX_LITERAL, Black.SatSolver.mathsat, "v074", blackMathsatTmpRunner_v074);
			blackCryptoMiniSAT_v074 = new Black(blackCryptominisatBin_v074, MAX_LITERAL, Black.SatSolver.cmsat, "v074", blackCryptominisatTmpRunner_v074);
			
			blackZ3_v052 = new BlackSingularity(blackBinInContainer_v052, blackSifSingularity_v052_z3, MAX_LITERAL, Black.SatSolver.z3, "v052", blackZ3TmpRunner_v052);
			blackMathSAT_v052 = new BlackSingularity(blackBinInContainer_v052, blackSifSingularity_v052_mathsat, MAX_LITERAL, Black.SatSolver.mathsat, "v052", blackMathsatTmpRunner_v052);
			blackCryptoMiniSAT_v052 = new BlackSingularity(blackBinInContainer_v052, blackSifSingularity_v052_cmsat, MAX_LITERAL, Black.SatSolver.cmsat, "v052", blackCryptominisatTmpRunner_v052);
		}

		//SATSolver blackMiniSAT = new Black(blackBin, MAX_LITERAL, Black.SatSolver.minisat, blackTmpRunner);
		//SATSolver blackcvc5 = new Black(blackBin, MAX_LITERAL, Black.SatSolver.cvc5, blackTmpRunner);
		List<SATSolver> solvers = new ArrayList<SATSolver>();

		/*
		 * ltl2ba - version 1.3
		 */
		//solvers.add(ltl2ba13);
		
		if (os.equals("linux")) {
			/*
			 * Aalta
			 */
			solvers.add(aalta);
			
			/*
			 * NuSMV - libreadline.so.8
			 */
			solvers.add(nuSMV_v260);
			solvers.add(nuSMVbmc_v260);
			
			/*
			 * PLTL
			 */
			solvers.add(PLTL_tableau);
			
			/*
			 * Black
			 */
			solvers.add(blackZ3_v074);
			solvers.add(blackMathSAT_v074);
			solvers.add(blackCryptoMiniSAT_v074);	
			
			solvers.add(blackZ3_v092);
			solvers.add(blackMathSAT_v092);
			solvers.add(blackCryptoMiniSAT_v092);
			solvers.add(blackCVC5_v092);
			//solvers.add(blackMiniSAT_v092);
			
			solvers.add(blackZ3_v052);
			solvers.add(blackMathSAT_v052);
			solvers.add(blackCryptoMiniSAT_v052);
			//solvers.add(blackCVC5_v092);
			//solvers.add(blackMiniSAT_v092);
			
			/*
			 * Aalta
			 */
			solvers.add(aalta_v1);
			
			/*
			 * NuSMV 
			 */
			//libreadline.so.6
			solvers.add(nuSMV_v254);
			solvers.add(nuSMVbmc_v254);
			
			//libreadline.so.4
			solvers.add(nuSMV_v243);
			solvers.add(nuSMVbmc_v243);
			
			/*
			 * PLTL
			 */
			solvers.add(PLTL_BDD);
			solvers.add(PLTL_multipass);
			
			//libreadline.so.4
			//solvers.add(nuSMV_v225);
			//solvers.add(nuSMVbmc_v225);
			
			//libreadline.so.3
			//solvers.add(nuSMV_v203);
			//solvers.add(nuSMVbmc_v203);
			
			//libreadline.so.3
			//solvers.add(nuSMV_v11);
			//solvers.add(nuSMVbmc_v11);
		}
		//solvers.add(blackMiniSAT);
		//solvers.add(blackcvc5);
		
		System.out.println(solvers);
		exp.run(solvers, run, satTimeout, log);
		deleteLib();
	}
	
}