package eu.fbk.dkm.aspit.rewriter;

import org.semanticweb.drew.dlprogram.model.CacheManager;
import org.semanticweb.drew.dlprogram.model.NormalPredicate;

/**
 * @author Loris
 * @version 0.1
 * 
 * Static set of predicate names used in the rewriting process.
 */
public class PoIRewritingVocabulary {
	
	private static CacheManager manager = CacheManager.getInstance();
	
	//--- VOCABULARY PREDICATES ---------------------------------------------------------
	
	//public final static NormalPredicate NOM  = manager.getPredicate("nom", 1);
	//public final static NormalPredicate CLS  = manager.getPredicate("cls", 1);
	//public final static NormalPredicate ROLE = manager.getPredicate("rol", 1);

	//--- INSTANCE LEVEL PREDICATES -----------------------------------------------------
	
	public final static NormalPredicate INST = manager.getPredicate("inst", 3);		
	public final static NormalPredicate REL  = manager.getPredicate("rel", 4);
			
	//--- CLASS AXIOMS PREDICATES -------------------------------------------------------
		
	//--- PROPERTY AXIOMS PREDICATES ----------------------------------------------------
			
}
//=======================================================================