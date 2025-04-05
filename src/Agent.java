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
import java.net.URL;
import java.util.ArrayList;

/*doc pour echange de donnée entre threadjava :
* https://jenkov.com/tutorials/java-concurrency/thread-signaling.html
* Permet de lier des thread par un point central qui est l'objet commun
* */
public class  Agent extends Thread {
    private ArrayList<String> requetes = new ArrayList<String>();

    private  String queryRepository;
    private  String bdRepository;

    private ExangeDataMonitor monitor;
    private String name;
    private String role;

   public Agent(ExangeDataMonitor monitor,String bdRepository,String queryRepository,String name,String role) {
       this.monitor = monitor;
       this.bdRepository = bdRepository;
       this.queryRepository = queryRepository;
       this.name = name;
       this.role = role;
       try {
           xmlQueryFileOpener();
       } catch (Exception e) {
           System.out.println(e.getMessage());
       }


   }


    private void manageData(ArrayList<Object>  data) throws Exception {
       if(data == null){
           System.out.println("Il n'y plus rien à lire ! Aurevoir !");

       }else{
           String type = data.get(0).toString();
           switch(type){
               case "query":
                   Document documentAnswer =this.manageQuery(data.get(1).toString().toString());
                   doNotify(documentAnswer);

                   break;
               case "answer":
                   this.manageAnswer(data.get(1));
                   doNotify(1,String.valueOf(data.get(1).hashCode()));
                   break;
               case "validation":
                   System.out.println(name+" : Mes donnés ont été validées ");
                   if(requetes.size()>0) {
                       String query = requetes.remove(0);
                       this.doNotify(query);
                   }else{
                       System.out.println(name+" : Je n'ai plus rien à lire  aurevoir !");
                       this.doNotify(-1,"noDataLeft");
                   }
                   break;
               case "endOfEXchange":
                   System.out.println(name+" : Je n'ai plus rien à lire  aurevoir !");
                   break;
               default:
                   break;
           }

       }


    }

    private void manageAnswer(Object doc) throws Exception {
       Document document= (Document) doc;
       String displayedAnswer = xmlDocumentDisplay(document);
       System.out.println(name+" : Voici la réponse à ma requête : \n"+displayedAnswer);
    }

    public Document manageQuery(String query) throws Exception {
            System.out.println("" + name + " : J'ai reçu la requête : " + query);
            System.out.println(name+" : Voici la réponse que je vais envoyé : ");
            Document queryDocument = retrieveQuery(query);
            System.out.println(xmlDocumentDisplay(queryDocument));
            System.out.println("\n");
            return queryDocument;

    }
    public void doWait(){
        synchronized(monitor){
            try{
                System.out.println(this.name+" : je  vais attendre");
                monitor.wait();
                ArrayList<Object>  data = monitor.getLastData();
                manageData(data);
            } catch(Exception e){

                System.out.println(e.getMessage());
            }
        }
    }
    public void doNotify(String query){
        synchronized(monitor){
            monitor.notify();

            monitor.addQuery(query);
            System.out.println(this.name+" : j'ai notifié la requête : "+query);
            this.doWait();

        }
    }
    public void doNotify(int code,String validation){
        synchronized(monitor){
            monitor.notify();
            if(code == -1){
                monitor.notifyEndOfEXchange();
            }else{
                System.out.println(this.name+" : Je valide les données  ");
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
                String query = requetes.remove(0);
                this.doNotify(query);
            }else{
                System.out.println("Je n'ai plus rien à lire  aurevoir !");
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
                    openQueryFile(queryRepository+"/"+fichier.getName());
                }
            }
        }
    }

    /**
     * Fonction enregistre la requete correspondant au nom du fichier donnée en paramètre
     * @param fileName
     * @throws Exception
     */
    public void openQueryFile(String fileName) throws Exception {
        File file = new File(String.valueOf(getClass().getResource(fileName).getFile()));
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        while((line = reader.readLine()) != null){
            stringBuilder.append(line);
        }
        String query = stringBuilder.toString().split("<QUERY>")[1].split("</QUERY>")[0];
        this.requetes.add(query);
    }

    public Document retrieveQuery(String query) throws Exception {
        File bdFile = new File(getClass().getResource(this.bdRepository).getFile());
        Document bd = xmlDocumentLoader(this.bdRepository);
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

    public Document xmlDocumentLoader(String path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(getClass().getResource(this.bdRepository).getFile());
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



}
