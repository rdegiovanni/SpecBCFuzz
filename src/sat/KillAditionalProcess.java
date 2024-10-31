package sat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class KillAditionalProcess {
	
	private List<Long> pidToKill() {
		List<Long> lPID = new ArrayList<Long>();
		String run = "ps -x";
		try {
			Process p = Runtime.getRuntime().exec(run);
			InputStream in = p.getInputStream();
	    	InputStreamReader inread = new InputStreamReader(in);
	    	BufferedReader bufferedreader = new BufferedReader(inread);
			String aux = "";
	    	while ((aux = bufferedreader.readLine()) != null) {
	    		//System.out.println(aux);
	    		if (!aux.contains("slurm") && !aux.contains("java")) {
	    			char[] chars = aux.toCharArray();
	    			String PID = "";
	    			boolean firstDigit = false;
	    			for (char c : chars) {
	    				if (!firstDigit && Character.isSpaceChar(c)) {
	    					continue;
	    				} else if (Character.isDigit(c)) {
	    					firstDigit = true;
	    					PID += c;
	    				} else {
	    					break;
	    				}
	    			}
	    			if (PID != "") {
	    				lPID.add(new Long(PID));
	    			}
	    		}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lPID;
	}
	
	public void all() {
		List<Long> lLong = pidToKill();
		for (Long l : lLong) {
			String cmd = "kill -9 " + l;
			try {
				//System.out.println(cmd);
				Process p = Runtime.getRuntime().exec(cmd);
				if (!p.waitFor(1000, TimeUnit.MILLISECONDS)) {
					p.destroy();
					p.destroyForcibly();
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		/*
		String []run = new String[5];
		run[0] = "pkill";
		run[1] = "-f";
		//String run = "pkill -f ";		
		for (String process : lString) {
			try {
				run[2] = process;
				run[3] = "-u";
				run[4] = "lude";
				System.out.println("RUN: " + run.toString());
				Process p = Runtime.getRuntime().exec(run);
				p.wait();
				InputStream in = p.getInputStream();
		    	InputStreamReader inread = new InputStreamReader(in);
		    	BufferedReader bufferedreader = new BufferedReader(inread);
				String aux = "";
		    	while ((aux = bufferedreader.readLine()) != null) {
		    		System.out.println(aux);
				}
		    	InputStream inerr = p.getErrorStream();
		    	InputStreamReader inreaderr = new InputStreamReader(inerr);
		    	BufferedReader bufferedreadererr = new BufferedReader(inread);
				aux = "";
		    	while ((aux = bufferedreadererr.readLine()) != null) {
		    		System.out.println(aux);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		*/
	}
	
}
