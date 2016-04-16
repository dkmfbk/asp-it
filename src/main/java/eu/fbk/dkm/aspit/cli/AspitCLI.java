package eu.fbk.dkm.aspit.cli;

import it.unical.mat.wrapper.DLVInvocationException;

import java.io.File;
import java.io.IOException;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import eu.fbk.dkm.aspit.kb.KBProgram;
import eu.fbk.dkm.aspit.kb.KnowledgeBase;

/** 
 * @author Loris 
 * @version 1.0
 * 
 * Command line interface for Asp-it (for ELc).
 */
public class AspitCLI {

	//--- FIELDS -----------------------------------------------

	private static final String DEFAULT_DLV_PATH = "./localdlv/dlv";
	//private static final String DEFAULT_OUTPUT_FILENAME = "./output.dlv";
	
    private KnowledgeBase inputKB;
    private KBProgram outputKBProgram;
    	
	private String dlvPath = null;
	private String outputKBFilePath = null;
	private String outputDLPFilePath = null;
	
	private boolean verbose = false;

	private String[] args;

	//--- CONSTRUCTOR ------------------------------------------
	
	public AspitCLI(String[] args) {
		this.args = args;
	}
	
	//--- MAIN THREAD ------------------------------------------

	/**
	 * Main thread of the command line interface.
	 */
	public void go() {
		
		//Create a new knowledge base to manage input ontology
		inputKB = new KnowledgeBase();
				
		//Parse input for ontology file
		printBanner();
		if (!parseArgs(args)) {
			printUsage();
			System.exit(1);
		} 
		
		//Check input file existence
		File ontofile = new File(inputKB.getOntologyFilename());
		if(!ontofile.exists()){
			System.err.println("[!] Input ontology file does not exists.");
			System.exit(1);
		}
		
		//Load ontology file.
		try {
			inputKB.loadOntology();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		if(verbose) System.out.println("Input ontology loaded: " + inputKB.getOntologyFilename());
		
		//Create new program for the input KB.
		outputKBProgram = new KBProgram(inputKB);
		
		//Set possibly custom DLV, output file path and lp program
		if(dlvPath != null) 
			outputKBProgram.setDlvPath(dlvPath);
		else{
			dlvPath = DEFAULT_DLV_PATH;
			outputKBProgram.setDlvPath(dlvPath);
		}

		if(outputKBFilePath != null) 
			inputKB.setOutputFilePath(outputKBFilePath);
		else 
			inputKB.setOutputFilePath(formatFilename());

		if(outputDLPFilePath != null)
			outputKBProgram.setOutputFilePath(outputDLPFilePath);
		
		//Rewrite the program.
		if(verbose) System.out.println("Rewriting program...");
		outputKBProgram.rewrite();
				
		if(verbose)
			System.out.println("Rewriting completed in: " + outputKBProgram.getRewritingTime() + " ms.");

		//Interaction with DLV: computes the IT for the input axioms
		outputKBProgram.computeIT();
		
		if(verbose)
			System.out.println("IT computed in: " + outputKBProgram.getmodelComputationTime() + " ms.");
		
		//Add IT annotations to KB
		inputKB.addITAnnotations();

		if(verbose){
			String warnings = inputKB.getPOIWarnings();
			if(!warnings.isEmpty())
				System.out.print(warnings);
			
			System.out.println("IT annotations added to KB: " + outputKBProgram.getinfotermsNumber());
		}

		//If required, store the program to file.
		if(outputDLPFilePath != null){
			try {
				outputKBProgram.storeToFile();
		    	if(verbose) System.out.println("KB program saved in: " + outputKBProgram.getOutputFilePath());
		    	
	    	} catch (Exception e) {
		    	e.printStackTrace();
	    	}
		}
   
		//Save ontology to file
		try {
			inputKB.saveToN3();
			if(verbose) System.out.println("Output KB saved to: " + inputKB.getOutputFilePath() + "\n");
			
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
		
		System.out.println("Asp-it: process completed.");
	}
	
	/**
	 * Formats the filename for output ontology.
	 *  
	 */
	public String formatFilename(){
		String fn = inputKB.getOntologyFilename();
		
		if(inputKB.getOntologyFile().getName().contains(".")){
		  String ext = fn.substring(fn.lastIndexOf("."));
		  fn = fn.replace(ext, "-out.n3");
		} else {
		  fn = fn.concat("-out.n3");
		}
		
		return fn;
	}
	
	//--- INPUT HANDLING METHODS ------------------------------------
	
	/** 
	 * Parses the input parameters. 
	 * 
	 * @param args input parameter string
	 * */
	public boolean parseArgs(String[] args) {
		
		if (args.length == 0) {			
			System.err.println("[!] Missing argument: <input-ontology-file>");
			System.err.println();			
			return false;
			
		} else {
			inputKB.setOntologyFilename(args[0]);
			if(verbose) System.out.println("Input ontology: " + inputKB.getOntologyFilename());
			
			for (int i = 1; i < args.length; i++) {				
				switch (args[i]) {
				case "-dlv":
					if(i+1 < args.length && !args[i+1].startsWith("-"))
						dlvPath = args[++i];
					else {
						System.err.println("[!] Missing argument: <dlv-path>");						
						return false;
					}						
					break;
				case "-out":
					if(i+1 < args.length && !args[i+1].startsWith("-"))
					   outputKBFilePath = args[++i];
					else {
						System.err.println("[!] Missing argument: <output-file>");						
						return false;						
					}
					break;
				case "-lp":
					if(i+1 < args.length && !args[i+1].startsWith("-"))
					   outputDLPFilePath = args[++i];
					else {
						System.err.println("[!] Missing argument: <program-file>");						
						return false;						
					}											
					break;
				case "-v":
				    verbose = true;
					break;
				default://arguments not supported
					System.err.println("[!] Option not supported: " + args[i]);
					System.err.println();
				    return false; 
				}
			}
			return true;
		}
	}

	/**
	 * Prints usage of the command line and exits.
	 * 
	 */
	void printUsage() {

		String usage = 
				"Usage: asp-it <input-ontology-file> [<options>]\n"
				+ //
				" <input-ontology-file>\n"
				+ //
				"  OWL file containing the ontology for which IT have to be computed.\n\n"
				+ //
				"Example: asp-it input.n3\n\n"
				+ //
				"Options:\n"
				+ //
				" -v: verbose (prints more information about loading and computing process)\n"
				+ //
				" -out <output-file>: specifies the path to the output ontology file (default: <input-ontology-file>-out.n3)\n"
				+ //
				" -lp <program-file>: output the result of rewriting to the specified file (default: false)\n"
				+ //
		        " -dlv <dlv-path>: specifies the path to the DLV executable (default: localdlv/dlv)";

		System.out.println(usage);
	}
	
	/**
	 * Prints initial banner and version.
	 */
	void printBanner(){
		String banner = "=== Asp-it v.1.0 (ELc) ===\n";
		System.out.println(banner);
	}
	
	//--- MAIN ------------------------------------------------------
	/**
	 * @param args
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 * @throws DLVInvocationException
	 */
	public static void main(String[] args) throws OWLOntologyCreationException,
			IOException, DLVInvocationException { 
		
        new AspitCLI(args).go();
		
        //(Test application)
		//String[] argtest = {"./examples/just-atomic.n3", "-v", "-lp", "./output.dlv"};
		//String[] argtest = {"./examples/complex-concepts.n3", "-v", "-lp", "./output.dlv"};
		//String[] argtest = {"./examples/isa-test.n3", "-v", "-lp", "./output.dlv"};
		//String[] argtest = {"./examples/food-and-wines-demo.n3", "-v", "-lp", "./output.dlv"};
						
		//new AspitCLI(argtest).go();
	}
	
}
//=======================================================================