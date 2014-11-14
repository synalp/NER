/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package conll03;

import CRFClassifier.AnalyzeCRFClassifier;
import edu.emory.mathcs.backport.java.util.Arrays;
import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Datum;
import gigaword.Conll03Preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import linearclassifier.AnalyzeLClassifier;
import tools.CNConstants;
import tools.GeneralConfig;
import tools.PlotAPI;
import utils.ErrorsReporting;

/**
 * Reads the CoNLL03 corpus
 * @author rojasbar
 *  <!--
 *   Modifications  History
 *   data            author      description
 *   10-Oct-2014     rojasbar      creation.  
 *                  
 * -->
 */
public class CoNLL03Ner {
    
   public static String corpusTrain;
   public static String corpusDev;
   public static String corpusTest;
   public static String corpusDir;
   public static String  TRAINFILE="conll.%S.tab.%CLASS.train";
   public static String  DEVFILE="conll.%S.tab.%CLASS.dev";
   public static String  TESTFILE="conll.%S.tab.%CLASS.test"; 
   public static String  MODELFILE="ner.%S.%CLASS.conll";
   public static String  WKSUPMODEL="bin.%S.lc.wsupmods";
   
   
   
   public CoNLL03Ner(){
       GeneralConfig.loadProperties();
       corpusTrain= GeneralConfig.corpusTrain;
       corpusDev=GeneralConfig.corpusDev;
       corpusTest=GeneralConfig.corpusTest;
       corpusDir=GeneralConfig.corpusDir;
   }
   public CoNLL03Ner(String[] validDirectories){
       GeneralConfig.loadProperties();
       corpusTrain= GeneralConfig.corpusTrain;
       corpusDev=GeneralConfig.corpusDev;
       corpusTest=GeneralConfig.corpusTest;
          
       String dir="";
       for(int i=0; i< validDirectories.length; i++){
           dir+=System.getProperty("file.separator")+corpusTrain;
           File file = new File(dir);
           if(file.exists()){
               corpusDir=validDirectories[i];
               break;
           }
       }
   }
   /**
    * Generate the input files to Stanford Linear Classifier or StanfordNER
    * @param entity
    * @param dataset
    * @param isCRF
    * @param wSupModelFile 
    */ 
   public void generatingStanfordInputFiles(String entity, String dataset, boolean isCRF, String wSupModelFile){
       if(dataset.equals("gigaw"))
          generatingStanfordInputFiles(entity,dataset,isCRF,(isCRF)?0:AnalyzeLClassifier.TESTSIZE,wSupModelFile); 
       else    
          generatingStanfordInputFiles(entity,dataset,isCRF,(isCRF|| (!dataset.equals("train")&&!dataset.equals("tropennlp")))?0:AnalyzeLClassifier.TRAINSIZE,wSupModelFile);
   }
   // I need a bit more flexibility and control over the size of the datasets that are produced
   // so I'ved added this method but still keeping the default behavior the same with the previous method
   public void generatingStanfordInputFiles(String entity, String dataset, boolean isCRF, int limitsize, String wSupModelFile){
        BufferedReader inFile = null;
        OutputStreamWriter outFile =null;
        HashMap<String,String> wordclasses = new HashMap<>();
        List<String> lines = new ArrayList<>();
        List<String> wordInLine= new ArrayList<>();
        try{
            switch(dataset){
                case "train":
                    inFile = new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusTrain)); 
                    
                    if(isCRF)
                        outFile = new OutputStreamWriter(new FileOutputStream(TRAINFILE.replace("%S", entity).replace("%CLASS", "CRF")),CNConstants.UTF8_ENCODING);
                    else
                        outFile = new OutputStreamWriter(new FileOutputStream(TRAINFILE.replace("%S", entity).replace("%CLASS", "LC")),CNConstants.UTF8_ENCODING);
                    break;
                case "dev":
                    inFile = new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusDev)); 
                    
                    if(isCRF)
                        outFile = new OutputStreamWriter(new FileOutputStream(DEVFILE.replace("%S", entity).replace("%CLASS", "CRF")),CNConstants.UTF8_ENCODING);  
                    else
                        outFile = new OutputStreamWriter(new FileOutputStream(DEVFILE.replace("%S", entity).replace("%CLASS", "LC")),CNConstants.UTF8_ENCODING);
                    break;
                case "test":
                    inFile = new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusTest)); 
                    
                    if(isCRF)
                        outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", entity).replace("%CLASS", "CRF")),CNConstants.UTF8_ENCODING);                      
                    else
                        outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", entity).replace("%CLASS", "LC")),CNConstants.UTF8_ENCODING);
                    break;
                case "gigaw":
                    String gwDir=GeneralConfig.corpusGigaDir;
                    String gwData = GeneralConfig.corpusGigaTrain;
                    if(gwDir == null || gwData == null ){
                        ErrorsReporting.report("The GigaWord configuration must be included in the properties file: ner.properties");
                    }
                    inFile = new BufferedReader(new FileReader(gwDir+System.getProperty("file.separator")+gwData)); 
                    TESTFILE=TESTFILE.replace("conll", "gw").replace("%S", entity).replace("%CLASS", "LC");
                    outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE),CNConstants.UTF8_ENCODING);
                    break;  
                case "tropennlp":
                    String newtagData = GeneralConfig.corpusTrainOpenNLP;
                    if(newtagData == null ){
                        ErrorsReporting.report("The conll03 corpus with the OpenNLP tags must be included in the properties file: ner.properties");
                    }                    
                     inFile = new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusTrain)); 
                    
                    if(isCRF)
                        outFile = new OutputStreamWriter(new FileOutputStream(TRAINFILE.replace("%S", entity).replace("%CLASS", "CRF")),CNConstants.UTF8_ENCODING);
                    else
                        outFile = new OutputStreamWriter(new FileOutputStream(TRAINFILE.replace("%S", entity).replace("%CLASS", "LC")),CNConstants.UTF8_ENCODING);
                    break;                   
                 
            }
            if(isCRF){
                AnalyzeCRFClassifier crf = new AnalyzeCRFClassifier();
                if(!wSupModelFile.equals(CNConstants.CHAR_NULL))
                    crf.updatingMappingBkGPropFile(entity,"O","word=0,tag=1,chunk=2,cluster=3,answer=4 ");     
                else
                    crf.updatingMappingBkGPropFile(entity,"O","word=0,tag=1,chunk=2,answer=3");
                
            }else{
                BufferedReader distSemFile = new BufferedReader(new FileReader("scripts/egw.bnc.200.pruned"));
                while(true){
                    String line=distSemFile.readLine();
                    if(line==null)
                        break;
                    String[] cols=line.split("\\s");
                    wordclasses.put(cols[0], cols[1]);
                }
            }
                
            int uttCount=CNConstants.INT_NULL;
            for(;;){
               String line = inFile.readLine();
               
               if(line == null)
                   break;
               if(line.startsWith("-DOCSTART-"))
                   continue;
               //utterance breaking
               if(line.equals("")){
                   if(limitsize>0 &&  uttCount>limitsize) break;                   
                   uttCount++;
                   continue;
               }    
               String[] cols= line.split("\\s");
               String label=CNConstants.OUTCLASS;
               String chunk=CNConstants.CHAR_NULL;
               if(cols.length > 2){
                    label=cols[3];
                    chunk=cols[2];
               }      
               
               if(entity.equals(CNConstants.PRNOUN)){
                   String prefix=label.substring(0,label.indexOf("-")+1);
                   if(!prefix.isEmpty())
                    label = CNConstants.PRNOUN;
               }else if(entity.equals(CNConstants.ALL)){
                   String prefix=label.substring(0,label.indexOf("-")+1);
                   if(!prefix.isEmpty())
                       label = label.replace(prefix, "");
               }
               if(label.equals(CNConstants.OUTCLASS))
                   label=CNConstants.OUTCLASS;
               if(isCRF){
                   if(!wSupModelFile.equals(CNConstants.CHAR_NULL)){
                        AnalyzeLClassifier.MODELFILE=wSupModelFile;
                        
                        LinearClassifier wsupModel = AnalyzeLClassifier.loadModelFromFile(wSupModelFile);
                        ColumnDataClassifier columnDataClass = new ColumnDataClassifier(AnalyzeLClassifier.PROPERTIES_FILE);
                        Datum<String, String> datum = columnDataClass.makeDatumFromLine(label+"\t"+cols[0]+"\t"+cols[1]+"\t"+cols[2]+"\n", 0);
                        String outClass = (String) wsupModel.classOf(datum);
                        outFile.append(cols[0]+"\t"+cols[1]+"\t"+chunk+"\t"+outClass+"\t"+label+"\n");
                        
                   }else
                        outFile.append(cols[0]+"\t"+cols[1]+"\t"+chunk+"\t"+label+"\n");
                        
               }else{
                   String wordClass="";
                   if(wordclasses.containsKey(cols[0]))
                     wordClass=wordclasses.get(cols[0])+"|DISTSIM";
                   else
                     wordClass= CNConstants.CHAR_NULL+"|DISTSIM";
                   
                   lines.add(label+"\t"+cols[0]+"\t"+cols[1]+"\t"+chunk+"\t"+wordClass);
                   wordInLine.add(cols[0]);
                   //outFile.append(label+"\t"+cols[0]+"\t"+cols[1]+"\t"+cols[2]+"\t"+wordClass+"\n");    
               }
               

           }
            
           if(!isCRF){
               
               for(int i=0; i<lines.size();i++){
                   String context="";
                   if(i-2 >= 0)
                       context+=wordInLine.get(i-2)+" "+wordInLine.get(i-1)+" "+wordInLine.get(i)+" ";
                   else if(i-1 >0)
                       context+=wordInLine.get(i-1)+" "+wordInLine.get(i)+" ";
                   else
                       context+=wordInLine.get(i)+" ";
                   if(i+2< lines.size())
                       context+=wordInLine.get(i+1)+" "+wordInLine.get(i+2);
                   else if(i+1 < lines.size())
                       context+=wordInLine.get(i+1);
                   
                  outFile.append(lines.get(i)+"\t"+context+"\n"); 
               }
                  
           }
            
            outFile.flush();
            outFile.close();
            inFile.close();            

        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    public void testingNewWeightsLC(String entity,boolean savingFiles, int trainSize, int testSize){
        AnalyzeLClassifier.TRAINSIZE=trainSize;
        if(savingFiles){
            generatingStanfordInputFiles(entity, "train", false,CNConstants.CHAR_NULL);
            generatingStanfordInputFiles(entity, "test", false,CNConstants.CHAR_NULL);
            generatingStanfordInputFiles(entity, "dev", false,CNConstants.CHAR_NULL);
        }
        AnalyzeLClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeLClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeLClassifier.MODELFILE=WKSUPMODEL.replace("%S", entity);
        //if exist recreates the binary file
        File mfile = new File(AnalyzeLClassifier.MODELFILE);
        File mfile2 = new File(AnalyzeLClassifier.MODELFILE+"_COPY");
        if(mfile.exists()){
            try {
                Files.copy(mfile.toPath(), mfile2.toPath(),StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        AnalyzeLClassifier lcclass= new AnalyzeLClassifier();
        ErrorsReporting.report("Training only on trainset");
        lcclass.trainAllLinearClassifier(entity, false, false, false);
        ColumnDataClassifier columnDataClass = new ColumnDataClassifier(AnalyzeLClassifier.PROPERTIES_FILE);
        columnDataClass.testClassifier(lcclass.getModel(entity), AnalyzeLClassifier.TESTFILE);  
        ErrorsReporting.report("Trainin on the union of the train adn test datasets, but putting test weights to zero");
        lcclass.allweightsKeepingOnlyTrain(entity,trainSize, testSize);
        
        columnDataClass = new ColumnDataClassifier(AnalyzeLClassifier.PROPERTIES_FILE);
        columnDataClass.testClassifier(lcclass.getModel(entity), AnalyzeLClassifier.TESTFILE);  
        
        
    }
    
    public void runningWeaklySupStanfordLC(String entity,boolean savingFiles, int trainSize){
        AnalyzeLClassifier.TRAINSIZE=trainSize;
        if(savingFiles){
            generatingStanfordInputFiles(entity, "train", false,CNConstants.CHAR_NULL);
            generatingStanfordInputFiles(entity, "test", false,CNConstants.CHAR_NULL);
            generatingStanfordInputFiles(entity, "dev", false,CNConstants.CHAR_NULL);
        }
        AnalyzeLClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeLClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeLClassifier.MODELFILE=WKSUPMODEL.replace("%S", entity);
        //if exist recreates the binary file
        File mfile = new File(AnalyzeLClassifier.MODELFILE);
        File mfile2 = new File(AnalyzeLClassifier.MODELFILE+"_COPY");
        if(mfile.exists()){
            try {
                Files.copy(mfile.toPath(), mfile2.toPath(),StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        AnalyzeLClassifier lcclass= new AnalyzeLClassifier();
        
        //lcclass.trainAllLinearClassifier(entity, false, false, false);
        //lcclass.testingClassifier(false, entity, false, false);
        lcclass.allweightsKeepingOnlyTrain(entity,trainSize, Integer.MAX_VALUE);
        
        ColumnDataClassifier columnDataClass = new ColumnDataClassifier(AnalyzeLClassifier.PROPERTIES_FILE);
        columnDataClass.testClassifier(lcclass.getModel(entity), AnalyzeLClassifier.TESTFILE);
        HashMap<String,Double> priorsMap = new HashMap<>();
        
        if(!entity.equals(CNConstants.ALL)){
            priorsMap.put("O", new Double(0.8));
            priorsMap.put(CNConstants.PRNOUN, new Double(0.2));

        }else{
            priorsMap.put("O", new Double(0.76));
            priorsMap.put("PER", new Double(0.1)); 
            priorsMap.put(CNConstants.ORG.toUpperCase(), new Double(0.05)); 
            priorsMap.put(CNConstants.LOC.toUpperCase(), new Double(0.06)); 
            priorsMap.put("MISC", new Double(0.03)); 
            
        } 
        
        /*
        float[] priors=lcclass.computePriors(entity, lcclass.getModel(entity));
        System.out.println(lcclass.getPriorsMap().toString());
        System.out.println(Arrays.toString(priors));
        */
        lcclass.setPriors(priorsMap);  
       
        //lcclass.wkSupParallelCoordD(entity, true);
        //lcclass.wkSupParallelFSCoordD(entity, true,2000);
        lcclass.wkSupParallelStocCoordD(entity, true,10000);
	//lcclass.wkSupClassifierConstr(entity, true,2000);
    }
    /**
     * Uses GigaWord as testdata (unlabeled data)
     * @param entity
     * @param savingFiles 
     */
    public void runningWeaklySupStanfordLC(String entity,boolean savingFiles, int trainSize, int testSize){
        AnalyzeLClassifier.TRAINSIZE=trainSize;
        AnalyzeLClassifier.TESTSIZE=testSize;
        if(savingFiles){
            generatingStanfordInputFiles(entity, "tropennlp", false,CNConstants.CHAR_NULL);
            generatingStanfordInputFiles(entity, "gigaw", false,CNConstants.CHAR_NULL);
            //generatingStanfordInputFiles(entity, "dev", false,CNConstants.CHAR_NULL);
        }
        AnalyzeLClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeLClassifier.TESTFILE=TESTFILE;
        AnalyzeLClassifier.MODELFILE=WKSUPMODEL.replace("%S", entity);
        //if exist recreates the binary file
        File mfile = new File(AnalyzeLClassifier.MODELFILE);
        File mfile2 = new File(AnalyzeLClassifier.MODELFILE+"_COPY");
        if(mfile.exists()){
            try {
                Files.copy(mfile.toPath(), mfile2.toPath(),StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        AnalyzeLClassifier lcclass= new AnalyzeLClassifier();
        
        //lcclass.trainAllLinearClassifier(entity, false, false, false);
        //lcclass.testingClassifier(false, entity, false, false);
        lcclass.allweightsKeepingOnlyTrain(entity,trainSize,testSize);
        
        ColumnDataClassifier columnDataClass = new ColumnDataClassifier(AnalyzeLClassifier.PROPERTIES_FILE);
        columnDataClass.testClassifier(lcclass.getModel(entity), AnalyzeLClassifier.TESTFILE);        
        HashMap<String,Double> priorsMap = new HashMap<>();
        
        if(!entity.equals(CNConstants.ALL)){
            priorsMap.put("O", new Double(0.8));
            priorsMap.put(CNConstants.PRNOUN, new Double(0.2));

        }else{
            priorsMap.put("O", new Double(0.76));
            priorsMap.put("PER", new Double(0.1)); 
            priorsMap.put(CNConstants.ORG.toUpperCase(), new Double(0.05)); 
            priorsMap.put(CNConstants.LOC.toUpperCase(), new Double(0.06)); 
            priorsMap.put("MISC", new Double(0.03)); 
            
        } 
        
        /*
        float[] priors=lcclass.computePriors(entity, lcclass.getModel(entity));
        System.out.println(lcclass.getPriorsMap().toString());
        System.out.println(Arrays.toString(priors));
        */
        lcclass.setPriors(priorsMap);  
       
        //lcclass.wkSupParallelCoordD(entity, true);
        //lcclass.wkSupParallelFSCoordD(entity, true,2000);
        lcclass.wkSupParallelStocCoordD(entity, true,10000);
	//lcclass.wkSupClassifierConstr(entity, true,2000);
    }
   
    
    public void evaluateOnlyStanfordLC(String entity){
        AnalyzeLClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeLClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeLClassifier.MODELFILE=WKSUPMODEL.replace("%S", entity);
        AnalyzeLClassifier lcclass = new AnalyzeLClassifier();
        lcclass.testingClassifier(false, entity, false, false);        
    }
    
    public void trainingOnlyWeaklySup(String entity){
        AnalyzeLClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeLClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeLClassifier.MODELFILE=WKSUPMODEL.replace("%S", entity);
        AnalyzeLClassifier lcclass = new AnalyzeLClassifier();
        lcclass.testingClassifier(false, entity, false, false); 
        
        lcclass.wkSupParallelCoordD(entity, true,2000);
    }
    /**
     * 
     * @param entity  classifier "all" for all the entities "pn" for the binary classification : proper noun/ not proper noun
     * @param savingFiles, true if it must generate the input files to StanfordNER
     * @param wSupFeat, true if it uses the weakly supervised model as feature "NOT WORKING YET"
     * @param useExistingModel , true if it uses an existing binary model file
     */
    public float trainStanfordCRF(String entity, boolean savingFiles, boolean wSupFeat, boolean useExistingModel){
        if(savingFiles){
            String wsupModel=CNConstants.CHAR_NULL;
            
            if(wSupFeat)
                wsupModel=WKSUPMODEL.replace("%S", entity);
            
            generatingStanfordInputFiles(entity, "train", true,wsupModel);
            generatingStanfordInputFiles(entity, "test", true,wsupModel);
            generatingStanfordInputFiles(entity, "dev", true,wsupModel);
        }
        AnalyzeCRFClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "CRF");
        AnalyzeCRFClassifier.MODELFILE=MODELFILE.replace("%S", entity).replace("%CLASS", "CRF");
        //if exist recreates the binary file
        if(!useExistingModel){
            File mfile = new File(AnalyzeCRFClassifier.MODELFILE);
            mfile.delete();
        }
        AnalyzeCRFClassifier crfclass= new AnalyzeCRFClassifier();

        crfclass.trainAllCRFClassifier(entity, false, false);
        AnalyzeCRFClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "CRF");
        crfclass.testingClassifier(entity, CNConstants.SNERJAR);
        AnalyzeCRFClassifier.OUTFILE=AnalyzeCRFClassifier.OUTFILE.replace("%S", entity);
        evaluatingCRFResults(entity);
        return conllEvaluation(AnalyzeCRFClassifier.OUTFILE);
    }
    
    public static void evaluatingCRFResults(String entity){
        AnalyzeCRFClassifier crfclass= new AnalyzeCRFClassifier(); 
        CRFClassifier crf=crfclass.loadModel(MODELFILE.replace("%S", entity).replace("%CLASS", "CRF"));
        AnalyzeCRFClassifier.OUTFILE=AnalyzeCRFClassifier.OUTFILE.replace("%S", entity);        
        for(Object label:crf.labels()){
            
            crfclass.evaluationCONLLBIOCLASSRESULTS((String) label,AnalyzeCRFClassifier.OUTFILE);
            
        }
        
    }
  
    public float conllEvaluation(String results){
    	float f1=0;
        try {
            //command
            String cmd="./scripts/evalconll.sh "+results;
            
            System.out.println(cmd);
            
            Process process = Runtime.getRuntime().exec(cmd);
            InputStream stdout = process.getInputStream();
            
            BufferedReader input = new BufferedReader (new InputStreamReader(stdout)); 
            while(true){
                String line=input.readLine();
                if(line == null) break;
                System.out.println(line);
                String s=line.trim();
                if (s.startsWith("accuracy")) {
                	StringTokenizer st = new StringTokenizer(s);
                	st.nextToken();
                	st.nextToken();
                	st.nextToken();
                	st.nextToken();
                	st.nextToken();
                	st.nextToken();
                	st.nextToken();
                	f1=Float.parseFloat(st.nextToken());
                }
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
       return f1;
    }     
     public void onlyEvaluatingCRFResults(String entity){
        AnalyzeCRFClassifier crfclass= new AnalyzeCRFClassifier();
        AnalyzeCRFClassifier.MODELFILE=MODELFILE.replace("%S", entity).replace("%CLASS", "CRF");
        AnalyzeCRFClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "CRF");
        crfclass.testingClassifier(entity, CNConstants.SNERJAR);
        AnalyzeCRFClassifier.OUTFILE=AnalyzeCRFClassifier.OUTFILE.replace("%S", entity);
        evaluatingCRFResults(entity);
        
    }   
    public void relationFAndR(String entity){
        AnalyzeLClassifier.TRAINSIZE=20;
        AnalyzeLClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeLClassifier.MODELFILE=WKSUPMODEL.replace("%S", entity);  
        AnalyzeLClassifier lcclass = new AnalyzeLClassifier();
        PlotAPI plotR = new PlotAPI("R vs Iterations","Num of Iterations", "R");
        PlotAPI plotF1 = new PlotAPI("F1 vs Iterations","Num of Iterations", "F1");
        generatingStanfordInputFiles(entity, "test", false,CNConstants.CHAR_NULL);
        generatingStanfordInputFiles(entity, "dev", false,CNConstants.CHAR_NULL);
        HashMap<String,Double> priorsMap = new HashMap<>();
        if(!entity.equals(CNConstants.ALL)){
            priorsMap.put("O", new Double(0.8));
            priorsMap.put(CNConstants.PRNOUN, new Double(0.2));

        }else{
            priorsMap.put("O", new Double(0.8));
            priorsMap.put("I-"+CNConstants.PERS.toUpperCase(), new Double(0.06)); 
            priorsMap.put("I-"+CNConstants.ORG.toUpperCase(), new Double(0.04)); 
            priorsMap.put("I-"+CNConstants.LOC.toUpperCase(), new Double(0.07)); 
            priorsMap.put("I-MISC", new Double(0.03)); 
        }     
        lcclass.setPriors(priorsMap);                   
   
        for(int i=0,k=0; i<20;i++){
            System.out.println("********** Corpus size (#utts)"+AnalyzeLClassifier.TRAINSIZE);
           
            File mfile = new File(AnalyzeLClassifier.MODELFILE);
            File mfile2 = new File(AnalyzeLClassifier.MODELFILE+"_COPY");
            if(mfile.exists()){
                try {
                    Files.copy(mfile.toPath(), mfile2.toPath(),StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }  
                mfile.delete();
            }
            generatingStanfordInputFiles(entity, "train", false,CNConstants.CHAR_NULL);
            AnalyzeLClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "LC");
            
            lcclass.trainAllLinearClassifier(entity,false,false,false);
            lcclass.testingClassifier(false,entity,false,false);
            LinearClassifier model = lcclass.getModel(entity);
            double f1=lcclass.testingClassifier(model,TESTFILE.replace("%S", entity).replace("%CLASS", "LC"));
            

            float r=lcclass.testingRForCorpus(entity,false);
            plotR.addPoint(k, r);
            plotF1.addPoint(k, f1);k++;
            AnalyzeLClassifier.TRAINSIZE+=50;
            
            
        }        
    }
    /**
     * Experiments of weakly supervised + CRF
     * @param trainSize
     * @param testSize 
     */
    public void experimentsCRFPlusWkSup(int trainSize){
        runningWeaklySupStanfordLC(CNConstants.PRNOUN,true,trainSize);
        trainStanfordCRF(CNConstants.ALL, true, true,false);
    }
  
    public void experimentsCRFPlusWkSupGWord(int trainSize, int testSize){
        runningWeaklySupStanfordLC(CNConstants.PRNOUN,true,trainSize,testSize);
        trainStanfordCRF(CNConstants.ALL, true, true,false);
    }    
    
    public static final String[] TASKS = {
    	"basecrf", "buildGigaword","weaklySupGW","crfwsfeat","opennlptags", "weaklySupConll","expGWord"
    };
    
    public static void main(String[] args){
    	int task=0;
    	if (args.length>0) {
    		for (int i=0;i<TASKS.length;i++) 
                    if (args[0].equals(TASKS[i])){
                        System.out.println(args[0]);
                      task=i;break;
                    }
    	}
    	
        CoNLL03Ner conll = new CoNLL03Ner();
        switch(task) {
        case 0:
        	// train and test the baseline CRF
        	conll.trainStanfordCRF(CNConstants.ALL, true, false,false);
        	break;

        case 1:
        	// segment, tokenize and tag the Gigaword corpus; this shall be done only once !
        	Conll03Preprocess.tagGigaword(null);
        	break;
        case 2:
                //testset = gigaword
                conll.runningWeaklySupStanfordLC(CNConstants.PRNOUN,true,500,500);
                break;
        case 3:
                conll.trainStanfordCRF(CNConstants.ALL, true, true,false);
                break;            
        case 4:
        	// retag the Conll03 corpus with openNLP: this'll be used to run weakly supervised training of the linear classifier on it
        	Conll03Preprocess.retagConll03();
        	break;
        case 5:
                conll.runningWeaklySupStanfordLC(CNConstants.PRNOUN,true,500);
                //conll.testingNewWeightsLC(CNConstants.PRNOUN, true, 500);
                break;
        case 6:
               conll.experimentsCRFPlusWkSupGWord(50, 500);
        }
        
        // PLEASE DONT UNCOMMENT ANY LINE BELOW! rather add a task and arg on the command-line  
        
        //conll.generatingStanfordInputFiles(CNConstants.ALL, "train", false, CNConstants.CHAR_NULL);
        //conll.onlyEvaluatingCRFResults(CNConstants.ALL);
        //conll.trainStanfordCRF(CNConstants.ALL, true, false,false);
        //CoNLL03Ner.evaluatingCRFResults(CNConstants.ALL);


        //conll.trainStanfordCRF(CNConstants.ALL, true, false,false);

        // CoNLL03Ner.evaluatingCRFResults(CNConstants.ALL);

        //conll.conllEvaluation("test.all.log");
        //conll.trainStanfordCRF(CNConstants.ALL, true, false);
        // CoNLL03Ner.evaluatingCRFResults(CNConstants.ALL, "mures.out");
        //conll.runningWeaklySupStanfordLC(CNConstants.PRNOUN,true,Integer.MAX_VALUE);


        //conll.runningWeaklySupStanfordLC(CNConstants.PRNOUN,true);

    
        //conll.relationFAndR(CNConstants.PRNOUN);
        //conll.runningWeaklySupStanfordLC(CNConstants.ALL,true,20);
        //conll.evaluateOnlyStanfordLC();
        //conll.trainingOnlyWeaklySup(CNConstants.PRNOUN);
        
    }
}
