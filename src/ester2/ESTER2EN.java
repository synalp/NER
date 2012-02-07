package ester2;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.Mot;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import utils.ErrorsReporting;
import utils.FileUtils;

import cc.mallet.fst.CRF;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.types.ArraySequence;
import cc.mallet.types.Instance;
import com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl;

import corpus.text.TextSegments;
import corpus.text.TextSegments.segtypes;

/**
 * 
 * classe utilisee pour la reco des EN sur ESTER
 * 
 * 
 * 
 * @author xtof
 *
 */
public class ESTER2EN {
	ArrayList<TypedSegments> segsPerTRS = new ArrayList<TypedSegments>();
	// contient la racine du nom du fichier TRS
	ArrayList<String> TRSfile = new ArrayList<String>();

	// liste des ENs actives pour le prochain segment avec leur token de debut
	ArrayList<String> curens = new ArrayList<String>();
	ArrayList<Integer> endebs = new ArrayList<Integer>();

	// contient le seg time du prochain texte
	float prevdeb=-1, prevend=-1;

	// vars utilisees pour les listes de wikipedia:
	ArrayList<String[]> motsseqs = new ArrayList<String[]>();
	ArrayList<Integer> seqcat = new ArrayList<Integer>();
	ArrayList<String> cats = new ArrayList<String>();
	
	public int getNbTRSfichs() {
		return TRSfile.size();
	}
	public String getTRSfich(int i) {return TRSfile.get(i);}
	public TypedSegments getSegments4trs(int fich) {
		return segsPerTRS.get(fich);
	}
	public TypedSegments getSegments4trs(String trspath) {
		for (int i=0;i<TRSfile.size();i++) {
			if (trspath.contains(TRSfile.get(i))) return getSegments4trs(i);
		}
		return null;
	}
	
	public void print() {
		for (int i=0;i<TRSfile.size();i++) {
			System.out.println("fich "+i+": "+segsPerTRS.get(i).toString());
		}
	}
	
