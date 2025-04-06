
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;

/*IMPORTANT : documentation pour échange de donnée entre threadjava :
 * https://jenkov.com/tutorials/java-concurrency/thread-signaling.html
 * Permet de lier des thread par un point central qui est l'objet commun
 * */
public class  Agent extends Thread {
    private final String color;
    private final PrivateKey signatureKey;
    private final PublicKey validationKey;
    private  Document bd;

    private ArrayList<Document> requetes = new ArrayList<Document>();

    private  String queryRepository;
    private  String bdRepository;

    private ExangeDataMonitor monitor;
    private String name;
    private String role;

    public Agent(ExangeDataMonitor monitor, String bdRepository, String queryRepository, String name, String role, String color, PrivateKey signatureKey, PublicKey validationKey) {
        this.monitor = monitor;
        this.bdRepository = bdRepository;
        this.queryRepository = queryRepository;
        this.name = name;
        this.role = role;
        this.color = color;
        this.signatureKey = signatureKey;
        this.validationKey = validationKey;
        try {
            this.bd = xmlDocumentLoader();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                //Ici on répond à une requête
                case "query":
                    ArrayList<Document> documentAnswer =this.manageQuery((Document) data.get(2),(Document) data.get(1));

                    doNotify(documentAnswer);

                    break;
               //Ici on traite la réponse à notre requête
                case "answer":
                    this.manageAnswer(data.get(1));
                    doNotify(1,String.valueOf(data.get(1).hashCode()));
                    break;
                //Ici on consulte la validation de notre réponse par l'interlocuteur
                case "validation":
                    System.out.println(color+name+" : Mes données ont été validées ");
                    if(requetes.size()>0) {
                        Document query = requetes.remove(0);
                        System.out.println(color+this.name+" : je vais notifié la REQUETE : \n"+xmlDocumentDisplay(query));
                        Document signedQuery = signDocument(copyXmlDocument(query),"REQUETE");
                        this.doNotify(query,signedQuery,0);
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

    /**
     * lA SIGNATURE DU DOCUMENT EST FAITE À PARTIR DU STANDARD W3C DISPONIBLE VERS CE LIEN :
     * https://docs.oracle.com/javase/8/docs/technotes/guides/security/xmldsig/XMLDigitalSignature.html
     * @param document  Document à signer
     * @return le document signé
     */
    public Document signDocument(Document document,String type) throws Exception{
        DOMSignContext dsc = new DOMSignContext(signatureKey, document.getDocumentElement());
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        Reference ref = fac.newReference
                ("", fac.newDigestMethod(DigestMethod.SHA256, null),
                        Collections.singletonList
                                (fac.newTransform(Transform.ENVELOPED,
                                        (TransformParameterSpec) null)), null, null);
        //Singnature info object : l'objet qui va être signé
        SignedInfo si = fac.newSignedInfo
                (fac.newCanonicalizationMethod
                                (CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
                                        (C14NMethodParameterSpec) null),
                        fac.newSignatureMethod("http://www.w3.org/2009/xmldsig11#dsa-sha256", null),
                        Collections.singletonList(ref));
        //Va contenir les infos pour récupéré la clé
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        //Création de la valeur de la clé
        KeyValue kv = kif.newKeyValue(validationKey);
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(kv));

        //Création de l'objet XMLSignature ,  contient les objets SignedInfo and KeyInfo crées plus haut:
        XMLSignature signature = fac.newXMLSignature(si, ki);

        //Génération de la signature
        signature.sign(dsc);
        System.out.println(color+name+" Voici la "+type+" signée  : \n"+xmlDocumentDisplay(document));

        return document;
    }

    private void manageAnswer(Object doc) throws Exception {
        if(doc == null){
            System.out.println(color+this.name+" : Ma signature Lors de l'envoie de la requête a été refusé !");
        }else{
            Document document= (Document) doc;
            String displayedAnswer = xmlDocumentDisplay(document);
            System.out.println(color+name+" : Voici la réponse à ma requête : \n"+displayedAnswer);
            verifySignature(document,"RÉPONSE");
        }
    }

    public void verifySignature(Object document,String type) throws Exception {
        System.out.println(color+this.name+" : Vérification de la signature de la "+type+"  ");

            //Instantiation du document à signer
            Document doc = (Document) document;

            NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (nl.getLength() == 0) {
                throw new Exception(color+name+" : Le document n'a pas de signature !");
            }
            //Context pour valider la signature
            DOMValidateContext valContext = new DOMValidateContext(validationKey, nl.item(0));
            //Création d'un objet signature à parrtir de la signature
            XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");

            XMLSignature signature = factory.unmarshalXMLSignature(valContext);

            //Validation de la signature
            boolean coreValidity = signature.validate(valContext);
            System.out.println(color+name+" : Validation de la signature : "+(coreValidity?"Signature validé avec ma clé publique ":"Signature refusé avec ma clé publique"));
    }

    public ArrayList<Document> manageQuery(Document query, Document signedQueryDocument) throws Exception {
        String xpathQuery = extractXPathQuery(query);
        System.out.println(color+name + " : J'ai reçu la REQUETE : \n" + xmlDocumentDisplay(signedQueryDocument));
        verifySignature(signedQueryDocument,"REQUETE");
        System.out.println(color+name+" : Voici la RÉPONSE que je vais envoyé : ");
        Document queryDocument = retrieveQuery(xpathQuery);
        System.out.println(color+xmlDocumentDisplay(queryDocument)+"\n");

        Document wrapedResultDocument = wrapWithQueryAndResult(queryDocument, xpathQuery);
        Document signedResultDocument = signDocument(wrapedResultDocument,"RÉPONSE");
        ArrayList<Document> answers = new ArrayList<>();
        answers.add(signedResultDocument);
        answers.add(wrapedResultDocument);
        return answers;

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
    public void doNotify(Document query,Document signedQuery,int code) throws Exception {
        synchronized(monitor){
            monitor.notify();

            monitor.addQuery(signedQuery,query);


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
    public void doNotify(ArrayList<Document> documents){
        synchronized(monitor){
            monitor.notify();
            this.monitor.addAnswer(documents.get(0),documents.get(1));
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
                    System.out.println(color+this.name+" : je vais notifié la REQUETE : \n"+xmlDocumentDisplay(query));
                    Document signedQuery = signDocument(copyXmlDocument(query),"REQUETE");
                    this.doNotify(query,signedQuery,0);
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
        for (int i = 1; i <= 5; i++) {
            String queryPath = this.queryRepository + "/query_" + i + ".xml";
            URL queryURL = getClass().getResource(queryPath);
            if (queryURL != null) {
                getQueryDocument(queryURL);
            } else {
                System.err.println("Fichier non trouvé: " + queryPath);
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
    public Document getQueryDocument(URL queryURL) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(queryURL.openStream()); // Utilisation de l'InputStream de l'URL
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

        InputStream inputStream = getClass().getResourceAsStream(this.bdRepository);

        if (inputStream == null) {
            throw new FileNotFoundException("Ressource non trouvée : " + this.bdRepository);
        }

        Document doc = builder.parse(inputStream);
        inputStream.close();

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

    public static Document copyXmlDocument(Document document) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document newDoc = builder.newDocument();

            Node copiedNode = newDoc.importNode(document.getDocumentElement(), true);
            newDoc.appendChild(copiedNode);

            return newDoc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
