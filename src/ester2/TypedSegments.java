package ester2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class TypedSegments {
	// pour supported le recouvrement de segment, je decompose les segments en "tokens" (il peu s'agir de caractere au niveau le + fin, ou de mots, ...)
	ArrayList<String> tokens = new ArrayList<String>();
	// chaque segment est alors une sequence contigue de tokens: [token_deb, token_fin inclus]
	ArrayList<int[]> segments = new ArrayList<int[]>();
	
	// on peut indiquer des debtime et endtime pour certains tokens
	ArrayList<Integer> tokidx4time = new ArrayList<Integer>();
	ArrayList<Float> tokdebtime = new ArrayList<Float>();
	ArrayList<Float> tokendtime = new ArrayList<Float>();
	
	// for each "type", list all segments of this type
	ArrayList<List<Integer>> types = new ArrayList<List<Integer>>();
	// map a type to its index (as used in var types)
	HashMap<String, Integer> dicotypes = new HashMap<String, Integer>();
	
	/**
	 * attention ! n'update pas les segments !
	 */
	public void splitTokens() {
		ArrayList<String> newtoks =new ArrayList<String>();
		ArrayList<Integer> newtokidx4time = new ArrayList<Integer>();
		ArrayList<Float> newtokdebtime = new ArrayList<Float>();
		ArrayList<Float> newtokendtime = new ArrayList<Float>();
		
		for (int oldtok=0;oldtok<tokens.size();oldtok++) {
			String[] ss = ESTER2EN.tokenize(tokens.get(oldtok));
			if (ss.length<=0) continue;
			int firsttok=newtoks.size();
			newtoks.addAll(Arrays.asList(ss));
			int lasttok = newtoks.size()-1;
			float deb = getTokenDebTime(oldtok);
			float end = getTokenEndTime(oldtok);
			if (lasttok-firsttok==0) {
				if (deb>=0||end>=0) {
					newtokidx4time.add(firsttok);
					newtokdebtime.add(deb);
					newtokendtime.add(end);
				}
			} else {
				if (deb>=0) {
					newtokidx4time.add(firsttok);
					newtokdebtime.add(deb);
					newtokendtime.add(-1f);
				}
				if (end>=0) {
					newtokidx4time.add(lasttok);
					newtokdebtime.add(-1f);
					newtokendtime.add(end);
				}
			}
		}
		tokens=newtoks;
		tokidx4time=newtokidx4time;
		tokdebtime=newtokdebtime;
		tokendtime=newtokendtime;
	}
	
	/**
	 * retourne l'indice du token
	 */
	public int addToken(String tok) {
		tokens.add(tok);
		return tokens.size()-1;
	}
	public int addToken(String tok, float debtime, float endtime) {
		int t = addToken(tok);
		tokidx4time.add(t);
		tokdebtime.add(debtime);
		tokendtime.add(endtime);
		return t;
	}
	public void setTokenDebTime(int tok, float debtime) {
		int i=tokidx4time.indexOf(tok);
		if (i<0) {
			tokidx4time.add(tok);
			tokdebtime.add(debtime);
			tokendtime.add(-1f);
		} else {
			tokdebtime.set(i, debtime);
		}
	}
	public void setTokenEndTime(int tok, float endtime) {
		int i=tokidx4time.indexOf(tok);
		if (i<0) {
			tokidx4time.add(tok);
			tokdebtime.add(-1f);
			tokendtime.add(endtime);
		} else {
			tokendtime.set(i, endtime);
		}
	}
	public float getTokenEndTime(int tok) {
		int i=tokidx4time.indexOf(tok);
		if (i<0) return -1f;
		return tokendtime.get(i);
	}
	public float getTokenDebTime(int tok) {
		int i=tokidx4time.indexOf(tok);
		if (i<0) return -1f;
		return tokdebtime.get(i);
	}
	public void clearSegments() {
		segments.clear();
		for (List<Integer> l : types) {
			l.clear();
		}
	}
	public void addTypedSegment(int tokdeb, int tokfin, String type) {
		Integer t = dicotypes.get(type);
		if (t==null) {
			t=dicotypes.size();
			dicotypes.put(type, t);
			types.add(new ArrayList<Integer>());
		}
		List<Integer> segs4type = types.get(t);
		assert segs4type!=null;
		segs4type.add(segments.size());
		int[] x = {tokdeb,tokfin};
		segments.add(x);
	}
	
	public List<Integer> getSegsForType(String type) {
		Integer t = dicotypes.get(type);
		if (t==null) return null;
		return types.get(t);
	}
	public String getToken(int i) {
		return tokens.get(i);
	}
	/**
	 * 
	 * @return the left[0] and right[1] context of a segment, up to len tokens
	 */
	public String[] getContextOfSegment(int seg, int len) {
		int[] x=segments.get(seg);
		int ltok=x[0], rtok=x[1];
		StringBuilder lsb = new StringBuilder();
		int t=ltok-len; if (t<0) t=0;
		for (int tok=t;tok<ltok;tok++) {
			lsb.append(tokens.get(tok));
			lsb.append(' ');
		}
		StringBuilder rsb = new StringBuilder();
		for (int tok=rtok+1;tok<tokens.size()&&tok-rtok<=len;tok++) {
			rsb.append(tokens.get(tok));
			rsb.append(' ');
		}
		String[] res = {lsb.toString().trim(),rsb.toString().trim()};
		return res;
	}
	public String[] getContextOfToken(int midtok, int len) {
		StringBuilder lsb = new StringBuilder();
		int t=midtok-len; if (t<0) t=0;
		for (int tok=t;tok<midtok;tok++) {
			lsb.append(tokens.get(tok));
			lsb.append(' ');
		}
		StringBuilder rsb = new StringBuilder();
		for (int tok=midtok+1;tok<tokens.size()&&tok-midtok<=len;tok++) {
			rsb.append(tokens.get(tok));
			rsb.append(' ');
		}
		String[] res = {lsb.toString().trim(),rsb.toString().trim()};
		return res;
	}
	public int[] getTokens4segment(int segidx) {
		return segments.get(segidx);
	}
	public String getSegment(int i) {
		int[] tokids = segments.get(i);
		if (tokids[0]==tokids[1]) return tokens.get(tokids[0]);
		StringBuilder sb = new StringBuilder();
		for (int j=tokids[0];j<=tokids[1];j++) {
			sb.append(tokens.get(j));
			sb.append(' ');
		}
		return sb.toString();
	}
	public int getNbSegments() {return segments.size();}
	public int getNbTokens() {return tokens.size();}
	public List<String> getAllTokens() {return tokens;}
	public Set<String> getAllTypes() {
		return dicotypes.keySet();
	}
	public boolean isFirstTokenInaSegment(int tok, ArrayList<String> segtypes, ArrayList<Integer> segends) {
		boolean is=false;
		for (int j=0;j<segments.size();j++) {
			int[] x = segments.get(j);
			if (x[0]>tok) break;
			if (x[0]==tok) {
				is=true;
				String[] types = getTypesForSegment(j);
				segtypes.addAll(Arrays.asList(types));
				Integer[] ends = new Integer[types.length];
				Arrays.fill(ends, x[x.length-1]);
				segends.addAll(Arrays.asList(ends));
			}
		}
		return is;
	}
	public String[] getTypesForToken(int i) {
		// quels segments avec ce token ?
		ArrayList<String> res = new ArrayList<String>();
		for (int j=0;j<segments.size();j++) {
			int[] x = segments.get(j);
			if (x[0]>i) break;
			if (x[0]==i) {
				String[] xx = getTypesForSegment(j);
//				for (int k=0;k<xx.length;k++) xx[k]="START"+xx[k];
				res.addAll(Arrays.asList(xx));
			} else
				if (x[1]==i) {
					String[] xx = getTypesForSegment(j);
					//				for (int k=0;k<xx.length;k++) xx[k]="END"+xx[k];
					res.addAll(Arrays.asList(xx));
				}
		}
		return res.toArray(new String[res.size()]);
	}
	public String[] getTypesForSegment(int i) {
		ArrayList<Integer> typs = new ArrayList<Integer>();
		for (int j=0;j<types.size();j++) {
			List<Integer> ts = types.get(j);
			if (ts.contains(i)) typs.add(j);
		}
		String[] res = new String[typs.size()];
		int l=0;
		for (String s : dicotypes.keySet()) {
			int k = dicotypes.get(s);
			if (typs.contains(k)) res[l++]=s;
		}
		return res;
	}
	
	public String toString() {
		StringBuilder sb =new StringBuilder();
		for (int i=0;i<tokens.size();i++) {
			String s=tokens.get(i);
			sb.append(s); sb.append(' ');
		}
		return sb.toString();
	}
}
