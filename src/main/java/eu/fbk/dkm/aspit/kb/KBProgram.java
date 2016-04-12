/**
 * 
 */
package eu.fbk.dkm.aspit.kb;

import it.unical.mat.dlv.program.Literal;
import it.unical.mat.dlv.program.Term;
import it.unical.mat.wrapper.DLVError;
import it.unical.mat.wrapper.DLVInputProgram;
import it.unical.mat.wrapper.DLVInputProgramImpl;
import it.unical.mat.wrapper.DLVInvocation;
import it.unical.mat.wrapper.DLVInvocationException;
import it.unical.mat.wrapper.DLVWrapper;
import it.unical.mat.wrapper.Model;
import it.unical.mat.wrapper.ModelHandler;
import it.unical.mat.wrapper.ModelResult;
import it.unical.mat.wrapper.Predicate;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.semanticweb.drew.dlprogram.format.DLProgramStorer;
import org.semanticweb.drew.dlprogram.format.DLProgramStorerImpl;
import org.semanticweb.drew.dlprogram.model.DLProgram;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eu.fbk.dkm.aspit.rewriter.PoIDatalogRewriter;

/**
 * @author Loris
 * @version 1.0
 * 
 * Represents the program generated in the DLP translation. 
 * Contains methods to compute and store the program. 
 */
public class KBProgram {
	
	//--- FIELDS ---------------------------------------------
	
	//TODO: move this to CLI
	private static final String DEFAULT_DLV_PATH = "./localdlv/dlv";
	private static final String DEFAULT_OUTPUT_FILENAME = "./output.dlv";
	
	private KnowledgeBase inputKB;
	private OWLOntologyManager manager;
	
	private DLProgram datalogProgram;
	private String additionalRules = "";
	
	private String outputFilePath; //Path to the output datalog file
	private String dlvPath; 	   //Path to DLV solver location
	
	private long rewritingTime;  //time in milliseconds for the complete rewriting
	private long modelComputationTime; //time in milliseconds for the DLV model computation
	
	//--- CONSTRUCTOR ------------------------------------------

	/**
	 * @param inputKB the knowledge base to be rewritten
	 */
	public KBProgram(KnowledgeBase inputKB) {
		this.inputKB = inputKB;
		this.manager = inputKB.getOWLOntology().getOWLOntologyManager();
		
		this.outputFilePath = DEFAULT_OUTPUT_FILENAME;
		this.dlvPath = DEFAULT_DLV_PATH;
		
		this.rewritingTime = 0;
	}
	
	//--- GET METHODS -----------------------------------------------------
	
	/**
	 * @return the outputFilePath
	 */
	public String getOutputFilePath() {	
		return outputFilePath; 
	}

	/**
	 * @return the dlvPath
	 */
	public String getDlvPath() { 
		return dlvPath; 
	}

	/**
	 * @return the rewritingTime
	 */
	public long getRewritingTime() { 
		return rewritingTime; 
	}

	/**
	 * @return the modelComputationTime
	 */
	public long getmodelComputationTime() { 
		return modelComputationTime; 
	}
	
	/**
	 * @return the program size in statements
	 */
	public int getProgramSize() { 
		return datalogProgram.getStatements().size(); 
	}	
	
	//--- SET METHODS -----------------------------------------------------
	
