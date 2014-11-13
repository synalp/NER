package gigaword;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class GigawordEn {

	ArrayList<String> chunkPaths = new ArrayList<String>();

	public GigawordEn() {
		try {
			BufferedReader flist = new BufferedReader(new FileReader("res/gigaword1996.list"));
			for (;;) {
				String s=flist.readLine();
				if (s==null) break;
				chunkPaths.add(s);
			}
			flist.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("found nchunks "+chunkPaths.size());
	}

	public int getNchunks() {return chunkPaths.size();}
	
	// no segmentation in sentences is done. But punctuation is preserved.
	// basic tokenization is done.
	public List<String> getChunk(int chunk) {
		if (chunk<0||chunk>=chunkPaths.size()) return null;
		ArrayList<String> res = new ArrayList<String>();
		String p = chunkPaths.get(chunk);
		try {
			GZIPInputStream i = new GZIPInputStream(new FileInputStream(p));
			BufferedReader f = new BufferedReader(new InputStreamReader(i,Charset.forName("UTF-8")));
			boolean istext = false;
			StringBuilder sb=null;
			for (int z=0;;z++) {
				String s = f.readLine();
				if (s==null) break;
				if (s.length()==0) continue;
				if (z%10000==0) System.out.println("lines "+z);
				if (s.indexOf("<P>")>=0) {
					sb=new StringBuilder();
					istext=true; continue;
				}
				if (s.indexOf("</P>")>=0) {
					String tt=sb.toString().trim();
					if (tt.length()>0) res.add(tt);
					istext=false; continue;
				}
				if (!istext) continue;
				if (s.charAt(0)=='<') continue;
				
				// don't tokenize any more, because this shall be done by openNLP
				
				// tokenisation tres simple, car la segmentation plus complexe est bcp trop lente...
//				s=s.replace('=', ' ');
//				s=s.replace("-", " - ");
//				s=s.replace(";", " ;");
//				s=s.replace(":", " :");
//				s=s.replace("!", " !");
//				s=s.replace("?", " ?");
//				s=s.replace("``", " `` ");
//				s=s.replace("''", " ''");
//				s=s.replace("/", " / ");
//				s=s.replace("\"", " \" ");
//				s=s.replace("[", " [ ");
//				s=s.replace("]", " ] ");
//				s=s.replace("{", " { ");
//				s=s.replace("}", " } ");
//				s=s.replace("(", " ( ");
//				s=s.replace(")", " ) ");

				// point
//				{
//					int x=0;
//					for (;;) {
//						x=s.indexOf('.',x);
//						if (x<0) break;
//						if (x>0) {
//							char c = s.charAt(x-1);
//							if (Character.isDigit(c)||Character.isUpperCase(c)) {
//								// on le laisse dans le mot
//							} else {
//								s=s.substring(0,x)+' '+s.substring(x);
//								++x;
//							}
//						}
//						++x;
//					}
//				}

				// virgule
//				{
//					int x=0,y=0;
//					for (;;) {
//						x=s.indexOf(',',y);
//						if (x<0) break;
//						if (x>0) {
//							char c = s.charAt(x-1);
//							if (Character.isDigit(c)) {
//								// on le laisse dans le mot
//							} else {
//								s=s.substring(0,x)+' '+s.substring(x);
//								++x;
//							}
//						}
//						y=x+1;
//					}
//				}
//
//				// apostrophe
//				{
//					int x=0,y=0;
//					for (;;) {
//						x=s.indexOf('\'',y);
//						if (x<0) break;
//						// TODO: do n't    does n't
//						if (x>0) {
//							if (s.substring(x+1).toLowerCase().startsWith("hui")) {
//								// on le laisse dans le mot
//							} else {
//								++x;
//								s=s.substring(0,x)+' '+s.substring(x);
//							}
//						}
//						y=x+1;
//					}
//				}

				s=s.replaceAll("  +", " ");
				s=s.trim();
				sb.append(' '+s);
			}
			f.close();

			return res;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	void show(List<String> c) {
		for (int i=0;i<c.size();i++) {
			System.out.println(c.get(i));
		}
	}
	
	public static void main(String args[]) {
		GigawordEn m = new GigawordEn();
		List<String> c = m.getChunk(0);
		m.show(c);
	}
}
