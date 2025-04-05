import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

public class ExangeDataMonitor {
    private List<Object> data = new ArrayList<>() {
    };
    private ArrayList<Document> answer = new ArrayList<Document>();
    private ArrayList<String> symbol = new ArrayList<String>();


    public ArrayList<Object> getLastData(){
        if(data.isEmpty() || symbol.isEmpty()){
            return null;
        }
        ArrayList<Object> rep = new ArrayList<>();
        String symb = symbol.removeLast();
        switch(symb){
            case "query":
                rep.add("query");
                rep.add(data.removeLast());
                break;
            case "answer":
                rep.add("answer");
                rep.add(data.removeLast());
                break;
            default:
                return null;
        }

        return rep;
    }
    public void addQuery(String query){
        this.data.add(query);
        this.symbol.add("query");
    }
    public void addAnswer(Document answer){
        this.data.add(answer);
        this.symbol.add("answer");

    }

}
