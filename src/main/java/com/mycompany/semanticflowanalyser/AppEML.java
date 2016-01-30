package com.mycompany.semanticflowanalyser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.ScoreDoc;
import java.io.IOException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.apache.lucene.document.Document;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.MultiReader;
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
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

/**
 * Simple Lucene based text Analysis for emails.
 *
 * A set of emails is grouped by a manual selected and assigned category in a
 * folder. We read all mails from one folder to index the mails for similarity
 * analysis and Spark topic model analysis.
 *
 * https://janav.wordpress.com/2013/10/27/tf-idf-and-cosine-similarity/
 *
 * 
 * This application wirks also as an "Morphline debugger and optimizer".
 */
public class AppEML {

    static boolean doIndex = true;

    static Session mailSession = null;

    public static void main(String[] args) throws IOException, ParseException, SAXException, TikaException, Exception {

        Properties props = System.getProperties();
        props.put("mail.host", "smtp.dummydomain.com");
        props.put("mail.transport.protocol", "smtp");

        mailSession = Session.getDefaultInstance(props, null);

        long t0 = 0;
        
        /**
         * Mails folder contains several groups of mails, for which individual
         * indexes will be created.
         */
        File docs = new File("./docs/mails/");
        
        /** Select a folder with mails **/
        String uh = System.getProperty("user.home");
        final JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory( new File( uh ) );
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int returnVal = fc.showOpenDialog( new JFrame("EMail-Versteher 1.0") );
        docs = fc.getSelectedFile();
        
        // for all groups ...
        File[] groups = docs.listFiles();
        System.out.println(docs.getAbsolutePath() + " contains " + groups.length + " items.");
        for (File g : groups) {
            System.out.println( g.getAbsolutePath() + " -> " + g.isHidden() + "," + g.isDirectory() );
        }
        
        t0 = System.currentTimeMillis();
        
        for (File g : groups) {

            if (!g.isHidden() && g.isDirectory() ) {

                String topic = g.getName();

                System.out.println(">>> READ eml-Files from: " + g.getAbsolutePath());

                File indexFolder = new File("./index/mails/" + topic + "/index-directory");

                if (doIndex) {

//                    Directory indexDir = FSDirectory.open(indexFolder);
//                    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_2, new SimpleAnalyzer());
//                    IndexWriter indexWriter = new IndexWriter(indexDir, config);

                    File[] doclist = g.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File fn) {
                            System.out.println(fn.getAbsolutePath());
                            if (fn.getName().endsWith("eml")) {
                                return true;
                            } else {
                                return false;
                            }
                        }
                    });

                    if (doclist == null) {
                        break;
                    }

                    int fz = 0;
                    for (File eml : doclist) {

                        fz++;
                        System.out.println("( " + fz + " ) " + eml.getAbsolutePath());

                        // Based on Java Mail API
                        display(eml);

                        // TIKA PART ...
                        // 
//                        Document doc = parseAutoMode(new FileInputStream(eml));

//////                        
//////                        BufferedReader br = new BufferedReader(new FileReader(d));
//////
//////                        while (br.ready()) {
//////                            sb.append(br.readLine());
//////                        }


//                    indexWriter.addDocument(doc);

                    }

//                    indexWriter.close();

                }
                // }

//////////                System.out.println();
//////////
//////////                System.out.println(">>> " + indexFolder.toString());
//////////
//////////                SearchEngine se = new SearchEngine(indexFolder);
//////////
//////////                try {
//////////
//////////                    // TopDocs topDocs = se.performSearch("*:*", 100); 
//////////                    TopDocs topDocs = se.performSearch("*:*", 100);
//////////
//////////                    // obtain the ScoreDoc (= documentID, relevanceScore) array from topDocs
//////////                    ScoreDoc[] hits = topDocs.scoreDocs;
//////////
//////////                    Vector<Document> docsV = new Vector<Document>();
//////////
//////////                    // retrieve each matching document from the ScoreDoc arry
//////////                    for (int i = 0; i < hits.length; i++) {
//////////                        Document doc = se.getDocument(hits[i].doc);
//////////                        String id = doc.get("id");
//////////                        System.out.println(id + " " + hits[i].toString());
//////////                        docsV.add(doc);
//////////
//////////                    }
//////////
//////////
//////////
//////////                } catch (Exception ex) {
//////////                    ex.printStackTrace();
//////////                }
//////////
//////////
//////////                SimpleFSDirectory directory = new SimpleFSDirectory(indexFolder);
//////////                DirectoryReader directoryReader = DirectoryReader.open(directory);
//////////
//////////                MultiReader mr = new MultiReader(directoryReader);
//////////
//////////                TFIDFSimilarity tfidfSIM = new DefaultSimilarity();
//////////
//////////                Map<String, Float> tf_Idf_Weights = new HashMap<>();
//////////                Map<String, Float> termFrequencies = new HashMap<>();
//////////
//////////                DefaultSimilarity similarity = new DefaultSimilarity();
//////////                int docnum = mr.numDocs();
//////////
//////////                System.out.println("### " + indexFolder + " (#docs=" + docnum + ")");
//////////
//////////
//////////                IndexSearcher searcher = new IndexSearcher(directoryReader);
//////////                TopScoreDocCollector collector = TopScoreDocCollector.create(10, true);
//////////
//////////                QueryParser parser = new QueryParser("text", new StandardAnalyzer());
//////////                Query query = parser.parse("*:*");
//////////                searcher.search(query, collector);
//////////                ScoreDoc[] hits = collector.topDocs().scoreDocs;
//////////
//////////                // 4. Display results
//////////
//////////                System.out.println("Found : " + hits.length + " hits.");
//////////
//////////                System.out.println("# \t id \t tokens \t text");
//////////
//////////                Vector<DocumentProfile> docsPro = new Vector<DocumentProfile>();
//////////
//////////
//////////                for (int i = 0; i < hits.length; i++) {
//////////                    int docID = hits[i].doc;
//////////
//////////
//////////                    Document d = searcher.doc(docID);
//////////
//////////                    DocumentProfile prof = new DocumentProfile();
//////////                    prof.doc = d;
//////////
//////////                    System.out.println((i + 1) + ". " + d.get("id") + "\t" + d.get("tokens") + "\t" + d.get("text") + "\t");
//////////
//////////                    Terms vector = directoryReader.getTermVector(docID, "text");
//////////                    prof.terms = vector;
//////////
//////////                    Vector<String> terms = new Vector<String>();
//////////
//////////                    TermsEnum termsEnum = vector.iterator(TermsEnum.EMPTY);
//////////
//////////                    Map<String, Integer> frequencies = new HashMap<>();
//////////                    BytesRef text = null;
//////////                    while ((text = termsEnum.next()) != null) {
//////////                        String term = text.utf8ToString();
//////////                        int freq = (int) termsEnum.totalTermFreq();
//////////                        frequencies.put(term, freq);
//////////                        terms.add(term);
//////////                    }
//////////
//////////                    prof.frequencies = frequencies;
//////////
//////////                    prof.rawText = d.get("text");
//////////
//////////                    docsPro.add(prof);
//////////
//////////                    System.out.println("# => " + terms.size());
//////////                }






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

            } // ONLY NON HIDDEN FILES 
        } // END PROCESS GROUP

        long t1 = System.currentTimeMillis();
        
        System.out.println( (( t1 - t0 )/ 1000) + " seconds." );
    }

    public static void display(File emlFile) throws Exception {
        
        InputStream source = new FileInputStream(emlFile);
        MimeMessage message = new MimeMessage(mailSession, source);

        Object msgContent = message.getContent();

        String content = "\n*\n*\n*";

        /* Check if content is pure text/html or in parts */
        if (msgContent instanceof Multipart) {

            Multipart multipart = (Multipart) msgContent;
            
            System.out.println(">>> Mail has " + multipart.getCount() + " parts.");
 
            for (int j = 0; j < multipart.getCount(); j++) {

                BodyPart bodyPart = multipart.getBodyPart(j);
                content = "Mail has " + multipart.getCount() + " parts.";
                
//                String disposition = bodyPart.getDisposition();

//                if (disposition != null && (disposition.equalsIgnoreCase("ATTACHMENT"))) {
                
                    System.out.println(">>> Handle Mail-attachment");

//                    DataHandler handler = bodyPart.getDataHandler();
//                    System.out.println("file name : " + handler.getName());
//                    
//                    BufferedInputStream bins = new BufferedInputStream( handler.getInputStream() );
//                    BufferedReader br = new BufferedReader( new InputStreamReader( bins ));
//                    String l = null;
//                    while( br.ready() ) {
//                        l = br.readLine();
//                        content =content+l;
//                    }
//                    content = content + "\n\n\n";
//
//                    br.close();
                
//                } 
//                else {
//                    content = content + "\n\n\n\n" + message.getContent().toString();
//                }
            }
        } 
        else {
            content = message.getContent().toString();
        }

        content = content + "\n*\n*\n*";


        System.out.println("\nFrom        : " + message.getFrom()[0]);
        System.out.println("Date          : " + message.getSentDate());
        System.out.println("ID            : " + message.getMessageID());
        System.out.println("MD5           : " + message.getContentMD5());
        System.out.println("Content-Type  : " + message.getContentType());
        
        System.out.println("Encoding      : " + message.getEncoding());
        System.out.println("--------------");
        System.out.println("Subject       : " + message.getSubject());
        System.out.println("--------------");
        System.out.println("Body          : " + content );
        System.out.println("==============\n");

    }

    public static Document parseAutoMode(InputStream stream) throws IOException, SAXException, TikaException {

        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();

        Metadata metadata = new Metadata();

        parser.parse(stream, handler, metadata);

        String[] NAMES = metadata.names();
        for (String n : NAMES) {
            System.out.println(n + " => " + metadata.get(n));
        }



        Document doc = new Document();

//                        // field 1
//                        doc.add(new StringField("id", d.getName(), Field.Store.YES));
//
//                        // field 2
//                        final FieldType bodyOptions = new FieldType();
//                        bodyOptions.setIndexed(true);
//                        bodyOptions.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
//                        bodyOptions.setStored(true);
//                        bodyOptions.setStoreTermVectors(true);
//                        bodyOptions.setTokenized(true);
//                        doc.add(new Field("text", sb.toString(), bodyOptions));
//
//                        // field 3
//                        TextField tf = new TextField("tokens", sb.toString().split(" ").length + "", Field.Store.YES);
//                        doc.add(tf);




        return doc;

    }
}
