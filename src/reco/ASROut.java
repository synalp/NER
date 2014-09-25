/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package reco;

import CRFClassifier.AnalyzeCRFClassifier;
import edu.stanford.nlp.classify.LinearClassifier;
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
import lex.Segment;
import lex.Utterance;
import lex.Word;


import linearclassifier.AnalyzeClassifier;
import static linearclassifier.AnalyzeClassifier.isStopWord;
import resources.WikipediaAPI;
import tools.CNConstants;
import utils.SuiteDeMots;


/**
 *
 * @author synalp
 */
public class ASROut {
    
    private AnalyzeClassifier lclass= new AnalyzeClassifier();
    private AnalyzeCRFClassifier crfclass=new AnalyzeCRFClassifier();
    private String developmentDir = "dev";
    private String testDir="test";
    public static String  DEVFILE="groups.%S.tab.lc.reco.dev";
    public static String  TESTFILE="groups.%S.tab.lc..reco.test";    
    public static String  LISTDEVFILES="esterRecoDev.xmll";
    public static String  LISTTESTFILES="esterRecoTest.xmll";
    
    public ASROut(){

       
    }
    

    public void processingASROutputToLC(String en, boolean istrain, boolean iswiki){
        
        
        GraphIO gio = new GraphIO(null);
        try{
                OutputStreamWriter outFile =null;
                String xmllist=LISTDEVFILES;
                if(istrain)
                    outFile = new OutputStreamWriter(new FileOutputStream(DEVFILE.replace("%S", en)),CNConstants.UTF8_ENCODING);
                else{
                    xmllist=LISTTESTFILES;
                    outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", en)),CNConstants.UTF8_ENCODING);
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

                                        if(en.equals(AnalyzeClassifier.ONLYONEPNOUNCLASS)){
                                            //all the groups are proper nouns pn
                                            for(String str:AnalyzeClassifier.groupsOfNE){
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
                                                if (en.equals(AnalyzeClassifier.ONLYONEMULTICLASS)) {
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
      public void processingASROutputToCRF(String en, boolean istrain, boolean iswiki){
        
        
        GraphIO gio = new GraphIO(null);
        try{
                OutputStreamWriter outFile =null;
                String xmllist=LISTDEVFILES;
                if(istrain)
                    outFile = new OutputStreamWriter(new FileOutputStream(DEVFILE.replace("%S", en)),CNConstants.UTF8_ENCODING);
                else{
                    xmllist=LISTTESTFILES;
                    outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", en)),CNConstants.UTF8_ENCODING);
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

                                        if(en.equals(AnalyzeClassifier.ONLYONEPNOUNCLASS)){
                                            //all the groups are proper nouns pn
                                            for(String str:AnalyzeClassifier.groupsOfNE){
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
                                                if (en.equals(AnalyzeClassifier.ONLYONEMULTICLASS)) {
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

        AnalyzeClassifier.MODELFILE="bin.%S.lc.mods.reco";
        
        processingASROutputToLC(CNConstants.PRNOUN, true, iswiki);
        processingASROutputToLC(CNConstants.PRNOUN, false, iswiki);
        AnalyzeClassifier.LISTTRAINFILES="esterTrainALL.xmll";
        AnalyzeClassifier.TRAINSIZE   =Integer.MAX_VALUE; 
        lclass.trainAllLinearClassifier(sclass,true,iswiki,true);
        System.out.println("===============trained ==================");
        //testing on dev
        AnalyzeClassifier.TESTFILE=DEVFILE;        
        lclass.testingClassifier(false,sclass,iswiki,true);
        LinearClassifier model = lclass.getModel(sclass);
        double f1=lclass.testingClassifier(model,TESTFILE.replace("%S", sclass));        
        //testing on test
        AnalyzeClassifier.TESTFILE=TESTFILE;        
        lclass.testingClassifier(false,sclass,iswiki,true);
        f1=lclass.testingClassifier(model,TESTFILE.replace("%S", sclass));
            
    }
     public void callStanfordNER(String sclass){
        AnalyzeCRFClassifier.MODELFILE="en.%S.crf.mods.reco";
        ///*
        processingASROutputToCRF(CNConstants.PRNOUN, true, false);
        processingASROutputToCRF(CNConstants.PRNOUN, false, false);
        //*/
        crfclass.trainAllCRFClassifier(true,true,true);
        
        AnalyzeCRFClassifier.TESTFILE=DEVFILE;  
        crfclass.testingClassifier(true,false,sclass,false);
        
        AnalyzeCRFClassifier.TESTFILE=TESTFILE;  
        crfclass.testingClassifier(true,false,sclass,false);        
            
    }
    public void evaluatingResults(String fileName){
        lclass.evaluationCLASSRESULTS(fileName);
        
    }
    public static void  main(String[] args){
        ASROut asrout= new ASROut();
        //asrout.processingASROutput(CNConstants.PRNOUN, true);
        //asrout.processingASROutput(CNConstants.PRNOUN, false);
        //asrout.callLClassifier(CNConstants.PRNOUN, false);
        //asrout.callStanfordNER(CNConstants.PRNOUN);
        ///*
        asrout.evaluatingResults("RECO/CRFResultsRecoDev_gazlower.txt");
        asrout.evaluatingResults("RECO/CRFResultsRecoTest_gazlower.txt");
        //asrout.evaluatingResults("RECO/LCResultsRecoDev.txt");
        //asrout.evaluatingResults("RECO/LCResultsRecoTest.txt");
        
        //*/
    }
    
}    
