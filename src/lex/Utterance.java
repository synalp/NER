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
    //stores frequencies in the utterance
    private HashMap<String, Integer> lucounterMap=new HashMap<>();
    private HashMap<String, Integer> poscounterMap=new HashMap<>();
    private HashMap<String, Integer> wscounterMap=new HashMap<>();

   
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
    
    public void computingWordFrequencies(){
        for(Word word:words){
            int freq=0;
            if(lucounterMap.containsKey(word.getContent()))
                freq = lucounterMap.get(word.getContent());
            lucounterMap.put(word.getContent(), freq+1);
            freq=0;
            if(poscounterMap.containsKey(word.getPosTag().getName()))
                freq = poscounterMap.get(word.getPosTag().getName());
            poscounterMap.put(word.getPosTag().getName(), freq+1);  
            freq=0;
            if(wscounterMap.containsKey(word.getLexicalUnit().getPattern()))
                freq = wscounterMap.get(word.getLexicalUnit().getPattern());
            wscounterMap.put(word.getLexicalUnit().getPattern(), freq+1);              
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
    
    public int getWordFrequency(String content){
        if(this.lucounterMap.isEmpty())
            return 0;
        
        return lucounterMap.get(content);
    }
  
     public int getPOSFrequency(String pos){
        if(this.poscounterMap.isEmpty())
            return 0;
        
        return poscounterMap.get(pos);
    }   
     
    public int getWordShapeFrequency(String shape){
        if(this.wscounterMap.isEmpty())
            return 0;
        
        return wscounterMap.get(shape);
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
