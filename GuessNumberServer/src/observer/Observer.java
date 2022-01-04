package observer;

import java.net.Socket;

import network.Connection;
import network.GuessServer;

public class Observer { // This class comunicates Connection (ClientHandler) with the server

	private GuessServer guessServer;

	public Observer(GuessServer guessServer) {
		this.guessServer = guessServer;
	}

	public int tryGuessing(int number) { // sends a try to guess server who owns the game
		return guessServer.tryGuessing(number);
	}

	public void removeConnection(Connection connection) { // ask to server to remove the given connection
		guessServer.removeConnection(connection);
	}

	public void initMinute() { // inits a countdown if the server has one player connected ( after 31) seconds a round will starts
		if (guessServer.getClientsQueue().size() == 1) {
			guessServer.initialMinute(31);
		}
	}

	public String getToGuessNumber() { // ask to server for to guess number
		return String.valueOf(guessServer.getToGuessNumber());
	}

	public boolean serverIsOnRound() { // asks if server is on a current round
		return guessServer.isOnRound();
	}
	
	public String currentRoundPlayersString() {
		return guessServer.currentRoundPlayersString();
	}

	public void oneMoreFinishedRound() { // notify the server that one player is done with his round
		guessServer.incrementPlayersDoneOnRoundCounter();
	}

	public void shiftToEndOfQueue(Connection connection) { // shifts the given connection to the ond of clients queue
		guessServer.shiftToEndOfQueue(connection);
	}

	public void updateRankList(String clientName, int triesCounter) { // add a player to the rank list (a player on round)
		guessServer.updateRankList(clientName, triesCounter);
	}
	
	public String onRoundPlayersName() {
		return guessServer.onRoundPlayerNames();
	}

	public void clientJoined(String clientName, Socket socket) {
		guessServer.clientJoined(clientName, socket);
	}

}
