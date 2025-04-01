public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");

        ExangeDataMonitor monitor = new ExangeDataMonitor();
        Agent agent1 = new Agent(monitor,"vide","batman",null);
        Agent agent2 = new Agent(monitor,"vide","robin","waiter");
        agent2.start();
        agent1.start();

    }
}