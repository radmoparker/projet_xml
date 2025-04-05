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
    public void doWait(){
        synchronized(monitor){
            try{
                System.out.println(this.name+" : je  vais attendre");
                monitor.wait();
                String query = monitor.getLastQueries();
                if(query != null){
                    System.out.println("" + name + " A reçu comme requête : " + query);
                    System.out.println("Voici la réponse de la requête : ");
                    retrieveQuery(query);
                    System.out.println("\n\n");
                }
                this.doNotify();
            } catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
    }

    public void doNotify(){
        synchronized(monitor){
            monitor.notify();
            System.out.println(this.name+" :");
            if(requetes.size()>0){
                String query = requetes.remove(0);
                monitor.addQuery(query);


                System.out.println(this.name+" : j'ai notifié la requête : "+query);
                this.doWait();
            }
            else{
                System.out.println("Je n'ai plus rien à lire  aurevoir !");

            }
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
            this.doNotify();
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

    public void retrieveQuery(String query) throws Exception {
        File bdFile = new File(getClass().getResource(this.bdRepository).getFile());
        Document bd = xmlDocumentLoader(this.bdRepository);
        XPathFactory xpathfactory = XPathFactory.newInstance();

        XPath xpath = xpathfactory.newXPath();
        XPathExpression expr = xpath.compile(query);
        Object result = expr.evaluate(bd, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                System.out.println("Balise : " + element.getTagName());
                System.out.println("Contenu balise : " + element.getTextContent());
            }
        }
        Node filmNode = (Node) expr.evaluate(bd, XPathConstants.NODE);

        // Nouveau document xml à signer
        Document newDoc = xmlDocumentFromNode(filmNode);

        xmlDocumentDisplay(newDoc);


    }

    public Document xmlDocumentLoader(String path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(getClass().getResource(this.bdRepository).getFile());
        return doc;
    }



    public void xmlDocumentDisplay(Document document) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));

        String xmlString = writer.getBuffer().toString();
        System.out.println(xmlString);
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
