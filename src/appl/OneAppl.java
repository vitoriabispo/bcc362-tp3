package appl;

import java.util.Iterator;
import java.util.List;

import core.Message;

public class OneAppl {

    public OneAppl() {
       // PubSubClient client = new PubSubClient();
        // client.startConsole();
    }

    public OneAppl(boolean flag) {
        PubSubClient joubert = new PubSubClient("localhost", 8082);
        PubSubClient debora = new PubSubClient("localhost", 8083);
        PubSubClient jonata = new PubSubClient("localhost", 8084);

        char selected = 'X';
        Thread accessOne;
        Thread accessTwo;
        Thread accessThree;
        List<Message> logJoubert;
        List<Message> logDebora;
        List<Message> logJonata;

        joubert.subscribe("localhost", 8080, "localhost", 8081);        
        debora.subscribe("localhost", 8080, "localhost", 8081);
        jonata.subscribe("localhost", 8080, "localhost", 8081);

        for (int i = 0; i < 100; i++) {
            
            accessOne = new ThreadWrapper(joubert, "joubert" + ":acquire:" + selected, "localhost", 8080);
            accessTwo = new ThreadWrapper(debora, "debora" + ":acquire:" + selected, "localhost", 8080);
            accessThree = new ThreadWrapper(jonata, "jonata" + ":acquire:" + selected, "localhost", 8080);

            accessOne.start();
            accessTwo.start();
            accessThree.start();

            try {
                accessTwo.join();
                accessOne.join();
                accessThree.join();
            } catch (Exception e) {
    
            }

            logJoubert = joubert.getLogMessages();
            logDebora = debora.getLogMessages();
            logJonata = jonata.getLogMessages();
    
            treatLog(logJoubert);
            treatLog(logDebora);
            treatLog(logJonata);
        }



        joubert.unsubscribe("localhost", 8080);
        debora.unsubscribe("localhost", 8080);
        jonata.unsubscribe("localhost", 8080);

        joubert.stopPubSubClient();
        debora.stopPubSubClient();
        jonata.stopPubSubClient();
    }

    private void treatLog(List<Message> logUser) {
		// aqui existe toda a logica do protocolo do TP2
		// se permanece neste metodo ate que o acesso a VAR X ou VAR Y ou VAR Z ocorra
		Iterator<Message> it = logUser.iterator();
		System.out.print("Log User itens: ");
		while (it.hasNext()) {
			Message aux = it.next();
			System.out.print(aux.getContent() + aux.getLogId() + " | ");
		}
		System.out.println();
	}

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        new OneAppl(true);
    }

    class ThreadWrapper extends Thread {
        PubSubClient c;
        String msg;
        String host;
        int port;

        public ThreadWrapper(PubSubClient c, String msg, String host, int port) {
            this.c = c;
            this.msg = msg;
            this.host = host;
            this.port = port;
        }

        public void run() {
            c.publish(msg, host, port);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
