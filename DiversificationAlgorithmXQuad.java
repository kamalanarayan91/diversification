import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by sharath on 7/4/17.
 */
public class DiversificationAlgorithmXQuad extends DiversificationAlgorithm
{
    public ScoreList getDiversifiedRanking(ScoreList initialRankings, ArrayList<ScoreList> intentRankings)
    {

        int resultLength = Diversifier.getInstance().getMaxResultRankingLength();
        int inputLength = initialRankings.size();

        int totalSize = 0;
        ScoreList result = new ScoreList();
        double numIntents = intentRankings.size();
        int selectedIntent = 0;

        //preprocessScores
        HashMap<Integer, ArrayList<Double>> scoreMap = preProcessScores(initialRankings,intentRankings);
        HashSet<Integer> selectedDocs = new HashSet<Integer>();
        double pqiq = 1.0/ numIntents;
        double lambda = Diversifier.getInstance().getLambda();

        while(totalSize<resultLength)
        {
            int selectedDocId = -1;
            double selectedDocScore = Double.MIN_VALUE;

            for(int index=0;index<inputLength;index++)
            {
                int docId = initialRankings.getDocid(index);
                double initialScore = initialRankings.getDocidScore(index);

                if(selectedDocs.contains(docId))
                {
                    continue;
                }

                double score = 0.0;

                score = (1-lambda)* initialScore;
                double second = 0.0;


                for(int intentIndex=0;intentIndex<intentRankings.size();intentIndex++)
                {
                    double innerSecond = 0.0;
                    double intentScore = scoreMap.get(docId).get(intentIndex);

                    innerSecond += pqiq* intentScore;

                    for(int innerSelectedDocId : selectedDocs)
                    {
                        ArrayList<Double> innerScores = scoreMap.get(innerSelectedDocId);
                        double innerScore = innerScores.get(intentIndex);
                        innerSecond *= (1 - innerScore);

                    }

                    second +=innerSecond;

                }

                score += lambda*second;

                if(score > selectedDocScore)
                {
                    selectedDocId = docId;
                    selectedDocScore = score;
                }

            }

            /*
            try{
                System.out.println(selectedDocId);
                System.out.println(Idx.getExternalDocid(selectedDocId) + " " + selectedDocScore);
            }
            catch (Exception e){}*/


            selectedDocs.add(selectedDocId);

            result.add(selectedDocId,selectedDocScore);
            totalSize++;
        }


        result.sort();
        return result;
    }

    private HashMap<Integer, ArrayList<Double>> preProcessScores(ScoreList initialRankings, ArrayList<ScoreList> intentRankings)
    {
        HashMap<Integer, ArrayList<Double>> result = new HashMap<>();
        int size = initialRankings.size();

        for(int index=0;index<size;index++)
        {
            ArrayList<Double> dummy = new ArrayList<Double>();
            for(int inn=0;inn<intentRankings.size();inn++)
            {
                dummy.add(0.0);
            }
            result.put(initialRankings.getDocid(index),dummy);
        }




        for(int intentIndex=0;intentIndex<intentRankings.size();intentIndex++)
        {
            ScoreList intentList = intentRankings.get(intentIndex);

            size = intentList.size();

            for(int index=0;index<size;index++)
            {
                int docId = intentList.getDocid(index);


                ArrayList<Double> res = result.get(docId);
                if(res==null)
                    continue;

                res.set(intentIndex,intentList.getDocidScore(index));

            }
        }


        return result;

    }


}
