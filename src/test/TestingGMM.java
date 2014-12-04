/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import conll03.CoNLL03Ner;
import edu.stanford.nlp.classify.LinearClassifier;
import gmm.GMMD1Diag;
import gmm.GMMDiag;
import java.io.File;
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

        public static void TestingGMMWithGeneratedData() throws Exception {
            AnalyzeLClassifier analyzing = new AnalyzeLClassifier();
            analyzing.reInitializingEsterFiles();
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
            File file = new File(AnalyzeLClassifier.MODELFILE.replace("%s", sclass));
            file.delete();
            analyzing.trainAllLinearClassifier(sclass,false, false, false);
            //analyzing.testingClassifier(true, sclass, false, false);
            LinearClassifier model = analyzing.getModel(sclass);
            analyzing.getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
            Margin margin = analyzing.getMargin(sclass);
            margin.setFeaturesPerInstance(featsperInst);
            margin.setLabelPerInstance(labelperInst);
            AnalyzeLClassifier.CURRENTPARENTMARGIN=margin;
                    
//            float[] priors=AnalyzeLClassifier.getPriors();            
            // bug before: margin.lblIndex is empty, so the priors were not set. I prefer to set them directly, to simplify testing GMM:
            float[] priors = {0.8f,0.2f};
           
           
            /*
             * Runs training 20 times to check variability of the estimate
             */
            double[][] means = new double[20][2];
            for (int retry=0;retry<means.length;retry++) {
            
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
            // variability of the result is a concern...
            gmm.nitersGMMTraining=5000;
            gmm.train(margin);
            System.out.println("mean=[ "+gmm.getMean(0)+" , "+-gmm.getMean(0)+";\n"+
            +gmm.getMean(1)+" , "+-gmm.getMean(1)+"]");
            System.out.println("var=[ "+gmm.getVar(0)+" , "+gmm.getVar(0)+";\n"+
            +gmm.getVar(1)+" , "+gmm.getVar(1));
            System.out.println("GMM trained");      

            means[retry][0]=gmm.getMean(0);
            means[retry][1]=gmm.getMean(1);
            
            System.out.println("******  MULTIDIMENSIONAL GMM ********");
            GMMDiag gmmMD = new GMMDiag(2, priors,false);

            gmmMD.train(margin);
            System.out.println("mean=[ "+gmmMD.getMean(0,0)+" , "+gmmMD.getMean(0,1)+";\n"+
            +gmmMD.getMean(1,0)+" , "+gmmMD.getMean(1,1)+"]");
            System.out.println("var=[ "+gmmMD.getVar(0,1,1)+" , "+gmmMD.getVar(0,1,1)+";\n"+
            +gmmMD.getVar(1,0,0)+" , "+gmmMD.getVar(1,1,1));
            System.out.println("GMM trained");
            }

            double vv0,vv1,mm0,mm1;
            {
            	double s=0,ss=0;
            	for (int i=0;i<means.length;i++) {
            		s+=means[i][0];
            		ss+=means[i][0]*means[i][0];
            	}
            	mm0 = s/(double)means.length;
            	vv0 = ss/(double)means.length-mm0*mm0;
            }
            {
            	double s=0,ss=0;
            	for (int i=0;i<means.length;i++) {
            		s+=means[i][1];
            		ss+=means[i][1]*means[i][1];
            	}
            	mm1 = s/(double)means.length;
            	vv1= ss/(double)means.length-mm1*mm1;
            }

            System.out.println("variability "+vv0+" "+vv1);
            if (Math.abs(mm0-8)>2) throw new Exception("ERROR: trained mean is not good "+mm0);
            if (Math.abs(mm1-2)>2) throw new Exception("ERROR: trained mean is not good "+mm1);
            
            // avoid future problems: this static var is only used in this method (?), and I got exceptions when calling
            // a test just after this one because of this var...
            Margin.GENERATEDDATA=false;
    }
 
        public static void TestingGMMWithClassifierWeights(){
            AnalyzeLClassifier analyzing = new AnalyzeLClassifier();
            analyzing.reInitializingEsterFiles();
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
            //creates the testfile
            analyzing.saveGroups(sclass,false, false, false);            
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
            GMMDiag gmmMD = new GMMDiag(2, priors,false);
            gmmMD.train(margin);
            System.out.println("mean=[ "+gmmMD.getMean(0,0)+" , "+gmmMD.getMean(0,1)+";\n"+
            +gmmMD.getMean(1,0)+" , "+gmmMD.getMean(1,1)+"]");
            System.out.println("var=[ "+gmmMD.getVar(0,1,1)+" , "+gmmMD.getVar(0,1,1)+";\n"+
            +gmmMD.getVar(1,0,0)+" , "+gmmMD.getVar(1,1,1));
            System.out.println("GMM trained");
            
            
    }
         public static void TestingGMMCoNLLData(){
            AnalyzeLClassifier analyzing = new AnalyzeLClassifier();
            CoNLL03Ner conll = new CoNLL03Ner();
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
            File file = new File(AnalyzeLClassifier.TRAINFILE);
            if(!file.exists())
                conll.generatingStanfordInputFiles(sclass, "train", false,CNConstants.CHAR_NULL);
            file = new File(AnalyzeLClassifier.TESTFILE);
            if(!file.exists())
                conll.generatingStanfordInputFiles(sclass, "test", false,CNConstants.CHAR_NULL);
            
            analyzing.trainAllLinearClassifier(sclass, false, false, false);
            
            
            LinearClassifier model = analyzing.getModel(sclass);
            analyzing.getValues(TESTFILE,model,featsperInst,labelperInst);
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
            GMMDiag gmmMD = new GMMDiag(priors.length, priors,false);
            gmmMD.train(margin);
            System.out.println("mean=[ "+gmmMD.getMean(0,0)+" , "+gmmMD.getMean(0,1)+";\n"+
            +gmmMD.getMean(1,0)+" , "+gmmMD.getMean(1,1)+"]");
            System.out.println("var=[ "+gmmMD.getVar(0,1,1)+" , "+gmmMD.getVar(0,1,1)+";\n"+
            +gmmMD.getVar(1,0,0)+" , "+gmmMD.getVar(1,1,1));
            System.out.println("GMM trained");
            
            
    }       
         
    public static void testingSortingGMMEst(){
        AnalyzeLClassifier analyzing = new AnalyzeLClassifier();
        CoNLL03Ner conll = new CoNLL03Ner();
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
        File file = new File(AnalyzeLClassifier.TRAINFILE);
        if(!file.exists())
            conll.generatingStanfordInputFiles(sclass, "train", false,CNConstants.CHAR_NULL);
        file = new File(AnalyzeLClassifier.TESTFILE);
        if(!file.exists())
            conll.generatingStanfordInputFiles(sclass, "test", false,CNConstants.CHAR_NULL);

        analyzing.trainAllLinearClassifier(sclass, false, false, false);


        LinearClassifier model = analyzing.getModel(sclass);
        analyzing.getValues(TESTFILE,model,featsperInst,labelperInst);
        Margin margin = analyzing.getMargin(sclass);
        margin.setFeaturesPerInstance(featsperInst);
        margin.setLabelPerInstance(labelperInst);
        AnalyzeLClassifier.CURRENTPARENTMARGIN=margin;
        int numinst=labelperInst.size();
        margin.setNumberOfInstances(numinst);
        priorsMap.put(sclass, new Double(0.2));
        analyzing.setPriors(priorsMap); 
        float[] priors=AnalyzeLClassifier.getPriors();
        GMMDiag gmmMD = new GMMDiag(2, priors,false);
        gmmMD.trainEstimatedGaussians(margin);
    }     
        
    public static void main(String[] args){
        try {
            //TestingGMM.TestingGMMWithGeneratedData();
            //TestingGMM.TestingGMMCoNLLData();
            //TestingGMM.TestingGMMCoNLLData();
            TestingGMM.testingSortingGMMEst();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
