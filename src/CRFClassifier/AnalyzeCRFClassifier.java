/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package CRFClassifier;

import edu.stanford.nlp.ie.NERFeatureFactory;
import linearclassifier.*;

import java.util.List;
import edu.stanford.nlp.ie.crf.CRFClassifier;


import edu.stanford.nlp.io.IOUtils;


import edu.stanford.nlp.objectbank.ObjectBank;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.Triple;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;
import java.util.Properties;


import jsafran.DetGraph;
import jsafran.GraphIO;
import tools.CNConstants;

import tools.GeneralConfig;
import tools.Histoplot;
import utils.ErrorsReporting;


/**
 * This class process the instances and features by instance in the Stanford linear classifier 
 * @author rojasbar
 */
public class AnalyzeCRFClassifier {
    
    public static String MODELFILE="en.%S.crf.mods";
    public static String TRAINFILE="groups.%S.tab.crf.train";
    public static String TESTFILE="groups.%S.tab.crf.test";
    public static String LISTTRAINFILES="esterTrainALL.xmll";
    public static String LISTTESTFILES="esterTestALL.xmll";
    public static String UTF8_ENCODING="UTF8";
    public static String PROPERTIES_FILE="scrf.props";
    public static String NUMFEATSINTRAINFILE="2-";
    public static String ONLYONEPNOUNCLASS=CNConstants.PRNOUN;
    public static String[] groupsOfNE = {CNConstants.PERS,CNConstants.ORG, CNConstants.LOC, CNConstants.PROD};
    public static int TRAINSIZE=Integer.MAX_VALUE;
    
    public static String OUTFILE="analysis/CRF/test.%S.log";
    public static boolean POSFILTER=true;
    
    
    private HashMap<String, CRFClassifier> modelMap = new HashMap<>();
    private HashMap<String,MarginCRF> marginMAP = new HashMap<>();
    private int numInstances=0;
    private String typeofClass="I0";  //possible values "IO","BIO","BILOU";
    private HashMap<String, List<List<Integer>>> featInstMap = new HashMap<>();
    
    private HashMap<String, List<Integer>> lblInstMap = new HashMap<>();
    
    public AnalyzeCRFClassifier(){
        GeneralConfig.loadProperties();
        LISTTRAINFILES=GeneralConfig.listCRFTrain;
        LISTTESTFILES=GeneralConfig.listCRFTest;
        PROPERTIES_FILE=GeneralConfig.crfProps;
    }

    public void setTypeOfClass(String type){
        this.typeofClass=type;
    }
    
    /**
     * Return the features per instance associated at one classifier
     * @param classifier
     * @param instance
     * @return 
     */
    public List<Integer> getFeaturesPerInstance(String classifier, int instance){
        if(featInstMap.containsKey(classifier)){
            return featInstMap.get(classifier).get(instance);
        }else
            return new ArrayList<>();
        
    }
    
