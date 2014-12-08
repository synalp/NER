/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package reco;

import CRFClassifier.AnalyzeCRFClassifier;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import tagger.Tagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import jsafran.DetGraph;
import jsafran.GraphIO;
import lex.LexicalUnit;
import lex.Segment;
import lex.Utterance;
import lex.Word;


import linearclassifier.AnalyzeLClassifier;
import static linearclassifier.AnalyzeLClassifier.isStopWord;
import resources.WikipediaAPI;
import tools.CNConstants;
import tools.GeneralConfig;
import utils.SuiteDeMots;


/**
 *
 * @author synalp
 */
public class ASROut {
    
    private AnalyzeLClassifier lclass= new AnalyzeLClassifier();
    private AnalyzeCRFClassifier crfclass=new AnalyzeCRFClassifier();
    private String developmentDir = "dev";
    private String testDir="test";
    public static String  DEVFILE="groups.%S.tab.lc.reco.dev";
    public static String  TESTFILE="groups.%S.tab.lc.reco.test";    
    public static String  LISTDEVFILES="esterRecoDev.xmll";
    public static String  LISTTESTFILES="esterRecoTest.xmll";
    
    public ASROut(){
        GeneralConfig.loadProperties();
        LISTDEVFILES=GeneralConfig.listASRDev;
        LISTTESTFILES=GeneralConfig.listASRTest;
        Capitalization.PROPERTIES_FILE=GeneralConfig.capProps;
    }
    

