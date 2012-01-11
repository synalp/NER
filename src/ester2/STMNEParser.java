package ester2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import utils.FileUtils;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.Mot;
import jsafran.POStagger;

public class STMNEParser {
	
	/**
	 * sauve un corpus au format .stm sans annotations d'EN (!) 
	 * utile pour passer sur ce corpus le LIAEN
	 * 
	 * @param g
	 */
	public static void saveSTM(List<DetGraph> gs, List<String> prefix, String outfile) {
		assert gs.size()==prefix.size();
		try {
			PrintWriter f = FileUtils.writeFileISO(outfile);
			for (int i=0;i<gs.size();i++) {
				StringBuilder sb = new StringBuilder();
				sb.append(prefix.get(i)); sb.append(' ');
				DetGraph g = gs.get(i);
				for (int j=0;j<g.getNbMots();j++) {
					sb.append(g.getMot(j)); sb.append(' ');
				}
				f.println(sb.toString());
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * sauve un corpus au format STME
	 * ATTENTION ! Chaque graphe doit avoir une SOURCE indiquee pointant vers un fichier STM(NE) d'origine
	 * De plus, chaque Mot doit avoir une position dans ce fichier STM d'origine !
	 * @param outfile
	 */
	public static void saveSTMNE(List<DetGraph> gs, String outfile) {
		try {
			final String EOL = "\n";
			String cursource="";
			ArrayList<String> srclines = new ArrayList<String>();
			ArrayList<String> outlines = new ArrayList<String>();
			String curoutline="";
			int cursrcline=-1;
			for (int i=0;i<gs.size();i++) {
				DetGraph g = gs.get(i);
				String newsource = g.getSource().getPath();
				if (!newsource.equals(cursource)) {
					BufferedReader fsrc = FileUtils.openFileUTF(newsource);
					cursource=newsource;
					srclines.clear();
					for (;;) {
						String s = fsrc.readLine();
						if (s==null) break;
						srclines.add(s);
					}
					fsrc.close();
				}
				
				for (int j=0;j<g.getNbMots();j++) {
					// get srcline for this word
					long poswd = g.getMot(j).getDebPosInTxt();
					int newsrcline=0;
					{
						long deblineinsrc=0;
						for (;;newsrcline++) {
							long endlineinsrc=deblineinsrc+srclines.get(newsrcline).length()+EOL.length();
							if (poswd<endlineinsrc) break;
							deblineinsrc=endlineinsrc;
						}
					}
					if (newsrcline>cursrcline) {
						if (curoutline.length()>0) {
							outlines.add(""+curoutline);
							curoutline="";
						}
						cursrcline=newsrcline;
						int k=srclines.get(cursrcline).indexOf("male>");
						curoutline+=srclines.get(cursrcline).substring(0,k+6);
					}
					// est-ce que le mot courant est un debut de groupe ?
					int[] gr = g.getGroups(j);
					if (gr!=null&&gr.length>0) {
						for (int grr : gr) {
							List<Mot> motsdugroupe = g.groups.get(grr);
							if (motsdugroupe.get(0)==g.getMot(j)) {
								curoutline+="["+g.groupnoms.get(grr)+" ";
							}
						}
					}
					curoutline+=g.getMot(j)+" ";
					// est-ce que le mot courant est une fin de groupe ?
					if (gr!=null&&gr.length>0) {
						for (int grr : gr) {
							List<Mot> motsdugroupe = g.groups.get(grr);
							if (motsdugroupe.get(motsdugroupe.size()-1)==g.getMot(j)) {
								curoutline+="] ";
							}
						}
					}
				}
			}
			if (curoutline.length()>0) {
				outlines.add(""+curoutline);
				curoutline="";
			}
			
			PrintWriter f = FileUtils.writeFileISO(outfile);
			for (String s : outlines) {
				f.println(s);
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static List<DetGraph> loadTRS(final String trsfiles) {
		final String[] args = {"-trs2stmne",trsfiles};
		ESTER2EN.main(args);
		// on a cree le fichier yy.stm-ne
		return loadSTMNE("yy.stm-ne");
	}
	
	/**
	 * charge un fichier STMNE dans une liste de graphes
	 * Le nom du fichier TRS de référence se trouve dans le champ "comment" du graphe
	 * La position de chaque mot dans le fichier STMNE est sauvegardée dans les Mots du graphe,
	 * afin de pouvoir ensuite récupérer regénérer un nouveau STMNE
	 * 
	 * @param stmnefile
	 * @return
	 */
	public static List<DetGraph> loadSTMNE(String stmnefile) {
		ArrayList<DetGraph> res = new ArrayList<DetGraph>();
		try {
			BufferedReader f = utils.FileUtils.openFileISO(stmnefile);
			long posinsrc = 0;
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				int i=s.indexOf("male>");
				if (i>=0) {
					String trs = s.substring(0,i).split(" ")[0];
					String ss=s.substring(i+6);
					long posss = posinsrc+i+6;
//					String[] x = ss.split(" ");
					DetGraph g = new DetGraph();
					g.comment=""+trs;
					g.setSource((new File(stmnefile)).toURI().toURL());
					ArrayList<String> entypes = new ArrayList<String>();
					ArrayList<Integer> endebs = new ArrayList<Integer>();
					int nextmotidx=0;
					for (int t=0;t<ss.length();t++) {
						int debmot=t;
						int finmot=ss.indexOf(' ',t);
						if (finmot<0) finmot=ss.length();
						if (finmot>debmot) {
							if (ss.charAt(debmot)=='[') {
								entypes.add(ss.substring(debmot+1,finmot));
								endebs.add(nextmotidx);
							} else if (ss.charAt(0)==']') {
								g.addgroup(endebs.get(endebs.size()-1), nextmotidx-1, entypes.get(entypes.size()-1));
								entypes.remove(entypes.size()-1);
								endebs.remove(endebs.size()-1);
							} else if (ss.charAt(finmot-1)==']') {
								String xxx = ss.substring(debmot,finmot-1).trim();
								if (xxx.length()>0) {
									Mot m = new Mot(xxx, xxx, "unk");
									m.setPosInTxt(posss+debmot, posss+finmot-1);
									g.addMot(nextmotidx++, m);
								}
								if (endebs.size()<=0) System.out.println("ZIP "+s);
								if (entypes.size()<=0) System.out.println("ZIP "+s);
								g.addgroup(endebs.get(endebs.size()-1), nextmotidx-1, entypes.get(entypes.size()-1));
								entypes.remove(entypes.size()-1);
								endebs.remove(endebs.size()-1);
							} else {
								String forme = ss.substring(debmot, finmot).trim();
								if (forme.length()>0) {
									Mot m = new Mot(forme, forme, "unk");
									m.setPosInTxt(posss+debmot, posss+finmot);
									g.addMot(nextmotidx++, m);
								}
							}
						}
						t=finmot;
					}
					res.add(g);
				}
				final String EOL="\n";
				posinsrc+=s.length()+EOL.length();
			}
			f.close();
			
			GraphIO gio = new GraphIO(null);
			gio.save(res, "xx.xml");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}
	
	public static void main(String[] args) {
		List<DetGraph> gs = STMNEParser.loadTRS(args[0]);
		for (DetGraph g : gs) {
			POStagger.tag(g);
		}
		GraphIO gio = new GraphIO(null);
		gio.save(gs, "output.xml");
	}
}
