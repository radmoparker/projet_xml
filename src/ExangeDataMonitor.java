import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

public class ExangeDataMonitor {
    private List<String> validation = new ArrayList<>() {
    };
    private ArrayList<Document> answer = new ArrayList<Document>();
    private ArrayList<Document> query = new ArrayList<Document>();

    private ArrayList<String> symbol = new ArrayList<String>();


    public ArrayList<Object> getLastData(){
        if( symbol.isEmpty()){
            return null;
        }
        ArrayList<Object> rep = new ArrayList<>();
        String symb = symbol.removeLast();
        switch(symb){
            case "query":
                rep.add("query");
                rep.add(query.removeLast());
                break;
            case "answer":
                rep.add("answer");
                rep.add(answer.removeLast());
                break;
            case "validation":
                rep.add("validation");
                rep.add(validation.removeLast());
                break;
            case "endOfEXchange":
                rep.add("endOfEXchange");
                rep.add(null);
                break;
            default:
                return null;
        }

        return rep;
    }
    public void addQuery(Document query){
        this.query.add(query);
        this.symbol.add("query");
    }
    public void addAnswer(Document answer){
        this.answer.add(answer);
        this.symbol.add("answer");

    }
    public void addValidation(String validation){
        this.validation.add(validation);
        this.symbol.add("validation");

    }


    public void notifyEndOfEXchange() {
        this.symbol.add("endOfEXchange");
    }
}
