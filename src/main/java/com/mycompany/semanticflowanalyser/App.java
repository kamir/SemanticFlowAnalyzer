package com.mycompany.semanticflowanalyser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.lucene.document.Document;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.TermStats;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

/**
 * Simple Lucene based text Analysis ...
 *
 * https://janav.wordpress.com/2013/10/27/tf-idf-and-cosine-similarity/
 *
 */
public class App {

    static boolean doIndex = true;

    public static void main(String[] args) throws IOException, ParseException {

        File docs = new File("./docs/exp1");

        File[] groups = docs.listFiles();

        for (File g : groups) {

            File indexFolder = new File("./index/exp1/" + g.getName() + "/index-directory");

            if (doIndex) {

                Directory indexDir = FSDirectory.open(indexFolder);
                IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_2, new SimpleAnalyzer());
                IndexWriter indexWriter = new IndexWriter(indexDir, config);

                File[] doclist = g.listFiles();
                for (File d : doclist) {

                    System.out.println( d.getAbsolutePath() );
                    
                    StringBuffer sb = new StringBuffer();
                    BufferedReader br = new BufferedReader(new FileReader(d));
                    while (br.ready()) {
                        sb.append(br.readLine());
                    }

                    Document doc = new Document();

                    // field 1
                    doc.add(new StringField("id", d.getName(), Field.Store.YES));

                    // field 2
                    final FieldType bodyOptions = new FieldType();
                    bodyOptions.setIndexed(true);
                    bodyOptions.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
                    bodyOptions.setStored(true);
                    bodyOptions.setStoreTermVectors(true);
                    bodyOptions.setTokenized(true);
                    doc.add(new Field("text", sb.toString(), bodyOptions));

                    // field 3
                    TextField tf = new TextField("tokens", sb.toString().split(" ").length + "", Field.Store.YES);
                    doc.add(tf);

                    indexWriter.addDocument(doc);

                }

                indexWriter.close();

            }

            System.out.println();

            System.out.println(">>> " + indexFolder.toString());

            SearchEngine se = new SearchEngine(indexFolder);

            try {

                // TopDocs topDocs = se.performSearch("*:*", 100); 
                TopDocs topDocs = se.performSearch("*:*", 100);

                // obtain the ScoreDoc (= documentID, relevanceScore) array from topDocs
                ScoreDoc[] hits = topDocs.scoreDocs;

                Vector<Document> docsV = new Vector<Document>();

                // retrieve each matching document from the ScoreDoc arry
                for (int i = 0; i < hits.length; i++) {
                    Document doc = se.getDocument(hits[i].doc);
                    String id = doc.get("id");
                    System.out.println(id + " " + hits[i].toString());
                    docsV.add(doc);

                }



            } catch (Exception ex) {
                ex.printStackTrace();
            }


            SimpleFSDirectory directory = new SimpleFSDirectory(indexFolder);
            DirectoryReader directoryReader = DirectoryReader.open(directory);

            MultiReader mr = new MultiReader(directoryReader);

            TFIDFSimilarity tfidfSIM = new DefaultSimilarity();

            Map<String, Float> tf_Idf_Weights = new HashMap<>();
            Map<String, Float> termFrequencies = new HashMap<>();

            DefaultSimilarity similarity = new DefaultSimilarity();
            int docnum = mr.numDocs();

            System.out.println("### " + indexFolder + " (#docs=" + docnum + ")");

            
            IndexSearcher searcher = new IndexSearcher(directoryReader);
            TopScoreDocCollector collector = TopScoreDocCollector.create(10, true);
            
            QueryParser parser = new QueryParser("text", new StandardAnalyzer());
            Query query = parser.parse("*:*");
            searcher.search(query, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            // 4. Display results

            System.out.println("Found : " + hits.length + " hits.");

            System.out.println("# \t id \t tokens \t text");

            Vector<DocumentProfile> docsPro = new Vector<DocumentProfile>();
            
            
            for (int i = 0; i < hits.length; i++) {
                int docID = hits[i].doc;
                
           
                Document d = searcher.doc(docID);
                
                DocumentProfile prof = new DocumentProfile();
                prof.doc = d;
                
                System.out.println((i + 1) + ". " + d.get("id") + "\t" + d.get("tokens") + "\t" + d.get("text") + "\t");

                Terms vector = directoryReader.getTermVector(docID, "text");
                prof.terms = vector;
                
                Vector<String> terms = new Vector<String>();
                
                TermsEnum termsEnum = vector.iterator(TermsEnum.EMPTY);
                
                Map<String, Integer> frequencies = new HashMap<>();
                BytesRef text = null;
                while ((text = termsEnum.next()) != null) {
                    String term = text.utf8ToString();
                    int freq = (int) termsEnum.totalTermFreq();
                    frequencies.put(term, freq);
                    terms.add(term);
                }

                prof.frequencies = frequencies;
                
                prof.rawText = d.get("text");
                
                docsPro.add(prof);
                
                System.out.println( "# => " + terms.size() );
            }

            
            
            


//            Fields fields = MultiFields.getFields(mr);
//            for (String field : fields) {
//
//                if (field.equals("text")) {
//
//                    Terms terms = fields.terms(field);
//                    TermsEnum termsEnum = terms.iterator(null);
////                    while (termsEnum.next() != null) {
////                        double idf = similarity.idf(termsEnum.docFreq(), docnum);
////                        System.out.println("" + field + ":" + termsEnum.term().utf8ToString() + " idf=" + idf);
////                    }
//
//
//                    while (termsEnum.next() != null) {
//                        // double idf = similarity.idf(termsEnum.docFreq(), docnum);
//                        double idf = Math.log(docnum / termsEnum.docFreq()); // idf = log(D/dt)
//                        System.out.println("" + field + ":" + termsEnum.term().utf8ToString() + " docFreq=" + termsEnum.docFreq() + " idf=" + idf);
//                    }
//                }
//
//            }
        }
    }
}
