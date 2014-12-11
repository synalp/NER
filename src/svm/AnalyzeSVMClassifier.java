/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package svm;


import edu.emory.mathcs.backport.java.util.Collections;
import edu.stanford.nlp.util.Pair;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.MateParser;
import jsafran.POStagger;

import lex.Dependency;
import lex.DependencyTree;
import lex.Segment;
import lex.Utterance;
import lex.Word;
import linearclassifier.AnalyzeLClassifier;
import static linearclassifier.AnalyzeLClassifier.isStopWord;
import tools.CNConstants;
import tools.GeneralConfig;
import tools.Histoplot;
import utils.ErrorsReporting;
import tools.IntegerValueComparator;

/**
 *
 * @author rojasbar
 */
public class AnalyzeSVMClassifier implements Serializable{
    private static final long serialVersionUID = 1L; 
    public static String MODELFILE="en.%S.treek.mods";
    public static String TRAINFILE="groups.%S.tab.treek.train";
    public static String TESTFILE="groups.%S.tab.treek.test";
    public static String INCHUNKER="chunk/%S.in.chunker";
    public static String OUTCHUNKER="chunk/%S.out.chunker";
    public static String LISTTRAINFILES="esterParseTrainALL.xmll";//"esterParseTrainETB.xmll";//
    public static String LISTTESTFILES="esterParseTestALL.xmll";//"esterParseTestETB.xmll"; //
    public static String UTF8_ENCODING="UTF8";
    //public static String PROPERTIES_FILE="streek.props";
    public static String NUMFEATSINTRAINFILE="2-";
    public static String ONLYONEPNOUNCLASS=CNConstants.PRNOUN;
    public static String[] groupsOfNE = {CNConstants.PERS,CNConstants.ORG, CNConstants.LOC, CNConstants.PROD};
    public static int TRAINSIZE= Integer.MAX_VALUE;
    

