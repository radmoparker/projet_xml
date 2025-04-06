import org.w3c.dom.Document;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Objet central d'echange entre thread
 */
public class ExangeDataMonitor {
    private List<String> validation = new ArrayList<>() {
    };
    private ArrayList<Document> answer = new ArrayList<Document>();
    private ArrayList<Document> query = new ArrayList<Document>();

    private ArrayList<String> symbol = new ArrayList<String>();
    private ArrayList<Document> signedQueries = new ArrayList<Document>();
    private ArrayList<Document> signedAnswer = new ArrayList<Document>();

    /**
     * Récupération haut de la pile = dernier échange
     * @return
     */
    public ArrayList<Object> getLastData(){
        if( symbol.isEmpty()){
            return null;
        }
        ArrayList<Object> rep = new ArrayList<>();
        String symb = symbol.removeLast();
        switch(symb){
            case "query":
                rep.add("query");
                rep.add(signedQueries.removeLast());
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

    /**
     * aJOUT REQUETE
     * @param signedQuery
     * @param query
     */
    public void addQuery(Document signedQuery,Document query){
        this.query.add(query);
        this.symbol.add("query");
        this.signedQueries.add(signedQuery);
    }

    /**Ajout reponse
     *
     * @param signedAnswer
     * @param answer
     */
    public void addAnswer(Document signedAnswer, Document answer){
        this.answer.add(answer);
        this.signedAnswer.add(signedAnswer);
        this.symbol.add("answer");

    }

    /**Ajout validation
     *
     * @param validation
     */
    public void addValidation(String validation){
        this.validation.add(validation);
        this.symbol.add("validation");

    }

    /**
     * Fin d'échange
     */
    public void notifyEndOfEXchange() {
        this.symbol.add("endOfEXchange");
    }
}
