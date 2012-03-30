package etape.unsup;

/**
 * 
 * @author cerisara
 *
 */
public class SparseRules {

	NECorpus corp;
	public SparseRules(NECorpus corp) {
		this.corp=corp;
	}
	
	public static void main(String args[]) throws Exception {
		NECorpus corp = new NECorpus();
		corp.load("/home/xtof/corpus/ETAPE2/Dom/Etape/quaero-ne-normalized/19990617_1900_1920_inter_fm_dga.ne");
		SparseRules m = new SparseRules(corp);
//		m.deterministic();
		corp.save("rec.ne");
	}
}
