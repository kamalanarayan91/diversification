
import org.apache.lucene.index.Term;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

/**
 * Created by sharath on 15/3/17.
 */
public class QryExpander
{

    /**
     * fb= Acceptable values are "true" and "false". This value controls whether query expansion is performed (fb=true).
     fbDocs= Acceptable values are integers > 0. This value determines the number of documents to use for query expansion.
     fbTerms= Acceptable values are integers > 0. This value determines the number of terms that are added to the query.
     fbMu= Acceptable values are integers >= 0. This value determines the amount of smoothing used to calculate p(r|d).
     fbOrigWeight= Acceptable values are between 0.0 and 1.0. This value determines the weight on the original query. The weight on the expanded query is (1-fbOrigWeight).
     fbInitialRankingFile= The value is a string that contains the name of a file (in trec_eval input format) that contains an initial document ranking for the query.
     fbExpansionQueryFile= The value is a string that contains the name of a file where your software must write its expansion query. The file format is described below.
     */

    /**
     * Populated by parameters file
     */

    public static final int INVALID = -1;
    public static boolean fb = false;
    public static int fbDocs = INVALID;
    public static int fbTerms = INVALID;
    public static int fbMu = INVALID;
    public static double fbOrigWeight = INVALID;
    public static String fbInitialRankingFile = null;
    public static String fbExpansionQueryFile = null;

    private static BufferedWriter queryWriter = null;


    public static String getExpandedQuery(ScoreList scoreList) throws IOException
    {

        scoreList.truncate(fbDocs);


        HashMap<Integer, TermVector> termVectorHashMap = new HashMap<>();
        HashSet<String> potentialTerms = new HashSet<>();

        for(int index=0;index<fbDocs;index++)
        {
            int docId = scoreList.getDocid(index);
            double score = scoreList.getDocidScore(index);

            TermVector termVector = new TermVector(docId,"body");
            termVectorHashMap.put(docId,termVector);

            //get potential Terms
            int totalTerms = termVector.stemsLength();

            for(int termIndex =0; termIndex < totalTerms; termIndex++)
            {
                String term = termVector.stemString(termIndex);

                if(term==null)
                {
                    continue;
                }

                if(term.contains(".") || term.contains(","))
                {
                    continue;
                }

                potentialTerms.add(term);

            }

        }

        //get Scores for Top N terms - Interview ftw!
        PriorityQueue<ExpansionTerm> topTerms = new PriorityQueue<>();
        for(String term: potentialTerms)
        {

            //calculate the score of the term in each document
            double modC = (double) Idx.getSumOfFieldLengths("body");
            double ctf = Idx.getTotalTermFreq("body",term);
            double pMLE = ctf/modC;
            double logScore = Math.log(1.0/pMLE);
            double termScore = 0.0;

            for(int docIndex=0;docIndex<fbDocs;docIndex++)
            {

                int docId = scoreList.getDocid(docIndex);
                TermVector termVector = termVectorHashMap.get(docId);


                double docOriginalScore = scoreList.getDocidScore(docIndex);


                double docLength = Idx.getFieldLength("body",docId);
                int index = 0;
                double tfd = 0.0;

                int stemIndex= termVector.indexOfStem(term);

                if(stemIndex != -1 )
                {
                    tfd = termVector.stemFreq(stemIndex);
                }

                double ptd = (tfd + (fbMu*pMLE))/(docLength + fbMu);

                termScore += ptd * logScore * docOriginalScore;

            }

            ExpansionTerm expansionTerm = new ExpansionTerm(term,termScore);

            if(topTerms.size()<fbTerms)
            {
                topTerms.add(expansionTerm);
            }
            else
            {
                if(topTerms.peek().score < expansionTerm.score)
                {
                    topTerms.poll();
                    topTerms.add(expansionTerm);
                }

            }


        }


        StringBuilder expandedQuery = new StringBuilder();
        expandedQuery.append("#WAND(");
        System.out.println("topSize:"+ topTerms.size());
        for(ExpansionTerm e: topTerms)
        {
            expandedQuery.append(" "+ String.format("%.4f",e.score) + " " + e.term);
        }

        expandedQuery.append(")");
        System.out.println(expandedQuery.toString());

        return expandedQuery.toString();
    }



    public static HashMap<String,ScoreList> processInitialRankingFile() throws IOException
    {
        HashMap<String,ScoreList> result = new HashMap<>();

        BufferedReader input = new BufferedReader(new FileReader(fbInitialRankingFile));
        String line;
        while(true)
        {

            line = input.readLine();
            if(line==null){
                break;
            }
            String[] splits = line.split("\\s+");
            String qid = splits[0];
            String externalDocId = splits[2];
            String score = splits[4];

            try {
                

                if (result.containsKey(qid)) {
                    ScoreList scoreList = result.get(qid);
                    scoreList.add(Idx.getInternalDocid(externalDocId), Double.parseDouble(score));
                } else {
                    ScoreList scoreList = new ScoreList();
                    scoreList.add(Idx.getInternalDocid(externalDocId), Double.parseDouble(score));
                    result.put(qid, scoreList);
                }
            }
            catch (java.lang.Exception e){
                e.printStackTrace();
            }
        }

        input.close();
        return result;
    }


    public static String getMergedQuery(String originalQuery, String expandedQuery)
    {
        StringBuilder mergedQuery = new StringBuilder();
        mergedQuery.append("#wand( ");
        mergedQuery.append(String.format("%.2f",1.0-fbOrigWeight) +" " + expandedQuery );
        mergedQuery.append(" " + String.format("%.2f",fbOrigWeight) +" #and(" +  originalQuery+ ")" );
        mergedQuery.append(")");
        return mergedQuery.toString();
    }

    public static void printExpandedQueryToFile(String expandedQuery,String qid) throws IOException
    {

        queryWriter.write(qid + ": " + expandedQuery);

    }

    public static void initializeFileWriter() throws IOException
    {
        //  Each pass of the loop processes one query.
        File outputFile = new File(fbExpansionQueryFile);
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        queryWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));

    }

    public static void closeFileWriter() throws IOException
    {
        queryWriter.close();
    }
}
