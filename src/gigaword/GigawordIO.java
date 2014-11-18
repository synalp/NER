package gigaword;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.Mot;


public class GigawordIO {
	final String corpdir = "/global/rojasbar/nasdata1/TALC/Synalp/Corpus/Gigaword_French/disk/data";//"../../corpus/Gigaword_French/disk/data";
	
	ArrayList<String> chunkPaths = new ArrayList<String>();
	
	public GigawordIO() {
		{
			File dir = new File(corpdir+"/afp");
			File[] fs = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File arg0, String arg1) {
					return arg1.endsWith(".gz");
				}
			});
			for (File f : fs) {
				chunkPaths.add(f.getAbsolutePath());
			}
		}
		{
			File dir = new File(corpdir+"/apw");
			File[] fs = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File arg0, String arg1) {
					return arg1.endsWith(".gz");
				}
			});
			for (File f : fs) {
				chunkPaths.add(f.getAbsolutePath());
			}
		}
		System.out.println("found nchunks "+chunkPaths.size());
	}
	
	public List<DetGraph> getChunk(int chunk) {
		if (chunk<0||chunk>=chunkPaths.size()) return null;
		String p = chunkPaths.get(chunk);
		try {
			GZIPInputStream i = new GZIPInputStream(new FileInputStream(p));
			BufferedReader f = new BufferedReader(new InputStreamReader(i,Charset.forName("UTF-8")));
			ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
			boolean istext = false;
			for (int z=0;;z++) {
				String s = f.readLine();
				if (s==null) break;
				if (s.length()==0) continue;
				if (z%10000==0) System.out.println("lines "+z);
				if (s.indexOf("<TEXT>")>=0) {
					istext=true; continue;
				}
				if (s.indexOf("</TEXT>")>=0) {
					istext=false; continue;
				}
				if (!istext) continue;
				if (s.charAt(0)=='<') continue;
				// tokenisation tres simple, car la segmentation plus complexe est bcp trop lente...
				s=s.replace('=', ' ');
				s=s.replace("-", " - ");
				s=s.replace(";", " ;");
				s=s.replace(":", " :");
				s=s.replace("!", " !");
				s=s.replace("?", " ?");
				s=s.replace("``", " `` ");
				s=s.replace("''", " ''");
				s=s.replace("/", " / ");
				s=s.replace("\"", " \" ");
				s=s.replace("[", " [ ");
				s=s.replace("]", " ] ");
				s=s.replace("{", " { ");
				s=s.replace("}", " } ");
				s=s.replace("(", " ( ");
				s=s.replace(")", " ) ");

				// point
				{
					int x=0;
					for (;;) {
						x=s.indexOf('.',x);
						if (x<0) break;
						if (x>0) {
							char c = s.charAt(x-1);
							if (Character.isDigit(c)||Character.isUpperCase(c)) {
								// on le laisse dans le mot
							} else {
								s=s.substring(0,x)+' '+s.substring(x);
								++x;
							}
						}
						++x;
					}
				}
				
				// virgule
				{
					int x=0,y=0;
					for (;;) {
						x=s.indexOf(',',y);
						if (x<0) break;
						if (x>0) {
							char c = s.charAt(x-1);
							if (Character.isDigit(c)) {
								// on le laisse dans le mot
							} else {
								s=s.substring(0,x)+' '+s.substring(x);
								++x;
							}
						}
						y=x+1;
					}
				}

				// apostrophe
				{
					int x=0,y=0;
					for (;;) {
						x=s.indexOf('\'',y);
						if (x<0) break;
						if (x>0) {
							if (s.substring(x+1).toLowerCase().startsWith("hui")) {
								// on le laisse dans le mot
							} else {
								++x;
								s=s.substring(0,x)+' '+s.substring(x);
							}
						}
						y=x+1;
					}
				}

				s=s.replaceAll("  +", " ");
				s=s.trim();
				String[] st = s.split(" ");
				DetGraph g = new DetGraph();
				int idx=1;
				for (int t=0;t<st.length;t++) {
					if (st[t].length()>0) {
						Mot m = new Mot(st[t].trim(), idx);
						g.addMot(idx-1, m);
						idx++;
					}
				}
				if (g.getNbMots()>0) gs.add(g);
			}
			f.close();
			
			return gs;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
        
        public void savingFiles(List<DetGraph> gs){
            
//            for (int i=0;i<gs.size();i++) {
//                    DetGraph group = gs.get(i);
//                    int nexinutt=0;
//                    //outFile.append("NO\tBS\tBS\n");
//                    for (int j=0;j<group.getNbMots();j++) {
//                            nexinutt++;
//
//                            // calcul du label
//                            String lab = CNConstants.NOCLASS;
//                            int[] groups = group.getGroups(j);
//                            if (groups!=null)
//                                for (int gr : groups) {
//
//                                    if(entity.equals(ONLYONEPNOUNCLASS)){
//                                        //all the groups are proper nouns pn
//                                        for(String str:groupsOfNE){
//                                            if (group.groupnoms.get(gr).startsWith(str)) {
//                                                if(typeofClass.equals(CNConstants.BIO)){
//                                                    int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
//                                                    if (debdugroupe==j) lab = entity+"B";    
//                                                    else lab = entity+"I";                                                            
//                                                }else if(typeofClass.equals(CNConstants.BILOU)){
//                                                    int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
//                                                    int endgroupe = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;   
//                                                            if(debdugroupe==endgroupe){ 
//                                                                lab=entity+"U";
//                                                            }else{
//                                                                if (debdugroupe==j) lab = entity+"B";
//                                                                else if(endgroupe==j) lab=entity+"L";
//                                                                else lab = entity+"I";
//                                                            }                                                            
//                                                }else
//                                                    lab=entity;
//                                                break;
//                                            }
//                                        }
//                                    }else{
//                                        if (group.groupnoms.get(gr).startsWith(entity)) {
//                                            if(typeofClass.equals(CNConstants.BIO)){
//                                                int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
//                                                if (debdugroupe==j) lab = entity+"B";    
//                                                else lab = entity+"I";
//                                            }else if(typeofClass.equals(CNConstants.BILOU)){
//                                                    int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
//                                                    int endgroupe = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;   
//                                                            if(debdugroupe==endgroupe){ 
//                                                                lab=entity+"U";
//                                                            }else{
//                                                                if (debdugroupe==j) lab = entity+"B";
//                                                                else if(endgroupe==j) lab=entity+"L";
//                                                                else lab = entity+"I";
//                                                            }                                                            
//                                            }else
//                                                lab=entity;
//                                            break;
//                                        }else{
//                                            if (entity.equals(ONLYONEMULTICLASS)) {
//                                                String groupName=group.groupnoms.get(gr);
//                                                groupName=groupName.substring(0, groupName.indexOf("."));
//                                                if(!Arrays.asList(groupsOfNE).toString().contains(groupName))
//                                                    continue;
//
//                                                if(typeofClass.equals(CNConstants.BIO)){
//                                                    int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
//                                                    if (debdugroupe==j) lab = entity+"B";    
//                                                    else lab = entity+"I";
//                                                }else if(typeofClass.equals(CNConstants.BILOU)){
//                                                    int debdugroupe = group.groups.get(gr).get(0).getIndexInUtt()-1;
//                                                    int endgroupe = group.groups.get(gr).get(group.groups.get(gr).size()-1).getIndexInUtt()-1;
//                                                    if (debdugroupe==endgroupe) lab = groupName+"U"; //Unit
//                                                    else if (debdugroupe==j) lab = groupName+"B"; //Begin
//                                                    else if (endgroupe==j) lab = groupName+"L"; //Last
//                                                    else lab = groupName+"I";//Inside
//                                                }else
//                                                    lab=groupName;
//                                                break;
//                                            }                                                    
//                                        }
//                                    }
//                                }
//                            ///*        
//                            if(iswiki){
//                                if(!isStopWord(group.getMot(j).getPOS())){
//                                    String inWiki ="F";
//                                    if(!group.getMot(j).getPOS().startsWith("PRO") && !group.getMot(j).getPOS().startsWith("ADJ")&&
//                                            !group.getMot(j).getPOS().startsWith("VER") && !group.getMot(j).getPOS().startsWith("ADV"))
//                                        inWiki =(WikipediaAPI.processPage(group.getMot(j).getForme()).equals(CNConstants.CHAR_NULL))?"F":"T";
//                                    outFile.append(lab+"\t"+group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\t"+ inWiki +"\n");
//                                    wordcount++;
//                                    System.out.println("processed word number " + wordcount);
//                                } 
//                            }else if(!isStopWord(group.getMot(j).getPOS())){
//                                if(isLower)
//                                    outFile.append(lab+"\t"+group.getMot(j).getForme().toLowerCase()+"\t"+group.getMot(j).getPOS()+"\n");
//                                else
//                                    outFile.append(lab+"\t"+group.getMot(j).getForme()+"\t"+group.getMot(j).getPOS()+"\n");
//                            }
//                    }
//
//
//
//            }            
        }
	
	public static void main(String args[]) {
		GigawordIO m = new GigawordIO();
		int i = Integer.parseInt(args[0]);
		List<DetGraph> gs = m.getChunk(i);
		GraphIO gio = new GraphIO(null);
		gio.save(gs, "c"+i+".xml");
	}
}