	String getWikicat(TypedSegments segs, int tok, String[] tokens, int word) {
		// TODO: comparer en commencant par les sequences les plus longues
		// pour le moment, je ne considère pas plus de 4 tokens
		ArrayList<String> obs = new ArrayList<String>();
		for (int i=word;i<tokens.length;i++) obs.add(tokens[i]);
		for (int i=0;i<4&&tok+i<segs.getNbTokens();i++) {
			String s = segs.getToken(tok+i);
			String[] ss = tokenize(s);
			for (String x : ss) obs.add(x);
		}
		for (int i=0;i<motsseqs.size();i++) {
			String[] mots = motsseqs.get(i);
			if (mots.length>obs.size()) continue;
			int j=0;
			for (j=0;j<mots.length;j++) {
				if (!mots[j].equals(obs.get(j))) break;
			}
			if (j>=mots.length) {
				// TODO: regrouper les segments contigus de la meme categorie !
				return cats.get(seqcat.get(i));
			}
		}
		// TODO: ajouter une catégorie partielle lorsque l'obs matche une partie seulement des mots des listes
		return "UNK";
	}
	void loadListes() {
		try {
			// ces listes son obtenues avec NomsPropres depuis Wikipedia
			BufferedReader f = new BufferedReader(new FileReader("listes.txt"));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				int i=s.indexOf("___");
				if (i>=0) {
					String cat = s.substring(0, i-1);
					int icat = cats.indexOf(cat);
					if (icat<0) {
						icat=cats.size();
						cats.add(cat);
					}
					String txt = s.substring(i+4);
					String[] mots = tokenize(txt);
					motsseqs.add(mots);
					seqcat.add(icat);
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i=0;i<cats.size();i++) {
			String s = cats.get(i);
			s=s.replace(' ', '_');
			cats.set(i, s);
		}
		System.out.println("listes loaded "+motsseqs.size());
	}
	
	void inferNE() {
		CRF crf=null;
		try {
			FileInputStream fin = new FileInputStream("malletmods.bin");
			ObjectInputStream oin = new ObjectInputStream(fin);
			crf = (CRF)oin.readObject();
			fin.close();
			System.out.println("CRF loaded");
		} catch (Exception e) {
			e.printStackTrace();
		}
		Pipe p = crf.getInputPipe();

		for (int trs=0;trs<TRSfile.size();trs++) {
			TypedSegments segs = segsPerTRS.get(trs);
			TypedSegments res = new TypedSegments();
			
			for (int tok=0;tok<segs.getNbTokens();tok++) {
				float debtok = segs.getTokenDebTime(tok);
				float endtok = segs.getTokenEndTime(tok);
				List<String[]> feats =calcFeats(segs,tok,"z");
				int newtok=-1;
				// nb de newtoks pour le oldtok courant:
				int tokn=0;
				for (String[] sf : feats) {
					boolean ok=true;
					for (String sx : sf) if (sx.trim().length()==0) {ok=false; break;}
					if (ok) {
						String s="";
						for (int i=0;i<sf.length-1;i++)
							s+=sf[i]+" ";
						s=s.trim();
						Iterator<Instance> it = new LineGroupIterator (new StringReader (s), Pattern.compile ("^$"), true);
						// bizarre: il n'y a pas besoin de passer par le pipe ? TODO: check this !
						//					Iterator<Instance> it0 = p.newIteratorFrom(it);
						Iterator<Instance> it0 = it;
						Instance i1 = it0.next();
						Instance i2 = crf.label(i1);
						ArraySequence seq = (ArraySequence)i2.getTarget();
						String lab = (String)seq.get(0);
						
						// on doit toujours avoir en 1ere feat = la forme !!
						newtok = res.addToken(sf[0]);
						if (tokn++==0) {
							if (debtok>=0) res.setTokenDebTime(newtok, debtok);
						}
						if (!lab.equals("TXT")) {
							System.out.println("lab found "+s+" "+lab+" "+seq.size());
							int i = res.getNbTokens()-1;
							// TODO: merge segments also !
							res.addTypedSegment(i, i, lab);
						}
					}
				}
				// pour le dernier newtok, set end time:
				if (newtok>=0&&endtok>=0) res.setTokenEndTime(newtok, endtok);
			}
			segsPerTRS.set(trs, res);
		}
	}

	/**
	 * sauve au format stm-ne pour scoring
	 * Utilisé seulement pour le test
	 * 
	 * utilise la liste des tokens dans segs: chaque segment typé a un type = named entity
	 * utilise aussi la liste des fichiers TRS associée
	 * 
	 * 
	 * @param fich
	 */
	void saveSTMNE(String fich) {
		try {
			//			PrintWriter f = new PrintWriter(new FileWriter(fich));
			PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fich), Charset.forName("ISO-8859-1")));
			for (int trs=0;trs<TRSfile.size();trs++) {
				// récupère le nom du fichier TRS du turn
				String s = TRSfile.get(trs);
				String nomtrs = s.replaceAll("^.*/([^/]*)\\.trs", "$1");

				TypedSegments segs = segsPerTRS.get(trs);
				int tokdebturn=0, tokendturn=-1;
				
//				int seg=0;
//				int[] tokseg = segs.getTokens4segment(seg);
//				System.out.println("save seg "+segs.getTypesForSegment(seg)[0]+" "+segs.getToken(tokseg[0])+".."+segs.getToken(tokseg[tokseg.length-1]));
				
				for (int turn=0;;turn++) {
					// cherche les limites du turn
					float turndeb=segs.getTokenDebTime(tokdebturn),turnend=-1f;
					for (int tok=tokdebturn;tok<segs.getNbTokens();tok++) {
						float time = segs.getTokenEndTime(tok);
						if (time>=0) {
							tokendturn=tok;
							turnend=time;
							break;
						}
					}
					if (tokendturn<tokdebturn) {
						System.out.println("ERROR missing time ! "+tokdebturn+" "+tokendturn+" "+segs.getNbTokens());
						tokendturn=segs.getNbTokens()-1;
					}
					
					StringBuilder turnTxt = new StringBuilder();
					ArrayList<Integer> segends = new ArrayList<Integer>();
					for (int curtok=tokdebturn;curtok<=tokendturn;curtok++) {
						ArrayList<String> segtypes = new ArrayList<String>();
						if (segs.isFirstTokenInaSegment(curtok, segtypes, segends)) {
							for (String t : segtypes)
								turnTxt.append("["+t+" ");
						}
						turnTxt.append(segs.getToken(curtok)+' ');
						for (;;)  {
							int i=segends.indexOf(curtok);
							if (i<0) break;
							segends.remove(i);
							turnTxt.append("] ");
						}
					}
					
					String txt = turnTxt.toString().trim();
					if (txt.length()>0) {
						f.println(nomtrs+" 1 UNK "+turndeb+" "+turnend+" <o,f3,male> "+txt);
// System.out.println("TURN "+txt);
					}
					if (tokendturn>=segs.getNbTokens()-1) {
						break;
					}
					tokdebturn=tokendturn+1;
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String cleanText(String s) {
		s=s.replaceAll("\\.\\.\\.", "");
		s=s.replaceAll("\\*", "");
		s=s.replaceAll("\\^", "");
		s=s.replaceAll("\\]", "");
		s=s.replaceAll("\n", "");
		return s.trim();
	}
	
	// dernier token ajouté: utile pour sauver les deb et endtime
	int tok=-1;
	// segs courant: doit etre cree en dehors de parse(), qui est recursif !
	TypedSegments segs = null;
	/**
	 * lit un fichier .TRS et mets le resultat dans segs
	 * @param node
	 */
	void parse(Node node) {
		if (node.getNodeType()==Node.TEXT_NODE) {
			String s = cleanText(node.getTextContent());
			if (s.length()>0) {
				tok = segs.addToken(s);
				if (prevdeb>=0) {
					// le token ajoute est un premier token d'un turn
					// on sauve le debut du turn
					segs.setTokenDebTime(tok, prevdeb);
					prevdeb=-1;
				}
				// debug
				/*
				System.out.println("SEG="+segs.getNbSegments());
				for (int z=0;z<curens.size();z++) {
					System.out.print(segs.getToken(endebs.get(z))+":"+curens.get(z)+" ");
				}
				System.out.println();
				 */
			}
		} else if (node.getNodeType()==Node.ELEMENT_NODE){
			if (node.getNodeName().equals("Turn")) {
				if (node.getAttributes().getNamedItem("startTime")!=null &&
						node.getAttributes().getNamedItem("endTime")!=null) {
					float deb = Float.parseFloat(node.getAttributes().getNamedItem("startTime").getNodeValue());
					float end = Float.parseFloat(node.getAttributes().getNamedItem("endTime").getNodeValue());
					if (tok>=0 && segs.getTokenEndTime(tok)<0) segs.setTokenEndTime(tok, prevend);
					// on prepare le turn suivant
					prevdeb=deb; prevend=end;
				}
			} else if (node.getNodeName().equals("Background")) {
				Node n = node.getAttributes().getNamedItem("time");
				if (n!=null) {
					float end = Float.parseFloat(n.getNodeValue());
					// on sauve la fin du turn precedent
					if (tok>=0 && segs.getTokenEndTime(tok)<0) segs.setTokenEndTime(tok, end);
					// on prepare le debut pour le token suivant
					prevdeb=end;
				}
			} else if (node.getNodeName().equals("Sync")) {
				float end = Float.parseFloat(node.getAttributes().getNamedItem("time").getNodeValue());
				// on sauve la fin du turn precedent
				if (tok>=0 && segs.getTokenEndTime(tok)<0) segs.setTokenEndTime(tok, end);
				// on prepare le debut pour le token suivant
				prevdeb=end;
			} else if (node.getNodeName().equals("Event")) {
				if (node.getAttributes().getNamedItem("type")!=null &&
						node.getAttributes().getNamedItem("type").getNodeValue().equals("entities")) {
					String ext = node.getAttributes().getNamedItem("extent").getNodeValue();
					String ne  = node.getAttributes().getNamedItem("desc").getNodeValue();
					// on peut avoir plusieurs segments recouvrants
					if (ext.equals("begin")) {
						curens.add(ne);
						endebs.add(segs.getNbTokens());
					} else if (ext.equals("end")) {
						// on cherche en partant de la fin car il pourrait y avoir plusieurs segments du meme type chevauchants
						int i;
						for (i=curens.size()-1;i>=0;i--) {
							if (curens.get(i).equals(ne)) {
								// safety check:
								int len = segs.getNbTokens()-endebs.get(i);
								if (len<0||len>20) {
									System.out.println("ERROR: REMOVING too long segment !"+len);
									curens.remove(i);
									endebs.remove(i);
									break;
								}
								
								if (endebs.get(i)>segs.getNbTokens()-1) {
									System.out.println("------------ERREUR limites "+endebs.get(i)+" "+(segs.getNbTokens()-1));
								} else 
									segs.addTypedSegment(endebs.get(i), segs.getNbTokens()-1, ne);
								//								System.out.println("add seg "+endebs.get(i)+" "+ (segs.getNbTokens()-1)+" "+segs.getSegment(segs.getNbSegments()-1)+" "+segs.getTypesForSegment(segs.getNbSegments()-1)[0]);
								if (len>10) {
									System.out.println("WARNING: long NE "+segs.getSegment(segs.getNbSegments()-1));
								}
								curens.remove(i);
								endebs.remove(i);
								break;
							}
						}
						if (i<0) {
							i=curens.size()-1;
							if (i<0) {
								System.out.println("ERROR: no start found for a given END "+ne);
								System.out.println("last segment: "+segs.getSegment(segs.getNbSegments()-1));
								// tant pis: on perd cette borne car il n'y a pas de correspondant !
							} else {
								System.out.println("ERROR assume "+ne+" = "+curens.get(i));
								if (endebs.get(i)>segs.getNbTokens()-1) {
									System.out.println("++++++++++ERREUR limites "+endebs.get(i)+" "+(segs.getNbTokens()-1));
									System.out.println("zz "+curens.size()+" "+endebs.size());
								} else 
									segs.addTypedSegment(endebs.get(i), segs.getNbTokens()-1, ne);
								curens.remove(i);
								endebs.remove(i);
							}
						}
					}
				}
			}
		}
		if (node.hasChildNodes())  {
			Node nextFils = node.getFirstChild();
			while (nextFils != null) {
				parse(nextFils);
				nextFils = nextFils.getNextSibling();
			}
		}
	}

	public void loadTrain() {
		DocumentBuilderFactoryImpl factory = new DocumentBuilderFactoryImpl();
		factory.setValidating(false);
		factory.setNamespaceAware(false);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId){
					return new InputSource(new ByteArrayInputStream(new byte[0]));
				}
			});

