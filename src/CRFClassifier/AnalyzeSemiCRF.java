/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package CRFClassifier;

import iitb.CRF.CRF;
import iitb.Model.FeatureGenImpl;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Properties;
import jsafran.DetGraph;
import jsafran.GraphIO;
import tools.CNConstants;
import utils.ErrorsReporting;

/**
 *
 * @author rojasbar
 */
public class AnalyzeSemiCRF {
    public static String MODELFILE="en.%S.scrf.mods";
    public static String TRAINFILE="groups.%S.tab.scrf.train";
    public static String TESTFILE="groups.%S.tab.scrf.test";
    public static String LISTTRAINFILES="esterTrainALL.xmll";
    public static String LISTTESTFILES="esterTestALL.xmll";
    public static String UTF8_ENCODING="UTF8";
    //public static String PROPERTIES_FILE="semicrf.props";
    public static String NUMFEATSINTRAINFILE="2-";
    public static String ONLYONEPNOUNCLASS=CNConstants.PRNOUN;
    public static String[] groupsOfNE = {CNConstants.PERS,CNConstants.ORG, CNConstants.LOC, CNConstants.PROD};
    
    public static int TRAINSIZE=Integer.MAX_VALUE;
       
    
    public void saveFilesForLClassifier(boolean bltrain) {
            try {
                GraphIO gio = new GraphIO(null);
                OutputStreamWriter outFile =null;
                String xmllist=LISTTRAINFILES;
                if(bltrain)
                    outFile = new OutputStreamWriter(new FileOutputStream(TRAINFILE.replace("%S", CNConstants.ALL)),UTF8_ENCODING);
                else{
                    xmllist=LISTTESTFILES;
                    outFile = new OutputStreamWriter(new FileOutputStream(TESTFILE.replace("%S", CNConstants.ALL)),UTF8_ENCODING);
                }
                BufferedReader inFile = new BufferedReader(new FileReader(xmllist));
                int uttCounter=0;
                for (;;) {
                    String s = inFile.readLine();
                    if (s==null) break;
                    List<DetGraph> gs = gio.loadAllGraphs(s);
                    for (int i=0;i<gs.size();i++) {
                            DetGraph group = gs.get(i);
                            int nexinutt=0;
                            String span="";
                            int lab = 0;
                            for (int j=0;j<group.getNbMots();j++) {
                                    nexinutt++;

                                    // calcul du label
                                    
                                    int[] groups  = group.getGroups(j);
                                    if (groups!=null && groups.length>0){
                                        for (int gr : groups) {
                                            //all the groups are proper nouns pn
                                            for(int k=0; k<groupsOfNE.length;k++){
                                                if (group.groupnoms.get(gr).startsWith(groupsOfNE[k])) {
                                                    if(span.length()>0 && lab != k+1){
                                                        outFile.append(span +"|"+lab+"\n");
                                                        span="";  
                                                    }
                                                    lab=k+1;
                                                    int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
                                                    int endgroupe = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;                                                    
                                                    break;
                                                }
                                            }
                                        }
                                   }else{
                                       if(span.length()>0 && lab!=0){
                                        outFile.append(span +"|"+lab+"\n");
                                        span="";
                                       }
                                       lab = 0; 
                                   }
     
                                    span+=group.getMot(j).getForme()+"\t";
                                    
                                        
                            }
                            if(span.length()>0)
                                outFile.append(span +"|"+lab+"\n");
                                                        
                            uttCounter++;
                            outFile.append("\n");
                            if(bltrain && uttCounter> TRAINSIZE){
                                break;
                            }
                    }
                    if(bltrain && uttCounter> TRAINSIZE){
                        break;
                    }                    
                }
                outFile.flush();
                outFile.close();
                inFile.close();
                ErrorsReporting.report("groups saved in groups.*.tab"+uttCounter);
            } catch (IOException e) {
                    e.printStackTrace();
            }
    }  
    
    public static void main(String args[]) {
       AnalyzeSemiCRF semiCRFAPI = new AnalyzeSemiCRF();
       semiCRFAPI.saveFilesForLClassifier(true);
    }    

}
