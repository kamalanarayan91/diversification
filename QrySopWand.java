import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by sharath on 17/2/17.
 */
public class QrySopWand extends QrySop {
    private double weightSum;
    private ArrayList<Double> weights;
    public QrySopWand()
    {
        weights = new ArrayList<>();
        weightSum = 0.0;
    }

    public void addWeight(double weight){
        weights.add(weight);
        weightSum += weight;
    }

    public void removeWeight(){
        weightSum = weightSum-weights.get(weights.size()-1);
        weights.remove(weights.size()-1);
    }



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

        if(r instanceof RetrievalModelIndri){
            return getScoreIndri(r);
        }
        else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SCORE operator.");
        }

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
            int weightIndex=0;
            for(Qry arg : args){

                if(! arg.docIteratorHasMatchCache() || arg.docIteratorGetMatch() != currentMatch)
                {

                    score  = score * Math.pow(((QrySop) arg).getDefaultScore(r,currentMatch), weights.get(weightIndex)/weightSum);
                }
                else
                {
                    score = score * Math.pow(((QrySop) arg).getScore(r), weights.get(weightIndex)/weightSum);
                }
                weightIndex++;
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

        int weightIndex=0;
        for(Qry arg:args)
        {
            score  = score * Math.pow(((QrySop) arg).getDefaultScore(r,docId) ,weights.get(weightIndex)/weightSum );
            weightIndex++;
        }

        return score;
    }
}
