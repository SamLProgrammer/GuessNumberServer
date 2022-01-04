package models;

import network.Connection;

public class Node { // a position in memory which contains a connection, and a reference to next node which will contains another connection
	
	private Connection connection;
	private Node nextNode;
	
	public Node(Connection connection) {
		this.connection = connection;
	}

	public Connection getConnection() {
		return connection;
	}

	public Node getNextNode() {
		return nextNode;
	}

	public void setNextNode(Node nextNode) {
		this.nextNode = nextNode;
	}
}
