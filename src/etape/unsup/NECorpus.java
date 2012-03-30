package etape.unsup;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

public class NECorpus {
	final String EOL = "__EOL__";
	
	HashMap<String, Integer> ne2id = new HashMap<String, Integer>();
	
	List<String> tokens = new ArrayList<String>();
	List<Integer> deben = new ArrayList<Integer>();
	List<Integer> finen = new ArrayList<Integer>();
	List<Integer> typen = new ArrayList<Integer>();
	
	String getEN(int enid) {
		for (String s : ne2id.keySet()) {
			if (ne2id.get(s)==enid) return s;
		}
		return null;
	}
	
	public NECorpus() {}
	
	private int getENid(String en) {
		Integer enid = ne2id.get(en);
		if (enid==null) {
			enid = ne2id.size();
			ne2id.put(en, enid);
		}
		return enid;
	}
	
	ArrayList<Integer> curendebs = new ArrayList<Integer>();
	ArrayList<Integer> curentyps = new ArrayList<Integer>();
	private void parseString(String s) {
		s=s.trim();
		s=s.replaceAll("<", " <");
		s=s.replaceAll(">", "> ");
		s=s.replaceAll("  *", " ");
		s=s.replaceAll(" +>", ">");
		s=s.replaceAll("< +", "<");
		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreTokens()) {
			String tok = st.nextToken();
			if (tok.charAt(0)=='<') {
				if (tok.charAt(1)=='/') {
					String enst = tok.substring(2, tok.length()-1);
					int enid = getENid(enst);
					int toremove=-1;
					for (int i=curendebs.size()-1;i>=0;i--) {
						if (curentyps.get(i)==enid) {
							toremove=i;
							break;
						}
					}
					if (toremove>=0) {
						deben.add(curendebs.get(toremove));
						finen.add(tokens.size()-1);
						typen.add(curentyps.get(toremove));
						curendebs.remove(toremove);
						curentyps.remove(toremove);
					} else {
						System.out.println("WARNING: EN terminated without deb ! "+getEN(enid));
						System.out.println(s);
						System.exit(1);
					}
				} else {
					String enst = tok.substring(1, tok.length()-1);
					curendebs.add(tokens.size());
					curentyps.add(getENid(enst));
				}
			} else {
				tokens.add(tok);
			}
		}
		tokens.add(EOL);
//		if (curendebs.size()>0)
//			System.out.println("WARNING: utt ended with ENs open !");
	}
	
	public void save(String outfile) {
		try {
			PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outfile),Charset.forName("ISO-8859-1")));
			for (int i=0;i<tokens.size();i++) {
				String tok = tokens.get(i);
				if (tok==EOL) f.println();
				else {
					for (int j=0;j<deben.size();j++) {
						if (deben.get(j)==i) {
							f.print("<"+getEN(typen.get(j))+"> ");
						}
					}
					f.print(tok+" ");
					for (int j=0;j<finen.size();j++) {
						if (finen.get(j)==i) {
							f.print("</"+getEN(typen.get(j))+"> ");
						}
					}
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void load(String nefile) {
		try {
			BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(nefile), Charset.forName("ISO-8859-1")));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				parseString(s);
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
