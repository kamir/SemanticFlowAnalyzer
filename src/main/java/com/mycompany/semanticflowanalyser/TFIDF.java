
package com.mycompany.semanticflowanalyser;

import java.io.IOException;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author kamir
 */
public class TFIDF {
  
  static float tf = 1;
  static float idf = 0;
  private float tfidf_score;
     
  static float[] tfidf = null;
 
   
 public void scoreCalculator(IndexReader reader,String field,String term) throws IOException 
     { 
         
         System.out.println("### field: " + field );
         
         /** GET TERM FREQUENCY & IDF **/
         TFIDFSimilarity tfidfSIM = new DefaultSimilarity();
         
         Bits liveDocs = MultiFields.getLiveDocs(reader);
         
         
         TermsEnum termEnum = MultiFields.getTerms(reader, field).iterator(null);
         BytesRef bytesRef;
         while ((bytesRef = termEnum.next()) != null) 
         {           
           if(bytesRef.utf8ToString().trim() == term.trim())
           {                  
              if (termEnum.seekExact(bytesRef)) 
              {
                 idf = tfidfSIM.idf(termEnum.docFreq(), reader.numDocs());
                 DocsEnum docsEnum = termEnum.docs(liveDocs, null);
                 if (docsEnum != null) 
                 {
                    int doc; 
                    while((doc = docsEnum.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) 
                     {
                         tf = tfidfSIM.tf(docsEnum.freq());
                         tfidf_score = tf*idf; 
                         System.out.println(" -tfidf_score- " + tfidf_score);
                     }
                 } 
             } 
           }
        } 
     }
   
}