/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package lex;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import tools.CNConstants;

/**
 * this class contains the dependency tree and
 * HashMaps to facilitate the tree computation
 * @author rojasbar

 * <!--
 * Modifications History
 * Date           Author   	Description
 * Sep 09, 2010   rojasbar      It is possible to have more than one
 *                              root, in case of parser errors.
 * May 11, 2010   rojasbar  	Creation
 * -->
 *
 */
public class DependencyTree implements Serializable{
    private static final long serialVersionUID = 1L; 
    public static String    rootCONLLLabel ="ROOT";
    private List<Word>   roots;
    private Word   targetSTRoot;
    private TreeMap<Word, Dependency> depTree;
    private int numNodes=0,level=0;
    private HashMap<Word, DependencyTree> subtree;
    
    //HashMaps that support the template mappings
   
    //dependents and the head
    private HashMap<Integer, Word> depsHead;
    //all dependents
    private HashMap<Integer, Word> allDeps;
    //dependents and relations
    private HashMap<Integer, String> depRels;
    //String of the word and id.
    // problem with repeated strings
    //private HashMap<String, Integer> positionLU;

    public DependencyTree(){
        this.depTree  = new TreeMap<>(new Comparator<Word>(){
                            public int compare(Word w1, Word w2)
                            {
                              return w1.getPosition()-w2.getPosition();
                            }});
        roots       = new ArrayList<>();
        depsHead    = new HashMap<>();
        allDeps     = new HashMap<>();
        depRels     = new HashMap<>();
        subtree     = new HashMap<>();
        //positionLU  = new HashMap<String, Integer>();

    }

    public void setAllDependencies(TreeMap<Word, Dependency> deps){
        this.depTree = deps;
        getDTRoot();
    }

    public void addDependency(Word head , Dependency dependences){
        this.depTree.put(head, dependences);
        allDeps.putAll(dependences.getDependents());
    }

    public TreeMap<Word, Dependency> getTree(){
        return this.depTree;
    }

   /**
     * Returns the dependents of the given head
     * @param head
     * @return
   */
    public Dependency getDependency(Word head){

        return this.depTree.get(head);

    }

    /**
     * Returns the root of the dependency tree associated to a
     * given sentence.
     * @param segment
     * @return
     */
    public Word getDTRoot(Segment segment){
        
        List<Word> headSet = new ArrayList<>(depTree.keySet());
        List<Word> headsInSeg = new ArrayList<>();
        List<Word> plausHead  = new ArrayList<>();
        HashMap<Integer,Word> allDepsInseg = new HashMap<>();
        Word tmpRoot = new Word(CNConstants.INT_NULL,CNConstants.CHAR_NULL);

        //extracts the heads for the given segment
       
       //headsInSeg = headSet.subList(minIndex, maxIndex);
       headsInSeg = getHeadsInSegment(segment.getStart(),segment.getEnd());
       if(headsInSeg.size() == 0){
           if(segment.getSize()==1)
              tmpRoot = segment.getStartWord();
           
           targetSTRoot = tmpRoot;
           return targetSTRoot;
       }

       //sets all the dependencies in a hashMap for the heads in the segment
       for(Word head:headsInSeg){
            Dependency dep = depTree.get(head);
            allDepsInseg.putAll(dep.getDependents());
       }
       //the root is the word that is not dependent of any other word
       for(Word head:headsInSeg){
           if(!allDepsInseg.containsKey(head.getPosition())){
              tmpRoot = head;
              break;
           }

       }
       targetSTRoot = tmpRoot;
       return targetSTRoot;

    }

