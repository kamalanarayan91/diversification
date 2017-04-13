import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by sharath on 7/4/17.
 */
public class DiversificationAlgorithmPM2 extends DiversificationAlgorithm
{
    public ScoreList getDiversifiedRanking(ScoreList initialRankings, ArrayList<ScoreList> intentRankings)
    {


        int resultLength = Diversifier.getInstance().getMaxResultRankingLength();
        int inputLength = initialRankings.size();

        int totalSize = 0;
        ScoreList result = new ScoreList();
        double numIntents = intentRankings.size();
        double v = resultLength/numIntents;

        double[] s = new double[intentRankings.size()];
        int selectedIntent = 0;
        int startIndex = 0;
        double lambda = Diversifier.getInstance().getLambda();

        //preprocessScores
        HashMap<Integer, ArrayList<Double>> scoreMap = preProcessScores(initialRankings,intentRankings);
        HashSet<Integer> selectedDocs = new HashSet<Integer>();
        double lastpm2Score = 0.0;

        while(totalSize<resultLength)
        {
            double[] qt = new double[intentRankings.size()];
            selectedIntent=0;

            // select the intent if all qt scores
            // equal else choose first intent
            double qtintent = Double.MIN_VALUE;
            for(int qtindex=0;qtindex<qt.length;qtindex++)
            {
                qt[qtindex] = v/ (2*s[qtindex] + 1);
                if(qt[qtindex] > qtintent)
                {
                    selectedIntent = qtindex;
                    qtintent = qt[qtindex];
                }
            }

            if(totalSize==0)
            {
                selectedIntent=0;
            }


            double selectedDocScore = Double.MIN_VALUE;
            int selectedDocId = -1;

            int zeroCount=0;
            for(int initialIntentIndex=0; initialIntentIndex<inputLength;initialIntentIndex++)
            {
                int docid = initialRankings.getDocid(initialIntentIndex);

                if(selectedDocs.contains(docid))
                {
                    continue;
                }

                //calculate score;
                ArrayList<Double> scores =  scoreMap.get(docid);
                double pm2score = 0.0;
                int scoreSize =scores.size();
                for(int scoreIndex=0;scoreIndex<scoreSize;scoreIndex++)
                {
                    if(scoreIndex!=selectedIntent){
                        pm2score += (1-lambda)*qt[scoreIndex]*scores.get(scoreIndex);
                    }
                    else{
                        pm2score += lambda*(qt[selectedIntent]  *scores.get(scoreIndex));
                    }
                }
                if(pm2score>0.0){
                    lastpm2Score  = pm2score;
                }
                if(pm2score==0.0){
                    zeroCount++;
                }

                if(pm2score>selectedDocScore)
                {
                    selectedDocId=docid;
                    selectedDocScore = pm2score;
                }
            }


            if(selectedDocId==-1)
            {

                if(zeroCount !=0 && zeroCount + selectedDocs.size()== inputLength){
                    System.out.println("zerocount detected");
                    double relevanceScore = lastpm2Score-0.0001;

                    for (int initialIndex = 0; initialIndex < inputLength; initialIndex++) {
                        int docid = initialRankings.getDocid(initialIndex);
                        if (selectedDocs.contains(docid)) {
                            continue;
                        } else {
                            totalSize++;

                            result.add(docid,relevanceScore);
                            relevanceScore = relevanceScore-0.0001;

                        }
                    }

                    //everything is zero from this point

                }
                else{
                    System.out.println("Error in PM2 Scores!!");
                }

            }
            else {

                selectedDocs.add(selectedDocId);

                result.add(selectedDocId, selectedDocScore);


                //update coverage of each intent
                ArrayList<Double> scores = scoreMap.get(selectedDocId);
                double sum = 0.0;

                for (double d : scores) {
                    sum += d;
                }
                for (int index = 0; index < s.length; index++) {
                    s[index] += scores.get(index) / sum;
                }

                totalSize++;
                startIndex++;
            }

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
