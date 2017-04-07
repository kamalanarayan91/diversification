/**
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopOr extends QrySop {

  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {


      return this.docIteratorHasMatchMin (r);
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
      double max = Double.MIN_VALUE;
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    }
    else {

      for(Qry arg : args)
      {
          if(! arg.docIteratorHasMatchCache())
          {
              continue;
          }

          /**
           * Only take maximum of scores of the same document from each
           * argument. if there is no match, just continue with the other
           * arguments
           */

          int docId = this.docIteratorGetMatch();// contains minuimum doc id
          int argId = arg.docIteratorGetMatch();
          if(docId!=argId)
          {
              continue;
          }

          double current = ((QrySop)arg).getScore(r);
          if(current>max)
          {
              max=current;
          }
      }
    }

      return max;
  }

    /**
     *  Get a default score
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getDefaultScore (RetrievalModel r,long docId) throws IOException {
        return 0.0;
    }

}
