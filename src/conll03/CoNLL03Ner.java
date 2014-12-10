package conll03;

import CRFClassifier.AnalyzeCRFClassifier;
import static CRFClassifier.AnalyzeCRFClassifier.TKPREDTEST;
import static CRFClassifier.AnalyzeCRFClassifier.TKPREDTRAIN;
import edu.emory.mathcs.backport.java.util.Arrays;
import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.Datum;
import gigaword.Conll03Preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.MateParser;
import jsafran.POStagger;
import linearclassifier.AnalyzeLClassifier;
import org.apache.commons.io.FileUtils;
import tools.CNConstants;
import tools.GeneralConfig;
import tools.PlotAPI;
import utils.ErrorsReporting;
import xtof.UnlabCorpus;

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
    * @param usetkFeat  uses tree kernel results? 
    */ 
   public String generatingStanfordInputFiles(String entity, String dataset, boolean isCRF, String wSupModelFile, boolean usetkFeat){
       if(dataset.equals("gigaw"))
          return generatingStanfordInputFiles(entity,dataset,isCRF,(isCRF)?0:AnalyzeLClassifier.TESTSIZE,wSupModelFile, usetkFeat); 
       else    
    	   return generatingStanfordInputFiles(entity,dataset,isCRF,(isCRF|| (!dataset.equals("train")&&!dataset.equals("tropennlp")))?0:AnalyzeLClassifier.TRAINSIZE,wSupModelFile, usetkFeat);
   }
   // I need a bit more flexibility and control over the size of the datasets that are produced
   // so I'ved added this method but still keeping the default behavior the same with the previous method
   public String generatingStanfordInputFiles(String entity, String dataset, boolean isCRF, int limitsize, String wSupModelFile, boolean usetkFeat){
       LinearClassifier wsupModel = null;
       if(!wSupModelFile.equals(CNConstants.CHAR_NULL))
           wsupModel = AnalyzeLClassifier.loadModelFromFile(wSupModelFile);
       
        BufferedReader inFile = null;
        OutputStreamWriter outFile =null;
        HashMap<String,String> wordclasses = new HashMap<>();
        List<String> lines = new ArrayList<>();
        List<String> wordInLine= new ArrayList<>();
        String outfilename=null;
        
       try {
                HashMap<Integer,String> predictedClass = new HashMap<>();
                BufferedReader predictionFile = null;
                if(usetkFeat){
                    TKPREDTRAIN="scripts/ner.conll.modeltk.sst.train";
                    TKPREDTEST="scripts/ner.conll.modeltk.sst.test";
                    if(entity.equals("train"))
                        predictionFile=new BufferedReader(new FileReader(TKPREDTRAIN));
                    else
                        predictionFile=new BufferedReader(new FileReader(TKPREDTEST));
                    int testLine=0;
                    while(true){
                       String line = predictionFile.readLine();
                       if(line  == null )
                           break;
                       line=line.trim();
                       double value = Double.parseDouble(line);
                       String classVal = CNConstants.NOCLASS;
                       if(value > 0)
                           classVal = CNConstants.PRNOUN;
                       
                       predictedClass.put(testLine, classVal );
                       testLine++;
                    }
                }  
     
        switch(dataset){
                case "train":
                    inFile = new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusTrain)); 
                    
                    if(isCRF)
                        outfilename = TRAINFILE.replace("%S", entity).replace("%CLASS", "CRF");
                    else
                        outfilename = TRAINFILE.replace("%S", entity).replace("%CLASS", "LC");
                    break;
                case "dev":
                    inFile = new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusDev)); 
                    
                    if(isCRF)
                        outfilename = DEVFILE.replace("%S", entity).replace("%CLASS", "CRF");
                    else
                        outfilename = DEVFILE.replace("%S", entity).replace("%CLASS", "LC");
                    break;
                case "test":
                    inFile = new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusTest)); 
                    if(isCRF)
                        outfilename = TESTFILE.replace("%S", entity).replace("%CLASS", "CRF");                      
                    else
                        outfilename = TESTFILE.replace("%S", entity).replace("%CLASS", "LC");
                    break;
                case "gigaw":
                    String gwDir=GeneralConfig.corpusGigaDir;
                    String gwData = GeneralConfig.corpusGigaTrain;
                    if(gwDir == null || gwData == null ){
                        ErrorsReporting.report("The GigaWord configuration must be included in the properties file: ner.properties");
                    }
                    inFile = new BufferedReader(new FileReader(gwDir+System.getProperty("file.separator")+gwData)); 
                    TESTFILE=TESTFILE.replace("conll", "gw").replace("%S", entity).replace("%CLASS", "LC");
                    outfilename = TESTFILE;
                    break;  
                case "tropennlp":
                    String newtagData = GeneralConfig.corpusTrainOpenNLP;
                    if(newtagData == null ){
                        ErrorsReporting.report("The conll03 corpus with the OpenNLP tags must be included in the properties file: ner.properties");
                    }                    
                     inFile = new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+newtagData)); 
                    
                    if(isCRF)
                        outfilename = TRAINFILE.replace("%S", entity).replace("%CLASS", "CRF");
                    else
                        outfilename = TRAINFILE.replace("%S", entity).replace("%CLASS", "LC");
                    break;                   
            }
            outFile = new OutputStreamWriter(new FileOutputStream(outfilename),CNConstants.UTF8_ENCODING);

            if(!isCRF){
                BufferedReader distSemFile = new BufferedReader(new FileReader("scripts/egw.bnc.200.pruned"));
                while(true){
                    String line=distSemFile.readLine();
                    if(line==null)
                        break;
                    String[] cols=line.split("\\s");
                    wordclasses.put(cols[0], cols[1]);
                }
                distSemFile.close();
            }
                
            int uttCount=CNConstants.INT_NULL;
            int wordCount=0;
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
               }
               /*
               else if(entity.equals(CNConstants.ALL)){
                   String prefix=label.substring(0,label.indexOf("-")+1);
                   if(!prefix.isEmpty())
                       label = label.replace(prefix, "");
               }
               */
               if(label.equals(CNConstants.OUTCLASS))
                   label=CNConstants.OUTCLASS;
               if(isCRF){
                   if(!wSupModelFile.equals(CNConstants.CHAR_NULL)){
                       String wordClass="";
                       // why do you add a pipe before "DISTSIM" ?
                       // doesn't it create 2 features: one with the word Class, and another constant "DISTSIM" ?
                       // don't you want to rather put an underscore instead of a pipe ? 
                       if(wordclasses.containsKey(cols[0]))
                         wordClass=wordclasses.get(cols[0])+"|DISTSIM";
                       else
                         wordClass= CNConstants.CHAR_NULL+"|DISTSIM";  
                       
                       lines.add(label+"\t"+cols[0]+"\t"+cols[1]+"\t"+chunk+"\t"+wordClass);
                       wordInLine.add(cols[0]);
                        //AnalyzeLClassifier.MODELFILE=wSupModelFile;
                        //LinearClassifier wsupModel = AnalyzeLClassifier.loadModelFromFile(wSupModelFile);
                        //ColumnDataClassifier columnDataClass = new ColumnDataClassifier(AnalyzeLClassifier.PROPERTIES_FILE);
                        //Datum<String, String> datum = columnDataClass.makeDatumFromLine(label+"\t"+cols[0]+"\t"+cols[1]+"\t"+cols[2]+"\n", 0);
                        //String outClass = (String) wsupModel.classOf(datum);
                        //outFile.append(cols[0]+"\t"+cols[1]+"\t"+chunk+"\t"+outClass+"\t"+label+"\n");
                        
                   }else{
                       if(usetkFeat)
                           outFile.append(cols[0]+"\t"+cols[1]+"\t"+chunk+"\t"+predictedClass.get(wordCount)+"\t"+label+"\n");
                       else
                            outFile.append(cols[0]+"\t"+cols[1]+"\t"+chunk+"\t"+label+"\n");
                   }    
                        
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
               wordCount++;
           }
            
           if(!isCRF || (isCRF && !wSupModelFile.equals(CNConstants.CHAR_NULL) ) ){
               
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
                   
                  if(!isCRF) 
                    outFile.append(lines.get(i)+"\t"+context+"\n"); 
                  else{
                	  if (wSupModelFile.equals(CNConstants.AUTOTESTORACLE)) {
                		  // special case, just for testing: put the oracle class in the additional column
                            String line =lines.get(i);
                            String label = line.substring(0,line.indexOf("\t"));
                            String outClass = label;
                            String newLine = line.substring(line.indexOf("\t")+1,line.lastIndexOf("\t")) +"\t"+outClass+"\t"+label+"\n";
                            outFile.append(newLine);
                	  } else if(wSupModelFile.equals(CNConstants.AUTOTESTPNORACLE)){
                             String line =lines.get(i);
                            String label = line.substring(0,line.indexOf("\t"));
                            String outClass = label;
                            String prefix=outClass.substring(0,label.indexOf("-")+1);
                            if(!prefix.isEmpty())
                            	outClass = CNConstants.PRNOUN;                            
                            String newLine = line.substring(line.indexOf("\t")+1,line.lastIndexOf("\t")) +"\t"+outClass+"\t"+label+"\n";
                            outFile.append(newLine);                            
                	  } else if(wSupModelFile.equals(CNConstants.TABLE_IN_UNLABCORPUS)){
                		  String line =lines.get(i);
                          String label = line.substring(0,line.indexOf("\t"));
                		  String weaksupClass = "WS"+UnlabCorpus.LCrec[i];
                		  String newLine = line.substring(line.indexOf("\t")+1,line.lastIndexOf("\t")) +"\t"+weaksupClass+"\t"+label+"\n";
                		  outFile.append(newLine);                            
                	  }else if(!wSupModelFile.equals(CNConstants.CHAR_NULL)){
                		  AnalyzeLClassifier.MODELFILE=wSupModelFile;
                            // you load the model for every line i ??!!
                            
                            ColumnDataClassifier columnDataClass = new ColumnDataClassifier(AnalyzeLClassifier.PROPERTIES_FILE);
                            String line =lines.get(i);
                            Datum<String, String> datum = columnDataClass.makeDatumFromLine(line+"\t"+context+"\n", 0);
                            String outClass = (String) wsupModel.classOf(datum);
                            String label = line.substring(0,line.indexOf("\t"));
                            String newLine ="";
                            if(usetkFeat)
                                newLine = line.substring(line.indexOf("\t")+1,line.lastIndexOf("\t")) +"\t"+ predictedClass.get(i)+"\t"+outClass+"\t"+label+"\n";
                            else
                                newLine = line.substring(line.indexOf("\t")+1,line.lastIndexOf("\t")) +"\t"+outClass+"\t"+label+"\n";
                            outFile.append(newLine);
                       }                      
                  }
               }
           }
           outFile.flush();
           outFile.close();
           inFile.close();            
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return outfilename;
    }
    
    public void testingNewWeightsLC(String entity,boolean savingFiles, int trainSize, int testSize, boolean useExistingModels){
        AnalyzeLClassifier.TRAINSIZE=trainSize;
        if(savingFiles){
            generatingStanfordInputFiles(entity, "train", false,CNConstants.CHAR_NULL,false);
            generatingStanfordInputFiles(entity, "test", false,CNConstants.CHAR_NULL,false);
            generatingStanfordInputFiles(entity, "dev", false,CNConstants.CHAR_NULL,false);
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
        //lcclass.allweightsKeepingOnlyTrain(entity,trainSize, testSize,useExistingModels);
        lcclass.allweightsKeepingOnlyTrain(entity,trainSize, testSize,useExistingModels);
        
        columnDataClass = new ColumnDataClassifier(AnalyzeLClassifier.PROPERTIES_FILE);
        columnDataClass.testClassifier(lcclass.getModel(entity), AnalyzeLClassifier.TESTFILE);  
    }
    
    public void trainLC(String entity,boolean savingFiles, int trainSize, boolean useExistingModels){
        AnalyzeLClassifier.TRAINSIZE=trainSize;
        if(savingFiles){
            generatingStanfordInputFiles(entity, "train", false,CNConstants.CHAR_NULL,false);
            generatingStanfordInputFiles(entity, "test", false,CNConstants.CHAR_NULL,false);
            generatingStanfordInputFiles(entity, "dev", false,CNConstants.CHAR_NULL,false);
        }
        AnalyzeLClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeLClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "LC");
        AnalyzeLClassifier.MODELFILE=WKSUPMODEL.replace("%S", entity);
        //if exist recreates the binary file
        File mfile = new File(AnalyzeLClassifier.MODELFILE);
        File mfile2 = new File(AnalyzeLClassifier.MODELFILE+"_COPY");
        if(mfile.exists()){
            try {
                if(!useExistingModels)
                    mfile.delete();
                else
                    Files.copy(mfile.toPath(), mfile2.toPath(),StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        AnalyzeLClassifier lcclass= new AnalyzeLClassifier();
        
        lcclass.trainAllLinearClassifier(entity, false, false, false);
        ColumnDataClassifier columnDataClass = new ColumnDataClassifier(AnalyzeLClassifier.PROPERTIES_FILE);
        columnDataClass.testClassifier(lcclass.getModel(entity), AnalyzeLClassifier.TESTFILE);  
        
        /*
        List<List<Integer>> featsperInst = new ArrayList<>(); 
        List<Integer> labelperInst = new ArrayList<>(); 
        AnalyzeLClassifier.serializeFeatures=true;
        lcclass.getValues(AnalyzeLClassifier.TRAINFILE,lcclass.getModel(entity),featsperInst,labelperInst);
        lcclass.getValues(AnalyzeLClassifier.TESTFILE,lcclass.getModel(entity),featsperInst,labelperInst);
        */
        
    }
    
    public void runningWeaklySupStanfordLC(String entity,boolean savingFiles, int trainSize, int numIters,boolean useExistingModels){
        if (trainSize>=0) AnalyzeLClassifier.TRAINSIZE=trainSize;
        if(savingFiles){
            generatingStanfordInputFiles(entity, "train", false,CNConstants.CHAR_NULL,false);
            generatingStanfordInputFiles(entity, "test", false,CNConstants.CHAR_NULL,false);
            generatingStanfordInputFiles(entity, "dev", false,CNConstants.CHAR_NULL,false);
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
        lcclass.allweightsKeepingOnlyTrain(entity,trainSize, Integer.MAX_VALUE,useExistingModels);
        
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
        lcclass.wkSupParallelStocCoordD(entity, true,numIters,true,true,true);
	//lcclass.wkSupClassifierConstr(entity, true,2000);
    }
    
    /**
     * Uses GigaWord as testdata (unlabeled data)
     * @param entity
     * @param savingFiles 
     */

    public void runningWeaklySupStanfordLC(String entity,boolean savingFiles, int trainSize, int testSize, int niters, boolean useExistingModels,
            boolean useSerializedFeatInst){
        
        AnalyzeLClassifier.TRAINSIZE=trainSize;
        AnalyzeLClassifier.TESTSIZE=testSize;
        if(savingFiles){
            generatingStanfordInputFiles(entity, "tropennlp", false,CNConstants.CHAR_NULL,false);
            generatingStanfordInputFiles(entity, "gigaw", false,CNConstants.CHAR_NULL,false);
            //generatingStanfordInputFiles(entity, "dev", false,CNConstants.CHAR_NULL);
        }else
            TESTFILE=TESTFILE.replace("conll", "gw").replace("%S", entity).replace("%CLASS", "LC");
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
        lcclass.allweightsKeepingOnlyTrain(entity,trainSize,testSize,useExistingModels);
        
        //ColumnDataClassifier columnDataClass = new ColumnDataClassifier(AnalyzeLClassifier.PROPERTIES_FILE);
        //columnDataClass.testClassifier(lcclass.getModel(entity), AnalyzeLClassifier.TESTFILE);        
        HashMap<String,Double> priorsMap = new HashMap<>();
        
        if(!entity.equals(CNConstants.ALL)){
            priorsMap.put("O", new Double(0.8));
            priorsMap.put(CNConstants.PRNOUN, new Double(0.2));

        }else{
        	// TODO: estimate these priors on train, because they seem a bit too much "hacked" to the task ??
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
        lcclass.wkSupParallelStocCoordD(entity, true,niters,true,false,useSerializedFeatInst);
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
    
    public float tuneOnDev() {
    	float basef1=0;
    	if (false) {
    		// first train the baseline CRF on train and test it on dev
    		String entity=CNConstants.ALL;
    		String wsupModel=CNConstants.CHAR_NULL;
    		AnalyzeCRFClassifier crf = new AnalyzeCRFClassifier();
    		crf.updatingMappingBkGPropFile(entity,"O","word=0,tag=1,ner=2,answer=3");         
    		generatingStanfordInputFiles(entity, "train", true,wsupModel,false);
    		generatingStanfordInputFiles(entity, "dev", true,wsupModel,false);

    		AnalyzeCRFClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "CRF");
    		AnalyzeCRFClassifier.MODELFILE=MODELFILE.replace("%S", entity).replace("%CLASS", "CRF");
    		File mfile = new File(AnalyzeCRFClassifier.MODELFILE);
    		mfile.delete();
    		AnalyzeCRFClassifier crfclass= new AnalyzeCRFClassifier();
    		crfclass.trainAllCRFClassifier(entity, false, false,false,false);

    		AnalyzeCRFClassifier.TESTFILE=DEVFILE.replace("%S", entity).replace("%CLASS", "CRF");
    		crfclass.testingClassifier(entity);
    		AnalyzeCRFClassifier.OUTFILE=AnalyzeCRFClassifier.OUTFILE.replace("%S", entity);
    		evaluatingCRFResults(entity);
    		basef1 = conllEvaluation(AnalyzeCRFClassifier.OUTFILE);
    		// got 93.79 F1 on dev
    	}
    	if (false) {
    		// second estimate the priors for weaksup on train
    		String entity=CNConstants.PRNOUN;
    		String wsupModel=CNConstants.CHAR_NULL;
    		generatingStanfordInputFiles(entity, "train", true,wsupModel,false);
    		String tabfile = TRAINFILE.replace("%S", entity).replace("%CLASS", "CRF");
    		try {
    			BufferedReader f = new BufferedReader(new FileReader(tabfile));
    			HashMap<String, Long> co = new HashMap<>();
    			for (;;) {
    				String s=f.readLine();
    				if (s==null) break;
    				StringTokenizer st = new StringTokenizer(s);
    				String cl=null;
    				while (st.hasMoreTokens()) cl=st.nextToken();
    				Long n=co.get(cl);
    				if (n==null) n=1l; else n++;
    				co.put(cl,n);
    			}
    			f.close();
    			int ntot=0;
    			for (Long n : co.values()) ntot+=n;
    			for (String cl : co.keySet()) {
    				long nn = co.get(cl);
    				double pr = (double)nn/(double)ntot;
    				System.out.println("PRIOR "+cl+" "+nn+" "+pr);
    			}
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		// got
//    	     [java] PRIOR pn 34043 0.16718806017061108
//    	     [java] PRIOR O 169578 0.832811939829389
    	}
    	{
    		// third tune the GMMDiag parameters by running 10 iterations of weaksup on DEV and taking the parms that give the highest F1
    		
    	}
        
        return basef1;
    }
    
    /**
     * 
     * @param entity  classifier "all" for all the entities "pn" for the binary classification : proper noun/ not proper noun
     * @param savingFiles, true if it must generate the input files to StanfordNER
     * @param wSupFeat, true if it uses the weakly supervised model as feature "NOT WORKING YET"
     * @param useExistingModel , true if it uses an existing binary model file
     * @return the F1 of the CRF on the test corpus
     */
    public float trainStanfordCRF(String entity, boolean savingFiles, boolean tkFeat, boolean wSupFeat, boolean useExistingModel){
        String wsupModel=CNConstants.CHAR_NULL;
        AnalyzeCRFClassifier crf = new AnalyzeCRFClassifier();
        if(wSupFeat){
            wsupModel=WKSUPMODEL.replace("%S", CNConstants.PRNOUN);
            if(tkFeat)
                crf.updatingMappingBkGPropFile(entity,"O","word=0,tag=1,chunk=2,feattk=3,feat=4,answer=5 ");     
            else
                crf.updatingMappingBkGPropFile(entity,"O","word=0,tag=1,chunk=2,feat=3,answer=4 ");     

        }
        else{
            if(tkFeat)
               crf.updatingMappingBkGPropFile(entity,"O","word=0,tag=1,chunk=2,feattk=3,answer=4");
            else
                crf.updatingMappingBkGPropFile(entity,"O","word=0,tag=1,chunk=2,answer=3");         
        }    
        if(savingFiles){
            //generate the files
            generatingStanfordInputFiles(entity, "train", true,wsupModel,tkFeat);
            generatingStanfordInputFiles(entity, "test", true,wsupModel,tkFeat);
            generatingStanfordInputFiles(entity, "dev", true,wsupModel,tkFeat);
        }
        AnalyzeCRFClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "CRF");
        AnalyzeCRFClassifier.MODELFILE=MODELFILE.replace("%S", entity).replace("%CLASS", "CRF");
        //if exist recreates the binary file
        if(!useExistingModel){
            File mfile = new File(AnalyzeCRFClassifier.MODELFILE);
            mfile.delete();
        }
        AnalyzeCRFClassifier crfclass= new AnalyzeCRFClassifier();

        crfclass.trainAllCRFClassifier(entity, false, false, false,false);
        AnalyzeCRFClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "CRF");
        crfclass.testingClassifier(entity);
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
        public static void evaluatingCRFResultsFile(String  resultFile){
        AnalyzeCRFClassifier crfclass= new AnalyzeCRFClassifier(); 
        String[] labels = { "B-PER","I-PER","B-LOC","I-LOC","B-PROD","I-PROD","B-ORG","I-ORG"};
        for(Object label:labels){
            
            crfclass.evaluationCONLLBIOCLASSRESULTS((String) label, resultFile);
            
        }
        
    }
  
    public static float conllEvaluation(String results){
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
        crfclass.testingClassifier(entity);
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
        generatingStanfordInputFiles(entity, "test", false,CNConstants.CHAR_NULL,false);
        generatingStanfordInputFiles(entity, "dev", false,CNConstants.CHAR_NULL,false);
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
            generatingStanfordInputFiles(entity, "train", false,CNConstants.CHAR_NULL,false);
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
    public void experimentsCRFPlusWkSup(int trainSize, boolean useExistingModels){
        runningWeaklySupStanfordLC(CNConstants.PRNOUN,true,trainSize,1000,useExistingModels);
        trainStanfordCRF(CNConstants.ALL, true, false,true,false);
    }
    /**
     * This method first train a weakly supervised algorithm  
     * with the train data configured in : corpusTrainOpenNLP
     * It uses as unlabeled data Gigaword as configured in :corpusGigaTrain
     * in the ner.properties file.
     * Then it uses the predictions of the weakly supervised model when training and testing the
     * CRF for NER
     * @param trainSize
     * @param testSize 
     */
    public void experimentsCRFPlusWkSupGWord(int trainSize, int testSize, boolean useExistingWSModel, boolean useSerializedFeats){
        
        runningWeaklySupStanfordLC(CNConstants.PRNOUN,true,trainSize,testSize,1000, useExistingWSModel,useSerializedFeats);
        //runningWeaklySupStanfordLC(CNConstants.PRNOUN,false,trainSize,testSize,10000, useExistingWSModel);
        trainStanfordCRF(CNConstants.ALL, true, false, true,false);
    }  
    
    public void trainCRFPlusWkSupGold(String entity, boolean savingFiles){
        String wksupModel=CNConstants.AUTOTESTPNORACLE;
        AnalyzeCRFClassifier crfclass = new AnalyzeCRFClassifier();
        crfclass.updatingMappingBkGPropFile(entity,"O","word=0,tag=1,chunk=2,feat=3,answer=4 "); 
        if(savingFiles){
            //generate the files
            generatingStanfordInputFiles(entity, "train", true,wksupModel,false);
            generatingStanfordInputFiles(entity, "test", true,wksupModel,false);
            generatingStanfordInputFiles(entity, "dev", true,wksupModel,false);
        }
        AnalyzeCRFClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "CRF");
        AnalyzeCRFClassifier.MODELFILE=MODELFILE.replace("%S", entity).replace("%CLASS", "CRF");
        //if exist recreates the binary file
        File mfile = new File(AnalyzeLClassifier.MODELFILE);
        File mfile2 = new File(AnalyzeLClassifier.MODELFILE+"_COPY_"+CNConstants.AUTOTESTORACLE);
        if(mfile.exists()){
            try {
                Files.copy(mfile.toPath(), mfile2.toPath(),StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        

        crfclass.trainAllCRFClassifier(entity, false, false,false,false);
        AnalyzeCRFClassifier.TESTFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "CRF");
        crfclass.testingClassifier(entity);
        AnalyzeCRFClassifier.OUTFILE=AnalyzeCRFClassifier.OUTFILE.replace("%S", entity);
        evaluatingCRFResults(entity);
        conllEvaluation(AnalyzeCRFClassifier.OUTFILE);        
    }
    
    public void computePriors(String entity,String trainSet){
        AnalyzeLClassifier.MODELFILE=WKSUPMODEL.replace("%S", entity);
        generatingStanfordInputFiles(entity, trainSet, false,CNConstants.CHAR_NULL,false);
        switch(trainSet){
           case "train":
               AnalyzeLClassifier.TRAINFILE=TRAINFILE.replace("%S", entity).replace("%CLASS", "LC");               
               break;
           case "dev":
               AnalyzeLClassifier.TRAINFILE=DEVFILE.replace("%S", entity).replace("%CLASS", "LC");               
               break; 
           case "test":
               AnalyzeLClassifier.TRAINFILE=TESTFILE.replace("%S", entity).replace("%CLASS", "LC");               
               break; 

        }

        AnalyzeLClassifier lcclass = new AnalyzeLClassifier();
        lcclass.trainAllLinearClassifier(entity, false, false, false);
        float[] priors=lcclass.computePriors(entity, lcclass.getModel(entity));
        System.out.println(lcclass.getPriorsMap().toString());
        System.out.println(Arrays.toString(priors));         
    }
    
    public void convertingConll03to06(String dataset){
        try {
            BufferedReader inFile = null;
            OutputStreamWriter outFile = null;
            switch(dataset){
                case "train":
                    inFile = new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusTrain)); 
                    outFile = new OutputStreamWriter(new FileOutputStream(corpusDir+System.getProperty("file.separator")+corpusTrain+".conll"),CNConstants.UTF8_ENCODING);
                    break;                            
                case "dev": 
                    inFile = new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusDev));
                    outFile = new OutputStreamWriter(new FileOutputStream(corpusDir+System.getProperty("file.separator")+corpusDev+".conll"),CNConstants.UTF8_ENCODING);
                    break;
                case "test":    
                    inFile = new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusTest));
                    outFile = new OutputStreamWriter(new FileOutputStream(corpusDir+System.getProperty("file.separator")+corpusTest+".conll"),CNConstants.UTF8_ENCODING);                    
                    break;                            
            }
            int wordCounter=1;
            for(;;){
                
               String line = inFile.readLine();
                if(line == null)
                   break;
               if(line.startsWith("-DOCSTART-"))
                   continue;
               //utterance breaking
               if(line.equals("")){
                   wordCounter=1;
                   outFile.append("\n");
                   continue;
               }    
                   
               String[] cols= line.split("\\s");
               
               outFile.append(wordCounter+"\t"+cols[0]+"\t"+cols[0]+"\t"+cols[1]+"\t"+cols[1]+"\t_\t_\t_\t_\t_\n");
               wordCounter++;
            } 
            outFile.flush();
            outFile.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public void parsing(String dataset){
            try {
                GraphIO gio = new GraphIO(null);
                OutputStreamWriter outFile =null;                
                               
//                OutputStreamWriter bigconllFile = new OutputStreamWriter(new FileOutputStream(new File("parse/all.in.conll"),true), CNConstants.UTF8_ENCODING);
                for (;;) {
                    String s = "";
                    switch(dataset){
                        case "train":
                            s = corpusDir+System.getProperty("file.separator")+corpusTrain+".conll";
                            break;                            
                        case "dev": 
                            s = corpusDir+System.getProperty("file.separator")+corpusDev+".conll";
                            break;
                        case "test":    
                            s = corpusDir+System.getProperty("file.separator")+corpusTest+".conll";
                            break;                            
                    }
                    
                    if (s==null) break;
                    List<DetGraph> graphs = gio.loadAllGraphs(s);
                    String filename=s.trim().replaceAll("[\\s]+"," ");
//                    String inconll="parse/"+filename+".in.conll";
                    String path="parse/en/";
                    
                    File outfile=new File(path+filename+".out.conll");
                    System.out.println("Processing file: "+ filename);
                    if(outfile.exists())
                        continue;
                    
//                    GraphIO.saveConLL09(graphs, null, inconll);
//                    BufferedReader inconllFile= new BufferedReader(new FileReader(inconll));
//                    
//                    for(;;){
//                        String line=inconllFile.readLine();
//                        if (line==null) break;
//                        bigconllFile.append(line);
//                    }
//                    bigconllFile.flush();  
                    POStagger.setEnglishModels();
                    //String model="en.mate.model";
                    String model="mate.mods.WSJ";
                                        
                    MateParser.setMods(model); 
                    try{
                        MateParser.parseAll(graphs);
                        File tmpFile = new File("_mate_out.conll");
                        FileUtils.copyFile(tmpFile, outfile);
                    }catch(Exception ex){
                        System.out.println("ERROR PARSING FILE : "+filename);
                        continue;
                    }    
                }      
            } catch (Exception e) {
                    e.printStackTrace();
            }
                    
     }    
    
    public static final String[] TASKS = {
    	"basecrf", "buildGigaword","weaklySupGW","crfwsfeat","opennlptags",  // 0 ... 4
    	"weaklySupConll", "expGWord", "dev","priors","conv", "lc","crfwsgoal",//5-11
        "evalResults","crftk","crftkwsup"

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
        	conll.trainStanfordCRF(CNConstants.ALL, true, false, false,false);
        	break;

        case 1:
        	// segment, tokenize and tag the Gigaword corpus; this shall be done only once !
        	Conll03Preprocess.tagGigaword(null);
        	break;
        case 2:
                //testset = gigaword
                conll.runningWeaklySupStanfordLC(CNConstants.PRNOUN,true,500,500,1000,true,true);
                break;
        case 3:
                conll.trainStanfordCRF(CNConstants.ALL, false, false,true,false);
                break;            
        case 4:
        	// retag the Conll03 corpus with openNLP: this'll be used to run weakly supervised training of the linear classifier on it
        	Conll03Preprocess.retagConll03();
        	break;
        case 5:
        	// what's the difference between case 5 and 6 ?
                conll.runningWeaklySupStanfordLC(CNConstants.PRNOUN,true,500,1000,false);
                //conll.testingNewWeightsLC(CNConstants.PRNOUN, true, 500);
                break;
        case 6:
               //conll.experimentsCRFPlusWkSupGWord(Integer.MAX_VALUE, Integer.MAX_VALUE,true);
               //conll.experimentsCRFPlusWkSupGWord(Integer.MAX_VALUE, 3000000,false);
               conll.experimentsCRFPlusWkSupGWord(500, 50,false,true);
               break;
        case 7:
        	// TODO: tune parameters on dev
        	float f1=conll.tuneOnDev();
        	System.out.println("F1 on DEV "+f1);
        	break;
        case 8:
               conll.computePriors(CNConstants.ALL,"dev");
               break;
        case 9:
               conll.convertingConll03to06("train");
               conll.convertingConll03to06("test");
               conll.convertingConll03to06("dev");
               break;
            
        case 10:
               conll.trainLC(CNConstants.PRNOUN,true,Integer.MAX_VALUE, false);
               break;
        case 11:
               conll.trainCRFPlusWkSupGold(CNConstants.ALL, true);
               break;
        case 12:
               CoNLL03Ner.evaluatingCRFResultsFile("analysis/CRF/test.all.log");
               break;
        case 13:
                conll.trainStanfordCRF(CNConstants.ALL, true, true,false,false);
                break;
        case 14:
                conll.trainStanfordCRF(CNConstants.ALL, true, true,true,false);
                break;           
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
