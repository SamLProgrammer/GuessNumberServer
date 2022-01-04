package network;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import models.ClientsQueue;
import models.GuessingGame;
import models.Ranking;
import observer.Observer;
import persistence.FileManager;

public class GuessServer {

	private GuessingGame guessingGame;
	private ServerSocket serverSocket;
	private ClientsQueue clientsQueue;
	private Observer observer;
	private boolean onRound;
	private boolean serverOn;
	private boolean initialMinute;
	private int playersOnRoundCounter;
	private int playersDoneOnRoundCounter;
	private int minuteCounter;
	private List<Ranking> rankList;
	private FileManager fileManager;

	public GuessServer() throws IOException {
		initComponents();
	}

	private void initComponents() {
		InputStreamReader in = new InputStreamReader(System.in); // create an inputstream over keyword input (System.in)
		BufferedReader buffer = new BufferedReader(in); // create buffer for faster reading
		String line = ""; // string variable to save line typed on terminal
		System.out.println("Please enter a port number where server is going to be running: \n");
		try {
			line = buffer.readLine(); // buffer reads the line on stream
			serverSocket = new ServerSocket(Integer.valueOf(line)); // inits components..
			observer = new Observer(this);
			System.out.println("Opened server at port: " + serverSocket.getLocalPort() + "\nClients are now allowed to try to connect...");
			clientsQueue = new ClientsQueue();
			serverOn = true;
			fileManager = new FileManager();
			initConnectionsThread();
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage()); // catch exception and show reason
		}
	}

	private void initConnectionsThread() {// joinning clients thread
		new Thread(new Runnable() {
			@Override
			public void run() {
				serverOn = true;
				while (serverOn) {
					try {
						Socket socket = serverSocket.accept(); // wait for new connections
						fileManager.writeOnServerEventsFile(LocalDate.now().toString() + " at: " + LocalTime.now() + " With SocketAdress: [" + socket.getRemoteSocketAddress() + "] tried to join the log");
						if (clientsQueue.push(new Connection(observer, socket, fileManager))) { // if there's a space on queue
							fileManager.writeOnServerEventsFile(LocalDate.now().toString() + " at: " + LocalTime.now() + " With SocketAdress: [" + socket.getRemoteSocketAddress() + "] Successfully joined To Server");
						} else { // if there's not
							new DataOutputStream(socket.getOutputStream()).writeUTF(Responses.FULL_LOBBY.toString());
							fileManager.writeOnServerEventsFile(LocalDate.now().toString() + " at: " + LocalTime.now() + " With SocketAdress: [" + socket.getRemoteSocketAddress() + "] Was rejected by Server due to full lobby queue");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	public void initialMinute(int secondsOnWait) { // starts countdown before the round starts
		initialMinute = true;
		minuteCounter = secondsOnWait;
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (initialMinute) {
					minuteCounter--;
					if (minuteCounter == 0) {
						initialMinute = false;
						initRound();
					} else {
						if (minuteCounter % 10 == 0) {// notifies clients every 10 seconds
							if (minuteCounter != 10) {
								updateRemainingTimeToClients(minuteCounter);
							} else {
								notifyFirstThreeNames("At The Moment, Next Players Get Ready To Play Into 10 Secs: ");
							}
						}
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	private void initRound() { // inits a round
		if (clientsQueue.size() > 0) {
			resetRoundState();
			for (int i = 0; i < 3; i++) {
				if (clientsQueue.get(i) != null && clientsQueue.get(i).isOnWait()) { // for every client which is on wait
					playersOnRoundCounter++;
				}
			}
			for (int i = 0; i < clientsQueue.size(); i++) {
				if (i < 3) {
					if (clientsQueue.get(i) != null && clientsQueue.get(i).isOnWait()) { // announce guessing team to on round players
						clientsQueue.get(i).notifyToClientToTry("==>You're On Round Now<==\nGuessing Team:" + toRoundPlayersNames());
					}
				} else {
					if (clientsQueue.get(i) != null && clientsQueue.get(i).isOnWait()) { // announce guessing team to on wait players
						clientsQueue.get(i).showRoundNamesToLobby("We Have " + playersOnRoundCounter + " Players on Current round: " + toRoundPlayersNames() + "\n==> Other players are still palying, please wait to get the ranking result <==");
					}
				}
			}
			fileManager.writeOnGameActivitiesFile(LocalDate.now().toString() + " at: " + LocalTime.now() + " Server started a round with guess number: " + guessingGame.getToGuessNumber() + " and these players: " + onRoundPlayerNames());
			fileManager.writeOnCurrentRoundLog(LocalDate.now().toString() + " at: " + LocalTime.now() + " Server started a round with guess number: " + guessingGame.getToGuessNumber() + " and these players: " + onRoundPlayerNames());
		}
	}

	private void resetRoundState() { // default set up this server
		rankList = Collections.synchronizedList(new CopyOnWriteArrayList<Ranking>());
		guessingGame = new GuessingGame(); // restart a game
		playersOnRoundCounter = 0;
		playersDoneOnRoundCounter = 0;
		onRound = true;
		System.out.println("New Round Started\nTo Guess Number: " + guessingGame.getToGuessNumber());
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH-mm-ss");
		fileManager.createNewRoundLogURL("Round at " + LocalDate.now().toString() + " - " + LocalTime.now().format(dtf));
	}

	private void notifyFirstThreeNames(String title) {// show on round player names to all players on wait
		for (int i = 0; i < clientsQueue.size(); i++) {
			if (clientsQueue.get(i) != null && !clientsQueue.get(i).isOnRound()) {
				clientsQueue.get(i).showRoundNamesToLobby(title + toRoundPlayersNames());
			}
		}
	}

	public String onRoundPlayerNames() { // gets on round players names
		String onRoundPlayersNames = "";
		for (int i = 0; i < clientsQueue.size(); i++) {
			if (clientsQueue.get(i).isOnRound()) {
				onRoundPlayersNames += clientsQueue.get(i).getName() + ", ";
			}
		}
		return onRoundPlayersNames;
	}
	
	public String currentRoundPlayersString() {
		String onRoundPlayersNames = "";
		for (int i = 0; i < clientsQueue.size(); i++) {
			if (clientsQueue.get(i).isOnRound()) {
				onRoundPlayersNames += clientsQueue.get(i).getName() + "\n";
			}
		}
		return onRoundPlayersNames;
	}

	private String toRoundPlayersNames() { // gets next round players names
		String toRoundPlayersNames = "";
		for (int i = 0; i < 3; i++) {
			if (clientsQueue.get(i) != null) {
				toRoundPlayersNames += "\n*" + clientsQueue.get(i).getName();
			}
		}
		return toRoundPlayersNames;
	}

	public void updateRemainingTimeToClients(int second) { // show to on lobby clients the countdown state to next round
		for (int i = 0; i < clientsQueue.size(); i++) {
			if (clientsQueue.get(i) != null) {
				clientsQueue.get(i).updateRemainingInitialTime(second);
			}
		}
	}

	public int tryGuessing(int number) { // sends a try to guessingGame
		return guessingGame.tryGuessing(number);
	}

	public void removeConnection(Connection connection) { // remove a connection from clients queue
		clientsQueue.remove(connection);
		boolean ableToRound = true;
		if (connection.isOnRound()) {
			incrementPlayersDoneOnRoundCounter();
			connection = null;
		} else {
			for (int i = 0; i < clientsQueue.size(); i++) {
				if (!clientsQueue.get(i).isOnWait()) {
					ableToRound = false;
				}
			}
			if (ableToRound) {
				initialMinute(15);
			}
		}
	}

	public void incrementPlayersDoneOnRoundCounter() { // Here starts new Round, increments "playersDoneOnRoundCounter" so server know
														// when to start a new round
		playersDoneOnRoundCounter++;
		if (playersDoneOnRoundCounter >= playersOnRoundCounter) {
			onRound = false;
			fileManager.writeOnGameActivitiesFile(LocalDate.now().toString() + " at: " + LocalTime.now() + " Round finished with these players: " + onRoundPlayerNames());
			fileManager.writeOnGameActivitiesFile(LocalDate.now().toString() + " at: " + LocalTime.now() + " Round finished with Rank: " + rankStringToLog());
			fileManager.writeOnCurrentRoundLog(LocalDate.now().toString() + " at: " + LocalTime.now() + " Round finished with these players: " + onRoundPlayerNames());
			fileManager.writeOnCurrentRoundLog(LocalDate.now().toString() + " at: " + LocalTime.now() + " Round finished with Rank: " + rankStringToLog());
			for (int i = 0; i < clientsQueue.size(); i++) {
				if (clientsQueue.get(i) != null) {
					if (clientsQueue.get(i).isOnRound()) {
						clientsQueue.get(i).showCurrentRoundRank(rankString());
						clientsQueue.get(i).showRoundFinishedOptions();
					} else {
						clientsQueue.get(i).showCurrentRoundRank(rankString());
					}
				}
			}
		}
	}
	public void shiftToEndOfQueue(Connection connection) {// shift a given connection to the ond of clients queue
		clientsQueue.remove(connection);
		clientsQueue.push(connection);
		boolean flag = false;
		for (int i = 0; i < clientsQueue.size(); i++) {
			if (!clientsQueue.get(i).isOnWait()) {
				flag = true;
			}
		}
		if (!flag && clientsQueue.size() > 0) {
			initialMinute(15);
		}
	}

	public void updateRankList(String clientName, int triesCounter) {// sorts the rank list given a new done on round client
		rankList.add(new Ranking(clientName, triesCounter));
		rankList.sort(new Comparator<Ranking>() {
			@Override
			public int compare(Ranking o1, Ranking o2) {
				if (o1.getTryNumber() > o2.getTryNumber()) {
					return 1;
				} else if (o1.getTryNumber() < o2.getTryNumber()) {
					return -1;
				} else {
					return 0;
				}
			}
		});
	}

	public String rankString() { // gets the rank as a string for current round
		String ranks = "Rank for Round: \n";
		int counter = 0;
		for (Ranking ranking : rankList) {
			counter++;
			ranks += counter + ") " + ranking.getPlayer() + " At Try: " + ranking.getTryNumber() + "\n";
		}
		return ranks;
	}

	public String rankStringToLog() {
		String ranks = "";
		int counter = 0;
		for (Ranking ranking : rankList) {
			counter++;
			ranks += counter + ") " + ranking.getPlayer() + " At Try: " + ranking.getTryNumber() + ", ";
		}
		return ranks;
	}

	public ClientsQueue getClientsQueue() {
		return clientsQueue;
	}

	public int getToGuessNumber() {
		return guessingGame.getToGuessNumber();
	}

	public boolean isOnRound() {
		return onRound;
	}

	public void clientJoined(String clientName, Socket socket) {
		System.out.println("new Client identified as: " + clientName + " At: " + socket.getInetAddress() + " Listenning on port: " + socket.getPort());
	}
}
