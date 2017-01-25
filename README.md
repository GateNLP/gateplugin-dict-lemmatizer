# gateplugin-Lemmatizer

A plugin for the GATE language technology framework for finding Lemmata for words.

This plugin combines word lists from [Wiktionary](https://www.wiktionary.org/) and, if available, 
morphological transducers created for the 
[Helsinki Finite-State Transducer (FST) software](http://www.ling.helsinki.fi/kieliteknologia/tutkimus/hfst/)
to find lemmata for tokens. 

Currently, the following languages are supported:
* en (English)
* de (German)
* fr (French)
* it (Italian)
* nl (Dutch)
* es (Spanish)

The input for the PR must already be tokenised and every token must have
a universal dependency POS tag as a feature.

This plugin is partly based on the [code](http://staffwww.dcs.shef.ac.uk/people/A.Aker/activityNLPProjects.html) developed by 
[Ahmet Aker](https://www.is.inf.uni-due.de/staff/aker.html.de) for POS tagging and lemmatization
in several languages.

