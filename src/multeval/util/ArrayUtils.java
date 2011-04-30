package multeval.util;

import com.google.common.base.Preconditions;

public class ArrayUtils {

	/**
	 * Add parallel arrays.
	 * 
	 * @param summedStats
	 * @param ds
	 */
	public static void plusEquals(float[] dest, float[] arg) {
		Preconditions.checkArgument(dest.length == arg.length, "Arrays not parallel");
		for(int i = 0; i<dest.length; i++) {
			dest[i] += arg[i];
		}
	}
	
	/**
	 * Add parallel arrays.
	 * 
	 * @param summedStats
	 * @param ds
	 */
	public static void plusEquals(int[] dest, int[] arg) {
		Preconditions.checkArgument(dest.length == arg.length, "Arrays not parallel");
		for(int i = 0; i<dest.length; i++) {
			dest[i] += arg[i];
		}
	}

	public static int[] toIntArray(float[] suffStats) {
		int[] result = new int[suffStats.length];
		for(int i=0; i<suffStats.length; i++) {
			result[i] = (int) suffStats[i];
		}
		return result;
	}

	public static float[] toFloatArray(int[] intSuffStats) {
		float[] result = new float[intSuffStats.length];
		for(int i=0; i<intSuffStats.length; i++) {
			result[i] = (float) intSuffStats[i];
		}
		return result;
	}

	public static int indexOf(double[] arr, double val) {
		for(int i=0; i<arr.length; i++) {
			if(arr[i] == val) {
				return i;
			}
		}
		return -1;
	}

}
