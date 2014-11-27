package tools;




/**
 * This class is used to store various constant used in the HomeNL.
 * 
 * @author lmrojas
 *
 *<!--
 *   Modifications History
 *   data            author      description
 *   Ene 11 2006     lmrojas      creation.
 *   
 *                  
 * -->
 */
 public class CNConstants {
    //properties file
    public static String PROPERTIES_FILE="etc/ner.properties";
    
    public static String PARSEDIRPROP="parseDir";
    public static String DEFPARSEDIR="parse";
    //TREETAGGER directory
    public static String TAGGERDIRPROP="treetagger";
    public static String DEFTAGGERDIR="tagger";

    //Stanford Linear Classifier
    public static String LISTLCTRAINPROP="listLCTrain";
    public static String DEFLISTTRAIN="esterTrainALL.xmll";
    public static String LISTLCTESTPROP="listLCTest";
    public static String DEFLISTTEST="esterTestALL.xmll";
    public static String LCPROPS="lcProps";
    public static String DEFLCPROPS="etc/slinearclassifier.props";
    public static String NTHREADSPROP="nthreads";
    public static String DEFNTHREADS="1";
    //StanfordCRF
    public static String LISTCRFTRAINPROP="listCRFTrain";
    public static String LISTCRFTESTPROP="listCRFTest";
    public static String CRFPROPS="crfProps";
    public static String DEFCRFPROPS="etc/scrf.props";
    public static String XMXSTANDFORD="XmxStanford";
    //SVMLight
    public static String LISTSVMTRAINPROP="listSVMTrain";
    public static String DEFLISTSVMTRAIN="esterParseTrainALL.xmll";
    public static String LISTSVMTESTPROP="listSVMTest";
    public static String DEFLISTSVMTEST="esterParseTestALL.xmll";
    //ASR Output
    public static String LISTASRDEVPROP="ListASRDev";
    public static String DEFLISTASRDEV="esterRecoDev.xmll";
    public static String LISTASRTESTPROP="ListASRTest";
    public static String DEFLISTASRTEST="esterRecoTest.xmll";
    //Capitalization
    public static String CAPPROPS="CAPProps";    
    public static String DEFCAPPROPS="etc/capitalization.props"; 
    //Wikipedia
    public static String WIKIDBHOSTPROP="wikiDBHost";
    public static String DEFWIKIDBHOST="localhost";
    public static String WIKIDBPORTPROP="wikiDBPort";
    public static String DEFWIKIDBPORT="3306";
    public static String WIKIDBINSTANCEPROP="wikiDBInstance";
    public static String DEFWIKIDBINSTANCE="wikidb";
    public static String WIKIDBUSERPROP="wikiDBUser";
    public static String DEFWIKIDBUSER="contnomina";
    public static String WIKIDBPASSPROP="wikiDBPassword";    
    public static String DEFWIKIDBPASS="contnomina";  
    
    //CoNLL03 config
    public static String CORPUSTRAINPROP="corpusTrain";
    public static String CORPUSDEVPROP="corpusDev";
    public static String CORPUSTESTPROP="corpusTest";
    public static String CORPUSDIRPROP="corpusDir";   
    public static String CORPUSTRAINOPENNLPPROP="corpusTrainOpenNLP";
    public static String CORPUSDEVOPENNLPPROP="corpusDevOpenNLP";
    public static String CORPUSTESTOPENNLPPROP="corpusTestOpenNLP";
    
    //Gigaword config
    public static String CORPUSGIGATRAINPROP="corpusGigaTrain";
    public static String CORPUSGIGADIRPROP="corpusGigaDir";   
    public static String CORPUSGIGAWORDPROP="corpusGigaword";   
    
    //RI
    public static String WVDIR="wvdir";
    public static String DEFWVDIR="wordvects";
    
    
    //General Constants
    public static final String  CHAR_NULL = "--";
    public static final int INT_NULL=-1;
    public static String PRNOUN="pn"; 
    public static String PERS="pers";
    public static String ORG="org"; 
    public static String LOC="loc"; 
    public static String PROD="prod";
    public static String NOCLASS="NO";
    public static String OUTCLASS="O";
    public static String ALL="all"; 
    public static String BIO="BIO";
    public static String BILOU="BILOU";
    
    public static String GAUSSIAN="G";
    public static String BETA="B";
    public static String UNIFORM="U";
    
    public static String POSTAGNAM="NAM";
    
    public static   String HIBERNATE_CONFIG_FILE="hibernate.cfg.xml";
    public static String XMLPERS="pers.hum";
    public static String UTF8_ENCODING="UTF8";
    public static String PARSEDIR="parse";
    public static String CONLLPARSEDIR="parse/train/conll";
    public static String CONLLEXT=".out.conll";
    
    //features
    public static String CURRWORD="CW_";
    public static int    tree_level_threshold=8;
    public static String RECOEXT=".utf8";
   
    //Tree tagger configuration
    public static final String TAGGER_REL="postag";
    public static final String TREETAGGERPAR="/lib/french-utf8.par";
    public static final String TREETAGGERBIN="/lib/french-par-linux-3.2-utf8.bin";    
    
    //StanfodNER class
    public static String SNERJAR="lib/StanfordNER.jar";
    
    //autotest oracle
    public static String AUTOTESTORACLE="autotest_oracle";
    public static String AUTOTESTPNORACLE="autotest_pnoracle";
}
