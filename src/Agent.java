import java.util.ArrayList;

/*doc pour echange de donnée entre threadjava :
* https://jenkov.com/tutorials/java-concurrency/thread-signaling.html
* */
public class  Agent extends Thread {
    private ArrayList<String> requetes = new ArrayList<String>();
    private  String queryRepository;
    private ExangeDataMonitor monitor;
    private String name;
    private String role;
   public Agent(ExangeDataMonitor monitor,String repository,String name,String role) {
       this.monitor = monitor;
       this.queryRepository = repository;
       requetes.add("a");
       requetes.add("b");
       requetes.add("c");
       requetes.add("d");
       this.name = name;
       this.role = role;
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
}
