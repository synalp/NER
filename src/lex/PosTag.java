/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package lex;



/**
 * This class mainly stores all the possible postags that could be present
 * in a sentence, this class has been created for loading
 * the values from the database. This corresponds to the basic table:
 * postag, which defines the same feature in the vector space
 * that will be used by the semantic role labelling classifier.
 *
 * @author rojasbar
* <!--
* Modifications History
* Date      	  Author   	Description
* Apr 9, 2010     rojasbar       Creation-
*/

public class PosTag implements Cloneable{

 
    private Long    id;
    private String  name;
    
    public static String NOM_POS="NOM";
    public static String VER_POS="VER";
    public static String ADV_POS="ADV";
    public static String ADJ_POS="ADJ";

    public PosTag(){
        
    }

    public PosTag(Long id, String tag){
        this.id = id;
        this.name = tag;
    }


    public PosTag(String tag){
        this.name = tag;
    }

    public String getName(){
        return this.name;
    }

    public String getFName(){
        return "|"+this.name+"|";
    }    
    
    @Override
    public Object clone() {
        try {
            return (PosTag)super.clone();
        } catch (CloneNotSupportedException ex) {
            return this;
        }
        

    }      
}
