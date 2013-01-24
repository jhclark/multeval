package multeval.output;

import java.io.PrintStream;

import multeval.ResultsManager;
import multeval.ResultsManager.Type;

public class AsciiTable {

  public void write(ResultsManager results, PrintStream out) {
		
	String[] columns = new String[results.metricNames.length+1];
	columns[0] = String.format("n=%d", results.numOptRuns);
	for(int i=0; i<results.metricNames.length; i++) {
		columns[i+1] = results.metricNames[i] +" (s_sel/s_opt/p)";
	}
	
	print(out, columns);
	
    String[] metrics = results.metricNames;
    String[] systems = results.sysNames;
    int sysCount = systems.length;
    for(int iSys=0; iSys<sysCount; iSys++) {
    	columns[0] = systems[iSys]; // system name
    	
    	for(int iMetric=0; iMetric<metrics.length; iMetric++) {
        	double avg = results.get(iMetric, iSys, Type.AVG); // avg metric score
            double sSel = results.get(iMetric, iSys, Type.RESAMPLED_STDDEV_AVG);
            double sTest = results.get(iMetric, iSys, Type.STDDEV);
            
    	    if (iSys == 0) {
                // baseline has no p-value
                if (results.numOptRuns > 1) {
                    columns[iMetric+1] = String.format("%2.1f (%.1f/%.1f/-)", avg, sSel, sTest);
                } else {
                    columns[iMetric+1] = String.format("%2.1f (%.1f/*/-)", avg, sSel);
                }
            } else {
                // TODO: Just show improvements over baseline?
                double p = results.get(iMetric, iSys, Type.P_VALUE);
                if (results.numOptRuns > 1) {
                    columns[iMetric+1] = String.format("%2.1f (%.1f/%.1f/%.2f)", avg, sSel, sTest, p);
                } else {
                    columns[iMetric+1] = String.format("%2.1f (%.1f/*/**)", avg, sSel);
                }
            }
    	}
    	print(out, columns);
    }
    if (results.numOptRuns < 2) {
            out.println("  *  Indicates no estimate of optimizer instability due to single optimizer run. Consider multiple optimizer runs.");
        if (sysCount > 1) {
            out.println("  ** Indicates no p-value due to single optimizer run. Consider multiple optimizer runs.");
        }
    }
    out.flush();
  }

	private void print(PrintStream out, String[] columns) {
		out.print(String.format("%-15s", columns[0]));
		for(int i=1; i<columns.length; i++) {
			out.print(String.format("%-23s", columns[i])); // 23 not 21 due to metric names
		}
		out.println();
	}
}
