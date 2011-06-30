Overview
========

MultEval takes machine translation hypotheses from several runs of an optimizer and provides 3 popular metric scores, as well as, standard deviations (via bootstrap resampling) and p-values (via approximate randomization). This allows researchers to mitigate some of the risk of using unstable optimizers such as MERT, MIRA, and MCMC. It is intended to help in evaluating the impact of in-house experimental variations on translation quality; it is currently not setup to do bake-off style comparisons (bake-offs can't require multiple optimizer runs nor a standard tokenization).

It is a user-friendly implementation of:
Jonathan Clark, Chris Dyer, Alon Lavie, and Noah Smith, "Better Hypothesis Testing for Statistical Machine Translation: Controlling for Optimizer Instability", Proceedings of the Association for Computational Lingustics, 2011. [PDF](http://www.cs.cmu.edu/~jhclark/pubs/significance.pdf)

To keep updated on new versions of this software, subscribe to our low-traffic announcement mailing list: http://groups.google.com/group/multeval-announce

Usage
=====

First, download and unpack the program:

``` bash
wget https://github.com/downloads/jhclark/multeval/multeval-0.2.tgz
tar -xvzf https://github.com/downloads/jhclark/multeval/multeval-0.2.tgz
```

To evaluate a single system from the example data and get its BLEU, METEOR, and TER scores along with its standard deviation use:

``` bash
multeval.sh eval --refs example/refs.test2010.lc.tok.en.* \
                 --hyps-baseline example/hyps.lc.tok.en.baseline.opt* \
                 --meteor.language en
```

The first time you run this command, METEOR (and its sizable paraphrase tables) will be downloaded. Also, to help the user determine if any tokenization mismatch happened, MultEval also prints the top OOVs according to METEOR.

To compare several systems from the example data and get its BLEU, METEOR, and TER scores along with their standard deviations and p-values, use:

``` bash
multeval.sh eval --refs example/refs.test2010.lc.tok.en.* \
                 --hyps-baseline example/hyps.lc.tok.en.baseline.opt* \
                 --hyps-sys1 example/hyps.lc.tok.en.sys1.opt* \
                 --hyps-sys2 example/hyps.lc.tok.en.sys2.opt* \
                 --meteor.language en
```

If you'd also like 1) a Latex table at you can copy-paste into your paper and 2) the hypotheses from the median optimization run ranked by improvement/decline over your baseline system, then run it like this:

``` bash
multeval.sh eval --refs example/refs.test2010.lc.tok.en.* \
                 --hyps-baseline example/hyps.lc.tok.en.baseline.opt* \
                 --hyps-sys1 example/hyps.lc.tok.en.sys1.opt* \
                 --hyps-sys2 example/hyps.lc.tok.en.sys2.opt* \
                 --meteor.language en \
                 --latex table.tex \
                 --rankDir rank
```

All files should contain *tokenized*, lowercased, space-delimited sentences in UTF-8 encoding, one sentence per line. Unlike many metric implementations, MultEval does no tokenization or segmentation for you (see discussion below).

Generally, you should evaluate full forms (i.e. without word segmentation). For languages without a canonical notion of words (e.g. Chinese, Japanese), we recommend splitting all non-Latin characters (e.g. each character that is not part of a borrowed Western word, URL, etc. should be evaluated as its own word.)

For a more detailed description of the various METEOR options, please see http://github.com/mjdenkowski/meteor.

METEOR and its paraphrase tables will automatically be downloaded from the web the first time you run multeval.sh. They are not included in the initial download due to the large size (~200MB) of the paraphrase tables.

What do p-values actually mean?
-------------------------------

A p-value is a model's estimate (where the model is a significance test) that a particular difference in scores arose by chance. Multeval uses approximate randomization, a test that approximates a permutation test via sampling shufflings of like hypotheses between systems.

The most important points are:

* a p-value **does** tell you whether a difference of this magnitude is likely to be generated again by some random process (a randomized optimizer)
* a p-value **does not** tell you whether a difference of this magnitude is meaningful (in terms of translation quality)

So even though a large difference may more frequently correspond to larger p-values, this is not guaranteed. In fact, small differences can be quite significant and vice versa. For example, if you give a single optimizer sample with identical hypotheses, there will be zero difference in scores and also a p-value of zero, since shuffling hypotheses between the systems produces no change, indicating that this difference (of zero) is likely to be reproducible. This also demonstrates that this significance test does not account for the user giving it too few (optimizer) samples, which is why it's important to report how many optimizer samples you used.

Rounding
--------

MultEval generally rounds to one decimal place for metrics and two decimal places for p-values just before externally reporting values in tables. One should expect that our current evaluation measures and significance testing models are not likely to be very discriminant beyond this point and so assigning meaning to finer distinctions is not necessarily informative.

Discussion on Tokenization, Segmentation, etc.
----------------------------------------------

While tokenization *can* introduce bias in many metrics by changing the length of hypotheses and references, noise can also be introduced into the evaluation process by strange interactions between running a detokenizer followed by tokenizer. Therefore, since research often isn't focused on details such as tokenization, we recommend evaluating on full-form references and hypotheses (i.e. not segmented, unless you're working with a language without a canonical notion of words) that are *tokenized*. Of course, in a bake-off scenario or in research in which multiple tokenization schemes are being compared, a standard tokenization will be necessary to have a comparable evaluation.

Still, special care must be taken for issues such as segmentation, that can effect matching of resources such as paraphrase tables in METEOR. Since METEOR doesn't use paraphrases including punctuation, this isn't an issue for tokenization, while lowercased full-forms remain a requirement for METEOR evaluation.


Cased vs Uncased Evaluation
---------------------------

For now, this program assumes you will give it lowercased input. Cased variants of BLEU and TER will be coming soon.


Using MultEval for Error Analysis and Oracle Scoring
====================================================

In addition to metrics and statistical analyses for quantitative analysis, MultEval comes with several tools to help
you qualitatively determine how your translation systems are doing. First, is the ability to rank hypotheses with regard
to its improvement or decline over the baseline system (command line given above). Note, some metrics (e.g. BLEU) are
notoriously unstable at the sentence level.

Second, is the ability to take a n-best list from a decoder (e.g. Moses, cdec), score all of the hypotheses using
all of the metrics in MultEval, and then sort the hypotheses for each sentence by each of the metrics so that the
first sentence output for each sentence is the n-best oracle. You can get this by running:

``` bash
multeval.sh nbest --nbest cdec.kbest \
                  --refs example/refs.test2010.lc.tok.en.* \
                  --meteor.language en \
                  --rankDir rank \
                  > kbest.scored
```

MultEval will also display the corpus-level oracle score over the n-best list according to each metric.

For even more detailed analysis, you should also consider using the METEOR X-Ray analysis tool.


Compatibility and Accuracy
==========================

MultEval produces *exactly* the same metric scores you would obtain by running the metrics as stand-alone programs -- with the exception that MultEval does not perform tokenization. MultEval calls METEOR and TER using library API calls, and we regression test against gold-standard scores produced by these metrics in stand-alone mode. Its internal jBLEU library is a direct port of NIST's mteval-v13a.pl; we regression test against mteval-v13m.pl (which allows disabling normalization), included in the METEOR distribution.


Adding Your Own Metric
======================

Implement the metrics.Metric interface and then add it as an option in multeval.MultEval.


Libraries
=========

MultEval uses the following libraries:

*  METEOR 1.2 (LGPL License, http://www.cs.cmu.edu/~alavie/METEOR/ -- WordNet database has a compatible free license)
*  Translation Error Rate 0.7 (LGPL License, TerCom, http://www.cs.umd.edu/~snover/tercom/ -- Thanks to Matt Snover for relicensing this)
*  Google Guava (Apache License)
*  Java Annotation Options (jannopts, LGPL License)


Citation
========

If you use this software, consider citing:

Jonathan Clark, Chris Dyer, Alon Lavie, and Noah Smith, "Better Hypothesis Testing for Statistical Machine Translation: Controlling for Optimizer Instability", Proceedings of the Association for Computational Lingustics, 2011.


The included metrics should be cited as:

Kishore Papineni, Salim Roukos, Todd Ard, and Wei-Jing Zhu, "BLEU: a method for automatic evaluation of machine translation," Proceedings of the 40th Annual Meeting of the Association for Computational Linguistics, 2002.

Michael Denkowski and Alon Lavie, "Extending the METEOR Machine Translation Evaluation Metric to the Phrase Level", Proceedings of NAACL/HLT, 2010.

Matthew Snover, Bonnie Dorr, Richard Schwartz, Linnea Micciulla, and John Makhoul, "A Study of Translation Edit Rate with Targeted Human Annotation," Proceedings of Association for Machine Translation in the Americas, 2006.

TODO Citation for the ORANGE Lin & Och 2004 paper that described smoothed BLEU in Sec 4