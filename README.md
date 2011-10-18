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
wget https://github.com/downloads/jhclark/multeval/multeval-0.3.tgz
tar -xvzf multeval-0.3.tgz
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

The ASCII table produced by multeval looks something like this:

```
n=3            BLEU (s_sel/s_opt/p)   METEOR (s_sel/s_opt/p) TER (s_sel/s_opt/p)    Length (s_sel/s_opt/p) 
baseline       18.5 (0.3/0.1/-)       29.3 (0.1/0.0/-)       65.7 (0.4/0.2/-)       107.5 (0.4/0.1/-)      
system 1       18.8 (0.3/0.3/0.00)    30.3 (0.1/0.1/0.00)    64.8 (0.4/0.6/0.00)    107.7 (0.3/0.7/0.09)   
system 2       18.5 (0.3/0.1/0.00)    29.3 (0.1/0.0/0.00)    65.7 (0.4/0.2/0.00)    107.5 (0.4/0.1/0.00)
```

The LaTeX table produced by multeval looks something like this:

![Latex Table](https://github.com/jhclark/multeval/raw/HEAD/table.png)

To see a full list of options, use:
``` bash
./multeval.sh eval
```

which gives:

```
Usage: program <module_name> [options...]

=== TER ===
-T [--ter.shiftCost]              Shift cost for TER 
-d [--ter.maxShiftDistance]       Maximum shift distance for TER 
-P [--ter.punctuation]            Use punctuation in TER? 
-b [--ter.beamWidth]              Beam width for TER 
-B [--ter.substituteCost]         Substitute cost for TER 
-D [--ter.deleteCost]             Delete cost for TER 
-M [--ter.matchCost]              Match cost for TER 
-I [--ter.insertCost]             Insert cost for TER 

=== BLEU ===

=== METEOR ===
-t [--meteor.task]                One of: rank adq hter tune (Rank is generally a good choice) 
-s [--meteor.synonymDirectory]    If default is not desired (NOTE: This option has a different short flag than stand-alone METEOR)  [optional]
-x [--meteor.beamSize]            Specify beam size (overrides default) 
-p [--meteor.params]              Custom parameters of the form 'alpha beta gamma' (overrides default)  [optional]
-w [--meteor.weights]             Specify module weights (overrides default)  [optional]
-a [--meteor.paraphraseFile]      If default is not desired  [optional]
-m [--meteor.modules]             Specify modules. (overrides default) Any of: exact stem synonym paraphrase  [optional]
-k [--meteor.keepPunctuation]     Consider punctuation when aligning sentences (if false, the meteor tokenizer will be run, after which punctuation will be removed) 
-l [--meteor.language]            Two-letter language code of a supported METEOR language (e.g. 'en') 

=== MultEvalModule (for eval module) ===
-b [--boot-samples]               Number of bootstrap replicas to draw during bootstrap resampling to estimate standard deviation for each system 
-H [--hyps-sys]                   Space-delimited list of files containing tokenized, fullform hypotheses, one per line 
-s [--ar-shuffles]                Number of shuffles to perform to estimate p-value during approximate randomization test system *PAIR* 
-r [--rankDir]                    Rank hypotheses of median optimization run of each system with regard to improvement/decline over median baseline system and output to the specified directory for analysis  [optional]
-R [--refs]                       Space-delimited list of files containing tokenized, fullform references, one per line 
-o [--metrics]                    Space-delimited list of metrics to use. Any of: bleu, meteor, ter, length 
-F [--fullLatexDoc]               Output a fully compilable Latex document instead of just the table alone  [optional]
-L [--latex]                      Latex-formatted table including measures that are commonly (or should be commonly) reported  [optional]
-D [--debug]                      Show debugging output?  [optional]
-B [--hyps-baseline]              Space-delimited list of files containing tokenized, fullform hypotheses, one per line 
-v [--verbosity]                  Verbosity level 

--help                        help message
```

What do p-values actually mean?
-------------------------------

A p-value is a model's estimate (where the model is a significance test) that a particular difference in scores arose by chance. Multeval uses approximate randomization, a test that approximates a permutation test via sampling shufflings of like hypotheses between systems.

The most important points are:

* a p-value **does** tell you whether a difference of this magnitude is likely to be generated again by some random process (a randomized optimizer)
* a p-value **does not** tell you whether a difference of this magnitude is meaningful (in terms of translation quality)

So even though a large difference may more frequently correspond to larger p-values, this is not guaranteed. In fact, small differences can be quite significant and vice versa. For example, if you give a single optimizer sample with identical hypotheses and tell MultEval that these are actually two different systems (as in the baseline system and system 2 in the example data), there will be zero difference in scores and also a p-value of zero, since shuffling hypotheses between the systems produces no change, indicating that this difference (of zero) is likely to be reproducible. This demonstrates 2 points about p-values: 1) that this significance test does not account for the user giving it too few (optimizer) samples, which is why it's important to report how many optimizer samples you used and 2) that the test only provides information about the replicability of a delta, not whether or not the magnitude can be assigned external meaning (in terms of translation quality).

