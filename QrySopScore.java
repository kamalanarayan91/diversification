/**
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.IllegalArgumentException;

/**
 *  The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

  /**
   *  Document-independent values that should be determined just once.
   *  Some retrieval models have these, some don't.
   */
  
  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchFirst (r);
  }

  /**
   *  Get a score for the document that docIteratorHasMatch matched.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScore (RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean) {
      return this.getScoreUnrankedBoolean (r);
    }
    else if(r instanceof RetrievalModelRankedBoolean){
      return this.getScoreRankedBoolean(r);
    }
    else if(r instanceof RetrievalModelBM25){
      return this.getScoreBM25(r);
    }
    else if(r instanceof RetrievalModelIndri){
      return this.getScoreIndri(r);
    }
    else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the SCORE operator.");
    }
  }
  
  /**
   *  getScore for the Unranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }

  /**
   *  getScore for the Unranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    }
    else {

      QryIop argument = this.getArg(0);
      return argument.docIteratorGetMatchPosting().tf;


    }
  }

  /**
   *  getScore for the BM25 retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreBM25(RetrievalModel r) throws  IOException
  {
    double score =0.0;
    if (! this.docIteratorHasMatchCache())
    {
      return score;
    }
    else
    {

      double qtf = 1.0;

      QryIop argument = this.getArg(0);
      double df = argument.getDf();

      //R.S.J
      double rsjWeight = 0.0;
      double N = (double) Idx.getNumDocs();
      rsjWeight =  Math.log((N-df+0.5)/(df+0.5));
      rsjWeight = Math.max(0.0,rsjWeight);

      //term weight
      double docLength = (double) Idx.getFieldLength(argument.getField(),docIteratorGetMatch());
      double avgDocLength = ((double) Idx.getSumOfFieldLengths(argument.getField()))/ (double)(Idx.getDocCount(argument.getField()));

      double tf = argument.docIteratorGetMatchPosting().tf;
      double k1 = RetrievalModelBM25.k1;
      double b = RetrievalModelBM25.b;
      double tfWeight = tf /(tf + (k1*  (   (1.0-b) +   (b*(docLength/avgDocLength) ) ) )  );


      // user weight is always 1
      double userWeight = ((RetrievalModelBM25.k3+1)* qtf)/(qtf + RetrievalModelBM25.k3);
      score = rsjWeight * tfWeight * userWeight;


    }

    return score;
  }

  /**
   *  getScore for the Indri retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreIndri(RetrievalModel r) throws IOException
    {
      double score=0.0;
      if(!this.docIteratorHasMatchCache())
      {
        return score;
      }
      else
      {

        //p mle (q/c)
        QryIop argument = this.getArg(0);
        double ctf = argument.getCtf();
        double modC = (double) Idx.getSumOfFieldLengths(argument.getField());
        double pMLE = ctf/modC;

        double tf = argument.docIteratorGetMatchPosting().tf;
        double mu = RetrievalModelIndri.mu;
        double lambda = RetrievalModelIndri.lambda;

        double docLength = (double) Idx.getFieldLength(argument.getField(),docIteratorGetMatch());

        score = (1-lambda)*((tf + (mu * pMLE))/(docLength+mu));
        score += lambda * pMLE;

      }

      return score;

    }

  /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators.  If the query operator is of type QryIop, it
   *  is fully evaluated, and the results are stored in an internal
   *  inverted list that may be accessed via the internal iterator.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   */
  public void initialize (RetrievalModel r) throws IOException {

    Qry q = this.args.get (0);
    q.initialize (r);
  }



  /**
   *  Get a default score
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getDefaultScore (RetrievalModel r,long docId) throws IOException
  {
    double score = 0.0;

    if(r instanceof RetrievalModelIndri)
    {
      //p mle (q/c)
      QryIop argument = this.getArg(0);
      double ctf = argument.getCtf();
      double modC = (double) Idx.getSumOfFieldLengths(argument.getField());
      double pMLE = ctf/modC;

      double tf = 0; // Default score -- TF = 0;
      double mu = RetrievalModelIndri.mu;
      double lambda = RetrievalModelIndri.lambda;

      double docLength = (double) Idx.getFieldLength(argument.getField(),(int) docId);

      score = (1-lambda)*((tf + (mu * pMLE))/(docLength+mu));
      score += lambda * pMLE;
    }
    else
    {
      throw new IllegalArgumentException
              (r.getClass().getName() + " Doesnt support default scores ");
    }

    return score;

  }



}
