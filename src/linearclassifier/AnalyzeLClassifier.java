/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package linearclassifier;


import conll03.CoNLL03Ner;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import java.util.List;
import edu.stanford.nlp.classify.ColumnDataClassifier;


import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.Datum;


import edu.stanford.nlp.util.Index;
import edu.stanford.nlp.util.Pair;
import gmm.GMMDiag;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jsafran.DetGraph;
import jsafran.GraphIO;
import optimization.MultiCoreCoordinateDescent;
import optimization.MultiCoreFSCoordinateDesc;
import optimization.MultiCoreStocCoordDescent;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import resources.WikipediaAPI;
import test.AutoTests;
import tools.CNConstants;
import tools.GeneralConfig;
import tools.Histoplot;

import tools.PlotAPI;
import utils.ErrorsReporting;
import utils.FileUtils;


/**
 * This class process the instances and features by instance in the Stanford linear classifier 
 * @author rojasbar
 */
public class AnalyzeLClassifier {
    private static final long serialVersionUID = 1L; 
    public static String MODELFILE="bin.%S.lc.mods";
    public static String TRAINFILE="groups.%S.tab.lc.train";
    public static String TESTFILE="groups.%S.tab.lc.test";
    //public static String LISTTRAINFILES="esterTrain.xmll";
    //public static String LISTTESTFILES="esterTest.xmll";
    public static String LISTTRAINFILES="esterTrain.xmll";
    public static String LISTTESTFILES="esterTest.xmll";
    public static String PROPERTIES_FILE="etc/slinearclassifier.props"; //slinearclassifier.props or slinearclassNOTNGRAM.props
    public static String NUMFEATSINTRAINFILE="2-";
    public static String ONLYONEPNOUNCLASS=CNConstants.PRNOUN;
    public static String ONLYONEMULTICLASS=CNConstants.ALL;
    public static String[] groupsOfNE = {CNConstants.PERS,CNConstants.ORG, CNConstants.LOC, CNConstants.PROD};
    public static String CURRENTSETCLASSIFIER=CNConstants.PRNOUN; //setted by default but you can change it 
    public static int TRAINSIZE=5; 
    public static int TESTSIZE=Integer.MAX_VALUE; 
    public static Margin CURRENTPARENTMARGIN=null;
    public static float  CURRENTPARENTESTIMR0=0f;
    public static double  CURENTPARENTF10=0f;
    public static boolean exitAfterTrainingFeaturization=false;
    public static boolean serializeFeatures=false;
    
    
    private String typeofClass="I0";  //possible values "IO","BIO","BILOU";
    //TRAINSIZE=20;  
    
    
    private HashMap<String, LinearClassifier> modelMap = new HashMap<>();
    private HashMap<String,Margin> marginMAP = new HashMap<>();
    private int numInstances=0;
    private static HashMap<String,Double> priorsMap;
    

    private List<List<Integer>> featperInstance = new ArrayList<>();
    private List<Integer> lblperInstance = new ArrayList<>();
    private HashMap<Integer,List<Integer>> instPerFeatures= new HashMap<>();
    private HashMap<Integer,List<String>> stLCDictTrainFeatures=new HashMap<>();
    private HashMap<Integer,List<String>> stLCDictTestFeatures=new HashMap<>();
    private long elapsedTime;
    
    private HashMap<Integer,Margin> parallelGrad = new HashMap<>();
    private Random rnd = new Random();
    
    
    
    
    public AnalyzeLClassifier(){
        GeneralConfig.loadProperties();
        LISTTRAINFILES=GeneralConfig.listLCTrain;
        LISTTESTFILES=GeneralConfig.listLCTest;
        PROPERTIES_FILE=GeneralConfig.lcProps;
        priorsMap=new HashMap<>();
    }
    public void setTypeOfClass(String type){
        this.typeofClass=type;
    }
    public void setPriors(HashMap<String,Double> py){
        priorsMap = py;
        //System.arraycopy(py, 0, priorsMap, 0, py.length);
    }
    
    /**
     * build the priors vector that coincides with the indexes of the classes in the classifier
     * 
     * @return 
     */
    public static float[] getPriors(){
        Margin margin = CURRENTPARENTMARGIN;
        if(margin==null){
           ErrorsReporting.report("The margin cannot be null");
        }
        Index<String> lblIndex = margin.getLabelIndex();
        float[] priors = new float [priorsMap.size()];
        
        for(String lbl:lblIndex){                
            priors[lblIndex.indexOf(lbl)]=priorsMap.get(lbl).floatValue();
            
        }
        return priors;
    }
    /**
     * Updates the properties files with the name of the training file
     * @param nameEntity
     * @param iswiki 
     */
    public void updatingPropFile(String nameEntity, boolean iswiki){

        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(PROPERTIES_FILE)); // FileInputStream
            prop.setProperty("trainFile", TRAINFILE.replace("%S", nameEntity));
            
