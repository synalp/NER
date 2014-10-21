/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package reco;

import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.util.StringUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Properties;
import jsafran.DetGraph;
import jsafran.GraphIO;
import linearclassifier.AnalyzeLClassifier;
import tools.CNConstants;
import utils.ErrorsReporting;

/**
 * This class implements a linear classifier that tags a word as capitalized or not.
 * The goal is to correct the capitalization of the ASR output
 * @author rojasbar
 */
public class Capitalization {
    public static String MODELFILE="bin.cap.mods";
    public static String TRAINFILE="cap.tab.crf.train";
    public static String TESTFILE="cap.tab.crf.test";
    public static String PROPERTIES_FILE="capitalization.props";
    public static String CAPITALIZATION="CAP";
    public static String OUTFILE="analysis/Reco/CapitalizationCRF.txt";

    private CRFClassifier model = null;
    
    public void saveFilesForClassifier(boolean bltrain) {
            try {
                //if(bltrain&iswiki)
                //WikipediaAPI.loadWiki();
                GraphIO gio = new GraphIO(null);
                OutputStreamWriter outFile =null;
                String xmllist=AnalyzeLClassifier.LISTTRAINFILES;
                if(bltrain)
                    outFile = new OutputStreamWriter(new FileOutputStream(TRAINFILE),CNConstants.UTF8_ENCODING);
                else{
                    xmllist=AnalyzeLClassifier.LISTTESTFILES;
                    outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE),CNConstants.UTF8_ENCODING);
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
                            
                            for (int j=0;j<group.getNbMots();j++) {
                                    nexinutt++;

                                    // calcul du label
                                    String lab = CNConstants.NOCLASS;
                                    int[] groups = group.getGroups(j);
                                    if (groups!=null)
                                        for (int gr : groups) {
                                            
                                           String wordForm= group.getMot(j).getForme();
                                            char[] chars = new char[wordForm.length()];
                                            wordForm.getChars(0, wordForm.length(), chars, 0);
                                            for(int c=0; c<chars.length;c++){
                                                if(Character.isUpperCase(chars[c])){
                                                    lab=CAPITALIZATION;
                                                    break;
                                                }   
                                                
                                                    
                                            }

                                        }
                                        
                                    
                                    outFile.append(group.getMot(j).getForme().toLowerCase()+"\t"+lab+"\n");
                                     
                            }
                            
                            uttCounter++;

                            

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
 
    public void trainCRF(){
        
   
        File mfile = new File(MODELFILE);
        String[]  arrProps = {"-props",PROPERTIES_FILE};
        Properties props = StringUtils.argsToProperties(arrProps);           
        if(!mfile.exists()){
            Properties prop = new Properties();
             try {
            prop.load(new FileInputStream(PROPERTIES_FILE)); // FileInputStream
            prop.setProperty("trainFile", TRAINFILE);
            prop.store(new FileOutputStream(PROPERTIES_FILE),""); // FileOutputStream 
            
            }catch(Exception ex){
                
            } 
  
            model = new CRFClassifier(props);       
            model.train(); 
            try {

                //save the model in a file
                model.serializeClassifier(MODELFILE);
            } catch (Exception ex) {

            }

        }else{
            try {
                model = new CRFClassifier(props);   
                model.loadClassifierNoExceptions(MODELFILE, props);            

            } catch (Exception ex) {
                ex.printStackTrace();
            } 

        }        
      
    } 
    
    public void testingClassifier(String stanfordClassPath){
        OutputStreamWriter outFile = null;
        try {
            outFile=new OutputStreamWriter(new FileOutputStream(OUTFILE));
            //command
            //String cmd="java -Xmx1g -cp  \"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\" edu.stanford.nlp.classify.ColumnDataClassifier -prop slinearclassifier.props groups.pers.tab.lc.train -testFile groups.pers.tab.lc.test > out.txt";

            //String[] call={"java","-Xmx1g","-cp","\"../stanfordNLP/stanford-classifier-2014-01-04/stanford-classifier-3.3.1.jar\"","edu.stanford.nlp.classify.ColumnDataClassifier", "-prop","slinearclassifier.props", "-testFile", TESTFILE.replace("%S", smodel),"> out.txt"};
            //Process process = Runtime.getRuntime().exec(call);
            //stanford-ner-2014-01-04/stanford-ner-2014-01-04.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier
            String cmd="java -Xmx1g -cp "+stanfordClassPath+"  edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier "+MODELFILE+" -testFile "+TESTFILE+ " > capResults.txt";
            Process process = Runtime.getRuntime().exec(cmd);
            InputStream stdout = process.getInputStream();
            
            BufferedReader input = new BufferedReader (new InputStreamReader(stdout)); 
            while(true){
                String line=input.readLine();
                if(line == null)
                    break;
                
                outFile.append(line+"\n");
                outFile.flush();
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
                
                //System.out.println("EVAL: "+line);
                 
            }          
            outFile.close();
        } catch (Exception ex) {
            
            ex.printStackTrace();
        }
        evaluationCLASSRESULTS();
       //System.out.println("ok");
       
    }
    public void evaluationCLASSRESULTS(){

        BufferedReader testFile = null;
        try {
            testFile = new BufferedReader(new InputStreamReader(new FileInputStream(OUTFILE), CNConstants.UTF8_ENCODING));
            
            int tp=0, tn=0, fp=0, fn=0;
            
            for(;;){

                String line = testFile.readLine();   
                
                if(line== null)
                    break;                
                if(line.startsWith("#"))
                    continue;
                
                String values[] = line.split("\\t");
                if(values.length<3)
                    continue;
                
                String label = values[1];
                String recognizedLabel = values[2];
                
                if(recognizedLabel.equals(CAPITALIZATION) && label.equals(CAPITALIZATION))
                    tp++;
                
                if(recognizedLabel.equals(CAPITALIZATION)&& label.equals(CNConstants.NOCLASS))
                    fp++;
                
                if(recognizedLabel.equals(CNConstants.NOCLASS)&&label.equals(CAPITALIZATION))
                    fn++;
                if(recognizedLabel.equals(CNConstants.NOCLASS)&&label.equals(CNConstants.NOCLASS))
                    tn++;

                
            }
            double precision= (double) tp/(tp+fp);
            double recall= (double) tp/(tp+fn);
            double f1=(2*precision*recall)/(precision+recall);
            
            System.out.println("  CAP precision: "+precision);
            System.out.println("  CAP recall: "+recall);
            System.out.println("  CAP f1: "+f1);
            
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
    
   public CRFClassifier getClassifier(){
       if(model==null)
           trainCRF();
       return this.model;
   }
    
    public static void main(String[] args){
        Capitalization cap = new Capitalization();
        /*
        AnalyzeLClassifier.LISTTRAINFILES="esterTrainALL.xmll";
        AnalyzeLClassifier.LISTTESTFILES="esterTestALL.xmll";
        cap.saveFilesForClassifier(true);
        cap.saveFilesForClassifier(false);
        cap.trainCRF();*/
        cap.testingClassifier("/home/rojasbar/development/contnomina/stanfordNLP/stanford-ner-2014-01-04/stanford-ner-2014-01-04.jar");
         
        
    }
}