	/**
	 * @param outputFilePath the outputFilePath to set
	 */
	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}

	/**
	 * @param dlvPath the dlvPath to set
	 */
	public void setDlvPath(String dlvPath) {
		this.dlvPath = dlvPath;
	}
	
	//--- REWRITING ---------------------------------------------
	
	/**
	 * Rewrites the whole input CKR to global and local programs.
	 */
	public void rewrite(){
		
		long startTime = System.currentTimeMillis();
		
		//Rewriting to datalog.
		PoIDatalogRewriter rewriter = new PoIDatalogRewriter();
		rewriter.setKnowledgeBase(inputKB);
		
		//XXX: ############################
		
		datalogProgram = rewriter.rewrite();
		additionalRules = rewriter.getAdditionalRules();
		//System.out.println("Rewriting program complete.");
		
		//TODO: separate this from rewrite()
		//Interaction with DLV: computes the IT for the input axioms
		computeIT(datalogProgram);
				
		long endTime = System.currentTimeMillis();
		rewritingTime = endTime - startTime;
	}
	
	//--- SEARCH IN POIs ------------------------------------------
	
	private PieceOfInformation findByAtom(String pred, List<Term> list){	
		
		LinkedList<String> slist = new LinkedList<String>();
		for(Term t : list){
			slist.add(t.toString());
		}
		slist.removeFirst(); //removes IT term
		//System.out.println("slist:" + slist);
		
		for (PieceOfInformation poi : inputKB.getPOIs()) {
			if (poi.getPredicate().equals(pred)){	
				
				boolean isEqual = true;
				
				for (int i = 0; i < poi.getArguments().size(); i++) {
					if(!poi.getArguments().get(i).equals(slist.get(i)))
						isEqual = false;
				}
				
				if (isEqual)return poi;
			}			
		}		
		return null;
	}
	
	//--- REWRITING: DLV INTERACTION ------------------------------
	
	/**
	 * Interacts with DLV to compute the IT for the input formulas.
	 * 
	 * @param datalogKB program to be evaluated by DLV
	 */
	private void computeIT(DLProgram datalogKB){
		
		DLVInvocation invocation = DLVWrapper.getInstance().createInvocation(dlvPath);
		DLVInputProgram inputProgram = new DLVInputProgramImpl();

		try {			
			DLProgramStorer storer = new DLProgramStorerImpl();
			StringBuilder target = new StringBuilder();
			storer.store(datalogKB, target);
            
			//Add to DLV input program the contents of KBprogram. 
			String datalogKBText = target.toString() + additionalRules;
			inputProgram.addText(datalogKBText);
			
			//Set input program for current invocation.
			invocation.setInputProgram(inputProgram);
			
			//Filter for \inst predicates. 
			//List<String> filters = new LinkedList<String>();
			//filters.add("inst");
			//invocation.setFilter(filters, true);
			invocation.addOption("-nofinitecheck");
			
			//List of computed models, used to check at least a model is computed.
			final List<Model> models = new ArrayList<Model>();
			
			//Model handler: retrieves ITs in the computed model(s).
			invocation.subscribe(new ModelHandler() {

				@Override
				public void handleResult(DLVInvocation paramDLVInvocation,
						ModelResult modelResult) {
					
					//System.out.print("{ ");
					Model model = (Model) modelResult;
					models.add(model);

					model.beforeFirst();
					while (model.hasMorePredicates()) {

					Predicate predicate = model.nextPredicate();
					//Predicate predicate = model.getPredicate("inst");
					if (predicate != null){
						//System.out.println(predicate.name() + ": ");
						while (predicate.hasMoreLiterals()) {

							Literal literal = predicate.nextLiteral();
							PieceOfInformation poi = findByAtom(predicate.name(), literal.attributes());
							//System.out.println(predicate.name() + "(" + literal.attributes().toString() + ")");
							
							if (poi != null){																					 
							    //if POI is found, add first argument as IT
								poi.addInfoterm(literal.getAttributeAt(0).toString());
								//System.out.println("IT: " + poi.getInfoterms().getFirst());								
							}
							
								//System.out.print(literal);
								//System.out.println(", ");	
							//}
						}
					}
					}
										
					//System.out.println("}");
					//System.out.println();
				}
			});
			
			long startTime = System.currentTimeMillis();
			
			invocation.run();
			invocation.waitUntilExecutionFinishes();
			
			long endTime = System.currentTimeMillis();
			modelComputationTime = endTime - startTime;
			
			//System.out.println("DLV computation time: " + modelComputationTime + " ms.");
			
			List<DLVError> k = invocation.getErrors();
			if (k.size() > 0)
				System.err.println(k);
			
			//System.out.println("Number of computed models: " + models.size());
			if(models.size() == 0) 
				System.err.println("[!] No models for KB program.");
			
		} catch (DLVInvocationException | IOException e) {
			System.err.println("[!] Cannot execute DLV invocation. Check if DLV executable file exists at path: " + dlvPath + "\n");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	//--- FILE MANAGEMENT ---------------------------------------------
	
	/**
	 * Stores the computed CKR program to file.
	 * 
	 * @throws IOException
	 */
	public void storeToFile() throws IOException {
		
		DLProgramStorer storer = new DLProgramStorerImpl();
		
		//String datalogFile = inputCKR.getGlobalOntologyFilename() + ".dlv";
		//String datalogFile = "./testcase/output.dlv";
		//System.out.println(datalogGlobal.getStatements().size());
		
		FileWriter writer = new FileWriter(outputFilePath);
		storer.store(datalogProgram, writer);
		writer.write(additionalRules);
		//writer.flush();
		writer.close();
		
		//System.out.println("CKR program saved in: " + outputFilePath);
	}
}
//=======================================================================