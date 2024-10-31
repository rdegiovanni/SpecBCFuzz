package ltl.gore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LogReader {

	private File log;
	private boolean opened = false;
	private BufferedReader br;
	
	public LogReader(File log) {
		this.log = log;
	}
	
	/*
	/**
	 * 
	 * @return (SATSolver,Formula)
	public Map<String, Formula> nextPerAllSolvers() throws IOException {
		Map<String,Formula> SATSolverToFormula = new HashMap<String,Formula>();
		//fill
		return SATSolverToFormula;
	}
	*/
	
	/**
	 * tool,formula,formula_id,output,time
	 * all,G(!p3|p1),0,SAT,no-time
	 * NUSMV,G(((s) | (!q))),0,SAT,44
	 * NUSMVbmc,G(((s) | (!q))),0,SAT,44
	 * PLTL,G(((s) | (!q))),0,SAT,44
	 * Black-z3,G(((s) | (!q))),0,SAT,44
	 * Black-mathsat,G(((s) | (!q))),0,SAT,44
	 * Black-cmsat,G(((s) | (!q))),0,SAT,44
	 * @return 
	 * @throws IOException
	 */
	public String next() throws IOException {
		if (!this.opened) {
			InputStream inputStream = new FileInputStream(this.log);
			this.br = new BufferedReader(new InputStreamReader(inputStream));
			this.opened = true;
		}
		String line = this.br.readLine();
		while (line != null) {
			if(line.startsWith("all,")) {
				String fields[] = line.split(",");
				final int formulaIndex = 1;
				String strFormula = fields[formulaIndex];
				return strFormula;
				//return LtlParser.parse(strFormula,vars).formula();
			}
			line = this.br.readLine();
		}
		return null;
	}
	
	public void setLog(File log) {
		this.log = log;
	}
	
	public File getLog() {
		return this.log;
	}
	
}
