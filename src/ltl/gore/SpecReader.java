package ltl.gore;

import java.io.File;
import java.io.IOException;

import fuzz.Seeds;

public interface SpecReader {

	public Spec read(File specFile) throws IOException;
	
	public Seeds search(File folder) throws IOException;
	
}
