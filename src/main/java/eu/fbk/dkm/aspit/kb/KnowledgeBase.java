package eu.fbk.dkm.aspit.kb;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * @author Loris
 * @version 1.0
 * 
 * Represents the working ontology for which information terms are computed. 
 */
public class KnowledgeBase {
	
	//--- FIELDS ---------------------------------------------
	
	private static final String ELC_SCHEMA_FILENAME = "./schemas/elc-schema.n3";
	private static final String ELC_SCHEMA_URI = "http://dkm.fbk.eu/asp-it/elc-schema#";
	
	private String outputFilePath;
	private String poiWarnings;

	private String kbFilename;
	private File kbFile;
	private OWLOntology kbOWLOntology;
	
	private OWLOntologyFormat format = new TurtleOntologyFormat();
	private OWLAnnotationProperty hasIT;
	
	//Set of pieces of informations computed by the rewriting
	private Set<PieceOfInformation> POIs = new HashSet<PieceOfInformation>();
			
	//--- CONSTRUCTOR ---------------------------------------------
	
	public KnowledgeBase() {
		this.kbFilename = "";
		this.poiWarnings = "";
	}

	//--- GET METHODS ---------------------------------------------

	public String getOntologyFilename() {
		return kbFilename;
	}

	public String getOutputFilePath() {		
		return this.outputFilePath;
	}
	
	public File getOntologyFile() {
		return kbFile;
	}
	
	public OWLOntology getOWLOntology() {
		return kbOWLOntology;
	}	
	
	public Set<PieceOfInformation> getPOIs() {
		return POIs;
	}

	public String getPOIWarnings() {
		return poiWarnings;
	}

	
	//--- SET METHODS ---------------------------------------------
	
	public void setOntologyFilename(String ofn) {
		this.kbFilename = ofn;
	}
	
	public void setOutputFilePath(String ofp) {
		outputFilePath = ofp;
	}

	private void setOntologyFile(File f) {
	   this.kbFile = f;
	}
	
	public void setOWLOntology(OWLOntology o) {
		this.kbOWLOntology = o;
	}
	
	//--- FILES LOAD ---------------------------------------------
	
	/**
	 * Initializes the files and loads the input ontology
	 * in the knowledge base.
	 * 
	 * @throws OWLOntologyCreationException
	 */
	public void loadOntology() throws OWLOntologyCreationException {
		File file = new File(this.getOntologyFilename());
		this.setOntologyFile(file);
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();

		//Load ontology.
		OWLOntology onto = man.loadOntologyFromOntologyDocument(file);
		
		//Load ELc definition vocabulary.
		try{
			File schemaFile = new File(ELC_SCHEMA_FILENAME);
			OWLOntology schema = man.loadOntologyFromOntologyDocument(schemaFile);
			man.addAxioms(onto, schema.getAxioms()); //*!*	
			
		} catch(Exception e) {
			throw new OWLOntologyCreationException("[!] ELc schema not found in " + ELC_SCHEMA_FILENAME);
		}
		
		this.setOWLOntology(onto);
	}
	
	//--- ADD ANNOTATIONS ----------------------------------------
	
	public void addITAnnotations(){
		
		OWLOntologyManager man = this.getOWLOntology().getOWLOntologyManager();
		OWLDataFactory factory = man.getOWLDataFactory(); 
		
    	this.hasIT = factory.getOWLAnnotationProperty(
    			IRI.create(ELC_SCHEMA_URI, "hasIT"));
		
    	Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
    	
    	for(PieceOfInformation poi : POIs){
    		
    		Set<OWLAnnotation> annotations = new HashSet<OWLAnnotation>();
    		
    		for(String it : poi.getInfoterms()){
    		  
    		   OWLAnnotationValue val = factory.getOWLLiteral(it);
    		   OWLAnnotation ann = factory.getOWLAnnotation(hasIT, val);
    		   
    		   annotations.add(ann);
    		   //System.out.println(ann);
    		}
    		
    		if(annotations.isEmpty()){
    			poiWarnings += "   [!] No ITs for axiom: " + poi.getFormula() + "\n";
    		}
    		
    		OWLAxiom ax = poi.getFormula().getAnnotatedAxiom(annotations);
    		//man.removeAxiom(getOWLOntology(), poi.getFormula());
    		axioms.add(ax);    		
    	}
    	
    	man.addAxioms(getOWLOntology(), axioms);
	}
	
	//--- FILES SAVE ---------------------------------------------

	/**
	 * Saves KB to its N3 representation to file
	 * 
	 * @param path
	 * @throws OWLOntologyStorageException
	 */
	public void saveToN3() throws OWLOntologyStorageException{
		
		String filepath = outputFilePath;
		
		OWLOntologyManager man = kbOWLOntology.getOWLOntologyManager();

		File file = new File(filepath);
		man.saveOntology(kbOWLOntology, format, IRI.create(file.toURI()));

		//System.out.println("Saved to: " + filepath);		
	}
	
}
//=======================================================================