Rounding
--------

MultEval generally rounds to one decimal place for metrics and two decimal places for p-values just before externally reporting values in tables. One should expect that our current evaluation measures and significance testing models are not likely to be very discriminant beyond this point and so assigning meaning to finer distinctions is not necessarily informative.

Discussion on Pitfalls in Tokenization, Segmentation, etc.
----------------------------------------------------------

While tokenization *can* introduce bias in many metrics by changing the length of hypotheses and references, noise can also be introduced into the evaluation process by strange interactions between running a detokenizer followed by tokenizer. Therefore, since research often isn't focused on details such as tokenization, we recommend evaluating on full-form references and hypotheses (i.e. not segmented, unless you're working with a language without a canonical notion of words) that are *tokenized*. Of course, in a bake-off scenario or in research in which multiple tokenization schemes are being compared, a standard tokenization will be necessary to have a comparable evaluation.

Still, special care must be taken for issues such as segmentation, that can effect matching of resources such as paraphrase tables in METEOR. Since METEOR doesn't use paraphrases including punctuation, this isn't an issue for tokenization, while lowercased full-forms remain a requirement for METEOR evaluation.

Care must also be taken to ensure experimental validity so that a particular tokenization does not bias your results in an unexpected way. For example, it would be easy to inflate the BLEU score by segmenting URLs into many tokens (since URLs are usually passed through, you would almost always get credit for lots of extra tokens being correct). Now, your absolute scores will be higher and any changes (positive or negative) will be diluted since there is now a greater number of overall tokens, this uninteresting subset now makes up a greater overall fraction of the evaluation set.

As a final pitfall example, consider the evaluation of Buckwalter-transliterated Arabic data. Using the traditional NIST mt-eval script where tokenization is forced (and the final tokenization is never shown to the user), one would see very biased (nearly senseless) scores since the many punctuation characters used to encode the Arabic alphabet will be used to split should-be single tokens into many illogical pieces. In this case, traditional tokenization is almost certainly the wrong thing to do. This is why MultEval leaves tokenization to the user.

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

MultEval produces *exactly* the same metric scores you would obtain by running the metrics as stand-alone programs -- with the exception that MultEval does not perform tokenization. MultEval calls METEOR and TER using library API calls, and we regression test against gold-standard scores produced by these metrics in stand-alone mode. Its internal jBLEU library is a direct port of NIST's mteval-v13a.pl; we regression test against mteval-v13m.pl (which allows disabling normalization), included in the METEOR distribution. jBLEU implements the segment-level smoothing of BLEU from mteval-v13a.pl, which is derived from the smoothing in Kishore Papineni's bleu-1.04.pl script (the original IBM implementation of BLEU), added as of 3/9/2004 -- note this is *not* the simplistic +1 BLEU smoothing used in some other systems.


Adding Your Own Metric
======================

Implement the metrics.Metric interface and then add it as an option in multeval.MultEval.


Libraries
=========

MultEval uses the following libraries:

*  METEOR 1.3 (LGPL License, http://www.cs.cmu.edu/~alavie/METEOR/ -- WordNet database has a compatible free license)
*  Translation Error Rate 0.7 (LGPL License, TerCom, http://www.cs.umd.edu/~snover/tercom/ -- Thanks to Matt Snover for relicensing this)
*  Google Guava (Apache License)
*  Java Annotation Options (jannopts, LGPL License)

Building
========

Should you want to build MultEval yourself instead of using the provided tarball distribution, you'll need to download meteor and install it under lib/meteor-1.3/meteor-1.3.jar. Then you can just run ant:

``` bash
$ ant
```

NOTE: There's a strange generics-related javac bug that's known to cause the build to fail under OpenJDK V1.6.0_17. However, this seems to be resolved as of version 1.6.0_21.


Citation
========

If you use this software, consider citing:

Jonathan Clark, Chris Dyer, Alon Lavie, and Noah Smith, "Better Hypothesis Testing for Statistical Machine Translation: Controlling for Optimizer Instability", Proceedings of the Association for Computational Lingustics, 2011.


The included metrics should be cited as:

Kishore Papineni, Salim Roukos, Todd Ard, and Wei-Jing Zhu, "BLEU: a method for automatic evaluation of machine translation," Proceedings of the 40th Annual Meeting of the Association for Computational Linguistics, 2002.

Michael Denkowski and Alon Lavie, "Extending the METEOR Machine Translation Evaluation Metric to the Phrase Level", Proceedings of NAACL/HLT, 2010.

Matthew Snover, Bonnie Dorr, Richard Schwartz, Linnea Micciulla, and John Makhoul, "A Study of Translation Edit Rate with Targeted Human Annotation," Proceedings of Association for Machine Translation in the Americas, 2006.
