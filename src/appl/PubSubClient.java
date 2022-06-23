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
    private String backupAddress = null;
    private int backupPort = 0;

    private boolean isPrimary = true;

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

        this.brokerAddress = brokerAddress;
        this.brokerPort = brokerPort;

        this.backupAddress = backupAddress;
        this.backupPort = backupPort;

        try {
            Message msgBroker = new MessageImpl();
            msgBroker.setBrokerId(brokerPort);
            msgBroker.setType("sub");
            msgBroker.setContent(clientAddress+":"+clientPort);
            Client subscriber = new Client(brokerAddress, brokerPort, null);
            Message response = subscriber.sendReceive(msgBroker);
            if(response != null && response.getType().equals("backup")){			
                brokerAddress = response.getContent().split(":")[0];
                brokerPort = Integer.parseInt(response.getContent().split(":")[1]);
                subscriber = new Client(brokerAddress, brokerPort, null);
                subscriber.sendReceive(msgBroker);
            }

            Client subscriberBackup = new Client(brokerAddress, brokerPort, null);
            Message msgBrokerBackup = new MessageImpl();
            msgBrokerBackup.setBrokerId(brokerPort);
            msgBrokerBackup.setType("giveMeSec");
            msgBrokerBackup.setContent(clientAddress+":"+clientPort);
            Message responseInfos = subscriberBackup.sendReceive(msgBrokerBackup);

            if (responseInfos != null) {
                this.backupPort = responseInfos.getBrokerId();
                this.backupAddress = responseInfos.getContent();
                System.out.println("backUpAddress: " + backupAddress);
            }
    
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
            Client subscriber = new Client(brokerAddress, brokerPort, null);
            Message response = subscriber.sendReceive(msgBroker);
    
            if (response != null && response.getType().equals("backup")) {
                brokerAddress = response.getContent().split(":")[0];
                brokerPort = Integer.parseInt(response.getContent().split(":")[1]);
                subscriber = new Client(brokerAddress, brokerPort, null);
                subscriber.sendReceive(msgBroker);
            }
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publish(String message, String brokerAddress, int brokerPort) {

        if (!isPrimary) {
			brokerAddress = this.backupAddress;
			brokerPort = this.backupPort;
		}
        
        try {
            Message msgPub = new MessageImpl();
            msgPub.setBrokerId(brokerPort);
            msgPub.setType("pub");
            msgPub.setContent(message);
            
            Client publisher = new Client(brokerAddress, brokerPort, () -> {
                this.isPrimary = false;
    
                subscribe(backupAddress, backupPort, backupAddress, backupPort);
                try {
                    Client publisher2 = new Client(backupAddress, backupPort, null);
                    Message msgPubAux = new MessageImpl();
                    msgPubAux.setBrokerId(backupPort);
                    msgPubAux.setType("updatePrimary");
                    msgPubAux.setContent(message);

                    Message response = publisher2.sendReceive(msgPubAux);

                    if(response != null && response.getType().equals("backup")){
                        this.brokerAddress = response.getContent().split(":")[0];
                        this.brokerPort = Integer.parseInt(response.getContent().split(":")[1]);
                        publisher2 = new Client(this.brokerAddress, this.brokerPort, null);
                        publisher2.sendReceive(msgPub);
                    }
                } catch (Exception e) {
                    System.out.println("[CLIENT] Client cannot connect with ");
                }
               
            });

            Message response = publisher.sendReceive(msgPub);

            if (response != null && response.getType().equals("backup")) {
                brokerAddress = response.getContent().split(":")[0];
                brokerPort = Integer.parseInt(response.getContent().split(":")[1]);
                publisher = new Client(brokerAddress, brokerPort, null);
                publisher.sendReceive(msgPub);
            }
        }  catch (Exception e) {
                System.out.println("[CLIENT] Client cannot connect with ");
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
