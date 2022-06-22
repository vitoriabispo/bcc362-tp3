package appl;

import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Collections;

import core.Message;

public class SingleUser {

	public static void main(String[] args) {
		new SingleUser();
	}

	public SingleUser() {
		Scanner reader = new Scanner(System.in); // Reading from System.in
		// System.out.print("Enter the Broker port number: ");
		// int brokerPort = reader.nextInt();

		// System.out.print("Enter the Broker address: ");
		// String brokerAdd = reader.next();

		System.out.print("Enter the User name: ");
		String userName = reader.next();

		System.out.print("Enter the User port number: ");
		int userPort = reader.nextInt();

		// System.out.print("Enter the User address: ");
		// String userAdd = reader.next();

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PubSubClient user = new PubSubClient("localhost", userPort);

		user.subscribe("localhost", 8080, "localhost", 8080);

		startTP2(user, userName, 8080, "localhost");

		reader.close();
	}

	private void startTP2(PubSubClient user, String userName, int brokerPort, String brokerAdd) {
		System.out.println("User " + userName + " entered the system!\n");
		Thread sendOneMsg;

		for (int i = 0; i < 2; i++) {
			String selected = "X";
			boolean proceed = false;

			sendOneMsg = new ThreadWrapper(user, userName + ":acquire:" + selected, brokerAdd, brokerPort);
			sendOneMsg.start();

			try {
				sendOneMsg.join();
			} catch (Exception e) {
				e.printStackTrace();
			}

			while (!proceed) {
				List<Message> logUser = user.getLogMessages();
				List<Message> logUserReverse = user.getLogMessages();
				int n = logUser.size();
				Collections.reverse(logUserReverse);
				if (n == 1) {
					proceed = handleAcquire(sendOneMsg, user, userName, brokerPort, brokerAdd, selected);
				} else {
					Iterator<Message> it = logUserReverse.iterator();
					String releaseUsername = "";

					boolean hasRelease = false;
					int indexRelease = 0;

					// Pegar a ultima release
					while (it.hasNext()) {
						Message aux = it.next();
						if (aux.getContent().split(":")[1].equals("release")) {
							String[] currentLogs = aux.getContent().split(":");
							releaseUsername = currentLogs[0];
							hasRelease = true;
							break;
						}
						indexRelease += 1;
					}

					if (hasRelease) {
						it = logUserReverse.iterator();
						Message next = null;
						int index = logUser.size() - 1;
						int j = 0;
						while (it.hasNext()) {
							Message aux = it.next();
							String[] currentLogs = aux.getContent().split(":");
							String currentUsername = currentLogs[0];
							String currentLogType = currentLogs[1];

							if (j > indexRelease && currentUsername.equals(releaseUsername)
									&& currentLogType.equals("acquire")) {
								next = logUser.get(index + 1);
								String nextTypeLog = next.getContent().split(":")[1];

								while (nextTypeLog.equals("release")) {
									next = logUser.get(index + 1);
									nextTypeLog = next.getContent().split(":")[1];
									index++;
								}
								break;
							}

							index -= 1;
							j++;
						}

						if (next != null) {
							String nextUsername = next.getContent().split(":")[0];
							if (nextUsername.equals(userName)) {
								proceed = handleAcquire(sendOneMsg, user, userName, brokerPort, brokerAdd, selected);
							} else {
								System.out.println(
										"The resource " + selected + " is *not* available for you. Please wait");
							}
						}

					}
				}

				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}

		user.unsubscribe(brokerAdd, brokerPort);

		user.stopPubSubClient();
	}

	public int getRandomInteger(int minimum, int maximum) {
		return ((int) (Math.random() * (maximum - minimum))) + minimum;
	}

	private boolean handleAcquire(Thread sendOneMsg, PubSubClient user, String userName, int brokerPort,
			String brokerAdd,
			String selected) {
		System.out.println("The resource " + selected + " is available for you.");
		try {
			int wait = getRandomInteger(1000, 5000);
			Thread.sleep(wait);
			System.out.println("The resource " + selected + " was used for " + wait / 1000 + " seconds.");
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		System.out.println("User Release resource!");
		sendOneMsg = new ThreadWrapper(user, userName + ":release:" + selected, brokerAdd, brokerPort);

		sendOneMsg.start();

		try {
			sendOneMsg.join();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			int wait = getRandomInteger(1000, 5000);
			System.out.println("Wait " + wait / 1000 + " seconds.");
			Thread.sleep(wait);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		return true;
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
		}
	}

}
