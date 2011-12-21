import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;


public class GigawordIO {
	final String corpdir = "../../corpus/Gigaword_French/disk/data";
	
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
			BufferedReader f = new BufferedReader(new InputStreamReader(i));
			
			boolean istext = false;
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				if (s.indexOf("<TEXT>")>=0) {
					istext=true; continue;
				}
				if (!istext) continue;
				// tokenisation tres simple, car la segmentation plus complexe est bcp trop lente...
				s=s.trim();
				String[] st = s.split(" ");
				for (int t=0;t<st.length;t++) {
					if (st[t].length()>1) {
						
					}
				}
			}
			f.close();
			
			return gs;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String args[]) {
		GigawordIO m = new GigawordIO();
		List<DetGraph> gs = m.getChunk(0);
		GraphIO gio = new GraphIO(null);
		gio.save(gs, "c0.xml");
	}
}
