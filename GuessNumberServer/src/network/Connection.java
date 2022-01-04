package network;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.swing.Timer;
import observer.Observer;
import persistence.FileManager;

public class Connection {

	private Observer observer;
	private String clientName;
	private OutputStream outputStream;
	private InputStream inputStream;
	private boolean connected;
	private boolean onRound;
	private boolean onWait;
	private boolean removed;
	private int triesCounter;
	private static final int TRIES_LIMIT_AMOUNT = 4;
	private FileManager fileManager;
	private Socket socket;
	private Timer onRoundTimer;
	private int keepAliveCounter;

	public Connection(Observer observer, Socket socket, FileManager fileManager) {
		initComponents(observer, socket, fileManager);
	}

	private void initOnRoundTimer() {// 5 min countdown and keepalive timer
		onRoundTimer = new Timer(1000, new ActionListener() {
			int onRoundTimeCounter = 0;

			@Override
			public void actionPerformed(ActionEvent e) {
				onRoundTimeCounter++;
				keepAliveCounter++;
				if (onRoundTimeCounter == 300) { // removes client at 5 mins
					connected = false;
					sendString(Responses.TIME_OUT.toString());
					sendString(Responses.CLIENT_REMOVED.toString());
					onRoundTimer.stop();
					observer.oneMoreFinishedRound();
					remove();
				} else if (keepAliveCounter == 30) {// send keep alive warning to client for inactivity
					sendString(Responses.KEEP_ALIVE.toString());
					sendString("\nSubmit your try [" + triesCounter + "]:");
					keepAliveCounter = 0;
				}
			}
		});
		onRoundTimer.start();
	}

