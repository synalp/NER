package tools;


import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * This class encapsulates the configuration data and properties,
 * loaded directly from a properties file. 
 *  
 * @author rojasbar
 *
 *  
 *<!--
 *   Modifications History
 *   data            author      description
 *   10-Oct-2014     rojasbar      creation.  
 *                  
 * -->
 */
 
public class GeneralConfig {
	
	      
    
    // properties
    public static String parseDir;

    //TREETAGGER directory
    public static String treetaggerDir;

    //Stanford Linear Classifier
    public static String listLCTrain;
    public static String listLCTest;
    public static String lcProps;
    public static int    nthreads=CNConstants.INT_NULL;
    //StanfordCRF
    public static String listCRFTrain;
    public static String listCRFTest;
    public static String crfProps;

    //SVMLight
    public static String listSVMTrain;
    public static String listSVMTest;

    //Wikipedia
    public static String wikiDBHost;
    public static String wikiDBPort;
    public static String wikiDBInstance;
    public static String wikiDBUser;
    public static String wikiDBPassword;

    //ASR output
    public static String listASRDev;
    public static String listASRTest;
    public static String capProps;
    
    //CoNLL03
    public static String corpusTrain;
    public static String corpusDev;
    public static String corpusTest;
    public static String corpusDir;   
	   

	/**
	 * This method reads the properties files located in etc/ner.properties and
	 * loads the properties to memory
	 */
	public static void loadProperties() {
	
		
	   Properties properties = new Properties();

	   try {
		   properties.load(new FileInputStream(CNConstants.PROPERTIES_FILE));
	           
	   } catch (FileNotFoundException e) {
	           // DO Nothing: empty property list is ok
	   } catch (IOException e) {
	           
	   } 
           // loads the different properties
           // loads the directory with the semantizer files

           // loads the directory with the files parsed with the MATE parser 
           parseDir = properties.getProperty(CNConstants.PARSEDIRPROP,
                                            CNConstants.DEFPARSEDIR);

           //loads the tree tagger directory
           treetaggerDir = properties.getProperty(CNConstants.TAGGERDIRPROP, CNConstants.DEFTAGGERDIR);
           //Stanford Linear Classifier
           listLCTrain = properties.getProperty(CNConstants.LISTLCTRAINPROP, CNConstants.DEFLISTTRAIN);
           listLCTest= properties.getProperty(CNConstants.LISTLCTRAINPROP, CNConstants.DEFLISTTEST);
           lcProps= properties.getProperty(CNConstants.LCPROPS, CNConstants.DEFLCPROPS);
           nthreads=Integer.parseInt(properties.getProperty(CNConstants.NTHREADSPROP, CNConstants.DEFNTHREADS));
           //Stanford CRF
           listCRFTrain= properties.getProperty(CNConstants.LISTCRFTRAINPROP, CNConstants.DEFLISTTRAIN);
           listCRFTest= properties.getProperty(CNConstants.LISTCRFTESTPROP, CNConstants.DEFLISTTEST);
           crfProps=properties.getProperty(CNConstants.CRFPROPS, CNConstants.DEFCRFPROPS);
           //SVMLight
           listSVMTrain = properties.getProperty(CNConstants.LISTSVMTRAINPROP, CNConstants.DEFLISTSVMTRAIN);
           listSVMTest = properties.getProperty(CNConstants.LISTSVMTESTPROP, CNConstants.DEFLISTSVMTEST);
           //Wiki database
           wikiDBHost= properties.getProperty(CNConstants.WIKIDBHOSTPROP, CNConstants.DEFWIKIDBHOST);
           wikiDBPort=properties.getProperty(CNConstants.WIKIDBPORTPROP, CNConstants.DEFWIKIDBPORT);
           wikiDBInstance=properties.getProperty(CNConstants.WIKIDBINSTANCEPROP, CNConstants.DEFWIKIDBINSTANCE);
           wikiDBUser=properties.getProperty(CNConstants.WIKIDBUSERPROP, CNConstants.DEFWIKIDBUSER);
           wikiDBPassword= properties.getProperty(CNConstants.WIKIDBPASSPROP, CNConstants.DEFWIKIDBPASS);
           
           //ASR output
           listASRDev=properties.getProperty(CNConstants.LISTASRDEVPROP,CNConstants.DEFLISTASRDEV);
           listASRTest=properties.getProperty(CNConstants.LISTASRTESTPROP,CNConstants.DEFLISTASRTEST);
           capProps=properties.getProperty(CNConstants.CAPPROPS,CNConstants.DEFCAPPROPS);      
           
           //CoNLL03 config
           corpusTrain=properties.getProperty(CNConstants.CORPUSTRAINPROP);
           corpusDev=properties.getProperty(CNConstants.CORPUSDEVPROP);
           corpusTest=properties.getProperty(CNConstants.CORPUSTESTPROP);
           corpusDir=properties.getProperty(CNConstants.CORPUSDIRPROP);
	       
	}


}
