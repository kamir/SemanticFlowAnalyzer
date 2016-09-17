package com.mycompany.semanticflowanalyser;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

public class AppFILES2Cluster {

    static boolean doIndex = true;

    public static void main(String[] args) throws Exception {

        /**
         * Start scan in this folder
         */
        String BASE = "/Users";

        /**
         * Use this collection name. The name will be added to the ID of each
         * document.
         */
//        String COLLECTION = "iMac_files_001";
        String COLLECTION = "MacPro2_files_001";

        /**
         * Where is the cluster?
         */
        String ZK = "192.168.3.171:22181,192.168.3.170:22181,192.168.3.240:22181/solr";

        System.setProperty("java.security.auth.login.config", "jaas-client.conf");
        HttpClientUtil.setConfigurer(new Krb5HttpClientConfigurer());

        //CloudSolrServer solr = new CloudSolrServer( ZK );
        //solr.setDefaultCollection(SYSTEM);
        SolrServer solr = new LBHttpSolrServer("http://master1:8983/solr/" + COLLECTION, "http://master2:8983/solr/" + COLLECTION);

        Properties props = System.getProperties();

        long t0 = 0;

        File docs = null;

        final JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(BASE));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnVal = fc.showOpenDialog(new JFrame("File Indexer V1.0"));
        docs = fc.getSelectedFile();

        File[] groups = docs.listFiles();

        System.out.println(docs.getAbsolutePath() + " contains " + groups.length + " items.");

        for (File g : groups) {
            System.out.println("***\n" + g.getAbsolutePath() + " -> (hidden:" + g.isHidden() + ", dir:" + g.isDirectory() + ")\n***");
        }

        t0 = System.currentTimeMillis();

        String ID = COLLECTION;

        for (File g : groups) {

            if (g.isHidden()) {

            } else if (g.isFile()) {

                System.out.println(">>> READ file properites from: " + g.getAbsolutePath());

                processFile(g, solr, ID);

            } else {

                System.out.println(">>> READ folder properites from: " + g.getAbsolutePath());

                processFolder(g, solr, ID);

            }
        }

        long t1 = System.currentTimeMillis();

        System.out.println(((t1 - t0) / 1000) + " seconds.");

        System.out.println("Nr of files:" + zf);
        System.out.println("Nr of errors:" + ec);

        System.exit(0);
    }

    static int zf = 0;
    static int ec = 0;

    static void processFile(File f, SolrServer solr, String ID) throws IOException, NoSuchAlgorithmException, SAXException, TikaException {

        zf++;

        // Based on Java Mail API
        displayMD(f);

        // TIKA PART ...
        // 
        MessageDigest md = MessageDigest.getInstance("MD5");

        try {

            InputStream is = Files.newInputStream(Paths.get(f.getAbsolutePath()));

            DigestInputStream dis = new DigestInputStream(is, md);
            /* Read decorated stream (dis) to EOF as normal... */
            SolrInputDocument doc = _parseAutoMode(dis);

            doc.addField("id", ID + "_" + f.getAbsolutePath());

            doc.addField("fn", f.getName());
            doc.addField("fqn", f.getAbsolutePath());
            doc.addField("parent", f.getParentFile().getAbsolutePath());

            byte[] digest = md.digest();
            String md5 = new String(digest);

            is.close();

            doc.addField("md5content", md5);
            doc.addField("size", f.length());

            solr.add(doc);

            if (zf % 100 == 0) {
                solr.commit();
            }

        } catch (Exception ex) {

            ex.printStackTrace();

            ec++;

        }

    }

    static void processFolder(File gr, SolrServer solr, String ID) throws IOException, NoSuchAlgorithmException, SAXException, TikaException, SolrServerException {

        if (gr == null) {
            return;
        }
        File[] list = gr.listFiles();

        System.out.println(gr);
        System.out.println(solr);
        System.out.println(ID);
        System.out.println(list);

        if (list == null) {
            return;
        }

        for (File g : list) {

            if (!g.isHidden() && !g.isDirectory()) {

                System.out.println(">>> READ file properites from: " + g.getAbsolutePath());

                processFile(g, solr, ID);

            } else {

                System.out.println(">>> READ folder properites from: " + g.getAbsolutePath());

                processFolder(g, solr, ID);

            }
        }

        solr.commit();
    }

    static private void displayMD(File f) {

        System.out.println(f.getName() + " " + f.getAbsolutePath() + " " + f.length() + " " + f.lastModified());

    }

    public static SolrInputDocument _parseAutoMode(InputStream stream) throws IOException, SAXException, TikaException {

        AutoDetectParser parser = new AutoDetectParser();

        BodyContentHandler handler = new BodyContentHandler();

        Metadata metadata = new Metadata();

        String parserError = "";

        try {

            parser.parse(stream, handler, metadata);

        } catch (Exception ex) {

            parserError = ex.getMessage();

        }

        parser = null;

        SolrInputDocument doc = new SolrInputDocument();

        String[] NAMES = metadata.names();
        for (String n : NAMES) {

//            doc.addField(n, metadata.get(n));
            System.out.println("===============>" + n + " => " + metadata.get(n));

        }

        return doc;

    }
}
