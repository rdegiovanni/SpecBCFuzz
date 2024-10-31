package sat;

/*
public enum SolverResult {
	SAT("", 0),
	UNSAT("", 0),
	TIMEOUT("", 0),
	ERROR("", 0);
	
	private String toolFormula = "";
	
	private double time = 0;
	
    SolverResult() {}
	
    SolverResult(String toolFormula, int time) { this.toolFormula = toolFormula; this.time = time; }
    
    public void formula(String toolFormula) { this.toolFormula = toolFormula; }
    
    public void time(double time) { this.time = time; }
    
    public String formula() { return toolFormula; }
    
    public double time() { return time; }
        
	public boolean inconclusive () { return this == TIMEOUT || this == ERROR; }
	
}
*/

public class SolverResult {
	/*
	SAT("", 0),
	UNSAT("", 0),
	TIMEOUT("", 0),
	ERROR("", 0);
	*/
	
	private String status = "UNKNOWN";
	
	private String toolFormula = "";
	
	private double time = -1;
	
    public SolverResult() {}
    
	/*
    SolverResult(String toolFormula, int time) { this.toolFormula = toolFormula; this.time = time; }
    */
    
    public void formulaAndTime(String toolFormula, int time) { 
    	this.toolFormula = toolFormula; 
    	this.time = time; 
    }
    
    private void formulaAndTimeAndStatus(String toolFormula, int time, String status) { 
    	this.toolFormula = toolFormula; 
    	this.time = time;
    	this.status = status;
    }
    
	public static SolverResult SAT() {
		SolverResult result = new SolverResult();
		result.status = "SAT";
		return result;
	}
    
	public static SolverResult SAT(String toolFormula, int time) {
		SolverResult result = new SolverResult();
		result.formulaAndTimeAndStatus(toolFormula, time, "SAT");
		return result;
	}
	
	public static SolverResult UNSAT(String toolFormula, int time) {
		SolverResult result = new SolverResult();
		result.formulaAndTimeAndStatus(toolFormula, time, "UNSAT");
		return result;
	}
	
	public static SolverResult UNSAT() {
		SolverResult result = new SolverResult();
		result.status = "UNSAT";
		return result;
	}
	
	public static SolverResult TIMEOUT(String toolFormula, int time) {
		SolverResult result = new SolverResult();
		result.formulaAndTimeAndStatus(toolFormula, time, "TIMEOUT");
		return result;
	}
	
	public static SolverResult TIMEOUT() {
		SolverResult result = new SolverResult();
		result.status = "TIMEOUT";
		return result;
	}
	
	public static SolverResult ERROR(String toolFormula, int time) {
		SolverResult result = new SolverResult();
		result.formulaAndTimeAndStatus(toolFormula, time, "ERROR");
		return result;
	}
	
	public static SolverResult ERROR() {
		SolverResult result = new SolverResult();
		result.status = "ERROR";
		return result;
	}
    
    public void formula(String toolFormula) { this.toolFormula = toolFormula; }
    
    public void time(double time) { this.time = time; }
    
    public String formula() { return toolFormula; }
    
    public double time() { return time; }
        
	public boolean inconclusive () { return this.status == "TIMEOUT" || this.status == "ERROR" || this.status == "UNKOWN"; }
	
	public String status() { return this.status; }
	
	public boolean equals(Object obj) {
		if (obj instanceof SolverResult) {
			SolverResult o = (SolverResult) obj;
			return o.status() == this.status;
		} else {
			return false;
		}
    }
	
	public String toString() {
		return this.status;
	}
	
}