import java.util.Comparator;

/**
 * Created by sharath on 16/3/17.
 */
public class ExpansionTerm implements Comparable<ExpansionTerm>
{

    public String term;
    public double score;

    public int compareTo(ExpansionTerm e2)
    {
        return Double.compare(score,e2.score);
    }

    public ExpansionTerm(String term, double score)
    {
        this.term = term;
        this.score = score;
    }


}
