package jbleu.util;

import java.util.*;

// TODO: Potentially use integers instead of Strings, if overhead is worth it
public class Ngram {

  private List<String> toks;
  private int hash = 0;

  public Ngram(List<String> toks) {
    this.toks = toks;
  }

  public int hashCode() {
    if (hash == 0) {
      for(String tok : toks) {
        hash ^= HashUtil.smear(tok.hashCode());
      }
    }
    return hash;
  }

  public boolean equals(Object obj) {
    if (obj instanceof Ngram) {
      // TODO: Slow
      Ngram other = (Ngram) obj;
      return toks.equals(other.toks);
    } else {
      throw new RuntimeException("Comparing n-gram to non-n-gram");
    }
  }

  public String toString() {
    return toks.toString();
  }
}
