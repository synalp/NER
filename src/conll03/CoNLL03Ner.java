/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package conll03;

import CRFClassifier.AnalyzeCRFClassifier;
import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Datum;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import linearclassifier.AnalyzeLClassifier;
import tools.CNConstants;
import tools.GeneralConfig;

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
    
    public void generatingStanfordInputFiles(String entity, String dataset, boolean isCRF, String wSupModelFile){
        BufferedReader inFile = null;
        OutputStreamWriter outFile =null;
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
            }
            if(isCRF){
                AnalyzeCRFClassifier crf = new AnalyzeCRFClassifier();
                if(!wSupModelFile.equals(CNConstants.CHAR_NULL))
                    crf.updatingMappingBkGPropFile(entity,"O","word=0,tag=1,chunk=2,cluster=3,answer=4 ");     
                else
                    crf.updatingMappingBkGPropFile(entity,"O","word=0,tag=1,chunk=2,answer=3");
                
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
                   if(dataset.equals("train") && (!isCRF) &&  uttCount>AnalyzeLClassifier.TRAINSIZE)
                       break;                   
                   uttCount++;
                   continue;
               }    
               String[] cols= line.split("\\s");
               String label=cols[3];
               if(entity.equals(CNConstants.PRNOUN)){
                   String prefix=label.substring(0,label.indexOf("-")+1);
                   if(!prefix.isEmpty())
                    label = CNConstants.PRNOUN;
               }
               if(label.equals("O"))
                   label=CNConstants.OUTCLASS;
               if(isCRF){
                   if(!wSupModelFile.equals(CNConstants.CHAR_NULL)){
                        AnalyzeLClassifier.MODELFILE=wSupModelFile;
                        
                        LinearClassifier wsupModel = AnalyzeLClassifier.loadModelFromFile(wSupModelFile);
                        ColumnDataClassifier columnDataClass = new ColumnDataClassifier(AnalyzeLClassifier.PROPERTIES_FILE);
                        Datum<String, String> datum = columnDataClass.makeDatumFromLine(label+"\t"+cols[0]+"\t"+cols[1]+"\t"+cols[2]+"\n", 0);
                        String outClass = (String) wsupModel.classOf(datum);
                        outFile.append(cols[0]+"\t"+cols[1]+"\t"+cols[2]+"\t"+outClass+label+"\n");
                   }else
                        outFile.append(cols[0]+"\t"+cols[1]+"\t"+cols[2]+"\t"+label+"\n");
                        
               }else
                   outFile.append(label+"\t"+cols[0]+"\t"+cols[1]+"\t"+cols[2]+"\n");

               

           }
            outFile.flush();
            outFile.close();
            inFile.close();            

        }catch(Exception ex){

        }
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
        lcclass.trainAllLinearClassifier(entity, false, false, false);
        lcclass.testingClassifier(false, entity, false, false);
        if(!entity.equals(CNConstants.ALL)){
            float[] priors = {0.9f,0.1f};
            lcclass.setPriors(priors);
        }else{
            float[] priors = {0.3f,0.2f,0.2f,0.15f,0.15f};
            lcclass.setPriors(priors);           
        }    
        //lcclass.wkSupConstrParallelCoordD(entity, true);
        lcclass.wkSupConstrParallelFSCoordD(entity, true);
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
        
        lcclass.wkSupConstrParallelCoordD(entity, true);
    }
    
    public void trainStanfordCRF(String entity, boolean savingFiles, boolean wSupFeat){
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
        File mfile = new File(AnalyzeCRFClassifier.MODELFILE);
        mfile.delete();
        AnalyzeCRFClassifier crfclass= new AnalyzeCRFClassifier();

        crfclass.trainAllCRFClassifier(entity, false, false);
        AnalyzeCRFClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "CRF");
        crfclass.testingClassifier(entity, "../stanfordNLP/stanford-ner-2014-01-04/stanford-ner-2014-01-04.jar");
        evaluatingCRFResults(entity);
    }
    
    public void evaluatingCRFResults(String entity){
        AnalyzeCRFClassifier crfclass= new AnalyzeCRFClassifier(); 
        CRFClassifier crf=crfclass.loadModel(MODELFILE.replace("%S", entity).replace("%CLASS", "CRF"));
                
        for(Object label:crf.labels()){
            crfclass.evaluationCONLLBIOCLASSRESULTS((String) label,"analysis/CRF/test.all.log");
            
        }
        
    }
     public void onlyEvaluatingCRFResults(String entity){
        AnalyzeCRFClassifier crfclass= new AnalyzeCRFClassifier();
        AnalyzeCRFClassifier.MODELFILE=MODELFILE.replace("%S", entity).replace("%CLASS", "CRF");
        AnalyzeCRFClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "CRF");
        crfclass.testingClassifier(entity, "../stanfordNLP/stanford-ner-2014-01-04/stanford-ner-2014-01-04.jar");
        evaluatingCRFResults(entity);
        
    }   
    public void relationFAndR(String entity){
        AnalyzeLClassifier.TRAINSIZE=20;

        generatingStanfordInputFiles(entity, "test", false,CNConstants.CHAR_NULL);
        generatingStanfordInputFiles(entity, "dev", false,CNConstants.CHAR_NULL);
               
        
        AnalyzeLClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeLClassifier.MODELFILE=WKSUPMODEL.replace("%S", entity);    
        for(int i=0; i<20;i++){
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
            AnalyzeLClassifier lcclass = new AnalyzeLClassifier();
            lcclass.trainAllLinearClassifier(entity,false,false,false);
            lcclass.testingClassifier(false,entity,false,false);
            LinearClassifier model = lcclass.getModel(entity);
            double f1=lcclass.testingClassifier(model,TESTFILE.replace("%S", entity).replace("%CLASS", "LC"));
            if(!entity.equals(CNConstants.ALL)){
                float[] priors = {0.9f,0.1f};
                lcclass.setPriors(priors);
            }else{
                float[] priors = {0.3f,0.2f,0.2f,0.15f,0.15f};
                lcclass.setPriors(priors);           
            }             
            lcclass.testingRForCorpus(entity,false);
            AnalyzeLClassifier.TRAINSIZE+=50;
            
            
        }        
    }
    
    public static void main(String[] args){
        CoNLL03Ner conll = new CoNLL03Ner();
        //conll.onlyEvaluatingCRFResults(CNConstants.ALL);
        //conll.trainStanfordCRF(CNConstants.ALL, true, false);
        //conll.evaluatingCRFResults(CNConstants.ALL);
        conll.runningWeaklySupStanfordLC(CNConstants.PRNOUN,true,20);
        //conll.relationFAndR(CNConstants.PRNOUN);
        //conll.runningWeaklySupStanfordLC(CNConstants.ALL,true,20);
        //conll.evaluateOnlyStanfordLC();
        //conll.trainingOnlyWeaklySup(CNConstants.PRNOUN);
    }
}
