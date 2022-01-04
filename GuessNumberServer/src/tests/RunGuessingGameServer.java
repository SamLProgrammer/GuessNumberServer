package tests;

import java.io.IOException;

import network.GuessServer;

public class RunGuessingGameServer {

	public static void main(String[] args) {// main class runs the Server
		try {
			new GuessServer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
