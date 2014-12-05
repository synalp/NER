package gigaword;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.Mot;
import tools.CNConstants;


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
        OutputStreamWriter outFile = null;
        try {
            outFile = new OutputStreamWriter(new FileOutputStream("../clustering/posinduction/src/bin/fgw.in",true),CNConstants.UTF8_ENCODING);
            for (int i=0;i<gs.size();i++) {
                    DetGraph group = gs.get(i);
                    int nexinutt=0;
                    //outFile.append("NO\tBS\tBS\n");
                    for (int j=0;j<group.getNbMots();j++) {
                            nexinutt++;
                            outFile.append(group.getMot(j).getForme()+"\n");
                            outFile.flush();
                    }
            }
            outFile.flush();
            outFile.close();            
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                outFile.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        }
	
	public static void main(String args[]) {
		GigawordIO m = new GigawordIO();
                for(int i=0; i< m.chunkPaths.size(); i++){
		//int i = Integer.parseInt(args[0]);
                    List<DetGraph> gs = m.getChunk(i);
                    //GraphIO gio = new GraphIO(null);
                    //gio.save(gs, "c"+i+".xml");
                    m.savingFiles(gs);
                }    
	}
}
