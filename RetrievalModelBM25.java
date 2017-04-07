/**
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

/**
 *  An object that stores parameters for the unranked Boolean
 *  retrieval model (there are none) and indicates to the query
 *  operators how the query should be evaluated.
 */
public class RetrievalModelBM25 extends RetrievalModel
{
    public static final double INVALID = -1;

    public static double k1 = INVALID;
    public static double k3 = INVALID;
    public static double b = INVALID;


    public String defaultQrySopName ()
    {
        return new String ("#sum");
    }


    public static double getLeTorScore(int docid, String field, String[] queryStems) throws  Exception
    {
        double totalScore = 0.0;
        double qtf = 1.0;


        TermVector termVector = new TermVector(docid,field);

        //get potential Terms
        int totalTerms = termVector.stemsLength();
        int pos = termVector.positionsLength();
        if(totalTerms==0)
        {
            return Double.POSITIVE_INFINITY;
        }

        //first term is query id, so it is ignored
        for(int stemIndex=1;stemIndex<queryStems.length;stemIndex++)
        {
            int stemDocIndex = termVector.indexOfStem(queryStems[stemIndex]);
            if(stemDocIndex ==-1)
            {
                continue;
            }

            double df = termVector.stemDf(stemDocIndex);

            //R.S.J
            double rsjWeight = 0.0;
            double N = (double) Idx.getNumDocs();
            rsjWeight = Math.log((N - df + 0.5) / (df + 0.5));
            rsjWeight = Math.max(0.0, rsjWeight);

            //term weight
            double docLength = (double) Idx.getFieldLength(field, docid);
            double avgDocLength = ((double) Idx.getSumOfFieldLengths(field)) / (double) (Idx.getDocCount(field));

            double tf = termVector.stemFreq(stemDocIndex);
            double k1 = RetrievalModelBM25.k1;
            double b = RetrievalModelBM25.b;
            double tfWeight = tf / (tf + (k1 * ((1.0 - b) + (b * (docLength / avgDocLength)))));


            // user weight is always 1
            double userWeight = ((RetrievalModelBM25.k3 + 1) * qtf) / (qtf + RetrievalModelBM25.k3);
            double score = rsjWeight * tfWeight * userWeight;

            totalScore += score;
        }

        return totalScore;
    }



}