    public List<Word> getHeadsInSegment(int start, int end){
        List<Word> headSet = new ArrayList<>(depTree.keySet());
        List<Word> words = new ArrayList<>();
        int minIndex = getGreaterOrEqualHeadIdx(start);
        int maxIndex = getLowerOrEqualHeadIdx(end);
        if(minIndex == CNConstants.INT_NULL ||
           maxIndex == CNConstants.INT_NULL)
            return words;

        if(maxIndex < minIndex)
            return words;

        if(minIndex == maxIndex)
            words.add(headSet.get(minIndex));
        else {
            //sublist returns a list from mixIndex inclusive to
            //maxIndex exclusive
            words = headSet.subList(minIndex, maxIndex);
            words.add(headSet.get(maxIndex));
        }
        return words;
    }

    public Word getNextRightHead(Integer wordId){
       List<Word> headSet = new ArrayList<>(depTree.keySet());
       int rightHeadId = getRightHeadIdx(wordId);
       if(rightHeadId == CNConstants.INT_NULL)
           return null;
       return headSet.get(rightHeadId);
    }

    private int getRightHeadIdx(Integer wordId){
        List<Word> headSet = new ArrayList<>(depTree.keySet());
       //extracts the heads for the given segment
       for(int i= 0; i< headSet.size();i++){
            if(headSet.get(i).getPosition() > wordId){
                return i;
            }
       }

       return CNConstants.INT_NULL;
    }

    private int getGreaterOrEqualHeadIdx(Integer wordId){
        List<Word> headSet = new ArrayList<>(depTree.keySet());
        Collections.sort(headSet, new Comparator<Word>() {
                     @Override
                     public int compare(Word w1, Word w2)
                     {
                        return w1.getPosition()-w2.getPosition();
                     }});

       //extracts the heads for the given segment
       for(int i= 0; i< headSet.size();i++){
            if(headSet.get(i).getPosition() >= wordId){
                return i;
            }
       }

       return CNConstants.INT_NULL;
    }

    public Word getNextLeftHead(Integer wordId){
       List<Word> headSet = new ArrayList<>(depTree.keySet());
       int nextLeftHeadId = getLowerOrEqualHeadIdx(wordId);
       if(nextLeftHeadId == CNConstants.INT_NULL)
           return null;
       return headSet.get(nextLeftHeadId);
    }
    
    private int getNextLeftHeadIdx(Integer index){
      List<Word> headSet = new ArrayList<>(depTree.keySet());
      for(int i=headSet.size()-1;i>=0;i--){
           if(headSet.get(i).getPosition() < index){
               return i;
            }

      }
      return CNConstants.INT_NULL;
    }


    private int getLowerOrEqualHeadIdx(Integer index){
      List<Word> headSet = new ArrayList<>(depTree.keySet());
       Collections.sort(headSet, new Comparator<Word>() {
                     @Override
                     public int compare(Word w1, Word w2)
                     {
                        return w1.getPosition()-w2.getPosition();
                     }});
      for(int i=headSet.size()-1;i>=0;i--){
           if(headSet.get(i).getPosition() <= index){
               return i;
            }

      }
      return CNConstants.INT_NULL;
    }

    public List<Word> getDTRoot(){
        roots.clear();
        setAllInMemory();
        //the root is the word that is not dependent of any other word
        for(Word head:depTree.keySet()){
            if(!allDeps.containsKey(head.getPosition()))
              roots.add(head);
              
            
        }

        return roots;
    }

    public void setTargetRoot(Word head){
        this.targetSTRoot = head;
    }

    public Word getTargetRoot(){
        return this.targetSTRoot;
    }

    public String getRelOfDep(Word dep){
        setAllInMemory();
        if(depRels.containsKey(dep.getPosition()))
            return depRels.get(dep.getPosition());
        else
            return rootCONLLLabel;//return HLSConstants.CONLLFIELD_NULL;
    }

    private void setAllInMemory(){
       if(!isMemoryEmpty())
           return;
       
       //sets all the dependencies in a hashMap for the heads in the segment
       for(Word head:depTree.keySet()){
            //positionLU.put(head.toString(), head.getId());
            Dependency dep = depTree.get(head);
            allDeps.putAll(dep.getDependents());
            
            for(Integer key:dep.getDependents().keySet() ){
                depsHead.put(key,head);
                //positionLU.put(dep.getDependent(key).toString(),key);
                depRels.put(key, dep.getRelation(key));
            }

       }
    }

