package multeval.util;

import com.google.common.base.Preconditions;

public class ArrayUtils {

	/**
	 * Add parallel arrays.
	 * 
	 * @param summedStats
	 * @param ds
	 */
	public static void plusEquals(double[] dest, double[] arg) {
		Preconditions.checkArgument(dest.length == arg.length, "Arrays not parallel");
		for(int i = 0; i<dest.length; i++) {
			dest[i] += arg[i];
		}
	}

}
