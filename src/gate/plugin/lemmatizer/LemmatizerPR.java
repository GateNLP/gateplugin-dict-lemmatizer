/* 
 * Copyright (C) 2015-2016 The University of Sheffield.
 *
 * This file is part of gateplugin-CorpusStats
 * (see https://github.com/johann-petrak/gateplugin-CorpusStats)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software. If not, see <http://www.gnu.org/licenses/>.
 */


/**
 * 
 *  TfIdf: Simple PR to calculate count DF and TF and calculate TFIDF scores,
 *  with support for parallel processing.
 */
package gate.plugin.lemmatizer;


import gate.*;
import gate.api.AbstractDocumentProcessor;
import gate.creole.ResourceData;
import gate.creole.metadata.*;
import gate.util.GateRuntimeException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@CreoleResource(name = "Lemmatizer",
        helpURL = "https://github.com/GateNLP/gateplugin-Lemmatizer/wiki/Lemmatizer",
        comment = "Find the lemmata of words.")
public class LemmatizerPR  extends AbstractDocumentProcessor {

  private static final long serialVersionUID = 1L;
  
  
  
  protected String inputASName = "";
  @RunTime
  @Optional
  @CreoleParameter(
          comment = "Input annotation set",
          defaultValue = "")
  public void setInputAnnotationSet(String ias) {
    inputASName = ias;
  }

  public String getInputAnnotationSet() {
    return inputASName;
  }
  
  
  protected String inputType = "";
  @RunTime
  @CreoleParameter(
          comment = "The input annotation type",
          defaultValue = "Token")
  public void setInputAnnotationType(String val) {
    this.inputType = val;
  }

