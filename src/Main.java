import java.io.File;

public class Main {
    public static void main(String[] args) {
        String YELLOW = "\u001B[33m";
        String BLUE = "\u001B[34m";
        String PURPLE = "\u001B[35m";
        String CYAN = "\u001B[36m";
        String red = "\u001B[31m";
        String green = "\u001B[32m";
        ExangeDataMonitor monitor = new ExangeDataMonitor();
        Agent agent1 = new Agent(monitor,"ressources/bd_xml_1.xml","requetes_1","batman",null,BLUE);
        Agent agent2 = new Agent(monitor,"ressources/bd_xml_2.xml","requetes_2","robin","waiter",YELLOW);
        agent2.start();
        agent1.start();

    }
}