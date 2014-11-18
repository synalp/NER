/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import conll03.CoNLL03Ner;
import edu.stanford.nlp.classify.LinearClassifier;
import gmm.GMMD1Diag;
import gmm.GMMDiag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import linearclassifier.AnalyzeLClassifier;
import static linearclassifier.AnalyzeLClassifier.TESTFILE;
import linearclassifier.Margin;
import tools.CNConstants;
import tools.Histoplot;

/**
 *
 * @author synalp
 */
public class TestingGMM {

        public static void TestingGMMWithGeneratedData(){
            AnalyzeLClassifier analyzing = new AnalyzeLClassifier();
            //final float[] priors = computePriors(sclassifier,model);
            List<List<Integer>> featsperInst = new ArrayList<>(); 
            List<Integer> labelperInst = new ArrayList<>();        
            //final float[] priors = {0.9f,0.1f};
            String sclass=CNConstants.PRNOUN;
            ///*
            HashMap<String,Double> priorsMap = new HashMap<>();
            
            priorsMap.put("NO", new Double(0.9));
            priorsMap.put(sclass, new Double(0.1));
            analyzing.setPriors(priorsMap); 
            AnalyzeLClassifier.PROPERTIES_FILE="etc/slinearclassifierORIG.props";
     
            analyzing.trainAllLinearClassifier(sclass,true, false, false);
            //analyzing.testingClassifier(true, sclass, false, false);
            LinearClassifier model = analyzing.getModel(sclass);
            analyzing.getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
            Margin margin = analyzing.getMargin(sclass);
            margin.setFeaturesPerInstance(featsperInst);
            margin.setLabelPerInstance(labelperInst);
            AnalyzeLClassifier.CURRENTPARENTMARGIN=margin;
                    
            float[] priors=AnalyzeLClassifier.getPriors();            
           
           
            //*/

            
            int numinst=50;
            margin.setNumberOfInstances(numinst);
            
            Margin.GENERATEDDATA=true;
            margin.generateBinaryRandomScore(numinst);
            /*
            System.out.println("x=[");
            for(int i=0;i<numinst;i++){
                System.out.println(margin.getGenScore(i, 0)+","+margin.getGenScore(i, 1)+";");
            }*/
            System.out.println("]");
            System.out.println("******  ONE DIMENSIONAL GMM ********");
            // get scores
            GMMD1Diag gmm = new GMMD1Diag(2, priors);
            gmm.train(margin);
            System.out.println("mean=[ "+gmm.getMean(0)+" , "+-gmm.getMean(0)+";\n"+
            +gmm.getMean(1)+" , "+-gmm.getMean(1)+"]");
            System.out.println("var=[ "+gmm.getVar(0)+" , "+gmm.getVar(0)+";\n"+
            +gmm.getVar(1)+" , "+gmm.getVar(1));
            System.out.println("GMM trained");      
            System.out.println("******  MULTIDIMENSIONAL GMM ********");
            GMMDiag gmmMD = new GMMDiag(2, priors);
            gmmMD.train(margin);
            System.out.println("mean=[ "+gmmMD.getMean(0,0)+" , "+gmmMD.getMean(0,1)+";\n"+
            +gmmMD.getMean(1,0)+" , "+gmmMD.getMean(1,1)+"]");
            System.out.println("var=[ "+gmmMD.getVar(0,1,1)+" , "+gmmMD.getVar(0,1,1)+";\n"+
            +gmmMD.getVar(1,0,0)+" , "+gmmMD.getVar(1,1,1));
            System.out.println("GMM trained");
    }
 
