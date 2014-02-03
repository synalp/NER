/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LinearClassifier;

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
import java.util.Properties;
import java.util.Stack;
import java.util.TreeMap;
import jsafran.DetGraph;
import jsafran.GraphIO;
import utils.ErrorsReporting;
import utils.FileUtils;

/**
 *
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
        
   public  void saveGroups(boolean bltrain){
        for(String str:groupsOfNE)
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
                                            if (group.groupnoms.get(gr).startsWith(en)) {
                                                //int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                //if (debdugroupe==j) lab = en+"B";    
                                                //else lab = en+"I";
                                                lab=en;
                                                break;
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
        
    /**
     * Returns the different models for each type of NE,
     * save the models in a file, so there is no need to retrain each time
     * @param labeled
     * @return 
     */    
    public void trainLinearClassifier(boolean blsavegroups) {
        //TreeMap<String,Double> lcfeatsDict = new TreeMap<>();
        //TreeMap<String,Double> featsDict = new TreeMap<>();
        //save the trainset
        if(blsavegroups)
            saveGroups(true);
        //call the classifier
        modelMap.clear();
        marginMAP.clear();
        for(String str:groupsOfNE){
            ///*
             if(!str.equals("pers"))
                continue;
            //*/
            LinearClassifier model = null;
            File mfile = new File(MODELFILE.replace("%S", str));
            if(!mfile.exists()){
                updatingPropFile(str);
                ColumnDataClassifier columnDataClass = new ColumnDataClassifier("slinearclassifier.props");                
                GeneralDataset data = columnDataClass.readTrainingExamples(TRAINFILE.replace("%S", str));
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
                getValues(TRAINFILE.replace("%S", str),model,featsperInst,labelperInst);
                modelMap.put(str,model);
                featInstMap.put(str,featsperInst);
                lblInstMap.put(str, labelperInst);
                Margin margin = new Margin(model);
                marginMAP.put(str,margin);      
            }
            
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
    public void testingClassifier(boolean isSavingGroups, String smodel){
       if(isSavingGroups)
            saveGroups(false);
       
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
                
                System.out.println(line);
                 
            }
        
            InputStream stderr = process.getErrorStream();
            input = new BufferedReader (new InputStreamReader(stderr)); 
            while(true){
                String line=input.readLine();
                if(line == null)
                    break;
                
                System.out.println("ERROR:"+line);
                 
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
    

    
    public static void main(String args[]) {
        AnalyzeClassifier analyzing = new AnalyzeClassifier();
        ///*
        //analyzing.trainLinearClassifier(false);
        //analyzing.computeFThetaOfX();
        analyzing.testingClassifier(false, "pers");
        //*/
        //analyzing.checkingInstances("pers");
        
        
    }
  
}
