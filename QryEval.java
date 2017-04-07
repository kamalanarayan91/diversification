/*
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.1.2.
 */
import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *  This software illustrates the architecture for the portion of a
 *  search engine that evaluates queries.  It is a guide for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 */
public class QryEval {

  //  --------------- Constants and variables ---------------------

  private static final String USAGE =
    "Usage:  java QryEval paramFile\n\n";

  private static final String[] TEXT_FIELDS =
    { "body", "title", "url", "inlink" };

  private static final int INVALID = -1;
  private static int truncateLimit  = INVALID;
  private static String outputFilePath = "";

  //  --------------- Methods ---------------------------------------

  /**
   *  @param args The only argument is the parameter file name.
   *  @throws Exception Error accessing the Lucene index.
   */
  public static void main(String[] args) throws Exception {

    //  This is a timer that you may find useful.  It is used here to
    //  time how long the entire program takes, but you can move it
    //  around to time specific parts of your code.

    Timer timer = new Timer();
    timer.start ();

    //  Check that a parameter file is included, and that the required
    //  parameters are present.  Just store the parameters.  They get
    //  processed later during initialization of different system
    //  components.

    if (args.length < 1) {
      throw new IllegalArgumentException (USAGE);
    }

    Map<String, String> parameters = readParameterFile (args[0]);

    //  Open the index and initialize the retrieval model.

    Idx.open (parameters.get ("indexPath"));

    if(parameters.get("retrievalAlgorithm").toLowerCase().equals("letor"))
    {
      //TrainModels
      LeToR.getInstance().train();
      LeToR.getInstance().processQueries(parameters.get("queryFilePath"),outputFilePath);
    }
    else
    {
      RetrievalModel model = initializeRetrievalModel(parameters);
      //  Perform experiments.
      processQueryFile(parameters.get("queryFilePath"), model);

    }



    //  Clean up.
    timer.stop ();
    System.out.println ("Time:  " + timer);
  }

