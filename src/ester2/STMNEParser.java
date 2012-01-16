package ester2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import utils.FileUtils;
import utils.SuiteDeMots;

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
	 * Load un fichier .trs et aligne dessus des graphes,
	 * puis projette les groupes depuis ces graphes vers un .stm-ne sous la forme d'entites nommees
	 * 
	 * @param gs
	 * @param stmneOriging
	 * @param outfile
	 */
	public static void projectGroupsInSTMNE(List<DetGraph> gs, String trsOriging, String outfile) {
		final String prefix = FileUtils.noExtNoDir(trsOriging)+" 1 UNK ";
		ArrayList<String[]> utts = new ArrayList<String[]>();
		ArrayList<Float> uttdebs = new ArrayList<Float>();
		ArrayList<Integer> uttidx = new ArrayList<Integer>();
		uttdebs.add(0f);
		uttidx.add(0);
		try {
			BufferedReader f=  FileUtils.getReaderGuessEncoding(trsOriging);
			float endturntime = Float.NaN;
			float lastsync = 0;
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				int i=s.indexOf("<Turn ");
				if (i>=0) {
					i=s.indexOf("startTime=");
					int j=s.indexOf('"',i);
					int k=s.indexOf('"',j+1);
					lastsync=Float.parseFloat(s.substring(j+1,k));
					uttdebs.add(lastsync);
					uttidx.add(utts.size());
					if (uttidx.size()>1&&uttidx.get(uttidx.size()-1)==uttidx.get(uttidx.size()-2)) {
						uttdebs.remove(uttdebs.size()-2);
						uttidx.remove(uttidx.size()-2);
					}
					i=s.indexOf("endTime=");
					j=s.indexOf('"',i);
					k=s.indexOf('"',j+1);
					endturntime=Float.parseFloat(s.substring(j+1,k));
				} else if (s.indexOf("</Turn>")>=0) {
					lastsync=endturntime;
					uttdebs.add(lastsync);
					uttidx.add(utts.size());
					if (uttidx.size()>1&&uttidx.get(uttidx.size()-1)==uttidx.get(uttidx.size()-2)) {
						uttdebs.remove(uttdebs.size()-2);
						uttidx.remove(uttidx.size()-2);
					}
				} else if ((i=s.indexOf("<Sync time="))>=0) {
					int j=s.indexOf('"',i);
					int k=s.indexOf('"',j+1);
					lastsync=Float.parseFloat(s.substring(j+1,k));
					uttdebs.add(lastsync);
					uttidx.add(utts.size());
					if (uttidx.size()>1&&uttidx.get(uttidx.size()-1)==uttidx.get(uttidx.size()-2)) {
						uttdebs.remove(uttdebs.size()-2);
						uttidx.remove(uttidx.size()-2);
					}
				} else {
					// on supprime toutes les autres infos, a part les sync times
					s=s.replaceAll("<[^<>]*>", "");
					if (s.indexOf('<')>=0) {
						System.out.println("WARNING parsing trs: unended < ? "+s);
					}
					String[] st = FileUtils.simpleTokenization(s);
					if (st.length>0) {
						utts.add(st);
					}
				}
			}
			f.close();
			System.out.println("trs loaded "+utts.size()+" "+uttdebs.size()+" "+uttidx.size());
			
			ArrayList<String> allmots = new ArrayList<String>();
			for (int i=0;i<utts.size();i++) {
				allmots.addAll(Arrays.asList(utts.get(i)));
			}
			String[] motsTRS = allmots.toArray(new String[allmots.size()]);
			SuiteDeMots strs = new SuiteDeMots(motsTRS);
			
			allmots = new ArrayList<String>();
			for (DetGraph g : gs) {
				allmots.addAll(Arrays.asList(g.getMots()));
			}
			String[] motsgs = allmots.toArray(new String[allmots.size()]);
			System.out.println("nb de mots du TRS: "+motsTRS.length+" "+motsgs.length+" |"+motsTRS[0]+"="+motsgs[0]+" |"+motsTRS[motsTRS.length-1]+"="+motsgs[motsgs.length-1]);
			SuiteDeMots sgs = new SuiteDeMots(motsgs);
			sgs.align(strs);
			System.out.println("alignement fait !");
			
			class EnInTRS implements Comparable<EnInTRS> {
				int deb, end;
				String en;
				public EnInTRS(int d, int f, String t) {
					deb=d; end=f; en=t;
				}
				@Override
				public int compareTo(EnInTRS o) {
					if (deb<o.deb) return -1;
					else if (deb>o.deb) return 1;
					else {
						if (end<o.end) return -1;
						else if (end>o.end) return 1;
						else {
							return en.compareTo(o.en);
						}
					}
				}
				public String toString() {return deb+"-"+end+":"+en;}
			}
			ArrayList<EnInTRS> ensintrs = new ArrayList<EnInTRS>();
			int gwordIdx=0;
			for (DetGraph g : gs) {
				if (g.groups!=null&&g.groups.size()>0) {
					for (int gr=0;gr<g.groups.size();gr++) {
						int debings = gwordIdx+g.groups.get(gr).get(0).getIndexInUtt()-1;
						int[] intrs = sgs.getLinkedWords(debings);
						if (intrs==null||intrs.length==0) {
							System.out.println("WARNING: groupe perdu !");
						} else {
							int debintrs = intrs[0];
							int endings = gwordIdx+g.groups.get(gr).get(g.groups.get(gr).size()-1).getIndexInUtt()-1;
							intrs = sgs.getLinkedWords(endings);
							if (intrs==null||intrs.length==0) {
								System.out.println("WARNING: groupe perdu !");
							} else {
								int endintrs = intrs[intrs.length-1];
								ensintrs.add(new EnInTRS(debintrs, endintrs, g.groupnoms.get(gr)));
							}
						}
					}
				}
				gwordIdx+=g.getNbMots();
			}
			Collections.sort(ensintrs);
			System.out.println("ens indexed with TRS done: found "+ensintrs.size());
			int enidx=0;
			ArrayList<EnInTRS> withinEN = new ArrayList<EnInTRS>();
			
			PrintWriter fstmne = new PrintWriter(new FileWriter(outfile));
			// TODO: check that we do not forget words in the end
			int motidx=0;
			for (int i=0;i<uttdebs.size()-1;i++) {
				float debtime = uttdebs.get(i);
				float endtime = uttdebs.get(i+1);
				int uidx1 = uttidx.get(i);
				int uidx2 = uttidx.get(i+1);
				StringBuilder utt = new StringBuilder();
				// TODO insert the groups info
				for (int u=uidx1;u<uidx2;u++) {
					String[] mots = utts.get(u);
					for (String m : mots) {
						// ajoute les ENs qui commencent sur ce mot
						if (enidx<ensintrs.size()) {
							if (motidx>ensintrs.get(enidx).deb) {
								System.out.println("ERROR "+motidx+" "+ensintrs.get(enidx));
							} else if (motidx==ensintrs.get(enidx).deb) {
								while (enidx<ensintrs.size()&&motidx==ensintrs.get(enidx).deb) {
									utt.append('['); utt.append(ensintrs.get(enidx).en); utt.append(' ');
									withinEN.add(ensintrs.get(enidx++));
								}
							}
						}
						utt.append(m);
						utt.append(' ');
						
						// supprime les ENs qui sont refermees sur ce mot
						ArrayList<Integer> toremove = new ArrayList<Integer>();
						for (int z=0;z<withinEN.size();z++) {
							EnInTRS eni=withinEN.get(z);
							if (eni.end==motidx) {
								utt.append("] ");
								toremove.add(z);
							} else if (eni.end<motidx) System.out.println("ERROR3 "+motidx+" "+eni);
						}
						for (int z=toremove.size()-1;z>=0;z--) {
							withinEN.remove((int)toremove.get(z));
						}
						motidx++;
					}
				}
				fstmne.println(prefix+debtime+" "+endtime+" <o,f3,male> "+utt);
			}
			fstmne.close();
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
		if (args[0].equals("-project2stmne")) {
			GraphIO gio = new GraphIO(null);
			List<DetGraph> gs = gio.loadAllGraphs(args[1]);
			String trs = args[2];
			projectGroupsInSTMNE(gs, trs, args[3]);
		} else {
			List<DetGraph> gs = STMNEParser.loadTRS(args[0]);
			for (DetGraph g : gs) {
				POStagger.tag(g);
			}
			GraphIO gio = new GraphIO(null);
			gio.save(gs, "output.xml");
		}
	}
}
