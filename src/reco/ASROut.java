/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package reco;

import fr.loria.emospeech.agents.tagger.Tagger;
import fr.loria.emospeech.model.Utterance;
import fr.loria.emospeech.model.Word;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import jsafran.DetGraph;
import jsafran.GraphIO;


import linearclassifier.AnalyzeClassifier;
import static linearclassifier.AnalyzeClassifier.LISTTESTFILES;
import static linearclassifier.AnalyzeClassifier.LISTTRAINFILES;
import static linearclassifier.AnalyzeClassifier.ONLYONEMULTICLASS;
import static linearclassifier.AnalyzeClassifier.ONLYONEPNOUNCLASS;
import static linearclassifier.AnalyzeClassifier.TESTFILE;
import static linearclassifier.AnalyzeClassifier.TRAINFILE;
import static linearclassifier.AnalyzeClassifier.TRAINSIZE;
import static linearclassifier.AnalyzeClassifier.UTF8_ENCODING;
import static linearclassifier.AnalyzeClassifier.groupsOfNE;
import static linearclassifier.AnalyzeClassifier.isStopWord;
import resources.WikipediaAPI;
import tools.CNConstants;
import utils.ErrorsReporting;
import utils.JDiff;
import utils.SuiteDeMots;


/**
 *
 * @author synalp
 */
public class ASROut {
    
    private AnalyzeClassifier lclass= new AnalyzeClassifier();
    private String developmentDir = "dev";
    private String testDir="test";
    
    
    public ASROut(){
       AnalyzeClassifier.LISTTRAINFILES="esterRecoTrain.xmll";
       AnalyzeClassifier.LISTTESTFILES="esterRecoTest.xmll";
    }
    

    public void processingASROutput(String en, boolean istrain){
        
        
        GraphIO gio = new GraphIO(null);
        try{
                OutputStreamWriter outFile =null;
                String xmllist=AnalyzeClassifier.LISTTRAINFILES;
                if(istrain)
                    outFile = new OutputStreamWriter(new FileOutputStream(TRAINFILE.replace("%S", en)),UTF8_ENCODING);
                else{
                    xmllist=LISTTESTFILES;
                    outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", en)),UTF8_ENCODING);
                }            
            BufferedReader inFile = new BufferedReader(new FileReader(xmllist));
            int uttCounter=0;
            for (;;) {
                String line = inFile.readLine();
                if (line==null) 
                    break;
                
                String filename= line.substring(line.lastIndexOf("/"));
                filename=filename.replace(CNConstants.RECOEXT, ".xml");
                String  orfilePath=developmentDir+filename;
                File file = new File(orfilePath);
                if(!file.exists())
                    orfilePath=testDir+filename;
                
                List<DetGraph> gs = gio.loadAllGraphs(orfilePath);
                List<String> allwords= new ArrayList<>();
                List<String> allpos= new ArrayList<>();
                List<String> goldlabels= new ArrayList<>();
                for (int i=0;i<gs.size();i++) {
                        DetGraph group = gs.get(i);  
                                 
                        
                        for (int j=0;j<group.getNbMots();j++) {
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

                                    /*    
                                    if(!isStopWord(group.getMot(j).getPOS()))
                                            outFile.append(lab+"\t"+group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\n");
                                    */

                                    allwords.add(group.getMot(j).getForme());
                                    allpos.add(group.getMot(j).getPOS());
                                    goldlabels.add(lab);
                                    
                        }

                           
                            //utt.getSegment().computingWordFrequencies();                        


                }
                List<String> allRecoWords=new ArrayList<>();
                BufferedReader recoFile = new BufferedReader(new FileReader(line));
                for(;;){
                    String recoLine = recoFile.readLine();
                    if (recoLine==null) 
                        break;
                    allRecoWords.add(recoLine);
                    
                }
                 
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
                    System.out.println(wordsMap.get(key));
                }
                */
                for(int i=0; i<allRecoWords.size();i++){
                    int goldIndx= wordsMap.get(i);
                    String wordStr=allRecoWords.get(i);
                    //What should I do if it does not find the word in the gold ???
                    String label=CNConstants.NOCLASS;
                    String pos=CNConstants.CHAR_NULL;
                    if(goldIndx!=CNConstants.INT_NULL){
                          label=goldlabels.get(goldIndx);
                          pos = allpos.get(goldIndx);
                    }      
                    if(pos.equals(CNConstants.CHAR_NULL)){
                        Tagger tagger = new Tagger();
                        Utterance taggedUtterance = new Utterance(wordStr);
                        tagger.computePOStags(taggedUtterance.getSegment());
                        tagger.destroy();
                        Tagger.clean(); 
                        for(Word word:taggedUtterance.getSegment().getWords()){
                            pos=word.getPosTag().getName();
                        }
                    }
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
    
 
    public static void  main(String[] args){
        ASROut asrout= new ASROut();
        asrout.processingASROutput(CNConstants.PRNOUN, true);
    }
    
}    