    public void processingASROutputToLC(String entity, boolean istrain, boolean iswiki){
        
        
        GraphIO gio = new GraphIO(null);
        try{
                OutputStreamWriter outFile =null;
                String xmllist=LISTDEVFILES;
                if(istrain)
                    outFile = new OutputStreamWriter(new FileOutputStream(DEVFILE.replace("%S", entity)),CNConstants.UTF8_ENCODING);
                else{
                    xmllist=LISTTESTFILES;
                    outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", entity)),CNConstants.UTF8_ENCODING);
                }            
            BufferedReader inFile = new BufferedReader(new FileReader(xmllist));
            
            for (;;) {
                String line = inFile.readLine();
                if (line==null) 
                    break;
                int idx=line.lastIndexOf("/");
                if(idx==-1)
                    break;
                String filename= line.substring(idx);
                filename=filename.replace(CNConstants.RECOEXT, ".xml");
                String  orfilePath=developmentDir+filename;
                File file = new File(orfilePath);
                if(!file.exists())
                    orfilePath=testDir+filename;
                
                List<DetGraph> gs = gio.loadAllGraphs(orfilePath);
                List<String> allwords= new ArrayList<>();
                List<String> goldlabels= new ArrayList<>();
                
                for (int i=0;i<gs.size();i++) {
                        DetGraph group = gs.get(i);  
                                 
                        
                        for (int j=0;j<group.getNbMots();j++) {
                                // calcul du label
                                String lab = CNConstants.NOCLASS;
                                int[] groups = group.getGroups(j);
                                if (groups!=null)
                                    for (int gr : groups) {

                                        if(entity.equals(AnalyzeLClassifier.ONLYONEPNOUNCLASS)){
                                            //all the groups are proper nouns pn
                                            for(String str:AnalyzeLClassifier.groupsOfNE){
                                                if (group.groupnoms.get(gr).startsWith(str)) {
                                                    lab=entity;
                                                    break;
                                                }
                                            }
                                        }else{
                                             if (group.groupnoms.get(gr).startsWith(entity)) {
                                                //int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                //if (debdugroupe==j) lab = entity+"B";    
                                                //else lab = entity+"I";
                                                lab=entity;
                                                break;
                                            }else{
                                                if (entity.equals(AnalyzeLClassifier.ONLYONEMULTICLASS)) {
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

                                    /*    
                                    if(!isStopWord(group.getMot(j).getPOS()))
                                            outFile.append(lab+"\t"+group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\n");
                                    */

                                    allwords.add(group.getMot(j).getForme());
                                    goldlabels.add(lab);
                                    
                        }

                           
                            //utt.getSegment().computingWordFrequencies();                        


                }
                
                List<String> allRecoWords=new ArrayList<>();
                BufferedReader recoFile = new BufferedReader(new FileReader(line));
                Utterance recoBIGUtterance= new Utterance(); 
                List<Word> recWords= new ArrayList<>();
                int lineNumber=0;
                for(;;){
                    String recoLine = recoFile.readLine();
                    if (recoLine==null) 
                        break;
                    allRecoWords.add(recoLine);
                    Word word = new Word(lineNumber,recoLine);
                    recWords.add(word);
                    lineNumber++;
                }
                recoBIGUtterance.setWords(recWords);
                
                //alignment
                String[] orWords= new String[allwords.size()];
                allwords.toArray(orWords);
                SuiteDeMots orMots= new SuiteDeMots(orWords);
                String[] reWords= new String[allRecoWords.size()];
                allRecoWords.toArray(reWords);
                SuiteDeMots reMots = new SuiteDeMots(reWords);
                reMots.align(orMots);
                HashMap<Integer,Integer> wordsMap=new HashMap<>();
                for(int i=0; i< reMots.getNmots(); i++){
                    int[] linkedWords= reMots.getLinkedWords(i);
                    if(linkedWords.length==0)
                        wordsMap.put(i, CNConstants.INT_NULL);
                    for(int j=0; j< linkedWords.length;j++){
                        wordsMap.put(i, linkedWords[j]);
                    }
                        
                }
                /*
                for(Integer key:wordsMap.keySet()){
                    int val = wordsMap.get(key);
                    String valst = (val!=-1)?allwords.get(val):"EMPTY";
                    System.out.println(allRecoWords.get(key)+" aligned to "+valst);
                }
                //*/
                //tag all the reco words
                Tagger tagger = new Tagger();
                tagger.computePOStags(recoBIGUtterance.getSegment());
                tagger.destroy();
                Tagger.clean();    
                System.out.println("size of reco words"+allRecoWords.size());
                System.out.println("size of words in big utterance"+recoBIGUtterance.getSegment().getWords().size());
                
                for(int i=0; i<allRecoWords.size();i++){
                    int goldIndx= wordsMap.get(i);
                    String wordStr=allRecoWords.get(i);
                    //What should I do if it does not find the word in the gold ???
                    String label=CNConstants.NOCLASS;
                    String pos=recoBIGUtterance.getWords().get(i).getPosTag().getName();
                    if(goldIndx!=CNConstants.INT_NULL)
                        label=goldlabels.get(goldIndx);
                        if(iswiki){
                            if(!isStopWord(pos)){
                                String inWiki ="F";
                                if(!pos.startsWith("PRO") && !pos.startsWith("ADJ")&&
                                        !pos.startsWith("VER") && !pos.startsWith("ADV"))
                                    inWiki =(WikipediaAPI.processPage(wordStr).equals(CNConstants.CHAR_NULL))?"F":"T";
                                outFile.append(label+"\t"+wordStr+"\t"+pos+"\t"+ inWiki +"\n");
                               
                            } 
                        }else if(!isStopWord(pos))
                            outFile.append(label+"\t"+wordStr+"\t"+pos+"\n");
                    
                }
                outFile.flush();
            }
            outFile.flush();
            outFile.close();    
            inFile.close();
               
            } catch (Exception e) {
                    e.printStackTrace();
            }
    }   
      public void processingASROutputToCRF(String entity, boolean istrain, boolean iswiki){
        
        
        GraphIO gio = new GraphIO(null);
        try{
                OutputStreamWriter outFile =null;
                String xmllist=LISTDEVFILES;
                if(istrain)
                    outFile = new OutputStreamWriter(new FileOutputStream(DEVFILE.replace("%S", entity)),CNConstants.UTF8_ENCODING);
                else{
                    xmllist=LISTTESTFILES;
                    outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", entity)),CNConstants.UTF8_ENCODING);
                }            
            BufferedReader inFile = new BufferedReader(new FileReader(xmllist));
            
            for (;;) {
                String line = inFile.readLine();
                if (line==null) 
                    break;
                int idx=line.lastIndexOf("/");
                if(idx==-1)
                    break;
                String filename= line.substring(idx);
                filename=filename.replace(CNConstants.RECOEXT, ".xml");
                String  orfilePath=developmentDir+filename;
                File file = new File(orfilePath);
                if(!file.exists())
                    orfilePath=testDir+filename;
                
                List<DetGraph> gs = gio.loadAllGraphs(orfilePath);
                List<String> allwords= new ArrayList<>();
                List<String> goldlabels= new ArrayList<>();
                
                for (int i=0;i<gs.size();i++) {
                        DetGraph group = gs.get(i);  
                                 
                        
                        for (int j=0;j<group.getNbMots();j++) {
                                // calcul du label
                                String lab = CNConstants.NOCLASS;
                                int[] groups = group.getGroups(j);
                                if (groups!=null)
                                    for (int gr : groups) {

                                        if(entity.equals(AnalyzeLClassifier.ONLYONEPNOUNCLASS)){
                                            //all the groups are proper nouns pn
                                            for(String str:AnalyzeLClassifier.groupsOfNE){
                                                if (group.groupnoms.get(gr).startsWith(str)) {
                                                    lab=entity;
                                                    break;
                                                }
                                            }
                                        }else{
                                             if (group.groupnoms.get(gr).startsWith(entity)) {
                                                //int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                //if (debdugroupe==j) lab = entity+"B";    
                                                //else lab = entity+"I";
                                                lab=entity;
                                                break;
                                            }else{
                                                if (entity.equals(AnalyzeLClassifier.ONLYONEMULTICLASS)) {
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

                                    /*    
                                    if(!isStopWord(group.getMot(j).getPOS()))
                                            outFile.append(lab+"\t"+group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\n");
                                    */

                                    allwords.add(group.getMot(j).getForme());
                                    goldlabels.add(lab);
                                    
                        }

                           
                            //utt.getSegment().computingWordFrequencies();                        


                }
                List<String> allRecoWords=new ArrayList<>();
                BufferedReader recoFile = new BufferedReader(new FileReader(line));
                Utterance recoBIGUtterance= new Utterance(); 
                List<Word> recWords= new ArrayList<>();
                int lineNumber=0;
                for(;;){
                    String recoLine = recoFile.readLine();
                    if (recoLine==null) 
                        break;
                    allRecoWords.add(recoLine);
                    Word word = new Word(lineNumber,recoLine);
                    recWords.add(word);
                    lineNumber++;
                }
                recoBIGUtterance.setWords(recWords);
                
                //capitalization
                Capitalization cap = new Capitalization();
                CRFClassifier capClass=cap.getClassifier();
                //String fileContents = IOUtils.slurpFile(args[1]);
                List<List<CoreLabel>> out = capClass.classify(recoBIGUtterance.toString());
                //System.out.println(recoBIGUtterance.toString());
                for (List<CoreLabel> sentence : out) {
                  int wordCount=0;
                  for (int i=0;i<sentence.size();i++) {
                      String wordString=sentence.get(i).word();
                      if(wordCount>=recoBIGUtterance.getWords().size()){
                          System.out.println("FOUND word: "+wordString+ recoBIGUtterance.toString().length());
                      }  

                      if(wordString.equals("'"))
                          wordString=sentence.get(i-1)+wordString;

                      
                      if(wordString.startsWith("aujourd")&&
                         !wordString.equals("aujourd'hui")&&sentence.get(i+2).word().equals("hui") ){
                              wordString=wordString+"'"+sentence.get(i+2);
                              i+=2;
                      } 
                      if(wordString.startsWith("star")&&
                         !wordString.equals("star'ac")&&sentence.get(i+2).word().equals("ac") ){
                              wordString=wordString+"'"+sentence.get(i+2);
                              i+=2;
                      }                       
                      if( wordString.equals("jusqu")){
                          wordString=wordString+"'";
                          i++;
                      }                        
                      System.out.println(wordString+"\t"+recoBIGUtterance.getWords().get(wordCount).getContent());
                      if(recoBIGUtterance.getWords().get(wordCount).getContent().startsWith("<")|| 
                         recoBIGUtterance.getWords().get(wordCount).getContent().startsWith("(%")){
                          wordCount++;
                          
                      }    
                      if(wordString.equals(recoBIGUtterance.getWords().get(wordCount).getContent())){
                          if(sentence.get(i).get(CoreAnnotations.AnswerAnnotation.class).equals(Capitalization.CAPITALIZATION)){
                            Word recWord= recoBIGUtterance.getWords().get(wordCount);
                            LexicalUnit lu=recWord.getLexicalUnit();
                            String wordStr=lu.getName();
                            char[] chars = new char[wordStr.length()];
                            wordStr.getChars(0, wordStr.length(), chars, 0); 
                            wordStr=wordStr.replace(chars[0], Character.toUpperCase(chars[0]));
                            lu.setName(wordStr);
                            
                          }
                          wordCount++;    
                      }
                      //word.get(CoreAnnotations.AnswerAnnotation.class));
                      
                    //System.out.print(word.word() + '/' + word.get(CoreAnnotations.AnswerAnnotation.class) + ' ');
                  }
                  //System.out.println();
                }
              
                
                //alignment
                String[] orWords= new String[allwords.size()];
                allwords.toArray(orWords);
                SuiteDeMots orMots= new SuiteDeMots(orWords);
                String[] reWords= new String[allRecoWords.size()];
                allRecoWords.toArray(reWords);
                SuiteDeMots reMots = new SuiteDeMots(reWords);
                reMots.align(orMots);
                HashMap<Integer,Integer> wordsMap=new HashMap<>();
                for(int i=0; i< reMots.getNmots(); i++){
                    int[] linkedWords= reMots.getLinkedWords(i);
                    if(linkedWords.length==0)
                        wordsMap.put(i, CNConstants.INT_NULL);
                    for(int j=0; j< linkedWords.length;j++){
                        wordsMap.put(i, linkedWords[j]);
                    }
                        
                }
                /*
                // Printing the alignment
                for(Integer key:wordsMap.keySet()){
                    int val = wordsMap.get(key);
                    String valst = (val!=-1)?allwords.get(val):"EMPTY";
                    System.out.println(allRecoWords.get(key)+" aligned to "+valst);
                }
                //*/
                //tag all the reco words
                Tagger tagger = new Tagger();
                tagger.computePOStags(recoBIGUtterance.getSegment());
                tagger.destroy();
                Tagger.clean();    
                System.out.println("size of reco words"+allRecoWords.size());
                System.out.println("size of words in big utterance"+recoBIGUtterance.getSegment().getWords().size());
                
                for(int i=0; i<allRecoWords.size();i++){
                    int goldIndx= wordsMap.get(i);
                    String wordStr=recoBIGUtterance.getWords().get(i).getContent();
                    //What should I do if it does not find the word in the gold ???
                    String label=CNConstants.NOCLASS;
                    String pos=recoBIGUtterance.getWords().get(i).getPosTag().getName();
                    if(goldIndx!=CNConstants.INT_NULL)
                        label=goldlabels.get(goldIndx);
                        if(iswiki){
                            if(!isStopWord(pos)){
                                String inWiki ="F";
                                if(!pos.startsWith("PRO") && !pos.startsWith("ADJ")&&
                                        !pos.startsWith("VER") && !pos.startsWith("ADV"))
                                    inWiki =(WikipediaAPI.processPage(wordStr).equals(CNConstants.CHAR_NULL))?"F":"T";
                                outFile.append(wordStr+"\t"+pos+"\t"+ inWiki +"\tNOCL\t"+label+"\n");
                               
                            } 
                        }else if(!isStopWord(pos))
                            outFile.append(wordStr+"\t"+pos+"\tNOCL\t"+label+"\n");
                    
                }
                outFile.flush();
            }
            outFile.flush();
            outFile.close();    
            inFile.close();
               
            } catch (Exception e) {
                    e.printStackTrace();
            }
    }     
    public void callLClassifier(String sclass, boolean iswiki){

        AnalyzeLClassifier.MODELFILE="bin.%S.lc.mods.reco";
        
        processingASROutputToLC(sclass, true, iswiki);
        processingASROutputToLC(sclass, false, iswiki);
        AnalyzeLClassifier.LISTTRAINFILES="esterTrainALL.xmll";
        AnalyzeLClassifier.TRAINSIZE   =Integer.MAX_VALUE; 
        lclass.trainAllLinearClassifier(sclass,true,iswiki,true);
        System.out.println("===============trained ==================");
        //testing on dev
        AnalyzeLClassifier.TESTFILE=DEVFILE;        
        lclass.testingClassifier(false,sclass,iswiki,true);
        LinearClassifier model = lclass.getModel(sclass);
        double f1=lclass.testingClassifier(model,TESTFILE.replace("%S", sclass));        
        //testing on test
        AnalyzeLClassifier.TESTFILE=TESTFILE;        
        lclass.testingClassifier(false,sclass,iswiki,true);
        f1=lclass.testingClassifier(model,TESTFILE.replace("%S", sclass));
            
    }
     public void callStanfordNER(String sclass){
        AnalyzeCRFClassifier.MODELFILE="en.%S.crf.mods.reco";
        /*
        processingASROutputToCRF(sclass, true, false);
        processingASROutputToCRF(sclass, false, false);
        //*/
        crfclass.trainAllCRFClassifier(CNConstants.PRNOUN,true,false,false,false);
        
        AnalyzeCRFClassifier.TESTFILE=DEVFILE;
        if(sclass.equals(CNConstants.PRNOUN)){
            crfclass.testingClassifier(CNConstants.PRNOUN,false,false,false,false);
            AnalyzeCRFClassifier.TESTFILE=TESTFILE;  
            crfclass.testingClassifier(CNConstants.PRNOUN,false,false, false,false);             
        }else{
            crfclass.testingClassifier(CNConstants.PRNOUN,false,false, false, false);
            AnalyzeCRFClassifier.TESTFILE=TESTFILE;  
            crfclass.testingClassifier(CNConstants.PRNOUN,false,false, false, false);             
        }    
       
            
    }
    public void evaluatingResults(String fileName){
        AnalyzeLClassifier.evaluationCLASSRESULTS(CNConstants.PRNOUN,fileName);
        
    }
    public static void  main(String[] args){
        ASROut asrout= new ASROut();
        //asrout.processingASROutputToCRF(CNConstants.PRNOUN, true,false);
        //asrout.processingASROutputToCRF(CNConstants.PRNOUN, false,false);
        //asrout.callLClassifier(CNConstants.PRNOUN, false);
        //asrout.callStanfordNER(CNConstants.PRNOUN);
        ///*
        //asrout.evaluatingResults("analysis/RECO/CRFResultsRecoDev_gazlower.txt");
        //asrout.evaluatingResults("analysis/RECO/CRFResultsRecoTest_gazlower.txt");
        //asrout.evaluatingResults("analysis/RECO/LCResultsRecoDev.txt");
        //asrout.evaluatingResults("analysis/RECO/LCResultsRecoTest.txt");
        //*/
        asrout.evaluatingResults("analysis/Reco/AfterCapcrfDev.txt");
        asrout.evaluatingResults("analysis/Reco/AfterCapcrfTest.txt");
        //asrout.evaluatingResults("analysis/Reco/AfterCapcrfDevNOGAZ.txt");
        //asrout.evaluatingResults("analysis/Reco/AfterCapcrfTestNOGAZ.txt");
        
    }
    
}    