  /**
   *  Allocate the retrieval model and initialize it using parameters
   *  from the parameter file.
   *  @return The initialized retrieval model
   *  @throws IOException Error accessing the Lucene index.
   */
  private static RetrievalModel initializeRetrievalModel (Map<String, String> parameters)
    throws IOException {

    RetrievalModel model = null;
    String modelString = parameters.get ("retrievalAlgorithm").toLowerCase();

    if (modelString.equals("unrankedboolean")) {
      model = new RetrievalModelUnrankedBoolean();
    }
    else if(modelString.equals("rankedboolean")){
        model = new RetrievalModelRankedBoolean();
    }
    else if(modelString.equals("bm25")){
      model = new RetrievalModelBM25();
    }
    else if(modelString.equals("indri")){
      model = new RetrievalModelIndri();
    }
    else {
      throw new IllegalArgumentException
        ("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
    }

    return model;
  }

  /**
   * Print a message indicating the amount of memory used. The caller can
   * indicate whether garbage collection should be performed, which slows the
   * program but reduces memory usage.
   *
   * @param gc
   *          If true, run the garbage collector before reporting.
   */
  public static void printMemoryUsage(boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc)
      runtime.gc();

    System.out.println("Memory used:  "
        + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
  }

  /**
   * Process one query.
   * @param qString A string that contains a query.
   * @param model The retrieval model determines how matching and scoring is done.
   * @return Search results
   * @throws IOException Error accessing the index
   */
  static ScoreList processQuery(String qString, RetrievalModel model)
    throws IOException {

    String defaultOp = model.defaultQrySopName ();
    qString = defaultOp + "(" + qString + ")";
    Qry q = QryParser.getQuery (qString);

    // Show the query that is evaluated

    System.out.println("    --> " + q);

    if (q != null) {

      ScoreList r = new ScoreList ();

      if (q.args.size () > 0) {		// Ignore empty queries

        q.initialize (model);

        while (q.docIteratorHasMatch (model)) {
          int docid = q.docIteratorGetMatch ();
          double score = ((QrySop) q).getScore (model);
          r.add (docid, score);
          q.docIteratorAdvancePast (docid);
        }
      }

      return r;
    } else
      return null;
  }

  /**
   *  Process the query file.
   *  @param queryFilePath
   *  @param model
   *  @throws IOException Error accessing the Lucene index.
   */
  static void processQueryFile(String queryFilePath,
                               RetrievalModel model)
      throws IOException {

    BufferedReader input = null;
    BufferedWriter bufferedWriter = null;

    try {
      String qLine = null;

      input = new BufferedReader(new FileReader(queryFilePath));

      //  Each pass of the loop processes one query.
      File outputFile = new File(outputFilePath);
      FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
      bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));

      if(QryExpander.fb)
      QryExpander.initializeFileWriter();

      while ((qLine = input.readLine()) != null) {
        int d = qLine.indexOf(':');

        if (d < 0) {
          throw new IllegalArgumentException
            ("Syntax error:  Missing ':' in query line.");
        }

        printMemoryUsage(false);

        String qid = qLine.substring(0, d);
        String query = qLine.substring(d + 1);

        System.out.println("Query " + qLine);

        ScoreList r = null;
        HashMap<String,ScoreList> initialRanking = new HashMap<>();
        /**query expansion**/

        if(QryExpander.fb)
        { //if true

          if(QryExpander.fbInitialRankingFile !=null)
          {
              //get inital Ranking
              initialRanking = QryExpander.processInitialRankingFile();
          }
          else
          {
            r = processQuery(query, model);
            r.sort();
            initialRanking.put(qid,r);
          }

          String expandedQuery = QryExpander.getExpandedQuery(initialRanking.get(qid));
          QryExpander.printExpandedQueryToFile(expandedQuery,qid);
          query = QryExpander.getMergedQuery(query,expandedQuery);

        }

        /**End - query expansion**/
        r = processQuery(query, model);


        printResults(qid, r,bufferedWriter);

      }
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      input.close();
      bufferedWriter.close();

      if(QryExpander.fbExpansionQueryFile != null){
        QryExpander.closeFileWriter();
      }

    }
  }

  /**
   * Print the query results.
   *
   * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
   * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
   *
   * QueryID Q0 DocID Rank Score RunID
   *
   * @param queryId
   *          Original query.
   * @param result
   *          A list of document ids and scores
   * @throws IOException Error accessing the Lucene index.
   */
  static void printResults(String queryId, ScoreList result,BufferedWriter bufferedWriter) throws IOException {

    if (result == null || result.size() < 1)
    {

      bufferedWriter.write(queryId+" " +"Q0" + " "+ "dummyRecord"
              +" 1" +" "+ "0" +" bleh");
      bufferedWriter.newLine();
    }
    else
    {

      result.sort();

      if(truncateLimit!=INVALID)
      {
        result.truncate(truncateLimit);
      }
      else{
        result.truncate(100);
      }


      for (int i = 0; i < result.size() ; i++) {



        bufferedWriter.write(queryId+" " +"Q0" + " "+Idx.getExternalDocid(result.getDocid(i))
                +" "+(i+1)+" "+ String.format("%.12f",result.getDocidScore(i))+" bleh");
        bufferedWriter.newLine();

      }
    }


  }


  /**
   *  Read the specified parameter file, and confirm that the required
   *  parameters are present.  The parameters are returned in a
   *  HashMap.  The caller (or its minions) are responsible for processing
   *  them.
   *  @return The parameters, in <key, value> format.
   */
  private static Map<String, String> readParameterFile (String parameterFileName)
    throws IOException {

    Map<String, String> parameters = new HashMap<String, String>();

    File parameterFile = new File (parameterFileName);

    if (! parameterFile.canRead ()) {
      throw new IllegalArgumentException
        ("Can't read " + parameterFileName);
    }

    Scanner scan = new Scanner(parameterFile);
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split ("=");
      parameters.put(pair[0].trim(), pair[1].trim());

    } while (scan.hasNext());

    scan.close();

    if (! (parameters.containsKey ("indexPath") &&
           parameters.containsKey ("queryFilePath") &&
           parameters.containsKey ("trecEvalOutputPath") &&
           parameters.containsKey ("retrievalAlgorithm"))) {
      throw new IllegalArgumentException
        ("Required parameters were missing from the parameter file.");
    }

    if(parameters.containsKey("trecEvalOutputLength")){
      truncateLimit = Integer.parseInt(parameters.get("trecEvalOutputLength"));
    }

    outputFilePath = parameters.get("trecEvalOutputPath");

    //BM25
    if(parameters.containsKey("BM25:k_1")){
      RetrievalModelBM25.k1 = Double.parseDouble(parameters.get("BM25:k_1"));
      System.out.println("k1=" + RetrievalModelBM25.k1);
    }

    if(parameters.containsKey("BM25:k_3")){
      RetrievalModelBM25.k3 = Double.parseDouble(parameters.get("BM25:k_3"));
      System.out.println("k3=" + RetrievalModelBM25.k3);
    }

    if(parameters.containsKey("BM25:b")){
      RetrievalModelBM25.b = Double.parseDouble(parameters.get("BM25:b"));
      System.out.println("b=" + RetrievalModelBM25.b);
    }

    if(parameters.containsKey("Indri:mu")){
      RetrievalModelIndri.mu = Double.parseDouble(parameters.get("Indri:mu"));
      System.out.println("mu=" + RetrievalModelIndri.mu);
    }


    if(parameters.containsKey("Indri:lambda")){
      RetrievalModelIndri.lambda = Double.parseDouble(parameters.get("Indri:lambda"));
      System.out.println("lambda=" + RetrievalModelIndri.lambda);
    }


    if(parameters.containsKey("fb")){
      QryExpander.fb = Boolean.parseBoolean(parameters.get("fb"));
      System.out.println("fb=" + QryExpander.fb);
    }


    if(parameters.containsKey("fbDocs")){
      QryExpander.fbDocs = Integer.parseInt(parameters.get("fbDocs"));
      System.out.println("fbDocs=" + QryExpander.fbDocs);
    }

    if(parameters.containsKey("fbTerms")){
      QryExpander.fbTerms = Integer.parseInt(parameters.get("fbTerms"));
      System.out.println("fbTerms=" + QryExpander.fbTerms);
    }


    if(parameters.containsKey("fbMu")){
      QryExpander.fbMu = Integer.parseInt(parameters.get("fbMu"));
      System.out.println("fbMu=" + QryExpander.fbMu);
    }



    if(parameters.containsKey("fbOrigWeight")){
      QryExpander.fbOrigWeight = Double.parseDouble(parameters.get("fbOrigWeight"));
      System.out.println("fbOrigWeight=" + QryExpander.fbOrigWeight);
    }


    if(parameters.containsKey("fbInitialRankingFile")){
      QryExpander.fbInitialRankingFile = parameters.get("fbInitialRankingFile");
      System.out.println("fbOrigWeight=" + QryExpander.fbOrigWeight);
    }

    if(parameters.containsKey("fbExpansionQueryFile")){
      QryExpander.fbExpansionQueryFile = parameters.get("fbExpansionQueryFile");
      System.out.println("fbExpansionQueryFile=" + QryExpander.fbExpansionQueryFile);
    }


    //LeToR
    /**
     * letor:trainingQueryFile= A file of training queries.
     letor:trainingQrelsFile= A file of relevance judgments. Column 1 is the query id. Column 2 is ignored. Column 3 is the document id. Column 4 indicates the degree of relevance (0-2).
     letor:trainingFeatureVectorsFile= The file of feature vectors that your software will write for the training queries.
     letor:pageRankFile= A file of PageRank scores.
     letor:featureDisable= A comma-separated list of features to disable for this experiment. For example, "letor:featureDisable=6,9,12,15" disables all Indri features. If this parameter is missing, use all features.
     letor:svmRankLearnPath= A path to the svm_rank_learn executable.
     letor:svmRankClassifyPath= A path to the svm_rank_classify executable.
     letor:svmRankParamC= The value of the c parameter for SVMrank. 0.001 is a good default.
     letor:svmRankModelFile= The file where svm_rank_learn will write the learned model.
     letor:testingFeatureVectorsFile= The file of feature vectors that your software will write for the testing queries.
     letor:testingDocumentScores= The file of document scores that svm_rank_classify will write for the testing feature vectors.
     */
    if(parameters.containsKey("letor:trainingQueryFile")){
      LeToR.getInstance().setTrainingQueryFile(parameters.get("letor:trainingQueryFile"));
    }

    if(parameters.containsKey("letor:trainingQrelsFile")){
      LeToR.getInstance().setTrainingQrelsFile(parameters.get("letor:trainingQrelsFile"));
    }
    if(parameters.containsKey("letor:trainingFeatureVectorsFile")){
      LeToR.getInstance().setTrainingFeatureVectorsFile(parameters.get("letor:trainingFeatureVectorsFile"));
    }


    if(parameters.containsKey("letor:pageRankFile")){
      LeToR.getInstance().setPageRankFile(parameters.get("letor:pageRankFile"));
    }
    if(parameters.containsKey("letor:featureDisable")){
      LeToR.getInstance().setFeatureDisable(parameters.get("letor:featureDisable"));
    }
    if(parameters.containsKey("letor:svmRankLearnPath")){
      LeToR.getInstance().setSvmRankLearnPath(parameters.get("letor:svmRankLearnPath"));
    }
    if(parameters.containsKey("letor:svmRankClassifyPath")){
      LeToR.getInstance().setSvmRankClassifyPath(parameters.get("letor:svmRankClassifyPath"));
    }
    if(parameters.containsKey("letor:svmRankParamC")){
      LeToR.getInstance().setSvmRankParamC(Double.parseDouble(parameters.get("letor:svmRankParamC")));
    }
    if(parameters.containsKey("letor:svmRankModelFile")){
      LeToR.getInstance().setSvmRankModelFile(parameters.get("letor:svmRankModelFile"));
    }
    if(parameters.containsKey("letor:testingFeatureVectorsFile")){
      LeToR.getInstance().setTestingFeatureVectorsFile(parameters.get("letor:testingFeatureVectorsFile"));
    }
    if(parameters.containsKey("letor:testingDocumentScores")){
      LeToR.getInstance().setTestingDocumentScores(parameters.get("letor:testingDocumentScores"));
    }


    return parameters;
  }

}
