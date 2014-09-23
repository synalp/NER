/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package linearclassifier;


import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import java.util.List;
import edu.stanford.nlp.classify.ColumnDataClassifier;


import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.Datum;

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
import java.util.Arrays;
import java.util.Collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.lang.Integer;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;


import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jsafran.DetGraph;
import jsafran.GraphIO;
import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import resources.WikipediaAPI;
import tools.CNConstants;
import tools.Histoplot;
import tools.IntegerValueComparator;
import tools.PlotAPI;
import utils.ErrorsReporting;
import utils.FileUtils;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import tools.DoubleValueComparator;

/**
 * This class process the instances and features by instance in the Stanford linear classifier 
 * @author rojasbar
 */
public class AnalyzeClassifier {
    private static final long serialVersionUID = 1L; 
    public static String MODELFILE="bin.%S.lc.mods";
    public static String TRAINFILE="groups.%S.tab.lc.train";
    public static String TESTFILE="groups.%S.tab.lc.test";
    public static String LISTTRAINFILES="esterTrain.xmll";
    public static String LISTTESTFILES="esterTest.xmll";
    public static String UTF8_ENCODING="UTF8";
    public static String PROPERTIES_FILE="slinearclassifier.props";
    public static String NUMFEATSINTRAINFILE="2-";
    public static String ONLYONEPNOUNCLASS=CNConstants.PRNOUN;
    public static String ONLYONEMULTICLASS=CNConstants.ALL;
    public static String[] groupsOfNE = {CNConstants.PERS,CNConstants.ORG, CNConstants.LOC, CNConstants.PROD};
    public static int TRAINSIZE=20;//Integer.MAX_VALUE; 
    //TRAINSIZE=20;  
    
    
    private HashMap<String, LinearClassifier> modelMap = new HashMap<>();
    private HashMap<String,Margin> marginMAP = new HashMap<>();
    private int numInstances=0;

    private HashMap<String, List<List<Integer>>> featInstMap = new HashMap<>();
    private HashMap<String, List<Integer>> lblInstMap = new HashMap<>();
    private HashMap<Integer,List<String>> stLCDictTrainFeatures=new HashMap<>();
    private HashMap<Integer,List<String>> stLCDictTestFeatures=new HashMap<>();
    private long elapsedTime;
    
    public AnalyzeClassifier(){

    }
    public void updatingPropFile(String nameEntity, boolean iswiki){
       

        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(PROPERTIES_FILE)); // FileInputStream
            prop.setProperty("trainFile", TRAINFILE.replace("%S", nameEntity));
            