    private void clearMemory(){
        allDeps.clear();
        depsHead.clear();
    }

    private boolean isMemoryEmpty(){
        return (allDeps.size() == 0 ||
                depsHead.size() == 0   );
    }

    public boolean containsHead(Word head){
        return this.depTree.containsKey(head);
    }

    public Word getHeadOfWord(Word dependent){
        setAllInMemory();
        if(this.depsHead.containsKey(dependent.getPosition())){
            return this.depsHead.get(dependent.getPosition());
        }
        return dependent;
    }

    public Word getCONLLHeadOfWord(Word dependent){
        setAllInMemory();
        if(this.depsHead.containsKey(dependent.getPosition())){
            return this.depsHead.get(dependent.getPosition());
        }
        return null;
    }

    /**
     * this method returns the whole segment in the utterance rooted by
     * the segment containing the root given as argument.
     * It looks for the left and right most dependents.
     * @param utterance
     * @param rootSegment
     * @return
     */
    public Segment getWholeSegmentRootedBy(Utterance utterance, Segment rootSegment){
        
            Integer argSegStart = getLeftMostDep(rootSegment.getStartWord());
            Integer argSegEnd   = getRightMostDep(rootSegment.getWord(rootSegment.getEnd()));

            if(argSegStart == rootSegment.getStart() && argSegEnd == rootSegment.getEnd())
                return rootSegment;
            //retrieves the subsegment in the utterance
            Segment newArgSeg   = utterance.getSegment().subSegment(argSegStart, argSegEnd);
             

        return newArgSeg;
    }

    public Integer getLeftMostDep(Word node){

        if(!containsHead(node))
            return node.getPosition();



        Dependency   dependency = getDependency(node);
        //if there are not left dependencies return node
        List<Word> words = dependency.getLeftDependents();
        if(words.size()==0){
            return node.getPosition();
        }

        words.add(node);
        /*Segment depSeg = new Segment(words);
        Integer start = depSeg.getStart();*/
        Integer start = words.get(0).getPosition();
        if(node.getPosition() == start)
            return start;

        return getLeftMostDep(dependency.getDependent(start));

        //Segment rightMostSubSeg = getArgumentInterval(utterance, dependency.getDependent(end));


    }

    public Integer getRightMostDep(Word node){

        if(!containsHead(node))
            return node.getPosition();
        else{

                Dependency   dependency = getDependency(node);
                //if there are not left dependencies return node

                List<Word> words = dependency.getRightDependents();
                if(words.size()==0){
                    return node.getPosition();
                }
                //words.addAll(dependency.getAllDependents());
                words.add(0,node);
                /*Segment depSeg = new Segment(words);
                Integer end = depSeg.getEnd();*/
                Integer end = words.get(words.size()-1).getPosition();
                if(node.getPosition() == end)
                    return end;
                return getRightMostDep(dependency.getDependent(end));

                //Segment rightMostSubSeg = getArgumentInterval(utterance, dependency.getDependent(end));

        }
    }
 

    public boolean isAncestor(Word ancestor, Word child){

        Word parent = getHeadOfWord(child);

        //the child is the root of the DT
        if(parent.getPosition() == child.getPosition())
            return false;

        if(parent.getPosition() == ancestor.getPosition())
            return true;


        return isAncestor(ancestor, parent);

    }
    
