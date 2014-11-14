/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import conll03.CoNLL03Ner;
import linearclassifier.AnalyzeLClassifier;
import tools.CNConstants;

/**
 *
 * @author rojasbar
 */
public class TestingConllExperiments {
    
    public static void main(String[] args){
        if(args.length < 2){
            System.out.println("two arguments are expected : [w|gw] train_size (use all trainset= -1) test_size (use all testset = -1)");
        }
        int trainSize=Integer.MAX_VALUE;
        try{
            trainSize=Integer.parseInt(args[1]);
        }catch(Exception ex){
            System.out.println("The second argument is the size of a subset of the trainset");
        }
        int testSize=Integer.MAX_VALUE;
        if(args.length == 3){
            try{
                testSize=Integer.parseInt(args[2]);
            }catch(Exception ex){
               System.out.println("The third argument is the size of a subset of the testset"); 
            }    
        }
        CoNLL03Ner conll = new CoNLL03Ner();
        switch(args[0]){
            
            case "w":
                conll.testingNewWeightsLC(CNConstants.PRNOUN, true, (trainSize==-1)?Integer.MAX_VALUE:trainSize, (testSize==-1)?Integer.MAX_VALUE:testSize );
                break;
            case "gw":
                AnalyzeLClassifier.TRAINSIZE=Integer.parseInt(args[1]);
                conll.runningWeaklySupStanfordLC(CNConstants.PRNOUN, true, testSize,1000);
                break;                
        }
    }
}
