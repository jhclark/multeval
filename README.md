Overview
========

MultEval takes machine translation hypotheses from several runs of an optimizer and provides 3 popular metric scores, as well as, variances (via bootstrap resampling) and p-values (via approximate randomization). This allows researchers to mitigate some of the risk of using unstable optimizers such as MERT, MIRA, and MCMC.

It is a user-friendly implementation of:
Jonathan Clark, Chris Dyer, Alon Lavie, and Noah Smith, "Better Hypothesis Testing for Statistical Machine Translation: Controlling for Optimizer Instability", Proceedings of the Association for Computational Lingustics, 2011. [PDF](http://www.cs.cmu.edu/~jhclark/pubs/significance.pdf)

Usage
=====

java -Xmx2G -jar MultEval.jar eval -H example/hyps.lc.tok.en.0 example/hyps.lc.tok.en.2 example/hyps.lc.tok.en.2 -R example/refs.test2010.lc.tok.en

For a more detailed description of the METEOR options, please see http://github.com/mjdenkowski/meteor


Discussion
==========

A few words on casing, tokenization, normalization, segmentation, and punctuation in evaluation.


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