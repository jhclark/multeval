Overview
========

MultEval takes machine translation hypotheses from several runs of an optimizer and provides 3 popular metric scores, as well as, variances (via bootstrap resampling) and p-values (via approximate randomization). This allows researchers to mitigate some of the risk of using unstable optimizers such as MERT, MIRA, and MCMC. It is intended to help in evaluating the impact of in-house experimental variations on translation quality; it is currently not setup to do bake-off style comparisons (bake-offs can't require multiple optimizer runs nor a standard tokenization).

It is a user-friendly implementation of:
Jonathan Clark, Chris Dyer, Alon Lavie, and Noah Smith, "Better Hypothesis Testing for Statistical Machine Translation: Controlling for Optimizer Instability", Proceedings of the Association for Computational Lingustics, 2011. [PDF](http://www.cs.cmu.edu/~jhclark/pubs/significance.pdf)

Usage
=====

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

For a more detailed description of the METEOR options, please see http://github.com/mjdenkowski/meteor


Discussion on Tokenization, Segmentation, etc.
----------------------------------------------

While tokenization *can* introduce bias in many metrics by changing the length of hypotheses and references, noise can also be introduced into the evaluation process by strange interactions between running a detokenizer followed by tokenizer. Therefore, since research often isn't focused on details such as tokenization, we recommend evaluating on full-form references and hypotheses (i.e. not segmented, unless you're working with a language without a canonical notion of words) that are *tokenized*. Of course, in a bake-off scenario or in research in which multiple tokenization schemes are being compared, a standard tokenization will be necessary to have a comparable evaluation.

Still, special care must be taken for issues such as segmentation, that can effect matching of resources such as paraphrase tables in METEOR. Since METEOR doesn't use paraphrases including punctuation, this isn't an issue for tokenization, while lowercased full-forms remain a requirement for METEOR evaluation.


Cased vs Uncased Evaluation
---------------------------

For now, this program assumes you will give it lowercased input. Cased variants of BLEU and TER will be coming soon.


Adding Your Own Metric
======================

Implement the metrics.Metric interface and then add it as an option in multeval.MultEval.


Libraries
=========

MultEval uses the following libraries:

*  METEOR 1.2 (LGPL License, http://www.cs.cmu.edu/~alavie/METEOR/ -- WordNet database has a compatible free license)
*  Translation Error Rate 0.7 (TerCom, http://www.cs.umd.edu/~snover/tercom/, NOTE: TER is licensed *for research purposes only* -- please see its license before using)
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