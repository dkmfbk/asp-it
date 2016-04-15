/**
 * 
 */
package eu.fbk.dkm.aspit.kb;

import java.util.LinkedList;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * @author Loris
 * @version 1.0
 * 
 * Represents a single (set of) pieces of information (POI) of the input ontology.
 * Stores the information terms satisfying the formula and the 
 * Datalog representation of the POI.
 */
public class PieceOfInformation {
	
	private OWLAxiom formula;
	private LinkedList<String> infoterms;

	private String predicate;
	private LinkedList<String> arguments;
	
	//--- CONSTRUCTOR ---------------------------------------------
	
	public PieceOfInformation(OWLAxiom f) {
		this.formula = f;
		this.infoterms = new LinkedList<String>();
		this.arguments = new LinkedList<String>();
	}
	
	//--- GET METHODS ---------------------------------------------
	
	public OWLAxiom getFormula() {
		return formula;
	}

	public LinkedList<String> getInfoterms() {
		return infoterms;
	}	
	
	public String getPredicate(){
		return predicate;
	}
	
	public LinkedList<String> getArguments() {
		return arguments;
	}
	
	//--- SET METHODS ---------------------------------------------
	
	public void addInfoterm(String it){
		this.infoterms.add(it);
	}
	
	public void setPredicate(String p){
		this.predicate = p;
	}

	public void addArgument(String a){
		this.arguments.add("\"" + a + "\"");
	}
		
	//--- EQUALS ---------------------------------------------
	
	public boolean equals(PieceOfInformation other){
		return this.formula.equals(other.getFormula());
	}
	
	//--- TO STRING ---------------------------------------------
	
	public String toString() {
		String s = "";
		
		for(String it : infoterms){
			s += it;
			s += " , ";
		}
		s += " : ";
		s += formula.toString();
		
		return s;
	}	
}
