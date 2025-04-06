
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.util.ArrayList;

/*IMPORTANT : documentation pour échange de donnée entre threadjava :
 * https://jenkov.com/tutorials/java-concurrency/thread-signaling.html
 * Permet de lier des thread par un point central qui est l'objet commun
 * */
public class  Agent extends Thread {
    private final String color;
    private ArrayList<Document> requetes = new ArrayList<Document>();

    private  String queryRepository;
    private  String bdRepository;

    private ExangeDataMonitor monitor;
    private String name;
    private String role;

    public Agent(ExangeDataMonitor monitor,String bdRepository,String queryRepository,String name,String role,String color) {
        this.monitor = monitor;
        this.bdRepository = bdRepository;
        this.queryRepository = queryRepository;
        this.name = name;
        this.role = role;
        this.color = color;
        try {
            xmlQueryFileOpener();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


    }


    private void manageData(ArrayList<Object>  data) throws Exception {
        if(data == null){
            System.out.println(this.color+this.name+"Il n'y plus rien à lire ! Aurevoir !");

        }else{
            String type = data.get(0).toString();
            switch(type){
                case "query":

                    Document documentAnswer =this.manageQuery((Document) data.get(1));
                    doNotify(documentAnswer);

                    break;
                case "answer":
                    this.manageAnswer(data.get(1));
                    doNotify(1,String.valueOf(data.get(1).hashCode()));
                    break;
                case "validation":
                    System.out.println(color+name+" : Mes donnés ont été validées ");
                    if(requetes.size()>0) {
                        Document query = requetes.remove(0);
                        this.doNotify(query,0);
                    }else{
                        System.out.println(color+name+" : Je n'ai plus rien à lire  aurevoir !");
                        this.doNotify(-1,"noDataLeft");
                    }
                    break;
                case "endOfEXchange":
                    System.out.println(color+name+" : Je n'ai plus rien à lire  aurevoir !");
                    break;
                default:
                    break;
            }

        }


    }

    private void manageAnswer(Object doc) throws Exception {
        if(doc == null){
            System.out.println(color+this.name+" : Ma signature Lors de l'envoie de la requête a été refusé !");
        }else{
            Document document= (Document) doc;
            String displayedAnswer = xmlDocumentDisplay(document);
            System.out.println(color+name+" : Voici la réponse à ma requête : \n"+displayedAnswer);
            verifySignature(doc,"Answer");
        }
    }

    public void verifySignature(Object doc,String type) {
        System.out.println(color+this.name+" : Vérification de la signature "+type+" : ");
    }

    public Document manageQuery(Document documentQuery) throws Exception {
        String xpathQuery = extractXPathQuery(documentQuery);
        System.out.println(color+name + " : J'ai reçu la requête : \n" + xmlDocumentDisplay(documentQuery));
        verifySignature(documentQuery,"Query");
        System.out.println(color+name+" : Voici la réponse que je vais envoyé : ");
        Document queryDocument = retrieveQuery(xpathQuery);
        Document wrapedQueryDocument = wrapWithQueryAndResult(queryDocument, xpathQuery);
        System.out.println(color+xmlDocumentDisplay(wrapedQueryDocument)+"\n");
        return wrapedQueryDocument;

    }
    public void doWait(){
        synchronized(monitor){
            try{
                System.out.println(color+this.name+" : je  vais attendre\n");
                monitor.wait();
                ArrayList<Object>  data = monitor.getLastData();
                manageData(data);
            } catch(Exception e){

                System.out.println(e.getMessage());
            }
        }
    }
    public void doNotify(Document query,int code) throws Exception {
        synchronized(monitor){
            monitor.notify();

            monitor.addQuery(query);
            System.out.println(color+this.name+" : j'ai notifié la requête : \n"+xmlDocumentDisplay(query));
            this.doWait();

        }
    }
    public void doNotify(int code,String validation){
        synchronized(monitor){
            monitor.notify();
            if(code == -1){
                monitor.notifyEndOfEXchange();
            }else{
                System.out.println(color+this.name+" : Je valide les données  ");
                monitor.addValidation(validation);
                this.doWait();
            }


        }
    }
    public void doNotify(Document document){
        synchronized(monitor){
            monitor.notify();
            this.monitor.addAnswer(document);
            this.doWait();

        }
    }

    @Override
    public void run() {
        if ("waiter".equals(role)) {
            this.doWait();
        } else {
            try {
                Thread.sleep(0); // Assure que agent2 commence à attendre avant la notification
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(requetes.size()>0) {
                Document query = requetes.remove(0);
                try {
                    this.doNotify(query,0);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }else{
                System.out.println(color+name+"Je n'ai plus rien à lire  aurevoir !");
            }
        }
    }

    /**
     * Foncrion Enregistre toutes les requetes du dossier correspondant aux requetes de l'agent
     * @throws Exception
     */
    public void xmlQueryFileOpener() throws Exception {
        File dossierQuery = new File(getClass().getResource(this.queryRepository).getFile());
        if (dossierQuery.exists() && dossierQuery.isDirectory()) {
            File[] fichiers = dossierQuery.listFiles();
            if (fichiers != null) {
                for (File fichier : fichiers) {
                    getQueryDocument(queryRepository+"/"+fichier.getName());
                }
            }
        }
    }

    /**
     * Fonction enregistre la requete correspondant au nom du fichier donnée en paramètre
     * @param name
     * @throws Exception
     */
    public void openQueryFile(String name) throws Exception {
        File file = new File(String.valueOf(getClass().getResource(name).getFile()));
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        while((line = reader.readLine()) != null){
            stringBuilder.append(line);
        }
        String query = stringBuilder.toString().split("<QUERY>")[1].split("</QUERY>")[0];

    }
    public Document getQueryDocument(String name) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File(getClass().getResource(name).getFile()));
        document.getDocumentElement().normalize();
        this.requetes.add(document);
        return document;
    }
    public String extractXPathQuery(Document xmlQuery) {
        NodeList queryNodes = xmlQuery.getElementsByTagName("QUERY");
        Element queryElement = (Element) queryNodes.item(0);
        String xpath = queryElement.getTextContent().trim();
        return xpath;

    }

    public Document retrieveQuery(String query) throws Exception {
        Document bd = null;
        try{
            bd = xmlDocumentLoader();

        }catch(Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        XPathFactory xpathfactory = XPathFactory.newInstance();

        XPath xpath = xpathfactory.newXPath();
        XPathExpression expr = xpath.compile(query);
        Object result = expr.evaluate(bd, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;


        Node filmNode = (Node) expr.evaluate(bd, XPathConstants.NODE);

        // Construction du document xml à signer
        Document newDoc = xmlDocumentFromNode(filmNode);

        return newDoc;


    }

    public Document xmlDocumentLoader() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(getClass().getResource(this.bdRepository).getFile()));
        return doc;
    }



    public String xmlDocumentDisplay(Document document) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));

        String xmlString = writer.getBuffer().toString();
        return xmlString;
    }


    public Document xmlDocumentFromNode(Node node) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder dbuilder = dbf.newDocumentBuilder();
        Document newDoc = dbuilder.newDocument();
        Node importedNode = newDoc.importNode(node, true);
        newDoc.appendChild(importedNode);
        return newDoc;
    }


    public  Document wrapWithQueryAndResult(Document filmDocument, String queryText) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document newDoc = builder.newDocument();

        Element rootElement = newDoc.createElement("ANSWER");
        newDoc.appendChild(rootElement);

        Element queryElement = newDoc.createElement("QUERY");
        queryElement.setTextContent(queryText);
        rootElement.appendChild(queryElement);

        Element resultElement = newDoc.createElement("RESULT");
        rootElement.appendChild(resultElement);

        Node filmNode = newDoc.importNode(filmDocument.getDocumentElement(), true);
        resultElement.appendChild(filmNode);

        return newDoc;
    }



}
