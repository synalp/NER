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
import org.apache.commons.math3.distribution.MixtureMultivariateNormalDistribution;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.fitting.MultivariateNormalMixtureExpectationMaximization;
import org.apache.commons.math3.util.Pair;
import tools.CNConstants;
import tools.Histoplot;

/**
 *
 * @author synalp
 */
public class TestingMultiGMM {

        public TestingMultiGMM(){
        }
        
        public void TestingMulticlassGMM(){
            AnalyzeLClassifier analyzing = new AnalyzeLClassifier();
            analyzing.reInitializingEsterFiles();
            AnalyzeLClassifier.PROPERTIES_FILE="etc/slinearclassifierORIG.props";
            
            List<List<Integer>> featsperInst = new ArrayList<>(); 
            List<Integer> labelperInst = new ArrayList<>();        
            //final float[] priors = {0.2f,0.3f,0.4f,0.1f};
            String sclass=CNConstants.ALL;
            HashMap<String,Double> priorsMap = new HashMap<>();
            priorsMap.put("NO", new Double(0.9));
            priorsMap.put("pers", new Double(0.04));
            priorsMap.put("loc", new Double(0.03));
            priorsMap.put("org", new Double(0.03));
            
            
            analyzing.setPriors(priorsMap);             
            
            analyzing.trainAllLinearClassifier(sclass,true,false,false);
            
            LinearClassifier model = analyzing.getModel(sclass);
            //analyzing.testingClassifier(true, sclass, false,false);
            analyzing.getValues(TESTFILE.replace("%S", sclass),model,featsperInst,labelperInst);
            Margin margin = analyzing.getMargin(sclass);
            margin.setFeaturesPerInstance(featsperInst);
            margin.setLabelPerInstance(labelperInst);
            AnalyzeLClassifier.CURRENTPARENTMARGIN=margin;
            double[] scores= new double[featsperInst.size()];
            Arrays.fill(scores, 0.0);            
            Histoplot.showit(margin.getScoreForAllInstancesLabel0(featsperInst,scores), featsperInst.size());
            
            
            int numinst=labelperInst.size();
            float[] priors=AnalyzeLClassifier.getPriors();
            //Margin margin = new Margin();
            /*
            numinst=10;
            margin.setNumberOfInstances(numinst);
            
            Margin.GENERATEDDATA=true;
            margin.generateRandomScore(numinst,priors);
            */
            System.out.println("******  MULTIDIMENSIONAL GMM ********");
            GMMDiag gmmMD = new GMMDiag(priors.length, priors,false);
            gmmMD.train(margin);
            String mean="mean=[";
            for(int i=0; i<  gmmMD.getDimension();i++){
                for(int j=0; j<  gmmMD.getDimension();j++){
                    mean+= gmmMD.getMean(i,j);
                    if(j<gmmMD.getDimension()-1)
                        mean+=" , ";
                } 
                if(i<gmmMD.getDimension()-1)
                    mean+=";\n";
            }    
            mean+="]";
            System.out.println(mean);
            String var=" var=[";
            for(int i=0; i<  gmmMD.getDimension();i++){
                for(int j=0; j<  gmmMD.getDimension();j++){
                    var+= gmmMD.getVar(i,j,j);
                    if(j<gmmMD.getDimension()-1)
                        var+=" , ";                    
                }  
                if(i<gmmMD.getDimension()-1)
                    var+=";\n";
            }    
            var+="]";
            System.out.println(var);            
            
            System.out.println("GMM trained");            
            /*
            System.out.println("x=[");
            for(int i=0;i<numinst;i++){
                System.out.println(margin.getGenScore(i, 0)+","+margin.getGenScore(i, 1)+";");
            }*/
            System.out.println("]");
            /*
            MultivariateNormalMixtureExpectationMaximization mvarGauss = new MultivariateNormalMixtureExpectationMaximization(margin.getGenScore());
            MixtureMultivariateNormalDistribution initialMixture= MultivariateNormalMixtureExpectationMaximization.estimate(margin.getGenScore(), priors.length);
            mvarGauss.fit(initialMixture);
            MixtureMultivariateNormalDistribution mixture= mvarGauss.getFittedModel();
            List<Pair<Double,MultivariateNormalDistribution>> gaussians= mixture.getComponents();
            for(Pair pair:gaussians){
                Double prob= (Double) pair.getFirst();
                MultivariateNormalDistribution distr= (MultivariateNormalDistribution) pair.getSecond();
                System.out.println("means: " + distr.getMeans());
                System.out.println(" covariance : "+ distr.getCovariances().toString());
                System.out.println("sdev :"+ distr.getStandardDeviations());
            }
            //*/
    }
    
         public static void testingGMMCoNLLData(){
            AnalyzeLClassifier analyzing = new AnalyzeLClassifier();
            CoNLL03Ner conll = new CoNLL03Ner();
            //final float[] priors = computePriors(sclassifier,model);
            List<List<Integer>> featsperInst = new ArrayList<>(); 
            List<Integer> labelperInst = new ArrayList<>();        
            HashMap<String,Double> priorsMap = new HashMap<>();
            String sclass=CNConstants.ALL;
            priorsMap.put("O", new Double(0.76));
            priorsMap.put("PER", new Double(0.1)); 
            priorsMap.put(CNConstants.ORG.toUpperCase(), new Double(0.05)); 
            priorsMap.put(CNConstants.LOC.toUpperCase(), new Double(0.06)); 
            priorsMap.put("MISC", new Double(0.03)); 
            analyzing.setPriors(priorsMap); 
            
            AnalyzeLClassifier.TRAINFILE=CoNLL03Ner.TRAINFILE.replace("%S", sclass).replace("%CLASS", "LC");
            AnalyzeLClassifier.TESTFILE=CoNLL03Ner.TESTFILE.replace("%S", sclass).replace("%CLASS", "LC");
            AnalyzeLClassifier.MODELFILE=CoNLL03Ner.WKSUPMODEL.replace("%S", sclass);
            File file = new File(AnalyzeLClassifier.TRAINFILE);
            if(!file.exists())
                conll.generatingStanfordInputFiles(sclass, "train", false,CNConstants.CHAR_NULL,false);
            file = new File(AnalyzeLClassifier.TESTFILE);
            if(!file.exists())
                conll.generatingStanfordInputFiles(sclass, "test", false,CNConstants.CHAR_NULL,false);
            
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
        
    public static void main(String[] args){
        TestingMultiGMM test = new TestingMultiGMM();
        //test.TestingMulticlassGMM();
        TestingMultiGMM.testingGMMCoNLLData();
    }
}
