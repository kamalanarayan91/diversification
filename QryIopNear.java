/**
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 *


 */
import java.io.*;
import java.util.*;

/**
 *  The NEAR/n operator for all retrieval models.
 */
public class QryIopNear extends QryIop {

  public static final int NOMATCH = -1;
  public static final int ABORT = -2;

  private int n;
  /**
   * Constructor
   */
  public QryIopNear(int n)
  {
    this.n = n;
  }
  /**
   *  Evaluate the query operator; the result is an internal inverted
   *  list that may be accessed via the internal iterators.
   *  @throws IOException Error accessing the Lucene index.
   *
   */
  protected void evaluate () throws IOException {

    //  Create an empty inverted list.  If there are no query arguments,
    //  that's the final result.

    this.invertedList = new InvList (this.getField());

    if (args.size () == 0) {
      return;
    }

    //  Each pass of the loop adds 1 document to result inverted list
    //  until all of the argument inverted lists are depleted.

    while (true)
    {
      // If there is no document that match all the query arguments
      // we are done
      if(! docIteratorHasMatchAll(null))
      {
        return;
      }


      int docId = args.get(0).docIteratorGetMatch();


      //System.out.println("Matching doc:" + docId);

      ArrayList<Integer> matches = new ArrayList<Integer>();
      int match = 0;

      while(match!=ABORT)
      {
        //iterate through every document's location list
        int argIndex = 0;
        while (argIndex <= args.size() - 1) {

          if (argIndex == 0)
          {

            match = getLocationMatch((QryIop)args.get(0),(QryIop)args.get(1));
            argIndex += 2;
            if(match==ABORT){
              break;
            }
          }
          else
          {
            match = getLocationMatch(match, (QryIop)args.get(argIndex));
            if (match == ABORT)
            {
              break;
            }
            else if(match==NOMATCH)
            {
              argIndex=0;
            }
            else
            {
              argIndex++;
            }
          }
        }

        if (match != ABORT  && match != NOMATCH)
        {
          matches.add(match);
        }
      }

      if(matches.size()>0)
      {
        Collections.sort(matches);

        invertedList.appendPosting(docId,matches);
      }



      args.get(0).docIteratorAdvancePast(docId);

    }
  }

  /**
   * Looking for the first match between any two documents
   * @param first
   * @param second
   * @return
   */
  public int  getLocationMatch(QryIop first, QryIop second)
  {
    int result = ABORT;

    //repeat till first match is found
    while(first.locIteratorHasMatch() && second.locIteratorHasMatch())
    {
      int firstLoc = first.locIteratorGetMatch();
      int secondLoc = second.locIteratorGetMatch();
      if(first.locIteratorGetMatch() >= second.locIteratorGetMatch())
      {
        second.locIteratorAdvance();

      }
      else if (second.locIteratorGetMatch() - first.locIteratorGetMatch() <= n)
      {
        result = second.locIteratorGetMatch();
        first.locIteratorAdvance();
        second.locIteratorAdvance();
        break;
      }
      else
      {
        first.locIteratorAdvance();
      }

    }

    return result;
  }

  /**
   * Look for matches based on previous match in the current inverted list
   * @param previous
   * @param current
   * @return
   */
  public int getLocationMatch(int previous, QryIop current)
  {
    int result = NOMATCH;

    //repeat till first match is found
    while(current.locIteratorHasMatch())
    {
      int temp = current.locIteratorGetMatch();
      if(previous >= current.locIteratorGetMatch())
      {
        current.locIteratorAdvance();
      }
      else if (current.locIteratorGetMatch() - previous <= n)
      {
        result = current.locIteratorGetMatch();
        current.locIteratorAdvance();
        return result;

      }
      else
      {
        break;
      }

    }

    if(!current.locIteratorHasMatch())
    {
      return ABORT;
    }
    //no match
    return result;
  }

}
