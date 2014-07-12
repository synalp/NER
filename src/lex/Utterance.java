package lex;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.text.Normalizer;
import java.util.regex.Pattern;


/**
 * This class contains the utterance
 * it mirrors the utterance inside the XML input file
 * of the corpus
 * 
 * @author rojasbar
 * Nov 17 2009
 * <!--
 * Modifications History
 * Date           Author   	Description
 * Nov 17, 2009   rojasbar  	Creation
 * -->
 */

public class Utterance  implements Cloneable{


    private Long id;

    //corpusid
    private String corpusid;
    //Parser dependency tree
    private DependencyTree depTree;


    private List<Word> words;


    //stores gold ner spans
    private HashMap<Segment, String> goldEntities= new HashMap<>();
   
    public Utterance(){
       words = new ArrayList<>();
       this.depTree = new DependencyTree();
    }
    


    public Utterance(String content){
        this.depTree = new DependencyTree();
        words= new ArrayList<>();
        
        
        content=content.replaceAll("\\b\\s{2,}\\b", " ");
        content=content.replaceAll("'", "' ");
        content=content.replaceAll(",", " , ");
        content=content.replaceAll("\\.", " \\. ");
        content=content.replaceAll(":", " : ");
        content=content.replaceAll(";", " ; ");
        content=content.replaceAll("-", " - ");
        content=content.replaceAll("\\(", " \\( ");
        content=content.replaceAll("\\)", " \\) ");
        content=content.replaceAll("\"", " \" ");
        content=content.replaceAll("!", " ! ");
        content=content.replaceAll("\\?", " \\? ");
        String[] tokens = content.split("[\\s]");

        int counter=0;
        for(int i=0;i< tokens.length;i++){
            if(tokens[i].equals(""))
                continue;
            Word word= new Word(counter,tokens[i]);
            words.add(word);
            counter++;
           
        }
        
        
        
    }
    


    //constructor form jaxb
    public Utterance(Segment segment, HashMap<Integer, Object> utteredOrder){
        this.depTree = new DependencyTree(); 
        words= new ArrayList<>();
        this.words = segment.getWords();
  
    }


    public Long getId(){
        return this.id;
    }
    public void setCorpusId(String id){
        this.corpusid = id;
    }

    public void setWords(List<Word> words){
        this.words = words;
    }

    public List<Word> getWords(){
        return this.words;
    }
    public Segment getSegment(){
        return new Segment(words);
    }

    public String getContent(){
        return getSegment().toString();
    }
    public String getLemmaContent(){
        return getSegment().getLemmas();
    }
    public String unAccentContent(){
        Segment seg = getSegment();
        String unAccentUtt = unAccent(getContent());
        /*
        String unAccentUtt ="";
        for(Word w:seg.getWords()){
            unAccentUtt+=" "+UtteranceFeatures.unAccent(w.getContent());
        }*/
        return unAccentUtt;
    }
    
        /*
     * Returns the string without accents
     */
    public static String unAccent(String s) {

      String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
      Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
      return pattern.matcher(temp).replaceAll("");
    }

    public String unAccentLemmas(){
        
        String unAccentUtt = unAccent(getLemmaContent());
        /*
        String unAccentUtt ="";
        for(Word w:seg.getWords()){
            unAccentUtt+=" "+UtteranceFeatures.unAccent(w.getContent());
        }*/
        return unAccentUtt;
    }    
    
   

    public String getCorpusId(){
        return this.corpusid;
    }
    

    public void setSegment(Segment segment){
        this.words = segment.getWords();
    }

    public void setId(Long id){
        this.id=id;
    }

    //toString
    @Override
    public String toString(){
        return this.getSegment().toString();
    }
    
    @Override
    public boolean equals(Object objU){
        if(objU instanceof Utterance){
        
            Utterance u = (Utterance) objU;
            if(u.getId().equals(this.getId()))
                  return true;
            return false;
        }
        return false;
    }
    
    public DependencyTree getDepTree(){
        return this.depTree;
    }

    public void setDepTree(DependencyTree dpTree){
        this.depTree=dpTree;
    }  
    

    public void addDependency(Dependency dependency){
        this.depTree.addDependency(dependency.getHead(), dependency);
    }
    
  
    
    public void addEntitySpan(String entity, Segment segment){
        
        goldEntities.put(segment,entity);
    }
    
    public boolean isEntitySpan(Segment segment){

        if(goldEntities.containsKey(segment))
            return true;
        
        
        return false;
    }
    
    public HashMap<Segment,String> getGoldEntities(){
        return this.goldEntities;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }
    
    @Override
    public Object clone() {
        try {
            Utterance u = (Utterance)super.clone();
            u.setSegment((Segment)u.getSegment().clone());
            return u;
        } catch (Exception ex) {
            return this;
        }
        

    }    
}
