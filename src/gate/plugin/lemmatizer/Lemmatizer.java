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
package gate.plugin.corpusstats;


import gate.*;
import gate.api.AbstractDocumentProcessor;
import gate.creole.metadata.*;
import gate.util.Benchmark;
import gate.util.GateRuntimeException;
import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

@CreoleResource(name = "Lemmatizer",
        helpURL = "https://github.com/GateNLP/gateplugin-Lemmatizer/wiki/Lemmatizer",
        comment = "Find the lemmata of words.")
public class Lemmatizer  extends AbstractDocumentProcessor {

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
    if(posFeature == null || posFeature.isEmpty()) {
      posFeature = "category";
    }
    return posFeature;
  }
  private String posFeature = "category";
  
  

  public enum Language {
    ENGLISH,
    GERMAN,
    FRENCH,
    SPANISH
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
      doIt(document,inputAnns)
    } else {
      // do it for each containing annotation
      for (Annotation containingAnn : containingAnns) {
        doIt(document,gate.Utils.getContainedAnnotations(inputAnns,containingAnn))
      }
    }
    
    fireProcessFinished();
    fireStatusChanged("Lemmatizer: processing complete!");
    return document;
  }
  
  private void doIt(Document doc, AnnotationSet anns) {
    for(Annotation token : anns) {
      FeatureMap fm = token.getFeatures();
      String pos = (String)fm.get(getPosFeature());
      if(pos == null || pos.trim().isEmpty()) {
        continue;
      } else {
      }
    }
  }
  

  @Override
  protected void beforeFirstDocument(Controller ctrl) {
  }
    

  @Override
  protected void afterLastDocument(Controller ctrl, Throwable t) {
  }

  @Override
  protected void finishedNoDocument(Controller ctrl, Throwable t) {
  }
  

  
} // class Lemmatizer