            if(iswiki){
                System.out.println("Entro a is wiki updatingPropFile " + PROPERTIES_FILE);
                prop.put("6.useString","true");
                //prop.setProperty("3.useString","true");
            }else{
                if(prop.getProperty("6.useString")!=null)
                    prop.remove("6.useString");
            }
            if(exitAfterTrainingFeaturization)
                prop.setProperty("tolerance","10");
            else{
                if(prop.getProperty("tolerance")!=null)
                prop.remove("tolerance");
            }    
            prop.store(new FileOutputStream(PROPERTIES_FILE),""); // FileOutputStream 
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
   }     
      
   public  void saveGroups(String sclass,boolean bltrain, boolean iswiki, boolean allLower){
       //only one proper noun classifier
       String[] classStr={ONLYONEPNOUNCLASS};
       
       if(sclass.equals(ONLYONEMULTICLASS)){
           classStr[0]=ONLYONEMULTICLASS;
       }
       if(!sclass.equals(ONLYONEPNOUNCLASS) && !sclass.equals(ONLYONEMULTICLASS))
           classStr=groupsOfNE;
       
       for(String str:classStr)
           saveFilesForLClassifier(str,bltrain, iswiki, allLower);

    }
   
    public void reInitializingEsterFiles(){
            MODELFILE="bin.%S.lc.mods";
            TRAINFILE="groups.%S.tab.lc.train";
            TESTFILE="groups.%S.tab.lc.test";
    }
        
    public void saveFilesForLClassifier(String entity, boolean bltrain, boolean iswiki, boolean isLower) {
            try {
                //if(bltrain&iswiki)
                //WikipediaAPI.loadWiki();
                GraphIO gio = new GraphIO(null);
                OutputStreamWriter outFile =null;
                String xmllist=LISTTRAINFILES;
                if(bltrain)
                    outFile = new OutputStreamWriter(new FileOutputStream(TRAINFILE.replace("%S", entity)),CNConstants.UTF8_ENCODING);
                else{
                    xmllist=LISTTESTFILES;
                    outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", entity)),CNConstants.UTF8_ENCODING);
                }
                BufferedReader inFile = new BufferedReader(new FileReader(xmllist));
                int uttCounter=0,wordcount=0;
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
                                    String lab = CNConstants.NOCLASS;
                                    int[] groups = group.getGroups(j);
                                    if (groups!=null)
                                        for (int gr : groups) {
                                            
                                            if(entity.equals(ONLYONEPNOUNCLASS)){
                                                //all the groups are proper nouns pn
                                                for(String str:groupsOfNE){
                                                    if (group.groupnoms.get(gr).startsWith(str)) {
                                                        if(typeofClass.equals(CNConstants.BIO)){
                                                            int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                            if (debdugroupe==j) lab = entity+"B";    
                                                            else lab = entity+"I";                                                            
                                                        }else if(typeofClass.equals(CNConstants.BILOU)){
                                                            int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                            int endgroupe = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;   
                                                                    if(debdugroupe==endgroupe){ 
                                                                        lab=entity+"U";
                                                                    }else{
                                                                        if (debdugroupe==j) lab = entity+"B";
                                                                        else if(endgroupe==j) lab=entity+"L";
                                                                        else lab = entity+"I";
                                                                    }                                                            
                                                        }else
                                                            lab=entity;
                                                        break;
                                                    }
                                                }
                                            }else{
                                                if (group.groupnoms.get(gr).startsWith(entity)) {
                                                    if(typeofClass.equals(CNConstants.BIO)){
                                                        int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                        if (debdugroupe==j) lab = entity+"B";    
                                                        else lab = entity+"I";
                                                    }else if(typeofClass.equals(CNConstants.BILOU)){
                                                            int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                            int endgroupe = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;   
                                                                    if(debdugroupe==endgroupe){ 
                                                                        lab=entity+"U";
                                                                    }else{
                                                                        if (debdugroupe==j) lab = entity+"B";
                                                                        else if(endgroupe==j) lab=entity+"L";
                                                                        else lab = entity+"I";
                                                                    }                                                            
                                                    }else
                                                        lab=entity;
                                                    break;
                                                }else{
                                                    if (entity.equals(ONLYONEMULTICLASS)) {
                                                        String groupName=group.groupnoms.get(gr);
                                                        groupName=groupName.substring(0, groupName.indexOf("."));
                                                        if(!Arrays.asList(groupsOfNE).toString().contains(groupName))
                                                            continue;
                                                        
                                                        if(typeofClass.equals(CNConstants.BIO)){
                                                            int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                            if (debdugroupe==j) lab = groupName+"B";    
                                                            else lab = groupName+"I";
                                                        }else if(typeofClass.equals(CNConstants.BILOU)){
                                                            int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                            int endgroupe = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;
                                                            if (debdugroupe==endgroupe) lab = groupName+"U"; //Unit
                                                            else if (debdugroupe==j) lab = groupName+"B"; //Begin
                                                            else if (endgroupe==j) lab = groupName+"L"; //Last
                                                            else lab = groupName+"I";//Inside
                                                        }else
                                                            lab=groupName;
                                                        break;
                                                    }                                                    
                                                }
                                            }
                                        }
                                    ///*        
                                    if(iswiki){
                                        if(!isStopWord(group.getMot(j).getPOS())){
                                            String inWiki ="F";
                                            if(!group.getMot(j).getPOS().startsWith("PRO") && !group.getMot(j).getPOS().startsWith("ADJ")&&
                                                    !group.getMot(j).getPOS().startsWith("VER") && !group.getMot(j).getPOS().startsWith("ADV"))
                                                inWiki =(WikipediaAPI.processPage(group.getMot(j).getForme()).equals(CNConstants.CHAR_NULL))?"F":"T";
                                            outFile.append(lab+"\t"+group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\t"+ inWiki +"\n");
                                            wordcount++;
                                            System.out.println("processed word number " + wordcount);
                                        } 
                                    }else if(!isStopWord(group.getMot(j).getPOS())){
                                        if(isLower)
                                            outFile.append(lab+"\t"+group.getMot(j).getForme().toLowerCase()+"\t"+group.getMot(j).getPOS()+"\n");
                                        else
                                            outFile.append(lab+"\t"+group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\n");
                                    }
                            }
                            
                            uttCounter++;
                            if(bltrain && uttCounter> TRAINSIZE){
                                break;
                            }
                            //test set size
                            if(!bltrain && uttCounter > TESTSIZE){
                                break;
                            }
                            /*
                            if (nexinutt>0)
                                outFile.append("NO\tES\tES\n");
                            else
                                outFile.append("NO\tES\tES\n");
                            */
                    }
                    if(bltrain && uttCounter> TRAINSIZE){
                        break;
                    }     
                                                //test set size
                    if(!bltrain && uttCounter > TESTSIZE){
                        break;
                    }
                }
                outFile.flush();
                outFile.close();
                inFile.close();
                ErrorsReporting.report("groups saved in groups.*.tab number of utterances: "+ uttCounter);
            } catch (IOException e) {
                    e.printStackTrace();
            }
    }   
    
 
    public static LinearClassifier loadModelFromFile(String filename){
        LinearClassifier model = null;
        File mfile = new File(filename);     
            Object object;
            try {
                object = IOUtils.readObjectFromFile(mfile);
                model=(LinearClassifier)object;                  

            } catch (Exception ex) {
                ex.printStackTrace();
            } 
        return model;
    }
    
    public void trainOneNERClassifier(String entity, boolean iswiki){
        LinearClassifier model = null;
        File mfile = new File(MODELFILE.replace("%S", entity));
        if(!mfile.exists()){
            updatingPropFile(entity, iswiki);
            ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE); 
            GeneralDataset data = columnDataClass.readTrainingExamples(TRAINFILE.replace("%S", entity));
            model = (LinearClassifier) columnDataClass.makeClassifier(data);

            //model.
            //save the model in a file
            try {
                IOUtils.writeObjectToFile(model, mfile);
            } catch (IOException ex) {

            }

        }else{
            Object object;
            try {
                object = IOUtils.readObjectFromFile(mfile);
                model=(LinearClassifier)object;                  

            } catch (Exception ex) {
                ex.printStackTrace();
            } 

        }
        if(model!=null){
            /*List<List<Integer>> featsperInst = new ArrayList<>(); 
            List<Integer> labelperInst = new ArrayList<>(); */
            //train data
            modelMap.put(entity,model);
            Margin margin = new Margin(model);
            marginMAP.put(entity,margin);  
            
            //compute the values for instances in the trainset
            /*getValues(TRAINFILE.replace("%S", entity),model,featsperInst,labelperInst);
            System.out.println("Total number of features: "+ model.features().size());
            featInstMap.put(entity,featsperInst);
            lblInstMap.put(entity, labelperInst);*/
    
        }        
    }
     public void trainOneClassifier(String entity){
        LinearClassifier model = null;
        File mfile = new File(MODELFILE.replace("%S", entity));
        if(!mfile.exists()){
            
            ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE);                
            GeneralDataset data = columnDataClass.readTrainingExamples(TRAINFILE.replace("%S", entity));
            model = (LinearClassifier) columnDataClass.makeClassifier(data);

            //model.
            //save the model in a file
            try {
                IOUtils.writeObjectToFile(model, mfile);
            } catch (IOException ex) {

            }

        }else{
            Object object;
            try {
                object = IOUtils.readObjectFromFile(mfile);
                model=(LinearClassifier)object;                  

            } catch (Exception ex) {
                ex.printStackTrace();
            } 

        }
        if(model!=null){
            /*List<List<Integer>> featsperInst = new ArrayList<>(); 
            List<Integer> labelperInst = new ArrayList<>(); */
            //train data
            modelMap.put(entity,model);
            Margin margin = new Margin(model);
            marginMAP.put(entity,margin);  
            
            //compute the values for instances in the trainset
            /*getValues(TRAINFILE.replace("%S", entity),model,featsperInst,labelperInst);
            System.out.println("Total number of features: "+ model.features().size());
            featInstMap.put(entity,featsperInst);
            lblInstMap.put(entity, labelperInst);*/
    
        }        
    }       
    public void trainBothDatasetsClassifier(String entity){
        LinearClassifier model = null;
        File mfile = new File(MODELFILE.replace("%S", entity));
        if(!mfile.exists()){
            
            ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE);                
            GeneralDataset data = columnDataClass.readTrainingExamples(TRAINFILE.replace("%S", entity));
            model = (LinearClassifier) columnDataClass.makeClassifier(data);

            //model.
            //save the model in a file
            try {
                IOUtils.writeObjectToFile(model, mfile);
            } catch (IOException ex) {

            }

        }else{
            Object object;
            try {
                object = IOUtils.readObjectFromFile(mfile);
                model=(LinearClassifier)object;                  

            } catch (Exception ex) {
                ex.printStackTrace();
            } 

        }
        if(model!=null){
            /*List<List<Integer>> featsperInst = new ArrayList<>(); 
            List<Integer> labelperInst = new ArrayList<>(); */
            //train data
            modelMap.put(entity,model);
            Margin margin = new Margin(model);
            marginMAP.put(entity,margin);  
            
            //compute the values for instances in the trainset
            /*getValues(TRAINFILE.replace("%S", entity),model,featsperInst,labelperInst);
            System.out.println("Total number of features: "+ model.features().size());
            featInstMap.put(entity,featsperInst);
            lblInstMap.put(entity, labelperInst);*/
    
        }        
    }  
    /**
     * Returns the different models for each type of NE,
     * save the models in a file, so there is no need to retrain each time
     * 
     * @param model : type of classifier: "pn",  binary classifier for detecting proper nouns, <\br>
     *                                     "all", multi-class classifier
     *                                     other, implements different binary classifiers for different name entities (see attribute groupsOfNE)
     * @param blsavegroups, generate first the train and test files
     * @param iswiki, use the wikipedia binary feature (T, if found in wikipedia as entity, F otherwise)
     * @param isLower, transform everything to lowercase for using with the ASR output
     */
    public void trainAllLinearClassifier(String model,boolean blsavegroups, boolean iswiki, boolean isLower) {
        //TreeMap<String,Double> lcfeatsDict = new TreeMap<>();
        //TreeMap<String,Double> featsDict = new TreeMap<>();
        //save the trainset
        if(blsavegroups)
            saveGroups(model,true,iswiki, isLower);
        //only one proper noun classifier
        String[] classStr={ONLYONEPNOUNCLASS};
       if(model.equals(ONLYONEMULTICLASS)){
           classStr[0]=ONLYONEMULTICLASS;
       }
       if(!model.equals(ONLYONEPNOUNCLASS) && !model.equals(ONLYONEMULTICLASS))
           classStr=groupsOfNE;
        //call the classifier
        modelMap.clear();
        marginMAP.clear();
        for(String str:classStr){

           trainOneNERClassifier(str, iswiki);
            
        }
        
    }   
    
    public void trainMulticlassNER(boolean savegroups, boolean iswiki, boolean isLower){
        //ONLYONEMULTICLASS
        if(savegroups)
            saveGroups(ONLYONEMULTICLASS,true,iswiki, isLower);
              
       
        //call the classifier
        modelMap.clear();
        marginMAP.clear();
        

       trainOneNERClassifier(ONLYONEMULTICLASS, iswiki);
            
                
        
    }
    
    public void properNounDetectionOnEster(){
        //Not letter n-gram
        
        File mfile = new File(MODELFILE.replace("%S", CNConstants.PRNOUN));
        mfile.delete();
        PROPERTIES_FILE="slinearclassNOTNGRAM.props";
        //LISTTRAINFILES="esterTrainALL.xmll";
        //LISTTESTFILES="esterTestALL.xmll";
        LISTTRAINFILES="esterTrain.xmll";
        LISTTESTFILES="esterTest.xmll";
        
        //TRAINSIZE=Integer.MAX_VALUE; 
        
        trainAllLinearClassifier(CNConstants.PRNOUN,true,false,false);
        testingClassifier(true, CNConstants.PRNOUN, false, false);
    }
    
    public static boolean isStopWord(String pos){
        if(pos.startsWith("PUN") || pos.startsWith("DET")|| pos.startsWith("PRP")||pos.startsWith("INT")||pos.startsWith("SENT"))
            return true;
        
        return false;
    }
    
    
    /**
     * Get the instances, the features and class by instance
     * @param fileName
     * @param model 
     */
    public void getValues(String fileName, LinearClassifier model, List<List<Integer>> featsperInst,List<Integer> labelperInst){
        

        BufferedReader inFile = null;
        try {
            inFile = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), CNConstants.UTF8_ENCODING));
            
            numInstances=0;
            List<String> lines = new ArrayList<>();
            for (;;) {
                String line = inFile.readLine();
                if (line==null) break;
                lines.add(line);
                numInstances++;  
            }
            int lineNumber=0;
                       
            
            for (int i=0;i<numInstances;i++) {

                String line = lines.get(i);
                ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE);
                Datum<String, String> datum = columnDataClass.makeDatumFromLine(line, 0);
                Collection<String> features = datum.asFeatures();
                
                
                List<Integer> feats = new ArrayList<>();
                //take the id (index) of the features
                for(String f:features){
                    if(model.featureIndex().indexOf(f)>-1){
                        int idx = model.featureIndex().indexOf(f);
                        feats.add(idx);
                        
                        List<Integer> insts= new ArrayList<>();
                        if(instPerFeatures.containsKey(idx))
                            insts=new ArrayList(instPerFeatures.get(idx));
                        
                        insts.add(i);    
                        instPerFeatures.put(idx,insts);
                           
                    }
                }
                //System.out.println("feats[:"+numInstances+"]="+feats);
                featsperInst.add(feats);
                //take the id (index) of the labels
                String label = line.substring(0, line.indexOf("\t"));
                int labelId = model.labelIndex().indexOf(label);
                labelperInst.add(labelId);
                lineNumber++;

                
                if(serializeFeatures){
                    if(fileName.contains("train"))
                        stLCDictTrainFeatures.put(numInstances, new ArrayList<>(features));
                    else
                         stLCDictTestFeatures.put(numInstances, new ArrayList<>(features)); 
                } 
                               
            }
            
            
            if(serializeFeatures){
                if(fileName.contains("train"))
                    serializingFeatures(stLCDictTrainFeatures,true);
                else
                    serializingFeatures(stLCDictTestFeatures,false);
            
            }
            
           this.featperInstance=featsperInst;
           this.lblperInstance=labelperInst;
          
              
           inFile.close();
           
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                inFile.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void getValues(String fileName, LinearClassifier model,boolean useSerializedFeatInst){
        
            
        //Load everything for previously serialized features per instances
        
        if(useSerializedFeatInst && featperInstance.isEmpty()){
            try{
            if(fileName.contains("train"))
                deserializingFeatsPerInstance(true);
            else
                deserializingFeatsPerInstance(false);
            }catch(Exception ex){
                featperInstance=new ArrayList<>();
            }
            numInstances=featperInstance.size();
        }
        
        if(!featperInstance.isEmpty())
            return;
        
        lblperInstance.clear();
        instPerFeatures.clear();
        BufferedReader inFile = null;
        try {
            inFile = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), CNConstants.UTF8_ENCODING));
            
            numInstances=0;
            List<String> lines = new ArrayList<>();
            for (;;) {
                String line = inFile.readLine();
                if (line==null) break;
                lines.add(line);
                numInstances++;  
            }
            int lineNumber=0;
                       
            
            for (int i=0;i<numInstances;i++) {

                String line = lines.get(i);
                ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE);
                Datum<String, String> datum = columnDataClass.makeDatumFromLine(line, 0);
                Collection<String> features = datum.asFeatures();
                
                
                List<Integer> feats = new ArrayList<>();
                //take the id (index) of the features
                for(String f:features){
                    if(model.featureIndex().indexOf(f)>-1){
                        int idx = model.featureIndex().indexOf(f);
                        feats.add(idx);
                        
                        List<Integer> insts= new ArrayList<>();
                        if(instPerFeatures.containsKey(idx))
                            insts=new ArrayList(instPerFeatures.get(idx));
                        
                        insts.add(i);    
                        instPerFeatures.put(idx,insts);
                           
                    }
                }
                //System.out.println("feats[:"+numInstances+"]="+feats);
                featperInstance.add(feats);
                //take the id (index) of the labels
                String label = line.substring(0, line.indexOf("\t"));
                int labelId = model.labelIndex().indexOf(label);
                lblperInstance.add(labelId);
                lineNumber++;

                
                if(serializeFeatures){
                    if(fileName.contains("train"))
                        stLCDictTrainFeatures.put(numInstances, new ArrayList<>(features));
                    else
                         stLCDictTestFeatures.put(numInstances, new ArrayList<>(features)); 
                } 
                               
            }
            
            
            if(serializeFeatures){
                if(fileName.contains("train"))
                    serializingFeatures(stLCDictTrainFeatures,true);
                else
                    serializingFeatures(stLCDictTestFeatures,false);
            
            }
            

           boolean istrain=(fileName.contains("train"))?true:false;
           serializingFeatsPerInstance(istrain);           
           inFile.close();
           
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                inFile.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }    
    public void setNumberOfInstances(int numInst){
        this.numInstances=numInst;
    }
    /**
     * Compute the score F_{\theta}(X) per category
     * Store the scores in an array, that is then analyzed for seen
     * if is normally distributed, we analyze it in Octave
     */
    public void computeFThetaOfX(){
        
        for(String key:marginMAP.keySet()){
            System.out.println("Analyzing classifier :"+key);
            Margin margin= marginMAP.get(key);            
            List<List<Integer>> featsperInst = margin.getFeaturesPerInstances();
            List<Integer> labelperInst = margin.getLabelPerInstances();
            
            int nInst=labelperInst.size();
            
            List<List<Double>> rscoreperLabel = new ArrayList<>();
            List<List<Double>> wscoreperLabel= new ArrayList<>(); 
            //checking the data
            List<List<Integer>> instRscoreperLabel = new ArrayList<>();
            List<List<Integer>> isntWscoreperLabel= new ArrayList<>();  
            for(int y=0; y<margin.getNlabs();y++){
                List<Double> rightlF = new ArrayList<>();
                List<Double> wronglF = new ArrayList<>();
                //checking the weird data of label NO
                List<Integer> instRightlF = new ArrayList<>();
                List<Integer> instWronglF = new ArrayList<>();
                for(int l=0; l<margin.getNlabs();l++){
           
                    for(int i=0; i<nInst;i++){

                        if(labelperInst.get(i)==y){
                            if(l==y){
                                rightlF.add(new Double(margin.getScore(featsperInst.get(i), y)));
                                instRightlF.add(new Integer(i));
                            }else{
                                wronglF.add(new Double(margin.getScore(featsperInst.get(i), l)));                             
                                instWronglF.add(new Integer(i));
                            }
                        }
                    }
                }
                rscoreperLabel.add(rightlF);
                wscoreperLabel.add(wronglF);  
                //checking the weird data of label NO
                instRscoreperLabel.add(instRightlF);
                isntWscoreperLabel.add(instWronglF);
                try{
                    //System.out.println(printVector(scoreperLabel[l]));
                    OutputStreamWriter outFile = new OutputStreamWriter(new FileOutputStream("analysis/ftheta_r"+key+"_"+y+".m"),CNConstants.UTF8_ENCODING);
                    outFile.append("ftheta_r"+key+"_"+y+"="+rscoreperLabel.get(y).toString());
                    outFile.flush();
                    outFile.close();  
                    outFile = new OutputStreamWriter(new FileOutputStream("analysis/ftheta_w"+key+"_"+y+".m"),CNConstants.UTF8_ENCODING);
                    outFile.append("ftheta_w"+key+"_"+y+"="+wscoreperLabel.get(y).toString());
                    outFile.flush();
                    outFile.close(); 
                    outFile = new OutputStreamWriter(new FileOutputStream("analysis/inst_r"+key+"_"+y+".m"),CNConstants.UTF8_ENCODING);
                    outFile.append("inst_r"+key+"_"+y+"="+instRscoreperLabel.get(y).toString());
                    outFile.flush();
                    outFile.close(); 
                    outFile = new OutputStreamWriter(new FileOutputStream("analysis/inst_w"+key+"_"+y+".m"),CNConstants.UTF8_ENCODING);
                    outFile.append("inst_w"+key+"_"+y+"="+isntWscoreperLabel.get(y).toString());
                    outFile.flush();
                    outFile.close();                     
                }catch(Exception ex){
                    ex.printStackTrace();
                }                      
            
            }

        }
    }
    
    public void savingModel(String sclassifier,LinearClassifier model){
        File mfile = new File(MODELFILE.replace("%S", sclassifier));
        try {
            IOUtils.writeObjectToFile(model, mfile);
        } catch (IOException ex) {

        }
    }
    
    public LinearClassifier getModel(String classifier){
        if(modelMap.containsKey(classifier))
            return modelMap.get(classifier);
        return null;
    }
    
    public Margin getMargin(String classifier){
        if(marginMAP.containsKey(classifier))
            return marginMAP.get(classifier);
        return null;
    }
    


    /**
     * Verify most frequent instances according to a list of instances obtained from a histogram in octave.
     * @param modelKey 
     */
    public void checkingInstances(String modelKey){
        try{
            BufferedReader inFile = new BufferedReader(new InputStreamReader(new FileInputStream("analysis/inst.mat"), CNConstants.UTF8_ENCODING));
            BufferedReader trFile = new BufferedReader(new InputStreamReader(new FileInputStream(TRAINFILE.replace("%S", modelKey)), CNConstants.UTF8_ENCODING));
            LinearClassifier lc=this.modelMap.get(modelKey);
            
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
    public void testingClassifier(boolean isSavingGroups, String smodel, boolean iswiki, boolean isLower){
       if(isSavingGroups)
            saveGroups(smodel,false, iswiki, isLower);
       
       
       updatingPropFile(smodel,iswiki);
        try {
            //command
            //String cmd="java -Xmx1g -cp  \"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\" edu.stanford.nlp.classify.ColumnDataClassifier -prop "+ PROPERTIES_FILE+" groups.pers.tab.lc.train -testFile groups.pers.tab.lc.test > out.txt";

            //String[] call={"java","-Xmx1g","-cp","\"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\"","edu.stanford.nlp.classify.ColumnDataClassifier", "-prop", PROPERTIES_FILE, "-testFile", TESTFILE.replace("%S", smodel),"> out.txt"};
            //Process process = Runtime.getRuntime().exec(call);
            String cmd="java -Xmx1g -cp  ../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar edu.stanford.nlp.classify.ColumnDataClassifier -prop "+ PROPERTIES_FILE+" "+TRAINFILE.replace("%S", smodel)+" -testFile "+TESTFILE.replace("%S", smodel);
            Process process = Runtime.getRuntime().exec(cmd);
            InputStream stdout = process.getInputStream();
            
            BufferedReader input = new BufferedReader (new InputStreamReader(stdout)); 
            while(true){
                String line=input.readLine();
                if(line == null)
                    break;
                
                //if(!line.startsWith("Cls"))
                //    continue;
                System.out.println(line);
                 
            }
        
            InputStream stderr = process.getErrorStream();
            input = new BufferedReader (new InputStreamReader(stderr)); 
            while(true){
                String line=input.readLine();
                if(line == null)
                    break;
                //if(!line.startsWith("Cls"))
                //    continue;
                
                System.out.println("EVAL: "+line);
                 
            }          
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
       System.out.println("ok");
       
    }
    public double testingClassifier(LinearClassifier model, String testfile){
        
        ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE);
        
        columnDataClass.testClassifier(model, testfile);
        if(CURRENTSETCLASSIFIER.equals(CNConstants.ALL))
            return columnDataClass.fs.get(new ArrayList(columnDataClass.fs.keySet()).get(0));
        return columnDataClass.fs.get(CURRENTSETCLASSIFIER);
        
    }
    public void testingClassifier(String smodel, String testfile){
        try {
            //command
            //String cmd="java -Xmx1g -cp  \"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\" edu.stanford.nlp.classify.ColumnDataClassifier -prop PROPERTIES_FILE groups.pers.tab.lc.train -testFile groups.pers.tab.lc.test > out.txt";

            //String[] call={"java","-Xmx1g","-cp","\"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\"","edu.stanford.nlp.classify.ColumnDataClassifier", "-prop","PROPERTIES_FILE", "-testFile", TESTFILE.replace("%S", smodel),"> out.txt"};
            //Process process = Runtime.getRuntime().exec(call);
            //String cmd="java -Xmx1g -cp  ../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar edu.stanford.nlp.classify.ColumnDataClassifier -prop " + PROPERTIES_FILE+ " -loadClassifier  "+MODELFILE.replace("%S", smodel)+" -testFile "+TESTFILE.replace("%S", smodel);
            String cmd="java -Xmx1g -cp  ../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar edu.stanford.nlp.classify.ColumnDataClassifier -prop "+ PROPERTIES_FILE+" "+TRAINFILE.replace("%S", smodel)+" -testFile "+TESTFILE.replace("%S", smodel);
            Process process = Runtime.getRuntime().exec(cmd);
            InputStream stdout = process.getInputStream();
            
            BufferedReader input = new BufferedReader (new InputStreamReader(stdout)); 
            while(true){
                String line=input.readLine();
                if(line == null)
                    break;
                
                
                System.out.println(line);
                 
            }
        
            InputStream stderr = process.getErrorStream();
            input = new BufferedReader (new InputStreamReader(stderr)); 
            while(true){
                String line=input.readLine();
                if(line == null)
                    break;
                
                System.out.println("EVAL: "+line);
                 
            }          
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
       System.out.println("ok");
       
    }  
    public void testingClassifierOut(String stanfordClasspath, String outputFile){
        try {
            //command
            OutputStreamWriter outFile = new OutputStreamWriter(new FileOutputStream(outputFile),CNConstants.UTF8_ENCODING);
            String cmd="java -Xmx1g -cp "+stanfordClasspath +" edu.stanford.nlp.classify.ColumnDataClassifier -prop "+ PROPERTIES_FILE+" "+TRAINFILE+" -testFile "+TESTFILE;
            Process process = Runtime.getRuntime().exec(cmd);
            InputStream stdout = process.getInputStream();
            
            BufferedReader input = new BufferedReader (new InputStreamReader(stdout)); 
            while(true){
                String line=input.readLine();
                if(line == null)
                    break;
                
                
                outFile.append(line+"\n");
                 
            }
            outFile.flush();
            outFile.close();
            InputStream stderr = process.getErrorStream();
            input = new BufferedReader (new InputStreamReader(stderr)); 
            while(true){
                String line=input.readLine();
                if(line == null)
                    break;
                
                System.out.println("EVAL: "+line);
                 
            }          
            
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
    
    public static String printMatrix(double[][] matrix){
	
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
    
    public HashMap<String,Double> getPriorsMap(){
        return this.priorsMap;
    }
    
    public float[] computePriors(String sclassifier,LinearClassifier model){
        // why do we need a model to compute priors ? This should be done only from a corpus.
        float[] priors = new float[model.labels().size()];
        
        float prob=0f, alpha=0.1f;
        List<List<Integer>> featsperInst = new ArrayList<>(); 
        List<Integer> labelperInst = new ArrayList<>();         
        getValues(TRAINFILE.replace("%S", sclassifier),model,featsperInst,labelperInst);
        Margin margin = marginMAP.get(sclassifier);
        margin.setFeaturesPerInstance(featsperInst);
        margin.setLabelPerInstance(labelperInst);   
        Index<String> lblIndex = margin.getLabelIndex();
        for(String lb:lblIndex){
            priorsMap.put(lb, 0.0);
        }
        
        
        List<Integer> vals=margin.getLabelPerInstances();
        int[] nTargetClass= new int[model.labels().size()];
        Arrays.fill(nTargetClass, 0);
        
        for(int i=0;i<vals.size();i++){
            int lblIdx=vals.get(i);
            nTargetClass[lblIdx]++;
        }
        for(int l=0; l<priors.length;l++){
            prob = (float) nTargetClass[l]/ (float) vals.size();        
            priors[l]=prob;
            priorsMap.put(lblIndex.get(l),new Double(prob));
            //priors[1]=1-prob;
        }     
                
      return priors;
         
    }
    
    // fractional error in math formula less than 1.2 * 10 ^ -7.
    // although subject to catastrophic cancellation when z in very close to 0
    // from Chebyshev fitting formula for erf(z) from Numerical Recipes, 6.2
    public static double erf(double z) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

        // use Horner's method
        double ans = 1 - t * Math.exp( -z*z   -   1.26551223 +
                t * ( 1.00002368 +
                        t * ( 0.37409196 + 
                                t * ( 0.09678418 + 
                                        t * (-0.18628806 + 
                                                t * ( 0.27886807 + 
                                                        t * (-1.13520398 + 
                                                                t * ( 1.48851587 + 
                                                                        t * (-0.82215223 + 
                                                                                t * ( 0.17087277))))))))));
        if (z >= 0) return  ans;
        else        return -ans;
    } 
    
    /**
     * Le GMM modélise les scores de la class 0, i.e: (mu_0,0 ; sigma_0,0) et (mu_1,0 ; sigma_1,0)
     */
    static float computeR(GMMDiag gmm, final float[] py, boolean isconstrained) {
        final float sqrtpi = (float)Math.sqrt(Math.PI);
        final float pi = (float)Math.PI;
        final float sigma00 = (float)Math.sqrt(gmm.getVar(0, 0, 0));
        
        final float sigma10 = (float)Math.sqrt(gmm.getVar(1, 0, 0));
        
        final float var00 = (float)gmm.getVar(0, 0, 0);
        final float var10 = (float)gmm.getVar(1, 0, 0);
        final float mean00  = (float)gmm.getMean(0, 0);
        final float mean01  = (float)gmm.getMean(0, 1);
        final float mean10  = (float)gmm.getMean(1, 0);
        final float mean11  = (float)gmm.getMean(1, 1);
        

        
        if(isconstrained){
            float t1 = py[0]*(1f-2f*mean00)/(4f*sigma00*sqrtpi) * (1f+(float)erf( (0.5-mean00)/sigma00 ));
            float t2 = py[0]/(2f*pi) * (float)Math.exp( -(0.5f-mean00)*(0.5f-mean00)/var00 );
            float t3 = py[1]*(1f+2f*mean10)/(4f*sigma10*sqrtpi) * (1f-(float)erf( (-0.5-mean10)/sigma10 ));
            float t4 = py[1]/(2f*pi) * (float)Math.exp( -(-0.5f-mean10)*(-0.5f-mean10)/var10 );
            return t1+t2+t3+t4;
        }
        
        float t1= 0.5f + (py[0]*mean01)/2f + (py[1]*mean10)/2f;
        float newsigma=  ((float) gmm.getVar(0, 0, 0) + (float)gmm.getVar(0, 1, 1));
        float t2 = py[0]*((float)gmm.getVar(0, 1, 1))* (float) gmm.getProbability(mean00, mean01+1, newsigma);
        newsigma=  ((float) gmm.getVar(1, 1, 1) + (float)gmm.getVar(1, 0, 0));
        float t3 = py[1]*((float)gmm.getVar(1, 0, 0))* (float) gmm.getProbability(mean11, mean10+1, newsigma);
        newsigma=  ((float) gmm.getVar(0, 1, 1) + (float)gmm.getVar(0, 0, 0));
        float t4= (py[0]/2f)*(mean00 - mean01 - 1)*(float)erf((mean00-mean01-1)/Math.sqrt(2*newsigma));
        newsigma=  ((float) gmm.getVar(1, 0, 0) + (float)gmm.getVar(1, 1, 1));
        float t5= (py[1]/2f)*(mean11 - mean10 - 1)*(float)erf((mean11-mean10-1)/Math.sqrt(2*newsigma));
                            
        
        return t1+t2+t3+t4+t5;
    }    
    
      
    
    /**
     * From the solved equation of R_{theta}
     * @param gmm
     * @param priorsMap
     * @return 
     */
    public static float computeR(GMMDiag gmm, final float[] py, int nLabels) {
            float risk=0f;
            final float pi = (float)Math.PI;
	    final float sqrtpi = (float)Math.sqrt(2*pi);
	    final float var[][] = new float[nLabels][nLabels];
            final float mean[][] = new float[nLabels][nLabels];
            /*
            for(int i=0;i<nLabels;i++)
                for(int j=0;j<nLabels;j++){
            
                var[i][j] = (float)gmm.getVar(i, j, j);
                mean[i][j] = (float)gmm.getMean(i, j);
                //System.out.println("var["+i+"]["+j+"]"+var[i][j]);
                //System.out.println("mean["+i+"]["+j+"]"+mean[i][j]);
                
            }*/
            var[0][0]=(float)gmm.getVar(0, 0, 0);
            var[0][1]=var[0][0];
            var[1][0]=(float)gmm.getVar(1, 0, 0);
            var[1][1]=var[1][0];
            mean[0][0]=(float)gmm.getMean(0, 0);
            mean[1][0]=-mean[0][0];
            mean[1][0]=(float)gmm.getMean(1, 0);
            mean[1][1]=-mean[1][0];
            
            for(int y=0;y<nLabels;y++){
                for(int l=0; l<nLabels;l++){
                    
                    if(y==l)
                        continue;
                    
                    float sqrtofvars= (float) Math.sqrt(var[y][y]+var[y][l]);
                    float doublesqrtofvars= (float) Math.sqrt(2)*sqrtofvars;
                    //Computing constant D
                    float dfirstTerm=  sqrtofvars/sqrtpi;
                    float inexpThirdTerm= ((float) Math.pow(((1f-mean[y][l])*var[y][y]+mean[y][y]*var[y][l]),2))/(var[y][y]+var[y][l]);
                    float inexp=((float) Math.pow((1f-mean[y][l]),2)*var[y][y] + var[y][l] * (float) Math.pow(mean[y][y],2)-inexpThirdTerm)/(2f*var[y][l]*var[y][y]);
                    float dexp= (float) Math.exp(-inexp);
                    float dConst= dfirstTerm*dexp;

                    float meansubstr=mean[y][y]-mean[y][l]-1f;

                    risk += py[y] * (dConst+ meansubstr/2 * ((float) erf(meansubstr/doublesqrtofvars)-1f));
                    
                }
            }
            
	    return risk;
	}    
    /**
     * Do not forget to set the priorsMap before calling this method
     * 
     * TODO: delete this method so that we have a SINGLE method for computing R !
     * 
     * @return 
     */
    public  float computeROfTheta() {
        String sclassifier=CURRENTSETCLASSIFIER;
        //final float[] priors = computePriors(sclassifier,model);
        float[] priors = getPriors();
        // get scores
        GMMDiag gmm = new GMMDiag(priors.length, priors,true);
        // what is in marginMAP ? It maps a Margin, which contains the corpus for train, or test, or both ? and it is mapped to what ?
        gmm.train(marginMAP.get(sclassifier));
        //gmm.trainEstimatedGaussians(marginMAP.get(sclassifier));
        System.out.println("mean=[ "+gmm.getMean(0, 0)+" , "+gmm.getMean(0, 1)+";\n"+
        +gmm.getMean(1, 0)+" , "+gmm.getMean(1, 1)+"]");
        System.out.println("sigma=[ "+gmm.getVar(0, 0, 0)+" , "+gmm.getVar(0, 1, 1)+";\n"+
        +gmm.getVar(1, 0, 0)+" , "+gmm.getVar(1, 1, 1));
        System.out.println("GMM trained");
        
        //return computeR(gmm, priorsMap,marginMAP.get(sclassifier).getNlabs() );
        long beforeR=System.nanoTime();
        float r= computeR(gmm, priors,true); //xtof
        long afterR=System.nanoTime();
        elapsedTime = afterR-beforeR;
        return r;
        
    }
    
    static float computeRNumInt(GMMDiag gmm, final float[] py, int nLabels, boolean isMC, int numIters) {
        float risk=0f;
        
        NumericalIntegration mcInt = new NumericalIntegration();
        //double[] mvIntegral= mcInt.integrate(gmm, CNConstants.UNIFORM, true);
        //mcInt.errorAnalysisBinInt(gmm,priorsMap,nLabels);
        
        for(int y=0;y<nLabels;y++){
            //arguments gmm, distribution of the proposal, metropolis, is plot
            //risk+=priorsMap[y]*mcInt.integrate(gmm,y,CNConstants.UNIFORM, true,false);
            //last paramenter, number of trials, when -1 default takes place = 50000 iterations
            double integral=0.0;

            
            if(isMC){
                if(nLabels>2)
                    integral=mcInt.integrate(gmm,y,CNConstants.UNIFORM, false,false,numIters);
                else
                    integral=mcInt.integrateBinaryCase(gmm,y,CNConstants.UNIFORM, false,false,numIters);
            }else
                integral=mcInt.trapeziumMethod(gmm,y,numIters);

            //System.out.println("Numerical Integration Integral: "+integral);
            risk+=py[y]*integral;
                
        }
        
        return risk;
    }
    /**
     * Set the correct priorsMap with the method setPriors(float[] priorsMap) before
     * call this method
     * @param isMC
     * @param numIters
     * @return 
     */
    public float computeROfThetaNumInt( boolean isMC, int numIters) {
        String sclassifier=CURRENTSETCLASSIFIER;
        //final float[] priors = computePriors(sclassifier,model);
        float[] priors = getPriors();
        //For Multiple dimensions it is better to use Montecarlo integration
        if(priors.length>2)
            isMC=true;
        
        
        // get scores
        GMMDiag gmm = new GMMDiag(priors.length, priors,false);
        gmm.train(marginMAP.get(sclassifier));
        System.out.println("mean=[ "+gmm.getMean(0, 0)+" , "+gmm.getMean(0, 1)+";\n"+
        +gmm.getMean(1, 0)+" , "+gmm.getMean(1, 1)+"]");
        System.out.println("sigma=[ "+gmm.getVar(0, 0, 0)+" , "+gmm.getVar(0, 1, 1)+";\n"+
        +gmm.getVar(1, 0, 0)+" , "+gmm.getVar(1, 1, 1));
        
        System.out.println("GMM trained");
        
        
        //return computeR(gmm, priorsMap,marginMAP.get(sclassifier).getNlabs() );
        long beforeR=System.nanoTime();
        float r= computeRNumInt(gmm, priors,marginMAP.get(sclassifier).getNlabs(), isMC, numIters ); //xtof
        long afterR=System.nanoTime();
        elapsedTime = afterR-beforeR;
        System.out.println("in computeROfThetaNumInt TIME: "+elapsedTime);
        
        return r;
        
    }   
    public  float checkingRNumInt( String sclassifier,double closedForm) {
        CURRENTSETCLASSIFIER=sclassifier;
        try {
        OutputStreamWriter fout  = new OutputStreamWriter(new FileOutputStream("analysis/EMNLPExps/comparingIntR.m"),CNConstants.UTF8_ENCODING);
        
        //final float[] priors = computePriors(sclassifier,model);
       float[] priors = getPriors();
        // get scores
        GMMDiag gmm = new GMMDiag(2, priors,false);
        gmm.train(marginMAP.get(sclassifier));
        System.out.println("mean 00 "+gmm.getMean(0, 0));
        System.out.println("mean 01 "+gmm.getMean(0, 1));
        System.out.println("mean 10 "+gmm.getMean(1, 0));
        System.out.println("mean 11 "+gmm.getMean(1, 1));
        System.out.println("GMM trained");

        float risk=0f,riskTrapezoidInt=0f;;
        NumericalIntegration mcInt = new NumericalIntegration();
        int numTrials=30000;
        //mcInt.errorAnalysisBinInt(gmm,priorsMap,gmm.getDimension(),numTrials);
        /*
        PlotAPI plotIntegral = new PlotAPI("Risk vs trials","Num of trials", "Integral");   

        
        fout.append("cf="+closedForm+";\n");
        fout.append("rmci=[");
        for(int i=100; i< 1000001;i+=100){
            risk=0f;
            for(int y=0;y<py.length;y++){
                    double integral=mcInt.integrateBinaryCase(gmm,y,CNConstants.UNIFORM, false,false,i);
                    risk+=priorsMap[y]*integral;
                    
            }
            plotIntegral.addPoint(i, risk);
            fout.append(risk+";\n");
            System.out.println("rmc="+risk+";");
            fout.flush();
        }  
        fout.append("]\n");
        fout.flush();
         //*/
        ///*
        //PlotAPI plotIntegral2 = new PlotAPI("Risk vs trials","Num of trials", "Integral"); 
        
        fout.append("rti=[");
        for(int i=1; i< 1001;i++){
            riskTrapezoidInt=0f;
            for(int y=0;y<priors.length;y++){
                //double integral = mcInt.trapezoidIntegration(gmm, y,Integer.MAX_VALUE);
                double integral = mcInt.trapeziumMethod(gmm, y,i);
                System.out.println("py="+priors[y]+" I["+y+"]="+integral);
                riskTrapezoidInt+=priors[y]*integral;

            }
            //plotIntegral2.addPoint(i, riskTrapezoidInt);
            fout.append(riskTrapezoidInt+";\n");
            System.out.println("rti="+riskTrapezoidInt+";");
        }
        fout.append("]\n");
        fout.flush();
        //*/
        fout.close();
        return risk;  
    } catch (Exception ex) {
         ex.printStackTrace();
         
         return 0f;
    }       
    }    
    public  void checkingMCTrapezoidNumInt( String sclassifier) {
  
        CURRENTSETCLASSIFIER=sclassifier;     
        //final float[] priors = computePriors(sclassifier,model);
        priorsMap.put(CNConstants.NOCLASS, new Double(0.9));
        priorsMap.put(CNConstants.PRNOUN, new Double(0.1));
                
        float[] priors = getPriors();        
        // get scores
        GMMDiag gmm = new GMMDiag(2, priors,false);
        gmm.train(marginMAP.get(sclassifier));
        System.out.println("mean 00 "+gmm.getMean(0, 0));
        System.out.println("mean 01 "+gmm.getMean(0, 1));
        System.out.println("mean 10 "+gmm.getMean(1, 0));
        System.out.println("mean 11 "+gmm.getMean(1, 1));
        System.out.println("GMM trained");
        
        NumericalIntegration mcInt = new NumericalIntegration();
        //
        double integral=mcInt.integrateMCEasyFunction(gmm,0,CNConstants.UNIFORM, true,true,50000);
       
        System.out.println("rmc="+integral+";");
            
        double riskTrapezoidInt= mcInt.trapezoidIntegration(gmm, 0,Integer.MAX_VALUE);
         
                    
        System.out.println("rti="+riskTrapezoidInt+";");

    }      
    
    public void testingTerms(String sclassifier){
        //final float[] priors = computePriors(sclassifier,model);
        priorsMap.put(CNConstants.NOCLASS, new Double(0.9));
        priorsMap.put(CNConstants.PRNOUN, new Double(0.1));
                
        float[] priors = getPriors();
        // get scores
        GMMDiag gmm = new GMMDiag(2, priors,true);
        gmm.train(marginMAP.get(sclassifier));
        System.out.println("mean 00 "+gmm.getMean(0, 0));
        System.out.println("mean 01 "+gmm.getMean(0, 1));
        System.out.println("mean 10 "+gmm.getMean(1, 0));
        System.out.println("mean 11 "+gmm.getMean(1, 1));
        System.out.println("sigma 00 "+gmm.getVar(0, 0, 0));
        System.out.println("sigma 01 "+gmm.getVar(0, 1, 1));
        System.out.println("sigma 10 "+gmm.getVar(1, 0, 0));
        System.out.println("sigma 11 "+gmm.getVar(1, 1, 1));
        System.out.println("GMM trained");
        
        final float sqrtpi = (float)Math.sqrt(Math.PI);
        final float pi = (float)Math.PI;
        final float sigma00 = (float)Math.sqrt(gmm.getVar(0, 0, 0));
        final float mean00  = (float)gmm.getMean(0, 0);
        float t1 = priors[0]*(1f-2f*mean00)/(2f*sigma00*sqrtpi) * (1f+(float)erf( (0.5-mean00)/sigma00 ));
        NumericalIntegration nInt = new NumericalIntegration();
        double t1NInt=  priors[0]*nInt.trapeziumMethodNSquared(gmm, 3000);
        System.out.println(t1+" vs "+t1NInt);
        
    }
    
    public float testingRForCorpus(String sclass, boolean iswiki){
        CURRENTSETCLASSIFIER=sclass;
        //train the classifier with a small set of train files
        trainOneNERClassifier(sclass, iswiki);  
        LinearClassifier model = modelMap.get(sclass);
        Margin margin = marginMAP.get(sclass);
        CURRENTPARENTMARGIN=margin;
        //scan the test instances for train the gmm
        List<List<Integer>> featsperInst = new ArrayList<>(); 
        List<Integer> labelperInst = new ArrayList<>(); 
        getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
        margin.setFeaturesPerInstance(featsperInst);
        margin.setLabelPerInstance(labelperInst);               
        
        System.out.println("Working with classifier "+sclass);
        float estimr0 = computeROfTheta();
        System.out.println("init R "+estimr0);
        return estimr0;
        
    }
 
   /**
     * The gradient method used ins the Finite Difference
     * f'(a) is approximately (f(a+h)-f(a))/h
     * @param sclass 
     */ 
   public void unsupervisedClassifier(String sclass, boolean closedForm, int niters) {
       CURRENTSETCLASSIFIER=sclass;
        PlotAPI plotR = new PlotAPI("R vs Iterations","Num of Iterations", "R");
        PlotAPI plotF1 = new PlotAPI("F1 vs Iterations","Num of Iterations", "F1");
        
        boolean isMC=false;
        int numIntIters=100;
        

        

        final float eps = 0.1f;   
        int counter=0;
        //train the classifier with a small set of train files
        trainOneNERClassifier(sclass,false);  
        LinearClassifier model = modelMap.get(sclass);
        Margin margin = marginMAP.get(sclass);
        CURRENTPARENTMARGIN=margin;
        int selectedFeats[] = margin.getTopWeights(0.5,50);
        //scan the test instances for train the gmm
        List<List<Integer>> featsperInst = new ArrayList<>(); 
        List<Integer> labelperInst = new ArrayList<>(); 
        getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
        margin.setFeaturesPerInstance(featsperInst);
        margin.setLabelPerInstance(labelperInst);
        margin.setNumberOfInstances(numInstances);
        
        //checks whether or not the classifier is multiclass, if that is the case it uses numerical integration by default
        if(margin.getNlabs()>2){
            closedForm=false;
            isMC=true;
        }
        double[] scores= new double[featsperInst.size()];
        Arrays.fill(scores, 0.0);
        //Histoplot.showit(scorest,featsperInst.size());
        HashSet<String> emptyfeats = new HashSet<>();
        System.out.println("Working with classifier "+sclass);
        
        float estimr0=(closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);


        System.out.println("init R "+estimr0);
        //plotR.addPoint(counter, estimr0);
        double f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
        //plotF1.addPoint(counter,f1);

        System.out.println("Number of features" + margin.getNfeats());
        for (int iter=0;iter<niters;iter++) {
            double[][] weightsForFeat=margin.getWeights();
            final float[] gradw = new float[weightsForFeat.length];
            //for(int i=0;i<weightsForFeat.length;i++){
            for(int i=0;i<selectedFeats.length;i++){
                int featIdx = selectedFeats[i];
                for(int w=0;w < weightsForFeat[featIdx].length;w++){
                    float w0 = (float) weightsForFeat[featIdx][w];
//                    if(iter==0)
//                        w0 = (float) Math.random(); //set initial weights randomly                   
                    if (emptyfeats.contains("["+i+","+w+"]")) continue;
                    float delta = 0.5f;
                    /*for (int j=0;;j++) {
                        if(j>10)
                            break;*/
                        System.out.println("before weight= "+w0);
                        weightsForFeat[featIdx][w] = w0 + w0*delta;
                        System.out.println("after delta= "+ delta);
                        System.out.println("after w0 + w0*delta= "+ (w0 + w0*delta));
                        System.out.println("after weight= "+weightsForFeat[featIdx][w]);
                        //TODO:updating the new weights in the gmm?
                        float estimr = (closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);


                        System.out.println("For feat["+ i +"] weight["+ w +"] R estim ["+iter+"] = "+estimr0);    

                        gradw[w] = (estimr-estimr0)/(w0*delta);
                        System.out.println("grad "+gradw[w]);
                        // we don't go above 10 because some weights may not be used at all
                        /*if (gradw[w]==0 && delta<10) delta*=2f;
                        else if (gradw[w]>0.1||gradw[w]<-0.1) delta/=2f;
                        else break;*/
                    //}

                    weightsForFeat[featIdx][w]=w0;    
                }
                for(int w=0;w < weightsForFeat[featIdx].length;w++){ 
                    if (gradw[w]==0) 
                            emptyfeats.add("["+i+","+w+"]");
                    else  
                        weightsForFeat[featIdx][w] -= gradw[w] * eps;

                }
                /*
                for(int w=0;w < weightsForFeat[0].length;w++){ 
                    weightsForFeat[0][w]= Math.random();
                    weightsForFeat[1][w]= Math.random();
                }*/
                counter++;
                estimr0 =(closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);
                System.out.println("*******************************"); 
                System.out.println("R estim ["+iter+"] = "+estimr0);     
                plotR.addPoint(counter, estimr0);
                System.out.println("*******************************");
                model.setWeights(weightsForFeat);
                f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
                plotF1.addPoint(counter, f1);
                System.out.println("*******************************"); 

                Histoplot.showit(margin.getScoreForAllInstancesLabel0(featsperInst,scores), featsperInst.size());
                //save the model regularly
                if(iter%30==0){
                    File mfile = new File(MODELFILE.replace("%S", sclass));
                    try {
                        IOUtils.writeObjectToFile(model, mfile);
                    } catch (IOException ex) {

                    }
               }            
            }

        }
        for(String emptyW:emptyfeats){
            System.out.println(emptyW);
        }
        
   }      

   public void wkSConstrStochCoordGr(String sclass, boolean closedForm, int niters){
       CURRENTSETCLASSIFIER=sclass;
        PlotAPI plotR = new PlotAPI("R vs Iterations","Num of Iterations", "R");
        PlotAPI plotF1 = new PlotAPI("F1 vs Iterations","Num of Iterations", "F1");
        
        boolean isMC=false;
        int numIntIters=100;
        

        final float eps = 0.1f;   
        int counter=0;
        //train the classifier with a small set of train files
        trainOneNERClassifier(sclass,false);  
        LinearClassifier model = modelMap.get(sclass);
        Margin margin = marginMAP.get(sclass);
        CURRENTPARENTMARGIN=margin;
        int selectedFeats[] = margin.getTopWeights(0.5,500);
        //scan the test instances for train the gmm
        List<List<Integer>> featsperInst = new ArrayList<>(); 
        List<Integer> labelperInst = new ArrayList<>(); 
        getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
        margin.setFeaturesPerInstance(featsperInst);
        margin.setLabelPerInstance(labelperInst); 
        margin.setNumberOfInstances(numInstances);
        
        //checks whether or not the classifier is multiclass, if that is the case it uses numerical integration by default
        if(margin.getNlabs()>2){
            closedForm=false;
            isMC=true;
        }        
        double[] scores= new double[featsperInst.size()];
        Arrays.fill(scores, 0.0);
        //Histoplot.showit(scorest,featsperInst.size());
        HashSet<String> emptyfeats = new HashSet<>();
        System.out.println("Working with classifier "+sclass);
        
        float estimr0=(closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);


        System.out.println("init R "+estimr0);
        plotR.addPoint(counter, estimr0);
        double f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
        plotF1.addPoint(counter,f1);

        System.out.println("Number of features" + margin.getNfeats());
        for (int iter=0;iter<niters;iter++) {
            double[][] weightsForFeat=margin.getWeights();
            final float[] gradw = new float[weightsForFeat.length];
            
            
            
            
            //takes one feature randomly
            float randomsetval=rnd.nextFloat();
            int featIdx =rnd.nextInt(weightsForFeat.length);
            
            if(randomsetval>0.8)
                 featIdx = selectedFeats[rnd.nextInt(selectedFeats.length)];
                 
            
            
            float w0 = (float) weightsForFeat[featIdx][0];

            if (emptyfeats.contains("["+featIdx+","+0+"]")) continue;
            float delta = 0.5f;


            weightsForFeat[featIdx][0] = w0 + w0*delta;

            float estimr = (closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);

            gradw[0] = (estimr-estimr0)/(w0*delta);
            System.out.println("grad "+gradw[0]);

            weightsForFeat[featIdx][0]=w0; 

            if (gradw[0]==0) 
                    emptyfeats.add("["+featIdx+","+0+"]");
            else{  
                weightsForFeat[featIdx][0] -= gradw[0] * eps;                    
                weightsForFeat[featIdx][1]=-weightsForFeat[featIdx][0];
            }    
                
            

            counter++;
            estimr0 =(closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);
            System.out.println("*******************************"); 
            System.out.println("R estim ["+iter+"] = "+estimr0);     
            plotR.addPoint(counter, estimr0);
            System.out.println("*******************************");
            model.setWeights(weightsForFeat);
            f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
            plotF1.addPoint(counter, f1);
            System.out.println("*******************************"); 

            Histoplot.showit(margin.getScoreForAllInstancesLabel0(featsperInst,scores), featsperInst.size());
            //save the model regularly
            if(iter%30==0){
                File mfile = new File(MODELFILE.replace("%S", sclass));
                try {
                    IOUtils.writeObjectToFile(model, mfile);
                } catch (IOException ex) {

                }
           }            
          

        }
        for(String emptyW:emptyfeats){
            System.out.println(emptyW);
        }
        
   }  
  /**
    * The stochastic coordinate gradient 
    * @param sclass  Type of Classifier (pn or [pers,org,loc,prod])
    * @param closedForm used the closed form or trapezoid integration
    */ 
  public void wkSupParallelStocCoordD(String sclass, boolean closedForm, int niters, boolean isModelInMemory, boolean computeF1,boolean useSerializedFeatInst) {
       CURRENTSETCLASSIFIER=sclass;
        UniformRealDistribution uDist = new UniformRealDistribution(-0.1,0.1);
        boolean isMC=false;
        int numIntIters=100;
        if(GeneralConfig.nthreads==CNConstants.INT_NULL){
            GeneralConfig.loadProperties();
        }
        int numberOfThreads=GeneralConfig.nthreads;

        //train the classifier with a small set of train files
        
        if(!isModelInMemory || !modelMap.containsKey(sclass) || !marginMAP.containsKey(sclass) ) {
        	System.out.println("RETRAINING model "+sclass);
            trainOneNERClassifier(sclass,false);  
        }

        
        LinearClassifier model = modelMap.get(sclass);
        Margin margin = marginMAP.get(sclass);        
        CURRENTPARENTMARGIN=margin;
        
        //scan the test instances for train the gmm

        getValues(TESTFILE.replace("%S", sclass),model,useSerializedFeatInst);
        margin.setFeaturesPerInstance(featperInstance);
        margin.setLabelPerInstance(lblperInstance);  
        margin.setInstancesPerFeatures(instPerFeatures);
        margin.setNumberOfInstances(numInstances);
           
        
        //checks whether or not the classifier is multiclass, if that is the case it uses numerical integration by default
        if(margin.getNlabs()>2){
            closedForm=false;
            isMC=true;            
        }        
//        double[] scores= new double[featsperInst.size()];
//        Arrays.fill(scores, 0.0);
        //Histoplot.showit(scorest,featsperInst.size());
        
        System.out.println("Working with classifier "+sclass);
        
        
        CURRENTPARENTESTIMR0=(closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);
        AutoTests.initR = CURRENTPARENTESTIMR0;

        System.out.println("init R "+CURRENTPARENTESTIMR0);
        System.out.println("Number of features " + margin.getNfeats());
        
        if(computeF1){
            ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE);
            columnDataClass.testClassifier(model, AnalyzeLClassifier.TRAINFILE.replace("%S", sclass));
            CURENTPARENTF10=ColumnDataClassifier.macrof1;
            if(!sclass.equals(CNConstants.ALL))
                CURENTPARENTF10=columnDataClass.fs.get(sclass);   
        }
        //by default give the initial weights for the first column column 0
        //List<Double> shuffleWeights=margin.shuffleWeights();       
        
        //List<Double> sameWeights=margin.getOrWeights(0);  
        double[][] sameWeights = margin.getWeights();
        float partiSize = (float) sameWeights.length/numberOfThreads;
        int partSize= Math.round(partiSize);
        MultiCoreStocCoordDescent mthread = new MultiCoreStocCoordDescent(niters,numberOfThreads, closedForm, isMC,numIntIters, computeF1);
        double[][] allfeats = new double[margin.getNfeats()][margin.getNlabs()];
        
        /*
        //set the weights of the test set to a random value
        for(int index=margin.getTrainFeatureSize(); index<margin.getTestFeatureSize();index++){
                double[] sc = new double[margin.getNlabs()];
                sc[0]=uDist.sample();
                sc[1]=-sc[0];
                margin.setWeight(index,sc);
                
        } 
        //*/  
        //Is the random score assignment of test weights deg rading the f-measure?
        System.out.println(" After assign random weights to the features of the test-set ");
        if(computeF1){
            ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE);
            columnDataClass.testClassifier(model, AnalyzeLClassifier.TRAINFILE.replace("%S", sclass));
            CURENTPARENTF10=ColumnDataClassifier.macrof1;
            if(!sclass.equals(CNConstants.ALL))
                CURENTPARENTF10=columnDataClass.fs.get(sclass);   
        }           
        for(int i=0; i<margin.getNfeats(); i++)
            Arrays.fill(allfeats[i], 0.0);
        for(int i=0; i<numberOfThreads; i++){
            try{
            String binaryFile = MODELFILE;
            binaryFile=binaryFile.replace("%S", sclass)+".Thread_"+i;
            File mfile = new File(MODELFILE.replace("%S", sclass));
            File thrfile = new File(binaryFile);
            
            Files.copy(mfile.toPath(), thrfile.toPath(),StandardCopyOption.REPLACE_EXISTING);
            //LinearClassifier modelThr = loadModelFromFile(binaryFile);   
            LinearClassifier modelThr = model;
            Margin marginThr = new Margin(modelThr);
            marginThr.setBinaryFile(binaryFile);
            marginThr.setWeights(margin.getWeights());
            marginThr.copySharedyInfoParallelGrad(margin);
            marginThr.setInstancesPerFeatures(instPerFeatures);
             
            //marginThr.setSamples(margin.getSamples());
            

            int initPart=i*partSize;
            marginThr.setSubListOfFeats(0,initPart, initPart+partSize);
            parallelGrad.put(i,marginThr);
            
            mthread.getWrapper().put(new Pair<>(i, marginThr));
                
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }
        mthread.getWrapper().join();
        while (mthread.getWrapper().peek()) {
            Pair<Integer, Double> result = mthread.getWrapper().poll();   
            //search for the margins and combine the features
            int thrId = result.first();
            Margin mThr=parallelGrad.get(thrId);
            
            for(int i=0; i<mThr.getSubListOfFeats(0).size();i++){
                int orIdx = mThr.getOrWeightIndex(i);
                allfeats[orIdx]=mThr.getPartialWeight(i);
            }
        }
        //Final weights
        margin.setWeights(allfeats);
        float estimrFinal=(closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);
        AutoTests.finalR=estimrFinal;
        System.out.println("Final R : "+ estimrFinal);
        double f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
        File mfile = new File(MODELFILE.replace("%S", sclass));
        try {
            IOUtils.writeObjectToFile(model, mfile);
        } catch (IOException ex) {

        }               
        
   }     
  
  /**
    * The parallel sequence coordinate gradient 
    * @param sclass  Type of Classifier (pn or [pers,org,loc,prod])
    * @param closedForm used the closed form or trapezoid integration
    */ 
  public void wkSupParallelCoordD(String sclass, boolean closedForm, int niters) {
       CURRENTSETCLASSIFIER=sclass;
 
        boolean isMC=false;
        int numIntIters=100;
        if(GeneralConfig.nthreads==CNConstants.INT_NULL){
            GeneralConfig.loadProperties();
        }
        int numberOfThreads=GeneralConfig.nthreads;

        //train the classifier with a small set of train files
        trainOneNERClassifier(sclass,false);  
        LinearClassifier model = modelMap.get(sclass);
        Margin margin = marginMAP.get(sclass);
        CURRENTPARENTMARGIN=margin;
        
        //scan the test instances for train the gmm
        List<List<Integer>> featsperInst = new ArrayList<>(); 
        List<Integer> labelperInst = new ArrayList<>(); 
        getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
        margin.setFeaturesPerInstance(featsperInst);
        margin.setLabelPerInstance(labelperInst);  
        margin.setNumberOfInstances(numInstances);
        
        //checks whether or not the classifier is multiclass, if that is the case it uses numerical integration by default
        if(margin.getNlabs()>2){
            closedForm=false;
            isMC=true;           
        }        
        double[] scores= new double[featsperInst.size()];
        Arrays.fill(scores, 0.0);
        //Histoplot.showit(scorest,featsperInst.size());
        
        System.out.println("Working with classifier "+sclass);
        
        float estimr0=(closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);
        CURRENTPARENTESTIMR0=estimr0;
        ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE);
        columnDataClass.testClassifier(model, AnalyzeLClassifier.TRAINFILE.replace("%S", sclass));
        CURENTPARENTF10=ColumnDataClassifier.macrof1;
        if(!sclass.equals(CNConstants.ALL))
            CURENTPARENTF10=columnDataClass.fs.get(sclass);        
        System.out.println("init R "+estimr0);
        System.out.println("Number of features" + margin.getNfeats());
        
        //List<Double> sameWeights=margin.getOrWeights(0);       
        double[][] sameWeights = margin.getWeights();
        float partiSize = (float) sameWeights.length/numberOfThreads;
        
        
        int partSize= Math.round(partiSize);
        MultiCoreCoordinateDescent mthread = new MultiCoreCoordinateDescent(numberOfThreads,niters, closedForm, isMC,numIntIters);
        double[][] allfeats = new double[margin.getNfeats()][margin.getNlabs()];
        
        for(int i=0; i<margin.getNfeats(); i++)
            Arrays.fill(allfeats[i], 0.0);
        for(int i=0; i<numberOfThreads; i++){
            try{
            String binaryFile = MODELFILE;
            binaryFile=binaryFile.replace("%S", sclass)+".Thread_"+i;
            File mfile = new File(MODELFILE.replace("%S", sclass));
            File thrfile = new File(binaryFile);
            
            Files.copy(mfile.toPath(), thrfile.toPath(),StandardCopyOption.REPLACE_EXISTING);
            LinearClassifier modelThr = loadModelFromFile(binaryFile);   
            Margin marginThr = new Margin(modelThr);
            marginThr.setBinaryFile(binaryFile);
            marginThr.setWeights(margin.getWeights());
            marginThr.copySharedyInfoParallelGrad(margin);
            
            //copy the weights
            int initPart=i*partSize;
            marginThr.setSubListOfFeats(0,initPart, initPart+partSize);
            parallelGrad.put(i,marginThr);
            
            mthread.getWrapper().put(new Pair<>(i, marginThr));
                
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }
        mthread.getWrapper().join();
        while (mthread.getWrapper().peek()) {
            Pair<Integer, Double> result = mthread.getWrapper().poll();   
            //search for the margins and combine the features
            int thrId = result.first();
            Margin mThr=parallelGrad.get(thrId);
            
            for(int i=0; i<mThr.getSubListOfFeats(0).size();i++){
                int orIdx = mThr.getOrWeightIndex(i);
                allfeats[orIdx]=mThr.getPartialWeight(i);
            }
        }
        //Final weights
        margin.setWeights(allfeats);
        float estimrFinal=(closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);
        System.out.println("Final R : "+ estimrFinal);
        double f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
        File mfile = new File(MODELFILE.replace("%S", sclass));
        try {
            IOUtils.writeObjectToFile(model, mfile);
        } catch (IOException ex) {

        }               
        
   }     
   public void wkSupParallelFSCoordD(String sclass, boolean closedForm, int niters) {
       CURRENTSETCLASSIFIER=sclass;
 
        boolean isMC=false;
        int numIntIters=100;
        if(GeneralConfig.nthreads==CNConstants.INT_NULL){
            GeneralConfig.loadProperties();
        }
        int numberOfThreads=GeneralConfig.nthreads;

        //train the classifier with a small set of train files
        trainOneNERClassifier(sclass,false);  
        LinearClassifier model = modelMap.get(sclass);
        Margin margin = marginMAP.get(sclass);
        CURRENTPARENTMARGIN=margin;
        
        //scan the test instances for train the gmm
        List<List<Integer>> featsperInst = new ArrayList<>(); 
        List<Integer> labelperInst = new ArrayList<>(); 
        getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
        margin.setFeaturesPerInstance(featsperInst);
        margin.setLabelPerInstance(labelperInst);  
        margin.setNumberOfInstances(numInstances);
        
        //checks whether or not the classifier is multiclass, if that is the case it uses numerical integration by default
        if(margin.getNlabs()>2){
            closedForm=false;
            isMC=true;            
        }        
        double[] scores= new double[featsperInst.size()];
        Arrays.fill(scores, 0.0);
        //Histoplot.showit(scorest,featsperInst.size());
        
        System.out.println("Working with classifier "+sclass);
        
        CURRENTPARENTESTIMR0=(closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);

        System.out.println("init R "+CURRENTPARENTESTIMR0);
        System.out.println("Number of features" + margin.getNfeats());        ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE);
        columnDataClass.testClassifier(model, AnalyzeLClassifier.TRAINFILE.replace("%S", sclass));
        CURENTPARENTF10=ColumnDataClassifier.macrof1;
        if(!sclass.equals(CNConstants.ALL))
            CURENTPARENTF10=columnDataClass.fs.get(sclass);    
        
        //List<Double> sameWeights=margin.getOrWeights(0);       
        double[][] sameWeights = margin.getWeights();
        float partiSize = (float) sameWeights.length/numberOfThreads;
        
        
        int partSize= Math.round(partiSize);
        MultiCoreFSCoordinateDesc mthread = new MultiCoreFSCoordinateDesc(numberOfThreads,niters, closedForm, isMC,numIntIters);
        double[][] allfeats = new double[margin.getNfeats()][margin.getNlabs()];
        
        for(int i=0; i<margin.getNfeats(); i++)
            Arrays.fill(allfeats[i], 0.0);
        for(int i=0; i<numberOfThreads; i++){
            try{
            String binaryFile = MODELFILE;
            binaryFile=binaryFile.replace("%S", sclass)+".Thread_"+i;
            File mfile = new File(MODELFILE.replace("%S", sclass));
            File thrfile = new File(binaryFile);
            
            Files.copy(mfile.toPath(), thrfile.toPath(),StandardCopyOption.REPLACE_EXISTING);
            LinearClassifier modelThr = loadModelFromFile(binaryFile);   
            Margin marginThr = new Margin(modelThr);
            marginThr.setBinaryFile(binaryFile);
            marginThr.setWeights(margin.getWeights());
            marginThr.copySharedyInfoParallelGrad(margin);
            
            //copy the weights
            int initPart=i*partSize;
            marginThr.setSubListOfFeats(0,initPart, initPart+partSize);
            parallelGrad.put(i,marginThr);
            
            mthread.getWrapper().put(new Pair<>(i, marginThr));
                
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }
        mthread.getWrapper().join();
        while (mthread.getWrapper().peek()) {
            Pair<Integer, Double> result = mthread.getWrapper().poll();   
            //search for the margins and combine the features
            int thrId = result.first();
            Margin mThr=parallelGrad.get(thrId);
            
            for(int i=0; i<mThr.getSubListOfFeats(0).size();i++){
                int orIdx = mThr.getOrWeightIndex(i);
                allfeats[orIdx]=mThr.getPartialWeight(i);
            }
        }
        //Final weights
        if(margin.areSameWeights(allfeats))
            System.out.println("same weights");
        else
            System.out.println("different weights");
        
        margin.setWeights(allfeats);
        float estimrFinal=(closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);
        System.out.println("Final R : "+ estimrFinal);
        double f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
        File mfile = new File(MODELFILE.replace("%S", sclass));
        try {
            IOUtils.writeObjectToFile(model, mfile);
        } catch (IOException ex) {

        }               
        
   } 

   
   /**
     * The gradient method used ins the Finite Difference
     * f'(a) is approximately (f(a+h)-f(a))/h
     * @param sclass 
     */ 
   public void wkSupClassifierConstr(String sclass, boolean closedForm, int niters) {
       CURRENTSETCLASSIFIER=sclass;
        PlotAPI plotR = new PlotAPI("R vs Iterations","Num of Iterations", "R");
        PlotAPI plotF1 = new PlotAPI("F1 vs Iterations","Num of Iterations", "F1");
        
        boolean isMC=false;
        int numIntIters=100;
        
        final float eps = 0.1f;   
        int counter=0;
        //train the classifier with a small set of train files
        trainOneNERClassifier(sclass,false);  
        LinearClassifier model = modelMap.get(sclass);
        Margin margin = marginMAP.get(sclass);
        CURRENTPARENTMARGIN=margin;
        int selectedFeats[] = margin.getTopWeights(0.5,50);
        //scan the test instances for train the gmm
        List<List<Integer>> featsperInst = new ArrayList<>(); 
        List<Integer> labelperInst = new ArrayList<>(); 
        getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
        margin.setFeaturesPerInstance(featsperInst);
        margin.setLabelPerInstance(labelperInst);
        margin.setNumberOfInstances(numInstances);
        
        //checks whether or not the classifier is multiclass, if that is the case it uses numerical integration by default
        if(margin.getNlabs()>2){
            ErrorsReporting.report("This method works only for binary constrained classifiers");
        }        
        double[] scores= new double[featsperInst.size()];
        Arrays.fill(scores, 0.0);
        //Histoplot.showit(scorest,featsperInst.size());
        HashSet<String> emptyfeats = new HashSet<>();
        System.out.println("Working with classifier "+sclass);
        
        float estimr0=(closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);


        System.out.println("init R "+estimr0);
        //plotR.addPoint(counter, estimr0);
        double f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
        double f1trainOr=testingClassifier(model,TRAINFILE.replace("%S", sclass));
        //plotF1.addPoint(counter,f1);

        System.out.println("Number of features" + margin.getNfeats());
        for (int iter=0;iter<niters;iter++) {
            double[][] weightsForFeat=margin.getWeights();
            final float[] gradw = new float[weightsForFeat.length];
            //for(int i=0;i<weightsForFeat.length;i++){
            for(int i=0;i<selectedFeats.length;i++){
                int featIdx = selectedFeats[i];
                
                float w0 = (float) weightsForFeat[featIdx][0];
//                    if(iter==0)
//                        w0 = (float) Math.random(); //set initial weights randomly                   
                if (emptyfeats.contains("["+i+","+0+"]")) continue;
                float delta = 0.5f;
                /*for (int j=0;;j++) {
                    if(j>10)
                        break;*/

                    weightsForFeat[featIdx][0] = w0 + w0*delta;
                    float estimr = (closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);


                    System.out.println("For feat["+ i +"] weight["+ 0 +"] R estim ["+iter+"] = "+estimr0);    

                    gradw[0] = (estimr-estimr0)/(w0*delta);
                    System.out.println("grad "+gradw[0]);
                    // we don't go above 10 because some weights may not be used at all
                    /*if (gradw[w]==0 && delta<10) delta*=2f;
                    else if (gradw[w]>0.1||gradw[w]<-0.1) delta/=2f;
                    else break;*/
                //}

                weightsForFeat[featIdx][0]=w0;    

                if (gradw[0]==0) 
                        emptyfeats.add("["+i+","+0+"]");
                else{  
                    weightsForFeat[featIdx][0] -= gradw[0] * eps;
                    weightsForFeat[featIdx][1] = -weightsForFeat[featIdx][0];
                    }
                double f1train=testingClassifier(model,TRAINFILE.replace("%S", sclass));  
                if(f1train<f1trainOr){
                   weightsForFeat[featIdx][0]=w0; 
                   weightsForFeat[featIdx][1]=-w0;
                }
                /*
                for(int w=0;w < weightsForFeat[0].length;w++){ 
                    weightsForFeat[0][w]= Math.random();
                    weightsForFeat[1][w]= Math.random();
                }*/
                counter++;
                estimr0 =(closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);
                System.out.println("*******************************"); 
                System.out.println("R estim ["+iter+"] = "+estimr0);     
                plotR.addPoint(counter, estimr0);
                System.out.println("*******************************");
                model.setWeights(weightsForFeat);
                f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
                plotF1.addPoint(counter, f1);
                System.out.println("*******************************"); 

                Histoplot.showit(margin.getScoreForAllInstancesLabel0(featsperInst,scores), featsperInst.size());
                //save the model regularly
                if(iter%30==0){
                    File mfile = new File(MODELFILE.replace("%S", sclass));
                    try {
                        IOUtils.writeObjectToFile(model, mfile);
                    } catch (IOException ex) {

                    }
               }            
            }

        }
        for(String emptyW:emptyfeats){
            System.out.println(emptyW);
        }
        
   }  
   public void chekingUnsupClassifierNInt(String sclass, boolean closedForm) {
        OutputStreamWriter fout = null;
         boolean isMC=true;        
        try {
            

            String data="datamc=[\n"; 
            
            if(closedForm){
                fout = new OutputStreamWriter(new FileOutputStream("analysis/EMNLPExps/ImpactCFAccOrW.m"),CNConstants.UTF8_ENCODING);
                data="datacf=[\n";
            }else
            fout = new OutputStreamWriter(new FileOutputStream("analysis/EMNLPExps/ImpactMCIntAcc_Init2.m"),CNConstants.UTF8_ENCODING);
            //train the classifier with a small set of train files
            trainOneNERClassifier(sclass,false);  
            LinearClassifier model = modelMap.get(sclass);
            Margin margin = marginMAP.get(sclass);
            CURRENTPARENTMARGIN=margin;
            double[][] orWeights=  new double[margin.getWeights().length][];                           
            
            List<List<Integer>> featsperInst = new ArrayList<>(); 
            List<Integer> labelperInst = new ArrayList<>(); 
            getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
            margin.setFeaturesPerInstance(featsperInst);
            margin.setLabelPerInstance(labelperInst); 
            double[] scores= new double[featsperInst.size()];
            Arrays.fill(scores, 0.0);
            
            for(int ninti=1; ninti<10000;ninti+=100){

                
                final int niters = 1;
                final float eps = 0.1f;   
                int counter=0;

                /*
                if(ninti==1){
                    for(int l=0; l< orWeights.length;l++)
                        orWeights[l]=Arrays.copyOf(margin.getWeights()[l],margin.getWeights()[l].length);
                }else{
                    //retrieves the original weights
                    margin.updateWeights(orWeights);
                }*/
                /*int selectedFeats[] = margin.getTopWeights();*/
                //scan the test instances for train the gmm


                //Histoplot.showit(scorest,featsperInst.size());
                HashSet<String> emptyfeats = new HashSet<>();
                System.out.println("Working with classifier "+sclass);
                float estimr0=(closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,ninti);

                double f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
                /*
                    System.out.println("init R "+estimr0);
                    //plotR.addPoint(counter, estimr0);
                   
                    //plotF1.addPoint(counter,f1);
                */

                data+=estimr0+",";
                data+=f1+",";
                double time=elapsedTime/1000000.0;
                data+=(time)+";\n";
                fout.append(data);
                fout.flush();              
                data="";
                System.out.println("Number of features" + margin.getNfeats());
                /*
                for (int iter=0;iter<niters;iter++) {
                    double[][] weightsForFeat=margin.getWeights();
                    

                    final float[] gradw = new float[weightsForFeat.length];
                    //for(int i=0;i<weightsForFeat.length;i++){
                    
                    for(int i=0;i<selectedFeats.length;i++){

                    //for(int i=0;i<5;i++){
                        int featIdx = selectedFeats[i];
                        

                        for(int w=0;w < weightsForFeat[featIdx].length;w++){
                            float w0 = (float) weightsForFeat[featIdx][w];
                            if (emptyfeats.contains("["+i+","+w+"]")) continue;
                            float delta = 0.5f;
                
                                weightsForFeat[featIdx][w] = w0 + w0*delta;
              
                                //TODO:updating the new weights in the gmm?
                                float estimr = (closedForm)?computeROfTheta(sclass):computeROfThetaNumInt(sclass, isMC,ninti);

                                System.out.println("For feat["+ i +"] weight["+ w +"] R estim ["+iter+"] = "+estimr0);    
                                gradw[w] = (estimr-estimr0)/(w0*delta);
                                System.out.println("grad "+gradw[w]);
                
                            weightsForFeat[featIdx][w]=w0;    
                        }
                        
                        for(int w=0;w < weightsForFeat[featIdx].length;w++){ 
                            if (gradw[w]==0) 
                                    emptyfeats.add("["+i+","+w+"]");
                            else    
                                weightsForFeat[featIdx][w] -= gradw[w] * eps;

                        }
                
                        counter++;
                        
                        estimr0 =(closedForm)?computeROfTheta(sclass):computeROfThetaNumInt(sclass,isMC,ninti);
                        System.out.println("TIME: "+elapsedTime);
                        model.setWeights(weightsForFeat);
                        f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
                              
                        //save the model regularly
                        if(iter%30==0){
                            File mfile = new File(MODELFILE.replace("%S", sclass));
                            try {
                                IOUtils.writeObjectToFile(model, mfile);
                            } catch (IOException ex) {

                            }
                       }            
                    }
                    System.out.println("*******************************"); 
                    System.out.println("R estim ["+iter+"] = "+estimr0);     
                    
                    System.out.println("*******************************");
                    //data+=estimr0+","+f1+";\n";
                    
                    System.out.println("*******************************");                 
                    data+=estimr0+",";
                    data+=f1+",";
                    double time=elapsedTime/1000000.0;
                    data+=(time)+";\n";
                    fout.append(data);
                    fout.flush();
                    data="";  
                }
               
                for(String emptyW:emptyfeats){
                    System.out.println(emptyW);

                }*/



            }
            data+="];";
            
            fout.append(data);
            
            fout.flush();
            fout.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                fout.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
   }      
   /**
    * Implements the parallel coordinate descent 
    * @param sclass
    * @param closedForm 
    */
   public void checkingRvsTheta(String sclass, boolean closedForm) {
       CURRENTSETCLASSIFIER=sclass;
        OutputStreamWriter fout = null;
        try {

            boolean isMC=false;
            int numIntIters=100;
            fout = new OutputStreamWriter(new FileOutputStream("analysis/EMNLPExps/Rvstheta.m"),CNConstants.UTF8_ENCODING);

            //train the classifier with a small set of train files
            trainOneNERClassifier(sclass,false);
            LinearClassifier model = modelMap.get(sclass);
            Margin margin = marginMAP.get(sclass);
            CURRENTPARENTMARGIN=margin;
            //int selectedFeats[] = margin.getTopWeights();
            //scan the test instances for train the gmm
            List<List<Integer>> featsperInst = new ArrayList<>();
            List<Integer> labelperInst = new ArrayList<>();
            getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
            margin.setFeaturesPerInstance(featsperInst);
            margin.setLabelPerInstance(labelperInst);
            double[] scores= new double[featsperInst.size()];
            Arrays.fill(scores, 0.0);
            
            
           
            System.out.println("Working with classifier "+sclass);
            System.out.println("Number of features" + margin.getNfeats());
            double[][] weightsForFeat=margin.getWeights();
            //for(int i=0;i<weightsForFeat.length;i++){
            int selectedFeats[] = margin.getTopWeights(0.5,50);
            for(int i=0;i<10;i++){
                String wvec="w=[";
                String rvec="r=[";
                int featIdx = selectedFeats[i];
                //int featIdx=i;
                //for(int w=0;w < weightsForFeat[featIdx].length;w++){
                float w0 = (float) weightsForFeat[featIdx][0];
                wvec+=w0+";\n";
                float r = (closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);
                rvec+=r+";\n";
                for(int k=0;k<100;k++){
                    System.out.println("INCREASING w "+k);
                    float w=w0+0.05f;
                    weightsForFeat[featIdx][0]=w;
                    wvec+=w+";\n";
                    r = (closedForm)?computeROfTheta():computeROfThetaNumInt(isMC,numIntIters);
                    rvec+=r+";\n";
                    w0=w;
                }    
                wvec+="];";
                rvec+="];";   
                fout.append(wvec);
                fout.append(rvec);
                fout.flush();
                        
                //}

                System.out.println("*******************************"); 

                   
             
                }
            fout.close();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                fout.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
   }      
   
   
   public void evaluationPOSTAGGER(){
        BufferedReader testFile = null;
        try {
            testFile = new BufferedReader(new InputStreamReader(new FileInputStream(TESTFILE.replace("%S", CNConstants.PRNOUN)), CNConstants.UTF8_ENCODING));
            int tp=0, tn=0, fp=0, fn=0;
            
            for(;;){

                String line = testFile.readLine();   
                
                if(line== null)
                    break;                
                
                String values[] = line.split("\\t");
                String label = values[0];
                String pos = values[2];
                
                if(pos.equals(CNConstants.POSTAGNAM) && label.equals(CNConstants.PRNOUN))
                    tp++;
                
                
                if(pos.equals(CNConstants.POSTAGNAM)&& label.equals(CNConstants.NOCLASS))
                    fp++;
                    
                 
                
                if(!pos.equals(CNConstants.POSTAGNAM) &&label.equals(CNConstants.PRNOUN))
                    fn++;
                    
                
                if(!pos.equals(CNConstants.POSTAGNAM) && label.equals(CNConstants.NOCLASS))
                    tn++;
                

            }
            double precision= (double) tp/(tp+fp);
            double recall= (double) tp/(tp+fn);
            double f1=(2*precision*recall)/(precision+recall);
            
            System.out.println(" POSTAG PN precision: "+precision);
            System.out.println(" POSTAG PN recall: "+recall);
            System.out.println(" POSTAG PN f1: "+f1);
 

            
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


    public static void evaluationCLASSRESULTS(String goalClass,String filename){

        BufferedReader testFile = null;
        try {
            testFile = new BufferedReader(new InputStreamReader(new FileInputStream(filename), CNConstants.UTF8_ENCODING));
            
            int tp=0, tn=0, fp=0, fn=0;
            
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
                
                if(recognizedLabel.equals(goalClass) && label.equals(goalClass))
                    tp++;
                
                if(recognizedLabel.equals(goalClass)&& label.equals(CNConstants.NOCLASS))
                    fp++;
                
                if(recognizedLabel.equals(CNConstants.NOCLASS)&&label.equals(goalClass))
                    fn++;
                if(recognizedLabel.equals(CNConstants.NOCLASS)&&label.equals(CNConstants.NOCLASS))
                    tn++;

                
            }
            double precision= (double) tp/(tp+fp);
            double recall= (double) tp/(tp+fn);
            double f1=(2*precision*recall)/(precision+recall);
            
            System.out.println("  PN precision: "+precision);
            System.out.println("  PN recall: "+recall);
            System.out.println("  PN f1: "+f1);
            
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
    public static void evaluationCLASSRESULTS(String goalClass,String backgrounSymb,String filename){

        BufferedReader testFile = null;
        try {
            testFile = new BufferedReader(new InputStreamReader(new FileInputStream(filename), CNConstants.UTF8_ENCODING));
            
            int tp=0, tn=0, fp=0, fn=0;
            
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
                
                if(recognizedLabel.equals(goalClass) && label.equals(goalClass))
                    tp++;
                
                if(recognizedLabel.equals(goalClass)&& label.equals(backgrounSymb))
                    fp++;
                
                if(recognizedLabel.equals(backgrounSymb)&&label.equals(goalClass))
                    fn++;
                if(recognizedLabel.equals(backgrounSymb)&&label.equals(backgrounSymb))
                    tn++;

                
            }
            double precision= (double) tp/(tp+fp);
            double recall= (double) tp/(tp+fn);
            double f1=(2*precision*recall)/(precision+recall);
            
            System.out.println("  PN precision: "+precision);
            System.out.println("  PN recall: "+recall);
            System.out.println("  PN f1: "+f1);
            
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

    
      public void evaluationKMEANS(){
        BufferedReader testFile = null;
        BufferedReader kmfile = null;
        try {
            testFile = new BufferedReader(new FileReader(TESTFILE.replace("%S",CNConstants.PRNOUN))); 
            kmfile = new BufferedReader(new InputStreamReader(new FileInputStream("/home/rojasbar/development/contnomina/kmeans/kmresults.mat"), CNConstants.UTF8_ENCODING));
            int tp=0, tn=0, fp=0, fn=0;
            String line = ""; 
            for(;;){
                 
                String km = kmfile.readLine();  
                if(line== null)
                    break;    
                
                if(km==null)
                    break;
                
                 
                if(km.startsWith("#"))
                    continue;
                else
                    line = testFile.readLine();   
                
                String values[] = line.split("\\t");
                String label = values[0];
                String recognizedLabel = km.trim();
                
                if(recognizedLabel.equals("1") && label.equals(CNConstants.PRNOUN))
                    tp++;
                
                if(recognizedLabel.equals("1")&& label.equals(CNConstants.NOCLASS))
                    fp++;
                
                if(recognizedLabel.equals("2")&&label.equals(CNConstants.PRNOUN))
                    fn++;
                if(recognizedLabel.equals("2")&&label.equals(CNConstants.NOCLASS))
                    tn++;

            }
            double precision= (double) tp/(tp+fp);
            double recall= (double) tp/(tp+fn);
            double f1=(2*precision*recall)/(precision+recall);
            
            System.out.println("  PN precision: "+precision);
            System.out.println("  PN recall: "+recall);
            System.out.println("  PN f1: "+f1);
            
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

   
   public void analyzingEvalFile(){
        try {
            PrintWriter fout = FileUtils.writeFileUTF("analysis/EMNLPExps/ErrorAnalysis500Iters.txt");
            BufferedReader fin = new BufferedReader(new FileReader("analysis/EMNLPExps/ResultsProposedClassifier500Iters.txt"));
            String outline="";
            for (;;) {
                    String line = fin.readLine();
                    if (line==null) break; 
                    Pattern p = Pattern.compile("pn\t.*\tNOM\t");
                    Matcher m = p.matcher(line);
                    
                    if(m.find())
                        outline=line;
                    
                    if(line.contains("Total:")){
                        String vals = line.substring(line.indexOf("Total:")+6);
                        vals=vals.trim().replaceAll("[\\s]+"," ");
                        String[] probs= vals.split("\\s");
                        
                        double probNO= Double.parseDouble(probs[0]);
                        double probPN= Double.parseDouble(probs[1]);  
                        if(outline.isEmpty())
                            continue;
                        if(probPN>probNO)
                            outline+=" pn ";
                        else
                            outline+=" NO ";
                        
                        fout.println(outline);
                        outline="";
                        
                    }
                    
                    
            }   
            fout.close();
            fin.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
   }
   
   public void printingValuesInOctave(String filename){
    try {
        HashMap<Integer, Double> rMap=readingRiskFromFile(filename,0);
        HashMap<Integer, Double> f1Map=readingF1FromFile(filename,0);
        //HashMap<Integer, Double> rtrIntMap=readingRiskFromFile("analysis/EMNLPExps/outLogAgo292014_TrapezoidInt_To712UnsupIters.txt",0);
        //rtrIntMap.putAll(readingRiskFromFile("analysis/EMNLPExps/outLogSep42014_TrapezoidInt_From712UnsupIters.txt",712));
        //HashMap<Integer, Double> f1trIntMap=readingF1FromFile("analysis/EMNLPExps/outLogAgo292014_TrapezoidInt_To712UnsupIters.txt",0);
        //f1trIntMap.putAll(readingF1FromFile("analysis/EMNLPExps/outLogSep42014_TrapezoidInt_From712UnsupIters.txt",712));
        PrintWriter fout = FileUtils.writeFileUTF("analysis/StochGrad/checkingRandF1.m");
        
        String r="r=[\n";
        String f1="f1=[\n";
        //String fcf="FCF=[\n";
        for(int i=0;i<f1Map.size();i++){
            r+=rMap.get(i)+";\n";
            f1+=f1Map.get(i)+";\n";
            //fcf+=f1cfFirstMap.get(i)+";\n";
            
        }
        r+="];";
        f1+="];";
        //fcf+="];";
                
        fout.println(r);
        fout.println(f1);
        //fout.println(fcf);
        
        fout.println("");       
        fout.close();
           
       }catch (Exception ex) {
            ex.printStackTrace();
        }
             
   }

   public void comparingNumIntVsClosedF(){
       try {           
            HashMap<Integer, Double> rcfMap=readingRiskFromFile("analysis/EMNLPExps/outLogAnalInt.txt",0);
            HashMap<Integer, Double> rniMap=readingRiskFromFile("analysis/EMNLPExps/outLogNumIntIter0to640.txt",0);
            rniMap.putAll(readingRiskFromFile("analysis/EMNLPExps/outLogNumInterIter640.txt",640));
            PrintWriter fout = FileUtils.writeFileUTF("analysis/EMNLPExps/comparingR.m");
            fout.println("RCF=[");
            for(int i=0;i<2336;i++){
                fout.println(rcfMap.get(i)+";");
            }
            fout.println("]");
            fout.println("");
            fout.println("RNI=[");
            for(int i=0;i<2000;i++){
                fout.println(rniMap.get(i)+";");
            }
            
            fout.println("]");          
            fout.close();
           
       }catch (Exception ex) {
            ex.printStackTrace();
        }
   }

private HashMap<Integer, Double> readingF1FromFile(String filename, int startIdx){
       HashMap<Integer, Double> f1ValMap=new HashMap<>();
       double f1=0.0;
 try {
           BufferedReader ifile = new BufferedReader(new FileReader(filename));
           
           int numOfEx=500;
           int nline=0;
           for (;;) {
                String line = ifile.readLine();
                if (line==null) break;
                
                if(line.contains(" examples in test set")){
                    Pattern p = Pattern.compile("(\\d+)");    
                    Matcher m = p.matcher(line);
                    if(m.find()){
                        try{
                        numOfEx= Integer.parseInt(m.group(0));
                        }catch(Exception ex){
                            System.out.println("ERRORR ==== "+ line);
                        }        
                     }                   
                    
                    
                }
                
                if(line.startsWith("Cls pn")){
                    if(numOfEx <500)
                        continue;                    
                    int initF1=line.indexOf("F1 ");
                    try{
                        f1= Double.parseDouble(line.substring(initF1+3));
                        
                    }catch(NumberFormatException ex){
                        
                    }
                }
                
                if(!line.startsWith("R["))
                    continue;
                Pattern p = Pattern.compile("(\\d+)");  
                Matcher m = p.matcher(line);
                if(m.find()){
                    int idx= Integer.parseInt(m.group(0));
                    f1ValMap.put(idx, f1);
                }
                nline++;
                
                
           }
           ifile.close();
           return f1ValMap;
           
       }catch (Exception ex) {
            ex.printStackTrace();
            return f1ValMap;
        }      
   }
   
   
private HashMap<Integer, Double> readingRiskFromFile(String filename, int startIdx){
       HashMap<Integer, Double> rValsMap=new HashMap<>();
 try {
           BufferedReader ifile = new BufferedReader(new FileReader(filename));
           
           
           for (;;) {
                String line = ifile.readLine();
                if (line==null) break;
                
                if(line.startsWith("Cls pn")){
                    int initF1=line.indexOf("F1 ");
                    try{
                        double f1= Double.parseDouble(line.substring(initF1+1));
                        
                    }catch(NumberFormatException ex){
                        
                    }
                }
                
                //if(!line.startsWith("R estim ["))
                if(!line.startsWith("R["))
                    continue;
                
                int initIdx= line.indexOf("[");
                int ednIdx= line.indexOf("]");
                
                int idx=-1;
                try{
                    idx= Integer.parseInt(line.substring(initIdx+1, ednIdx))+startIdx;
                }catch(Exception nex){
                    System.out.println("ERRORR --------"+line);
                    continue;
                }    
                int equal= line.indexOf("= ")+2;
                String strval=line.substring(equal);
                System.out.println(idx+"-"+strval);
                try{
                    double value= Double.parseDouble(strval);
                    rValsMap.put(idx, value);
                }catch(NumberFormatException ex){
                    
                }
                
                
           }
           ifile.close();
           return rValsMap;
           
       }catch (Exception ex) {
            ex.printStackTrace();
            return rValsMap;
        }      
   }

    private void serializingFeatures(HashMap vocFeats, boolean isTrain){
    try{
            String fileName="";
            if(isTrain)
                fileName="StanfordLCTrainfeaturesDict.ser";
            else
                fileName="StanfordLCTestfeaturesDict.ser";
            FileOutputStream fileOut =
            new FileOutputStream(fileName);
            ObjectOutputStream out =
              new ObjectOutputStream(fileOut);
            out.writeObject(vocFeats);
            out.close();
            fileOut.close();
        }catch(Exception i)
        {
            i.printStackTrace();
        }
    }
    
    public HashMap<Integer,List<String>> deserializingFeatures(boolean isTrain){
      try
      {
        HashMap<Integer,List<String>> vocFeats = new HashMap<>();
        String fileName="";
        if(isTrain)
            fileName="StanfordLCTrainfeaturesDict.ser";
        else
            fileName="StanfordLCTestfeaturesDict.ser";          
        FileInputStream fileIn =  new FileInputStream(fileName);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        vocFeats = (HashMap<Integer,List<String>>) in.readObject();
        System.out.println("vocabulary of features: "+vocFeats.size());
        /*
        for(Integer key:vocFeats.keySet()){
            System.out.println("feature id "+ vocFeats.get(key)+" value : "+ key);

        }*/
              
         in.close();
         fileIn.close();
         return vocFeats;
      }catch(IOException i)
      {
         i.printStackTrace();
         return new HashMap<>();
      }catch(ClassNotFoundException c)
      {
         System.out.println("class not found");
         c.printStackTrace();
         return new HashMap<>();
      } 
   
    }   

    private void serializingFeatsPerInstance(boolean isTrain){
    try{
            String featPerInst="";
            String instPerFeat="";
            String lblPerInst="";
            if(isTrain){
                featPerInst="StanfordLCTrainFeaturePerInstance.ser";
                instPerFeat="StanfordLCTrainInstancePerFeature.ser";
                lblPerInst="StanfordLCTrainLabelPerInstance.ser";
            }else{
                featPerInst="StanfordLCTestFeaturePerInstance.ser";
                instPerFeat="StanfordLCTestInstancePerFeature.ser";
                lblPerInst="StanfordLCTestLabelPerInstance.ser";
            }    
            FileOutputStream fileOut =  new FileOutputStream(featPerInst);
            ObjectOutputStream out =  new ObjectOutputStream(fileOut);
            out.writeObject(featperInstance);
            out.close();
            fileOut.close();
            fileOut =  new FileOutputStream(instPerFeat);
            out =  new ObjectOutputStream(fileOut);
            out.writeObject(instPerFeatures);
            out.close();
            fileOut.close();             
            fileOut =  new FileOutputStream(lblPerInst);
            out =  new ObjectOutputStream(fileOut);
            out.writeObject(lblperInstance);
            out.close();
            fileOut.close();            
        }catch(Exception i)
        {
            i.printStackTrace();
        }
    }
   
    public void deserializingFeatsPerInstance(boolean isTrain){
      try
      {
        
        String fileName="";
        String instPerFeat="";
        String lblPerInst="";
        if(isTrain){
            fileName="StanfordLCTrainFeaturePerInstance.ser";
            instPerFeat="StanfordLCTrainInstancePerFeature.ser";
            lblPerInst="StanfordLCTrainLabelPerInstance.ser";
            
        }else{
            fileName="StanfordLCTestFeaturePerInstance.ser";  
            instPerFeat="StanfordLCTestInstancePerFeature.ser";
            lblPerInst="StanfordLCTestLabelPerInstance.ser";
        }    
        FileInputStream fileIn =  new FileInputStream(fileName);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        featperInstance = (List<List<Integer>>) in.readObject();
        System.out.println("loading features per instance: "+featperInstance.size());
        in.close();
        fileIn.close();
        fileIn =  new FileInputStream(instPerFeat);
        in = new ObjectInputStream(fileIn);
        instPerFeatures = (HashMap<Integer,List<Integer>>) in.readObject();
        System.out.println("loading labels per instance: "+instPerFeatures.size());
        in.close();
        fileIn.close();        
        fileIn =  new FileInputStream(lblPerInst);
        in = new ObjectInputStream(fileIn);
        lblperInstance = (List<Integer>) in.readObject();
        System.out.println("loading labels per instance: "+lblperInstance.size());
        in.close();
        fileIn.close();      
      }catch(IOException i)
      {
         featperInstance=new ArrayList<>();
         
      }catch(ClassNotFoundException c)
      {
         System.out.println("class not found");
         c.printStackTrace();
        
      } 
   
    } 
    
    
    
    public void generatingArffData(boolean istrain){
        
        //HashMap<Integer,List<String>> trainfeats= deserializingFeatures(true);
        HashMap<String,Integer> dictFeatures=new HashMap<>();
        HashMap<Integer,List<String>> feats=deserializingFeatures(istrain);
        BufferedReader infile = null;
        
        OutputStreamWriter outFile=null;
        try {
        if(istrain){  
            infile = new BufferedReader(new FileReader(TRAINFILE.replace("%S",CNConstants.PRNOUN)));
            outFile = new OutputStreamWriter(new FileOutputStream(TRAINFILE.replace("%S", CNConstants.PRNOUN)+".arff"),CNConstants.UTF8_ENCODING);
        }else{
            infile = new BufferedReader(new FileReader(TESTFILE.replace("%S",CNConstants.PRNOUN))); 
            outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", CNConstants.PRNOUN)+".arff"),CNConstants.UTF8_ENCODING);
        }

         
         outFile.append("@relation ner\n@attribute entity {pn, NO} \n@attribute word numeric \n@attribute postag numeric\n@attribute wshape numeric\n@attribute ngrams numeric"
                + "\n\n@data\n");
         
         
        /*
         outFile.append("@relation ner\n@attribute word numeric \n@attribute postag numeric\n@attribute wshape numeric\n@attribute ngrams numeric"
                + "\n\n@data\n");        */
        outFile.flush();
        int linecounter=1;
        
            for (;;) {
                String line = infile.readLine();
                 if (line==null) break;
                String[] stdata=line.split("\t");
                String categ= stdata[0];
                String word= stdata[1];
                if(!dictFeatures.containsKey(word))
                    dictFeatures.put(word,dictFeatures.size()+1);
                String pos= stdata[2];
                if(!dictFeatures.containsKey(pos))
                    dictFeatures.put(pos,dictFeatures.size()+1);
                List<String> restfeats= feats.get(linecounter);
                String wshape="";
                String lngram="";
                for(String feat:restfeats){
                    if(feat.contains("SHAPE"))
                        wshape="["+feat.substring(feat.indexOf("SHAPE")+5)+"]";
                    if(feat.contains("1-#"))
                        lngram+="["+feat.substring(feat.indexOf("1-#")+5)+"] ";
                                
                }
                if(!dictFeatures.containsKey(wshape.trim()))
                    dictFeatures.put(wshape.trim(),dictFeatures.size()+1);
                if(!dictFeatures.containsKey(lngram.trim()))
                    dictFeatures.put(lngram.trim(),dictFeatures.size()+1);
                linecounter++;
                outFile.append(categ+","+dictFeatures.get(word)+","+dictFeatures.get(pos)+","+dictFeatures.get(wshape.trim())+","+dictFeatures.get(lngram.trim())+"\n");
                outFile.flush();
            }
            
            outFile.close();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                infile.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void organizingREPEREFiles(){
        BufferedReader infile = null;
        BufferedReader currTestList = null;
        BufferedReader intrainlist = null;
        BufferedReader intestlist = null;
        OutputStreamWriter outFile=null;
        HashMap<String,String> exactTest= new HashMap<>();
        try{
            
            infile = new BufferedReader(new FileReader("exactRepereFiles.txt"));
            String targetDir="";
            for(;;){
                String line=infile.readLine();
                if(line == null)
                    break;
                if(line.equals(""))
                    continue;
                
                if(line.startsWith("Dev"))
                    targetDir="dev";
                else if(line.startsWith("Test"))
                    targetDir="test";                
                else{
                    if(targetDir.equals("test"))
                        exactTest.put(line,line);
                    String trainfile="/global/rojasbar/nasdata1/TALC/ExternalResources/NEW_RESOURCES/REPERE/data/reference/train/"+line+".trs";
                    File file = new File(trainfile);
                    File targetFile = new File(targetDir+"/"+line+".xml");
                    if(file.exists()){
                        System.out.println("File in "+targetDir +" == "+ line +" found in the train directory");
                        //Files.move(file.toPath(), targetFile.toPath(),StandardCopyOption.ATOMIC_MOVE);
                    }else{
                        String testfile="/global/rojasbar/nasdata1/TALC/ExternalResources/NEW_RESOURCES/REPERE/data/reference/test/"+line+".trs";
                        file = new File(testfile);
                        if(file.exists()){
                            System.out.println("File in "+targetDir +" == "+ line +" found in the test directory");
                            //Files.move(file.toPath(), targetFile.toPath(),StandardCopyOption.ATOMIC_MOVE);                            
                        }else{
                            String devfile="/global/rojasbar/nasdata1/TALC/ExternalResources/NEW_RESOURCES/REPERE/data/reference/dev/"+line+".trs";
                            file = new File(devfile);
                            if(file.exists()){
                                System.out.println("File in "+targetDir +" == "+ line +" found in the dev directory");
                                //Files.move(file.toPath(), targetFile.toPath(),StandardCopyOption.ATOMIC_MOVE);                            
                            }else                           
                                System.out.println("File in "+targetDir +" == "+ line +" not found!!!");
                        }    
                    }
                }
                
            }
            currTestList = new BufferedReader(new FileReader("currtest.txt"));
            HashMap<String,String> currTest = new HashMap<>();
            for(;;){
                String line=currTestList.readLine();
                if(line == null)
                    break;
                if(line.equals(""))
                    continue;  
                currTest.put(line,line);
                if(!exactTest.containsKey(line)){
                  System.out.println("file "+ line +" in the current test is not in the exact test");  
                }
            }
            
            for(String key:exactTest.keySet()){
                if(!currTest.containsKey(key)){
                    System.out.println("file "+ key +" in the exact test is not in the current test");  
                }
            }
            
            intestlist = new BufferedReader(new FileReader("orReplist.xmll"));
            intrainlist = new BufferedReader(new FileReader("reptrain.xmll"));
            List<String> correctTrainFiles= new ArrayList<>();
            /*
            for(;;){
                String line=intrainlist.readLine();
                if(line == null)
                    break;
                if(line.equals(""))
                    continue;                
                correctTrainFiles.add(line);
            }*/
            int linenumber=0;
            for(;;){
                String line=intestlist.readLine();
                if(line == null)
                    break;
                if(line.equals(""))
                    continue; 
                
                //String trainfile=line.replace("reptest","reptrain");
                String trainfile="/global/rojasbar/nasdata1/TALC/ExternalResources/NEW_RESOURCES/REPERE/data/reference/train/"+line.replace("reptest/", "");
                File file = new File(trainfile); 
                
                if(file.exists()){
                    correctTrainFiles.remove(trainfile);
                    System.out.println("file "+ targetDir+"=="+line +" found in train:"+file.getAbsolutePath());
                    //Files.delete(file.toPath());
                }
                linenumber++;
                
            }
            /*
            outFile = new OutputStreamWriter(new FileOutputStream("reptrain.xmll"),CNConstants.UTF8_ENCODING);
            for(int i=0; i<correctTrainFiles.size();i++){
                outFile.append(correctTrainFiles.get(i)+"\n");
                outFile.flush();
            }*/
            outFile.close();
            infile.close();
            intrainlist.close();
            intestlist.close();       
                    
        }catch(Exception ex){

        }
    }

    public HashMap<Integer,Margin> getThreadPartitioning(){
        return this.parallelGrad;
    }
    /**
     * 
     * Trains on all data, train and test but keeps only the weights of the little train data, the weights for all the other features
     * (testset) are set to zero
     * @param entity
     * @param isSavingFiles
     * @param isWiki
     * @param isLower 
     */
    public void allweightsKeepingOnlyTrain(String entity, int trainSize, boolean isSavingFiles, boolean isWiki, boolean isLower){
        
        AnalyzeLClassifier.TRAINSIZE=trainSize;

        if(isSavingFiles){
            saveGroups(entity,true,isWiki, isLower);
            saveGroups(entity,false,isWiki, isLower);
        }   
 
        File trainSet = new File(AnalyzeLClassifier.TRAINFILE.replace("%S", entity));
        File testSet = new File(AnalyzeLClassifier.TESTFILE.replace("%S", entity));
        String allTrainAndTest=AnalyzeLClassifier.TRAINFILE.replace("%S", entity)+"andtest";
        File trainAndTest = new File(allTrainAndTest);          
            
        try {
            String file1Str = org.apache.commons.io.FileUtils.readFileToString(trainSet);
            String file2Str = org.apache.commons.io.FileUtils.readFileToString(testSet);
            // Write the file
            org.apache.commons.io.FileUtils.write(trainAndTest, file1Str);
            org.apache.commons.io.FileUtils.write(trainAndTest, file2Str, true); // true for append
            
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
       //Train all train and test data
        
        String tmpRealTrain=AnalyzeLClassifier.TRAINFILE.replace("%S", entity);

        String realTrainModel=MODELFILE.replace("%S", entity);
        AnalyzeLClassifier.TRAINFILE=allTrainAndTest;
        //AnalyzeLClassifier.MODELFILE="bin.%S.allfeats.lc.mods".replace("%S", entity);
        //AnalyzeLClassifier.exitAfterTrainingFeaturization=true;
        //delete the files if they exist
        File mfile = new File(AnalyzeLClassifier.MODELFILE);
        mfile.delete();
        updatingPropFile(entity, false);
        ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE);   
        GeneralDataset datatr = columnDataClass.readTrainingExamples(tmpRealTrain);        
        LinearClassifier model = null;
        if(!mfile.exists()){
            GeneralDataset dataAll = columnDataClass.readTrainingExamples(TRAINFILE.replace("%S", entity));
            Pair<GeneralDataset, GeneralDataset> splitData= dataAll.split(0, datatr.size());
            model = (LinearClassifier) columnDataClass.makeClassifier(splitData.second);  

            //save the model in a file
            try {
                IOUtils.writeObjectToFile(model, realTrainModel);
            } catch (IOException ex) {

            }

        }else{
            Object object;
            try {
                object = IOUtils.readObjectFromFile(mfile);
                model=(LinearClassifier)object;                  

            } catch (Exception ex) {
                ex.printStackTrace();
            } 

        }        

        //saves the model as train
        modelMap.put(entity, model);
        Margin margin = new Margin(model);
        marginMAP.put(entity,margin);          
        margin.setTrainFeatureSize(datatr.numFeatures());    
        margin.setTestFeatureSize(model.weights().length);
    }
    /**
     * This methods follows the following steps:
     * <ul>
     * <li>It trains the linear classifier on both datasets train and test and we keep
     * all the weights.</li>
     * <li>Then it trains the linear classifier on the trainset only.</li>
     * <li>It updates the weights that are not in the trainset to zero in the set containing all weights (train and test).</li>
     * <li>It updates the weights of the trainset with the values obtained in the second step (train on trainset only).</li>
     * <li>It saves the model as a training model.</li>
     * </ul>
     * @param entity
     * @param trainSize
     * @param testSize 
     */
//    public void allweightsKeepingOnlyTrain(String entity, int trainSize,int testSize, boolean useExistingModels){
//        if (testSize>=0) AnalyzeLClassifier.TESTSIZE=testSize;
//        File trainSet = new File(AnalyzeLClassifier.TRAINFILE.replace("%S", entity));
//        File testSet = new File(AnalyzeLClassifier.TESTFILE.replace("%S", entity));
//        String allTrainAndTest=AnalyzeLClassifier.TRAINFILE.replace("%S", entity)+"andtest";
//        File trainAndTest = new File(allTrainAndTest);        
//        try {
//            String file1Str = org.apache.commons.io.FileUtils.readFileToString(trainSet);
//            String file2Str = org.apache.commons.io.FileUtils.readFileToString(testSet);
//            // Write the file
//            org.apache.commons.io.FileUtils.write(trainAndTest, file1Str);
//            org.apache.commons.io.FileUtils.write(trainAndTest, file2Str, true); // true for append
//            
//            
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//        //Train all train and test data
//        AnalyzeLClassifier.TRAINSIZE=Integer.MAX_VALUE;
//        String tmpRealTrain=AnalyzeLClassifier.TRAINFILE.replace("%S", entity);
//        String realTrainModel=MODELFILE.replace("%S", entity);
//        AnalyzeLClassifier.TRAINFILE=allTrainAndTest;
//        AnalyzeLClassifier.MODELFILE="bin.%S.allfeats.lc.mods".replace("%S", entity);
//        AnalyzeLClassifier.exitAfterTrainingFeaturization=true;
//        //delete the files if they exist
//        File file = new File(AnalyzeLClassifier.MODELFILE);
//        if(!useExistingModels)
//            file.delete();
//        trainAllLinearClassifier(entity,false,false,false);
//        LinearClassifier modelAllFeats = modelMap.get(entity);
//        Margin           marginAllFeats = marginMAP.get(entity);
//        //train only train data
//        AnalyzeLClassifier.exitAfterTrainingFeaturization=false;
//        if (trainSize>=0) AnalyzeLClassifier.TRAINSIZE=trainSize;
//        AnalyzeLClassifier.MODELFILE=realTrainModel;
//        AnalyzeLClassifier.TRAINFILE=tmpRealTrain;
//        file = new File(AnalyzeLClassifier.MODELFILE);
//        if(!useExistingModels)
//            file.delete();        
//        trainAllLinearClassifier(entity,false,false,false);
//        LinearClassifier modelTrainFeats = modelMap.get(entity);
//                
//        Index<String> featIdxs = modelTrainFeats.featureIndex();
//        Index<String> allfeatIdxs = modelAllFeats.featureIndex();
//        List<String> trainFeats = featIdxs.objectsList(); 
//        //List<String> allFeats = new ArrayList(allfeatIdxs.objectsList());
//        List<Integer>testIdx=new ArrayList<>();
//        
//        double[][] weightsAllFeats = marginAllFeats.getWeights();
//        Margin  marginTrainFeats = marginMAP.get(entity);
//        double[][] trainWeights = marginTrainFeats.getWeights();
//        List<Integer> trainIdx=new ArrayList<>();        
//        System.out.println("Before scanning all features: "+allfeatIdxs.size());
//        Long stTime=System.currentTimeMillis();
//        for(String feat:allfeatIdxs){
//            int featIdx=allfeatIdxs.indexOf(feat);
//            if(trainFeats.contains(feat)){
//                //copy the initial weights of the supervised method
//                int trIdx=featIdxs.indexOf(feat);
//                trainIdx.add(featIdx);
//                weightsAllFeats[featIdx]=trainWeights[trIdx];
//            }else{
//                //set all the features in the testset to 0.0
//                for(int i=0; i<weightsAllFeats[featIdx].length;i++){
//                    weightsAllFeats[featIdx][i]=0.0;
//                }
//                testIdx.add(featIdx);
//            }
//            
//        }
//        Long endTime=System.currentTimeMillis(); 
//        long totalTime=endTime-stTime;
//        System.out.println("Total time in milliseconds:"+ totalTime);
//        //saves the model as train
//        AnalyzeLClassifier.TRAINFILE=allTrainAndTest;
//        modelAllFeats.setWeights(weightsAllFeats);
//
//        try {
//            IOUtils.writeObjectToFile(modelAllFeats, realTrainModel);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }  
//   
//        
//        marginAllFeats.setTrainFeatureIndexes(trainIdx);
//        marginAllFeats.setTestFeatureIndexes(testIdx);
//        modelMap.put(entity, modelAllFeats);
//        marginMAP.put(entity, marginAllFeats);
//       
//    } 
    /**
     * Trains on all data, train and test but keeps only the weights of the little train data, the weights for all the other features
     * (testset) are set to zero
     * @param entity
     * @param trainSize
     * @param testSize 
     */    
    public void allweightsKeepingOnlyTrain(String entity, int trainSize,int testSize, boolean useExistingModels){
        if (testSize>=0) 
            AnalyzeLClassifier.TESTSIZE=testSize;
        File trainSet = new File(AnalyzeLClassifier.TRAINFILE.replace("%S", entity));
        File testSet = new File(AnalyzeLClassifier.TESTFILE.replace("%S", entity));
        String allTrainAndTest=AnalyzeLClassifier.TRAINFILE.replace("%S", entity)+"andtest";
        File trainAndTest = new File(allTrainAndTest);        
        try {
            String file1Str = org.apache.commons.io.FileUtils.readFileToString(trainSet);
            String file2Str = org.apache.commons.io.FileUtils.readFileToString(testSet);
            // Write the file
            org.apache.commons.io.FileUtils.write(trainAndTest, file1Str);
            org.apache.commons.io.FileUtils.write(trainAndTest, file2Str, true); // true for append
    
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        //Train all train and test data
        // TODO: all these public static vars should never be set inside methods, but only once by the main app, otherwise, it's hard to know what's going on
        AnalyzeLClassifier.TRAINSIZE=Integer.MAX_VALUE;
        String tmpRealTrain=AnalyzeLClassifier.TRAINFILE.replace("%S", entity);

        String realTrainModel=MODELFILE.replace("%S", entity);
        AnalyzeLClassifier.TRAINFILE=allTrainAndTest;
        //AnalyzeLClassifier.MODELFILE="bin.%S.allfeats.lc.mods".replace("%S", entity);
        //AnalyzeLClassifier.exitAfterTrainingFeaturization=true;
        //delete the files if they exist
        File mfile = new File(AnalyzeLClassifier.MODELFILE);
        if(!useExistingModels)
            mfile.delete();
        updatingPropFile(entity, false);
        ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE);
        // how to be sure that the features index read here are the same than below ?
        // because this train file is the first to appear in the merge train+test file read below ?
        // it's a bit risky, because if both corpora are put in reverse order one day...
        GeneralDataset datatr = columnDataClass.readTrainingExamples(tmpRealTrain);        
        LinearClassifier model = null;
        if(!mfile.exists()){
            GeneralDataset dataAll = columnDataClass.readTrainingExamples(TRAINFILE.replace("%S", entity));
            Pair<GeneralDataset, GeneralDataset> splitData= dataAll.split(0, datatr.size());
            model = (LinearClassifier) columnDataClass.makeClassifier(splitData.second);  

            //save the model in a file
            try {
                IOUtils.writeObjectToFile(model, realTrainModel);
            } catch (IOException ex) {

            }

        }else{
            Object object;
            try {
                object = IOUtils.readObjectFromFile(mfile);
                model=(LinearClassifier)object;                  

            } catch (Exception ex) {
                ex.printStackTrace();
            } 

        }        

        //saves the model as train
        modelMap.put(entity, model);
        Margin margin = new Margin(model);
        marginMAP.put(entity,margin);          
        margin.setTrainFeatureSize(datatr.numFeatures());    
        margin.setTestFeatureSize(model.weights().length);

    }      
    public void evalutatingF1AndR(){
        PlotAPI plotR = new PlotAPI("R vs Iterations","Num of Iterations", "R");
        PlotAPI plotF1 = new PlotAPI("F1 vs Iterations","Num of Iterations", "F1");        
        HashMap<String,Double> priorMap = new HashMap<>();
        priorMap.put(CNConstants.NOCLASS, new Double(0.9));
        priorMap.put(CNConstants.PRNOUN, new Double(0.1));
        setPriors(priorMap);
        ///*
        AnalyzeLClassifier.TRAINSIZE=20;
        for(int i=0,k=0; i<20;i++){
            System.out.println("********** Corpus size (#utts)"+AnalyzeLClassifier.TRAINSIZE);
            String sclass="pn";
            File mfile = new File(MODELFILE.replace("%S", sclass));
            mfile.delete();
            mfile = new File(TRAINFILE.replace("%S", sclass));
            mfile.delete();
            mfile = new File(TESTFILE.replace("%S", sclass));
            mfile.delete();
            //trainAllLinearClassifier(sclass,true,false,false);
            allweightsKeepingOnlyTrain(sclass,AnalyzeLClassifier.TRAINSIZE, true,false,false);
            testingClassifier(true,sclass,false,false);
            LinearClassifier model = getModel(sclass);
            double f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
            double r=testingRForCorpus(sclass,false);
            plotR.addPoint(k, r);
            plotF1.addPoint(k, f1);
            k++;
            AnalyzeLClassifier.TRAINSIZE+=50;
            //break;
            
        }       
    }
    
    public void testingWSupOnEster(){
        reInitializingEsterFiles();
        AnalyzeLClassifier.PROPERTIES_FILE="etc/slinearclassifierORIG.props";
        File mfile = new File(MODELFILE.replace("%S", CNConstants.PRNOUN));
        mfile.delete();
        HashMap<String,Double> priorMap = new HashMap<>();
        priorMap.put(CNConstants.NOCLASS, new Double(0.9));
        priorMap.put(CNConstants.PRNOUN, new Double(0.1));
        setPriors(priorMap);
        //Long beforeUnsup=System.currentTimeMillis();
        System.out.println("generated data:"+ Margin.GENERATEDDATA);
        allweightsKeepingOnlyTrain(CNConstants.PRNOUN,20, true,false,false);
        //analyzing.wkSupParallelCoordD(CNConstants.PRNOUN, true);
        //analyzing.wkSupParallelFSCoordD(CNConstants.PRNOUN, true,50);
        wkSupParallelStocCoordD(CNConstants.PRNOUN, true,1000,true,true,false);        
    }
    
    public void transformingDistSimInput(){
        try {
            //
            //
            BufferedReader ester2File = new BufferedReader(new FileReader("/home/rojasbar/development/contnomina/clustering/posinduction/src/bin/ester2"));
            OutputStreamWriter outE2File = new OutputStreamWriter(new FileOutputStream("/home/rojasbar/development/contnomina/clustering/posinduction/src/bin/ester2Upper"),CNConstants.UTF8_ENCODING);
            BufferedReader fgwFile = new BufferedReader(new FileReader("/home/rojasbar/development/contnomina/clustering/posinduction/src/bin/fgw.in"));
            OutputStreamWriter outFGWFile = new OutputStreamWriter(new FileOutputStream("/home/rojasbar/development/contnomina/clustering/posinduction/src/bin/fgw.in.upper"),CNConstants.UTF8_ENCODING);
            
            while(true){
                String line = ester2File.readLine();
                if(line == null)
                    break;
                
                outE2File.append(line.toUpperCase()+"\n");
                outE2File.flush();
            }
            outE2File.close();
            while(true){
                String line = fgwFile.readLine();
                if(line == null)
                    break;
                
                outFGWFile.append(line.toUpperCase()+"\n");
                outFGWFile.flush();
            }            
            outFGWFile.close();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        
    }
    
   public static void main(String args[]) {
        AnalyzeLClassifier analyzing = new AnalyzeLClassifier();
        

        //*/
        //trainLinearclassifier(ispn,blsavegroups)
        /*//analyzing.trainAllLinearClassifier(true,true,false);
        analyzing.trainMulticlassNER(false, false);
        String sclass=CNConstants.ALL;
        analyzing.testingClassifier(true,sclass,false);
        //float[] priorsMap = analyzing.computePriors(sclass,analyzing.getModel(sclass));
        ///*/
        /*
        String sclass=CNConstants.PRNOUN;
        analyzing.trainAllLinearClassifier(sclass,true,false);
        analyzing.testingClassifier(true,sclass,false);
        //*/
        /*
        //analyzing.trainAllLinearClassifier(CNConstants.PRNOUN,true,false);
        String sclass=CNConstants.PRNOUN;
        //LinearClassifier model = analyzing.getModel(sclass);
        Object  object;
        try {
            object = IOUtils.readObjectFromFile("bin.pn.lc.mods.anal888iters");
            LinearClassifier  model=(LinearClassifier)object;  
            analyzing.testingClassifier(model, TESTFILE.replace("%S", sclass));
            //analyzing.testingClassifier(sclass, TESTFILE.replace("%S", sclass));
            
            
          
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        
        //*/
        //analyzing.computeFThetaOfX();
        //analyzing.computeROfTheta("pn");
        //testingClassifier(ispn,blsavegroups,smodel,iswiki)
       
        //analyzing.testingClassifier(true, "pn",false);
        //*/
        //analyzing.checkingInstances("pers");
        //computing the risk
        //analyzing.evalutatingF1AndR();
        /*
        // Checking WEAKLY SUPERVISED OPTIONS
        File mfile = new File(MODELFILE.replace("%S", CNConstants.PRNOUN));
        mfile.delete();
        HashMap<String,Double> priorMap = new HashMap<>();
        priorMap.put(CNConstants.NOCLASS, new Double(0.9));
        priorMap.put(CNConstants.PRNOUN, new Double(0.1));
        analyzing.setPriors(priorMap);
        Long beforeUnsup=System.currentTimeMillis();
        System.out.println("generated data:"+ Margin.GENERATEDDATA);
        analyzing.allweightsKeepingOnlyTrain(CNConstants.PRNOUN,20, true,false,false);
        //analyzing.wkSupParallelCoordD(CNConstants.PRNOUN, true);
        //analyzing.wkSupParallelFSCoordD(CNConstants.PRNOUN, true,50);
        analyzing.wkSupParallelStocCoordD(CNConstants.PRNOUN, true,100,false,true,false);
        //analyzing.wkSupClassifierConstr(CNConstants.PRNOUN,true);
        //analyzing.wkSConstrStochCoordGr(CNConstants.PRNOUN,true);
        //analyzing.unsupervisedClassifier(CNConstants.PRNOUN,false);
        //analyzing.chekingUnsupClassifierNInt(CNConstants.PRNOUN,false);
        //analyzing.checkingRvsTheta(CNConstants.PRNOUN,false);
        Long afterUnsup=System.currentTimeMillis();
        System.out.println("Time spent while computing R:" + (afterUnsup-beforeUnsup));
        //*/
        /*
        //Training without wiki
        analyzing.trainAllLinearClassifier(true,true,false);
        analyzing.testingClassifier(true,true, "",false);
        */

        /* 
		//Debugging the Stanford Classifier
            ColumnDataClassifier columnDataClass = new ColumnDataClassifier("PROPERTIES_FILE");                
            GeneralDataset data = columnDataClass.readTrainingExamples(TRAINFILE.replace("%S", "pn"));
            LinearClassifier model = (LinearClassifier) columnDataClass.makeClassifier(data);        
         
         //*/
        //analyzing.evaluationPOSTAGGER();
        //analyzing.evaluationCLASSRESULTS();
        /*Testing numerical integration
        String sclass="pn";
        
        //analyzing.saveFilesForLClassifier(sclass,true,false);
        analyzing.trainOneNERClassifier(sclass, false);
        //analyzing.saveFilesForLClassifier(sclass,false,false);
        List<List<Integer>> featsperInst = new ArrayList<>(); 
        List<Integer> labelperInst = new ArrayList<>();     
        LinearClassifier model = analyzing.getModel(sclass);
        //analyzing.getValues(TRAINFILE.replace("%S", sclass),model,featsperInst,labelperInst);
        analyzing.getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
        analyzing.featInstMap.put(sclass,featsperInst);
        analyzing.lblInstMap.put(sclass, labelperInst);   
        
        float  risk = analyzing.computeROfTheta(sclass);

        System.out.println("Closed form: "+risk);

        analyzing.checkingRNumInt(sclass, risk);  
        //analyzing.testingTerms(sclass);
        //*/
        /*
        risk = analyzing.computeROfThetaNumInt(sclass,true);
        System.out.println("Risk MCIntegration: "+risk);

        risk = analyzing.computeROfThetaNumInt(sclass,false);
        System.out.println("Risk MCIntegration: "+risk);
        //*/

        //*/
        //analyzing.analyzingEvalFile();
        //analyzing.comparingNumIntVsClosedF();
        //analyzing.checkingMCTrapezoidNumInt(sclass);

        //analyzing.printingValuesInOctave("analysis/StochGrad/outLogOct32014_NOTNGRAMS.txt");
        //analyzing.printingValuesInOctave("analysis/StochGrad/outLogOct062014_NOTNGRAMS.txt");
        //analyzing.printingValuesInOctave("analysis/conll/parallelSeqCoordDescOct10.txt");
        //analyzing.printingValuesInOctave("analysis/conll/fselectionparallelcoordGrad.txt");
        //analyzing.organizingREPEREFiles();
        
        //analyzing.generatingArffData(true);
        //analyzing.evaluationKMEANS();
        //AnalyzeClassifier.evaluationCLASSRESULTS(CNConstants.PERS,"repereOut.txt");
        //AnalyzeClassifier.evaluationCLASSRESULTS(CNConstants.PERS,"analysis/CRF/test.pers.log.repere");
        //AnalyzeClassifier.evaluationCLASSRESULTS(CNConstants.ALL,"analysis/CRF/test.pers.log");
        //analyzing.properNounDetectionOnEster();
        //analyzing.testingClassifier(CNConstants.PRNOUN, TESTFILE.replace("%S", CNConstants.PRNOUN));
        /*
         * Train on all data, train and test but keep only the weights of the little data, the weights for all the other features
         * are set to zero
         */
        //analyzing.allweightsKeepingOnlyTrain(CNConstants.PRNOUN,20, true, false, false);
        //testing new weights
        //CoNLL03Ner conll = new CoNLL03Ner();
        //conll.testingNewWeightsLC(CNConstants.PRNOUN, true, 50,500,false);
        //analyzing.testingWSupOnEster();
        analyzing.transformingDistSimInput();
    }
  
}
