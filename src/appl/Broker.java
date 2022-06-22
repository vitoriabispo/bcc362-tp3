package appl;

import core.Server;

import java.util.Scanner;

public class Broker {

    public Broker() {
        Scanner reader = new Scanner(System.in);  // Reading from System.in
        System.out.print("Enter the Broker port number: ");
        int port = reader.nextInt(); // Scans the next token of the input as an int.

        System.out.print("Is the broker primary? (Y|N): ");
        boolean isPrimary = reader.next().equalsIgnoreCase("Y");

        String auxAdress;
        int auxPort;

        if(isPrimary) { 
            System.out.print("Enter the secundary Broker address: ");
            auxAdress = reader.next();

            System.out.print("Enter the secundary Broker port number: ");
            auxPort = reader.nextInt();
        }
        else {
            System.out.print("Enter the primary Broker address: ");
            auxAdress = reader.next();

            System.out.print("Enter the primary Broker port number: ");
            auxPort = reader.nextInt();
        }

        Server server = new Server(port, isPrimary, auxAdress, auxPort);
        ThreadWrapper brokerThread = new ThreadWrapper(server);
        brokerThread.start();

        System.out.print("Shutdown the broker? (Y|N): ");
        boolean shutdown = reader.next().equalsIgnoreCase("Y");
        if (shutdown) {
            System.out.println("Broker stopped...");
            server.stop();
            brokerThread.interrupt();
        }

        //once finished
        reader.close();
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        new Broker();
    }

    class ThreadWrapper extends Thread {
        Server server;

        public ThreadWrapper(Server server) {
            this.server = server;
        }

        public void run() {
            server.begin();
        }
    }

}
