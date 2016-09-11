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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
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
public class AppFILES {

    static boolean doIndex = true;

    public static void main(String[] args) throws Exception {

        Properties props = System.getProperties();

        long t0 = 0;

        /**
         * Mails folder contains several groups of mails, for which individual
         * indexes will be created.
         */
        File docs = null;

        /**
         * Select a folder with mails *
         */
//        String BASE = System.getProperty("file.indexer.base");
//        String SYSTEM = System.getProperty("file.indexer.label");
        String BASE = "/users/home";
        String SYSTEM = "iMAC_users_home";
        
        File indexFolder = new File("/Volumes/iMAC_index/" + SYSTEM + "/index-directory/");
        indexFolder.mkdirs();
        
        Directory indexDir = FSDirectory.open(indexFolder);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_2, new StandardAnalyzer());
        IndexWriter indexWriter = new IndexWriter(indexDir, config);

        final JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(BASE));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnVal = fc.showOpenDialog(new JFrame("File Indexer V1.0"));
        docs = fc.getSelectedFile();

        // for all groups ...
        File[] groups = docs.listFiles();

        System.out.println(docs.getAbsolutePath() + " contains " + groups.length + " items.");

        for (File g : groups) {
            System.out.println("***\n" + g.getAbsolutePath() + " -> (hidden:" + g.isHidden() + ", dir:" + g.isDirectory() + ")\n***" );
        }


        t0 = System.currentTimeMillis();

        String ID = SYSTEM;

        for (File g : groups) {

            if ( g.isHidden() ) {
            
            }
            else if ( g.isFile() ) {

                System.out.println(">>> READ file properites from: " + g.getAbsolutePath());

                processFile(g, indexWriter, ID);

            } 
            else {

                System.out.println(">>> READ folder properites from: " + g.getAbsolutePath());

                processFolder(g, indexWriter, ID);

            }
        }
        
        indexWriter.close();
        
        long t1 = System.currentTimeMillis();

        System.out.println(((t1 - t0) / 1000) + " seconds.");

        System.exit(0);
    }

    
    static int zf = 0;
    
    static void processFile(File f, IndexWriter indexWriter, String ID) throws IOException, NoSuchAlgorithmException, SAXException, TikaException {

        
            zf++;
 

           
            // Based on Java Mail API
            displayMD(f);

            // TIKA PART ...
            // 

            MessageDigest md = MessageDigest.getInstance("MD5");
            
            try {
                
                InputStream is = Files.newInputStream(Paths.get( f.getAbsolutePath() ));
                    
                DigestInputStream dis = new DigestInputStream(is, md);
                /* Read decorated stream (dis) to EOF as normal... */
                Document doc = _parseAutoMode( dis );

                doc.add(new StringField("id", ID + "_" + f.getAbsolutePath(), Field.Store.YES));

                doc.add(new StringField("fn", f.getName(), Field.Store.YES));
                doc.add(new StringField("fqn", f.getAbsolutePath(), Field.Store.YES));
                doc.add(new StringField("parent", f.getParentFile().getAbsolutePath(), Field.Store.YES));
 
                byte[] digest = md.digest();
                String md5 = new String(digest);
                is.close();
            
                doc.add(new StringField("md5content", md5, Field.Store.YES));

                indexWriter.addDocument(doc);
                
                if ( zf % 100  == 0 ) indexWriter.commit();

                
            }
            catch(Exception ex) {
            
            ex.printStackTrace(); 
            
            }
            
          
            
            
 
    }

    static void processFolder(File gr, IndexWriter indexWriter, String ID) throws IOException, NoSuchAlgorithmException, SAXException, TikaException {

        if ( gr == null ) return;
        File[] list = gr.listFiles();
        
        System.out.println( gr );
        System.out.println( indexWriter );
        System.out.println( ID );
        System.out.println( list );
        
        if ( list == null ) return;
        
        for (File g : list ) {

            if (!g.isHidden() && !g.isDirectory()) {

                System.out.println(">>> READ file properites from: " + g.getAbsolutePath());

                processFile(g, indexWriter, ID);

            } else {

                System.out.println(">>> READ folder properites from: " + g.getAbsolutePath());

                processFolder(g, indexWriter, ID);


            }
        }
        
        indexWriter.commit();
    }

    static private void displayMD(File f) {

        System.out.println(f.getName() + " " + f.getAbsolutePath() + " " + f.length() + " " + f.lastModified());

    }

    public static Document _parseAutoMode(InputStream stream) throws IOException, SAXException, TikaException {

        AutoDetectParser parser = new AutoDetectParser();

        BodyContentHandler handler = new BodyContentHandler();
        

        Metadata metadata = new Metadata();
        
        String parserError = "";
        
    try {
    
        parser.parse(stream, handler, metadata);
        
    } 
    catch (Exception ex ) {
        
        parserError = ex.getMessage();
        
    }
    
    parser = null;
    
    Document doc = new Document();

        String[] NAMES = metadata.names();
        for (String n : NAMES) {
            
            doc.add(new TextField(n, metadata.get(n), Field.Store.YES));
            
            System.out.println("===============>" + n + " => " + metadata.get(n));
        }

        

                        // field 1
                        doc.add(new TextField("parser_error", parserError, Field.Store.YES));
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