            if(iswiki){
                System.out.println("Entro a is wiki updatingPropFile " + PROPERTIES_FILE);
                prop.put("3.useString","true");
                //prop.setProperty("3.useString","true");
            }else{
                if(prop.getProperty("3.useString")!=null)
                    prop.remove("3.useString");
            }
            prop.store(new FileOutputStream(PROPERTIES_FILE),""); // FileOutputStream 
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
   }     
        
   public  void saveGroups(String sclass,boolean bltrain, boolean iswiki){
       //only one proper noun classifier
       String[] classStr={ONLYONEPNOUNCLASS};
       
       if(sclass.equals(ONLYONEMULTICLASS)){
           classStr[0]=ONLYONEMULTICLASS;
       }
       if(!sclass.equals(ONLYONEPNOUNCLASS) && !sclass.equals(ONLYONEMULTICLASS))
           classStr=groupsOfNE;
       
       for(String str:classStr)
           saveFilesForLClassifier(str,bltrain, iswiki);

    }
        
    public void saveFilesForLClassifier(String en, boolean bltrain, boolean iswiki) {
            try {
                //if(bltrain&iswiki)
                //WikipediaAPI.loadWiki();
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
                                            
                                            if(en.equals(ONLYONEPNOUNCLASS)){
                                                //all the groups are proper nouns pn
                                                for(String str:groupsOfNE){
                                                    if (group.groupnoms.get(gr).startsWith(str)) {
                                                        lab=en;
                                                        break;
                                                    }
                                                }
                                            }else{
                                                 if (group.groupnoms.get(gr).startsWith(en)) {
                                                    //int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                    //if (debdugroupe==j) lab = en+"B";    
                                                    //else lab = en+"I";
                                                    lab=en;
                                                    break;
                                                }else{
                                                    if (en.equals(ONLYONEMULTICLASS)) {
                                                        String groupName=group.groupnoms.get(gr);
                                                        groupName=groupName.substring(0, groupName.indexOf("."));
                                                        //if(!Arrays.asList(groupsOfNE).toString().contains(groupName))
                                                        //    continue;
                                                        if(!Arrays.asList(CNConstants.PERS).toString().contains(groupName))
                                                            continue;                                                        
                                                        int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                        int endgroupe = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;
                                                        if (debdugroupe==endgroupe) lab = groupName+"U"; //Unit
                                                        else if (debdugroupe==j) lab = groupName+"B"; //Begin
                                                        else if (endgroupe==j) lab = groupName+"L"; //Last
                                                        else lab = groupName+"I";//Inside
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
                                    }else if(!isStopWord(group.getMot(j).getPOS()))
                                        outFile.append(lab+"\t"+group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\n");
                                     
                            }
                            
                            uttCounter++;
                            if(bltrain && uttCounter> TRAINSIZE){
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
                }
                outFile.flush();
                outFile.close();
                inFile.close();
                ErrorsReporting.report("groups saved in groups.*.tab number of utterances: "+ uttCounter);
            } catch (IOException e) {
                    e.printStackTrace();
            }
    }   
    
    public void trainOneClassifier(String sclassifier, boolean iswiki){
        LinearClassifier model = null;
        File mfile = new File(MODELFILE.replace("%S", sclassifier));
        if(!mfile.exists()){
            updatingPropFile(sclassifier, iswiki);
            ColumnDataClassifier columnDataClass = new ColumnDataClassifier("slinearclassifier.props");                
            GeneralDataset data = columnDataClass.readTrainingExamples(TRAINFILE.replace("%S", sclassifier));
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
            modelMap.put(sclassifier,model);
            Margin margin = new Margin(model);
            marginMAP.put(sclassifier,margin);  
            
            //compute the values for instances in the trainset
            /*getValues(TRAINFILE.replace("%S", sclassifier),model,featsperInst,labelperInst);
            System.out.println("Total number of features: "+ model.features().size());
            featInstMap.put(sclassifier,featsperInst);
            lblInstMap.put(sclassifier, labelperInst);*/
    
        }        
    }
        
    /**
     * Returns the different models for each type of NE,
     * save the models in a file, so there is no need to retrain each time
     * @param labeled
     * @return 
     */    
    public void trainAllLinearClassifier(String model,boolean blsavegroups, boolean iswiki) {
        //TreeMap<String,Double> lcfeatsDict = new TreeMap<>();
        //TreeMap<String,Double> featsDict = new TreeMap<>();
        //save the trainset
        if(blsavegroups)
            saveGroups(model,true,iswiki);
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

            trainOneClassifier(str, iswiki);
            
        }
        
    }   
    
    public void trainMulticlassNER(boolean savegroups, boolean iswiki){
        //ONLYONEMULTICLASS
        if(savegroups)
            saveGroups(ONLYONEMULTICLASS,true,iswiki);
              
       
        //call the classifier
        modelMap.clear();
        marginMAP.clear();
        

       trainOneClassifier(ONLYONEMULTICLASS, iswiki);
            
                
        
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
            inFile = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), UTF8_ENCODING));
            
            numInstances=0;

            for (;;) {
                String line = inFile.readLine();
                if (line==null) break;
                ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE);
                Datum<String, String> datum = columnDataClass.makeDatumFromLine(line, 0);
                Collection<String> features = datum.asFeatures();
                
                List<Integer> feats = new ArrayList<>();
                //take the id (index) of the features
                for(String f:features){

                    if(model.featureIndex().indexOf(f)>-1)
                        feats.add(model.featureIndex().indexOf(f));
                }
                //System.out.println("feats[:"+numInstances+"]="+feats);
                featsperInst.add(feats);
                //take the id (index) of the labels
                String label = line.substring(0, line.indexOf("\t"));
                int labelId = model.labelIndex().indexOf(label);
                labelperInst.add(labelId);
                numInstances++;    
                if(fileName.contains("train"))
                    stLCDictTrainFeatures.put(numInstances, new ArrayList<>(features));
                else
                     stLCDictTestFeatures.put(numInstances, new ArrayList<>(features));                       
                               
            }
            if(fileName.contains("train"))
                serializingFeatures(stLCDictTrainFeatures,true);
            else
                serializingFeatures(stLCDictTestFeatures,false);
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
    /**
     * Compute the score F_{\theta}(X) per category
     * Store the scores in an array, that is then analyzed for seen
     * if is normally distributed, we analyze it in Octave
     */
    public void computeFThetaOfX(){
        
        for(String key:lblInstMap.keySet()){
            System.out.println("Analyzing classifier :"+key);
                        
            List<List<Integer>> featsperInst = featInstMap.get(key);
            List<Integer> labelperInst = lblInstMap.get(key);
            
            int nInst=labelperInst.size();
            Margin margin= marginMAP.get(key);
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
                    OutputStreamWriter outFile = new OutputStreamWriter(new FileOutputStream("analysis/ftheta_r"+key+"_"+y+".m"),UTF8_ENCODING);
                    outFile.append("ftheta_r"+key+"_"+y+"="+rscoreperLabel.get(y).toString());
                    outFile.flush();
                    outFile.close();  
                    outFile = new OutputStreamWriter(new FileOutputStream("analysis/ftheta_w"+key+"_"+y+".m"),UTF8_ENCODING);
                    outFile.append("ftheta_w"+key+"_"+y+"="+wscoreperLabel.get(y).toString());
                    outFile.flush();
                    outFile.close(); 
                    outFile = new OutputStreamWriter(new FileOutputStream("analysis/inst_r"+key+"_"+y+".m"),UTF8_ENCODING);
                    outFile.append("inst_r"+key+"_"+y+"="+instRscoreperLabel.get(y).toString());
                    outFile.flush();
                    outFile.close(); 
                    outFile = new OutputStreamWriter(new FileOutputStream("analysis/inst_w"+key+"_"+y+".m"),UTF8_ENCODING);
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
     * Verify most frequent instances according to a list of instances obtained from a histogram in octave.
     * @param modelKey 
     */
    public void checkingInstances(String modelKey){
        try{
            BufferedReader inFile = new BufferedReader(new InputStreamReader(new FileInputStream("analysis/inst.mat"), UTF8_ENCODING));
            BufferedReader trFile = new BufferedReader(new InputStreamReader(new FileInputStream(TRAINFILE.replace("%S", modelKey)), UTF8_ENCODING));
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
    public void testingClassifier(boolean isSavingGroups, String smodel, boolean iswiki){
       if(isSavingGroups)
            saveGroups(smodel,false, iswiki);
       
       
       updatingPropFile(smodel,iswiki);
        try {
            //command
            //String cmd="java -Xmx1g -cp  \"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\" edu.stanford.nlp.classify.ColumnDataClassifier -prop slinearclassifier.props groups.pers.tab.lc.train -testFile groups.pers.tab.lc.test > out.txt";

            //String[] call={"java","-Xmx1g","-cp","\"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\"","edu.stanford.nlp.classify.ColumnDataClassifier", "-prop","slinearclassifier.props", "-testFile", TESTFILE.replace("%S", smodel),"> out.txt"};
            //Process process = Runtime.getRuntime().exec(call);
            String cmd="java -Xmx1g -cp  /home/rojasbar/development/contnomina/stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar edu.stanford.nlp.classify.ColumnDataClassifier -prop slinearclassifier.props "+TRAINFILE.replace("%S", smodel)+" -testFile "+TESTFILE.replace("%S", smodel);
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
        return columnDataClass.fs.get("pn");
        
    }
    public void testingClassifier(String smodel, String testfile){
      
       
      
        try {
            //command
            //String cmd="java -Xmx1g -cp  \"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\" edu.stanford.nlp.classify.ColumnDataClassifier -prop slinearclassifier.props groups.pers.tab.lc.train -testFile groups.pers.tab.lc.test > out.txt";

            //String[] call={"java","-Xmx1g","-cp","\"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\"","edu.stanford.nlp.classify.ColumnDataClassifier", "-prop","slinearclassifier.props", "-testFile", TESTFILE.replace("%S", smodel),"> out.txt"};
            //Process process = Runtime.getRuntime().exec(call);
            String cmd="java -Xmx1g -cp  /home/rojasbar/development/contnomina/stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar edu.stanford.nlp.classify.ColumnDataClassifier -prop slinearclassifier.props  -loadClassifier  "+MODELFILE.replace("%S", smodel)+" -testFile "+TESTFILE.replace("%S", smodel);
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
    
    public float[] computePriors(String sclassifier,LinearClassifier model){
        
        float[] priors = new float[model.labels().size()];
        float prob=0f, alpha=0.1f;
        List<List<Integer>> featsperInst = new ArrayList<>(); 
        List<Integer> labelperInst = new ArrayList<>();         
        getValues(TRAINFILE.replace("%S", sclassifier),model,featsperInst,labelperInst);
        featInstMap.put(sclassifier,featsperInst);
        lblInstMap.put(sclassifier, labelperInst);          
        List<Integer> vals=lblInstMap.get(sclassifier);
        int[] nTargetClass= new int[model.labels().size()];
        Arrays.fill(nTargetClass, 0);
        
        for(int i=0;i<vals.size();i++){
            int lblIdx=vals.get(i);
            nTargetClass[lblIdx]++;
        }
        for(int l=0; l<priors.length;l++){
            prob = (float) nTargetClass[l]/ (float) vals.size();        
            priors[l]=prob;
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
     * @param py
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
    
    public float computeROfTheta(String sclassifier) {

        //final float[] priors = computePriors(sclassifier,model);
        final float[] priors = {0.9f,0.1f};
        // get scores
        GMMDiag gmm = new GMMDiag(2, priors);
        gmm.setClassifier(sclassifier);
        gmm.train(this, marginMAP.get(sclassifier));
        System.out.println("mean=[ "+gmm.getMean(0, 0)+" , "+gmm.getMean(0, 1)+";\n"+
        +gmm.getMean(1, 0)+" , "+gmm.getMean(1, 1)+"]");
        System.out.println("sigma=[ "+gmm.getVar(0, 0, 0)+" , "+gmm.getVar(0, 1, 1)+";\n"+
        +gmm.getVar(1, 0, 0)+" , "+gmm.getVar(1, 1, 1));
        System.out.println("GMM trained");
        
        //return computeR(gmm, priors,marginMAP.get(sclassifier).getNlabs() );
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
        //mcInt.errorAnalysisBinInt(gmm,py,nLabels);
        
        for(int y=0;y<nLabels;y++){
            //arguments gmm, distribution of the proposal, metropolis, is plot
            //risk+=py[y]*mcInt.integrate(gmm,y,CNConstants.UNIFORM, true,false);
            //last paramenter, number of trials, when -1 default takes place = 50000 iterations
            double integral=0.0;
            
            if(isMC)
                integral=mcInt.integrateBinaryCase(gmm,y,CNConstants.UNIFORM, false,false,numIters);
            else
                integral=mcInt.trapeziumMethod(gmm,y,numIters);
            
            //System.out.println("Numerical Integration Integral: "+integral);
            risk+=py[y]*integral;
                
        }
        
        return risk;
    }

       public float computeROfThetaNumInt(String sclassifier, boolean isMC, int numIters) {

        //final float[] priors = computePriors(sclassifier,model);
        final float[] priors = {0.9f,0.1f};
        // get scores
        GMMDiag gmm = new GMMDiag(2, priors);
        gmm.setClassifier(sclassifier);
        gmm.train(this, marginMAP.get(sclassifier));
        System.out.println("mean=[ "+gmm.getMean(0, 0)+" , "+gmm.getMean(0, 1)+";\n"+
        +gmm.getMean(1, 0)+" , "+gmm.getMean(1, 1)+"]");
        System.out.println("sigma=[ "+gmm.getVar(0, 0, 0)+" , "+gmm.getVar(0, 1, 1)+";\n"+
        +gmm.getVar(1, 0, 0)+" , "+gmm.getVar(1, 1, 1));
        
        System.out.println("GMM trained");
        
        
        //return computeR(gmm, priors,marginMAP.get(sclassifier).getNlabs() );
        long beforeR=System.nanoTime();
        float r= computeRNumInt(gmm, priors,marginMAP.get(sclassifier).getNlabs(), isMC, numIters ); //xtof
        long afterR=System.nanoTime();
        elapsedTime = afterR-beforeR;
        System.out.println("in computeROfThetaNumInt TIME: "+elapsedTime);
        
        return r;
        
    }   
    public  float checkingRNumInt( String sclassifier,double closedForm) {
        try {
        OutputStreamWriter fout  = new OutputStreamWriter(new FileOutputStream("analysis/EMNLPExps/comparingIntR.m"),UTF8_ENCODING);
        
        //final float[] priors = computePriors(sclassifier,model);
        final float[] py = {0.9f,0.1f};
        // get scores
        GMMDiag gmm = new GMMDiag(2, py);
        gmm.setClassifier(sclassifier);
        gmm.train(this, marginMAP.get(sclassifier));
        System.out.println("mean 00 "+gmm.getMean(0, 0));
        System.out.println("mean 01 "+gmm.getMean(0, 1));
        System.out.println("mean 10 "+gmm.getMean(1, 0));
        System.out.println("mean 11 "+gmm.getMean(1, 1));
        System.out.println("GMM trained");

        float risk=0f,riskTrapezoidInt=0f;;
        NumericalIntegration mcInt = new NumericalIntegration();
        int numTrials=30000;
        //mcInt.errorAnalysisBinInt(gmm,py,gmm.getDimension(),numTrials);
        /*
        PlotAPI plotIntegral = new PlotAPI("Risk vs trials","Num of trials", "Integral");   

        
        fout.append("cf="+closedForm+";\n");
        fout.append("rmci=[");
        for(int i=100; i< 1000001;i+=100){
            risk=0f;
            for(int y=0;y<py.length;y++){
                    double integral=mcInt.integrateBinaryCase(gmm,y,CNConstants.UNIFORM, false,false,i);
                    risk+=py[y]*integral;
                    
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
            for(int y=0;y<py.length;y++){
                //double integral = mcInt.trapezoidIntegration(gmm, y,Integer.MAX_VALUE);
                double integral = mcInt.trapeziumMethod(gmm, y,i);
                System.out.println("py="+py[y]+" I["+y+"]="+integral);
                riskTrapezoidInt+=py[y]*integral;

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
  
                
        //final float[] priors = computePriors(sclassifier,model);
        final float[] py = {0.9f,0.1f};
        // get scores
        GMMDiag gmm = new GMMDiag(2, py);
        gmm.setClassifier(sclassifier);
        gmm.train(this, marginMAP.get(sclassifier));
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
        final float[] py = {0.9f,0.1f};
        // get scores
        GMMDiag gmm = new GMMDiag(2, py);
        gmm.setClassifier(sclassifier);
        gmm.train(this, marginMAP.get(sclassifier));
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
        float t1 = py[0]*(1f-2f*mean00)/(2f*sigma00*sqrtpi) * (1f+(float)erf( (0.5-mean00)/sigma00 ));
        NumericalIntegration nInt = new NumericalIntegration();
        double t1NInt=  py[0]*nInt.trapeziumMethodNSquared(gmm, 3000);
        System.out.println(t1+" vs "+t1NInt);
        
    }
    
    public float testingRForCorpus(String sclass, boolean iswiki){
                //train the classifier with a small set of train files
        trainOneClassifier(sclass, iswiki);  
        LinearClassifier model = modelMap.get(sclass);
        //scan the test instances for train the gmm
        List<List<Integer>> featsperInst = new ArrayList<>(); 
        List<Integer> labelperInst = new ArrayList<>(); 
        getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
        featInstMap.put(sclass,featsperInst);
        lblInstMap.put(sclass, labelperInst);        
        
        
        System.out.println("Working with classifier "+sclass);
        float estimr0 = computeROfTheta(sclass);
        System.out.println("init R "+estimr0);
        return estimr0;
        
    }
   /**
     * The gradient method used ins the Finite Difference
     * f'(a) is approximately (f(a+h)-f(a))/h
     * @param sclass 
     */ 
   public void unsupervisedClassifier(String sclass, boolean closedForm) {
        PlotAPI plotR = new PlotAPI("R vs Iterations","Num of Iterations", "R");
        PlotAPI plotF1 = new PlotAPI("F1 vs Iterations","Num of Iterations", "F1");
        
        boolean isMC=false;
        int numIntIters=100;
        
        final int niters = 5000;
        final float eps = 0.1f;   
        int counter=0;
        //train the classifier with a small set of train files
        trainOneClassifier(sclass,false);  
        LinearClassifier model = modelMap.get(sclass);
        Margin margin = marginMAP.get(sclass);
        int selectedFeats[] = margin.getTopWeights();
        //scan the test instances for train the gmm
        List<List<Integer>> featsperInst = new ArrayList<>(); 
        List<Integer> labelperInst = new ArrayList<>(); 
        getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
        featInstMap.put(sclass,featsperInst);
        lblInstMap.put(sclass, labelperInst);   
        double[] scores= new double[featsperInst.size()];
        Arrays.fill(scores, 0.0);
        //Histoplot.showit(scorest,featsperInst.size());
        HashSet<String> emptyfeats = new HashSet<>();
        System.out.println("Working with classifier "+sclass);
        
        float estimr0=(closedForm)?computeROfTheta(sclass):computeROfThetaNumInt(sclass, isMC,numIntIters);


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
                        float estimr = (closedForm)?computeROfTheta(sclass):computeROfThetaNumInt(sclass, isMC,numIntIters);

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
                estimr0 =(closedForm)?computeROfTheta(sclass):computeROfThetaNumInt(sclass,isMC,numIntIters);
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
                fout = new OutputStreamWriter(new FileOutputStream("analysis/EMNLPExps/ImpactCFAccOrW.m"),UTF8_ENCODING);
                data="datacf=[\n";
            }else
            fout = new OutputStreamWriter(new FileOutputStream("analysis/EMNLPExps/ImpactMCIntAcc_Init2.m"),UTF8_ENCODING);
            //train the classifier with a small set of train files
            trainOneClassifier(sclass,false);  
            LinearClassifier model = modelMap.get(sclass);
            Margin margin = marginMAP.get(sclass);
            double[][] orWeights=  new double[margin.getWeights().length][];                           
            
            List<List<Integer>> featsperInst = new ArrayList<>(); 
            List<Integer> labelperInst = new ArrayList<>(); 
            getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
            featInstMap.put(sclass,featsperInst);
            lblInstMap.put(sclass, labelperInst);   
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
                float estimr0=(closedForm)?computeROfTheta(sclass):computeROfThetaNumInt(sclass, isMC,ninti);
                //double f1=0.0;
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
        OutputStreamWriter fout = null;
        try {

            boolean isMC=false;
            int numIntIters=100;
            fout = new OutputStreamWriter(new FileOutputStream("analysis/EMNLPExps/Rvstheta.m"),UTF8_ENCODING);

            //train the classifier with a small set of train files
            trainOneClassifier(sclass,false);
            LinearClassifier model = modelMap.get(sclass);
            Margin margin = marginMAP.get(sclass);
            //int selectedFeats[] = margin.getTopWeights();
            //scan the test instances for train the gmm
            List<List<Integer>> featsperInst = new ArrayList<>();
            List<Integer> labelperInst = new ArrayList<>();
            getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
            featInstMap.put(sclass,featsperInst);
            lblInstMap.put(sclass, labelperInst);
            double[] scores= new double[featsperInst.size()];
            Arrays.fill(scores, 0.0);
            
            
           
            System.out.println("Working with classifier "+sclass);
            System.out.println("Number of features" + margin.getNfeats());
            double[][] weightsForFeat=margin.getWeights();
            //for(int i=0;i<weightsForFeat.length;i++){
            int selectedFeats[] = margin.getTopWeights();
            for(int i=0;i<10;i++){
                String wvec="w=[";
                String rvec="r=[";
                int featIdx = selectedFeats[i];
                //int featIdx=i;
                //for(int w=0;w < weightsForFeat[featIdx].length;w++){
                float w0 = (float) weightsForFeat[featIdx][0];
                wvec+=w0+";\n";
                float r = (closedForm)?computeROfTheta(sclass):computeROfThetaNumInt(sclass, isMC,numIntIters);
                rvec+=r+";\n";
                for(int k=0;k<100;k++){
                    System.out.println("INCREASING w "+k);
                    float w=w0+0.05f;
                    weightsForFeat[featIdx][0]=w;
                    wvec+=w+";\n";
                    r = (closedForm)?computeROfTheta(sclass):computeROfThetaNumInt(sclass, isMC,numIntIters);
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
            testFile = new BufferedReader(new InputStreamReader(new FileInputStream(TESTFILE.replace("%S", CNConstants.PRNOUN)), UTF8_ENCODING));
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
    public void evaluatingLabelProp(){
        HashMap<String,Integer> wordDict=deserializingWords();
        HashMap<Integer,String> wordStr=new HashMap<>();
        for(String key:wordDict.keySet()){
            //System.out.println("ID\t"+wordDict.get(key)+"\tWORD\t"+key);
            wordStr.put(wordDict.get(key), key);
        }
        HashMap<String,String> recLabels= new HashMap<>();
        
        BufferedReader testFile = null;
        BufferedReader lpFile=null, input=null;
        try {
            testFile = new BufferedReader(new FileReader(TESTFILE.replace("%S",CNConstants.PRNOUN))); //label_prop_output_100iters
            lpFile = new BufferedReader(new InputStreamReader(new FileInputStream("lprop/label_prop_output_catdist_pos3"), UTF8_ENCODING));
            input = new BufferedReader(new InputStreamReader(new FileInputStream("lprop/input_graph_pos3"), UTF8_ENCODING));
            
            //read the input just to inspect it was correct, and to check the vocabulary of words
            /*
            for(;;){
                              
               String inputline= input.readLine();
               
               if(inputline == null)
                   break;
               
               String[] graph= inputline.split("\\t");
               int node1=Integer.parseInt(graph[0].substring(1));
               int node2=Integer.parseInt(graph[1].substring(1));
               System.out.println(wordStr.get(node1)+"-"+ wordStr.get(node2)+"-"+graph[2]);                
            }
            */
            //read the label propagation output
            for(;;){
               String lpline = lpFile.readLine();
               if(lpline== null)
                    break; 
      
                String values[] = lpline.split("\\t");
                int wordid=Integer.parseInt(values[0].substring(1));
                String[] recvals= values[3].split(" ");
//                String[] labels= new String[3];
//                double[] vlabels= new double[3];
//                labels[0]=recvals[0];
//                vlabels[0]= Double.parseDouble(recvals[1]);
                String maxlabel="";
                double maxval=Integer.MIN_VALUE;
                for(int i=0; i< recvals.length;i+=2){
		    String lbl=recvals[i];
	            if(lbl.startsWith("__DUM"))
			continue;
                    double val = Double.parseDouble(recvals[i+1]);
                    if(val>maxval){
                        maxval=val;
                        maxlabel=recvals[i];
                    }    
                }
                System.out.println("ID\t"+wordid+"\tWORD\t"+wordStr.get(wordid)+"\tREC LABEL\t"+maxlabel);
                recLabels.put(wordStr.get(wordid), (maxlabel.equals("L1"))?CNConstants.PRNOUN:CNConstants.NOCLASS);              
                //System.out.println("label: " + maxlabel + "value " + maxval);
            }   
            int tp=0, tn=0, fp=0, fn=0;
            int tp0=0, tn0=0, fp0=0, fn0=0;
            for(;;){
                String line = testFile.readLine();   
                
                
                if(line== null)
                    break;    
                              
                
                String values[] = line.split("\\t");
                String label = values[0];
                String recognizedLabel = recLabels.get(values[1]);
                //System.out.println("ID\t"+wordDict.get(values[1])+"\tWORD\t"+values[1]+"\tREC LABEL\t"+recognizedLabel+"\tTRUE LABEL\t"+label);
                if(recognizedLabel.equals(CNConstants.PRNOUN) && label.equals(CNConstants.PRNOUN)){
		    //System.out.println("tp word: "+ values[1]+" "+recognizedLabel);	
                    tp++;tn0++;
		}
                
                if(recognizedLabel.equals(CNConstants.PRNOUN)&& label.equals(CNConstants.NOCLASS)){
                    fp++;fn0++;
                }
                if(recognizedLabel.equals(CNConstants.NOCLASS)&&label.equals(CNConstants.PRNOUN)){
                    fn++;fp0++;
                }    
                if(recognizedLabel.equals(CNConstants.NOCLASS)&&label.equals(CNConstants.NOCLASS)){
                    tn++;tp0++;
                }    

            }
            double precision= (double) tp/(tp+fp);
            double recall= (double) tp/(tp+fn);
            double f1=(2*precision*recall)/(precision+recall);
            double accuracy=(double) (tp+tn)/(tp+tn+fp+fn);
            
            System.out.println("confusion matrix:\n ["+ tp+","+fp+"\n"+fn+","+tn+"]");
            System.out.println("confusion matrix:\n ["+ tp0+","+fp0+"\n"+fn0+","+tn0+"]");
            System.out.println("  PN precision: "+precision);
            System.out.println("  PN recall: "+recall);
            System.out.println("  PN f1: "+f1);
            System.out.println("  Accuracy: "+accuracy);
            
            
            precision= (double) tp0/(tp0+fp0);
            recall= (double) tp0/(tp0+fn0);
            f1=(2*precision*recall)/(precision+recall);
            accuracy=(double) (tp0+tn0)/(tp0+tn0+fp0+fn0);
            
            System.out.println(" NO precision: "+precision);
            System.out.println(" NO recall: "+recall);
            System.out.println(" NO f1: "+f1);  
            
            System.out.println("GENERAL ACCURACY "+ accuracy);
            
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
    public void evaluationCLASSRESULTS(){
        BufferedReader testFile = null;
        try {
            testFile = new BufferedReader(new InputStreamReader(new FileInputStream("analysis/CRFS/lcResults.txt"), UTF8_ENCODING));
            
            int tp=0, tn=0, fp=0, fn=0;
            for(;;){

                String line = testFile.readLine();   
                
                if(line== null)
                    break;                
                if(line.startsWith("#"))
                    continue;
                
                String values[] = line.split("\\t");
                String label = values[1];
                String recognizedLabel = values[2];
                
                if(recognizedLabel.equals(CNConstants.PRNOUN) && label.equals(CNConstants.PRNOUN))
                    tp++;
                
                if(recognizedLabel.equals(CNConstants.PRNOUN)&& label.equals(CNConstants.NOCLASS))
                    fp++;
                
                if(recognizedLabel.equals(CNConstants.NOCLASS)&&label.equals(CNConstants.PRNOUN))
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
      public void evaluationKMEANS(){
        BufferedReader testFile = null;
        BufferedReader kmfile = null;
        try {
            testFile = new BufferedReader(new FileReader(TESTFILE.replace("%S",CNConstants.PRNOUN))); 
            kmfile = new BufferedReader(new InputStreamReader(new FileInputStream("/home/rojasbar/development/contnomina/kmeans/kmresults.mat"), UTF8_ENCODING));
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
   public void setFeaturesPerInst(HashMap<String, List<List<Integer>>>  fMap){
       this.featInstMap=fMap;
   }
   
   public void setLabelsPerInst(HashMap<String, List<Integer>> lMap){
       this.lblInstMap=lMap;
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
   

   public void comparingNumIntVsClosedF(){
       try {
    
            HashMap<Integer, Double> rcfMap=readingRiskFromFile("analysis/EMNLPExps/outLogAnalInt.txt",0);
            HashMap<Integer, Double> rniMap=readingRiskFromFile("analysis/EMNLPExps/outLogNumIntIter0to640.txt",0);
            rniMap.putAll(readingRiskFromFile("analysis/EMNLPExps/outLogNumInterIter640.txt",640));
            PrintWriter fout = FileUtils.writeFileUTF("analysis/EMNLPExps/comparingR.m");
            fout.println("RCF=[");
            for(int i=0;i<2000;i++){
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
   
   private HashMap<Integer, Double> readingRiskFromFile(String filename, int startIdx){
       HashMap<Integer, Double> rValsMap=new HashMap<>();
 try {
           BufferedReader ifile = new BufferedReader(new FileReader(filename));
           
           
           for (;;) {
                String line = ifile.readLine();
                if (line==null) break;
                if(!line.startsWith("R estim ["))
                    continue;
                
                int initIdx= line.indexOf("[");
                int ednIdx= line.indexOf("]");
                int idx= Integer.parseInt(line.substring(initIdx+1, ednIdx))+startIdx;
                int equal= line.indexOf("= ")+2;
                double value= Double.parseDouble(line.substring(equal));
                rValsMap.put(idx, value);
                
           }
           ifile.close();
           return rValsMap;
           
       }catch (Exception ex) {
            ex.printStackTrace();
            return rValsMap;
        }      
   }
    private void serializingWords(HashMap vocFeats){
    try{
            String fileName="WordDict.ser";
            
            FileOutputStream fileOut = new FileOutputStream(fileName);
            ObjectOutputStream out =
                            new ObjectOutputStream(fileOut);
            out.writeObject(vocFeats);
            out.close();
            fileOut.close();
        }catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }
    public HashMap<String,Integer> deserializingWords(){
      try
      {
        HashMap<String,Integer> vocFeats = new HashMap<>();
        String fileName="WordDict.ser";
        FileInputStream fileIn=  new FileInputStream(fileName);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        vocFeats = (HashMap<String,Integer>) in.readObject();
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
    
    public void generatingArffData(boolean istrain){
        
        //HashMap<Integer,List<String>> trainfeats= deserializingFeatures(true);
        HashMap<String,Integer> dictFeatures=new HashMap<>();
        HashMap<Integer,List<String>> feats=deserializingFeatures(istrain);
        BufferedReader infile = null;
        
        OutputStreamWriter outFile=null;
        try {
        if(istrain){  
            infile = new BufferedReader(new FileReader(TRAINFILE.replace("%S",CNConstants.PRNOUN)));
            outFile = new OutputStreamWriter(new FileOutputStream(TRAINFILE.replace("%S", CNConstants.PRNOUN)+".arff"),UTF8_ENCODING);
        }else{
            infile = new BufferedReader(new FileReader(TESTFILE.replace("%S",CNConstants.PRNOUN))); 
            outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", CNConstants.PRNOUN)+".arff"),UTF8_ENCODING);
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

    public void generatingLabelPropGraph(){
        
        DecimalFormat decFormat = new DecimalFormat("#.##");
        HashMap<String,Integer> dictFeatures=new HashMap<>();
        HashMap<String,Integer> dictWords=new HashMap<>();
        
        HashMap<Integer,List<String>> stfeats=deserializingFeatures(false);
        HashMap<Integer,String> wordsperInst=new HashMap<>();
        HashMap<String,List<Double>> vectorfeats=new HashMap<>();
        HashMap<String,String> seedTrain=new HashMap<>();
        HashMap<String,String> goldTest=new HashMap<>();
        //HashMap<Integer,TreeMap<Integer,Double>> relatednodes= new HashMap<>();
        
       
        BufferedReader train = null, test = null;
        OutputStreamWriter outInput=null;
        OutputStreamWriter outGold=null;
        OutputStreamWriter outSeed=null;
        try {
         
            train = new BufferedReader(new FileReader(TRAINFILE.replace("%S",CNConstants.PRNOUN)));
            test = new BufferedReader(new FileReader(TESTFILE.replace("%S",CNConstants.PRNOUN))); 
            outInput = new OutputStreamWriter(new FileOutputStream("lprop/input_graph_pos3"),UTF8_ENCODING);
            outGold = new OutputStreamWriter(new FileOutputStream("lprop/gold_labels_pos3"),UTF8_ENCODING);
            outSeed = new OutputStreamWriter(new FileOutputStream("lprop/seeds_pos3"),UTF8_ENCODING);
        
        int linecounter=1;
        
        for(;;){
            String seedLine= train.readLine();
            if (seedLine==null) break;
                String[] stdata=seedLine.split("\t");
                if(!dictWords.containsKey(stdata[1]))
                    dictWords.put(stdata[1],dictWords.size()+1);

                seedTrain.put(stdata[1],stdata[0]);           
        }
        
        for (;;) {
            
            String line = test.readLine();
            if (line==null) break;
            String[] stdata=line.split("\t");
            String categ= stdata[0];
            String wordt= stdata[1];
            String pos= stdata[2];    
            
            if(!dictWords.containsKey(wordt))
                    dictWords.put(wordt,dictWords.size()+1);

            if(!dictFeatures.containsKey(pos))
                dictFeatures.put(pos,dictFeatures.size()+1);


            List<String> restfeats= stfeats.get(linecounter);
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

            wordsperInst.put(linecounter,wordt);
            goldTest.put(wordt, categ);
            List<Double> numFeats=new ArrayList<>();
            numFeats.add(Double.parseDouble(dictFeatures.get(pos).toString()));
            //numFeats.add(Double.parseDouble(dictFeatures.get(wshape.trim()).toString()));
            //numFeats.add(Double.parseDouble(dictFeatures.get(lngram.trim()).toString()));
            vectorfeats.put(wordt,numFeats);

            linecounter++;

        }
        serializingWords(dictWords);
        System.out.println("TOTALOFLINES # "+linecounter);
        System.out.println("TOTALOFNODES(words) # "+vectorfeats.size());
        double[][] feats= new double[linecounter][1];

        
        for(String key:vectorfeats.keySet()){
            List<Double> vector = vectorfeats.get(key);

            for(int k=0;k<vector.size();k++)
                feats[dictWords.get(key)][k]=vector.get(k);

          
        }
        
///*        
        //double[][] relallnodes=new double[linecounter][1];
        Long before=System.currentTimeMillis();
	System.out.println("Before computing all x all distance "+before);
        //Euclidean Distance of one word with the rest
        //EuclideanDistance eDist = new EuclideanDistance();
        int outercounter=0;
        for(String key1:vectorfeats.keySet()){
            if(key1.equals("Fabrice"))
                System.out.println("entro : "+ vectorfeats.get("Fabrice"));
            
            TreeMap<Integer,Double> lowDist=new TreeMap<>();
            TreeMap<Integer,Double> topDist=new TreeMap<>();
            //relatednodes.put(i,relsni);
            for(String  key2:vectorfeats.keySet()){
                
                if(key1.equals(key2))
                    continue;
                
                //TreeMap<Integer,Double> relsnj= new TreeMap<>();                
                //double dist = eDist.compute(feats[dictWords.get(key1)], feats[dictWords.get(key2)]);
                
                double dist=0.0;
                //double[] w= {0.4,0.4,0.2};
                for(int fs1=0;fs1<feats[dictWords.get(key1)].length;fs1++){
                    if(feats[dictWords.get(key1)][fs1]==feats[dictWords.get(key2)][fs1])
                        dist+=1.0;//*w[fs1];
                    else
                        dist+=4.0;//*w[fs1];
                    
                }
                if(dist==1.0)
                    lowDist.put(dictWords.get(key2),dist);
                else
                    topDist.put(dictWords.get(key2),dist);
                //outInput.append("N"+dictWords.get(key1)+"\tN"+dictWords.get(key2)+"\t"+decFormat.format(dist)+"\n");
                //outInput.flush();                  

            }
            //DoubleValueComparator bvc = new DoubleValueComparator(nodesrel);
            //SortedSet<Map.Entry<Integer, Double>> sortedDist = DoubleValueComparator.entriesSortedByValues(nodesrel);
            //TreeMap<Integer, Double> sortedDist = new TreeMap<>(bvc);
            //sortedDist.putAll(nodesrel);
            
         
            
            
            int totalDist=lowDist.size()+topDist.size();
            double propL1= (double) lowDist.size()/totalDist;
            double propL2= 1-propL1;
            double totalL1=1000*propL1;
            double totalL2=1000*propL2;
            int counter =0;
            for(Integer keys:lowDist.keySet()){
                
                outInput.append("N"+dictWords.get(key1)+"\tN"+keys+"\t"+decFormat.format(lowDist.get(keys).doubleValue())+"\n");
                outInput.flush();  
                                
                if(counter>=totalL1)
                    break;
                counter++;
            }    
            counter =0;
            for(Integer keys:topDist.keySet()){
                
                outInput.append("N"+dictWords.get(key1)+"\tN"+keys+"\t"+decFormat.format(topDist.get(keys).doubleValue())+"\n");
                outInput.flush();  
                                
                if(counter>=totalL2)
                    break;
                counter++;
            }            
            //System.out.println("scanned node"+dictWords.get(key1)+"===WORD==="+key1);
            System.out.println("scanned outer counter"+outercounter);
            outercounter++;
        }  
//    System.out.println("after computing all x all distance "+System.currentTimeMillis());
//        //for(Integer key:relatednodes.keySet()){
//            TreeMap<Integer,Double> nodesrel=new TreeMap<>();
//            //for(int j=0;j<relallnodes.length;j++){
//            //    nodesrel.put(j,relallnodes[i][j]);
//            //}
//            
//            DoubleValueComparator bvc = new DoubleValueComparator(nodesrel);
//            TreeMap<Integer, Double> sortedDist = new TreeMap<>(bvc);
//            sortedDist.putAll(nodesrel);
//            
//            int counter =0;
//            for(Integer keys:sortedDist.keySet()){
//                if(sortedDist.get(key)==null)
//                    continue;
//                //relsn.put(key,sortedDist.get(key));
//                outInput.append("N"+key+"\tN"+keys+"\t"+decFormat.format(sortedDist.get(keys).doubleValue())+"\n");
//                outInput.flush();  
//                                
//                if(counter>=1000)
//                    break;
//                counter++;
//            }
//            //relatednodes.put(i,relsn);
//            System.out.println("scanned node"+key);
//        //}
        
        System.out.println("After computing all x all distance ");
        //nodes
//        for(Integer key:relatednodes.keySet()){
//            HashMap<Integer,Double> rels= relatednodes.get(key);
//            for(Integer key2:rels.keySet()){
//                outInput.append("N"+key+"\tN"+key2+"\t"+decFormat.format(rels.get(key2).doubleValue())+"\n");
//                outInput.flush();          
//            }  
//            System.out.println("scanned node"+key);
//        }
 //*/           
        //seed- words seen in the training data
        for(String key:goldTest.keySet()){
            
            String cat=seedTrain.get(key);
            if(cat!=null){
                int numcat=cat.equals(CNConstants.PRNOUN)?1:2;

                outSeed.append("N"+dictWords.get(key)+"\tL"+numcat+"\t1.0\n");
                outSeed.flush();
            }
            cat=goldTest.get(key);
            int numcat=cat.equals(CNConstants.PRNOUN)?1:2;
            outGold.append("N"+dictWords.get(key)+"\tL"+numcat+"\t1.0\n");
            outGold.flush();
        }
        //gold

        outSeed.close();
        outSeed.close();
        outGold.close();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                train.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    
   public static void main(String args[]) {
        AnalyzeClassifier analyzing = new AnalyzeClassifier();
        
        /*
        AnalyzeClassifier.TRAINSIZE=20;
        for(int i=0; i<20;i++){
            System.out.println("********** Corpus size (#utts)"+AnalyzeClassifier.TRAINSIZE);
            String sclass="pn";
            File mfile = new File(MODELFILE.replace("%S", sclass));
            mfile.delete();
            mfile = new File(TRAINFILE.replace("%S", sclass));
            mfile.delete();
            mfile = new File(TESTFILE.replace("%S", sclass));
            mfile.delete();
            analyzing.trainAllLinearClassifier(sclass,true,false);
            analyzing.testingClassifier(true,sclass,false);
            LinearClassifier model = analyzing.getModel(sclass);
            double f1=analyzing.testingClassifier(model,TESTFILE.replace("%S", sclass));
            analyzing.testingRForCorpus(sclass,false);
            AnalyzeClassifier.TRAINSIZE+=50;
            break;
            
        }
        //*/
        //trainLinearclassifier(ispn,blsavegroups)
        /*//analyzing.trainAllLinearClassifier(true,true,false);
        analyzing.trainMulticlassNER(false, false);
        String sclass=CNConstants.ALL;
        analyzing.testingClassifier(true,sclass,false);
        //float[] priors = analyzing.computePriors(sclass,analyzing.getModel(sclass));
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
        /*
        //File mfile = new File(MODELFILE.replace("%S", CNConstants.PRNOUN));
        //mfile.delete();
        Long beforeUnsup=System.currentTimeMillis();
        //analyzing.unsupervisedClassifier(CNConstants.PRNOUN,false);
        analyzing.chekingUnsupClassifierNInt(CNConstants.PRNOUN,false);
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
				//Debuggin the Stanford Classifier
            ColumnDataClassifier columnDataClass = new ColumnDataClassifier("slinearclassifier.props");                
            GeneralDataset data = columnDataClass.readTrainingExamples(TRAINFILE.replace("%S", "pn"));
            LinearClassifier model = (LinearClassifier) columnDataClass.makeClassifier(data);        
         
         //*/
        //analyzing.evaluationPOSTAGGER();
        //analyzing.evaluationCLASSRESULTS();
        /*Testing numerical integration
        String sclass="pn";
        
        //analyzing.saveFilesForLClassifier(sclass,true,false);
        analyzing.trainOneClassifier(sclass, false);
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
        //analyzing.generatingArffData(true);
        //analyzing.evaluationKMEANS();
        analyzing.generatingLabelPropGraph();
        //analyzing.evaluatingLabelProp();
    }
  
}
