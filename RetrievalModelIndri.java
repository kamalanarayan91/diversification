/**
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

/**
 *  An object that stores parameters for the unranked Boolean
 *  retrieval model (there are none) and indicates to the query
 *  operators how the query should be evaluated.
 */
public class RetrievalModelIndri extends RetrievalModel
{
    public static final double INVALID = -1;

    public static double mu = INVALID;
    public static double lambda = INVALID;



    public String defaultQrySopName ()
    {
        return new String ("#and");
    }

    public static double getLeToRScore(int docid, String field, String[] queryStems) throws Exception
    {
        double totalScore = 1.0;
        //p mle (q/c)


        TermVector termVector = new TermVector(docid,field);

        //get potential Terms
        int totalTerms = termVector.stemsLength();
        if(totalTerms==0)
        {
            return Double.POSITIVE_INFINITY;
        }

        boolean hasMatch = false;
        for(int stemIndex=1;stemIndex<queryStems.length;stemIndex++)
        {

            double ctf = Idx.getTotalTermFreq(field,queryStems[stemIndex]);
            double modC = (double) Idx.getSumOfFieldLengths(field);
            double pMLE = ctf / modC;

            double tf = 0;
            int docStemIndex = termVector.indexOfStem(queryStems[stemIndex]);
            if(docStemIndex!=-1)
            {
                hasMatch=true;
                tf = termVector.stemFreq(docStemIndex);
            }

            double mu = RetrievalModelIndri.mu;
            double lambda = RetrievalModelIndri.lambda;

            double docLength = (double) Idx.getFieldLength(field, docid);

            double score = (1 - lambda) * ((tf + (mu * pMLE)) / (docLength + mu));
            score +=lambda * pMLE;

            totalScore *= Math.pow(score,1.0/((double)(queryStems.length-1)));
        }

        if(!hasMatch)
            return 0.0;

        return totalScore;

    }



}
