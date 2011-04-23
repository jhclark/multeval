package multeval.output;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class LatexTable {
	
	List<String> metrics;
	List<String> systems;
	Map<String, Map<String, float[]>> systemMetricData;
	
	public void add(String system, String metric, float avg, float sTest, float sOptTest, float p) {
		
	}

	public void write(PrintWriter out) {
		out.println("\\begin{table}[htb]");
		out.println("\\begin{center}");
		out.println("\\begin{footnotesize}");
		out.println("\\begin{tabular}{|l|l|l|l|l|l|}");
		out.println("\\hline");
		out.println("\\bf Metric & \\bf System & \\bf Avg & \\bf $\\overline{s}_{\\text{Test}}$ &\\bf & \\bf $s_{\\text{OptTest}}$ & \\bf $p$-value \\\\");
		out.println("\\hline");
//		              \multicolumn{6}{|l|}{BTEC Zh-En} \\
		for(String metric : metrics) {
			int sysCount = systems.size();
		
			out.println("\\multirow{"+sysCount+"}{*}{"+metric+" $\\uparrow$}");
			for(String system : systems) {
				float[] data = systemMetricData.get(system).get(metric);
				float avg = data[0];
				float sTest = data[1];
				float sOptTest = data[2];
				float p = data[3];
		        out.println(String.format("& %s & %.1f & %.1f & %.1f & %.1f \\\\", system, avg, sTest, sOptTest, p));
			}
			out.println("\\hline");
		}

      out.println("\\end{tabular}");
      out.println("\\end{footnotesize}");
      out.println("\\end{center}");
      //out.println("\\vspace{-.2cm}");
      out.println("\\caption{\\label{tab:scores} Metric scores for all systems: INCLUDE METRIC VERSIONS ETC. CITATIONS ON DEMAND}");
      out.println("\\end{table}");
	}
}
