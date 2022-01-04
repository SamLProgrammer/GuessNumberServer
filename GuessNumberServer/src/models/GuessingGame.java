package models;

public class GuessingGame {

	private int toGuessNumber;

	public GuessingGame() {
		toGuessNumber = (int) (Math.random() * 13); // game generates a number between 0 and 12 and save it into "toGuessNumber"
	}

	/*
	 * tryGuessing will return 0 if the input number is equals to  "toGuessNumber", 1 if "toGuessNumber" is bigger
	 * and -1 if "toGuessNumber is smaller
	 */
	public int tryGuessing(int number) { // compares the reveived message with "toGuessNumber" returns 0 if equals, -1 if toguessNumber is smaller, 1 if bigger
		int result = 0;
		if(number > toGuessNumber) {
			result = -1;
		} else if(number < toGuessNumber) {
			result = 1;
		}
		return result;
	}
	
	public int getToGuessNumber() {
		return toGuessNumber;
	}
}