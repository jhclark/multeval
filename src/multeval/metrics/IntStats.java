package multeval.metrics;

import java.util.*;

import multeval.util.*;

public class IntStats extends SuffStats<IntStats> {
  public final int[] arr;

  public IntStats(int size) {
    this.arr = new int[size];
  }

  @Override
  public void add(IntStats other) {
    ArrayUtils.plusEquals(this.arr, other.arr);
  }

  @Override
  public SuffStats<IntStats> create() {
    return new IntStats(arr.length);
  }

  @Override
  public String toString() {
    return Arrays.toString(arr);
  }
}
