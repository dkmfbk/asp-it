package eu.fbk.dkm.aspit.cli;

import it.unical.mat.wrapper.DLVInvocationException;

import java.io.IOException;

import org.semanticweb.drew.dlprogram.parser.ParseException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import eu.fbk.dkm.aspit.kb.KBProgram;
import eu.fbk.dkm.aspit.kb.KnowledgeBase;

/** 
 * @author Loris 
 * @version 0.1
 * 
 * Command line interface for Asp-it (for ELc).
 */
public class AspitCLI {

	//--- FIELDS -----------------------------------------------
	
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
		
		//Load ontology file.
		try {
			inputKB.loadOntology();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if(verbose) System.out.println("Input ontology loaded: " + inputKB.getOntologyFilename());
		
		//Create new program for the input KB.
		outputKBProgram = new KBProgram(inputKB);
		
		//Set possibly custom DLV, output file path and lp program
		if(dlvPath != null) 
			outputKBProgram.setDlvPath(dlvPath);
		if(outputKBFilePath != null) 
			inputKB.setOutputFilePath(outputKBFilePath);
		else 
			inputKB.setOutputFilePath(formatFilename());
		if(outputDLPFilePath != null)
			outputKBProgram.setOutputFilePath(outputDLPFilePath);
		
		//Rewrite the program.
		if(verbose) System.out.println("Rewriting program...");
		outputKBProgram.rewrite();
				
		if(verbose){
			System.out.println("Rewriting completed in " + outputKBProgram.getRewritingTime() + " ms.");
			System.out.println("DLV computation time: " + outputKBProgram.getmodelComputationTime() + " ms.");
		}

		//Add IT annotations to KB
		inputKB.addITAnnotations();
		if(verbose) System.out.println("IT annotations added to KB.");
		
		//Store the program to file.
		if(outputDLPFilePath != null){
			try {
				outputKBProgram.storeToFile();
	    	} catch (Exception e) {
		    	e.printStackTrace();
	    	}
	    	if(verbose) System.out.println("KB program saved in: " + outputKBProgram.getOutputFilePath());
		}
		
		//Save ontology to file
		try {
			inputKB.saveToN3();
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
		if(verbose) System.out.println("Output KB saved to: " + inputKB.getOutputFilePath() + "\n");
		
		System.out.println("Asp-it: process complete.");
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
			System.err.println("Missing argument: <input-ontology-file>");
			System.err.println();			
			return false;
		} else {
			inputKB.setOntologyFilename(args[0]);
			if(verbose) System.out.println("Input ontology: " + inputKB.getOntologyFilename());
			
			for (int i = 1; i < args.length; i++) {				
				switch (args[i]) {
				case "-dlv":
					if(i+1 < args.length)
					   dlvPath = args[++i];
					break;
				case "-out":
					if(i+1 < args.length)
					   outputKBFilePath = args[++i];
					break;
				case "-lp":
					if(i+1 < args.length)
					   outputDLPFilePath = args[++i];
					break;
				case "-v":
				    verbose = true;
					break;
				default:
				    return false; //more arguments than expected 
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
				"  OWL file containing the ontology for which IT have to be computed. \n"
				+ //
				"Example: aspit input.n3\n"
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
		String banner = "=== Asp-it v.0.1 (ELc) ===\n";
		System.out.println(banner);
	}
	
	//--- MAIN ------------------------------------------------------
	/**
	 * @param args
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 * @throws ParseException
	 * @throws DLVInvocationException
	 */
	public static void main(String[] args) throws OWLOntologyCreationException,
			IOException, ParseException, DLVInvocationException {
		
        new AspitCLI(args).go();
		
        //(Test application)
		//String[] argtest = {"./examples/just-atomic.n3", "-v", "-lp", "./output.dlv"};
						
		//new AspitCLI(argtest).go();
	}
	
}
//=======================================================================