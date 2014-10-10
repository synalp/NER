/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tagger;




import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lex.Segment;
import lex.Utterance;
import lex.Word;
import org.annolab.tt4j.TokenHandler;
import org.annolab.tt4j.TreeTaggerWrapper;
import tools.CNConstants;


/**
 *
 * @author rojasbar
* <!--
* Modifications History
* Date      	  Author   	Description
* Sept 23, 2014   rojasbar       Creation-
*/
public class Tagger{
    
    private Utterance utterance;
    private TreeTaggerWrapper<String> treetagger;
    private int tokenPos;
    static String solvableString = "tag(Input,TAGS)";


    public Tagger() {

        //GeneralConfig genConf= new GeneralConfig();
        String taggerPath= System.getProperty("com.sun.aas.instanceRoot");
        if(taggerPath== null){
            taggerPath=CNConstants.DEFTAGGERDIR;
        }
        else
            taggerPath+= "/applications/WOZ/"+CNConstants.DEFTAGGERDIR;
        treetagger = new TreeTaggerWrapper<String>();
        tokenPos = 0;
        System.setProperty("treetagger.home", taggerPath);
        //System.out.println("properties: "+ System.getProperty("treetagger.home"));
         /*Logger.getLogger(Tagger.class.getName()).info(taggerPath);
         try {
            Runtime.getRuntime().exec("chmod -R 755 " + taggerPath);

            } catch (IOException ex) {
                Logger.getLogger(Tagger.class.getName()).log(Level.SEVERE, null, ex);
            }*/
        try {

            treetagger.setModel(taggerPath +CNConstants.TREETAGGERPAR);
            treetagger.setModel(taggerPath +CNConstants.TREETAGGERBIN);
          

        }catch (Exception e) {
            Logger.getLogger(Tagger.class.getName()).severe("ERROR:"+e.getMessage());
            e.printStackTrace();
	}
    }

    	public void computePOStags(final Segment segment)throws Exception {
		treetagger.setHandler(new TokenHandler<String>() {
			public void token(String token, String pos, String lemma) {
                        segment.getWord(tokenPos).setPOSTag(pos, lemma);
                        tokenPos++;
                   
			}
		});
		tokenPos=0;
		try {
                       List<String> lexUnits = new ArrayList<String>();
                       if(segment == null)
                          Logger.getLogger(Tagger.class.getName()).severe("ERROR: segment null");
			for(Word word:segment.getWords()){
                            if(word==null)
                                Logger.getLogger(Tagger.class.getName()).severe("ERROR: word null in the segment: "+segment.toString());
                            //lexUnits.add(word.getCleanedWord());
                            lexUnits.add(word.getContent());
                            
                        }

			treetagger.process(lexUnits);
		} catch (Exception e) {
                       //ErrorManager.setError(CNConstants.MSG_TREETAGGERERR, e.getMessage(), true);
                    Logger.getLogger(Tagger.class.getName()).severe("ERROR:"+e.getMessage());
                    e.printStackTrace();
                       
		}
	}


    
    public void destroy(){
        treetagger.destroy();
    }
    
    public static void clean(){
        
          try {
            
            Runtime.getRuntime().exec("killall -r \\'\\.*tagger\\.*\\'") ;

            } catch (IOException ex) {
                Logger.getLogger(Tagger.class.getName()).log(Level.SEVERE, null, ex);
            }       
    }

    public static void main(String[] args) {
        
        Tagger tagger = new Tagger();
        try{
            //String g1="G1",g2="G2";
            //int valor = Integer.parseInt(g1.substring(g1.length()-1)) - Integer.parseInt(g2.substring(g2.length()-1));
            
            
            //String realCont= "Conditioner le produit veut dire l'emballer soigneusement pour pouvoir l' expédier au client. Aldo, le logisticien assure qu'on ne manque pas de stock et de gérer les livraisons et les expéditions.";
            //String realCont= "Vous pouvez décorer notre contrôleur .";
            //String realCont= "tu livres un livre blue xx! vous pouvez décorer notre contrôleur.";
            //String realCont="Vous me croyez si je vous dis qu'on recycle les bouteilles d'eau en stylos ? Avec le bouchon en Polyéthylène, on peut faire le capuchon, et avec la bouteille, on fait le corps du stylo. Les bouteilles en plastique recyclées servent à faire plein de choses  de nouvelles bouteilles, des fibres textiles comme la polaire, de nouvelles pièces, et même des stylos  Justement, il y en a dans le bac de la machine, derrière moi. Servez-vous";
            String realCont="Je souhaiterais construire une manette pour professeur Geekman   et je ne sais pas vraiment par où commencer .";
            //String content = uttMgr.removingPunct(realCont);
            Utterance utterance = new Utterance(realCont);
            Utterance taggedUtterance = new Utterance(realCont);
            tagger.computePOStags(taggedUtterance.getSegment());
            tagger.destroy();
            Tagger.clean();
            for(Word word:taggedUtterance.getSegment().getWords()){
                //Word taggedWord = uttMgr.isWordInUtterance(taggedUtterance, word);
                System.out.println("Tagged Word lema:"+ word.getLemma().getName()+ " POS: "+ word.getPosTag().getName());
                System.out.println(" LEX UNIT: "+ word.getLexicalUnit().getName());
                //System.out.println(" LEMMA : "+ word.getLemma());
                //System.out.println(" POS :"+ word.getPosTag().getName());
            }

        }catch(Exception ex){

        }
    }
}
