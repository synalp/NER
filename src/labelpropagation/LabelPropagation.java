/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package labelpropagation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import linearclassifier.AnalyzeLClassifier;
import tools.CNConstants;

/**
 * This class interfaces with the JUNTO Label propagation software, it prepares the input and evaluate the output file
 * @author rojasbar
 */
public class LabelPropagation {
    
    private AnalyzeLClassifier analiz = new AnalyzeLClassifier();
    
    public void generatingLabelPropGraph(){
        
        DecimalFormat decFormat = new DecimalFormat("#.##");
        HashMap<String,Integer> dictFeatures=new HashMap<>();
        HashMap<String,Integer> dictWords=new HashMap<>();
        
        String[][] stfeats=analiz.deserializingFeatures(false);
        HashMap<Integer,String> wordsperInst=new HashMap<>();
        HashMap<String,List<Double>> vectorfeats=new HashMap<>();
        HashMap<String,String> seedTrain=new HashMap<>();
        HashMap<String,String> goldTest=new HashMap<>();
        //HashMap<Integer,TreeMap<Integer,Double>> relatednodes= new HashMap<>();
        
       
        BufferedReader train = null, test = null;
        OutputStreamWriter outInput=null;
        OutputStreamWriter outGold=null;
        OutputStreamWriter outSeed=null;
        try {
         
            train = new BufferedReader(new FileReader(analiz.TRAINFILE.replace("%S",CNConstants.PRNOUN)));
            test = new BufferedReader(new FileReader(analiz.TESTFILE.replace("%S",CNConstants.PRNOUN))); 
            outInput = new OutputStreamWriter(new FileOutputStream("lprop/input_graph_pos3"),CNConstants.UTF8_ENCODING);
            outGold = new OutputStreamWriter(new FileOutputStream("lprop/gold_labels_pos3"),CNConstants.UTF8_ENCODING);
            outSeed = new OutputStreamWriter(new FileOutputStream("lprop/seeds_pos3"),CNConstants.UTF8_ENCODING);
        
        int linecounter=1;
        
        for(;;){
            String seedLine= train.readLine();
            if (seedLine==null) break;
                String[] stdata=seedLine.split("\t");
                if(!dictWords.containsKey(stdata[1]))
                    dictWords.put(stdata[1],dictWords.size()+1);

                seedTrain.put(stdata[1],stdata[0]);           
        }
        
        for (;;) {
            
            String line = test.readLine();
            if (line==null) break;
            String[] stdata=line.split("\t");
            String categ= stdata[0];
            String wordt= stdata[1];
            String pos= stdata[2];    
            
            if(!dictWords.containsKey(wordt))
                    dictWords.put(wordt,dictWords.size()+1);

            if(!dictFeatures.containsKey(pos))
                dictFeatures.put(pos,dictFeatures.size()+1);


           String[] restfeats= stfeats[linecounter];
            String wshape="";
            String lngram="";
            for(String feat:restfeats){
                if(feat.contains("SHAPE"))
                    wshape="["+feat.substring(feat.indexOf("SHAPE")+5)+"]";
                if(feat.contains("1-#"))
                    lngram+="["+feat.substring(feat.indexOf("1-#")+5)+"] ";

            }                              

            if(!dictFeatures.containsKey(wshape.trim()))
                dictFeatures.put(wshape.trim(),dictFeatures.size()+1);
            if(!dictFeatures.containsKey(lngram.trim()))
                dictFeatures.put(lngram.trim(),dictFeatures.size()+1);

            wordsperInst.put(linecounter,wordt);
            goldTest.put(wordt, categ);
            List<Double> numFeats=new ArrayList<>();
            numFeats.add(Double.parseDouble(dictFeatures.get(pos).toString()));
            //numFeats.add(Double.parseDouble(dictFeatures.get(wshape.trim()).toString()));
            //numFeats.add(Double.parseDouble(dictFeatures.get(lngram.trim()).toString()));
            vectorfeats.put(wordt,numFeats);

            linecounter++;

        }
        serializingWords(dictWords);
        System.out.println("TOTALOFLINES # "+linecounter);
        System.out.println("TOTALOFNODES(words) # "+vectorfeats.size());
        double[][] feats= new double[linecounter][1];

        
        for(String key:vectorfeats.keySet()){
            List<Double> vector = vectorfeats.get(key);

            for(int k=0;k<vector.size();k++)
                feats[dictWords.get(key)][k]=vector.get(k);

          
        }
        
///*        
        //double[][] relallnodes=new double[linecounter][1];
        Long before=System.currentTimeMillis();
	System.out.println("Before computing all x all distance "+before);
        //Euclidean Distance of one word with the rest
        //EuclideanDistance eDist = new EuclideanDistance();
        int outercounter=0;
        for(String key1:vectorfeats.keySet()){
            if(key1.equals("Fabrice"))
                System.out.println("entro : "+ vectorfeats.get("Fabrice"));
            
            TreeMap<Integer,Double> lowDist=new TreeMap<>();
            TreeMap<Integer,Double> topDist=new TreeMap<>();
            //relatednodes.put(i,relsni);
            for(String  key2:vectorfeats.keySet()){
                
                if(key1.equals(key2))
                    continue;
                
                //TreeMap<Integer,Double> relsnj= new TreeMap<>();                
                //double dist = eDist.compute(feats[dictWords.get(key1)], feats[dictWords.get(key2)]);
                
                double dist=0.0;
                //double[] w= {0.4,0.4,0.2};
                for(int fs1=0;fs1<feats[dictWords.get(key1)].length;fs1++){
                    if(feats[dictWords.get(key1)][fs1]==feats[dictWords.get(key2)][fs1])
                        dist+=1.0;//*w[fs1];
                    else
                        dist+=4.0;//*w[fs1];
                    
                }
                if(dist==1.0)
                    lowDist.put(dictWords.get(key2),dist);
                else
                    topDist.put(dictWords.get(key2),dist);
                //outInput.append("N"+dictWords.get(key1)+"\tN"+dictWords.get(key2)+"\t"+decFormat.format(dist)+"\n");
                //outInput.flush();                  

            }
            //DoubleValueComparator bvc = new DoubleValueComparator(nodesrel);
            //SortedSet<Map.Entry<Integer, Double>> sortedDist = DoubleValueComparator.entriesSortedByValues(nodesrel);
            //TreeMap<Integer, Double> sortedDist = new TreeMap<>(bvc);
            //sortedDist.putAll(nodesrel);
            
         
            
            
            int totalDist=lowDist.size()+topDist.size();
            double propL1= (double) lowDist.size()/totalDist;
            double propL2= 1-propL1;
            double totalL1=1000*propL2;
            double totalL2=1000*propL1;
            int counter =0;
            for(Integer keys:lowDist.keySet()){
                
                outInput.append("N"+dictWords.get(key1)+"\tN"+keys+"\t"+decFormat.format(lowDist.get(keys).doubleValue())+"\n");
                outInput.flush();  
                                
                if(counter>=totalL1)
                    break;
                counter++;
            }    
            counter =0;
            for(Integer keys:topDist.keySet()){
                
                outInput.append("N"+dictWords.get(key1)+"\tN"+keys+"\t"+decFormat.format(topDist.get(keys).doubleValue())+"\n");
                outInput.flush();  
                                
                if(counter>=totalL2)
                    break;
                counter++;
            }            
            //System.out.println("scanned node"+dictWords.get(key1)+"===WORD==="+key1);
            System.out.println("scanned outer counter"+outercounter);
            outercounter++;
        }  
//    System.out.println("after computing all x all distance "+System.currentTimeMillis());
//        //for(Integer key:relatednodes.keySet()){
//            TreeMap<Integer,Double> nodesrel=new TreeMap<>();
//            //for(int j=0;j<relallnodes.length;j++){
//            //    nodesrel.put(j,relallnodes[i][j]);
//            //}
//            
//            DoubleValueComparator bvc = new DoubleValueComparator(nodesrel);
//            TreeMap<Integer, Double> sortedDist = new TreeMap<>(bvc);
//            sortedDist.putAll(nodesrel);
//            
//            int counter =0;
//            for(Integer keys:sortedDist.keySet()){
//                if(sortedDist.get(key)==null)
//                    continue;
//                //relsn.put(key,sortedDist.get(key));
//                outInput.append("N"+key+"\tN"+keys+"\t"+decFormat.format(sortedDist.get(keys).doubleValue())+"\n");
//                outInput.flush();  
//                                
//                if(counter>=1000)
//                    break;
//                counter++;
//            }
//            //relatednodes.put(i,relsn);
//            System.out.println("scanned node"+key);
//        //}
        
        System.out.println("After computing all x all distance ");
        //nodes
//        for(Integer key:relatednodes.keySet()){
//            HashMap<Integer,Double> rels= relatednodes.get(key);
//            for(Integer key2:rels.keySet()){
//                outInput.append("N"+key+"\tN"+key2+"\t"+decFormat.format(rels.get(key2).doubleValue())+"\n");
//                outInput.flush();          
//            }  
//            System.out.println("scanned node"+key);
//        }
 //*/           
        //seed- words seen in the training data
        for(String key:goldTest.keySet()){
            
            String cat=seedTrain.get(key);
            if(cat!=null){
                int numcat=cat.equals(CNConstants.PRNOUN)?1:2;

                outSeed.append("N"+dictWords.get(key)+"\tL"+numcat+"\t1.0\n");
                outSeed.flush();
            }
            cat=goldTest.get(key);
            int numcat=cat.equals(CNConstants.PRNOUN)?1:2;
            outGold.append("N"+dictWords.get(key)+"\tL"+numcat+"\t1.0\n");
            outGold.flush();
        }
        //gold

        outSeed.close();
        outSeed.close();
        outGold.close();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                train.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }    
    
      public void evaluatingLabelProp(){
        HashMap<String,Integer> wordDict=deserializingWords();
        HashMap<Integer,String> wordStr=new HashMap<>();
        for(String key:wordDict.keySet()){
            //System.out.println("ID\t"+wordDict.get(key)+"\tWORD\t"+key);
            wordStr.put(wordDict.get(key), key);
        }
        HashMap<String,String> recLabels= new HashMap<>();
        
        BufferedReader testFile = null;
        BufferedReader lpFile=null, input=null;
        try {
            testFile = new BufferedReader(new FileReader(analiz.TESTFILE.replace("%S",CNConstants.PRNOUN))); //label_prop_output_100iters
            lpFile = new BufferedReader(new InputStreamReader(new FileInputStream("lprop/label_prop_output_catdist_pos3"), CNConstants.UTF8_ENCODING));
            input = new BufferedReader(new InputStreamReader(new FileInputStream("lprop/input_graph_pos3"), CNConstants.UTF8_ENCODING));
            
            //read the input just to inspect it was correct, and to check the vocabulary of words
            /*
            for(;;){
                              
               String inputline= input.readLine();
               
               if(inputline == null)
                   break;
               
               String[] graph= inputline.split("\\t");
               int node1=Integer.parseInt(graph[0].substring(1));
               int node2=Integer.parseInt(graph[1].substring(1));
               System.out.println(wordStr.get(node1)+"-"+ wordStr.get(node2)+"-"+graph[2]);                
            }
            */
            //read the label propagation output
            for(;;){
               String lpline = lpFile.readLine();
               if(lpline== null)
                    break; 
      
                String values[] = lpline.split("\\t");
                int wordid=Integer.parseInt(values[0].substring(1));
                String[] recvals= values[3].split(" ");
//                String[] labels= new String[3];
//                double[] vlabels= new double[3];
//                labels[0]=recvals[0];
//                vlabels[0]= Double.parseDouble(recvals[1]);
                String maxlabel="";
                double maxval=Integer.MIN_VALUE;
                for(int i=0; i< recvals.length;i+=2){
		    String lbl=recvals[i];
	            if(lbl.startsWith("__DUM"))
			continue;
                    double val = Double.parseDouble(recvals[i+1]);
                    if(val>maxval){
                        maxval=val;
                        maxlabel=recvals[i];
                    }    
                }
                System.out.println("ID\t"+wordid+"\tWORD\t"+wordStr.get(wordid)+"\tREC LABEL\t"+maxlabel);
                recLabels.put(wordStr.get(wordid), (maxlabel.equals("L1"))?CNConstants.PRNOUN:CNConstants.NOCLASS);              
                //System.out.println("label: " + maxlabel + "value " + maxval);
            }   
            int tp=0, tn=0, fp=0, fn=0;
            int tp0=0, tn0=0, fp0=0, fn0=0;
            for(;;){
                String line = testFile.readLine();   
                
                
                if(line== null)
                    break;    
                              
                
                String values[] = line.split("\\t");
                String label = values[0];
                String recognizedLabel = recLabels.get(values[1]);
                //System.out.println("ID\t"+wordDict.get(values[1])+"\tWORD\t"+values[1]+"\tREC LABEL\t"+recognizedLabel+"\tTRUE LABEL\t"+label);
                if(recognizedLabel.equals(CNConstants.PRNOUN) && label.equals(CNConstants.PRNOUN)){
		    //System.out.println("tp word: "+ values[1]+" "+recognizedLabel);	
                    tp++;tn0++;
		}
                
                if(recognizedLabel.equals(CNConstants.PRNOUN)&& label.equals(CNConstants.NOCLASS)){
                    fp++;fn0++;
                }
                if(recognizedLabel.equals(CNConstants.NOCLASS)&&label.equals(CNConstants.PRNOUN)){
                    fn++;fp0++;
                }    
                if(recognizedLabel.equals(CNConstants.NOCLASS)&&label.equals(CNConstants.NOCLASS)){
                    tn++;tp0++;
                }    

            }
            double precision= (double) tp/(tp+fp);
            double recall= (double) tp/(tp+fn);
            double f1=(2*precision*recall)/(precision+recall);
            double accuracy=(double) (tp+tn)/(tp+tn+fp+fn);
            
            System.out.println("confusion matrix:\n ["+ tp+","+fp+"\n"+fn+","+tn+"]");
            System.out.println("confusion matrix:\n ["+ tp0+","+fp0+"\n"+fn0+","+tn0+"]");
            System.out.println("  PN precision: "+precision);
            System.out.println("  PN recall: "+recall);
            System.out.println("  PN f1: "+f1);
            System.out.println("  Accuracy: "+accuracy);
            
            
            precision= (double) tp0/(tp0+fp0);
            recall= (double) tp0/(tp0+fn0);
            f1=(2*precision*recall)/(precision+recall);
            accuracy=(double) (tp0+tn0)/(tp0+tn0+fp0+fn0);
            
            System.out.println(" NO precision: "+precision);
            System.out.println(" NO recall: "+recall);
            System.out.println(" NO f1: "+f1);  
            
            System.out.println("GENERAL ACCURACY "+ accuracy);
            
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

    private void serializingFeatures(HashMap vocFeats, boolean isTrain){
    try{
            String fileName="";
            if(isTrain)
                fileName="StanfordLCTrainfeaturesDict.ser";
            else
                fileName="StanfordLCTestfeaturesDict.ser";
            FileOutputStream fileOut =
            new FileOutputStream(fileName);
            ObjectOutputStream out =
                            new ObjectOutputStream(fileOut);
            out.writeObject(vocFeats);
            out.close();
            fileOut.close();
        }catch(Exception i)
        {
            i.printStackTrace();
        }
    }      
      
    private void serializingWords(HashMap vocFeats){
    try{
            String fileName="WordDict.ser";
            
            FileOutputStream fileOut = new FileOutputStream(fileName);
            ObjectOutputStream out =
                            new ObjectOutputStream(fileOut);
            out.writeObject(vocFeats);
            out.close();
            fileOut.close();
        }catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }
         
    
   public HashMap<String,Integer> deserializingWords(){
      try
      {
        HashMap<String,Integer> vocFeats = new HashMap<>();
        String fileName="WordDict.ser";
        FileInputStream fileIn=  new FileInputStream(fileName);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        vocFeats = (HashMap<String,Integer>) in.readObject();
        System.out.println("vocabulary of features: "+vocFeats.size());
        /*
        for(Integer key:vocFeats.keySet()){
            System.out.println("feature id "+ vocFeats.get(key)+" value : "+ key);

        }*/
              
         in.close();
         fileIn.close();
         return vocFeats;
      }catch(IOException i)
      {
         i.printStackTrace();
         return new HashMap<>();
      }catch(ClassNotFoundException c)
      {
         System.out.println("class not found");
         c.printStackTrace();
         return new HashMap<>();
      } 
   
    }     
   
    public static void main(String[] args){
        LabelPropagation lprop = new LabelPropagation();
        //lprop.generatingLabelPropGraph();
        lprop.evaluatingLabelProp();
    }
      
}
