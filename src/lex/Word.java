package lex;




import java.io.Serializable;
import tools.CNConstants;



/**
 * This class has information about each word.
 *
 * @author rojasbar
 *
 * <!--
 * Modifications History
 * Date      	  Author   	Description
 * Mar 31, 2010   rojasbar  	The attributes category and lemma were added.
 * -->
 *
 */

public class Word  implements Cloneable, Serializable{


    private Integer position;

    private LexicalUnit lexicalUnit;
 
    private PosTag    postag;

    private Utterance utterance;
    
    private int label;
    

    public Word(){
        label=CNConstants.INT_NULL;
    }

    public Word(Integer position, String content){
        LexicalUnit lu= new LexicalUnit(content);
        this.position = position;
        this.lexicalUnit = lu;
        postag = new PosTag(CNConstants.CHAR_NULL);
        label=CNConstants.INT_NULL;
    }

 

    public void setUtterance(Utterance utter){
        this.utterance = utter;
    }

  
    public void setLexicalUnit(LexicalUnit lu){
        this.lexicalUnit = lu;
        
    }

    public void setPosTag(PosTag pos){
        this.postag = pos;
    }


    public void setPosition(Integer position){
        this.position = position;
    }

    public void setLabel(int label){
        this.label=label;
    }
    
    public Integer getPosition(){
        return this.position;
    }

    public String getContent(){
        try {
            return this.lexicalUnit.getName();
        } catch (Exception ex) {
            return this.lexicalUnit.getName();
        }

    }

    public String getLemmaContent(){
        try {
            return this.lexicalUnit.getLemma().getName();
        } catch (Exception ex) {
            return this.lexicalUnit.getLemma().getName();
        }

    }    
    
    public PosTag getPosTag(){
        return this.postag;
    }

    public Lemma getLemma(){
        return this.lexicalUnit.getLemma();
    }

    public LexicalUnit getLexicalUnit(){
        return this.lexicalUnit;
    }

   public Utterance getUtterance(){
       return this.utterance;
   }

   public String getCleanedWord(){
       //String word = this.lexicalUnit.getName();
       return  this.lexicalUnit.getCleanLexicalUnit();
       
   }
   
   public int getLabel(){
       return this.label;
   }


  public void setPOSTag(String pos, String lemma){
      PosTag category = new PosTag(pos);
      this.lexicalUnit.setLemma(lemma);
      this.postag = category;

      
  }

   //toString
    @Override
   public String toString(){
        return this.lexicalUnit.toString();
    }

   public static void main(String[] args) {
        
            Word word1 = new Word(1, "(ME)DIA");
            System.out.println("cleaned word1: " + word1.getCleanedWord());
            Word word2 = new Word(2, "*touristique");
            System.out.println("cleaned word2: " + word2.getCleanedWord());
            Word word3 = new Word(3, "'touristique");
            System.out.println("cleaned word3: " + word3.getCleanedWord());
        

   }

   public String cleanContent(){
      return  this.lexicalUnit.getCleanLexicalUnit();

   }
    @Override
    public Object clone() {
        try {
            Word word = (Word)super.clone();
            word.setLexicalUnit((LexicalUnit)lexicalUnit.clone());
            word.setPosTag((PosTag)postag.clone());
            return word;
        } catch (CloneNotSupportedException ex) {
            return this;
        }
        

    }     
}
