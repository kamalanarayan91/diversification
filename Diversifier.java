import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by sharath on 7/4/17.
 */
public class Diversifier
{
    /**
     * diversity= Acceptable values are "true" and "false". This value controls whether diversity is performed (fb=true).
     diversity:initialRankingFile= The value is a string that contains the name of a file (in trec_eval input format) that contains document rankings for queries and query intents.
     diversity:maxInputRankingsLength= Acceptable values are integers > 0. This value determines the (maximum) number of documents in the relevance ranking and the intent rankings that your software should use for diversification. Your software should ignore documents below this ranking.
     diversity:maxResultRankingLength= Acceptable values are integers > 0. This value determines the number of documents in the diversified ranking that your software will produce.
     diversity:algorithm= Acceptable values are "PM2" and "xQuAD" (case does not matter). This value controls the diversification algorithm used by your software.
     diversity:intentsFile= The path to the query intents file.
     diversity:lambda= Acceptable values are in the range [0.0, 1.0].
     */

    private boolean diversity;
    private String initialRankingFile;
    private int maxInputRankingsLength;
    private int maxResultRankingLength;
    private String algorithm;
    private String intentsFile;
    private double lambda;

    private static Diversifier instance = null;

    private HashMap<String,ArrayList<String>> intentsMap;
    private HashMap<String,ScoreList> initialRankingsMap;
    private HashMap<String,ArrayList<ScoreList>> intentRankingsMap;


    private DiversificationAlgorithm diversificationAlgorithm;

    public int getMaxInputRankingsLength() {
        return maxInputRankingsLength;
    }

    public int getMaxResultRankingLength() {
        return maxResultRankingLength;
    }

    public static Diversifier getInstance()
    {
        if(instance ==null)
        {
            instance = new Diversifier();
        }

        return instance;
    }

    public void setDiversity(boolean diversity) {
        this.diversity = diversity;
    }

    public void setInitialRankingFile(String initialRankingFile) {
        this.initialRankingFile = initialRankingFile;
    }

    public void setMaxInputRankingsLength(int maxInputRankingsLength) {
        this.maxInputRankingsLength = maxInputRankingsLength;
    }

    public void setMaxResultRankingLength(int maxResultRankingLength) {
        this.maxResultRankingLength = maxResultRankingLength;
    }

    public void setAlgorithm(String algorithm) throws Exception
    {
        this.algorithm = algorithm;

        if(algorithm.toLowerCase().equals("pm2")){
            diversificationAlgorithm = new DiversificationAlgorithmPM2();
        }
        else if(algorithm.toLowerCase().equals("xquad")){
            diversificationAlgorithm = new DiversificationAlgorithmXQuad();
        }
        else
        {
            throw new Exception("Unkown diversification Algorithm");
        }
    }