    /**
     * Obtain the top-down tree rooted by the given head
     * @param head
     * @return 
     */
    public String getTreeTopDownFeatureForHead(Word head, boolean isPOS){
        String tree="";
        
        
        Dependency dep = getDependency(head);
        String value=(isPOS)?head.getPosTag().getName():head.cleanContent();
        if(dep==null)
            return value;
          
        
        tree+="("+value;
        //tree+="("+head.getPosTag().getName();
        for(Integer depid:dep.getDependents().keySet()){           
            String relName = dep.getRelations().get(depid);
            Word dependent=dep.getDependents().get(depid);
            //String subtree=getTreeTopDownFeatureForHead(dependent);
            //subtree= (subtree.contains("("))?"("+subtree+")":subtree;
            tree+=" ("+relName+" "+getTreeTopDownFeatureForHead(dependent,isPOS)+")";

        }
        
        tree+=") ";
        return tree;
    }    
    public String getTreeTopDownFeatureForHeadCW(Word head, int nCalls, boolean isPOS){
        String tree="";
        
        String nodeContent=(isPOS)?head.getPosTag().getName():head.cleanContent();
        Dependency dep = getDependency(head);
        if(dep==null)
           return head.getPosTag().getName();
        
        if(nCalls==0)
            tree+="("+CNConstants.CURRWORD+nodeContent;
        else
            tree+="("+nodeContent;
        //tree+="("+nodeContent;
        for(Integer depid:dep.getDependents().keySet()){           
            String relName = dep.getRelations().get(depid);
            Word dependent=dep.getDependents().get(depid);
            //String subtree=getTreeTopDownFeatureForHead(dependent);
            //subtree= (subtree.contains("("))?"("+subtree+")":subtree;
            tree+=" ("+relName+" "+getTreeTopDownFeatureForHeadCW(dependent,nCalls+1,isPOS)+")";

        }
        
        tree+=") ";
        return tree;
    }
     public String getPruningTreeTopDownFeatureForHeadCW(Word head, DependencyTree dT, boolean isPOS){
        String tree="";
        
        Dependency dep = getDependency(head);
        
        dT.numNodes++;
        String nodeContent=(isPOS)?head.getPosTag().getName():head.cleanContent();
        if(dep==null || dT.level > CNConstants.tree_level_threshold)
            return nodeContent;
        
              
        if(dT.depTree.isEmpty())
            tree+="("+CNConstants.CURRWORD+nodeContent;
        else
            tree+="("+nodeContent;
        
        dT.addDependency(head, dep);
        dT.level++;
        
        //tree+="("+head.getPosTag().getName();
        for(Integer depid:dep.getDependents().keySet()){           
            String relName = dep.getRelations().get(depid);
            Word dependent=dep.getDependents().get(depid);
            //String subtree=getTreeTopDownFeatureForHead(dependent);
            //subtree= (subtree.contains("("))?"("+subtree+")":subtree;
            tree+=" ("+relName+" "+getPruningTreeTopDownFeatureForHeadCW(dependent,dT, isPOS)+")";
            
            
        }
        
        tree+=") ";
        subtree.put(head, dT);
        return tree;
    }  
     
     public String getPruningTreeTopDownFeatureForHead(Word head, DependencyTree dT, boolean isPOS){
        String tree="";
        
        Dependency dep = getDependency(head);
        
        dT.numNodes++;
        String nodeContent=(isPOS)?head.getPosTag().getName():head.cleanContent();
        if(dep==null || dT.level > CNConstants.tree_level_threshold)
            return nodeContent;
        
              
        if(dT.depTree.isEmpty())
            tree+="("+nodeContent;
        else
            tree+="("+nodeContent;
        
        dT.addDependency(head, dep);
        dT.level++;
        
        //tree+="("+head.getPosTag().getName();
        for(Integer depid:dep.getDependents().keySet()){           
            String relName = dep.getRelations().get(depid);
            Word dependent=dep.getDependents().get(depid);
            //String subtree=getTreeTopDownFeatureForHead(dependent);
            //subtree= (subtree.contains("("))?"("+subtree+")":subtree;
            tree+=" ("+relName+" "+getPruningTreeTopDownFeatureForHead(dependent,dT, isPOS)+")";
            
            
        }
        
        tree+=") ";
        subtree.put(head, dT);
        return tree;
    }      
    