  public String getInputAnnotationType() {
    return inputType;
  }

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The optional containing annotation set type",
          defaultValue = "")
  public void setContainingAnnotationType(String val) {
    this.containingType = val;
  }

  public String getContainingAnnotationType() {
    return containingType;
  }
  protected String containingType = "";

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The feature from the input annotation to use as word string, if left blank the document text",
          defaultValue = "")
  public void setTextFeature(String val) {
    this.textFeature = val;
  }

  public String getTextFeature() {
    return textFeature;
  }
  protected String textFeature = "";

  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The feature that contains the POS tag",
    defaultValue = "category")
  public void setPosFeature(String val) {
    posFeature = val;
  }
  public String getPosFeature() {
    return posFeature;
  }
  private String posFeature = "category";
  
  

  public enum Language {
    ENGLISH("en"),
    GERMAN("de"),
    FRENCH("fr"),
    SPANISH("es");
    private Language(String code) {
      this.code = code;
    }
    private String code;
    public String getLangCode() { return code; }
  }


  
  private Language language;
  @RunTime
  @CreoleParameter( 
          comment = "The language to use."
  )
  public void setLanguage(Language val) {
    language = val;
  }
  public Language getLanguage() { return language; }
  
  
  ////////////////////// FIELDS
  Map<String, String> nounDic;
  Map<String, String> adjDic;
  Map<String, String> advDic;
  Map<String, String> verbDic;
  Map<String, String> detDic;
  Map<String, String> pronDic;
  
  String textFeatureToUse = "";
  String posFeatureToUse = "category";
  
  HfstLemmatizer hfstLemmatizer = null;  // if null we do not have a FST
  
  ////////////////////// PROCESSING
  
  @Override
  protected Document process(Document document) {
    
    AnnotationSet inputAS = null;
    if (inputASName == null
            || inputASName.isEmpty()) {
      inputAS = document.getAnnotations();
    } else {
      inputAS = document.getAnnotations(inputASName);
    }

    AnnotationSet inputAnns = null;
    if (inputType == null || inputType.isEmpty()) {
      throw new GateRuntimeException("Input annotation type must not be empty!");
    }
    inputAnns = inputAS.get(inputType);

    AnnotationSet containingAnns = null;
    if (containingType == null || containingType.isEmpty()) {
      // leave the containingAnns null to indicate we do not use containing annotations
    } else {
      containingAnns = inputAS.get(containingType);
      //System.out.println("DEBUG: got containing annots: "+containingAnns.size()+" type is "+containingAnnotationType);
    }

    fireStatusChanged("Lemmatizer: running on " + document.getName() + "...");
    
    if (containingAnns == null) {
      doIt(document,inputAnns);
    } else {
      // do it for each containing annotation
      for (Annotation containingAnn : containingAnns) {
        doIt(document,gate.Utils.getContainedAnnotations(inputAnns,containingAnn));
      }
    }
    
    fireProcessFinished();
    fireStatusChanged("Lemmatizer: processing complete!");
    return document;
  }
  
  private void doIt(Document doc, AnnotationSet anns) {
    for(Annotation token : anns) {
      FeatureMap fm = token.getFeatures();
      String pos = (String)fm.get(posFeatureToUse);
      if(pos == null || pos.trim().isEmpty()) {
        continue;
      } else {
        lemmatize(token,fm,pos);
      }
    }
  }
  
  private void lemmatize(Annotation token, FeatureMap fm, String pos) {
    String tokenString;
    if (textFeatureToUse == null) {
      tokenString = gate.Utils.cleanStringFor(document, token);
    } else {
      tokenString = (String) fm.get(textFeatureToUse);
    }
    String kind = (String) fm.get("kind");
    String lemma = null;  // as long as the lemma is null we can still try to find one ...
    if (kind.equals("number")) {
      lemma = tokenString;
    } else if (kind.equals("punct")) {
      lemma = tokenString;
    } else if (detDic.get(tokenString.toLowerCase()) != null) {
      lemma = tokenString;
    } else {

      // TODO: why is this done????
      //String lemma = null;
      //String posType = posTaggedVersion[s];
      //if ("it".equalsIgnoreCase(language)) {
      //  posType = posType.substring(0, 1);
      //}
      //System.out.println(posType);
      //String generalType = posMap.get(posType.toLowerCase());
      if ("NOUN".equalsIgnoreCase(pos)) {
        lemma = nounDic.get(tokenString.toLowerCase());
      } else if ("VERB".equalsIgnoreCase(pos)) {
        lemma = verbDic.get(tokenString.toLowerCase());
      } else if ("ADJ".equalsIgnoreCase(pos)) {
        lemma = adjDic.get(tokenString.toLowerCase());
      } else if ("ADV".equalsIgnoreCase(pos)) {
        lemma = advDic.get(tokenString.toLowerCase());
      } else if ("PRON".equalsIgnoreCase(pos)) {
        lemma = pronDic.get(tokenString.toLowerCase());
      }
      // TODO: replace with indicator of if we have a FST from the init phase
      if (!"nl".equalsIgnoreCase(language.getLangCode()) && lemma == null) {
        // lemma = hfstLemmatizer.getLemma(tokenString,pos);
      }
    if (lemma == null || "".equals(lemma)) {
      lemma = tokenString;
    }
  }
  //// END

}
  

  @Override
  protected void beforeFirstDocument(Controller ctrl) {
    
    if(posFeature == null || posFeature.trim().isEmpty()) {
      posFeatureToUse = "category";      
    } else {
      posFeatureToUse = posFeature;
    }
    
    if(textFeature == null || textFeature.trim().isEmpty()) {
      textFeatureToUse = null;
    } else {
      textFeatureToUse = textFeature;
    }
  
    ResourceData myResourceData =
        Gate.getCreoleRegister().get("gate.plugin.lemmatizer.Lemmatizer");
    java.net.URL creoleXml = myResourceData.getXmlFileUrl();
    File pluginDir = gate.util.Files.fileFromURL(creoleXml).getParentFile();
    File resourcesDir = new File(pluginDir,"resources");
    File dictDir = new File(new File(resourcesDir,"dictionaries"),language.getLangCode());
    nounDic = loadDictionary(new File(dictDir,"nounDic.txt"));
    adjDic = loadDictionary(new File(dictDir,"adjDic.txt"));
    advDic = loadDictionary(new File(dictDir,"advDic.txt"));
    verbDic = loadDictionary(new File(dictDir,"verbDic.txt"));
    detDic = loadDictionary(new File(dictDir,"detDic.txt"));
    pronDic = loadDictionary(new File(dictDir,"pronounDic.txt"));
    
    // Load the hfst lemmatizer if it exists for the language, otherwise
    // the hfstLemmatizer variable remains null
    File lemmatizerDir = new File(resourcesDir,"lemmaModels");
    File lemmatizerFile = new File(lemmatizerDir,language.getLangCode()+".hfst.ol");
    if(lemmatizerFile.exists()) {
      try {
        hfstLemmatizer = HfstLemmatizer.load(lemmatizerFile,language.getLangCode());
      } catch (Exception ex) {
        throw new GateRuntimeException("Could not load lemmatization transducer "+lemmatizerFile,ex);
      }
    } else {
      hfstLemmatizer = null;
    }
    
  }
    

  @Override
  protected void afterLastDocument(Controller ctrl, Throwable t) {
  }

  @Override
  protected void finishedNoDocument(Controller ctrl, Throwable t) {
  }
  

  
  public static Map<String, String> loadDictionary(File dictFile) {
    BufferedReader in = null;
    try {
      Map<String, String> map = new HashMap<String, String>();
      in = new BufferedReader(new InputStreamReader(new FileInputStream(dictFile), "UTF-8"));
      String str;
      while ((str = in.readLine()) != null) {
        if (!"".equals(str.trim())) {
          String values[] = str.split("===");
          if (values.length == 2) {
            String vals[] = values[1].split(";");
            for (int i = 0; i < vals.length; i++) {
              String val = vals[i];
              map.put(val.toLowerCase(), values[0].trim());

            }
          }
        }
      }
      in.close();
      return map;
    } catch (Exception ex) {
      throw new GateRuntimeException("Could not read dictionary " + dictFile.getAbsolutePath(), ex);
    }
  }
  
  
  
  
  
} // class Lemmatizer
