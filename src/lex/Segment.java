package lex;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import java.util.Objects;
import tools.CNConstants;
import utils.ErrorsReporting;

/**
 * This class encapsulates a segment, it contains a list of words
 * with the start and end id's of the start and end word respectively.
 * @author rojasbar
 * Nov 17 2009
 * <!--
 * Modifications History
 * Date           Author   	Description
 * Nov 17, 2009   rojasbar  	Creation
 * -->
 */
public class Segment implements Cloneable{
    //list of words
    private Integer start;
    private Integer end;
    private HashMap<Integer, Word> words;
    //stores frequencies in the utterance
    private HashMap<String, Integer> lucounterMap=new HashMap<>();
    private HashMap<String, Integer> poscounterMap=new HashMap<>();
    private HashMap<String, Integer> wscounterMap=new HashMap<>();    

    /**
     * Creates a segment, a segment is a list of words
     * assigns the start as the id of the first word
     * and the end as the id of the last word.
     * It verifies that the input list is in fact organized
     * by words ids.
     *
     * @param words
     */
    public Segment(List<Word> words){

        this.words = new HashMap<>();

        if(words == null || words.size() == 0){
            this.start = CNConstants.INT_NULL;
            this.end   = CNConstants.INT_NULL;
            return;
        }

        //organize words according to their id
        Collections.sort(words, new Comparator<Word>() {
        @Override
        public int compare(Word w1, Word w2)
        {
                return w1.getPosition()-w2.getPosition();
        }});
        
        for(Word word:words){
            this.words.put(word.getPosition(), word);
        }     
        
        //verifies that the start and end corresponds to the lower and higher id
        //respectively
        this.start = words.get(new Integer(0)).getPosition();
        int size = (words.size() == 0)? 0 :words.size()-1;
        this.end   = words.get(size).getPosition();

 
    }

    /**
     * Sets the start, the id of the first word of the segment
     * @return
     */
    public Integer getStart(){
        return this.start;
    }

    /**
     * Sets the end, the id of the last word of the segment
     * @return
     */
    public Integer getEnd(){
        return this.end;
    }

    //toString
    @Override
    public String toString(){
        String text="";
         if(words == null || words.size() == 0){
             return text;
         }
        for(int i=start; i<=end; i++){
            text = text + " " + words.get(i).getContent();
        }
        return text.trim();
    }

    public String getLemmas(){
        String text="";
         if(words == null || words.size() == 0){
             return text;
         }
        for(int i=start; i<=end; i++){
            text = text + " " + words.get(i).getLemmaContent();
        }
        return text.trim();
    }  