        public static void TestingGMMWithClassifierWeights(){
            AnalyzeLClassifier analyzing = new AnalyzeLClassifier();
            //final float[] priors = computePriors(sclassifier,model);
            List<List<Integer>> featsperInst = new ArrayList<>(); 
            List<Integer> labelperInst = new ArrayList<>();        
            HashMap<String,Double> priorsMap = new HashMap<>();
            String sclass=CNConstants.PRNOUN;
            priorsMap.put("NO", new Double(0.9));
            priorsMap.put(sclass, new Double(0.1));
            analyzing.setPriors(priorsMap); 
            
            AnalyzeLClassifier.PROPERTIES_FILE="etc/slinearclassifierORIG.props";

            analyzing.trainOneNERClassifier(sclass,false);
            
            LinearClassifier model = analyzing.getModel(sclass);
            analyzing.getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
            Margin margin = analyzing.getMargin(sclass);
            margin.setFeaturesPerInstance(featsperInst);
            margin.setLabelPerInstance(labelperInst);
            AnalyzeLClassifier.CURRENTPARENTMARGIN=margin;
            int numinst=labelperInst.size();
            margin.setNumberOfInstances(numinst);
            float[] priors=AnalyzeLClassifier.getPriors();
            System.out.println(Arrays.toString(priors));
            double[] scores= new double[featsperInst.size()];
            Arrays.fill(scores, 0.0);
            Histoplot.showit(margin.getScoreForAllInstancesLabel0(featsperInst,scores), featsperInst.size());
            //Histoplot.showit(margin.getScoreForAllInstancesLabel1(featsperInst,scores), featsperInst.size());
            /*
            System.out.println("x=[");
            for(int i=0;i<numinst;i++){
                System.out.println(margin.getGenScore(i, 0)+","+margin.getGenScore(i, 1)+";");
            }*/
            System.out.println("]");
            System.out.println("******  ONE DIMENSIONAL GMM ********");
            // get scores
            GMMD1Diag gmm = new GMMD1Diag(2, priors);
            gmm.train(margin);
            System.out.println("mean=[ "+gmm.getMean(0)+" , "+-gmm.getMean(0)+";\n"+
            +gmm.getMean(1)+" , "+-gmm.getMean(1)+"]");
            System.out.println("var=[ "+gmm.getVar(0)+" , "+gmm.getVar(0)+";\n"+
            +gmm.getVar(1)+" , "+gmm.getVar(1));
            System.out.println("GMM trained");      
            System.out.println("******  MULTIDIMENSIONAL GMM ********");
            GMMDiag gmmMD = new GMMDiag(2, priors);
            gmmMD.train(margin);
            System.out.println("mean=[ "+gmmMD.getMean(0,0)+" , "+gmmMD.getMean(0,1)+";\n"+
            +gmmMD.getMean(1,0)+" , "+gmmMD.getMean(1,1)+"]");
            System.out.println("var=[ "+gmmMD.getVar(0,1,1)+" , "+gmmMD.getVar(0,1,1)+";\n"+
            +gmmMD.getVar(1,0,0)+" , "+gmmMD.getVar(1,1,1));
            System.out.println("GMM trained");
            
            
    }
         public static void TestingGMMCoNLLData(){
            AnalyzeLClassifier analyzing = new AnalyzeLClassifier();
            //final float[] priors = computePriors(sclassifier,model);
            List<List<Integer>> featsperInst = new ArrayList<>(); 
            List<Integer> labelperInst = new ArrayList<>();        
            HashMap<String,Double> priorsMap = new HashMap<>();
            String sclass=CNConstants.PRNOUN;
            priorsMap.put("O", new Double(0.8));
            priorsMap.put(sclass, new Double(0.2));
            analyzing.setPriors(priorsMap); 
            
            AnalyzeLClassifier.TRAINFILE=CoNLL03Ner.TRAINFILE.replace("%S", sclass).replace("%CLASS", "LC");
            AnalyzeLClassifier.TESTFILE=CoNLL03Ner.TESTFILE.replace("%S", sclass).replace("%CLASS", "LC");
            AnalyzeLClassifier.MODELFILE=CoNLL03Ner.WKSUPMODEL.replace("%S", sclass);

            
            analyzing.trainAllLinearClassifier(sclass, false, false, false);
            LinearClassifier model = analyzing.getModel(sclass);
            analyzing.getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
            Margin margin = analyzing.getMargin(sclass);
            margin.setFeaturesPerInstance(featsperInst);
            margin.setLabelPerInstance(labelperInst);
            AnalyzeLClassifier.CURRENTPARENTMARGIN=margin;
            int numinst=labelperInst.size();
            margin.setNumberOfInstances(numinst);
            float[] priors=AnalyzeLClassifier.getPriors();
            System.out.println(Arrays.toString(priors));
            double[] scores= new double[featsperInst.size()];
            Arrays.fill(scores, 0.0);
            Histoplot.showit(margin.getScoreForAllInstancesLabel0(featsperInst,scores), featsperInst.size());
            //Histoplot.showit(margin.getScoreForAllInstancesLabel1(featsperInst,scores), featsperInst.size());
            /*
            System.out.println("x=[");
            for(int i=0;i<numinst;i++){
                System.out.println(margin.getGenScore(i, 0)+","+margin.getGenScore(i, 1)+";");
            }*/
            System.out.println("]");
            System.out.println("******  ONE DIMENSIONAL GMM ********");
            // get scores
            GMMD1Diag gmm = new GMMD1Diag(priors.length, priors);
            gmm.train(margin);
            System.out.println("mean=[ "+gmm.getMean(0)+" , "+-gmm.getMean(0)+";\n"+
            +gmm.getMean(1)+" , "+-gmm.getMean(1)+"]");
            System.out.println("var=[ "+gmm.getVar(0)+" , "+gmm.getVar(0)+";\n"+
            +gmm.getVar(1)+" , "+gmm.getVar(1));
            System.out.println("GMM trained");      
            System.out.println("******  MULTIDIMENSIONAL GMM ********");
            GMMDiag gmmMD = new GMMDiag(priors.length, priors);
            gmmMD.train(margin);
            System.out.println("mean=[ "+gmmMD.getMean(0,0)+" , "+gmmMD.getMean(0,1)+";\n"+
            +gmmMD.getMean(1,0)+" , "+gmmMD.getMean(1,1)+"]");
            System.out.println("var=[ "+gmmMD.getVar(0,1,1)+" , "+gmmMD.getVar(0,1,1)+";\n"+
            +gmmMD.getVar(1,0,0)+" , "+gmmMD.getVar(1,1,1));
            System.out.println("GMM trained");
            
            
    }       
        
    public static void main(String[] args){
        //TestingGMM.TestingGMMWithGeneratedData();
        //TestingGMM.TestingGMMWithClassifierWeights();
        TestingGMM.TestingGMMCoNLLData();
    }
}
