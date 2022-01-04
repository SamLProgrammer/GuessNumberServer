package persistence;

import java.io.FileOutputStream;
import java.io.IOException;
/*
 * This class is in charge of save Server and GamePlay events in a registry txt file
 */
public class FileManager {
	
	private final static String SERVER_EVENTS_LOGIN_PATH = "./files/ServerEventsLog.txt";
	private final static String GAME_EVENTS_LOGIN_PATH = "./files/GameEventsLog.txt";
	private final static String FILE_URL = "./files/";
	private String currentRoundLogName;
	
	public void writeOnServerEventsFile(String activity) {
		try {
			FileOutputStream fos = new FileOutputStream(SERVER_EVENTS_LOGIN_PATH, true);
			fos.write((activity + "\n").getBytes()); // writes string into the FileOutputStream specified route
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeOnGameActivitiesFile(String activity) {
		try {
			FileOutputStream fos = new FileOutputStream(GAME_EVENTS_LOGIN_PATH, true);
			fos.write((activity + "\n").getBytes()); // writes string into the FileOutputStream specified route
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void createNewRoundLogURL(String string) { // modifies current log url for every round
		currentRoundLogName =  FILE_URL + string + ".txt";
	}
	
	public void writeOnCurrentRoundLog(String string) {
		try {
			FileOutputStream fos = new FileOutputStream(currentRoundLogName, true);
			fos.write((string + "\n").getBytes()); // writes string into the FileOutputStream specified route
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