    public void computingWordFrequencies(){
        for(Word word:words.values()){
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
    
    public HashMap<String, Integer> getWordFrequencies(){
        return this.lucounterMap;
    }
    
    public int getWordFrequency(String content){
        if(this.lucounterMap.isEmpty())
            return 0;
        
        return lucounterMap.get(content);
    }

    public HashMap<String, Integer> getPOSFrequencies(){
        return this.poscounterMap;
    }    
    
    public int getPOSFrequency(String pos){
        if(this.poscounterMap.isEmpty())
            return 0;
        
        return poscounterMap.get(pos);
    }   
     
    public HashMap<String, Integer> getWordShapeFrequency(){
        return this.wscounterMap;
    }      
    
    public int getWordShapeFrequency(String shape){
        if(this.wscounterMap.isEmpty())
            return 0;
        
        return wscounterMap.get(shape);
    }     
    
    /**
     * It returns the list of words included in the segment given the start and the end parameters.
     * If the start and end are not included in the segment it returns a PMException
     * @param start
     * @param end
     * @return
     */
    public Segment subSegment(Integer start, Integer end, String elementId){

        List<Word> wordsInInterval = new ArrayList<>();
        if(start == this.start && end == this.end)
            return new Segment(this.getWords());

        if( start < this.start || end > this.end){
            //builds the string error with the values to be replaced in the error message, separated by pipes
            String error= start + "|" + end + "|"+ elementId +"|"+ this.start +"|"+this.end;
            ErrorsReporting.report("The given start value: "+start+" and end value: "+ end +" for the element "+ elementId +" are out of bounds: ["+this.start+"-"+this.end +"]");
            
        }

        if(start == end){
            
            Word word = this.words.get(start);
            wordsInInterval.add(word);
            return new Segment(wordsInInterval);

        }

        //wordsInInterval = getWords().subList(start, end);
        ///*
        for(int i=start; i<=end; i++){
            Word word = this.words.get(i);
            wordsInInterval.add(word);
        }//*/

        return new Segment(wordsInInterval);

    }

    public Segment subSegment(Integer start, Integer end){

        List<Word> wordsInInterval = new ArrayList<>();
        if(start == this.start && end == this.end)
            return new Segment(this.getWords());

        if( start < this.start || end > this.end){
           return null;
        }

        if(start == end){

            Word word = this.words.get(start);
            wordsInInterval.add(word);
            return new Segment(wordsInInterval);

        }

        for(int i=start; i<=end; i++){
            Word word = this.words.get(i);
            wordsInInterval.add(word);
        }
        Segment subSeg = new Segment(wordsInInterval);

        return subSeg;

    }

    public boolean isEmpty(){
        return this.words.isEmpty();
    }

    public boolean contains(Segment segment){

        if(this.start <= segment.start &&
           this.end >= segment.end)
            return true;

        return false;

    }

    public boolean contains(Word word){

        return contains(word.getPosition());

    }
    
    public boolean contains(Integer key){
        if(this.start <= key &&
           this.end >= key)
            return true;

        return false;        
    }
        
    public List<Word> getWords(){
        List<Word> listOfWords = new ArrayList<>();
       //It returns the ordered list of words according to the keys
        for(int i=start; i<=end; i++){
            Word word = this.words.get(i);
            listOfWords.add(word);
        }
        
        return listOfWords;
    }

    public HashMap<Integer, Word>getWordMap(){
        return this.words;
    }
   
    public Word getWord(Integer key){
        return this.words.get(key);
    }

    public Word getWord(String content){
        for(Word word:words.values()){
            if(word.getContent().equals(content))
                    return word;
        }
        return null;
    }

    public void replaceWord(Integer key, Word word){
        this.words.put(key, word);
    }

   public Word getStartWord(){
       return this.getWord(this.getStart());
   }

   public int getSize(){
       return (this.end - this.start);

   }

   public boolean equals(Segment segment2){
       if(this.start == segment2.getStart() &&
          this.end   == segment2.getEnd()){
           return toString().equals(segment2.toString());
       }
       return false;
   }
   


    @Override
    public int hashCode() {
        int firstHash  = (start == null ? CNConstants.INT_NULL : start.hashCode());
        int secondHash = (end == null ? CNConstants.INT_NULL : end.hashCode());

        return firstHash*31 + secondHash;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Segment other = (Segment) obj;
        return this.equals(other);
    }
   
    @Override
    public Object clone() {
        
            //Segment s = (Segment)super.clone();
            List<Word> clonedWords = new ArrayList<>();
            for(int i=start; i<=end; i++){
                Word w = getWord(i);
                clonedWords.add((Word)w.clone());
            }
            Segment s = new Segment(clonedWords);
            return s;
        
        

    }   
    
    public List<Segment> difference(Segment otherSegment){
        List<Segment> segs= new ArrayList<>();
        if(!this.contains(otherSegment))
            return segs;
        
        if(this.equals(otherSegment))
            return segs;
        
        if(otherSegment.getStart() == start){
            if(otherSegment.getEnd() < end){
                Segment seg=this.subSegment(otherSegment.getEnd()+1,end);
                segs.add(seg);
                return segs;
                     
            }
        }
        if(otherSegment.getEnd() == end){
            if(otherSegment.getStart() > start){
                Segment seg=this.subSegment(start,otherSegment.getStart()-1);
                segs.add(seg);
                return segs;              
            }
        }
        
        segs.add(this.subSegment(start,otherSegment.getStart()-1)); 
        segs.add(this.subSegment(otherSegment.getEnd()+1,end));
        
        return segs;
    }
   
    public List<Segment> difference(List<Segment> otherSegments){
        List<Segment> subsegs= new ArrayList<>();
        if(otherSegments.isEmpty()){
            subsegs.add(this);
            return subsegs;
        } 
        //order the subsegments
        Collections.sort(otherSegments, new Comparator<Segment>() {
        @Override
        public int compare(Segment s1, Segment s2)
        {
                return s1.getStart()-s2.getStart();
        }}); 
        
        Segment seg = otherSegments.get(0);
        subsegs=  difference(seg);
        if(subsegs.size()>0){
            Segment last=subsegs.get(subsegs.size()-1);
            otherSegments.remove(seg);
            if(otherSegments.isEmpty())
                return subsegs;
            
            subsegs.remove(last);
            subsegs.addAll(last.difference(otherSegments));
            
        }
        
        
        return subsegs;
    }
    
   public static void main(String[] args) {
        
        List<Word> words = new ArrayList<>();
        words.add(new Word(4, "réservation"));
        words.add(new Word(0, "j'"));
        words.add(new Word(2, "voulu"));
        words.add(new Word(6, "Nancy"));
        words.add(new Word(3, "une"));
        words.add(new Word(5, "à"));
        words.add(new Word(1, "aurais"));
        Segment segment = new Segment(words);
        System.out.println("Segment :" + segment.toString());
        Segment subSeg = segment.subSegment(3, 5);
        System.out.println("Sub Segment: " + subSeg.toString());
        
        List<Segment> segs= new ArrayList<>();
        segs.add(subSeg);
        segs.add(segment.subSegment(0, 1));
        segs.add(segment.subSegment(1, 3));
        /*List<Segment> jointSegs=segment.union(segs);
        
        for(Segment s:jointSegs)
            System.out.println(s+" ");           
        System.out.println(segs.toString());*/
        
        List<Segment> disjointSegs= segment.difference(segs);
        for(Segment s:disjointSegs)
            System.out.println(s+" ");
        
    
   }

}
