package appl;

import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import core.Message;
import core.MessageImpl;
import core.Server;
import core.client.Client;

public class PubSubClient {

    private Server observer;
    private ThreadWrapper clientThread;

    private String clientAddress;
    private int clientPort;

    private String brokerAddress;
    private int brokerPort;
    private String backupAddress;
    private int backupPort;

    public PubSubClient() {
        //this constructor must be called only when the method
        //startConsole is used
        //otherwise the other constructor must be called
    }

    public PubSubClient(String clientAddress, int clientPort) {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        observer = new Server(clientPort);
        clientThread = new ThreadWrapper(observer);
        clientThread.start();
    }

    public void subscribe(String brokerAddress, int brokerPort, String backupAddress, int backupPort) {

        try {
            Message msgBroker = new MessageImpl();
            msgBroker.setBrokerId(brokerPort);
            msgBroker.setType("sub");
            msgBroker.setContent(clientAddress + ":" + clientPort);
            Client subscriber = new Client(brokerAddress, brokerPort);
            Message response = subscriber.sendReceive(msgBroker);
            if (response.getType().equals("backup")) {
                brokerAddress = response.getContent().split(":")[0];
                brokerPort = Integer.parseInt(response.getContent().split(":")[1]);
                subscriber = new Client(brokerAddress, brokerPort);
                subscriber.sendReceive(msgBroker);
            }
    
            this.brokerAddress = brokerAddress;
            this.brokerPort = brokerPort;
            this.backupAddress = backupAddress;
            this.backupPort = backupPort;
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unsubscribe(String brokerAddress, int brokerPort) {

        try {
            Message msgBroker = new MessageImpl();
            msgBroker.setBrokerId(brokerPort);
            msgBroker.setType("unsub");
            msgBroker.setContent(clientAddress + ":" + clientPort);
            Client subscriber = new Client(brokerAddress, brokerPort);
            Message response = subscriber.sendReceive(msgBroker);
    
            if (response.getType().equals("backup")) {
                brokerAddress = response.getContent().split(":")[0];
                brokerPort = Integer.parseInt(response.getContent().split(":")[1]);
                subscriber = new Client(brokerAddress, brokerPort);
                subscriber.sendReceive(msgBroker);
            }
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publish(String message, String brokerAddress, int brokerPort) {
        
        try {
            Message msgPub = new MessageImpl();
            msgPub.setBrokerId(brokerPort);
            msgPub.setType("pub");
            msgPub.setContent(message);
            
            Client publisher = new Client(brokerAddress, brokerPort);

            Message response = publisher.sendReceive(msgPub);

            if (response.getType().equals("backup")) {
                brokerAddress = response.getContent().split(":")[0];
                brokerPort = Integer.parseInt(response.getContent().split(":")[1]);
                publisher = new Client(brokerAddress, brokerPort);
                publisher.sendReceive(msgPub);
            }
        }  catch (Exception e) {
            String messageError = e.getMessage();
            if(messageError.equals("Client cannot connect")) {
                try {
                    System.out.println("Client cannot connect, change to secondary: " + backupAddress + ":" + backupPort);

                    Message msgBroker = new MessageImpl();
                    msgBroker.setBrokerId(brokerPort);
                    msgBroker.setContent("Backup becomes primary: " + backupAddress + ":" + backupPort);
                    msgBroker.setType("syncNewPrimary");

                    Client clientBackup = new Client(backupAddress, backupPort);
                    clientBackup.sendReceive(msgBroker);

                    // Troca os endereços do broker
                    brokerAddress = backupAddress;
                    brokerPort = backupPort;

                    // Espera um tempo para poder reenviar o pub
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException se) {
                        se.printStackTrace();
                    }

                    Message msgPub = new MessageImpl();
                    msgPub.setBrokerId(brokerPort);
                    msgPub.setType("pub");
                    msgPub.setContent(message);

                    Client publisher = new Client(brokerAddress, brokerPort);
                    Message response = publisher.sendReceive(msgPub);

                    if (response != null) {
                        if(response.getType().equals("backup")){
                            brokerAddress = response.getContent().split(":")[0];
                            brokerPort = Integer.parseInt(response.getContent().split(":")[1]);
                            publisher = new Client(brokerAddress, brokerPort);
                            publisher.sendReceive(msgPub);
                        }
                    }
                }  catch (Exception e2) {
                    System.out.println("[CLIENT 2] Client cannot connect with ");
                }
            } 
        }
    }

    public List<Message> getLogMessages() {
        return observer.getLogMessages();
    }

    public void stopPubSubClient() {
        System.out.println("Client stopped...");
        observer.stop();
        clientThread.interrupt();
    }

    class ThreadWrapper extends Thread {
        Server s;

        public ThreadWrapper(Server s) {
            this.s = s;
        }

        public void run() {
            s.begin();
        }
    }

}
