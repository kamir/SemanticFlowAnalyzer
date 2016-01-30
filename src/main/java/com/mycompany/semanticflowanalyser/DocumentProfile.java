/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.semanticflowanalyser;

import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Terms;

/**
 *
 * @author kamir
 */
public class DocumentProfile {
    
    public Document doc;
    
    public String rawText;
    
    public Terms terms;
    
    public Map<String, Integer> frequencies;
    
}