    public Integer getLabelsPerInstance(String classifier, int instance){
        if(lblInstMap.containsKey(classifier)){
            return lblInstMap.get(classifier).get(instance);
        }else
            return -1;
        
    }    
    
    
       /**
     * Get the instances, the features and class by instance
     * @param fileName
     * @param model 
     */
    public void getValues(String fileName, CRFClassifier model, List<List<Integer>> featsperInst,List<Integer> labelperInst){
        
        //BufferedReader inFile = null;
        try {
//            inFile = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), UTF8_ENCODING));
            
            Properties props = new Properties();
            props.load(new BufferedReader(new FileReader(PROPERTIES_FILE)));

            CRFClassifier crfClass = new CRFClassifier(props);

            ObjectBank<List> objblank = crfClass.makeObjectBankFromFile(fileName);
            ArrayList<List> docs = new ArrayList(objblank);
            List insts = docs.get(0);
            Triple<int[][][], int[], double[][][]> vals = model.documentToDataAndLabels(insts);

            numInstances=0;
            int[][][] data = vals.first();
            int[] labels= vals.second();
           
            
           
            for(int i=0; i< labels.length; i++){
                List<Integer> feats = new ArrayList<>();
                //chanin either the features of the input data at a given position
                // or the features of the label given the previous one -- linear chain
                for(int chain=0; chain<data[i].length;chain++){
                    
                   int[] features = data[i][chain];
                    
                    //take the id (index) of the features
                    for(int f=0;f<features.length;f++){
                        feats.add(features[f]);
                    }
                   
                    
                    
                }
                labelperInst.add(labels[i]);
                featsperInst.add(feats);
                numInstances++;                 
            }
//            for (;;) {
//                String line = inFile.readLine();
//                if (line==null) break;
//
//                CRFDatum<String, String> datum = crfClass.makeDatum(docs, numInstances, crfClass.featureFactory);            
//                Collection<String> features = datum.asFeatures();
//                List<Integer> feats = new ArrayList<>();
//                //take the id (index) of the features
//                for(String f:features){
//                    if(model.featureIndex.indexOf(f)>-1)
//                        feats.add(model.featureIndex().indexOf(f));
//                }
//                //System.out.println("feats[:"+numInstances+"]="+feats);
//                featsperInst.add(feats);
//                //take the id (index) of the labels
//                String label = line.substring(0, line.indexOf("\t"));
//                int labelId = model.labelIndex().indexOf(label);
//                labelperInst.add(labelId);
//                numInstances++;    
//                
//            }
//           inFile.close();
//           
        } catch (Exception ex) {
            ex.printStackTrace();
        } //finally {
//            try {
//                inFile.close();
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
//        }
    } 
    