    /**
     * Obtain the bottom up tree that has the given word as dependent
     * @param dependent
     * @param tree
     * @return 
     */
    public String getTreeBottomUpFeatureForHead(Word dependent, String tree,boolean isPOS){
        if(tree.isEmpty()) 
            tree="%S";
        
        String nodeContent=(isPOS)?dependent.getPosTag().getName():dependent.cleanContent();
        if(allDeps.containsKey(dependent.getPosition())){
            Word head= depsHead.get(dependent.getPosition());
            String rel = depRels.get(dependent.getPosition());
            if(tree.equals("%S"))
                //tree=tree.replace("%S", " ("+rel+" "+nodeContent)+")";
                tree=tree.replace("%S", " ("+rel+" "+CNConstants.CURRWORD+nodeContent)+")";
            else
                tree=tree.replace("%S", " ("+rel+" ("+nodeContent)+"))";
            tree=getTreeBottomUpFeatureForHead(head, "%S"+tree, isPOS);
        }else
            tree=tree.replace("%S", nodeContent);
            
        
        return tree;
    }
    
    public String getPrunningTreeBottomUpFeatureForHeadCW(Word dependent, String tree, DependencyTree dT, boolean isPOS){
        if(tree.isEmpty()) 
            tree="%S";
        
        dT.level++;
        dT.numNodes++;
        String nodeContent=(isPOS)?dependent.getPosTag().getName():dependent.cleanContent();
        ///*
        if(dT.level> CNConstants.tree_level_threshold)
            return tree.replace("%S", nodeContent);
        //*/
        if(allDeps.containsKey(dependent.getPosition())){
            Word head= depsHead.get(dependent.getPosition());
            String rel = depRels.get(dependent.getPosition());
            if(tree.equals("%S"))
                //tree=tree.replace("%S", " ("+rel+" "+nodeContent)+")";
                tree=tree.replace("%S", " ("+rel+" "+CNConstants.CURRWORD+nodeContent)+")";
            else
                tree=tree.replace("%S", " ("+rel+" ("+nodeContent)+"))";
            tree=getPrunningTreeBottomUpFeatureForHeadCW(head, "%S"+tree, dT,isPOS);
        }else{
            if(tree.equals("%S"))
              tree=tree.replace("%S", CNConstants.CURRWORD+nodeContent);
            else
                tree=tree.replace("%S", nodeContent);
        }    
        
        return tree;
    }  
    public String getPrunningTreeBottomUpFeatureForHead(Word dependent, String tree, DependencyTree dT,boolean isPOS){
        if(tree.isEmpty()) 
            tree="%S";
        
        dT.level++;
        dT.numNodes++;
        String nodeContent=(isPOS)?dependent.getPosTag().getName():dependent.cleanContent();
        ///*
        if(dT.level> CNConstants.tree_level_threshold)
            return tree.replace("%S", nodeContent);
        //*/
        if(allDeps.containsKey(dependent.getPosition())){
            Word head= depsHead.get(dependent.getPosition());
            String rel = depRels.get(dependent.getPosition());
            tree=tree.replace("%S", " ("+rel+" ("+nodeContent)+"))";
            tree=getPrunningTreeBottomUpFeatureForHead(head, "%S"+tree, dT,isPOS);
        }else{
    
                tree=tree.replace("%S", nodeContent);
        }    
        
        return tree;
    }       
    /**
     * Obtain the bottom up tree for the given segment
     * @param segment
     * @return 
     */
    public String getTreeTopDownFeatureForSegment(Segment segment, boolean isPOS){
        String tree="";
        
        Word head= getDTRoot(segment);
        Dependency dep = getDependency(head);
        String nodeContent=(isPOS)?head.getPosTag().getName():head.cleanContent();
        if(dep==null)
            return nodeContent;
        
        List<Integer> wordsintree=new ArrayList(dep.getDependents().keySet());
        wordsintree.add(head.getPosition());
        wordsintree.retainAll(segment.getWordMap().keySet());
        if(wordsintree.size()==1 && wordsintree.contains(head.getPosition()))
            return tree;
        
        tree+="("+nodeContent;
        for(Integer depid:dep.getDependents().keySet()){
            String relName = dep.getRelations().get(depid);
            Word dependent=dep.getDependents().get(depid);
            if(!segment.contains(dependent))
                continue;
            //String subtree=getTreeTopDownFeatureForHead(dependent);
            //subtree= (subtree.contains("("))?"("+subtree+")":subtree;
            tree+=" ("+relName+" "+getTreeTopDownFeatureForHead(dependent,isPOS)+")";
        }
        
        tree+=") ";
        return tree;
    }
    
