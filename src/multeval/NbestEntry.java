package multeval;

import java.util.Iterator;

import com.google.common.base.Splitter;

public class NbestEntry {
	public int sentId;
	public int origRank;
	public String hyp;
	public String feats;
	public float total;
	public double[] metricScores;
	
	public static NbestEntry parse(String cdecStr) {
		
		NbestEntry result = new NbestEntry();
		
		//0 ||| the transactions with the shares of čez achieved almost half of the normal agenda . ||| Glue=8 LanguageModel=-39.2525 PassThrough=1 PhraseModel_0=2.18572 PhraseModel_1=13.4858 PhraseModel_2=4.24232 WordPenalty=-6.51442 ContextCRF=-35.9812 crf.ContentWordCount=7 crf.NonContentWordCount=26 crf.StopWordCount=7 crf.NonStopWordCount=26 ||| -28.842
		Iterator<String> columns = Splitter.on(" ||| ").split(cdecStr).iterator();
		result.sentId = Integer.parseInt(columns.next());
		result.hyp = columns.next();
		result.feats = columns.next();
		result.total = Float.parseFloat(columns.next());
		
		return result;
	}

    public String toString(String[] metricNames) {
		StringBuilder metricString = new StringBuilder();
		if(metricScores != null) {
		    metricString.append(" ||| ");
	
		    for(int iMetric=0; iMetric<metricNames.length; iMetric++) {
		    	metricString.append(String.format(metricNames[iMetric] + "=" + metricScores[iMetric]));
		    }
		}
	
		return String.format("%d ||| %s ||| %s ||| %f ||| origRank=%d", sentId, hyp, feats, total, origRank) + metricString.toString();
    }
}
