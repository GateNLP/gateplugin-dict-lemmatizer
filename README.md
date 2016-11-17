# gateplugin-Lemmatizer

A plugin for the GATE language technology framework for finding Lemmata for words.

This is based on the code developed by Ahmet Aker for POS tagging and lemmatization
in several languages published here: 
* http://staffwww.dcs.shef.ac.uk/people/A.Aker/activityNLPProjects.html

The plugin keeps much of the approach for creating the lemmata from either 
a dictionary of terms from wiktionary or by using a HFST morphological
transducer, if available for the language.

The input for the PR must already be tokenised and every token must have
a universal dependency POS tag as a feature.

Currently the following languages are supported:
* en (English)
* de (German)
* fr (French)
* it (Italian)
* nl (Dutch)
* es (Spanish)
