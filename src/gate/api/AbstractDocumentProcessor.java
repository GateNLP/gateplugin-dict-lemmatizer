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
package gate.api;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gate.Controller;
import gate.Document;
import gate.Resource;
import gate.creole.ControllerAwarePR;
import gate.creole.ResourceInstantiationException;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.Sharable;
import gate.util.Benchmark;
import gate.util.Benchmarkable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
//import java.util.Optional;

/**
 * Abstract base class for all the PRs in this plugin.
 * This is very similar to what the AbstractLanguageAnalyser should have been.
 */
public abstract class AbstractDocumentProcessor
        extends AbstractLanguageAnalyser
        implements Serializable, ControllerAwarePR, Benchmarkable {

  /**
   *
   */
  private Logger logger = Logger.getLogger(AbstractDocumentProcessor.class.getCanonicalName());

  private int seenDocuments = 0;

  protected Controller controller;

  protected Throwable throwable;
  
  
  private static final Object syncObject = new Object();
  
  protected AtomicInteger nDuplicates = null;
  @Sharable
  public void setNDuplicates(AtomicInteger n) {
    nDuplicates = n;
  }
  public AtomicInteger getNDuplicates() {
    return nDuplicates;
  }
  
  
  protected ConcurrentHashMap<String,Object> sharedData = null;
  @Sharable
  public void setSharedData(ConcurrentHashMap<String,Object> v) {
    sharedData = v;
  }
  public ConcurrentHashMap<String,Object> getSharedData() {
    return sharedData;
  }
  
  protected int duplicateId = 0;

  //===============================================================================
  // Implementation of the relevant API methods for DocumentProcessors. These
  // get inherited by the implementing class. This also defines abstract methods 
  // that make it easier to handle the control flow:
  // void process(Document doc) - replaces void execute()
  // void beforeFirstDocument(Controller) - called before the first document is processed
  //     (not called if there were no documents in the corpus, for example)
  // void afterLastDocument(Controller, Throwable) - called after the last document was processed
  //     (not called if there were no documents in the corpus). If Throwable is
  //     not null, processing stopped because of an exception.
  // void finishedNoDocument(Controller, Throwable) - called when processing 
  //     finishes and no documents were processed. If Throwable is not null,
  //     processing finished because of an error.
  //================================================================================
  @Override
  public Resource init() throws ResourceInstantiationException {
    // we always provide the following fields to all PRs which are used for duplicated PRs:
    // nDuplicates is an AtomicInt which gets incremented whenever a resource
    // gets duplicated. 
    synchronized (syncObject) {
      if(getNDuplicates() == null) {        
        System.err.println("DEBUG: creating first instance of PR "+this.getName());
        setNDuplicates(new AtomicInteger(0));
        setSharedData(new ConcurrentHashMap<String,Object>());
        System.err.println("DEBUG: created duplicate 0 of PR "+this.getName());
      } else {
        int thisn = getNDuplicates().addAndGet(1);
        duplicateId = thisn;
        System.err.println("DEBUG: created duplicate "+thisn+" of PR "+this.getName());
      }
    }
    return this;
  }

  @Override
  public void execute() throws ExecutionException {
    if (seenDocuments == 0) {
      beforeFirstDocument(controller);
    }
    seenDocuments += 1;
    process(getDocument());
  }

  @Override
  public void controllerExecutionAborted(Controller arg0, Throwable arg1)
          throws ExecutionException {
    // reset the flags for the next time the controller is run
    controller = arg0;
    throwable = arg1;
    if (seenDocuments > 0) {
      afterLastDocument(arg0, arg1);
    } else {
      finishedNoDocument(arg0, arg1);
    }
  }

  @Override
  public void controllerExecutionFinished(Controller arg0)
          throws ExecutionException {
    controller = arg0;
    if (seenDocuments > 0) {
      afterLastDocument(arg0, null);
    } else {
      finishedNoDocument(arg0, null);
    }
  }

  @Override
  public void controllerExecutionStarted(Controller arg0)
          throws ExecutionException {
    controller = arg0;
    seenDocuments = 0;
  }
  

  //=====================================================================
  // New simplified API for the child classes 
  //=====================================================================
  
  // NOTE: not sure which of these should be abstract (and thus force 
  // the programmer to implement them even if empty) and which should be
  // pre-implemented to do nothing. 
  
  /**
   * The new method to implement by PRs which derive from this class.
   * This must return a document which will usually be the same object
   * as it was passed.
   * NOTE: in the future the better option here may be to return 
   * Optional<Document> or even List<Document>. That way downstream
   * PRs could be made to not process filtered documents and to process
   * additional generated documents. 
   * 
   * @param document 
   */
  protected abstract Document process(Document document);

  /**
   * This can be overridden in PRs and will be run once before
   * the first document seen. 
   * This method is not called if no documents are processed at all. 
   * @param ctrl 
   */
  protected abstract void beforeFirstDocument(Controller ctrl);

  /**
   * This can be overridden in PRs and will be run after processing has started.
   * This will run once before any document is processed and before the method
   * beforeFirstDocument is invoked, even if no document is being processed at all.
   * 
   * @param ctrl 
   */
  protected void processingStarted(Controller ctrl) { };
  
  protected abstract void afterLastDocument(Controller ctrl, Throwable t);

  protected abstract void finishedNoDocument(Controller ctrl, Throwable t);
  
  protected void benchmarkCheckpoint(long startTime, String name) {
    if (Benchmark.isBenchmarkingEnabled()) {
      Benchmark.checkPointWithDuration(
              Benchmark.startPoint() - startTime,
              Benchmark.createBenchmarkId(name, this.getBenchmarkId()),
              this, null);
    }
  }

  @Override
  public String getBenchmarkId() {
    return benchmarkId;
  }

  @Override
  public void setBenchmarkId(String string) {
    benchmarkId = string;
  }
  private String benchmarkId = this.getName();

  
  
  
  
  
  /**
   * Implement high-level API functions that can be used without importing
   * anything.
   * @param methodName
   * @param parms
   * @return 
   */
  // TODO: not yet, we will implement this once we removed the requirement to
  // be compatible with Java 7
  /*
  protected Optional<Object> call(String methodName, Object... parms) {
    return Optional.empty();
  }
  */
}
