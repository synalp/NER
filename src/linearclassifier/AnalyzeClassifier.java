/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package linearclassifier;

import edu.stanford.nlp.classify.LinearClassifier;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import edu.stanford.nlp.classify.ColumnDataClassifier;


import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.stats.Distribution;
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
import java.io.OutputStreamWriter;
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
    public static String ONLYONEPNOUNCLASS="pn";
    public static String[] groupsOfNE = {"pers","org", "loc", "prod"};
    
    
    private HashMap<String, LinearClassifier> modelMap = new HashMap<>();
    private HashMap<String,Margin> marginMAP = new HashMap<>();
    private int numInstances=0;

    private HashMap<String, List<List<Integer>>> featInstMap = new HashMap<>();
    private HashMap<String, List<Integer>> lblInstMap = new HashMap<>();
    
    public AnalyzeClassifier(){

    }
    public void updatingPropFile(String nameEntity){
       

        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(PROPERTIES_FILE)); // FileInputStream
            prop.setProperty("trainFile", TRAINFILE.replace("%S", nameEntity));
            prop.store(new FileOutputStream(PROPERTIES_FILE),""); // FileOutputStream 
        } catch (Exception ex) {
            ex.printStackTrace();
        }
   }     
        
   public  void saveGroups(boolean ispn,boolean bltrain){
       //only one proper noun classifier
       String[] classStr={ONLYONEPNOUNCLASS};
       if(!ispn)
           classStr=groupsOfNE;
       
       for(String str:classStr)
           saveFilesForLClassifier(str,bltrain);

    }
        
    public void saveFilesForLClassifier(String en, boolean bltrain) {
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
                                                }
                                            }
                                        }
                                            
                                    if(!isStopWord(group.getMot(j).getPOS()))
                                        outFile.append(lab+"\t"+group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\n");
                            }
                            /*
                            if (nexinutt>0)
                                outFile.append("NO\tES\tES\n");
                            else
                                outFile.append("NO\tES\tES\n");
                            */
                    }
                }
                outFile.flush();
                outFile.close();
                inFile.close();
                ErrorsReporting.report("groups saved in groups.*.tab");
            } catch (IOException e) {
                    e.printStackTrace();
            }
    }   
    
    public void trainOneClassifier(String sclassifier){
        LinearClassifier model = null;
        File mfile = new File(MODELFILE.replace("%S", sclassifier));
        if(!mfile.exists()){
            updatingPropFile(sclassifier);
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
            List<List<Integer>> featsperInst = new ArrayList<>(); 
            List<Integer> labelperInst = new ArrayList<>(); 
            //train data
            modelMap.put(sclassifier,model);
            Margin margin = new Margin(model);
            marginMAP.put(sclassifier,margin);  
            //compute the values for instances in the testset
            if(!(new File(TESTFILE.replace("%S", sclassifier)).exists()))
                saveGroups((sclassifier.equals(ONLYONEPNOUNCLASS)),false);
            getValues(TESTFILE.replace("%S", sclassifier),model,featsperInst,labelperInst);
            featInstMap.put(sclassifier,featsperInst);
            lblInstMap.put(sclassifier, labelperInst);
    
        }        
    }
        
    /**
     * Returns the different models for each type of NE,
     * save the models in a file, so there is no need to retrain each time
     * @param labeled
     * @return 
     */    
    public void trainAllLinearClassifier(boolean ispn,boolean blsavegroups) {
        //TreeMap<String,Double> lcfeatsDict = new TreeMap<>();
        //TreeMap<String,Double> featsDict = new TreeMap<>();
        //save the trainset
        if(blsavegroups)
            saveGroups(ispn,true);
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
    public void testingClassifier(boolean ispn,boolean isSavingGroups, String smodel){
       if(isSavingGroups)
            saveGroups(ispn,false);
       
       if(ispn)
           smodel=ONLYONEPNOUNCLASS;
       
       updatingPropFile(smodel);
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
    public void testingClassifier(LinearClassifier model, String testfile){
        ColumnDataClassifier columnDataClass = new ColumnDataClassifier(PROPERTIES_FILE);
        columnDataClass.testClassifier(model, testfile);
       
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

        List<Integer> vals=lblInstMap.get(sclassifier);
        int nTargetClass=0;

        for(int i=0;i<vals.size();i++){
            String label = (String) new ArrayList(model.labels()).get(vals.get(i));
            if(label.equals(sclassifier))
                nTargetClass++;
        }
       prob = (float) nTargetClass/vals.size();        
       priors[0]=prob;
       priors[1]=1-prob;
               
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
            
            for(int i=0;i<nLabels;i++)
                for(int j=0;j<nLabels;j++){
            
                var[i][j] = (float)gmm.getVar(i, j, j);
                mean[i][j] = (float)gmm.getMean(i, j);
     
                
            }
            
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
        System.out.println("GMM trained");
        return computeR(gmm, priors,marginMAP.get(sclassifier).getNlabs() );
        
    }
   /**
     * The gradient method used ins the Finite Difference
     * f'(a) is approximately (f(a+h)-f(a))/h
     * @param sclass 
     */ 
   public void unsupervisedClassifier(String sclass) {
        final int niters = 10000;
        final float eps = 0.1f;   
        
        if(!modelMap.containsKey(sclass))
            trainOneClassifier(sclass);        
        LinearClassifier model = modelMap.get(sclass);
        HashSet<String> emptyfeats = new HashSet<>();
        System.out.println("Working with classifier "+sclass);
        float estimr0 = computeROfTheta(sclass);
        System.out.println("init R "+estimr0);
        testingClassifier(model,TESTFILE.replace("%S", sclass));
        
        Margin margin = marginMAP.get(sclass);
        for (int iter=0;iter<niters;iter++) {
            double[][] weightsForFeat=margin.getWeights();
            final float[] gradw = new float[weightsForFeat.length];
            for(int i=0;i<weightsForFeat.length;i++){
                for(int w=0;w < weightsForFeat[i].length;w++){
                    float w0 = (float) weightsForFeat[i][w];
                    if (emptyfeats.contains("["+i+","+w+"]")) continue;
                    float delta = 0.5f;
                    /*for (int j=0;;j++) {
                        if(j>10)
                            break;*/
                        System.out.println("before weight= "+w0);
                        weightsForFeat[i][w] = w0 + w0*delta;
                        System.out.println("after delta= "+ delta);
                        System.out.println("after w0 + w0*delta= "+ (w0 + w0*delta));
                        System.out.println("after weight= "+weightsForFeat[i][w]);
                        //TODO:updating the new weights in the gmm?
                        float estimr = computeROfTheta(sclass);
                        System.out.println("For feat["+ i +"] weight["+ w +"] R estim ["+iter+"] = "+estimr0);    
                        gradw[w] = (estimr-estimr0)/(w0*delta);
                        System.out.println("grad "+gradw[w]);
                        // we don't go above 10 because some weights may not be used at all
                        /*if (gradw[w]==0 && delta<10) delta*=2f;
                        else if (gradw[w]>0.1||gradw[w]<-0.1) delta/=2f;
                        else break;*/
                    //}
                    
                    weightsForFeat[i][w]=w0;    
                }
                for(int w=0;w < weightsForFeat[i].length;w++){ 
                    if (gradw[w]==0) 
                            emptyfeats.add("["+i+","+w+"]");
                    else    
                        weightsForFeat[i][w] -= gradw[w] * eps;
                                     
                }
                estimr0 = computeROfTheta(sclass);
                System.out.println("*******************************"); 
                System.out.println("R estim ["+iter+"] = "+estimr0);            
                System.out.println("*******************************");
                model.setWeights(weightsForFeat);
                testingClassifier(model,TESTFILE.replace("%S", sclass));
                System.out.println("*******************************");

            }
        }
        for(String emptyW:emptyfeats){
            System.out.println(emptyW);
        }
        
   }      
    
    public static void main(String args[]) {
        AnalyzeClassifier analyzing = new AnalyzeClassifier();
        ///*
        //trainLinearclassifier(ispn,blsavegroups)
        analyzing.trainAllLinearClassifier(true,true);
        //analyzing.computeFThetaOfX();
        //analyzing.computeROfTheta("pn");
        //testingClassifier(ispn,blsavegroups,smodel)
        analyzing.testingClassifier(true,true, "");
        //*/
        //analyzing.checkingInstances("pers");
        //computing the risk
        analyzing.unsupervisedClassifier("pn");
        
    }
  
}
