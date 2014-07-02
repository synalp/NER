/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package svm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.MateParser;
import jsafran.POStagger;

import lex.Dependency;
import lex.Lemma;
import lex.Utterance;
import lex.Word;
import tools.CNConstants;
import utils.ErrorsReporting;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author rojasbar
 */
public class AnalyzeSVMClassifier {
    public static String MODELFILE="en.%S.treek.mods";
    public static String TRAINFILE="groups.%S.tab.treek.train";
    public static String TESTFILE="groups.%S.tab.treek.test";
    public static String LISTTRAINFILES="esterParseTrainALL.xmll";
    public static String LISTTESTFILES="esterTestALL.xmll";
    public static String UTF8_ENCODING="UTF8";
    public static String PROPERTIES_FILE="streek.props";
    public static String NUMFEATSINTRAINFILE="2-";
    public static String ONLYONEPNOUNCLASS=CNConstants.PRNOUN;
    public static String[] groupsOfNE = {CNConstants.PERS,CNConstants.ORG, CNConstants.LOC, CNConstants.PROD};
    public static int TRAINSIZE=Integer.MAX_VALUE;


    public AnalyzeSVMClassifier(){

    }
      public void saveFilesForLClassifier(String en, boolean bltrain) {
            try {
                
                HashMap<String,Integer> dictWords=new HashMap<>();
                HashMap<String,Integer> dictPosTag=new HashMap<>();
                HashMap<String,Integer> dictWordShape=new HashMap<>();
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
                int uttCounter=0;
                for (;;) {
                    String filename = inFile.readLine();
                    if (filename==null) break;
                    List<DetGraph> gs = gio.loadAllGraphs(filename);
                    List<Utterance> utts= new ArrayList<>();
                    for (int i=0;i<gs.size();i++) {
                            DetGraph group = gs.get(i);
                            int nexinutt=0;
                            //outFile.append("NO\tBS\tBS\n");
                            
                            Utterance utt= new Utterance();
                            utt.setId(new Long(uttCounter+1));                            
                            List<Word> words= new ArrayList<>();
                            for (int j=0;j<group.getNbMots();j++) {
                                    nexinutt++;

                                    // calcul du label
                                    int lab = CNConstants.INT_NULL;
                                    int[] groups = group.getGroups(j);
                                    if (groups!=null)
                                        for (int gr : groups) {
                                            
                                            if(en.equals(ONLYONEPNOUNCLASS)){
                                                //all the groups are proper nouns pn
                                                for(String str:groupsOfNE){
                                                    if (group.groupnoms.get(gr).startsWith(str)) {
                                                        lab=1;
                                                        break;
                                                    }
                                                }
                                            }else{
                                                if (group.groupnoms.get(gr).startsWith(en)) {
                                                    //int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                    //if (debdugroupe==j) lab = en+"B";    
                                                    //else lab = en+"I";
                                                    lab=1;
                                                    break;
                                                }
                                            }
                                        }
                                    /*        
                      
                                    if(!isStopWord(group.getMot(j).getPOS())){
					String inWiki ="F";
                                        if(!group.getMot(j).getPOS().startsWith("PRO") && !group.getMot(j).getPOS().startsWith("ADJ"))
                                            inWiki =(WikipediaAPI.processPage(group.getMot(j).getForme()).equals(CNConstants.CHAR_NULL))?"F":"T";
                                        outFile.append(lab+"\t"+group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\t"+ inWiki +"\n");
                                    } 
                                     */                                  
                                    Word word = new Word(j,group.getMot(j).getForme());
                                    word.setPOSTag(group.getMot(j).getPOS(), group.getMot(j).getLemme());
                                    word.setLabel(lab);
                                    word.setUtterance(utt);
                                    words.add(word);
                                    
                                    if(!dictWords.containsKey(word.getContent()))
                                        dictWords.put(word.getContent(), dictWords.size()+1);
                                    
                                    if(!dictPosTag.containsKey(word.getPosTag().getName()))
                                        dictPosTag.put(word.getPosTag().getName(), dictPosTag.size()+1);
                                    
                                    if(!dictWordShape.containsKey(word.getLexicalUnit().getPattern()))
                                        dictWordShape.put(word.getLexicalUnit().getPattern(), dictWordShape.size()+1);                                    
                                    
                                    
                                        
                            }
                            uttCounter++;
                            utt.setWords(words);
                            utt.computingWordFrequencies();
                            
                            //extracting the dependency tree from jsafran to our own format
                            for(int d=0;d<group.deps.size();d++){
                                Dep dep = group.deps.get(d);
                                int headidx=dep.head.getIndexInUtt()-1;
                                Word       head = words.get(headidx);
                                int depidx=dep.gov.getIndexInUtt()-1;
                                Word       governor = words.get(depidx);
                                //Assign the dependencies
                                Dependency dependency;
                                if(!utt.getDepTree().containsHead(head)){
                                    dependency = new Dependency(head);
                                }else{
                                    dependency = utt.getDepTree().getDependency(head);
                                }
                                dependency.addDependent(governor, dep.toString());
                                utt.addDependency(dependency);                            
                            }
                            //sets the roots
                            utt.getDepTree().getDTRoot();
                            utts.add(utt);

                    }
                    for(Utterance utt:utts){
                            /** 
                             * built the tree and vector features here
                             */
                            System.out.println("processing utterance:"+utt);
                            //iterate again through words
                            for(Word word:utt.getWords()){
                                   if(word.getContent().equals("Martin"))
                                       System.out.println("Entro");
                                String tree= utt.getDepTree().getTreeTopDownFeatureForHead(word);
                                tree=(tree.contains("("))?"|BT| "+ tree+ " |ET|":"";
                                String treeBUp=utt.getDepTree().getTreeBottomUpFeatureForHead(word,"");
                                treeBUp=(treeBUp.contains("("))?" |BT| ("+ treeBUp + ") |ET|":"";
                                tree=tree.trim()+ treeBUp.trim();
                                int posid=dictWords.size()+dictPosTag.get(word.getPosTag().getName());
                                int wsid= dictWords.size()+dictPosTag.size()+dictWordShape.get(word.getLexicalUnit().getPattern());
                                String vector=dictWords.get(word.getContent())+":"+utt.getWordFrequency(word.getContent())+" "+
                                              posid+":"+utt.getPOSFrequency(word.getPosTag().getName())+" "+
                                              wsid+":"+utt.getWordShapeFrequency(word.getLexicalUnit().getPattern());
                                //Print the word form just to make the debug easy
                                //outFile.append(word.getLabel()+"\t"+word.getContent()+" "+tree.trim()+" "+vector+"\n");
                                outFile.append(word.getLabel()+"\t"+tree.trim()+" "+vector+"\n");
                                
                            }                    
                    }
                    if(bltrain && uttCounter> TRAINSIZE){
                        break;
                    }                    
                }
                outFile.flush();
                outFile.close();
                inFile.close();
                ErrorsReporting.report("groups saved in groups.*.tab"+uttCounter);
            } catch (IOException e) {
                    e.printStackTrace();
            }
    }   
      
