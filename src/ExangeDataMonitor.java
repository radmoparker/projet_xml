import java.util.ArrayList;

public class ExangeDataMonitor {
    private ArrayList<String> queries = new ArrayList<String>();

    public String getLastQueries(){
        if(queries.isEmpty()){
            return null;
        }
        return queries.removeLast();
    }
    public void addQuery(String query){
        this.queries.add(query);
    }
}
