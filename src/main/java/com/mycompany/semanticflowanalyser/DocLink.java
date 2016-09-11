/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.semanticflowanalyser;

import java.util.Vector;

/**
 *
 * @author kamir
 */
public class DocLink {
    
    DocumentProfile d1 = null;
    DocumentProfile d2 = null;
    
    // cosinus of angle between the termvectors
    double angle = 0.0;
    
    // list of all terms in both documents ...
    Vector<TermLinkContribution> overlappingTerms = null;
    
    public DocLink(DocumentProfile _d1, DocumentProfile _d2) {
        d1 = _d1;
        d2 = _d2;
        
    }
    
}