    private HashMap<String,Integer> dictFeatures=new HashMap<>();
    private String[][] stdictTrainFeatures;
    
    
    public AnalyzeSVMClassifier(){
        GeneralConfig.loadProperties();
        LISTTRAINFILES=GeneralConfig.listSVMTrain;
        LISTTESTFILES=GeneralConfig.listSVMTest;
        
    }
    public void saveFilesForLClassifierWords(String en, boolean istrain, boolean onlyVector, boolean isPOS) {
            try {

                GraphIO gio = new GraphIO(null);
                OutputStreamWriter outFile =null;
                String xmllist=LISTTRAINFILES;
                if(istrain){
                    String fname=(onlyVector)?TRAINFILE.replace("%S", en).replace("treek", "vector"):TRAINFILE.replace("%S", en);
                    outFile = new OutputStreamWriter(new FileOutputStream(fname),UTF8_ENCODING);
                }else{
                    xmllist=LISTTESTFILES;
                     String fname=(onlyVector)?TESTFILE.replace("%S", en).replace("treek", "vector"):TESTFILE.replace("%S", en);
                    outFile = new OutputStreamWriter(new FileOutputStream(fname),UTF8_ENCODING);
                    if(dictFeatures.isEmpty())
                        deserializingFeatures();
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
                                    /*
                                    if(!dictFeatures.containsKey(word.getContent()))
                                        dictFeatures.put(word.getContent(), dictFeatures.size()+1);
                                    */
                                    if(!dictFeatures.containsKey(word.getPosTag().getFName()))
                                        dictFeatures.put(word.getPosTag().getFName(), dictFeatures.size()+1);
                                    
                                    if(!dictFeatures.containsKey(word.getLexicalUnit().getPattern()))
                                        dictFeatures.put(word.getLexicalUnit().getPattern(), dictFeatures.size()+1);                                    
                                    
                                    
                                        
                            }
                            uttCounter++;
                            utt.setWords(words);
                            utt.getSegment().computingWordFrequencies();
                            
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
                                List<Integer> vals= new ArrayList<>();
                                //int wordid= dictFeatures.get(word.getContent());
                                int posid=dictFeatures.get(word.getPosTag().getFName());
                                int wsid= dictFeatures.get(word.getLexicalUnit().getPattern());
                                //vals.add(wordid);
                                vals.add(posid);vals.add(wsid);        
                                Collections.sort(vals);
                                String vector="";
                                for(int i=0; i<vals.size();i++)                                
                                    vector+=vals.get(i)+":1 ";
                                
                                vector=vector.trim();
                                //vector = word.getContent()+" "+vector;
                                //Print the word form just to make the debug easy*/
                                //outFile.append(word.getLabel()+"\t"+word.getContent()+" "+tree.trim()+" "+vector+"\n");
                                //outFile.append(word.getLabel()+"\t"+tree.trim()+" "+vector+"\n");
                                if(onlyVector)
                                    outFile.append(word.getLabel()+"\t"+vector+"\n");
                                else{
                                    String tree= utt.getDepTree().getTreeTopDownFeatureForHeadCW(word,0,isPOS);
                                    tree=(tree.contains("("))?"|BT| "+ tree+ "|ET|":"|BT| |ET|";
                                    //String treeBUp=utt.getDepTree().getTreeBottomUpFeatureForHead(word,"");
                                    //treeBUp=(treeBUp.contains("("))?" |BT| ("+ treeBUp + ") |ET|":"|BT| |ET|";
                                    //tree=tree.trim()+ treeBUp.trim();    
                                    outFile.append(word.getLabel()+"\t"+tree.trim()+" "+vector+"\n");
                                }
                            }   
                        
                    }
                    if(istrain && uttCounter> TRAINSIZE){
                        break;
                    }                    
                }
                outFile.flush();
                outFile.close();
                inFile.close();
                if(istrain)
                    serializingFeatures();
                ErrorsReporting.report("groups saved in groups.*.tab"+uttCounter);
            } catch (IOException e) {
                    e.printStackTrace();
            }
    }   
    
    public void saveFilesForLClassifierWordsPruningTrees(String en, boolean istrain, boolean onlyVector, boolean isCW, boolean isTopDown, boolean isBottomUp, boolean isPOS) {
            try {

                GraphIO gio = new GraphIO(null);
                OutputStreamWriter outFile =null;
                String xmllist=LISTTRAINFILES;
                if(istrain){
                    String fname=(onlyVector)?TRAINFILE.replace("%S", en).replace("treek", "vector"):TRAINFILE.replace("%S", en);
                    outFile = new OutputStreamWriter(new FileOutputStream(fname),UTF8_ENCODING);
                }else{
                    xmllist=LISTTESTFILES;
                     String fname=(onlyVector)?TESTFILE.replace("%S", en).replace("treek", "vector"):TESTFILE.replace("%S", en);
                    outFile = new OutputStreamWriter(new FileOutputStream(fname),UTF8_ENCODING);
                    if(dictFeatures.isEmpty())
                        deserializingFeatures();
                }
                //load stanford features
                addStanfordLCFeatures(istrain);
                BufferedReader inFile = new BufferedReader(new FileReader(xmllist));
                int uttCounter=0;
                int wordCounter=0;
                for (;;) {
                    String filename = inFile.readLine();
                    if (filename==null) break;
                    List<DetGraph> gs = gio.loadAllGraphs(filename);
                    List<Utterance> utts= new ArrayList<>();
                    for (int i=0;i<gs.size();i++) {
                            DetGraph group = gs.get(i);
                            //outFile.append("NO\tBS\tBS\n");
                            
                            Utterance utt= new Utterance();
                            utt.setId(new Long(uttCounter+1));                            
                            List<Word> words= new ArrayList<>();
                            for (int j=0;j<group.getNbMots();j++) {
                                    

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
                                    wordCounter++;
                                    /*
                                    if(!dictFeatures.containsKey(word.getContent()))
                                        dictFeatures.put(word.getContent(), dictFeatures.size()+1);
                                    
                                    if(!dictFeatures.containsKey(word.getPosTag().getFName()))
                                        dictFeatures.put(word.getPosTag().getFName(), dictFeatures.size()+1);
                                    
                                    if(!dictFeatures.containsKey(word.getLexicalUnit().getPattern()))
                                        dictFeatures.put(word.getLexicalUnit().getPattern(), dictFeatures.size()+1);                                    
                                    */
                                    String[] addFeats = null;
                                    if(stdictTrainFeatures[wordCounter]!=null)
                                        addFeats = stdictTrainFeatures[wordCounter];
                                    
                                    if(addFeats == null)
                                       ErrorsReporting.report("NOT FEATURES FOUND FOR WORD["+wordCounter+"] = "+word); 
                                    List<String> addListFeats=new ArrayList<>();
                                    for(String feat:addFeats){
                                        /*
                                        if(!feat.contains("#"))
                                            continue;
                                        //extracts the letter ngram features
                                        //filteredFeats.add(feat);//*/
                                        addListFeats.add(feat);
                                        if(!dictFeatures.containsKey(feat))
                                            dictFeatures.put(feat, dictFeatures.size()+1);
                                    }
                                    //add letter ngram features
                                    word.setAdditionalFeats(addListFeats);                                    
                                        
                            }
                            uttCounter++;
                            utt.setWords(words);
                            utt.getSegment().computingWordFrequencies();
                            
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
                                List<Integer> vals= new ArrayList<>();
                                /*
                                //int wordid= dictFeatures.get(word.getContent());
                                int posid=dictFeatures.get(word.getPosTag().getFName());
                                int wsid= dictFeatures.get(word.getLexicalUnit().getPattern());
                                //vals.add(wordid);
                                vals.add(posid);vals.add(wsid);        
                                Collections.sort(vals);
                                String vector="";
                                for(int i=0; i<vals.size();i++)                                
                                    vector+=vals.get(i)+":1 ";
                                
                                vector=vector.trim();
                                */
                                //vector = word.getContent()+" "+vector;
                                //Print the word form just to make the debug easy*/
                                //outFile.append(word.getLabel()+"\t"+word.getContent()+" "+tree.trim()+" "+vector+"\n");
                                //outFile.append(word.getLabel()+"\t"+tree.trim()+" "+vector+"\n");
                                
                                List<String> letterNgrams= word.getAdditionalFeats();
                                for(String feat:letterNgrams){
                                    vals.add(dictFeatures.get(feat));
                                }

                                Collections.sort(vals);
                                String vector="";
                                for(int i=0; i<vals.size();i++)                                
                                    vector+=vals.get(i)+":1 ";                                
                                
                                if(onlyVector)
                                    outFile.append(word.getLabel()+"\t"+vector+"\n");
                                    //outFile.append(word.getLabel()+"\t"+vector+"\n");
                                else{
                                    
                                    DependencyTree subTree= new DependencyTree();
                                    if(word.getContent().contains("("))
                                        System.out.println("found");
                                    String tree="";
                                    if(isTopDown){
                                        if(isCW)
                                            tree= utt.getDepTree().getPruningTreeTopDownFeatureForHeadCW(word,subTree,isPOS);
                                        else
                                            tree= utt.getDepTree().getPruningTreeTopDownFeatureForHead(word,subTree,isPOS);
                                        //tree=(tree.contains("("))?"|BT| "+ tree+ "|ET|":"|BT| |ET|";
                                        tree=(tree.contains("("))?"|BT| "+ tree+ " ":"|BT| ";
                                    }
                                    subTree= new DependencyTree();
                                    String treeBUp="";
                                    if(isBottomUp){
                                        if(isCW)
                                            treeBUp=utt.getDepTree().getPrunningTreeBottomUpFeatureForHeadCW(word,"",subTree,isPOS);
                                        else
                                            treeBUp=utt.getDepTree().getPrunningTreeBottomUpFeatureForHead(word,"",subTree,isPOS);
                                        treeBUp=(treeBUp.contains("("))?" |BT| ("+ treeBUp + ") |ET|":"|BT| |ET|";
                                    }
                                    tree=tree.trim()+ treeBUp.trim();
                                    //outFile.append(word.getLabel()+"\t"+ word.getContent() +" "+tree.trim()+" nnodes= "+ subTree.getNumberOfNodes() + " level= "+subTree.getLevel() +" "+vector+"\n");
                                    //if(!isStopWord(word.getPosTag().getName()))
                                        outFile.append(word.getLabel()+"\t"+tree.trim() +" "+vector+"\n");
                                }
                            }   
                        
                    }
                    if(istrain && uttCounter> TRAINSIZE){
                        break;
                    }                    
                }
                outFile.flush();
                outFile.close();
                inFile.close();
                if(istrain)
                    serializingFeatures();
                ErrorsReporting.report("groups saved in groups.*.tab"+uttCounter);
            } catch (IOException e) {
                    e.printStackTrace();
            }
    } 

    public void saveConllForLClassifierWordsPruningTrees(String en, boolean istrain, boolean onlyVector, boolean isCW, boolean isTopDown, boolean isBottomUp, boolean isPOS) {
            try {
                String corpusDir = GeneralConfig.corpusDir;
                String corpusTrain= GeneralConfig.corpusTrain;
                String corpusTest=GeneralConfig.corpusTest;
                BufferedReader conllFile = null;
                GraphIO gio = new GraphIO(null);
                OutputStreamWriter outFile =null;
                String xmllist=LISTTRAINFILES;
                if(istrain){
                    conllFile= new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusTrain)); 
                    String fname=(onlyVector)?TRAINFILE.replace("%S", en).replace("treek", "vector"):TRAINFILE.replace("%S", en);
                    outFile = new OutputStreamWriter(new FileOutputStream(fname),UTF8_ENCODING);
                }else{
                    conllFile= new BufferedReader(new FileReader(corpusDir+System.getProperty("file.separator")+corpusTest)); 
                    xmllist=LISTTESTFILES;
                     String fname=(onlyVector)?TESTFILE.replace("%S", en).replace("treek", "vector"):TESTFILE.replace("%S", en);
                    outFile = new OutputStreamWriter(new FileOutputStream(fname),UTF8_ENCODING);
                    if(dictFeatures.isEmpty())
                        deserializingFeatures();
                }
                //load stanford features
                addStanfordLCFeatures(istrain);
                BufferedReader inFile = new BufferedReader(new FileReader(xmllist));
                int uttCounter=0;
                int wordCounter=0;
                for (;;) {
                    String filename = inFile.readLine();
                    if (filename==null) break;
                    List<DetGraph> gs = gio.loadAllGraphs(filename);
                    List<Utterance> utts= new ArrayList<>();
                    for (int i=0;i<gs.size();i++) {
                            DetGraph group = gs.get(i);
                            //outFile.append("NO\tBS\tBS\n");
                            
                            Utterance utt= new Utterance();
                            utt.setId(new Long(uttCounter+1));                            
                            List<Word> words= new ArrayList<>();
                            for (int j=0;j<group.getNbMots();j++) {
                                    String conllLine="";
                                    while(true){
                                        conllLine = conllFile.readLine();
                                        if(conllLine == null)
                                            break;
                                        if(conllLine.startsWith("-DOCSTART-"))
                                            continue;
                                        if(conllLine.equals(""))
                                            continue;
                                        if(conllLine.startsWith(group.getMot(j).getForme()))
                                            break;
                                    }
                                    int lab = CNConstants.INT_NULL;
                                    if(conllLine!=null){
                                        String[] cols = conllLine.split("\\s");
                                        if(cols.length>0 && !cols[3].equals("O"))
                                                lab=1;
                                            
                                    }else
                                        System.out.println("alignment problem");
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
                                    wordCounter++;
                                    /*
                                    if(!dictFeatures.containsKey(word.getContent()))
                                        dictFeatures.put(word.getContent(), dictFeatures.size()+1);
                                    
                                    if(!dictFeatures.containsKey(word.getPosTag().getFName()))
                                        dictFeatures.put(word.getPosTag().getFName(), dictFeatures.size()+1);
                                    
                                    if(!dictFeatures.containsKey(word.getLexicalUnit().getPattern()))
                                        dictFeatures.put(word.getLexicalUnit().getPattern(), dictFeatures.size()+1);                                    
                                    */
                                    String[] addFeats=null;
                                    if(stdictTrainFeatures[wordCounter]!=null)
                                        addFeats = stdictTrainFeatures[wordCounter];
                                    
                                    if(addFeats==null)
                                       ErrorsReporting.report("NOT FEATURES FOUND FOR WORD["+wordCounter+"] = "+word); 
                                    List<String> addListFeats=new ArrayList<>();
                                    for(String feat:addFeats){
                                        /*
                                        if(!feat.contains("#"))
                                            continue;
                                        //extracts the letter ngram features
                                        //filteredFeats.add(feat);//*/
                                        addListFeats.add(feat);
                                        if(!dictFeatures.containsKey(feat))
                                            dictFeatures.put(feat, dictFeatures.size()+1);
                                    }
                                    //add letter ngram features
                                    word.setAdditionalFeats(addListFeats);                                    
                                        
                            }
                            uttCounter++;
                            utt.setWords(words);
                            utt.getSegment().computingWordFrequencies();
                            
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
                                List<Integer> vals= new ArrayList<>();
                                /*
                                //int wordid= dictFeatures.get(word.getContent());
                                int posid=dictFeatures.get(word.getPosTag().getFName());
                                int wsid= dictFeatures.get(word.getLexicalUnit().getPattern());
                                //vals.add(wordid);
                                vals.add(posid);vals.add(wsid);        
                                Collections.sort(vals);
                                String vector="";
                                for(int i=0; i<vals.size();i++)                                
                                    vector+=vals.get(i)+":1 ";
                                
                                vector=vector.trim();
                                */
                                //vector = word.getContent()+" "+vector;
                                //Print the word form just to make the debug easy*/
                                //outFile.append(word.getLabel()+"\t"+word.getContent()+" "+tree.trim()+" "+vector+"\n");
                                //outFile.append(word.getLabel()+"\t"+tree.trim()+" "+vector+"\n");
                                
                                List<String> letterNgrams= word.getAdditionalFeats();
                                for(String feat:letterNgrams){
                                    vals.add(dictFeatures.get(feat));
                                }

                                Collections.sort(vals);
                                String vector="";
                                for(int i=0; i<vals.size();i++)                                
                                    vector+=vals.get(i)+":1 ";                                
                                
                                if(onlyVector)
                                    outFile.append(word.getLabel()+"\t"+vector+"\n");
                                    //outFile.append(word.getLabel()+"\t"+vector+"\n");
                                else{
                                    
                                    DependencyTree subTree= new DependencyTree();
                                    if(word.getContent().contains("("))
                                        System.out.println("found");
                                    String tree="";
                                    if(isTopDown){
                                        if(isCW)
                                            tree= utt.getDepTree().getPruningTreeTopDownFeatureForHeadCW(word,subTree,isPOS);
                                        else
                                            tree= utt.getDepTree().getPruningTreeTopDownFeatureForHead(word,subTree,isPOS);
                                        //tree=(tree.contains("("))?"|BT| "+ tree+ "|ET|":"|BT| |ET|";
                                        tree=(tree.contains("("))?"|BT| "+ tree+ " ":"|BT| ";
                                    }
                                    subTree= new DependencyTree();
                                    String treeBUp="";
                                    if(isBottomUp){
                                        if(isCW)
                                            treeBUp=utt.getDepTree().getPrunningTreeBottomUpFeatureForHeadCW(word,"",subTree,isPOS);
                                        else
                                            treeBUp=utt.getDepTree().getPrunningTreeBottomUpFeatureForHead(word,"",subTree,isPOS);
                                        treeBUp=(treeBUp.contains("("))?" |BT| ("+ treeBUp + ") |ET|":"|BT| |ET|";
                                    }
                                    tree=tree.trim()+ treeBUp.trim();
                                    //outFile.append(word.getLabel()+"\t"+ word.getContent() +" "+tree.trim()+" nnodes= "+ subTree.getNumberOfNodes() + " level= "+subTree.getLevel() +" "+vector+"\n");
                                    //if(!isStopWord(word.getPosTag().getName()))
                                        outFile.append(word.getLabel()+"\t"+tree.trim() +" "+vector+"\n");
                                }
                            }   
                        
                    }
                    if(istrain && uttCounter> TRAINSIZE){
                        break;
                    }                    
                }
                outFile.flush();
                outFile.close();
                inFile.close();
                if(istrain)
                    serializingFeatures();
                ErrorsReporting.report("groups saved in groups.*.tab"+uttCounter);
            } catch (IOException e) {
                    e.printStackTrace();
            }
    }     
    
    public void saveFilesForPolyClassifierWords(String en, boolean istrain) {
            try {

                GraphIO gio = new GraphIO(null);
                OutputStreamWriter outFile =null;
                String xmllist=LISTTRAINFILES;
                if(istrain){
                    String fname=TRAINFILE.replace("%S", en).replace("treek", "poly");
                    outFile = new OutputStreamWriter(new FileOutputStream(fname),UTF8_ENCODING);
                    
                }else{
                    xmllist=LISTTESTFILES;
                     String fname=TESTFILE.replace("%S", en).replace("treek", "poly");
                    outFile = new OutputStreamWriter(new FileOutputStream(fname),UTF8_ENCODING);
                    if(dictFeatures.isEmpty())
                        deserializingFeatures();
                }
                //load stanford features
                addStanfordLCFeatures(istrain);
                //reads the input file
                BufferedReader inFile = new BufferedReader(new FileReader(xmllist));
                int uttCounter=0;
                int wordCounter=0;
                for (;;) {
                    String filename = inFile.readLine();
                    if (filename==null) break;
                    List<DetGraph> gs = gio.loadAllGraphs(filename);
                    List<Utterance> utts= new ArrayList<>();
                    for (int i=0;i<gs.size();i++) {
                            DetGraph group = gs.get(i);
                            //outFile.append("NO\tBS\tBS\n");
                            
                            Utterance utt= new Utterance();
                            utt.setId(new Long(uttCounter+1));                            
                            List<Word> words= new ArrayList<>();
                            for (int j=0;j<group.getNbMots();j++) {
                                    

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
                                    wordCounter++;
                                    /*
                                    if(!dictFeatures.containsKey(word.getContent()))
                                        dictFeatures.put(word.getContent(), dictFeatures.size()+1);
                                    
                                    if(!dictFeatures.containsKey(word.getPosTag().getFName()))
                                        dictFeatures.put(word.getPosTag().getFName(), dictFeatures.size()+1);
                                    
                                    if(!dictFeatures.containsKey(word.getLexicalUnit().getPattern()))
                                        dictFeatures.put(word.getLexicalUnit().getPattern(), dictFeatures.size()+1);  
                                    */
                                    String[] addFeats=null;
                                    if(stdictTrainFeatures[wordCounter]!=null)
                                        addFeats = stdictTrainFeatures[wordCounter];
                                    
                                    if(addFeats==null)
                                       ErrorsReporting.report("NOT FEATURES FOUND FOR WORD["+wordCounter+"] = "+word); 
                                    List<String> addListFeats=new ArrayList<>();
                                    for(String feat:addFeats){
                                        /*
                                        if(!feat.contains("#"))
                                            continue;
                                        //extracts the letter ngram features
                                        //filteredFeats.add(feat);//*/
                                        addListFeats.add(feat);
                                        if(!dictFeatures.containsKey(feat))
                                            dictFeatures.put(feat, dictFeatures.size()+1);
                                    }
                                    //add letter ngram features
                                    word.setAdditionalFeats(addListFeats);
                                        
                            }
                            uttCounter++;
                            utt.setWords(words);
                            utt.getSegment().computingWordFrequencies();
                            
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
                           /*
                           for(Word word:utt.getWords()){
                              String tree= utt.getDepTree().getTreeTopDownFeatureForHead(word,false);
                              tree=(tree.contains("("))?"|BT| "+ tree+ "|ET|":"|BT| |ET|"; 
                              dictFeatures.put(tree,dictFeatures.size()+1);
                           } 
                           //*/

                    }
                  
                    
                    for(Utterance utt:utts){
                            /** 
                             * built the tree and vector features here
                             */
                            System.out.println("processing utterance:"+utt);
                            //iterate again through words
                            
                    
                            
                            for(Word word:utt.getWords()){
                                List<Integer> vals= new ArrayList<>();
                                //int wordid= dictFeatures.get(word.getContent());
                                /*
                                int posid=dictFeatures.get(word.getPosTag().getFName());
                                int wsid= dictFeatures.get(word.getLexicalUnit().getPattern());
                                vals.add(posid);vals.add(wsid);
                                //*/
                                /*
                                String tree= utt.getDepTree().getTreeTopDownFeatureForHead(word,false);
                                tree=(tree.contains("("))?"|BT| "+ tree+ "|ET|":"|BT| |ET|";
                                int treeid=dictFeatures.get(tree);
                                vals.add(treeid);
                                //*/
                                //String treeBUp=utt.getDepTree().getTreeBottomUpFeatureForHead(word,"");
                                //treeBUp=(treeBUp.contains("("))?" |BT| ("+ treeBUp + ") |ET|":"|BT| |ET|";
                                //tree=tree.trim()+ treeBUp.trim();    
                                //vals.add(wordid);
                                 
                                
                                List<String> letterNgrams= word.getAdditionalFeats();
                                for(String feat:letterNgrams){
                                    vals.add(dictFeatures.get(feat));
                                }
                                                                
                                
                                Collections.sort(vals);
                                String vector="";
                                for(int i=0; i<vals.size();i++)                                
                                    vector+=vals.get(i)+":1 ";
                                
                                vector=vector.trim();                                
                                //outFile.append(word.getLabel()+" "+word.getContent() +"\t"+vector+"\n");
                                if(!isStopWord(word.getPosTag().getName()))
                                    outFile.append(word.getLabel()+"\t"+vector+"\n");
                            }   
                        
                    }
                    if(istrain && uttCounter> TRAINSIZE){
                        break;
                    }                    
                }
                outFile.flush();
                outFile.close();
                inFile.close();
                if(istrain)
                    serializingFeatures();
                ErrorsReporting.report("groups saved in groups.*.tab"+uttCounter);
            } catch (IOException e) {
                    e.printStackTrace();
            }
    }       
    
    /**
     * Syntactic driven spans
     * @param en
     * @param onlyVector 
     */
    
    public void saveFilesForLClassifierSpans(String en, boolean onlyVector, boolean isPOS) {
            try {
    
                int totalOfspans=0;
                
                GraphIO gio = new GraphIO(null);
                OutputStreamWriter outFile =null;
                String xmllist=LISTTRAINFILES;
                
                String fname=(onlyVector)?TRAINFILE.replace("%S", en).replace("treek", "spvector"):TRAINFILE.replace("%S", en).replace("treek", "sptk");
                outFile = new OutputStreamWriter(new FileOutputStream(fname),UTF8_ENCODING);
                
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
                            HashMap<Pair,String> entitySpans = new HashMap<>();
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
                                                        int stgroup = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                        int endgroup = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;
                                                        Pair pair = new Pair(new Integer(stgroup), new Integer(endgroup));
                                                        entitySpans.put(pair, en);
                                                        lab=1;                                                       
                                                        break;
                                                    }
                                                }
                                            }else{
                                                if (group.groupnoms.get(gr).startsWith(en)) {
                                                    int stgroup = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                    int endgroup = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;
                                                    Pair pair = new Pair(new Integer(stgroup), new Integer(endgroup));
                                                    List<Pair> pairs=new ArrayList<>();
                                                    entitySpans.put(pair, en);
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
                                    
                                    if(!dictFeatures.containsKey(word.getContent()))
                                        dictFeatures.put(word.getContent(), dictFeatures.size()+1);
                                    
                                    if(!dictFeatures.containsKey(word.getPosTag().getName()))
                                        dictFeatures.put(word.getPosTag().getName(), dictFeatures.size()+1);
                                    
                                    if(!dictFeatures.containsKey(word.getLexicalUnit().getPattern()))
                                        dictFeatures.put(word.getLexicalUnit().getPattern(), dictFeatures.size()+1);         
                            }
                            uttCounter++;
                            utt.setWords(words);
                            
                            
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
                            //add the gold spans
                            for(Pair pair:entitySpans.keySet()){
                                int start = ((Integer) pair.first).intValue();
                                int end = ((Integer) pair.second).intValue();
                                Segment span = new Segment(words.subList(start, end+1));
                                span.computingWordFrequencies();
                                utt.addEntitySpan(entitySpans.get(pair), span);
                                
                            }
                    }
                    for(Utterance utt:utts){
                        /** 
                         * built the tree and vector features here
                         */
                        System.out.println("processing utterance:"+utt);
                        //find all possible spans according to dependency trees                        

                        List<Word> roots = utt.getDepTree().getDTRoot();
                        HashMap<Segment,String> neSegs = utt.getGoldEntities();
                        HashMap<Segment,Integer> outSegs = new HashMap<>();                        
                        
                        if(utt.toString().contains("bonjour"))
                            System.out.println("Entro");
                        for(Word head:roots){
                            List<Word> words=new ArrayList();
                            words.add(head);
                            Segment rootWordSeg= new Segment(words);
                            Segment headSegment = utt.getDepTree().getWholeSegmentRootedBy(utt,rootWordSeg);

                            //headSegment.computingWordFrequencies();
                            int label=-1;
                            if(outSegs.keySet().toString().contains(headSegment.toString()))
                                continue;
                            HashMap<Segment,Integer> tmpOutSegs = new HashMap<>(outSegs);
                            if(!utt.isEntitySpan(headSegment)){          
                                List<Segment> entSegs=new ArrayList<>(neSegs.keySet());
                                List<Segment> segs= headSegment.difference(entSegs);

                               for(Segment seg:segs){
                                   boolean segFound=false;
                                    for(Segment oSeg:outSegs.keySet()){
                                        if(oSeg.contains(seg)){
                                          segFound=true;
                                          break;
                                        }
                                        if(seg.contains(oSeg))
                                            tmpOutSegs.remove(oSeg);                                             
                                    }
                                    if(!segFound)
                                        tmpOutSegs.put(seg,label);
                                }
                               outSegs=tmpOutSegs;


                            } 

                                 
                                
                            }
                            //compute frequencies per span
                            List<Segment> allSegs = new ArrayList<>();                          
                            allSegs.addAll(neSegs.keySet());
                            allSegs.addAll(outSegs.keySet());
                            Collections.sort(allSegs, new Comparator<Segment>() {
                            @Override
                            public int compare(Segment s1, Segment s2)
                            {
                                    return s1.getStart()-s2.getStart();
                            }});  

                            for(Segment seg:allSegs){
                                seg.computingWordFrequencies();
                                int label = CNConstants.INT_NULL;
                                if(neSegs.containsKey(seg))
                                    label = 1;
                                
                                //Feature extraction
                                String vector="";
                                String bow="";
                                HashMap<Integer,Integer> vals= new HashMap<>();
                                for(Word w:seg.getWords()){
                                    int wid=dictFeatures.get(w.getContent());
                                    vals.put(wid,seg.getWordFrequency(w.getContent())); 
                                    bow+=w.getContent()+" ";
                                }

                                for(String pos:seg.getPOSFrequencies().keySet()){
                                    int posid=dictFeatures.get(pos);
                                    vals.put(posid,seg.getPOSFrequency(pos)); 
                                } 

                                for(String ws:seg.getWordShapeFrequency().keySet()){
                                    int wsid= dictFeatures.get(ws);
                                    vals.put(wsid,seg.getWordShapeFrequency(ws));
                                }
                                    
                                List<Integer> keys= new ArrayList(vals.keySet());
                                Collections.sort(keys);
                                for(Integer key:keys){
                                    vector+= key+":"+vals.get(key)+" ";
                                }
                                if(onlyVector)
                                    outFile.append(label+"\t"+vector.trim()+"\n");
                                else{
                                    String tree= utt.getDepTree().getTreeTopDownFeatureForSegment(seg,isPOS);
                                    tree=(tree.contains("("))?"|BT| "+ tree+ " |ET|":"|BT| |ET|";
                                    //String treeBUp=utt.getDepTree().getTreeBottomUpFeatureForHead(word,"");
                                    //treeBUp=(treeBUp.contains("("))?" |BT| ("+ treeBUp + ") |ET|":"";
                                    tree=tree.trim(); //+ treeBUp.trim();    
                                    //outFile.append(label+"\t"+bow.trim()+" "+tree.trim()+" "+vector.trim()+"\n");
                                    outFile.append(label+"\t"+tree.trim()+" "+vector.trim()+"\n");
                                }   
                                totalOfspans++;
                              }     
                              

                    }
                    
                    if(uttCounter> TRAINSIZE){
                        break;
                    }                    
                }
                outFile.flush();
                outFile.close();
                inFile.close();
                serializingFeatures();
                ErrorsReporting.report("groups saved in groups.*.tab || utterances:"+uttCounter+" spans: "+totalOfspans);
            } catch (IOException e) {
                    e.printStackTrace();
            }
    }   
    
    public void saveFilesForClassifierAllSpans(String en,boolean istrain, boolean onlyVector, boolean isPOS) {
            try {

                int totalOfspans=0;
                
                GraphIO gio = new GraphIO(null);
                OutputStreamWriter outFile =null;
                String fname="";
                String xmllist=LISTTRAINFILES;
                if(istrain)
                    fname=(onlyVector)?TRAINFILE.replace("%S", en).replace("treek", "spvector"):TRAINFILE.replace("%S", en).replace("treek", "asptk");    
                else{
                    xmllist=LISTTESTFILES;
                    fname=(onlyVector)?TESTFILE.replace("%S", en).replace("treek", "spvector"):TESTFILE.replace("%S", en).replace("treek", "asptk"); 
                    if(dictFeatures.isEmpty())
                        deserializingFeatures();
                }
                outFile = new OutputStreamWriter(new FileOutputStream(fname),UTF8_ENCODING);
                BufferedReader inFile = new BufferedReader(new FileReader(xmllist));
                int uttCounter=0;
                for (;;) {
                    String filename = inFile.readLine();
                    if (filename==null) break;
                    List<DetGraph> gs = gio.loadAllGraphs(filename);
                    List<Utterance> utts= new ArrayList<>();
                    for (int i=0;i<gs.size();i++) {
                            DetGraph group = gs.get(i);
                            
                            //outFile.append("NO\tBS\tBS\n");
                            
                            Utterance utt= new Utterance();
                            utt.setId(new Long(uttCounter+1));                            
                            List<Word> words= new ArrayList<>();
                            HashMap<Pair,String> entitySpans = new HashMap<>();
                            for (int j=0;j<group.getNbMots();j++) {
                                    
                                    // calcul du label
                                    int lab = CNConstants.INT_NULL;
                                    int[] groups = group.getGroups(j);
                                    if (groups!=null)
                                        for (int gr : groups) {
                                            
                                            if(en.equals(ONLYONEPNOUNCLASS)){
                                                //all the groups are proper nouns pn
                                                for(String str:groupsOfNE){
                                                    if (group.groupnoms.get(gr).startsWith(str)) {
                                                        int stgroup = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                        int endgroup = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;
                                                        Pair pair = new Pair(new Integer(stgroup), new Integer(endgroup));
                                                        entitySpans.put(pair, en);
                                                        lab=1;                                                       
                                                        break;
                                                    }
                                                }
                                            }else{
                                                if (group.groupnoms.get(gr).startsWith(en)) {
                                                    int stgroup = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                    int endgroup = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;
                                                    Pair pair = new Pair(new Integer(stgroup), new Integer(endgroup));
                                                    List<Pair> pairs=new ArrayList<>();
                                                    entitySpans.put(pair, en);
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
                                    
                                    if(!dictFeatures.containsKey(word.getContent()))
                                        dictFeatures.put(word.getContent(), dictFeatures.size()+1);
                                    
                                    if(!dictFeatures.containsKey(word.getPosTag().getName()))
                                        dictFeatures.put(word.getPosTag().getName(), dictFeatures.size()+1);
                                    
                                    if(!dictFeatures.containsKey(word.getLexicalUnit().getPattern()))
                                        dictFeatures.put(word.getLexicalUnit().getPattern(), dictFeatures.size()+1);         
                            }
                            uttCounter++;
                            utt.setWords(words);
                            
                            
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
                            //add the gold spans
                            for(Pair pair:entitySpans.keySet()){
                                int start = ((Integer) pair.first).intValue();
                                int end = ((Integer) pair.second).intValue();
                                Segment span = new Segment(words.subList(start, end+1));
                                utt.addEntitySpan(entitySpans.get(pair), span);
                                
                            }
                    }
                    for(Utterance utt:utts){
                            /** 
                             * built the tree and vector features here
                             */
                            System.out.println("processing utterance:"+utt);
                            //find all possible spans according to dependency trees                        
                            if(utt.toString().contains("rue Rodier à Paris"))
                                System.out.println("Entro");                            
                            List<Word> heads = utt.getDepTree().getHeadsInSegment(utt.getSegment().getStart(), utt.getSegment().getEnd());
                            
                            HashMap<Segment,String> neSegs = utt.getGoldEntities();
                            List<Segment> neSegList = new ArrayList<>(neSegs.keySet());
                            List<Segment> neSegList2 = new ArrayList<>(neSegs.keySet());
 
                             List<Segment> headSegList = new ArrayList<>();
                            for(Word head:heads){

                                HashMap<Segment,String> headSegs = utt.getDepTree().getHeadDepSpans(head,utt.getSegment());
          
                                headSegList.addAll(headSegs.keySet());
                               
                                for(Segment headSegment:headSegs.keySet()){ 
                                    headSegment.computingWordFrequencies();
                                    int label=-1;
                                    if(utt.isEntitySpan(headSegment))
                                        label=utt.getSegment().getWord(headSegment.getEnd()).getLabel();
                                    
                                                                            
                                    //compute frequencies per span

                                    //Feature extraction
                                    String vector="";
                                    String bow="";
                                    HashMap<Integer,Integer> vals= new HashMap<>();
                                    for(Word w:headSegment.getWords()){
                                        bow+=w.getContent()+" ";
                                    }
                                        
                                    for(String pos:headSegment.getPOSFrequencies().keySet()){
                                        int posid=dictFeatures.get(pos);
                                        vals.put(posid,headSegment.getPOSFrequency(pos)); 
                                    } 
                                    
                                    for(String ws:headSegment.getWordShapeFrequency().keySet()){
                                        int wsid= dictFeatures.get(ws);
                                        vals.put(wsid,headSegment.getWordShapeFrequency(ws));
                                    }
                                    
                                    List<Integer> keys= new ArrayList(vals.keySet());
                                    Collections.sort(keys);
                                    for(Integer key:keys){
                                        vector+= key+":"+vals.get(key)+" ";
                                    }
                                    if(onlyVector)
                                        outFile.append(label+"\t"+vector.trim()+"\n");
                                    else{
                                        String tree= utt.getDepTree().getTreeTopDownFeatureForHead(head,isPOS);
                                        tree=(tree.contains("("))?"|BT| "+ tree+ " |ET|":"|BT| |ET|";
                                        //String treeBUp=utt.getDepTree().getTreeBottomUpFeatureForHead(word,"");
                                        //treeBUp=(treeBUp.contains("("))?" |BT| ("+ treeBUp + ") |ET|":"";
                                        tree=tree.trim(); //+ treeBUp.trim();    
                                        //outFile.append(label+" "+headSegment +"\t"+tree.trim()+" "+vector+"\n");
                                        //outFile.append(label+"\t"+bow.trim()+" "+tree.trim()+" "+vector.trim()+"\n");
                                        outFile.append(label+"\t"+" "+tree.trim()+" "+vector.trim()+"\n");
                                    }   
                                    totalOfspans++;
                                    
                                }  

                            }
                            //all leaves
                            List<Word> leaves = utt.getDepTree().getLeaves();
                            List<Segment> leafSegs = new ArrayList<>();
                            for(Word leaf:leaves){
                                List<Word> unique= new ArrayList<>();
                                unique.add(leaf);
                                leafSegs.add(new Segment(unique));
                                //Segment leafSeg=utt.getSegment().subSegment(leaf.getPosition(), leaf.getPosition());
                                                               
                                //if(istrain && !utt.isEntitySpan(leafSeg))
                                //    continue;
                                    
                                int posid=dictFeatures.get(leaf.getPosTag().getName());
                                int wsid= dictFeatures.get(leaf.getLexicalUnit().getPattern());
                                String vector=posid+":1 "+wsid+":1";
                                if(posid>wsid)
                                    vector=wsid+":1 "+posid+":1";
                                                                   
                                String tree="|BT| |ET|";
                                //outFile.append(leaf.getLabel()+"\t"+ leaf.getContent()+" "+tree.trim()+" "+vector.trim()+"\n");
                                outFile.append(leaf.getLabel()+"\t"+ tree.trim()+" "+vector.trim()+"\n");
                                    
                            }
                            neSegList2.retainAll(headSegList);
                            neSegList.removeAll(neSegList2); 
                            neSegList.removeAll(leafSegs);
                            if(!neSegList.isEmpty()){
                                ErrorsReporting.report("the following annotated entity-spans are not covered by any head "+neSegList.toString());
                            }     

                    }
                     if(istrain && uttCounter> TRAINSIZE){
                        break;
                    }                 
                }
                outFile.flush();
                outFile.close();
                inFile.close();
                if(istrain)
                    serializingFeatures();
                ErrorsReporting.report("groups saved in groups.*.tab || utterances:"+uttCounter+" spans: "+totalOfspans);
            } catch (IOException e) {
                    e.printStackTrace();
            }
    }   
    
    public void chunking(boolean istrain){
            try {

                GraphIO gio = new GraphIO(null);
                OutputStreamWriter outFile =null;
                
                String xmllist=LISTTRAINFILES;
                String fname=INCHUNKER.replace("%S", "train");    
                if(!istrain){
                    xmllist=LISTTESTFILES;
                    fname=INCHUNKER.replace("%S", "train");    
                     
                }
                outFile = new OutputStreamWriter(new FileOutputStream(fname),UTF8_ENCODING);
                BufferedReader inFile = new BufferedReader(new FileReader(xmllist));
                
                for (;;) {
                    String filename = inFile.readLine();
                    if (filename==null) break;
                    List<DetGraph> gs = gio.loadAllGraphs(filename);
                   
                    for (int i=0;i<gs.size();i++) {
                        DetGraph group = gs.get(i);

                        for (int j=0;j<group.getNbMots();j++) {
                            String outStr=group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\n";
                            outFile.append(outStr);
                        }
                        outFile.append("\n");
                    }        
                }
                outFile.flush();
                outFile.close();
                inFile.close();                
            }catch(Exception ex){
                
            }      
        
    }
    
     public void parsing(boolean bltrain, boolean isftb){
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
                    String path="parse/";
                    if(isftb)
                        path="parse/FTB/";
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
                    POStagger.setFrenchModels();
                    String model="mate.mods.ETB";
                    if(isftb)
                        model="mate.mods.FTBfull";
                    
                    MateParser.setMods(model); 
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
 
    public void evaluationSVMLightRESULTS(String testFileName, String outputFileName){
        BufferedReader testFile=null, svmOutput = null;
        try {
            //"analysis/SVM/groups.pn.tab.treek.test"
            //"analysis/SVM/outmodel200000testset.txt"
            List<Double> instScores= new ArrayList<>();
            testFile = new BufferedReader(new InputStreamReader(new FileInputStream(testFileName), UTF8_ENCODING));
            svmOutput = new BufferedReader(new InputStreamReader(new FileInputStream(outputFileName), UTF8_ENCODING));
            int tp=0, tn=0, fp=0, fn=0;
            for(;;){

                String line = testFile.readLine();   
                String result = svmOutput.readLine();
                
                if(line== null)
                    break;                
                
                String values[] = line.split("\\t");
                String res[] = result.split("\\t");
                int label = Integer.parseInt(values[0]);
                double recognizedLabel = Double.parseDouble(res[0]);
                instScores.add(recognizedLabel);
                int ok=1, nok=-1;
                
                if(recognizedLabel>0 && label==ok)
                    tp++;
                
                if(recognizedLabel>0 && label==nok)
                    fp++;
                
                if(recognizedLabel<0 &&label==ok)
                    fn++;
                if(recognizedLabel<0 &&label==nok)
                    tn++;

            }
            double precision= (double) tp/(tp+fp);
            double recall= (double) tp/(tp+fn);
            double f1=(2*precision*recall)/(precision+recall);
            
            System.out.println("  PN precision: "+precision);
            System.out.println("  PN recall: "+recall);
            System.out.println("  PN f1: "+f1);
            
            double[] scores = new double[instScores.size()];
            for(int i=0;i<instScores.size();i++){
                scores[i]=instScores.get(i);
            }
            Histoplot.showit(scores, instScores.size());
            
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                testFile.close();
                svmOutput.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
       
       
   } 
    
    public void addStanfordLCFeatures(boolean istrain){
        AnalyzeLClassifier analyzing = new AnalyzeLClassifier();
        stdictTrainFeatures=analyzing.deserializingFeatures(istrain);
        
    }
    
    private void serializingFeatures(){
    try{
            FileOutputStream fileOut =
            new FileOutputStream("svmfeaturesDict.ser");
            ObjectOutputStream out =
                            new ObjectOutputStream(fileOut);
            out.writeObject(dictFeatures);
            out.close();
            fileOut.close();
        }catch(Exception i)
        {
            i.printStackTrace();
        }
    }
    
    public void deserializingFeatures(){
      try
      {
        FileInputStream fileIn =  new FileInputStream("svmfeaturesDict.ser");
        ObjectInputStream in = new ObjectInputStream(fileIn);
        dictFeatures = (HashMap<String,Integer>) in.readObject();
        System.out.println("vocabulary of features: "+dictFeatures.size());
        
        
        IntegerValueComparator bvc = new IntegerValueComparator(dictFeatures);
        TreeMap<String, Integer> sortedSyn = new TreeMap<>(bvc);
        sortedSyn.putAll(dictFeatures);
        
        for(String key:sortedSyn.keySet()){
            System.out.println("feature id "+ sortedSyn.get(key)+" value : "+ key);

        }
              
         in.close();
         fileIn.close();
      }catch(IOException i)
      {
         i.printStackTrace();
         return;
      }catch(ClassNotFoundException c)
      {
         System.out.println("class not found");
         c.printStackTrace();
         return;
      } 
   
    }    
 
    public void savingAllSpansFiles(String strclass, boolean isvector, boolean isPOS){
        saveFilesForClassifierAllSpans(strclass, true, isvector, isPOS);
        saveFilesForClassifierAllSpans(strclass, false, isvector, isPOS);
    }    
    
    public void savingSpansFiles(String strclass, boolean isvector, boolean isPOS){
        saveFilesForLClassifierSpans(strclass, isvector,isPOS);
        saveFilesForClassifierAllSpans(strclass, false, isvector,isPOS);
    }
     
    public void savingWordsFiles(String strclass, boolean isvector, boolean isPOS){
        saveFilesForLClassifierWords(strclass,true, isvector,isPOS);
        saveFilesForLClassifierWords(strclass,false, isvector,isPOS);
    }    
    public void savingWordsPrTrFiles(String strclass, boolean isvector, boolean isCW, boolean isTopDown, boolean isBottomUp, boolean isPOS){
        saveFilesForLClassifierWordsPruningTrees(strclass,true, isvector, isCW, isTopDown, isBottomUp, isPOS);
        saveFilesForLClassifierWordsPruningTrees(strclass,false, isvector, isCW, isTopDown, isBottomUp, isPOS);
    }
    public void savingWordsPrTrConll(String strclass, boolean isvector, boolean isCW, boolean isTopDown, boolean isBottomUp, boolean isPOS){
        saveConllForLClassifierWordsPruningTrees(strclass,true, isvector, isCW, isTopDown, isBottomUp, isPOS);
        saveConllForLClassifierWordsPruningTrees(strclass,false, isvector, isCW, isTopDown, isBottomUp, isPOS);
    }    
    public void savingWordsPolyFiles(String strclass){
        saveFilesForPolyClassifierWords(strclass,true);
        saveFilesForPolyClassifierWords(strclass,false);
    }
    public static void main(String args[]){
         AnalyzeSVMClassifier svmclass= new AnalyzeSVMClassifier();
         //train
         //svmclass.parsing(false,true);
         //test
         //svmclass.parsing(true,true);
         //svmclass.saveFilesForLClassifierWords(CNConstants.PRNOUN, true,false);
         //testset
         //svmclass.saveFilesForLClassifierWords(CNConstants.PRNOUN, false,false);
         //spans
         //train & test
         //entity class / vector feature
         // svmclass.savingWordsFiles(CNConstants.PRNOUN, false);
         //svmclass.savingWordsFiles(CNConstants.PRNOUN, false);
         //svmclass.evaluationSVMLightRESULTS("analysis/SVM/groups.pn.tab.treek.test", "analysis/SVM/outputPrunedtkJul282014");
         //svmclass.savingSpansFiles(CNConstants.PRNOUN, false);
         //svmclass.savingAllSpansFiles(CNConstants.PRNOUN, false);
         //Chunking
         //svmclass.chunking(true);
         //svmclass.deserializingFeatures();
         //svmclass.savingWordsFiles(CNConstants.PRNOUN, false);
         //Pruned trees
         //classifier type, isvector, isCW, isTopDown, isBottomUp, isPOS
         svmclass.savingWordsPrTrConll(CNConstants.PRNOUN, false,true,true,true,false);
         //svmclass.savingWordsPrTrFiles(CNConstants.PRNOUN, false,true,true,true,false);
         //trees as string features for polynomial kernels
         //svmclass.savingWordsPolyFiles(CNConstants.PRNOUN);
         
         
     }
      
}
