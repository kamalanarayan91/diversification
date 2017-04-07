import java.io.IOException;

/**
 * Created by Kamal on 15/2/17.
 */
public class QrySopSum extends  QrySop {

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

        if (r instanceof RetrievalModelBM25) {
            return getScoreBM25 (r);
        }

        else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SUM operator.");
        }
    }

    /**
     *  getScore for the RankedBoolean retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    private double getScoreBM25 (RetrievalModel r) throws IOException
    {
        double score = 0.0;
        if (! this.docIteratorHasMatch(r))
        {
            return score;
        }
        else
        {
            int match = docIteratorGetMatch();

            for(Qry arg : args)
            {
                if(!arg.docIteratorHasMatch(r))
                {
                    continue;
                }

                if(match==arg.docIteratorGetMatch())
                {
                    score = score + ((QrySop) arg).getScore(r);
                }

            }
        }

        return score;
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
