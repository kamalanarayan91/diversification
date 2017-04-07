/**
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;


/**
 *  The OR operator for all retrieval models.
 */
public class QrySopAnd extends QrySop {

  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {

    if(r instanceof RetrievalModelIndri)
    {
      return this.docIteratorHasMatchMin(r);
    }

    return this.docIteratorHasMatchAll (r);
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
        return this.getScoreRankedBoolean (r);
    }
    else if(r instanceof RetrievalModelIndri){
      return this.getScoreIndri(r);
    }
    else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the OR operator.");
    }
  }

  /**
   *  getScore for the UnrankedBoolean retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }

  /**
   *  getScore for the RankedBoolean retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    double min = Double.MAX_VALUE;
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {

      for(Qry arg: args)
      {
        double current = ((QrySop) arg).getScore(r);
        if(current<min){
          min = current;
        }
      }
    }

    return min;
  }

  /**
   *  getScore for the Indri retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreIndri (RetrievalModel r) throws IOException {
    double score = 0.0;
    if(!this.docIteratorHasMatchCache()){
      return score;
    }
    else{
        score = 1.0;
        int currentMatch = docIteratorGetMatch();
        double argSize  = args.size();
        for(Qry arg : args){

          if(! arg.docIteratorHasMatchCache() || arg.docIteratorGetMatch() != currentMatch)
          {

              score  = score * Math.pow(((QrySop) arg).getDefaultScore(r,currentMatch), 1/argSize);
          }
          else
          {
            score = score * Math.pow(((QrySop) arg).getScore(r), 1/argSize);
          }

      }

    }

    return score;
  }

  /**
   *  Get a default score combined from all arguments
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getDefaultScore (RetrievalModel r,long docId) throws IOException {
      double score = 1.0;
      float argSize = args.size();

      for(Qry arg:args)
      {
        score  = score * Math.pow(((QrySop) arg).getDefaultScore(r,docId) , 1/argSize);
      }

      return score;
  }

}
