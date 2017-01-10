# gateplugin-Lemmatizer

A plugin for the GATE language technology framework for finding Lemmata for words.

This plugin combines word lists from [Wiktionary](https://www.wiktionary.org/) and, if available, 
morphological transducers created for the 
[Helsinki Finite-State Transducer (FST) software](http://www.ling.helsinki.fi/kieliteknologia/tutkimus/hfst/)
to find lemmata for tokens. 

IMPORTANT: because of the large size of the dictionary files and HFST models, the language specific
files must be downloaded separately. Currently, resources for the following languages are available:
* en (English)
* de (German)
* fr (French)
* it (Italian)
* nl (Dutch)
* es (Spanish)

To install the resources, download the relevant zip archives and unpack tham in the root directory of the plugin so
that the files get extracted as `./resources/dictionaries/<langcode>/*` and `./resources/lemmaModels/<langcode>.hfst.ol`.

Of course it is also possible to use dictionary files or HFST models you have created yourself instead.

The software for creating the dictionaries from Wiktionary dumps is available here: (To Be Done)

The input for the PR must already be tokenised and every token must have
a universal dependency POS tag as a feature.

This plugin is partly based on the [code](http://staffwww.dcs.shef.ac.uk/people/A.Aker/activityNLPProjects.html) developed by 
[Ahmet Aker](https://www.is.inf.uni-due.de/staff/aker.html.de) for POS tagging and lemmatization
in several languages.

