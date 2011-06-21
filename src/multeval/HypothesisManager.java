package multeval;

import java.io.*;
import java.util.*;

import multeval.util.*;

import com.google.common.base.*;
import com.google.common.io.*;

/** System index 0 is the baseline system
 * 
 * @author jon */
public class HypothesisManager {

  // indices: iHyp, iRef
  private List<List<String>> allRefs = new ArrayList<List<String>>();

  // indices: iSys, oOpt, iHyp
  private List<List<List<String>>> allHyps = new ArrayList<List<List<String>>>();

  private int numHyps = -1;
  private int numRefs = -1;
  private int numOptRuns = -1;
  private int numSystems = -1;

  public int getNumHyps() {
    return numHyps;
  }

  public int getNumOptRuns() {
    return numOptRuns;
  }

  public int getNumRefs() {
    return numRefs;
  }

  public void loadData(String[] hypFilesBaseline, String[][] hypFilesBySys, String[] refFiles) throws IOException {

    // first, load system hypotheses
    numSystems = hypFilesBySys.length + 1; // include baseline
    allHyps.clear();
    loadHyps(hypFilesBaseline, "baseline");
    for(int iSys = 0; iSys < hypFilesBySys.length; iSys++) {
      loadHyps(hypFilesBySys[iSys], "" + (iSys + 1));
    }

    numRefs = refFiles.length;
    allRefs = loadRefs(refFiles, numHyps);
  }

  public static List<List<String>> loadRefs(String[] refFiles, int numHypsHint) throws IOException {

    // TODO: Auto-detect laced refs?

    List<List<String>> allRefs = new ArrayList<List<String>>();

    int numRefs = refFiles.length;
    while(allRefs.size() <= numHypsHint) {
      allRefs.add(new ArrayList<String>(numRefs));
    }
    for(String refFile : refFiles) {
      List<String> oneRefPerHyp = loadSentences(refFile, "non-laced references");
      if (numHypsHint != oneRefPerHyp.size()) {
        throw new RuntimeException("Non-parallel inputs detected. Expected " + numHypsHint + " references, but got "
            + oneRefPerHyp.size());
      }

      for(int iHyp = 0; iHyp < oneRefPerHyp.size(); iHyp++) {
        String ref = oneRefPerHyp.get(iHyp);
        allRefs.get(iHyp).add(ref);
      }
    }

    return allRefs;
  }

  private void loadHyps(String[] hypFiles, String sys) throws IOException {
    if (numOptRuns == -1) {
      numOptRuns = hypFiles.length;
    } else {
      if (numOptRuns != hypFiles.length) {
        throw new RuntimeException("Non-parallel number of optimization runs. Expected " + numOptRuns + " but got "
            + hypFiles.length);
      }
    }

    List<List<String>> sysHypsForAllOptRuns = new ArrayList<List<String>>();
    allHyps.add(sysHypsForAllOptRuns);
    for(int iOpt = 0; iOpt < numOptRuns; iOpt++) {
      List<String> hypsForOptRun = loadSentences(hypFiles[iOpt], "Hypotheses for system " + sys + " opt run "
          + (iOpt + 1));
      sysHypsForAllOptRuns.add(hypsForOptRun);

      if (numHyps == -1) {
        numHyps = hypsForOptRun.size();
      } else {
        if (numHyps != hypsForOptRun.size()) {
          throw new RuntimeException("Non-parallel inputs detected. Expected " + numHyps + " hypotheses, but got "
              + hypsForOptRun.size());
        }
      }
    }
  }

  public static List<String> loadSentences(String hypFile, String forWhat) throws IOException {

    File file = new File(hypFile);
    // TODO: Say what system (or reference) this is for
    System.err.println("Reading " + forWhat + " file " + file.getAbsolutePath());
    List<String> sentences = Files.readLines(file, Charsets.UTF_8);
    // normalize any "double" whitespace, if it exists
    for(int i = 0; i < sentences.size(); i++) {
      String sent = sentences.get(i);
      sent = StringUtils.normalizeWhitespace(sent);
      // if(StringUtils.hasRedundantWhitespace(sent)) {
      // System.err.println("Normalizing extraneous whitespace: ");
      //
      // }
      sentences.set(i, sent);
    }

    return sentences;
  }

  public int getNumSystems() {
    return numSystems;
  }

  public List<String> getHypotheses(int iSys, int iOpt) {
    // TODO: More informative error messages w/ bounds checking
    return allHyps.get(iSys).get(iOpt);
  }

  public String getHypothesis(int iSys, int iOpt, int iHyp) {
    return getHypotheses(iSys, iOpt).get(iHyp);
  }

  public List<String> getReferences(int iHyp) {
    // TODO: More informative error messages w/ bounds checking
    return allRefs.get(iHyp);
  }

  public List<List<String>> getAllReferences() {
    return allRefs;
  }
}
