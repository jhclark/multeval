package multeval.util;

import java.util.*;

public class MathUtils {
  public static final double SQRT_2 = Math.sqrt(2.0);

  public static double factorial(int val) {
    double result = 1.0;
    for(int i = 2; i <= val; i++) {
      result *= i;
    }
    return result;
  }

  public static double gamma(int alpha) {
    return factorial(alpha - 1);
  }

  public static double average(List<Double> observations) {
    double total = 0.0;
    for(double d : observations) {
      total += d;
    }
    return total / observations.size();
  }

  public static double average(double[] data) {
    double total = 0.0;
    for(double d : data) {
      total += d;
    }
    return total / data.length;
  }

  public static double variance(List<Double> observations) {
    double mean = average(observations);
    double varSum = 0.0;
    for(double d : observations) {
      varSum += (d - mean) * (d - mean);
    }
    return varSum / observations.size();
  }

  public static double variance(double[] observations) {
    double mean = average(observations);
    double varSum = 0.0;
    for(double d : observations) {
      varSum += (d - mean) * (d - mean);
    }
    // use bessel's correction
    return varSum / (observations.length - 1);
  }

  // from http://introcs.cs.princeton.edu/21function/ErrorFunction.java.html
  // fractional error in math formula less than 1.2 * 10 ^ -7.
  // although subject to catastrophic cancellation when z in very close to 0
  // from Chebyshev fitting formula for erf(z) from Numerical Recipes, 6.2
  public static double erf(double z) {
    double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

    // use Horner's method
    double ans = 1
        - t
        * Math
            .exp(-z
                * z
                - 1.26551223
                + t
                * (1.00002368 + t
                    * (0.37409196 + t
                        * (0.09678418 + t
                            * (-0.18628806 + t
                                * (0.27886807 + t
                                    * (-1.13520398 + t * (1.48851587 + t * (-0.82215223 + t * (0.17087277))))))))));
    if (z >= 0)
      return ans;
    else
      return -ans;
  }

  public static double min(double[] samples) {
    double result = Double.MAX_VALUE;
    for(double d : samples)
      result = Math.min(result, d);
    return result;
  }

  public static double max(double[] samples) {
    double result = Double.MIN_VALUE;
    for(double d : samples)
      result = Math.max(result, d);
    return result;
  }

  public static double stddev(double[] observations) {
    return Math.sqrt(variance(observations));
  }

  public static int medianIndexInSorted(double[] arr) {
    // TODO: Better tie breaking?
    Arrays.sort(arr);
    if (arr.length < 2) {
      return 0;
    } else if (arr.length % 2 == 0) {
      // return the higher of the 2 midpoints
      return (arr.length / 2);
    } else {
      // return exactly the midpoint
      return ((arr.length - 1) / 2);
    }
  }

  public static int medianIndex(double[] arr) {

    if (arr.length < 2) {
      return 0;
    } else {
      double[] copy = Arrays.copyOf(arr, arr.length);
      Arrays.sort(copy);

      final int copyIdx;
      if (copy.length % 2 == 0) {
        // return the higher of the 2 midpoints
        copyIdx = (copy.length / 2);
      } else {
        // return exactly the midpoint
        copyIdx = ((copy.length - 1) / 2);
      }
      double median = copy[copyIdx];
      int arrIdx = ArrayUtils.indexOf(arr, median);
      return arrIdx;
    }
  }
}
