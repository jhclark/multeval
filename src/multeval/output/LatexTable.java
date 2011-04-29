package multeval.output;

import java.io.PrintWriter;

import multeval.ResultsManager;
import multeval.ResultsManager.Type;

public class LatexTable {

	public void write(ResultsManager results, PrintWriter out) {
		out.println("\\begin{table}[htb]");
		out.println("\\begin{center}");
		out.println("\\begin{footnotesize}");
		out.println("\\begin{tabular}{|l|l|l|l|l|l|}");
		out.println("\\hline");
		out.println("\\bf Metric & \\bf System & \\bf Avg & \\bf $\\overline{s}_{\\text{sel}}$ &\\bf & \\bf $s_{\\text{Test}}$ & \\bf $p$-value \\\\");
		out.println("\\hline");
		// \multicolumn{6}{|l|}{BTEC Zh-En} \\

		String[] metrics = results.metricNames;
		String[] systems = results.sysNames;
		int sysCount = systems.length;
		for (int iMetric = 0; iMetric < metrics.length; iMetric++) {
			String metricName = metrics[iMetric];

			out.println("\\multirow{" + sysCount + "}{*}{" + metricName + " $\\uparrow$}");
			for (int iSys = 0; iSys < sysCount; iSys++) {
				String sysName = systems[iSys];
				double avg = results.get(iMetric, iSys, Type.AVG);
				double sSel = results.get(iMetric, iSys, Type.RESAMPLED_MEAN_AVG);
				double sTest = results.get(iMetric, iSys, Type.STDDEV);
				if (iSys == 0) {
					// baseline has no p-value
					out.println(String.format("& %s & %.1f & %.1f & %.1f & - \\\\", sysName, avg,
							sSel, sTest));
				} else {
					double p = results.get(iMetric, iSys, Type.P_VALUE);
					out.println(String.format("& %s & %.1f & %.1f & %.1f & %.1f \\\\", sysName,
							avg, sSel, sTest, p));
				}
			}
			out.println("\\hline");
		}

		out.println("\\end{tabular}");
		out.println("\\end{footnotesize}");
		out.println("\\end{center}");
		// out.println("\\vspace{-.2cm}");
		out.println("\\caption{\\label{tab:scores} Metric scores for all systems: INCLUDE METRIC VERSIONS ETC. CITATIONS ON DEMAND. Note p-values are relative to baseline.}");
		out.println("\\end{table}");
		out.flush();
	}
}