			{
				File listfs = new File("train.trsl");
				BufferedReader fl = new BufferedReader(new FileReader(listfs));
				for (;;) {
					String s = fl.readLine();
					if (s==null) break;
					s=s.trim();
					if (s.length()>0) {
						File f = new File(s);
						Document doc = builder.parse(f);
						segs = new TypedSegments();
						segsPerTRS.add(segs);
						parse(doc);
						TRSfile.add(s);
					}
				}
				fl.close();
				System.out.println("loadtrain end "+segsPerTRS.size());
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * charge un fichier TRS en mettant une phrase par TURN
	 * Le résultat se trouve dans les segments 
	 * @param trslist
	 */
	public void loadTRS(String trslist) {
		DocumentBuilderFactoryImpl factory = new DocumentBuilderFactoryImpl();
		factory.setValidating(false);
		factory.setNamespaceAware(false);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId){
					return new InputSource(new ByteArrayInputStream(new byte[0]));
				}
			});

			File listfs = new File(trslist);
			BufferedReader fl = new BufferedReader(new FileReader(listfs));
			for (;;) {
				String onetrsfile = fl.readLine();
				if (onetrsfile==null) break;
				onetrsfile=onetrsfile.trim();
				if (onetrsfile.length()>0) {
					File f = new File(onetrsfile);
					Document doc = builder.parse(f);
					TRSfile.add(onetrsfile);
					System.out.println("trs "+TRSfile.get(TRSfile.size()-1));
					tok=-1; prevdeb=prevend=-1;
					segs = new TypedSegments();
					segsPerTRS.add(segs);
					parse(doc);
					if (tok>=0 && segs.getTokenEndTime(tok)<0) segs.setTokenEndTime(tok, prevend);
					// just for check: 5 premiers EN lues
					for (int i=0;i<5;i++) {
						System.out.println("SEG TYPE: "+segs.getTypesForSegment(i)[0]+" : "+segs.getSegment(i));
					}
				}
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String[] tokenize(String s) {
		s=s.replace('^', ' ').replace('_', ' ').replace('=', ' ');
		s=s.replaceAll("\\(", "").replaceAll("\\)","").replaceAll("\\*", "");
		String[] ss = s.split(" ");
		return ss;
	}

	/**
	 * 1 token = 1 mot séparé par des espaces
	 */
	private String[] calcFeatsOneWord(TypedSegments segs, int tok, String[] tokens, int word, String classe) {
		ArrayList<String> res = new ArrayList<String>();

		/*
		String[] ctxt = segs.getContextOfToken(tok, 3);
		String[] lctxt = tokenize(ctxt[0]);
		String[] rctxt = tokenize(ctxt[1]);
		String[] utt = Arrays.copyOf(lctxt, lctxt.length+rctxt.length+tokens.length);
		for (int i=0;i<tokens.length;i++) utt[lctxt.length+i]=tokens[i];
		for (int i=0;i<rctxt.length;i++) utt[lctxt.length+tokens.length+i]=rctxt[i];
		int wpos = lctxt.length+word;
		 */

		// le mot lui-meme : OBLIGATOIRE sinon on ne peut pas reconstruire les tokens ensuite
		String lemot = tokens[word].trim();
		if (lemot.length()==0) return null;
		res.add(lemot);

		if (false) {
			// le mot avant
			String prevmot="UNK";
			if (word>0) {
				String s = tokens[word-1].trim();
				if (s.length()>0) prevmot=s;
			} else if (tok>0) {
				String ptok = segs.getToken(tok-1);
				String[] ptoks = tokenize(ptok);
				if (ptoks.length>0) {
					String s = ptoks[ptoks.length-1].trim();
					if (s.length()>0) prevmot=s;
				}
			}
			res.add(prevmot);
		}
		
		// la catégorie de Wikipedia si elle existe
		res.add(getWikicat(segs,tok,tokens,word));
		
		// majuscules: 0=sans 1=1st 2=plusieurs
		int maj=0;
		if (Character.isUpperCase(lemot.charAt(0))) {
			maj=1;
			for (int j=1;j<lemot.length();j++) {
				if (Character.isUpperCase(lemot.charAt(j))) {
					maj=2;
					break;
				}
			}
		}
		res.add(""+maj);

		// inutile de calculer les mots avants ou apres car le CRF s'en charge
		// le mot avant
		//		if (wpos-1>=0) res.add("WM1"+utt[wpos-1]);
		// le mot apres
		//		if (wpos+1<utt.length) res.add("WP1"+utt[wpos+1]);

		res.add(classe);
		return res.toArray(new String[res.size()]);
	}

	List<String[]> calcFeats(TypedSegments segs, int tok, String classe) {
		// il faut revenir a une tokenisation selon les espaces, qui sera utilisee au test
		ArrayList<String[]> res = new ArrayList<String[]>();
		String stok = segs.getToken(tok);
		String[] tokens = tokenize(stok);
		if (tokens.length==0) {
			// ???
		} else if (tokens.length==1) {
			String[] x = calcFeatsOneWord(segs,tok,tokens,0,classe);
			if (x!=null) res.add(x);
		} else {
			boolean BIO=false;
			if (BIO) {
				// solution avec segmentation

				// si il y a ainsi plusieurs mots dans un token, alors il faut associer au premier mot
				// les classes de type START* et au dernier mot les classes de type END*
				// et aux mots intermediaires la classe TXT
				String[] classes = classe.split("_");
				String clfirst = "", cllast="";
				for (String c : classes) {
					if (c.startsWith("START")) clfirst+=c+"_";
					if (c.startsWith("END")) cllast+=c+"_";
				}
				if (clfirst.length()>0) clfirst=clfirst.substring(0,clfirst.length()-1);
				else clfirst="TXT";
				if (cllast.length()>0) cllast=cllast.substring(0,cllast.length()-1);
				else cllast="TXT";

				String[] x = calcFeatsOneWord(segs,tok,tokens,0,clfirst); if (x!=null) res.add(x);
				for (int i=1;i<tokens.length-1;i++) {
					x = calcFeatsOneWord(segs,tok,tokens,i,"TXT"); if (x!=null) res.add(x);
				}
				x = calcFeatsOneWord(segs,tok,tokens,tokens.length-1,cllast); if (x!=null) res.add(x);
			} else {
				// solution sans segmentation, mais lorsque plusieurs classes = classe jointe

				for (int i=0;i<tokens.length;i++) {
					String[] x = calcFeatsOneWord(segs,tok,tokens,i,classe); if (x!=null) res.add(x);
				}
			}
		}
		return res;
	}

	int computeFeats(String outfile, int nmax, int tokoffset) {
		int ntot=0;
		try {
			PrintWriter f = new PrintWriter(new FileWriter(outfile));
			for (int trs=0;trs<segsPerTRS.size();trs++) {
				TypedSegments segs = segsPerTRS.get(trs);
				for (int tokidx=0;tokidx<segs.getNbTokens();tokidx++) {
					if (ntot++<tokoffset) continue;
					if (nmax>=0&&ntot-tokoffset>nmax) return ntot;
					System.err.println(tokidx+"/"+segs.getNbTokens());
					String[] x = segs.getTypesForToken(tokidx);
					String classe = "";
					if (x.length>1) {
						for (String xx: x) classe+=xx+"_";
						classe=classe.substring(0, classe.length()-1);
					} else if (x.length==1) classe=x[0];
					else classe="TXT";

					List<String[]> feats =calcFeats(segs,tokidx,classe);
					for (String[] sf : feats) {
						boolean ok=true;
						for (String sx : sf) if (sx.trim().length()==0) {ok=false; break;}
						if (ok) {
							String s="";
							for (String sx : sf) s+=sx+" ";
							s=s.trim();
							f.println(s);
						}
					}
				}
				f.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ntot;
	}

	void allFeats() {
		computeFeats("train.feats", -1, 0);
	}

	/**
	 * create the feature file for the StanfordNER
	 * 
	 * @param gs
	 * @param en
	 */
	public static void saveGroups(java.util.List<DetGraph> gs, String en) {
		try {
			PrintWriter fout = FileUtils.writeFileUTF("groups."+en+".tab");
			for (int i=0;i<gs.size();i++) {
				DetGraph g = gs.get(i);
				int nexinutt=0;
				for (int j=0;j<g.getNbMots();j++) {
					nexinutt++;
					
					// calcul du label
					String lab = "NO";
					int[] groups = g.getGroups(j);
					if (groups!=null)
						for (int gr : groups) {
							if (g.groupnoms.get(gr).equals(en)) {
								int debdugroupe = g.groups.get(gr).get(0).getIndexInUtt()-1;
								if (debdugroupe==j) lab = en+"B";
								else lab = en+"I";
								break;
							}
						}
					fout.println(g.getMot(j).getForme()+"\t"+g.getMot(j).getPOS()+"\t"+lab);
				}
				if (nexinutt>0)
					fout.println();
			}
			fout.close();
			ErrorsReporting.report("groups saved in groups.*.tab");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void saveGroups(String xmllist, String en) {
		try {
			GraphIO gio = new GraphIO(null);
			PrintWriter fout = FileUtils.writeFileUTF("groups."+en+".tab");
			BufferedReader fl = new BufferedReader(new FileReader(xmllist));
			for (;;) {
				String s = fl.readLine();
				if (s==null) break;
				List<DetGraph> gs = gio.loadAllGraphs(s);
				for (int i=0;i<gs.size();i++) {
					DetGraph g = gs.get(i);
					int nexinutt=0;
					for (int j=0;j<g.getNbMots();j++) {
						nexinutt++;
						
						// calcul du label
						String lab = "NO";
						int[] groups = g.getGroups(j);
						if (groups!=null)
							for (int gr : groups) {
								if (g.groupnoms.get(gr).startsWith(en)) {
									int debdugroupe = g.groups.get(gr).get(0).getIndexInUtt()-1;
									if (debdugroupe==j) lab = en+"B";
									else lab = en+"I";
									break;
								}
							}
						fout.println(g.getMot(j).getForme()+"\t"+g.getMot(j).getPOS()+"\t"+"NOCL\t"+lab);
					}
					if (nexinutt>0)
						fout.println();
				}
			}
			fout.close();
			fl.close();
			ErrorsReporting.report("groups saved in groups.*.tab");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void mergeENs(String xmllist, String[] ens) {
		final int recol=2;
		try {
			GraphIO gio = new GraphIO(null);
			BufferedReader fl = new BufferedReader(new FileReader(xmllist));
			BufferedReader fens[] = new BufferedReader[ens.length];
			for (int i=0;i<ens.length;i++) fens[i]=new BufferedReader(new FileReader("test."+ens[i]+".log"));
			int debin[] = new int[ens.length];
			Arrays.fill(debin, -1);
			for (;;) {
				String gsfilename = fl.readLine();
				if (gsfilename==null) break;
				List<DetGraph> gs = gio.loadAllGraphs(gsfilename);
				for (int i=0;i<gs.size();i++) {
					DetGraph g = gs.get(i);
					g.clearGroups();
					for (int j=0;j<g.getNbMots();j++) {
						for (int k=0;k<ens.length;k++) {
							String sen = fens[k].readLine();
							String[] ss = sen.split("\t");
							if (ss[recol].equals(ens[k]+'B')) {
								if (debin[k]>=0) {
									g.addgroup(debin[k], j-1, ens[k]);
								}
								debin[k]=j;
							} else if (ss[recol].equals(ens[k]+'I')) {
								if (debin[k]<0) {
									System.err.println("warning: enIn without Begin "+ens[k]);
									debin[k]=j;
								}
							} else {
								if (debin[k]>=0) {
									g.addgroup(debin[k], j-1, ens[k]);
								}
								debin[k]=-1;
							}
						}
					}
					for (int k=0;k<ens.length;k++) {
						if (debin[k]>=0) {
							g.addgroup(debin[k], g.getNbMots()-1, ens[k]);
							debin[k]=-1;
						}
					}
					if (g.getNbMots()>0) {
						for (int k=0;k<ens.length;k++) {
							String sen = fens[k].readLine();
							if (sen.trim().length()>0) System.err.println("warning: decalage des phrases ? "+sen);
						}
					}
				}
				gio.save(gs, gsfilename+".merged.xml");
			}
			for (int i=0;i<ens.length;i++) fens[i].close();
			fl.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	int[] getGraphMot(List<DetGraph> gs, int charidx) {
		// cherche le graphe
		for (int i=0;i<gs.size();i++) {
			DetGraph g = gs.get(i);
			long gdeb = g.getMot(0).getDebPosInTxt();
			long gfin = g.getMot(g.getNbMots()-1).getEndPosInTxt();
			if (charidx<gdeb) {
				System.out.println("ERROR char non trouve ! "+charidx);
				System.exit(1);
				return null;
			}
			if (charidx<=gfin) {
				// cherche mot
				for (int j=0;j<g.getNbMots();j++) {
					if (g.getMot(j).getEndPosInTxt()>=charidx) {
						final int[] r = {i,j};
						return r;
					}
				}
				System.out.println("ERROR char fin non trouve !");
				System.exit(1);
				return null;
			}
		}
		System.out.println("ERROR char graph non trouve !");
		System.exit(1);
		return null;
	}

	public List<DetGraph> toGraphs() {
		ArrayList<DetGraph> res = new ArrayList<DetGraph>();
		for (int i=0;i<getNbTRSfichs();i++) {
			ArrayList<DetGraph> graphsOneTrs = new ArrayList<DetGraph>();

			TextSegments segs4syntax = new TextSegments();
			ArrayList<Integer> tokendebs = new ArrayList<Integer>();
			ArrayList<Integer> tokenends = new ArrayList<Integer>();
			TypedSegments segs = getSegments4trs(i);
			System.out.println("trs "+i+" "+segs.getNbTokens());

			// on concatene tous les tokens d'un fichier TRS
			// on garde la position de chaque token par rapport a la String immutable stockee dans segs4syntax
			StringBuilder sb = new StringBuilder();
			for (int j=0;j<segs.getNbTokens();j++) {
				String tok = segs.getToken(j);
				tokendebs.add(sb.length());
				sb.append(tok);
				tokenends.add(sb.length());
				sb.append(' ');
			}
			final String immutablesource = sb.toString();
			segs4syntax.addOneString(immutablesource);

			// on tokenize+segmente, en conservant les segments associes a chaque mot pour chaque phrase
			ArrayList<Integer> uttends = new ArrayList<Integer>();
			final String eos = ".!?";
			TextSegments allmots=segs4syntax.tokenizeBasic(0);
			allmots.tokenizePonct();
			for (int k=0;k<allmots.getNbSegments();k++) {
				if (allmots.getSegmentType(k)==segtypes.ponct) {
					String s = allmots.getSegment(k).trim();
					if (eos.indexOf(s.charAt(0))>=0) {
						// new utt !
						uttends.add(k);
					}
				}
			}
			// derniere phrase si sans ponctuation finale
			if (uttends.size()==0||uttends.get(uttends.size()-1)<allmots.getNbSegments()) uttends.add(allmots.getNbSegments());

			// on cree les graphes
			int deb=0;
			for (int j=0;j<uttends.size();j++) {
				DetGraph g = new DetGraph();
				g.comment=getTRSfich(i);
				for (int k=deb, l=0;k<=uttends.get(j)&&k<allmots.getNbSegments();k++,l++) {
					String s=allmots.getSegment(k).trim();
					Mot m=new Mot(s, s, "unk");
					m.setPosInTxt(allmots.getSegmentStartPos(k), allmots.getSegmentEndPos(k));
					g.addMot(l, m);
				}
				graphsOneTrs.add(g);
				deb=uttends.get(j)+1;
				if (deb>=allmots.getNbSegments()) break;
			}
			res.addAll(graphsOneTrs);
			System.out.println("graphs crees "+graphsOneTrs.size());

			// on ajoute les groupes pour les ENs
			for (int j=0;j<segs.getNbSegments();j++) {
				int[] toks = segs.getTokens4segment(j);
				if (toks.length>0) {
					Arrays.sort(toks);
					int tdeb = tokendebs.get(toks[0]);
					int tfin = tokenends.get(toks[toks.length-1]);
					int[] gmd = getGraphMot(graphsOneTrs,tdeb);
					int[] gme = getGraphMot(graphsOneTrs,tfin);
					if (gmd[0]!=gme[0]) {
						// ceci arrive lorsqu'on a une EN qui contient des marques de ponctuation finale
						// il s'agit donc d'une erreur de segmentation en phrases !

						System.out.println("ERROR "+Arrays.toString(gmd)+" "+Arrays.toString(gme)+" "+tdeb+" "+tfin);
						System.out.println(immutablesource.substring(tdeb,tfin));
						System.out.println("en context: "+immutablesource.substring(tdeb<10?0:tdeb-10, tfin+10));
						{
							Mot m = graphsOneTrs.get(gmd[0]).getMot(gmd[1]);
							System.out.println("nmots "+graphsOneTrs.get(gmd[0]).getNbMots());
							System.out.println("bad "+m.getDebPosInTxt()+" "+m.getEndPosInTxt()+" : "+m.getForme()+" : "+immutablesource.substring((int)m.getDebPosInTxt(),(int)m.getEndPosInTxt()));
						}
						{
							System.out.println("graph suivant:");
							Mot m = graphsOneTrs.get(gme[0]).getMot(gme[1]);
							System.out.println("nmots "+graphsOneTrs.get(gme[0]).getNbMots());
							System.out.println("bad "+m.getDebPosInTxt()+" "+m.getEndPosInTxt()+" : "+m.getForme()+" : "+immutablesource.substring((int)m.getDebPosInTxt(),(int)m.getEndPosInTxt()));
						}
						// TODO il faudrait empecher la segmentation ici, mais pour le moment, je ne tiens pas compte de cette EN 
						continue;
					}
					DetGraph g = graphsOneTrs.get(gmd[0]);
					g.addgroup(gmd[1], gme[1], segs.getTypesForSegment(j)[0]);
				}
			}
		}
		return res;
	}
	
	public static void main(String args[]) {
		ESTER2EN m =new ESTER2EN();
		if (args[0].equals("-train")) {
			m.loadTrain();
			m.loadListes();
			m.allFeats();
		} else if (args[0].equals("-trs2stmne")) {
			String trslist = args[1];
			m.loadTRS(trslist);
			m.saveSTMNE("yy.stm-ne");
		} else if (args[0].equals("-mergeens")) {
			String xmllist = args[1];
			mergeENs(xmllist,Arrays.copyOfRange(args, 2, args.length));
		} else if (args[0].equals("-saveNER")) {
			System.out.println("saveNER");
			String xmllist = args[1];
			String en = args[2];
			saveGroups(xmllist, en);
		} else if (args[0].equals("-test")) {
			System.out.println("test0: save feats for all test");
			m.loadTRS("dev.trsl");
			m.loadListes();
			System.out.println("test1: infer types and save stm-ne");
			m.inferNE();
			m.saveSTMNE("yy.stm-ne");
		} else if (args[0].equals("-trs2xml")) {
			m.loadTRS(args[1]);
			List<DetGraph> gs = m.toGraphs();
			GraphIO gio = new GraphIO(null);
			gio.save(gs, "output.xml");
		} else if (args[0].equals("-load")) {
			m.loadTRS(args[1]);
			m.print();
		}
	}
}