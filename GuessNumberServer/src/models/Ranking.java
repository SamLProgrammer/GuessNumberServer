package models;

public class Ranking {
	
	String player;
	int tryNumber = 5;

	public Ranking(String player, int tryNumber) { // receives a player and the attempt at what he guessed the guess number
		this.player = player;
		this.tryNumber = tryNumber;
	}

	public String getPlayer() {
		return player;
	}

	public int getTryNumber() {
		return tryNumber;
	}
}