    public void updatingPropFile(String nameEntity){
       

        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(PROPERTIES_FILE)); // FileInputStream
            prop.setProperty("trainFile", TRAINFILE.replace("%S", nameEntity));
            prop.setProperty("serializeTo",MODELFILE.replace("%S", nameEntity));
            prop.store(new FileOutputStream(PROPERTIES_FILE),""); // FileOutputStream 
        } catch (Exception ex) {
            ex.printStackTrace();
        }
   }     
        
   public  void saveGroups(boolean ispn,boolean bltrain, boolean isLower){
       //only one proper noun classifier
       String[] classStr={ONLYONEPNOUNCLASS};
       if(!ispn)
           classStr=groupsOfNE;
       
       for(String str:classStr)
           saveFilesForLClassifier(str,bltrain,isLower);

    }
        
    public void saveFilesForLClassifier(String en, boolean bltrain, boolean isLower) {
            try {
                GraphIO gio = new GraphIO(null);
                OutputStreamWriter outFile =null;
                String xmllist=LISTTRAINFILES;
                if(bltrain)
                    outFile = new OutputStreamWriter(new FileOutputStream(TRAINFILE.replace("%S", en)),UTF8_ENCODING);
                else{
                    xmllist=LISTTESTFILES;
                    outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", en)),UTF8_ENCODING);
                }
                BufferedReader inFile = new BufferedReader(new FileReader(xmllist));
                int uttCounter=0;
                for (;;) {
                    String s = inFile.readLine();
                    if (s==null) break;
                    List<DetGraph> gs = gio.loadAllGraphs(s);
                    for (int i=0;i<gs.size();i++) {
                            DetGraph group = gs.get(i);
                            int nexinutt=0;
                            //outFile.append("NO\tBS\tBS\n");
                            for (int j=0;j<group.getNbMots();j++) {
                                    nexinutt++;

                                    // calcul du label
                                    String lab = "NO";
                                    int[] groups = group.getGroups(j);
                                    if (groups!=null)
                                        for (int gr : groups) {
                                            
                                            if(en.equals(ONLYONEPNOUNCLASS)){
                                                //all the groups are proper nouns pn
                                                for(String str:groupsOfNE){
                                                    if (group.groupnoms.get(gr).startsWith(str)) {
                                                        if(typeofClass.equals(CNConstants.BIO)){
                                                            int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                            if (debdugroupe==j) lab = en+"B";    
                                                            else lab = en+"I";                                                            
                                                        }else if(typeofClass.equals(CNConstants.BILOU)){
                                                            int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                            int endgroupe = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;   
                                                                    if(debdugroupe==endgroupe){ 
                                                                        lab=en+"U";
                                                                    }else{
                                                                        if (debdugroupe==j) lab = en+"B";
                                                                        else if(endgroupe==j) lab=en+"L";
                                                                        else lab = en+"I";
                                                                    }                                                            
                                                        }else
                                                            lab=en;
                                                        break;
                                                    }
                                                }
                                            }else{
                                                if (group.groupnoms.get(gr).startsWith(en)) {
                                                    if(typeofClass.equals(CNConstants.BIO)){
                                                        int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                        if (debdugroupe==j) lab = en+"B";    
                                                        else lab = en+"I";
                                                    }else if(typeofClass.equals(CNConstants.BILOU)){
                                                            int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                            int endgroupe = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;   
                                                                    if(debdugroupe==endgroupe){ 
                                                                        lab=en+"U";
                                                                    }else{
                                                                        if (debdugroupe==j) lab = en+"B";
                                                                        else if(endgroupe==j) lab=en+"L";
                                                                        else lab = en+"I";
                                                                    }                                                            
                                                    }else
                                                        lab=en;
                                                    break;
                                                }
                                            }
                                        }
                                    /*        
                      
                                    if(!isStopWord(group.getMot(j).getPOS())){
					String inWiki ="F";
                                        if(!group.getMot(j).getPOS().startsWith("PRO") && !group.getMot(j).getPOS().startsWith("ADJ"))
                                            inWiki =(WikipediaAPI.processPage(group.getMot(j).getForme()).equals(CNConstants.CHAR_NULL))?"F":"T";
                                        outFile.append(lab+"\t"+group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\t"+ inWiki +"\n");
                                    } 
                                     */                                  
                                    if(POSFILTER && !isStopWord(group.getMot(j).getPOS())){
                                        if(isLower)
                                            outFile.append(group.getMot(j).getForme().toLowerCase()+"\t"+group.getMot(j).getPOS()+"\t"+"NOCL\t"+lab+"\n");
                                        else
                                            outFile.append(group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\t"+"NOCL\t"+lab+"\n");
                                    }else if(!POSFILTER){
                                        if(isLower)
                                            outFile.append(group.getMot(j).getForme().toLowerCase()+"\t"+group.getMot(j).getPOS()+"\t"+"NOCL\t"+lab+"\n");
                                        else
                                            outFile.append(group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\t"+"NOCL\t"+lab+"\n");                                       
                                    }    
                                        
                            }
                            
                                                        
                            uttCounter++;
                            if(bltrain && uttCounter> TRAINSIZE){
                                break;
                            }
                    }
                    if(bltrain && uttCounter> TRAINSIZE){
                        break;
                    }                    
                }
                outFile.flush();
                outFile.close();
                inFile.close();
                ErrorsReporting.report("groups saved in groups "+ TRAINFILE.replace("%S", en) + " utterances: "+uttCounter);
            } catch (IOException e) {
                    e.printStackTrace();
            }
    }   
    
    public void trainOneClassifier(String sclassifier){
        CRFClassifier model = null;
        File mfile = new File(MODELFILE.replace("%S", sclassifier));
        String[]  arrProps = {"-props",PROPERTIES_FILE};
        Properties props = StringUtils.argsToProperties(arrProps);        
        if(!mfile.exists()){
            updatingPropFile(sclassifier);
  
            model = new CRFClassifier(props);       
            model.train(); 
            try {

                //save the model in a file
                model.serializeClassifier(MODELFILE.replace("%S", sclassifier));
            } catch (Exception ex) {

            }

        }else{
            
            try {
                model = new CRFClassifier(props);   
                model.loadClassifierNoExceptions(MODELFILE.replace("%S", sclassifier), props);            

            } catch (Exception ex) {
                ex.printStackTrace();
            } 

        }
        if(model!=null){
          
            //train data
            modelMap.put(sclassifier,model);
            MarginCRF margin = new MarginCRF(model);
            marginMAP.put(sclassifier,margin); 
   
    
        }        
    }
        
    /**
     * Returns the different models for each type of NE,
     * save the models in a file, so there is no need to retrain each time
     * @param labeled
     * @return 
     */    
    public void trainAllCRFClassifier(boolean ispn,boolean blsavegroups, boolean isLower) {
        //TreeMap<String,Double> lcfeatsDict = new TreeMap<>();
        //TreeMap<String,Double> featsDict = new TreeMap<>();
        //save the trainset
        if(blsavegroups)
            saveGroups(ispn,true,isLower);
        //only one proper noun classifier
        String[] classStr={ONLYONEPNOUNCLASS};
        if(!ispn)
            classStr=groupsOfNE ;
        //call the classifier
        modelMap.clear();
        marginMAP.clear();
        for(String str:classStr){
            /*
             if(!str.equals("pers"))
                continue;
            //*/
            trainOneClassifier(str);
            
        }
        
    }   
    
    public boolean isStopWord(String pos){
        if(pos.startsWith("PUN") || pos.startsWith("DET")|| pos.startsWith("PRP")||pos.startsWith("INT")||pos.startsWith("SENT"))
            return true;
        
        return false;
    }
    
    
    /**
     * Get the instances, the features and class by instance
     * @param fileName
     * @param model 
     */
   
//    
//    public void savingModel(String sclassifier,CRFClassifier model){
//        File mfile = new File(MODELFILE.replace("%S", sclassifier));
//        try {
//            IOUtils.writeObjectToFile(model, mfile);
//        } catch (IOException ex) {
//
//        }
//    }
    
    public CRFClassifier getModel(String classifier){
        if(modelMap.containsKey(classifier))
            return modelMap.get(classifier);
        return null;
    }
    
 
    /**
     * Verify most frequent instances according to a list of instances obtained from a histogram in octave.
     * @param modelKey 
     */
    public void checkingInstances(String modelKey){
        try{
            BufferedReader inFile = new BufferedReader(new InputStreamReader(new FileInputStream("analysis/inst.mat"), UTF8_ENCODING));
            BufferedReader trFile = new BufferedReader(new InputStreamReader(new FileInputStream(TRAINFILE.replace("%S", modelKey)), UTF8_ENCODING));
            CRFClassifier lc=this.modelMap.get(modelKey);
            
            for(;;){
               
                String line = inFile.readLine();
                
                if(line== null)
                    break;
                if(line.startsWith("#"))
                    continue;
                
                if(line.equals("")||line.equals("\n")||line.equals(" "))
                    continue;                
                
                String[] instances = line.split("\\s");
                int lineNumber=0;
                for(String inst:instances){
                    if(inst.equals(""))
                        continue;
                    int instance = Integer.parseInt(inst);
                    for(;;lineNumber++){
                        String trLine = trFile.readLine();
                        
                        if(trLine== null)
                            break;
                        
                        if(lineNumber==instance){
                           System.out.println(trLine); 
                           lineNumber++;
                           break;
                        }
                        
                    }
                }

            }
        }catch(Exception ex){
            
        }   
    }
    
    /**
     * Test the classifier
     */
    public void testingClassifier(boolean ispn,boolean isSavingGroups, boolean isLower, String classClassPath){

           
           if(isSavingGroups)
                saveGroups(ispn,false, isLower);

            //only one proper noun classifier
            String[] classStr={ONLYONEPNOUNCLASS};
            if(!ispn)
                classStr=groupsOfNE ;
               
           for(String smodel:classStr){          
               updatingPropFile(smodel);
               OutputStreamWriter   outFile= null;
               try{               
                    outFile=new OutputStreamWriter(new FileOutputStream(OUTFILE.replace("%S", smodel)));
                
                    //command
                    //String cmd="java -Xmx1g -cp  \"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\" edu.stanford.nlp.classify.ColumnDataClassifier -prop slinearclassifier.props groups.pers.tab.lc.train -testFile groups.pers.tab.lc.test > out.txt";

                    //String[] call={"java","-Xmx1g","-cp","\"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\"","edu.stanford.nlp.classify.ColumnDataClassifier", "-prop","slinearclassifier.props", "-testFile", TESTFILE.replace("%S", smodel),"> out.txt"};
                    //Process process = Runtime.getRuntime().exec(call);
                    //stanford-ner-2014-01-04/stanford-ner-2014-01-04.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier
                    String cmd="java -Xmx1g -cp "+classClassPath+ "  edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier "+MODELFILE.replace("%S", smodel)+" -testFile "+TESTFILE.replace("%S", smodel);
                    Process process = Runtime.getRuntime().exec(cmd);
                    InputStream stdout = process.getInputStream();

                    BufferedReader input = new BufferedReader (new InputStreamReader(stdout)); 
                    while(true){
                        String line=input.readLine();
                        if(line == null)
                            break;


                        outFile.append(line+"\n");
                        outFile.flush();

                    }

                    InputStream stderr = process.getErrorStream();
                    input = new BufferedReader (new InputStreamReader(stderr)); 
                    while(true){
                        String line=input.readLine();
                        if(line == null)
                            break;
                        /*
                        if(!line.startsWith("Cls"))
                            continue;
                        */
                        System.out.println("EVAL: "+line);

                    }      
                    outFile.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
           }
           System.out.println("ok");
       
 
    }
    public void testingClassifier(String smodel,boolean isSavingGroups, boolean isLower, String classClassPath){

           
           if(isSavingGroups)
                saveFilesForLClassifier(smodel,false,isLower);

            //only one proper noun classifier
            
           updatingPropFile(smodel);
           OutputStreamWriter   outFile= null;
           try{               
                outFile=new OutputStreamWriter(new FileOutputStream(OUTFILE.replace("%S", smodel)));

                //command
                //String cmd="java -Xmx1g -cp  \"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\" edu.stanford.nlp.classify.ColumnDataClassifier -prop slinearclassifier.props groups.pers.tab.lc.train -testFile groups.pers.tab.lc.test > out.txt";

                //String[] call={"java","-Xmx1g","-cp","\"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\"","edu.stanford.nlp.classify.ColumnDataClassifier", "-prop","slinearclassifier.props", "-testFile", TESTFILE.replace("%S", smodel),"> out.txt"};
                //Process process = Runtime.getRuntime().exec(call);
                //stanford-ner-2014-01-04/stanford-ner-2014-01-04.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier
                String cmd="java -Xmx1g -cp "+classClassPath+ "  edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier "+MODELFILE.replace("%S", smodel)+" -testFile "+TESTFILE.replace("%S", smodel);
                Process process = Runtime.getRuntime().exec(cmd);
                InputStream stdout = process.getInputStream();

                BufferedReader input = new BufferedReader (new InputStreamReader(stdout)); 
                while(true){
                    String line=input.readLine();
                    if(line == null)
                        break;


                    outFile.append(line+"\n");
                    outFile.flush();

                }

                InputStream stderr = process.getErrorStream();
                input = new BufferedReader (new InputStreamReader(stderr)); 
                while(true){
                    String line=input.readLine();
                    if(line == null)
                        break;
                    /*
                    if(!line.startsWith("Cls"))
                        continue;
                    */
                    System.out.println("EVAL: "+line);

                }      
                outFile.close();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
           
           System.out.println("ok");
       
 
    }   
    public String printVector(double[] matrix){
	
		StringBuffer buf = new StringBuffer();
                buf.append("[");
		for (int i = 0; i < matrix.length; i++) {
			
                        buf.append(matrix[i]);
                                
                        if(i< (matrix.length-1))
                            buf.append(";");
		}
                buf.append("];");
		return buf.toString();
	      
    }    
    
    public String printMatrix(double[][] matrix){
	
		StringBuffer buf = new StringBuffer();
                buf.append("[");
		for (int i = 0; i < matrix.length; i++) {

			for (int j = 0; j < matrix[i].length; j++) {
				buf.append(matrix[i][j]);
                                if(j< (matrix[i].length-1))
                                    buf.append(",");
			}
                        if(i< (matrix.length-1))
                            buf.append(";");
		}
                buf.append("];");
		return buf.toString();
	      
    }
    
    /**
     * Return the number of instances
     * @return 
     */
    public int getNumberOfInstances(){
        return this.numInstances;
    }
    

    public void drawingPNScores(){

        trainAllCRFClassifier(true,true,false);
        //Histoplot.showit(margin.getScoreForAllInstancesLabel0(featsperInst,scores), featsperInst.size());
        //analyzing.testingClassifier(true,true, "");
        String sclass = CNConstants.PRNOUN;
        CRFClassifier model = getModel(sclass);
        List<List<Integer>> featsperInst = new ArrayList<>(); 
         
        List<Integer> labelperInst = new ArrayList<>();
        getValues(TRAINFILE.replace("%S", sclass), model, featsperInst, labelperInst);
        double[] scores= new double[featsperInst.size()];
        Arrays.fill(scores, 0.0);       
        MarginCRF margin= marginMAP.get(sclass);
        Histoplot.showit(margin.getScoreForAllInstancesGivenLabel(featsperInst,scores,0), featsperInst.size());
    }
    
   public void evaluationBIOCLASSRESULTS(String goalClass,String filename){

        BufferedReader testFile = null;
        try {
            testFile = new BufferedReader(new InputStreamReader(new FileInputStream(filename), CNConstants.UTF8_ENCODING));
            String[] goalClasses = null;
            
            if(typeofClass.equals(CNConstants.BIO)){
               String[] classes = {goalClass+"B", goalClass+"I"};
               goalClasses=classes;
            }else{
                 String[] classes  = {goalClass+"B", goalClass+"I",goalClass+"L",goalClass+"U"};
                 goalClasses=classes;
            }    

            int[]  tps= new int[goalClasses.length];int[]  fps= new int[goalClasses.length];
            int[]  fns= new int[goalClasses.length];  int[]  tns= new int[goalClasses.length];  
            Arrays.fill(tps, 0);Arrays.fill(fps, 0);
            Arrays.fill(fns, 0);Arrays.fill(tns, 0);
            for(;;){

                String line = testFile.readLine();   
                
                if(line== null)
                    break;                
                if(line.startsWith("#"))
                    continue;
                
                String values[] = line.split("\\t");
                
                if(values.length < 3)
                    continue;
                String label = values[1];
                String recognizedLabel = values[2];
                
                for(int i=0;i< goalClasses.length;i++){
                    if(recognizedLabel.equals(goalClasses[i]) && label.equals(goalClasses[i]))
                        tps[i]++;

                    if(recognizedLabel.equals(goalClasses[i])&& !label.equals(goalClasses[i]))
                        fps[i]++;

                    if(!recognizedLabel.equals(goalClasses[i])&&label.equals(goalClasses[i]))
                        fns[i]++;
                    if(!recognizedLabel.equals(goalClasses[i])&&!label.equals(goalClasses[i]))
                        tns[i]++;
                }
                
            }
            for(int i=0;i< goalClasses.length;i++){
                double precision= (double) tps[i]/(tps[i]+fps[i]);
                double recall= (double) tps[i]/(tps[i]+fns[i]);
                double f1=(2*precision*recall)/(precision+recall);

                System.out.println(goalClasses[i]+"  precision: "+precision);
                System.out.println(goalClasses[i]+"  recall: "+recall);
                System.out.println(goalClasses[i]+"  f1: "+f1);
            
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                testFile.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
       
       
   }      


  /**
    * Call all methods for training and evaluate the CRF
    * you have to set whether or not it uses BIO or BILOU configuration
    * and whether or not it uses gazetteers 
    * @param isGaz
    * @param typeOfclasses 
    */ 
  public void properNounDetectionOnEster(boolean isGaz, String typeOfclasses){
        
        File mfile = new File(MODELFILE.replace("%S", CNConstants.PRNOUN));
        mfile.delete(); 
        this.typeofClass=typeOfclasses;
        if(typeOfclasses.startsWith(CNConstants.BIO.substring(0,2)))
            POSFILTER=false;
        if(isGaz)
            PROPERTIES_FILE="scrfGaz.props";
        trainAllCRFClassifier(true,true,false);
        testingClassifier(true,true,false,"/home/rojasbar/development/contnomina/stanfordNLP/stanford-ner-2014-01-04/stanford-ner-2014-01-04.jar");
        

        
        if(!typeOfclasses.startsWith(CNConstants.BIO.substring(0,2)))
            AnalyzeClassifier.evaluationCLASSRESULTS(CNConstants.PRNOUN,OUTFILE);
        else
            evaluationBIOCLASSRESULTS(CNConstants.PRNOUN,OUTFILE);
    }
    
  public void detectingNEOnEster(boolean isGaz, String typeOfclasses){

    for(String str:groupsOfNE) 
         detectingOneEntityOnEster(str,isGaz, typeOfclasses);


    }  
   public void detectingOneEntityOnEster(String str,boolean isGaz, String typeOfclasses){
        this.typeofClass=typeOfclasses;
        if(typeOfclasses.startsWith(CNConstants.BIO.substring(0,2)))
            POSFILTER=false;
        if(isGaz)
            PROPERTIES_FILE="scrfGaz.props";      
        File mfile = new File(MODELFILE.replace("%S", str));
        mfile.delete(); 
        saveFilesForLClassifier(str,true,false);
        trainOneClassifier(str);
        testingClassifier(str,true,false,"/home/rojasbar/development/contnomina/stanfordNLP/stanford-ner-2014-01-04/stanford-ner-2014-01-04.jar");
 
        if(!typeofClass.startsWith(CNConstants.BIO.substring(0,2)))
            AnalyzeClassifier.evaluationCLASSRESULTS(CNConstants.PRNOUN,OUTFILE.replace("%S", str));
        else
            evaluationBIOCLASSRESULTS(str,OUTFILE.replace("%S", str));           
         
    }   
    
    public static void main(String args[]) {
        AnalyzeCRFClassifier analyzing = new AnalyzeCRFClassifier();
        
        /*
        AnalyzeCRFClassifier.TRAINSIZE=500;
        for(int i=0; i<20;i++){
            System.out.println("********** Corpus size (#utts)"+AnalyzeCRFClassifier.TRAINSIZE);
            String sclass="pn";
            File mfile = new File(MODELFILE.replace("%S", sclass));
            mfile.delete();
            mfile = new File(TRAINFILE.replace("%S", sclass));
            mfile.delete();
            mfile = new File(TESTFILE.replace("%S", sclass));
            mfile.delete();
            analyzing.trainAllCRFClassifier(true,true);
            analyzing.testingClassifier(true,true, "");
            
            AnalyzeCRFClassifier.TRAINSIZE+=100;
            
        }
        //*/
        //trainLinearclassifier(ispn,blsavegroups,islower)
        //analyzing.trainAllCRFClassifier(false,true,false);
        //Histoplot.showit(margin.getScoreForAllInstancesLabel0(featsperInst,scores), featsperInst.size());
        //analyzing.testingClassifier(true,false, "",false,"/home/rojasbar/development/contnomina/stanfordNLP/stanford-ner-2014-01-04/stanford-ner-2014-01-04.jar");
        //analyzing.testingClassifier(true,true, "",false,"/home/rojasbar/development/contnomina/stanfordNLP/stanford-ner-2014-01-04/stanford-ner-2014-01-04.jar");
        //analyzing.drawingPNScores();
        //analyzing.detectingNEOnEster(false,CNConstants.BIO);
        //analyzing.properNounDetectionOnEster(false,CNConstants.BIO);
        //analyzing.detectingOneEntityOnEster(CNConstants.PERS,false,CNConstants.BIO);
        //analyzing.detectingOneEntityOnEster(CNConstants.ORG,false,CNConstants.BIO);
        analyzing.detectingOneEntityOnEster(CNConstants.PROD,false,CNConstants.BIO);
        //analyzing.detectingOneEntityOnEster(CNConstants.LOC,false,CNConstants.BIO);
        //analyzing.evaluationBIOCLASSRESULTS(CNConstants.PRNOUN,OUTFILE);
        //AnalyzeClassifier.evaluationCLASSRESULTS(CNConstants.PRNOUN,OUTFILE);

    }
  
}
