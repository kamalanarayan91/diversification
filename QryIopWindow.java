import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by sharath on 17/2/17.
 */
public class QryIopWindow extends QryIop {

    private int n;

    public QryIopWindow(int n){
        this.n = n;
    }



    protected void evaluate () throws IOException
    {
        //  Create an empty inverted list.  If there are no query arguments,
        //  that's the final result.

        this.invertedList = new InvList (this.getField());

        if (args.size () == 0) {
            return;
        }

        while(true)
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
            boolean abort = false;

            QryIop minArg = null;
            while(!abort)
            {

                int min = Integer.MAX_VALUE;
                int max = Integer.MIN_VALUE;

                for(Qry arg:args)
                {
                    //this one has reached end of list
                    if(  ! ((QryIop) arg).locIteratorHasMatch())
                    {
                        abort = true;
                        break;
                    }
                    int currMatch = ((QryIop) arg).locIteratorGetMatch();

                    max = Math.max(currMatch,max);

                    if(min > currMatch)
                    {
                        min = currMatch;
                        minArg = (QryIop)arg;

                    }
                }

                if(abort)
                {
                    break;
                }

                int currentSize = 1 + max-min;

                if(currentSize>n)
                {
                    //advance min
                    minArg.locIteratorAdvance();

                }
                else
                {
                    matches.add(max);

                    //advance all
                    advanceAll(args);
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


    public boolean advanceAll(List<Qry> args)
    {
        boolean abort = false;

        for(Qry arg: args)
        {
            ((QryIop) arg).locIteratorAdvance();

        }

        return abort;

    }
}