	private void initComponents(Observer observer, Socket socket, FileManager fileManager) { // inits output, inputstream, filemanager and socket for this connection
		connected = true;
		onWait = true;
		this.fileManager = fileManager;
		this.socket = socket;
		this.observer = observer;
		try {
			outputStream = socket.getOutputStream();
			inputStream = socket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		initConnectionThread();// init communication thread
	}

	private void initConnectionThread() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (connected) {
					try {
						String trace = receiveString(); // on hold untill server sends any data
						if (!trace.equals("")) {
							switch (Requests.valueOf(trace)) {
							case CLIENT_SENDS_TRY:
								triesCounter++;
								String clientResponse = receiveString();
								String log = LocalDate.now().toString() + " at: " + LocalTime.now() + " Player: " + clientName + " at: [" + socket.getInetAddress() + "] With name: " + clientName + " Sent try [" + triesCounter + "]: " + clientResponse;
								if (!clientResponse.equals("")) {
									if (clientResponse.equalsIgnoreCase("e")) {
										removed = true;
										onRoundTimer.stop();
										connected = false;
										fileManager.writeOnServerEventsFile(LocalDate.now().toString() + " Player: " + clientName + " at: " + LocalTime.now() + " With SocketAdress: " + socket.getRemoteSocketAddress() + " Got removed from server");
										fileManager.writeOnGameActivitiesFile(LocalDate.now().toString() + " Player: " + clientName + " at: " + LocalTime.now() + " With SocketAdress: " + socket.getRemoteSocketAddress() + " Left the game by pressing \"e\"");
										fileManager.writeOnCurrentRoundLog(LocalDate.now().toString() + " Player: " + clientName + " at: " + LocalTime.now() + " With SocketAdress: " + socket.getRemoteSocketAddress() + " Left the game by pressing \"e\"");
										sendString(Responses.CLIENT_REMOVED.toString());
										observer.removeConnection(myInstance());
									} else {
										try {
											switch (observer.tryGuessing(Integer.valueOf(clientResponse))) {
											case -1:// if Method "guessingGame.tryGuessing" returns -1 then...
												if (triesCounter < TRIES_LIMIT_AMOUNT) {
													fileManager.writeOnGameActivitiesFile(log + " (Incorrect)");
													fileManager.writeOnCurrentRoundLog(log + " (Incorrect)");
													sendString(Responses.TO_GUESS_NUMBER_IS_SMALLER.toString());
													notifyToClientToTry("");
												} else {
													fileManager.writeOnGameActivitiesFile(log + " Player failed on guessing the number");
													fileManager.writeOnCurrentRoundLog(log + " Player failed on guessing the number");
													onRoundTimer.stop();
													sendString(Responses.CLIENT_FAILED.toString());
													sendString("\nThe guess number is: " + observer.getToGuessNumber());
													observer.oneMoreFinishedRound();
													if (observer.serverIsOnRound()) {
														sendString(Responses.ON_WAIT_FOR_ROUND_PARTNERS_TO_FINISH.toString());
													}
												}
												break;
											case 1:// if Method "guessingGame.tryGuessing" returns 1 then...
												if (triesCounter < TRIES_LIMIT_AMOUNT) {
													fileManager.writeOnGameActivitiesFile(log + " (Incorrect)");
													fileManager.writeOnCurrentRoundLog(log + " (Incorrect)");
													sendString(Responses.TO_GUESS_NUMBER_IS_BIGGER.toString());
													notifyToClientToTry("");
												} else {
													fileManager.writeOnGameActivitiesFile(log + " Player failed on guessing the number");
													fileManager.writeOnCurrentRoundLog(log + " Player failed on guessing the number");
													onRoundTimer.stop();
													sendString(Responses.CLIENT_FAILED.toString());
													sendString("\nThe guess number is: " + observer.getToGuessNumber());
													observer.oneMoreFinishedRound();
													if (observer.serverIsOnRound()) {
														sendString(Responses.ON_WAIT_FOR_ROUND_PARTNERS_TO_FINISH.toString());
													}
												}
												break;
											case 0:// if Method "guessingGame.tryGuessing" returns 0 then...
												fileManager.writeOnGameActivitiesFile(log + " Player guessed the number");
												fileManager.writeOnCurrentRoundLog(log + " Player guessed the number");
												onRoundTimer.stop();
												sendString(Responses.CONGRATULATION.toString());
												observer.updateRankList(clientName, triesCounter);
												observer.oneMoreFinishedRound();
												if (observer.serverIsOnRound()) {
													sendString(Responses.ON_WAIT_FOR_ROUND_PARTNERS_TO_FINISH.toString());
												}
												break;
											}
										} catch (NumberFormatException e) {
											onRoundTimer.stop();
											connected = false;
											observer.removeConnection(myInstance());
										}
									}
									break;
								}
							case CLIENT_SENDS_NAME:
								clientName = receiveString();
								observer.clientJoined(clientName, socket);
								wellcomeMessageToClient();
								observer.initMinute();
								break;
							case CLIENT_ASKS_TO_EXIT:
								onRoundTimer.stop();
								remove();
								break;
							case CLIENT_CHOSE_FINAL_ROUND_OPTION:
								String option = receiveString();
								if (option.equalsIgnoreCase("p")) {
									fileManager.writeOnGameActivitiesFile(LocalDate.now().toString() + " at: " + LocalTime.now() + " Client at: [" + socket.getInetAddress() + "] With name: " + clientName + " decided to play again by pressing \"p\"");
									fileManager.writeOnCurrentRoundLog(LocalDate.now().toString() + " at: " + LocalTime.now() + " Client at: [" + socket.getInetAddress() + "] With name: " + clientName + " decided to play again by pressing \"p\"");
									onWait = true;
									triesCounter = 0;
									shiftToEndOfQueue();
								} else {
									fileManager.writeOnGameActivitiesFile(LocalDate.now().toString() + " at: " + LocalTime.now() + " Client at: [" + socket.getInetAddress() + "] With name: " + clientName + " decided to leave server by pressing \"q\"");
									fileManager.writeOnCurrentRoundLog(LocalDate.now().toString() + " at: " + LocalTime.now() + " Client at: [" + socket.getInetAddress() + "] With name: " + clientName + " decided to leave server by pressing \"q\"");
									remove();
								}
								break;
							}
						}
					} catch (IOException e) {
						if (onRoundTimer != null) {
							onRoundTimer.stop();
						}
						if (connected && !removed) {
							fileManager.writeOnServerEventsFile(LocalDate.now().toString() + " at: " + LocalTime.now() + " Player: " + clientName + " With SocketAdress: " + socket.getRemoteSocketAddress() + " Got removed by brute force");
						}
						connected = false;
						observer.removeConnection(myInstance());
					}
				}
			}
		}).start();
	}

	private void shiftToEndOfQueue() { // shifts this connection to end of queue
		sendString(Responses.CLIENT_MOVED_TO_END_OF_QUEUE.toString());
		observer.shiftToEndOfQueue(myInstance());
	}

	private void remove() { // ask to server to remove this instance of connection, and register the event on server log file
		removed = true;
		fileManager.writeOnServerEventsFile(LocalDate.now().toString() + " at: " + LocalTime.now() + " Player: " + clientName + " With SocketAdress: " + socket.getRemoteSocketAddress() + " Got removed from server");
		sendString(Responses.CLIENT_REMOVED.toString());
		observer.removeConnection(myInstance());
	}

	public void notifyJoined() { // notifies the client, he successfully joined to server
		sendString(Responses.CLIENT_JOINED_TO_SERVER.toString());
	}

	public void updateRemainingInitialTime(int second) {// updates the countdown state to client
		sendString(Responses.SEND_INITIAL_WAITING_TIME_TO_CLIENT.toString());
		sendString("" + second);
	}

	public void wellcomeMessageToClient() {// send instructions and say wallecome message to client
		sendString(Responses.SEND_WELLCOME_TO_GAME_TO_CLIENT.toString());
		sendString("Instructions:\n1)Wait on the lobby for a round which picks you up into the game\n2)When your round starts try to guess the number or press \"e\" to leave the game\n3)A Round can lasts untill 5 min\n!Good Luck!");
		if(observer.serverIsOnRound()) {
			sendString(Responses.CURRENT_ROUND_RANK.toString());
			sendString("Current Round Players: \n" + observer.currentRoundPlayersString());
		}
	}

	public void showRoundNamesToLobby(String firstThreeNames) {// shows the client the next players name for next round
		sendString(Responses.NEXT_ROUND_NAMES_TO_CLIENT.toString());
		sendString(firstThreeNames);
	}
	
	public void showCurrentRoundRank(String rankString) {
		sendString(Responses.CURRENT_ROUND_RANK.toString());
		sendString(rankString);
	}

	public void showRoundFinishedOptions() { // show final options menu to client, and sends him the rank for the current round
		onRound = false;
		sendString(Responses.ROUND_FINAL_OPTIONS_ASKED_TO_CLIENT.toString());		
	}

	private void sendString(String string) { // function to send String data to client
		try {
			outputStream.write(string.getBytes().length);
			outputStream.write((string).getBytes());
			outputStream.flush();
		} catch (IOException e) {
			onRoundTimer.stop();
			connected = false;
			if(!removed) {
			fileManager.writeOnServerEventsFile(LocalDate.now().toString() + " at: " + LocalTime.now() + " Player: " + clientName + " With SocketAdress: " + socket.getRemoteSocketAddress() + " Got isconnected by brute force");
			}
			observer.removeConnection(myInstance());
		}
	}

	private String receiveString() throws IOException { // this function receives string located in the inputStream which server has
		// sent to client (taken from lab)
		String string = "";
		int length = inputStream.read();
		if (length >= 0) {
			keepAliveCounter = 0; // restarts keep alive countdown
			byte[] buffer = new byte[length];
			inputStream.read(buffer);
			string = new String(buffer).trim();
		}
		return string;
	}

	public void notifyToClientToTry(String string) { // notify the client to do sends a try
		onRound = true;
		onWait = false;
		sendString(Responses.ASKS_TO_CLIENT_TO_TRY.toString());
		if (triesCounter > 0) {
			sendString("\nSubmit your try [" + (triesCounter + 1) + "]:");
		} else {
			initOnRoundTimer(); // if the try is the first one then inits 5 min round timer
			sendString(string + "\n==>> Wellcome " + clientName + " To Guess The Number <<==\n" + "==> You have 4 Opportunities to guess a number between 0 and 12 <==\n" + "==> good luck! <==\n" + "\nSubmit your try [" + (triesCounter + 1) + "]:");
		}
	}
	
	public Connection myInstance() { // returns own instance for this class
		return this;
	}

	public boolean isOnRound() {
		return onRound;
	}

	public boolean isOnWait() {
		return onWait;
	}

	public boolean isConnected() {
		return connected;
	}

	public String getName() {
		return clientName;
	}
}
