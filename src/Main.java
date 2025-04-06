import java.io.File;
import java.security.*;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;

public class Main {
    public static void main(String[] args) {
        String yellow = "\u001B[33m";
        String blue = "\u001B[34m";
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("DSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }


        kpg.initialize(2048);
        KeyPair kpA = kpg.generateKeyPair();
        KeyPair kpB = kpg.generateKeyPair();

        PublicKey publicKeyA = kpA.getPublic();
        PrivateKey privateKeyA = kpA.getPrivate();

        PublicKey publicKeyB = kpB.getPublic();
        PrivateKey privateKeyB = kpB.getPrivate();


        ExangeDataMonitor monitor = new ExangeDataMonitor();
        Agent agent1 = new Agent(monitor,"/bd_xml_1.xml","/requetes_1","BATMAN",null,blue,privateKeyA,publicKeyB);
        Agent agent2 = new Agent(monitor,"/bd_xml_2.xml","/requetes_2","ROBIN","waiter",yellow,privateKeyB,publicKeyA);
        agent2.start();
        agent1.start();

    }
}