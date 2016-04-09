package eu.fbk.dkm.aspit.rewriter;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.drew.dlprogram.model.CacheManager;
import org.semanticweb.drew.dlprogram.model.Clause;
import org.semanticweb.drew.dlprogram.model.Constant;
import org.semanticweb.drew.dlprogram.model.DLProgram;
import org.semanticweb.drew.dlprogram.model.Literal;
import org.semanticweb.drew.dlprogram.model.NormalPredicate;
import org.semanticweb.drew.dlprogram.model.Term;
import org.semanticweb.drew.dlprogram.model.Variable;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntityVisitor;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;

import eu.fbk.dkm.aspit.kb.KnowledgeBase;
import eu.fbk.dkm.aspit.kb.PieceOfInformation;

/**
 * @author Loris
 * @version 0.1
 * 
 * Encodes the translation to Datalog of the PoI of input ontology.
 * 
 */
public class PoIDatalogRewriter extends OWLAxiomVisitorAdapter implements
OWLEntityVisitor{ 
	
	private KnowledgeBase inputKB;
	private OWLDataFactory factory;
	//protected OWLOntologyManager manager;
	
	private DLProgram datalog;
//	private Term indiv1Term, indiv2Term, indiv3Term;
	
	private IRI tt;

    //--- CONSTRUCTORS ----------------------------------------------------------
    
    public PoIDatalogRewriter() {
    	
    	this.tt = IRI.create("tt");
    	
//		this.indiv1Term = getVariable("X");
//		this.indiv2Term = getVariable("Y");
//		this.indiv3Term = getVariable("Z");		
	}

	//--- SET METHODS -----------------------------------------------------
        
    public void setKnowledgeBase(KnowledgeBase kb){
    	this.inputKB = kb;
    }    
    
    //--- REWRITE METHOD --------------------------------------------------------
    
	/**
	 * Rewrites inputKB ontology as a Datalog program.
	 * 
	 * @return the output Datalog program
	 */
	public DLProgram rewrite() {
		
		datalog = new DLProgram();
		
		//Instantiate global properties.
		OWLOntology ontology = inputKB.getOWLOntology();
		this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		
		//Rewriting for ontology components.
		for (OWLLogicalAxiom ax : ontology.getLogicalAxioms()) {
			ax.accept(this);
		}
		
		for (OWLNamedIndividual i : ontology.getIndividualsInSignature()) {
			i.accept(this);
		}

		for (OWLObjectProperty p : ontology.getObjectPropertiesInSignature()) {
			p.accept(this);
		}

		for (OWLClass cls : ontology.getClassesInSignature()) {
			cls.accept(this);
		}

 		//Add deduction rules.
		//datalog.addAll(DeductionRuleset.getPrl());
		
		return datalog;
	}
        
	//--- ONTOLOGY VISIT METHODS --------------------------------------------------------
    //NOTE: v.0.1: only for atomic axioms
	
	//- - ABOX AXIOMS - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	//Rewrite C(a)
	public void visit(OWLClassAssertionAxiom axiom) {
		IRI individualIRI = axiom.getIndividual().asOWLNamedIndividual().getIRI(); 
		IRI classIRI = axiom.getClassExpression().asOWLClass().getIRI();
		
		addFact(PoIRewritingVocabulary.INST,
				tt,
				individualIRI, 
				classIRI);
		
		PieceOfInformation poi = new PieceOfInformation(axiom);
		
		poi.setPredicate(PoIRewritingVocabulary.INST.getName());
		poi.addArgument(individualIRI.getFragment());
		poi.addArgument(classIRI.getFragment());
		
		inputKB.getPOIs().add(poi);
		
		//System.out.println(poi);
		//System.out.println(poi.getPredicate() + "(X, " + poi.getArguments().toString() + ")");
	}
	
	@Override
	//Rewrite R(a,b)
	public void visit(OWLObjectPropertyAssertionAxiom axiom) {
		
		addFact(PoIRewritingVocabulary.REL,
				tt,
				axiom.getSubject().asOWLNamedIndividual().getIRI(),
 				axiom.getProperty().asOWLObjectProperty().getIRI(),
				axiom.getObject().asOWLNamedIndividual().getIRI()
		);

		PieceOfInformation poi = new PieceOfInformation(axiom);
		
		poi.setPredicate(PoIRewritingVocabulary.REL.getName());		
		poi.addArgument(axiom.getSubject().asOWLNamedIndividual().getIRI().getFragment());
		poi.addArgument(axiom.getProperty().asOWLObjectProperty().getIRI().getFragment());
		poi.addArgument(axiom.getObject().asOWLNamedIndividual().getIRI().getFragment());
		
		inputKB.getPOIs().add(poi);
		
		//System.out.println(poi);
		//System.out.println(poi.getPredicate() + "(X, " + poi.getArguments().toString() + ")");
	}
	
	//- - TBOX AXIOMS - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
    
	@Override
	//Rewrite A \subs B
	public void visit(OWLSubClassOfAxiom axiom) {
//		OWLClassExpression subClass = axiom.getSubClass();
//		OWLClassExpression superClass = axiom.getSuperClass();
//
//		// atomic case: A \subs C
//		if ((subClass.getClassExpressionType() == ClassExpressionType.OWL_CLASS)
//				&& (superClass.getClassExpressionType() == ClassExpressionType.OWL_CLASS)) {
//			// int a =
//			// iriEncoder.encode(axiom.getSubClass().asOWLClass().getIRI());
//			// int c =
//			// iriEncoder.encode(axiom.getSuperClass().asOWLClass().getIRI());
//
//			//if (subClass.isOWLThing()) {
//			//	// addFact(RewritingVocabulary.TOP, c);
//			//	addFact(CKRRewritingVocabulary.TOP, axiom.getSuperClass()
//			//			.asOWLClass().getIRI());
//			//} else if (superClass.isOWLNothing()) {
//			//	// addFact(RewritingVocabulary.BOT, a);
//			//	addFact(CKRRewritingVocabulary.BOT, axiom.getSubClass()
//			//			.asOWLClass().getIRI());
//			//} else /* (!subClass.isOWLNothing() && !superClass.isOWLThing()) */{
//				// addFact(RewritingVocabulary.SUB_CLASS, a, c);
//
//			       addFact(PoIRewritingVocabulary.SUB_CLASS, //
//						subClass.asOWLClass().getIRI(), 
//						superClass.asOWLClass().getIRI(),
//						contextID);
//			//}
//				//add rule for overriding
//			    if(isDefeasible(axiom)){
//			    	//System.out.println("D( A subs B ): " + axiom.getAxiomWithoutAnnotations());
//			    	
//					Term subclassTerm = getConstant(subClass.asOWLClass().getIRI());
//					Term superclassTerm = getConstant(superClass.asOWLClass().getIRI());
//
//					Literal ovrsubs = getLiteral(true, PoIRewritingVocabulary.OVRSUBCLASS, 
//							                            indiv1Term, subclassTerm, superclassTerm, contextTerm);
//					Literal ninstd = getLiteral(false, PoIRewritingVocabulary.INSTD, 
//							                            indiv1Term, superclassTerm, contextTerm);
//					Literal instd = getLiteral(true, PoIRewritingVocabulary.INSTD, 
//                                                        indiv1Term, subclassTerm, contextTerm);
//					Literal prec = getLiteral(true, PoIRewritingVocabulary.PREC, 
//							                          contextTerm, globalTerm);
//					addRule(ovrsubs, ninstd, instd, prec);	
//			    }								
//		}
//		
//		// A' subclass C
//		else if (superClass.getClassExpressionType() == ClassExpressionType.OWL_CLASS) {
//			// and(A, B) subclass C
//			if (subClass.getClassExpressionType() == ClassExpressionType.OBJECT_INTERSECTION_OF) {
//				// TODO: CASE WITH N>2 IN NORMALIZATION
//				// OWLObjectIntersectionOf inter = (OWLObjectIntersectionOf) subClass;
//				// Set<OWLClassExpression> operands = inter.getOperands();
//				// int[] params = new int[3];
//				// int i = 0;
//				// for (OWLClassExpression op : operands) {
//				//   params[i++] = iriEncoder.encode(op.asOWLClass().getIRI());
//				// }
//				// params[2] = iriEncoder.encode(superClass.asOWLClass().getIRI());
//				// addFact(RewritingVocabulary.SUB_CONJ, params);
//				OWLObjectIntersectionOf inter = (OWLObjectIntersectionOf) subClass;
//				Set<OWLClassExpression> operands = inter.getOperands();
//				IRI[] params = new IRI[4];
//				int i = 0;
//				for (OWLClassExpression op : operands) {
//					params[i++] = op.asOWLClass().getIRI();
//				}
//				params[2] = superClass.asOWLClass().getIRI();
//				params[3] = contextID;
//
//				addFact(PoIRewritingVocabulary.SUB_CONJ, params);
//				
//				//add rule for overriding
//			    if(isDefeasible(axiom)){
//			    	//System.out.println("D( A1 \\and A2 \\subs B ): " + axiom.getAxiomWithoutAnnotations());
//			    	
//					Term subclassATerm = getConstant(params[0]);
//					Term subclassBTerm = getConstant(params[1]);
//					Term superclassTerm = getConstant(superClass.asOWLClass().getIRI());
//					
//					Literal ovrsubs = getLiteral(true, PoIRewritingVocabulary.OVRSUBCONJ, 
//							                            indiv1Term, subclassATerm, subclassBTerm, superclassTerm, contextTerm);
//					Literal ninstd = getLiteral(false, PoIRewritingVocabulary.INSTD, 
//							                            indiv1Term, superclassTerm, contextTerm);
//					Literal instdA = getLiteral(true, PoIRewritingVocabulary.INSTD, 
//                                                        indiv1Term, subclassATerm, contextTerm);
//					Literal instdB = getLiteral(true, PoIRewritingVocabulary.INSTD, 
//                                                        indiv1Term, subclassBTerm, contextTerm);					
//					Literal prec = getLiteral(true, PoIRewritingVocabulary.PREC, 
//							                          contextTerm, globalTerm);
//					addRule(ovrsubs, ninstd, instdA, instdB, prec);	
//			    }
//			}
//
//			// exist(R,A) subclass B
//			else if (subClass.getClassExpressionType() == ClassExpressionType.OBJECT_SOME_VALUES_FROM) {
//				OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) subClass;
//				// int r =
//				// iriEncoder.encode(some.getProperty().asOWLObjectProperty().getIRI());
//				// int a =
//				// iriEncoder.encode(some.getFiller().asOWLClass().getIRI());
//				// int b = iriEncoder.encode(superClass.asOWLClass().getIRI());
//				//
//				// addFact(RewritingVocabulary.SUB_EX, r, a, b);
//				addFact(PoIRewritingVocabulary.SUB_EX,//
//						some.getProperty().asOWLObjectProperty().getIRI(),//
//						some.getFiller().asOWLClass().getIRI(),//
//						superClass.asOWLClass().getIRI(), 
//						contextID);
//				
//				//add rule for overriding
//			    if(isDefeasible(axiom)){
//			    	//System.out.println("D( \\exists R.A \\subs B ): " + axiom.getAxiomWithoutAnnotations());
//			    	
//					Term propertyTerm = getConstant(some.getProperty().asOWLObjectProperty().getIRI());
//					Term subclassATerm = getConstant(some.getFiller().asOWLClass().getIRI());
//					Term superclassTerm = getConstant(superClass.asOWLClass().getIRI());
//
//					Literal ovrsubs = getLiteral(true, PoIRewritingVocabulary.OVRSUBEX, 
//							                            indiv1Term, propertyTerm, subclassATerm, superclassTerm, contextTerm);
//					Literal ninstd = getLiteral(false, PoIRewritingVocabulary.INSTD, 
//							                            indiv1Term, superclassTerm, contextTerm);
//					Literal tripled = getLiteral(true, PoIRewritingVocabulary.TRIPLED, 
//                                                        indiv1Term, propertyTerm, indiv2Term, contextTerm);
//					Literal instd = getLiteral(true, PoIRewritingVocabulary.INSTD, 
//                                                        indiv2Term, subclassATerm, contextTerm);					
//					Literal prec = getLiteral(true, PoIRewritingVocabulary.PREC, 
//							                          contextTerm, globalTerm);
//					addRule(ovrsubs, ninstd, tripled, instd, prec);	
//			    }
//
//			} else if (subClass.getClassExpressionType() == ClassExpressionType.OBJECT_ONE_OF) {
//				// {c} \subs A -> inst(c, A)
//				OWLObjectOneOf oneOf = (OWLObjectOneOf) subClass;
//				// int c =
//				// iriEncoder.encode(oneOf.getIndividuals().iterator().next().asOWLNamedIndividual().getIRI());
//				// int a = iriEncoder.encode(superClass.asOWLClass().getIRI());
//				// addFact(RewritingVocabulary.SUB_CLASS, c, a);
//				addFact(PoIRewritingVocabulary.INSTA, //
//						oneOf.getIndividuals().iterator().next().asOWLNamedIndividual().getIRI(),//
//						superClass.asOWLClass().getIRI(),
//						contextID);
//				
//				//add rule for overriding
//			    if(isDefeasible(axiom)){
//			    	//System.out.println("D( {a} \\subs B ): " + axiom.getAxiomWithoutAnnotations());
//			    	
//					Term indivTerm = getConstant(oneOf.getIndividuals().iterator().next().asOWLNamedIndividual().getIRI());
//					Term classTerm = getConstant(superClass.asOWLClass().getIRI());
//
//					Literal ovrinsta = getLiteral(true, PoIRewritingVocabulary.OVRINSTA, 
//							                              indivTerm, classTerm, contextTerm);
//					Literal ninstd = getLiteral(false, PoIRewritingVocabulary.INSTD, 
//							                            indivTerm, classTerm, contextTerm);
//					Literal prec = getLiteral(true, PoIRewritingVocabulary.PREC, 
//							                          contextTerm, globalTerm);					
//					addRule(ovrinsta, ninstd, prec);	
//			    }
//
//			} else {
//				throw new IllegalStateException();
//			}
//		}
//		
//		// A subclass C'
//		else if (subClass.getClassExpressionType() == ClassExpressionType.OWL_CLASS) {
//			
//			if (superClass.getClassExpressionType() == ClassExpressionType.OBJECT_SOME_VALUES_FROM) {
//				
//				OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) superClass;
//				OWLObjectOneOf object = (OWLObjectOneOf) some.getFiller();
//				
//				addFact(PoIRewritingVocabulary.SUP_EX, //
//						subClass.asOWLClass().getIRI(),//
//						some.getProperty().asOWLObjectProperty().getIRI(),
//						object.getIndividuals().iterator().next().asOWLNamedIndividual().getIRI(),
//						contextID);
//
//				//add rule for overriding
//				if(isDefeasible(axiom)){
//					//System.out.println("D( A \\subs \\exists R.{a} ): " + axiom.getAxiomWithoutAnnotations());
//					
//					Term propertyTerm = getConstant(some.getProperty().asOWLObjectProperty().getIRI());
//					Term subclassTerm = getConstant(subClass.asOWLClass().getIRI());
//					Term nominalTerm = getConstant(object.getIndividuals().iterator().next().asOWLNamedIndividual().getIRI());
//
//					Literal ovrsubs = getLiteral(true, PoIRewritingVocabulary.OVRSUPEX, 
//							                            indiv1Term, subclassTerm, propertyTerm, nominalTerm, contextTerm);
//					Literal ntripled = getLiteral(false, PoIRewritingVocabulary.TRIPLED, 
//	                                                    indiv1Term, propertyTerm, nominalTerm, contextTerm);
//					Literal instd = getLiteral(true, PoIRewritingVocabulary.INSTD, 
//	                                                    indiv1Term, subclassTerm, contextTerm);					
//					Literal prec = getLiteral(true, PoIRewritingVocabulary.PREC, 
//							                          contextTerm, globalTerm);
//					addRule(ovrsubs, ntripled, instd, prec);
//				}
//				    
//			// A subclass all(R, C')		
//			} else if (superClass.getClassExpressionType() == ClassExpressionType.OBJECT_ALL_VALUES_FROM) {
//
//				OWLObjectAllValuesFrom all = (OWLObjectAllValuesFrom) superClass;
//				OWLClass object = (OWLClass) all.getFiller();
//				
//				addFact(PoIRewritingVocabulary.SUP_ALL, //
//						subClass.asOWLClass().getIRI(),//
//						all.getProperty().asOWLObjectProperty().getIRI(),
//						object.asOWLClass().getIRI(),
//						contextID);
//				
//				//add rule for overriding
//			    if(isDefeasible(axiom)){
//			    	//System.out.println("D( A \\subs \\forall R.B ): " + axiom.getAxiomWithoutAnnotations());
//			    	
//					Term propertyTerm = getConstant(all.getProperty().asOWLObjectProperty().getIRI());
//					Term subclassTerm = getConstant(subClass.asOWLClass().getIRI());
//					Term objectTerm = getConstant(object.asOWLClass().getIRI());
//
//					Literal ovrsubs = getLiteral(true, PoIRewritingVocabulary.OVRSUPALL, 
//							                            indiv1Term, indiv2Term, subclassTerm, propertyTerm, objectTerm, contextTerm);
//					Literal ninstd = getLiteral(false, PoIRewritingVocabulary.INSTD, 
//                                                        indiv2Term, objectTerm, contextTerm);					
//					Literal instd = getLiteral(true, PoIRewritingVocabulary.INSTD, 
//                                                        indiv1Term, subclassTerm, contextTerm);
//					Literal tripled = getLiteral(true, PoIRewritingVocabulary.TRIPLED, 
//                                                        indiv1Term, propertyTerm, indiv2Term, contextTerm);
//					Literal prec = getLiteral(true, PoIRewritingVocabulary.PREC, 
//							                          contextTerm, globalTerm);					
//					addRule(ovrsubs, ninstd, instd, tripled, prec);
//			    }
//				
//			// A subclass max(R, 1, B')
//		    // Assuming n=1
//			} else if (superClass.getClassExpressionType() == ClassExpressionType.OBJECT_MAX_CARDINALITY) {
//				
//				OWLObjectMaxCardinality max = (OWLObjectMaxCardinality) superClass;
//				OWLClass object = (OWLClass) max.getFiller();
//				
//				addFact(PoIRewritingVocabulary.SUP_LEQONE, //
//						subClass.asOWLClass().getIRI(),//
//						max.getProperty().asOWLObjectProperty().getIRI(),
//						object.asOWLClass().getIRI(),
//						contextID);
//				
//				//add rule for overriding
//			    if(isDefeasible(axiom)){
//			    	//System.out.println("D( A \\subs \\leq 1 R.B ): " + axiom.getAxiomWithoutAnnotations());
//			    	
//					Term propertyTerm = getConstant(max.getProperty().asOWLObjectProperty().getIRI());
//					Term subclassTerm = getConstant(subClass.asOWLClass().getIRI());
//					Term objectTerm = getConstant(object.asOWLClass().getIRI());					
//
//					Literal ovrsubs = getLiteral(true, PoIRewritingVocabulary.OVRSUPLEQONE, 
//							                            indiv1Term, indiv2Term, indiv3Term, subclassTerm, propertyTerm, objectTerm, contextTerm);
//					Literal ovrsubs2 = getLiteral(true, PoIRewritingVocabulary.OVRSUPLEQONE, 
//                                                        indiv1Term, indiv3Term, indiv2Term, subclassTerm, propertyTerm, objectTerm, contextTerm);					
//					Literal neq = getLiteral(false, PoIRewritingVocabulary.EQ, 
//                                                        indiv2Term, indiv3Term, contextTerm);
//					Literal instdA = getLiteral(true, PoIRewritingVocabulary.INSTD, 
//                                                        indiv1Term, subclassTerm, contextTerm);
//					Literal tripledy = getLiteral(true, PoIRewritingVocabulary.TRIPLED, 
//                                                        indiv1Term, propertyTerm, indiv2Term, contextTerm);					
//					Literal tripledz = getLiteral(true, PoIRewritingVocabulary.TRIPLED, 
//                                                        indiv1Term, propertyTerm, indiv3Term, contextTerm);
//					Literal instdBy = getLiteral(true, PoIRewritingVocabulary.INSTD, 
//                                                        indiv2Term, objectTerm, contextTerm);
//					Literal instdBz = getLiteral(true, PoIRewritingVocabulary.INSTD, 
//                                                        indiv3Term, objectTerm, contextTerm);
//					Literal prec = getLiteral(true, PoIRewritingVocabulary.PREC, 
//							                          contextTerm, globalTerm);
//					addRule(ovrsubs, neq, instdA, tripledy, tripledz, instdBy, instdBz, prec);
//					addRule(ovrsubs2, neq, instdA, tripledy, tripledz, instdBy, instdBz, prec);
//			    }
//			    
//			// A subclass not(B)
//			} else if (superClass.getClassExpressionType() == ClassExpressionType.OBJECT_COMPLEMENT_OF) {
//
//				OWLObjectComplementOf not = (OWLObjectComplementOf) superClass;
//				IRI negatedClass = null;
//				
//				for (OWLClassExpression ce : not.getNestedClassExpressions()) {
//					if (!ce.isAnonymous()) negatedClass = ce.asOWLClass().getIRI();
//				}
//				
//				addFact(PoIRewritingVocabulary.SUP_NOT, //
//						subClass.asOWLClass().getIRI(),//
//						negatedClass,
//						contextID);
//
//				//add rule for overriding
//			    if(isDefeasible(axiom)){
//			    	//System.out.println("D( A \\subs \\not B ): " + axiom.getAxiomWithoutAnnotations());
//			    	
//					Term subclassTerm = getConstant(subClass.asOWLClass().getIRI());
//					Term negatedTerm = getConstant(negatedClass);
//
//					Literal ovrsubs = getLiteral(true, PoIRewritingVocabulary.OVRSUPNOT, 
//							                            indiv1Term, subclassTerm, negatedTerm, contextTerm);
//					Literal instdA = getLiteral(true, PoIRewritingVocabulary.INSTD, 
//                                                        indiv1Term, subclassTerm, contextTerm);
//					Literal instdB = getLiteral(true, PoIRewritingVocabulary.INSTD, 
//                                                        indiv1Term, negatedTerm, contextTerm);
//					Literal prec = getLiteral(true, PoIRewritingVocabulary.PREC, 
//							                          contextTerm, globalTerm);
//					addRule(ovrsubs, instdA, instdB, prec);
//			    }
//
//			//if (superClass.getClassExpressionType() == ClassExpressionType.OBJECT_UNION_OF) {
//			//	throw new IllegalStateException();
//			//}
//			//else if (superClass.getClassExpressionType() == ClassExpressionType.OBJECT_INTERSECTION_OF) {
//			//	throw new IllegalStateException();
//			//}
//			//else if (superClass.getClassExpressionType() == ClassExpressionType.OBJECT_SOME_VALUES_FROM) {
//			//} else if (superClass.getClassExpressionType() == ClassExpressionType.OBJECT_ONE_OF) {
//			//} else if (superClass.getClassExpressionType() == ClassExpressionType.OBJECT_MIN_CARDINALITY) {  
//			//} else if (superClass.getClassExpressionType() == ClassExpressionType.OBJECT_HAS_SELF) {
//
//			} else {
//				throw new IllegalStateException();
//			}
//		}
//		
//		super.visit(axiom);
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
	public void visit(OWLNamedIndividual arg0) {
	}

	@Override
	public void visit(OWLDatatype arg0) {
	}

	@Override
	public void visit(OWLAnnotationProperty arg0) {
	}
	
	
	//--- ADD FACT METHODS -------------------------------------------------------

	/**
	 * Adds a fact to the translated program given a predicate and 
	 * a list of parameter IRIs.
	 * 
	 * @param predicate predicate of fact.
	 * @param params list of IRI for parameters of fact.
	 */
	protected void addFact(NormalPredicate predicate, IRI... params) {

		List<Term> terms = new ArrayList<>();

		for (IRI param : params) {
			terms.add(CacheManager.getInstance().getConstant(param));
		}

		datalog.add(
				new Clause(new Literal[] { new Literal(predicate, terms
                        .toArray(new Term[terms.size()])) }, new Literal[] {}));
	}
	
	//--- ADD RULE METHOD -------------------------------------------------------

	/**
	 * Adds a rule to the translated program.
	 * 
	 */
	protected void addRule(Literal head, Literal... body) {		
		datalog.add( new Clause(new Literal[] {head},  body) );	  
	}

	/**
	 * Provides a literal with the given predicate and terms
	 * 
	 * @param positive indicates whether output literal is positive or negative.
	 */	
	protected Literal getLiteral(boolean positive, NormalPredicate predicate, Term... params) {		
		Literal literal = new Literal(predicate, params);
		
		if(!positive) literal.setNegative(true);
		
		return literal;
	}	

	/**
	 * Provides a constant representing the given IRI.
	 */	
	protected Constant getConstant(IRI i) {
		return CacheManager.getInstance().getConstant(i);
	}

	/**
	 * Provides a variable identified from the given string.
	 */	
	protected Variable getVariable(String s) {
		return CacheManager.getInstance().getVariable(s);
	}
	
}
//=======================================================================