/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package conll03;

import CRFClassifier.AnalyzeCRFClassifier;
import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Datum;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.List;
import linearclassifier.AnalyzeClassifier;
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
       corpusTrain= GeneralConfig.conll03Train;
       corpusDev=GeneralConfig.conll03Dev;
       corpusTest=GeneralConfig.conll03Test;
       corpusDir=GeneralConfig.conll03Dir;
   }
   public CoNLL03Ner(String[] validDirectories){
       GeneralConfig.loadProperties();
       corpusTrain= GeneralConfig.conll03Train;
       corpusDev=GeneralConfig.conll03Dev;
       corpusTest=GeneralConfig.conll03Test;
          
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
                    outFile = new OutputStreamWriter(new FileOutputStream(TRAINFILE.replace("%S", entity).replace("%CLASS", "LC")),CNConstants.UTF8_ENCODING);
                    if(isCRF)
                        outFile = new OutputStreamWriter(new FileOutputStream(TRAINFILE.replace("%S", entity).replace("%CLASS", "CRF")),CNConstants.UTF8_ENCODING);
                    break;
                case "dev":
                    inFile = new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusDev)); 
                    outFile = new OutputStreamWriter(new FileOutputStream(DEVFILE.replace("%S", entity).replace("%CLASS", "LC")),CNConstants.UTF8_ENCODING);
                    if(isCRF)
                        outFile = new OutputStreamWriter(new FileOutputStream(DEVFILE.replace("%S", entity).replace("%CLASS", "CRF")),CNConstants.UTF8_ENCODING);  
                    break;
                case "test":
                    inFile = new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusTest)); 
                    outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", entity).replace("%CLASS", "LC")),CNConstants.UTF8_ENCODING);
                    if(isCRF)
                        outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", entity).replace("%CLASS", "CRF")),CNConstants.UTF8_ENCODING);                      
                    break;
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
                   if(dataset.equals("train") &&  uttCount>AnalyzeClassifier.TRAINSIZE)
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
                   label=CNConstants.NOCLASS;
               if(isCRF){
                   if(!wSupModelFile.equals(CNConstants.CHAR_NULL)){
                        AnalyzeClassifier.MODELFILE=wSupModelFile;
                        
                        LinearClassifier wsupModel = AnalyzeClassifier.loadModelFromFile(wSupModelFile);
                        ColumnDataClassifier columnDataClass = new ColumnDataClassifier(AnalyzeClassifier.PROPERTIES_FILE);
                        Datum<String, String> datum = columnDataClass.makeDatumFromLine(label+"\t"+cols[0]+"\t"+cols[1]+"\t"+cols[2]+"\n", 0);
                        String outClass = (String) wsupModel.classOf(datum);
                        AnalyzeCRFClassifier crf = new AnalyzeCRFClassifier();
                        crf.updatingMappingPropFile(entity,"word=0, tag=1, chunk=2, cluster=3");
                        outFile.append(cols[0]+"\t"+cols[1]+"\t"+cols[2]+"\t"+outClass+label);
                   }else
                        outFile.append(cols[0]+"\t"+cols[1]+"\t"+cols[2]+"\t"+label);
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
        AnalyzeClassifier.TRAINSIZE=trainSize;
        if(savingFiles){
            generatingStanfordInputFiles(entity, "train", false,CNConstants.CHAR_NULL);
            generatingStanfordInputFiles(entity, "test", false,CNConstants.CHAR_NULL);
            generatingStanfordInputFiles(entity, "dev", false,CNConstants.CHAR_NULL);
        }
        AnalyzeClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeClassifier.MODELFILE=WKSUPMODEL.replace("%S", entity);
        //if exist recreates the binary file
        File mfile = new File(AnalyzeClassifier.MODELFILE);
        mfile.delete();
        AnalyzeClassifier lcclass= new AnalyzeClassifier();
        lcclass.trainAllLinearClassifier(entity, false, false, false);
        lcclass.testingClassifier(false, entity, false, false);
            
        lcclass.wkSupConstrParallelCoordD(entity, true);
    }
    
    public void evaluateOnlyStanfordLC(String entity){
        AnalyzeClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeClassifier.MODELFILE=WKSUPMODEL.replace("%S", entity);
        AnalyzeClassifier lcclass = new AnalyzeClassifier();
        lcclass.testingClassifier(false, entity, false, false);        
    }
    
    public void trainingOnlyWeaklySup(String entity){
        AnalyzeClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeClassifier.MODELFILE=WKSUPMODEL.replace("%S", entity);
        AnalyzeClassifier lcclass = new AnalyzeClassifier();
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
        crfclass.trainAllCRFClassifier(true, false, false);
        crfclass.testingClassifier(entity, false, false, "/analysis/conll03/");
    }
    
    
    public static void main(String[] args){
        CoNLL03Ner conll = new CoNLL03Ner();
        conll.runningWeaklySupStanfordLC(CNConstants.PRNOUN,true,20);
        //conll.evaluateOnlyStanfordLC();
        //conll.trainingOnlyWeaklySup(CNConstants.PRNOUN);
    }
}
