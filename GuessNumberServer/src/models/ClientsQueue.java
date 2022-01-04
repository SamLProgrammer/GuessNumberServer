package models;

import java.util.concurrent.CopyOnWriteArrayList;
import network.Connection;

public class ClientsQueue {

	private Node rootNode;
	private static final int CAPACITY = 6;

	public boolean push(Connection connection) { // adds a connection to the end of queue
		boolean added = false;
		if (size() < CAPACITY) {
			Node node = new Node(connection);
			if (rootNode == null) {
				rootNode = node;
			} else {
				Node auxNode = rootNode;
				while (auxNode.getNextNode() != null) {
					auxNode = auxNode.getNextNode();
				}
				auxNode.setNextNode(node);
			}
			added = true;
		}
		return added;
	}

	public void remove(Connection connection) { // removes a connection from this queue
		if (rootNode != null) {
			Node auxNode = rootNode;
			if (auxNode.getConnection() == connection) {
				rootNode = rootNode.getNextNode();
			} else {
				while (auxNode.getNextNode() != null && auxNode.getNextNode().getConnection() != connection) {
					auxNode = auxNode.getNextNode();
				}
				if (auxNode.getNextNode() != null) {
					auxNode.setNextNode(auxNode.getNextNode().getNextNode());
				}
			}
		}
	}

	public Node find(Connection connection) { // finds a connection in this queue
		Node auxNode = rootNode;
		while (auxNode != null && auxNode.getConnection() != connection) {
			auxNode = auxNode.getNextNode();
		}
		return auxNode;
	}

	public CopyOnWriteArrayList<Connection> returnFirstThreeConnections() { // returns a concurrent arraylist with the first three connections on queue or less 
		CopyOnWriteArrayList<Connection> firstThree = new CopyOnWriteArrayList<Connection>();
		int counter = 0;
		Node auxNode = rootNode;
		while (auxNode != null && counter < 3) {
			firstThree.add(auxNode.getConnection());
			counter++;
			auxNode = auxNode.getNextNode();
		}
		return firstThree;
	}

	public Connection get(int number) { // gets the connection on the given position (number) 
		Node auxNode = rootNode;
		Connection connection = null;
		int counter = 0;
		while (auxNode != null && counter < number) {
			auxNode = auxNode.getNextNode();
			counter++;
		}
		if (auxNode != null) {
			connection = auxNode.getConnection();
		}
		return connection;
	}

	public Node peek() { // returns the connection at the head of this queue
		return rootNode;
	}

	public Node poll() { // returns and remove the connection at the head of this queue
		Node auxNode = rootNode;
		rootNode = auxNode.getNextNode();
		return auxNode;
	}

	public int size() { // returns the queue size as integer
		int size = 0;
		Node auxNode = rootNode;
		while (auxNode != null) {
			size++;
			auxNode = auxNode.getNextNode();
		}
		return size;
	}
}