     public void parsing(boolean bltrain){
            try {
                GraphIO gio = new GraphIO(null);
                OutputStreamWriter outFile =null;
                String xmllist=LISTTRAINFILES;
                if(!bltrain)
                     xmllist=LISTTESTFILES;
                    
                
                BufferedReader inFile = new BufferedReader(new FileReader(xmllist));
//                OutputStreamWriter bigconllFile = new OutputStreamWriter(new FileOutputStream(new File("parse/all.in.conll"),true), CNConstants.UTF8_ENCODING);
                for (;;) {
                    String s = inFile.readLine();
                    if (s==null) break;
                    List<DetGraph> graphs = gio.loadAllGraphs(s);
                    String filename=s.trim().replaceAll("[\\s]+"," ");
//                    String inconll="parse/"+filename+".in.conll";
                    File outfile=new File("parse/"+filename+".out.conll");
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
                    POStagger.setFrenchModels();
                    MateParser.setMods("mate.mods.ETB"); 
                    try{
                        MateParser.parseAll(graphs);
                    }catch(Exception ex){
                        System.out.println("ERROR PARSING FILE : "+filename);
                        continue;
                    }    
                }      
            } catch (IOException e) {
                    e.printStackTrace();
            }
                    
     }
     
     public static void main(String args[]){
         AnalyzeSVMClassifier svmclass= new AnalyzeSVMClassifier();
         //svmclass.parsing(true);
         svmclass.saveFilesForLClassifier(CNConstants.PRNOUN, true);
     }
      
}
