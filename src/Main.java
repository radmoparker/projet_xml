import java.io.File;

public class Main {
    public static void main(String[] args) {

        ExangeDataMonitor monitor = new ExangeDataMonitor();
        Agent agent1 = new Agent(monitor,"ressources/bd_xml_1.xml","requetes_1","batman",null);
        Agent agent2 = new Agent(monitor,"ressources/bd_xml_2.xml","requetes_2","robin","waiter");
        agent2.start();
        agent1.start();

    }
}