/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package linearclassifier;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import edu.stanford.nlp.classify.ColumnDataClassifier;


import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.Datum;

import gmm.GMMDiag;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Stack;
import java.util.TreeMap;

import jsafran.DetGraph;
import jsafran.GraphIO;
import resources.WikipediaAPI;
import tools.CNConstants;
import tools.Histoplot;
import tools.PlotAPI;
import utils.ErrorsReporting;
import utils.FileUtils;

/**
 * This class process the instances and features by instance in the Stanford linear classifier 
 * @author rojasbar
 */
public class AnalyzeClassifier {
    
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
    public static int    TRAINSIZE=20;  //Integer.MAX_VALUE;
    
    
    private HashMap<String, LinearClassifier> modelMap = new HashMap<>();
    private HashMap<String,Margin> marginMAP = new HashMap<>();
    private int numInstances=0;

    private HashMap<String, List<List<Integer>>> featInstMap = new HashMap<>();
    private HashMap<String, List<Integer>> lblInstMap = new HashMap<>();
    
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
                
            }
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
                
                if(!line.startsWith("Cls"))
                    continue;
                System.out.println(line);
                 
            }
        
            InputStream stderr = process.getErrorStream();
            input = new BufferedReader (new InputStreamReader(stderr)); 
            while(true){
                String line=input.readLine();
                if(line == null)
                    break;
                if(!line.startsWith("Cls"))
                    continue;
                
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
        return columnDataClass.f1;
        
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
        System.out.println("mean 00 "+gmm.getMean(0, 0));
        System.out.println("mean 01 "+gmm.getMean(0, 1));
        System.out.println("mean 10 "+gmm.getMean(1, 0));
        System.out.println("mean 11 "+gmm.getMean(1, 1));
        System.out.println("GMM trained");
        
        //return computeR(gmm, priors,marginMAP.get(sclassifier).getNlabs() );
        return computeR(gmm, priors,true); //xtof
        
    }
    
    static float computeRNumInt(GMMDiag gmm, final float[] py, int nLabels) {
        float risk=0f;
        
        MonteCarloIntegration mcInt = new MonteCarloIntegration();
        //double[] mvIntegral= mcInt.integrate(gmm, CNConstants.UNIFORM, true);
        mcInt.errorAnalysis(gmm,py,nLabels);
        for(int y=0;y<nLabels;y++){
            //arguments gmm, distribution of the proposal, metropolis, is plot
            risk+=py[y]*mcInt.integrate(gmm,y,CNConstants.UNIFORM, true,false);
        }
        
        return risk;
    }
    public float computeROfThetaNumInt(String sclassifier) {

        //final float[] priors = computePriors(sclassifier,model);
        final float[] priors = {0.9f,0.1f};
        // get scores
        GMMDiag gmm = new GMMDiag(2, priors);
        gmm.setClassifier(sclassifier);
        gmm.train(this, marginMAP.get(sclassifier));
        System.out.println("GMM trained");
        
        //return computeR(gmm, priors,marginMAP.get(sclassifier).getNlabs() );
        return computeRNumInt(gmm, priors,marginMAP.get(sclassifier).getNlabs() ); //xtof
        
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
   public void unsupervisedClassifier(String sclass) {
        PlotAPI plotR = new PlotAPI("R vs Iterations","Num of Iterations", "R");
        PlotAPI plotF1 = new PlotAPI("F1 vs Iterations","Num of Iterations", "F1");
        
      
        final int niters = 10000;
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
        float estimr0 = computeROfThetaNumInt(sclass);
        System.out.println("init R "+estimr0);
        plotR.addPoint(counter, estimr0);
        double f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
        plotF1.addPoint(counter,f1);
        
        System.out.println("Number of features" + margin.getNfeats());
        for (int iter=0;iter<niters;iter++) {
            double[][] weightsForFeat=margin.getWeights();
            final float[] gradw = new float[weightsForFeat.length];
            //for(int i=0;i<weightsForFeat.length;i++){
            for(int i=0;i<selectedFeats.length;i++){
                int featIdx = selectedFeats[i];
                for(int w=0;w < weightsForFeat[featIdx].length;w++){
                    float w0 = (float) weightsForFeat[featIdx][w];
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
                        float estimr = computeROfThetaNumInt(sclass);
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
            estimr0 = computeROfThetaNumInt(sclass);
            System.out.println("*******************************"); 
            System.out.println("R estim ["+iter+"] = "+estimr0);     
            plotR.addPoint(counter, estimr0);
            System.out.println("*******************************");
            model.setWeights(weightsForFeat);
            f1=testingClassifier(model,TESTFILE.replace("%S", sclass));
            plotF1.addPoint(counter, f1);
            System.out.println("*******************************"); 
                
            Histoplot.showit(margin.getScoreForAllInstancesLabel0(featsperInst,scores), featsperInst.size());
            }
            
        }
        for(String emptyW:emptyfeats){
            System.out.println(emptyW);
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
                
                if(label.equals(CNConstants.PRNOUN)&&!pos.equals(CNConstants.POSTAGNAM))
                    fn++;
                if(label.equals(CNConstants.NOCLASS)&& pos.equals(CNConstants.POSTAGNAM))
                    fn++;

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
   
   public void setFeaturesPerInst(HashMap<String, List<List<Integer>>>  fMap){
       this.featInstMap=fMap;
   }
   
   public void setLabelsPerInst(HashMap<String, List<Integer>> lMap){
       this.lblInstMap=lMap;
   }
   
    public static void main(String args[]) {
        AnalyzeClassifier analyzing = new AnalyzeClassifier();
        
        ///*
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
            
        }
        //*/
        //trainLinearclassifier(ispn,blsavegroups)
        /*//analyzing.trainAllLinearClassifier(true,true,false);
        analyzing.trainMulticlassNER(false, false);
        String sclass=CNConstants.ALL;
        analyzing.testingClassifier(true,sclass,false);
        //float[] priors = analyzing.computePriors(sclass,analyzing.getModel(sclass));
        //*/
        /*
        String sclass=CNConstants.PERS;
        analyzing.trainAllLinearClassifier(sclass,true,false);
        analyzing.testingClassifier(true,sclass,false);
        */
        /*
        analyzing.trainAllLinearClassifier(CNConstants.PRNOUN,true,false);
        String sclass="pn";
        LinearClassifier model = analyzing.getModel(sclass);
        analyzing.testingClassifier(model, TESTFILE.replace("%S", sclass));
        //*/
        //analyzing.computeFThetaOfX();
        //analyzing.computeROfTheta("pn");
        //testingClassifier(ispn,blsavegroups,smodel,iswiki)
       
        //analyzing.testingClassifier(true, "pn",false);
        //*/
        //analyzing.checkingInstances("pers");
        //computing the risk
        ///*
        File mfile = new File(MODELFILE.replace("%S", CNConstants.PRNOUN));
        mfile.delete();
        analyzing.unsupervisedClassifier(CNConstants.PRNOUN);
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
        /*Testing numerical integration
        String sclass="pn";
        analyzing.trainOneClassifier(sclass,false);  
        List<List<Integer>> featsperInst = new ArrayList<>(); 
        List<Integer> labelperInst = new ArrayList<>();     
        LinearClassifier model = analyzing.getModel(sclass);
        analyzing.getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
        analyzing.featInstMap.put(sclass,featsperInst);
        analyzing.lblInstMap.put(sclass, labelperInst);   
        float  risk = analyzing.computeROfTheta(sclass);
        System.out.println("Analitical sol: "+risk);
                
        risk = analyzing.computeROfThetaNumInt(sclass);
        System.out.println("MCIntegration: "+risk);

        //*/
    }
  
}
