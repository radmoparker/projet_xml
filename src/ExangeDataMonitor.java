import java.util.ArrayList;

public class ExangeDataMonitor {
    private ArrayList<String> queries = new ArrayList<String>();

    public String getLastQueries(){
        return queries.getLast();
    }
    public void addQuery(String query){
        this.queries.add(query);
    }
}
