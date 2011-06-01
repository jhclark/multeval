package multeval.util;

import java.util.*;

import multeval.metrics.*;

import com.google.common.base.*;

public class SuffStatUtils {
  public static SuffStats<?> sumStats(List<SuffStats<?>> suffStats) {

    Preconditions.checkArgument(suffStats.size() > 0, "Need more than zero data points.");
    SuffStats<?> summedStats = suffStats.get(0).create();

    for(SuffStats<?> dataPoint : suffStats) {
      summedStats.add(dataPoint);
    }
    return summedStats;
  }
}