    public void setIntentsFile(String intentsFile) {
        this.intentsFile = intentsFile;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    public boolean isDiversifierEnabled()
    {
        return this.diversity;
    }

    public String getInitialRankingFile(){
        return this.initialRankingFile;
    }


    public void parseIntents() throws Exception
    {
        if(intentsMap!=null)
        {
            return;
        }

        intentsMap = new HashMap<>();
        BufferedReader bufferedWriter = new BufferedReader(new FileReader(intentsFile));

        String line  = null;
        HashMap<String, ArrayList<String>> result = new HashMap<>();
        while((line = bufferedWriter.readLine())!=null)
        {
            String[] splits = line.split(":");

            //query id and intent id seperation
            int dotIndex = splits[0].indexOf('.');
            String queryId = splits[0].substring(0,dotIndex);

            if (!result.containsKey(queryId))
            {
                result.put(queryId,new ArrayList<String>());
            }

            ArrayList<String> intentList = result.get(queryId);

            intentList.add(splits[1].trim());

        }

        intentsMap = result;



    }

    public ArrayList<String> getIntents( String queryId) throws Exception
    {
        if(intentsMap==null)
        {
            parseIntents();
        }

        return intentsMap.get(queryId);
    }

    public ScoreList diversify(ScoreList initialRankings, ArrayList<ScoreList> intentRankings, boolean regFlag)
    {
        if(regFlag)
        {
            regularize(initialRankings, intentRankings);
        }
        else
        {
            initialRankings.sort();
            initialRankings.truncate(maxInputRankingsLength);

            for(ScoreList scorelist : intentRankings)
            {
                scorelist.sort();
                scorelist.truncate(maxInputRankingsLength);
            }
        }


        return diversificationAlgorithm.getDiversifiedRanking(initialRankings,intentRankings);
    }

    public ArrayList<ScoreList> getIntentRankings (String queryId)
    {
        return intentRankingsMap.get(queryId);
    }

    public ScoreList getInitialRanking(String queryId) throws Exception
    {
        if(initialRankingsMap!=null)
        {
            return initialRankingsMap.get(queryId);
        }
        if(initialRankingsMap == null)
        {
            initialRankingsMap = new HashMap<>();
            intentRankingsMap = new HashMap<>();

        }

        FileInputStream fileInputStream = new FileInputStream(initialRankingFile);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
        HashMap<String,String> prevIntentIdMap = new HashMap<>();
        String line;
        while((line = bufferedReader.readLine())!=null)
        {
            //System.out.println(line);
           // System.out.println(line);
            String[] splits = line.split("\\s+");
            String qid = splits[0];
            String externalDocId = splits[2];
            double score = Double.parseDouble(splits[4]);
            int docid = Idx.getInternalDocid(externalDocId);

            if(!qid.contains("."))
            {
                ScoreList res;
                if (initialRankingsMap.containsKey(qid))
                {
                    res = initialRankingsMap.get(qid);
                }
                else
                {
                    res = new ScoreList();
                    initialRankingsMap.put(qid, res);
                }

                res.add(docid, score);
            }
            else
            {
                int dotIndex = qid.indexOf('.');
                String id = qid.substring(0,dotIndex);
                String intentId = qid.substring(dotIndex+1);


                if(intentRankingsMap.containsKey(id))
                {
                    ArrayList<ScoreList> scoreLists = intentRankingsMap.get(id);
                    if(!prevIntentIdMap.get(id).equals(intentId))
                    {
                        scoreLists.add(new ScoreList());
                        prevIntentIdMap.put(id,intentId);

                    }
                    scoreLists.get(scoreLists.size()-1).add(docid,score);

                }
                else
                {
                    //new intent
                    ArrayList<ScoreList> scoreLists = new ArrayList<ScoreList>();
                    intentRankingsMap.put(id,scoreLists);

                    scoreLists.add(new ScoreList());
                    scoreLists.get(scoreLists.size()-1).add(docid,score);
                    prevIntentIdMap.put(id,intentId);

                }

            }

        }
        return initialRankingsMap.get(queryId);
    }

    public double getLambda() {
        return lambda;
    }

    public void regularize(ScoreList initialRankings, ArrayList<ScoreList> intentRankings)
    {
        initialRankings.sort();
        int truncateLimit = Math.min(maxInputRankingsLength,initialRankings.size());
        initialRankings.truncate(truncateLimit);

        double max = Double.MIN_VALUE;
        int index=0;
        int size = initialRankings.size();
        double sum = 0;
        HashSet<Integer> includeDocs= new  HashSet<>();
        for(;index< size;index++)
        {
            sum = sum + initialRankings.getDocidScore(index);
            includeDocs.add(initialRankings.getDocid(index));
        }

        max= sum;



        for(ScoreList scorelist : intentRankings)
        {
            sum=0;
            scorelist.sort();
            scorelist.truncate(truncateLimit);


            size = scorelist.size();
            for(index=0;index< size;index++){
                if(includeDocs.contains(scorelist.getDocid(index)))
                    sum = sum + scorelist.getDocidScore(index);
            }

            if(max<sum)
            {
                max= sum;
            }
        }

        //normalize
        size = truncateLimit;
        for(index=0;index< size;index++)
        {

            initialRankings.setDocidScore(index,initialRankings.getDocidScore(index)/max);
        }


        for(ScoreList scorelist : intentRankings)
        {
            size = scorelist.size();
            for(index=0;index< size;index++)
            {
                if(includeDocs.contains(scorelist.getDocid(index)))
                scorelist.setDocidScore(index,scorelist.getDocidScore(index)/max);
            }

        }


        return;

    }

}
