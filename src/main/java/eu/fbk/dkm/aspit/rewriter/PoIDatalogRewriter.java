package eu.fbk.dkm.aspit.rewriter;

import java.util.Set;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntityVisitor;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;

import eu.fbk.dkm.aspit.kb.KnowledgeBase;
import eu.fbk.dkm.aspit.kb.PieceOfInformation;

/**
 * @author Loris
 * @version 1.0
 * 
 * Encodes the translation to Datalog for the PoIs of input ontology.
 * 
 */
public class PoIDatalogRewriter extends OWLAxiomVisitorAdapter implements
OWLEntityVisitor{ 
	
	private final static IRI TT = IRI.create("tt");
	private final static IRI TOP = IRI.create("top");
	
	private KnowledgeBase inputKB;
			
	private int labelnum;
	private String rulesString;

    //--- CONSTRUCTORS ----------------------------------------------------------
    
    public PoIDatalogRewriter() {
    	
    	this.labelnum = 0;
    	this.rulesString = "";
   	}
    
	//--- SET METHODS -----------------------------------------------------
        
    public void setKnowledgeBase(KnowledgeBase kb){
    	this.inputKB = kb;
    }    
    	
	//--- GET METHODS -----------------------------------------------------
	
	public String getProgramString(){
		return this.rulesString;
	}
	
    //--- REWRITE METHODS -------------------------------------------------------
    
	private void addStringRules(String r) {
		this.rulesString += r;
	}
	
	/**
	 * Rewrites input ontology as a Datalog program. 
	 */
	public void rewrite() {
		
		//datalog = new DLProgram();
		
		//Instantiate global properties.
		OWLOntology ontology = inputKB.getOWLOntology();
		
		//Rewriting for ontology components.
		for (OWLLogicalAxiom ax : ontology.getLogicalAxioms()) {
			ax.accept(this);
		}
		
		//for (OWLNamedIndividual i : ontology.getIndividualsInSignature()) {
		//	i.accept(this);
		//}

		//for (OWLObjectProperty p : ontology.getObjectPropertiesInSignature()) {
		//	p.accept(this);
		//}

		//for (OWLClass cls : ontology.getClassesInSignature()) {
		//	cls.accept(this);
		//}
		
		//return datalog;
	}
    	
	//--- COMPLEX CONCEPTS REWRITING METHODS --------------------------------------------
	
	private IRI getNewLabel(){
		return IRI.create("L" + labelnum++);
	}
	
	/**
	 * Recursively decomposes and rewrites the input concept.
	 * 
	 * @param isaxiom if the input concept represent the concept of an axiom
	 * @param individualIRI input individual
	 * @param classIRI input (possibly complex) concept
	 */
	private IRI rewriteConcept(boolean isaxiom, OWLIndividual individual, OWLClassExpression classExp){
		
		//atomic case: C = \top
		if (classExp.isOWLThing()){//treated as atomic ABox formula!
			//System.out.println("Top: " + classExp.toString());
			
			IRI label = getNewLabel();
			
			if(individual == null){//c in Var
			
				//add is_it(tt, c, Ltop) :- is(c, top).
				addStringRules("is_it("+ TT +", X,\""  + label + "\") :- "
						      +"is(X," + TOP + ").\n");

				//add is(c, top) :- is(c, Ltop).
				addStringRules("is(X," + TOP + ") :- "
						      +"is(X,\""  + label + "\").\n");
				
			} else {//c in NI
				IRI individualIRI = individual.asOWLNamedIndividual().getIRI(); 
			
				if(isaxiom){				
					addStringRules("is(\"" + individualIRI.getFragment() + "\"," + TOP + ").\n");					
				}
				//add is_it(tt, c, Ltop) :- is(c, top).
				addStringRules("is_it(" + TT + ", \"" + individualIRI.getFragment() + "\", \"" 
						+ label + "\") :- is(\"" 
						+ individualIRI.getFragment() + "\", " + TOP + ").\n");
				
				//add is(c, top) :- is(c, Ltop).
				addStringRules("is(\"" + individualIRI.getFragment() + "\", " + TOP + ") :- is(\"" 
						+ individualIRI.getFragment() + "\", \"" 
						+ label + "\").\n");				
			}
			return label;
						
		//atomic case: C = A \in NC
		} else if(classExp.getClassExpressionType() == ClassExpressionType.OWL_CLASS){
			//System.out.println("Atomic: " + classExp.toString());
			
			IRI classIRI = classExp.asOWLClass().getIRI();
			
			IRI label = getNewLabel();
			
			if(individual == null){//c in Var
			
				//add is_it(tt, c, LA) :- is(c, A).
				addStringRules("is_it(" + TT + ", X,\""  + label + "\") :- "
						      +"is(X,\""  + classIRI.getFragment() + "\").\n");

				//add is(c, A) :- is(c, LA).
				addStringRules("is(X,\""  + classIRI.getFragment() + "\") :- "
						      +"is(X,\""  + label + "\").\n");				
			} else {//c in NI
				
				IRI individualIRI = individual.asOWLNamedIndividual().getIRI(); 
			
				if(isaxiom){
					//add is(c, A)
					addStringRules("is(\"" + individualIRI.getFragment() + "\", \"" 
					                       + classIRI.getFragment() + "\").\n");					
				}
				//add is_it(tt, c, LA) :- is(c, A).
				addStringRules("is_it("+ TT + ", \"" + individualIRI.getFragment() + "\", \"" 
						+ label + "\") :- is(\"" 
						+ individualIRI.getFragment() + "\", \"" 
						+ classIRI.getFragment() + "\").\n");
				
				//add is(c, A) :- is(c, LA).
				addStringRules("is(\"" + individualIRI.getFragment() + "\", \"" 
						+ classIRI.getFragment() + "\") :- is(\"" 
						+ individualIRI.getFragment() + "\", \"" 
						+ label + "\").\n");				
			}
			return label;
						
		//conjunction: C = C1 \and C2
		} else if (classExp.getClassExpressionType() == ClassExpressionType.OBJECT_INTERSECTION_OF){
			//System.out.println("And: " + classExp.toString());
			
			IRI label = getNewLabel();

			OWLObjectIntersectionOf inter = (OWLObjectIntersectionOf) classExp;
			Set<OWLClassExpression> operands = inter.getOperands();
			OWLClassExpression[] params = new OWLClassExpression[2];
			int i = 0;
			for (OWLClassExpression op : operands) {
				params[i++] = op;
			}
			IRI labelA = rewriteConcept(false, individual, params[0]); //*!*
			IRI labelB = rewriteConcept(false, individual, params[1]); //*!*
						
			if(individual == null){//c in Var

				//add is_it([X,Y], c, LC) :- is_it(X, c, LA), is_it(Y, c, LB).
				addStringRules("is_it([X,Y], Z,\"" + label + "\") :- "
				               +"is_it(X, Z,\"" + labelA + "\"), "
	                           +"is_it(Y, Z,\"" + labelB + "\").\n");
				
				//add is(c,LA) :- is(c, LC). is(c,LB) :- is(c, LC). 
				addStringRules("is(X,\"" + labelA + "\") :- "
			               +"is(X,\"" + label + "\").\n");
			    addStringRules("is(X,\"" + labelB + "\") :- "
		               	   +"is(X,\"" + label + "\").\n");								
			} else {//c in NI
				
				IRI individualIRI = individual.asOWLNamedIndividual().getIRI(); 
				
				if(isaxiom){
					//add is(c,LC).
					addStringRules("is(\"" + individualIRI.getFragment() + "\", \"" 
										   + label + "\").\n");					
				}
				
				//add is_it([X,Y], c, LC) :- is_it(X, c, LA), is_it(Y, c, LB).
				addStringRules("is_it([X,Y], \"" + individualIRI.getFragment() + "\",\"" + label + "\") :- "
				               +"is_it(X, \"" + individualIRI.getFragment() + "\",\"" + labelA + "\"), "
	                           +"is_it(Y, \"" + individualIRI.getFragment() + "\",\"" + labelB + "\").\n");
				
				//add is(c,LA) :- is(c, LC). is(c,LB) :- is(c, LC). 
				addStringRules("is(\"" + individualIRI.getFragment() + "\",\"" + labelA + "\") :- "
			               +"is(\"" + individualIRI.getFragment() + "\",\"" + label + "\").\n");
			    addStringRules("is(\"" + individualIRI.getFragment() + "\",\"" + labelB + "\") :- "
		               	   +"is(\"" + individualIRI.getFragment() + "\",\"" + label + "\").\n");				
			}
			return label;
			
		//exists: C = \exists R.C1
		} else if (classExp.getClassExpressionType() == ClassExpressionType.OBJECT_SOME_VALUES_FROM){
			//System.out.println("Exists: " + classExp.toString());

			IRI label = getNewLabel();
			
			OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) classExp;
			IRI labelA = rewriteConcept(false, null, some.getFiller()); //*!*
			
			if(individual == null){//c in Var
				
				//add is_it([X,Y], c, LC) :- rel_it(tt, c, r, X), is_it(Y, X, LA).
				addStringRules("is_it([X,Y], Z, \"" + label + "\") :- "
				               +"rel_it("+ TT + ", Z, \"" + some.getProperty().asOWLObjectProperty().getIRI().getFragment() + "\", X), "
	                           +"is_it(Y, X, \"" + labelA + "\").\n");
				
				//add is(X,LA) :- rel(c,r,X),is(c,LC).
				addStringRules("is(X, \"" + labelA + "\") :- "
			                  +"rel(Z, \"" + some.getProperty().asOWLObjectProperty().getIRI().getFragment() + "\", X), "
                              +"is(Z, \"" + label + "\").\n");								
				
			} else {//c in NI
				IRI individualIRI = individual.asOWLNamedIndividual().getIRI(); 

				if(isaxiom){
					//add is(c,LC).
					addStringRules("is(\"" + individualIRI.getFragment() + "\", \"" 
										   + label + "\").\n");					
				}				
				//add is_it([X,Y], c, LC) :- rel_it(tt, c, r, X), is_it(Y, X, LA).
				addStringRules("is_it([X,Y], \"" + individualIRI.getFragment() + "\", \"" + label + "\") :- "
				               +"rel_it(" + TT + ", \"" + individualIRI.getFragment() + "\", \"" + some.getProperty().asOWLObjectProperty().getIRI().getFragment() + "\", X), "
	                           +"is_it(Y, X, \"" + labelA + "\").\n");
				
				//add is(X,LA) :- rel(c,r,X),is(c,LC).
				addStringRules("is(X, \"" + labelA + "\") :- "
			                  +"rel(\"" + individualIRI.getFragment() + "\", \"" + some.getProperty().asOWLObjectProperty().getIRI().getFragment() + "\", X), "
                              +"is(\"" + individualIRI.getFragment() + "\", \"" + label + "\").\n");				
			}			
			return label;			
		}
		return null;
	}
	
	//--- ONTOLOGY VISIT METHODS --------------------------------------------------------
	
	//- - ABOX AXIOMS - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	//Rewrite C(a)
	public void visit(OWLClassAssertionAxiom axiom) {
		
		OWLIndividual individual = axiom.getIndividual();
		OWLClassExpression classExp = axiom.getClassExpression();
		
		//rewrites complex concept and returns label associated to this concept
		IRI label = rewriteConcept(true, individual, classExp);
		
		if(label != null){
			//System.out.println(label.toString());
		
			PieceOfInformation poi = new PieceOfInformation(axiom);
		
			//poi.setPredicate(PoIRewritingVocabulary.INST.getName());
			poi.setPredicate("is_it");
			poi.addArgument(individual.asOWLNamedIndividual().getIRI().getFragment());
			poi.addArgument(label.toString()); //*!*
	
			inputKB.getPOIs().add(poi);
		
			//System.out.println(poi.getPredicate() + "(X, " + poi.getArguments().toString() + ")");
		}
	}
	
	@Override
	//Rewrite R(a,b)
	public void visit(OWLObjectPropertyAssertionAxiom axiom) {
		
		OWLNamedIndividual subj = axiom.getSubject().asOWLNamedIndividual();
		OWLObjectProperty prop = axiom.getProperty().asOWLObjectProperty();
		OWLNamedIndividual obj = axiom.getObject().asOWLNamedIndividual();
		
		//add Rel(a, R, b).
		addStringRules("rel(\"" + subj.getIRI().getFragment() + "\", \""
				                + prop.getIRI().getFragment() + "\", \"" 
				                + obj.getIRI().getFragment() + "\").\n");
		
		//add rel_it(tt, a, R, b) :- rel(a, R, b).
		addStringRules("rel_it(" + TT + ", \""
								+ subj.getIRI().getFragment() + "\", \""
								+ prop.getIRI().getFragment() + "\", \"" 
								+ obj.getIRI().getFragment() +
				                "\") :- rel(\"" + subj.getIRI().getFragment() + "\", \""
				                + prop.getIRI().getFragment() + "\", \"" 
				                + obj.getIRI().getFragment() + "\").\n");

		PieceOfInformation poi = new PieceOfInformation(axiom);
		
		poi.setPredicate("rel_it");
		poi.addArgument(subj.getIRI().getFragment());
		poi.addArgument(prop.getIRI().getFragment());
		poi.addArgument(obj.getIRI().getFragment());
		
		inputKB.getPOIs().add(poi);
		
		//System.out.println(poi.getPredicate() + "(X, " + poi.getArguments().toString() + ")");
	}
	
	//- - TBOX AXIOMS - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
    
	@Override
	//Rewrite A \subs B
	public void visit(OWLSubClassOfAxiom axiom) {
		OWLClassExpression subClass = axiom.getSubClass();
		OWLClassExpression superClass = axiom.getSuperClass();
		
		//System.out.println("SubClass: " + axiom);
				
		IRI gen = subClass.asOWLClass().getIRI(); 
		
		//rewrites complex concept for superclass
		IRI labelA = rewriteConcept(false, null, superClass); //*!*

		//add is(X, LA) :- is(X, g).
		addStringRules("is(X, \"" + labelA + "\") :- "
		               +"is(X, \"" + gen.getFragment() + "\").\n");
		
		//add isa_it([X,Y], g, LA) :- is(X, g), is_it(Y, X, LA).
		addStringRules("isa_it([X,Y], \"" + gen.getFragment() + "\", \"" + labelA + "\") :- "
		               +"is(X, \"" + gen.getFragment() + "\"), "
                       +"is_it(Y, X, \"" + labelA + "\").\n");
		
		if(labelA != null){
			//System.out.println(label.toString());
		
			PieceOfInformation poi = new PieceOfInformation(axiom);
		
			//poi.setPredicate(PoIRewritingVocabulary.INST.getName());
			poi.setPredicate("isa_it");
			poi.addArgument(gen.getFragment());
			poi.addArgument(labelA.toString()); //*!*
	
			inputKB.getPOIs().add(poi);
		
			//System.out.println(poi.getPredicate() + "(X, " + poi.getArguments().toString() + ")");
		}
	}		
		
	//- - RBOX AXIOMS - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
		
	//- - SIGNATURE - - - - - - - - - - - - - - - - - - - - - - - -	
		
	//- - NOT TREATED - - - - - - - - - - - - - - - - - - - - - - - 
		
	@Override
	public void visit(OWLClass arg0) {
	}

	@Override
	public void visit(OWLObjectProperty arg0) {
	}

	@Override
	public void visit(OWLDataProperty arg0) {
	}

	@Override
	public void visit(OWLNamedIndividual individual) {
	}
	
	@Override
	public void visit(OWLDatatype arg0) {
	}

	@Override
	public void visit(OWLAnnotationProperty arg0) {
	}
		
}
//=======================================================================