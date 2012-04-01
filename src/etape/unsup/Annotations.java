package etape.unsup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Annotations {
	
	final public static String[] baseens = {"amount","func","loc","org","pers","prod","time","time","unk"};
	
	final public static String[] transverse = {"demonym","kind","name","object","qualifier","range-mark","unit","val"};
	final public static String[] specific_pers_ind = {"name.first","name.last","name.middle","title"};
	final public static String[] specific_loc_add_phys = {"address.number","other-address-component","po-box","zip-code"};
	final public static String[] specific_time_date = {"century","day","millenium","month","reference-era","time-modifier","week","year"};
	
	List<Integer> debs = new ArrayList<Integer>();
	List<Integer> ends = new ArrayList<Integer>();
	List<Integer> typen = new ArrayList<Integer>();
	
	List<Integer> openDebs = new ArrayList<Integer>();
	List<Integer> openTyps = new ArrayList<Integer>();

	HashMap<String, Integer> ne2id = new HashMap<String, Integer>();

	
	public void startNewEN(int tokenDeb, String entyp) {
		openDebs.add(tokenDeb);
		openTyps.add(getENid(entyp));
	}
	/**
	 * 
	 * @param lastToken inclusive !!
	 * @param entyp
	 */
	public void closePreviousEN(int lastToken, String entyp) {
		int typ = getENid(entyp);
		if (openTyps.size()==0) {
			System.out.println("WARNING: closing EN without deb !");
			System.exit(1);
		}
		if (openTyps.get(openTyps.size()-1)!=typ) {
			System.out.println("WARNING: closing EN "+typ+" crossing "+openTyps.get(openTyps.size()-1));
			System.exit(1);
		}
		debs.add(openDebs.get(openDebs.size()-1));
		ends.add(lastToken);
		typen.add(typ);
		
		openDebs.remove(openDebs.size()-1);
		openTyps.remove(openTyps.size()-1);
	}
	
	
/*	
	public void addDeb(int tokenIdx) {
		debs.add(tokenIdx);
	}
	public void addFin(int tokenIdx) {
		ends.add(tokenIdx);
	}
	public void addTyp(int tokenIdx) {
		debs.add(tokenIdx);
	}
*/
	
	class EN implements Comparable<EN> {
		String typ;
		int end;
		public EN(String t, int e) {
			typ=t; end=e;
		}
		@Override
		public int compareTo(EN o) {
			if (end>o.end) return 1;
			else if (end<o.end) return -1;
			else {
				// TODO: trier par type
				return 0;
			}
		}
	}
	String[] getTypDeb(int token) {
		ArrayList<EN> ens = new ArrayList<EN>();
		for (int i=0;i<debs.size();i++) {
			if (debs.get(i)==token) {
				ens.add(new EN(getEN(typen.get(i)),ends.get(i)));
			}
		}
		Collections.sort(ens);
		String[] res=new String[ens.size()];
		for (int i=0;i<res.length;i++)
			res[i]=ens.get(i).typ;
		
		return res;
	}
	String[] getTypEnd(int token) {
		ArrayList<EN> ens = new ArrayList<EN>();
		for (int i=0;i<ends.size();i++) {
			if (ends.get(i)==token) {
				ens.add(new EN(getEN(typen.get(i)),debs.get(i)));
			}
		}
		Collections.sort(ens);
		String[] res=new String[ens.size()];
		for (int i=res.length-1;i>=0;i--)
			res[i]=ens.get(i).typ;
		
		return res;
	}
	
	String getEN(int enid) {
		for (String s : ne2id.keySet()) {
			if (ne2id.get(s)==enid) return s;
		}
		return null;
	}
	private int getENid(String en) {
		Integer enid = ne2id.get(en);
		if (enid==null) {
			enid = ne2id.size();
			ne2id.put(en, enid);
		}
		return enid;
	}
	

}
