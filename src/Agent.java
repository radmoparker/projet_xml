import javax.xml.parsers.DocumentBuilderFactory;
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
       } catch (FileNotFoundException e) {
           throw new RuntimeException(e);
       } catch (IOException e) {
           throw new RuntimeException(e);
       }


   }
    public void doWait(){
        synchronized(monitor){
            try{
                System.out.println(this.name+" : je  vais attendre");
                monitor.wait();
                String m = monitor.getLastQueries();
                System.out.println(""+name+" lit message : "+m);
                this.doNotify();
            } catch(InterruptedException e){
                System.out.println("Agent interrupted !");
            }
        }
    }

    public void doNotify(){
        synchronized(monitor){
            monitor.notify();
            System.out.println(this.name+" : je  vais notifier");
            if(requetes.size()>0){
                monitor.addQuery(requetes.remove(0));


                System.out.println(this.name+" : j'ai notifié");
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
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.doNotify();
        }
    }

    /**
     * Foncrion Enregistre toutes les requetes du dossier correspondant aux requetes de l'agent
     * @throws IOException
     */
    public void xmlQueryFileOpener() throws IOException {
        File dossierQuery = new File(getClass().getResource(this.queryRepository).getFile());
        if (dossierQuery.exists() && dossierQuery.isDirectory()) {
            File[] fichiers = dossierQuery.listFiles();

            if (fichiers != null) {
                for (File fichier : fichiers) {
                    openQueryFile(fichier.getName());
                }
            }
        }



    /*
        FileInputStream fileIS = new FileInputStream(this.getFile());
        DocumentBuilderFactory builderFactory = newSecureDocumentBuilderFactory();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(fileIS);
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/Tutorials/Tutorial";
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);


        test ne focntionne pas :         FileInputStream fileIS = new FileInputStream(file.getAbsoluteFile());

     */

    }

    /**
     * Fonction enregistre la requete correspondant au nom du fichier donnée en paramètre
     * @param fileName
     * @throws IOException
     */
    public void openQueryFile(String fileName) throws IOException {
        File file = new File(getClass().getResource(this.queryRepository+"/"+fileName).getFile());
        FileInputStream fileIS = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        while((line = reader.readLine()) != null){
            stringBuilder.append(line);
        }
        String query = stringBuilder.toString().split("<QUERY>")[1].split("</QUERY>")[0];
        this.requetes.add(query);
    }
}