    /**
      * Returns the segments yielded by all the dependencies governed by the given head
      * @param head
      * @param uttSegment
      * @return 
      */
    public HashMap<Segment,String> getHeadDepSpans(Word head, Segment uttSegment){
        HashMap<Segment,String> spans = new HashMap<>();
        
        /*
        //put all the span yielded by the head
        Integer left= getLeftMostDep(head);
        Integer right= getRightMostDep(head); 
        spans.put(uttSegment.subSegment(left, right),head.getContent());
        */
        Dependency dep = getDependency(head);
        if(dep==null)
             return spans;
                //sub spans yielded by its dependents
        for(Integer depid:dep.getDependents().keySet()){  
            Integer left= getLeftMostDep(dep.getDependent(depid));
            Integer right= getRightMostDep(dep.getDependent(depid));
            if(head.getPosition()<left)
                left=head.getPosition();
            if(head.getPosition()>right)
                right=head.getPosition();
            
            spans.put(uttSegment.subSegment(left, right),head.getContent());
        }         
          
          spans.putAll(unionOfChildSpans(spans));
          spans.put(uttSegment.subSegment(head.getPosition(), head.getPosition()),head.getContent());
          return spans;
          
      }
    
   public HashMap<Segment,String> unionOfChildSpans(HashMap<Segment,String> subsegments){
       
       HashMap<Segment,String> concatenatedSegs=new HashMap<>();
       List<Segment>forwardSegs = new ArrayList<>(subsegments.keySet());
       List<Segment> segs = new ArrayList<>(subsegments.keySet());
       Collections.sort(segs, new Comparator<Segment>() {
       @Override
       public int compare(Segment s1, Segment s2)
       {
                return s1.getStart()-s2.getStart();
       }});
       for(int i=0;i<segs.size();i++){
           if(i+1 > segs.size()-1)
               break;
           if(segs.get(i).getEnd()==segs.get(i+1).getStart() || 
                   segs.get(i).contains(segs.get(i+1).getStart())){
               List<Word> wordsInSeg = new ArrayList<>(segs.get(i).getWords());
               wordsInSeg.addAll(segs.get(i+1).getWords());
               concatenatedSegs.put(new Segment(wordsInSeg),subsegments.get(segs.get(0)));
               forwardSegs.remove(segs.get(i));
           }
       } 
       
       if(concatenatedSegs.isEmpty())
           return concatenatedSegs;
       
       //segs.addAll(concatenatedSegs);
       concatenatedSegs.putAll(unionOfChildSpans(concatenatedSegs));
       return concatenatedSegs;
        
   }    
      
      public List<Word> getLeaves(){
          List<Word> leaves= new ArrayList<>();
          
          for(Integer key:allDeps.keySet()){
              Word dep= allDeps.get(key);
              if(!this.depTree.containsKey(dep))
                  leaves.add(dep);
          }
          
          
          return leaves;
      }
     
      public int getNumberOfNodes(){
          return this.numNodes;
      }
      public int getLevel(){
          return this.level;
      }
      
      public DependencyTree getSubTree(Word head){
          return subtree.get(head);
      }
}

