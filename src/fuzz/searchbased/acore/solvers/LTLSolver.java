package fuzz.searchbased.acore.solvers;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeUnit;

public class LTLSolver {

	public static enum SolverResult {
		SAT,
		UNSAT,
		TIMEOUT,
		ERROR;
		
		public boolean inconclusive () { return this == TIMEOUT || this == ERROR; }
	}
	 
	public static void clearState() {
		numOfTimeout = 0;
		numOfError = 0;
		numOfReevaluations = 0;
		has_inputstream = false;
		needReevaluation = false;
	}
	
	public static int numOfTimeout = 0;
	public static int numOfError = 0;
	public static int numOfCalls = 0;
//	public static int TIMEOUT = 30;
	private static int numOfReevaluations;
	private static boolean has_inputstream = false;
	private static boolean needReevaluation = false;
	private static File polsatFolder;
	
	public static String SATID = "";
	private static File SATSolverFolder;
	private static int SAT_TIMEOUT = 300;
	
	private static String getCommand(){
		String cmd = "";
		String currentOS = System.getProperty("os.name");
		if (currentOS.startsWith("Mac"))
//			if (useAalta)
				cmd = "../aalta_mac";
//			else
//				cmd = "./lib/pltl graph";
		else
			cmd = "./polsat_wo_trp_k50";
		return cmd;
	}
	
	/**
	 * 	This method copies the polsat folder and other SATs to an unique folder. Thus, avoiding parallel problems with commons tmp files.
 	 */
	public static boolean init() {
		File source = new File("./lib");
		SATID = SATID.replaceAll("/", "_");
		SATID = SATID.replaceAll("\\.", "_");
		File target = new File("lib_tmp/lib_" + SATID);
		
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
				SATSolverFolder = target;
				return true;
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * Removes the unique folder to the polsat.
	 */
	public static boolean clear() {
		try {
			String cmd = "rm -rf " + SATSolverFolder.getAbsolutePath();
			System.out.println("Execute: " + cmd);
			Process deleteCmd = Runtime.getRuntime().exec(cmd, null, null);
			deleteCmd.waitFor();
			return true;
		} catch (IOException | InterruptedException e1) {
			e1.printStackTrace();
		}
		return false;
	}
	
	public static SolverResult isSAT(String formula) throws IOException, InterruptedException {
		return isSAT(formula, 0);
	}
		
	
	private static SolverResult isSAT(String formula, int k) throws IOException, InterruptedException {
		if (k == 0) {
			clearState();
		}
		numOfCalls++;
//		System.out.println(formula);
		Process p = null;
    	
		if (formula != null) {
			String cmd = getCommand();
		//	System.out.println("Execute: " + cmd + " " + formula + " (inside " + SATSolverFolder + ")");
			if (!System.getProperty("os.name").startsWith("Mac"))
				p = Runtime.getRuntime().exec(new String[]{cmd,formula},null, new File(SATSolverFolder.getAbsolutePath() + "/Polsat"));
			else 
				p = Runtime.getRuntime().exec(new String[]{cmd,formula},null, new File(SATSolverFolder.getAbsolutePath() + "/Polsat"));
//	    	OutputStream out = p.getOutputStream();
//	    	OutputStreamWriter bufout = new OutputStreamWriter(out);
//	    	BufferedWriter bufferedwriter = new BufferedWriter(bufout, formula.getBytes().length);
//    		bufferedwriter.write(formula);
//	    	bufferedwriter.close();
//	    	bufout.close();
//	    	out.close();
    	}

		boolean timeout = false;
		if(!p.waitFor(SAT_TIMEOUT, TimeUnit.SECONDS)) {
		    timeout = true; //kill the process. 
			p.destroy(); // consider using destroyForcibly instead
		}
		
		SolverResult sat = SolverResult.ERROR;
		String aux;
		if (timeout){
			numOfTimeout++;
			sat = SolverResult.TIMEOUT;
			System.out.println("TIMEOUT");
			System.out.println(formula);
			p.destroy();
		}
		else {
			InputStream in = p.getInputStream();
	    	InputStreamReader inread = new InputStreamReader(in);
	    	BufferedReader bufferedreader = new BufferedReader(inread);
			sat = SolverResult.UNSAT;
			
			//System.out.println(formula);
		    while ((aux = bufferedreader.readLine()) != null) {
		    	//System.out.println(aux);
		    	if (aux.contains("pltl") && aux.contains("ignored")) { //pltl was removed
		    		continue;
		    	}
		    	if (aux.contains("cannot find")) {
		    		System.out.println("Error calling solvers");
		    		sat =  SolverResult.ERROR;
		    		break;
		    	}
		    	if (aux.contains("Cannot recognize the output")) {
		    		//System.out.println(formula);
		    		sat = SolverResult.ERROR;
		    		needReevaluation = true;
		    		break;
		    	}
		    	else {
			    	has_inputstream = true;
			    	if ((aux.equals("sat")) || (aux.contains("sat") && !aux.contains("unsat") )){
			    		sat = SolverResult.SAT;
			    		break;
			    	}
		    	}
		    }
		    
		 // Leer el error del programa.
	    	InputStream err = p.getErrorStream();
	    	InputStreamReader errread = new InputStreamReader(err);
	    	BufferedReader errbufferedreader = new BufferedReader(errread);
		    while ((aux = errbufferedreader.readLine()) != null && !has_inputstream && !needReevaluation) {
		    	System.out.println("errstream: "+ aux);

		    	// Model checker uses error output to send warning message when formulae is a contradiction.
		    	if (aux.isEmpty()) continue;
		    	if (aux.contains("WARNING")) {
		    		System.out.println(aux);
		    		//sat = SolverResult.UNSAT;
		    		sat = SolverResult.ERROR;
		    		break;
		    	}
		    	else {
		    		System.out.println("ERR: " + aux);
					System.out.println(formula);
		    		sat = SolverResult.ERROR;
		    		break;
		    	}
		    }
		    
		    // Check for failure
			if (p.waitFor() != 0) {
				System.out.println("exit value = " + p.exitValue());
				System.out.println(formula);
				numOfError++;
				System.out.println("has inputstream? " + has_inputstream);
				System.out.println("Need reevaluation? " + needReevaluation);
			}
			
			// Close the InputStream
	    	bufferedreader.close();
	    	inread.close();
	    	in.close();
	  
	   		// Close the ErrorStream
	   		errbufferedreader.close();
	   		errread.close();
	   		err.close();
	   
		}
    		
   		if (p!=null) {
  			OutputStream os = p.getOutputStream();
			if (os!=null) os.close();
   		}
   		if (has_inputstream) {
   			numOfReevaluations = 0;
   			return sat;
   		}
   		else if (needReevaluation && numOfReevaluations < 10) {
   			numOfReevaluations++;
   			return isSAT(formula, k + 1);
   		}
   		else if (needReevaluation && numOfReevaluations>=10) {
   			numOfReevaluations = 0;
   			System.out.println("ERR: The number of allowed reevaluations has been overcome");
   			return SolverResult.ERROR;
   		}
   		else {
   			numOfReevaluations = 0;
   			System.out.println("ERR: Unknown");
   			return SolverResult.ERROR;
   		}
	}
}
