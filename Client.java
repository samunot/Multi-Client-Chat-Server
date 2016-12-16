import java.net.*;
import java.io.*;
import java.util.*;


public class Client  {

	// for I/O
	private ObjectInputStream sInput;		// to read from the socket
	private ObjectOutputStream sOutput;		// to write on the socket
	private Socket socket;

	// the server, the port and the user_Name
	private String server, username;
	private int port;

	/*
	 *  Constructor called by console mode
	 *  server: the server address
	 *  port: the port number
	 *  username: the username
	 */
	Client(String server, int port, String username) {
		this.server = server;
		this.port = port;
		this.username = username;
	}

	public boolean start() {
		// try to connect to the server
		try {
			socket = new Socket(server, port);
		} 
		// if it failed,error is given
		catch(Exception ec) {
			display("Error connecting to server:" + ec);
			return false;
		}
		
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		display(msg);
	
		/* Creating both Data Stream */
		try
		{
			sInput  = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException eIO) {
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// creates the Thread to listen from the server 
		new ListenFromServer().start();
		// Send our username to the server as a String, all other messages will be ChatMessage objects
		try
		{
			sOutput.writeObject(username);
		}
		catch (IOException eIO) {
			display("Exception doing login : " + eIO);
			disconnect();
			return false;
		}
		// on successful connection ,inform the caller
		return true;
	}

	/*
	 * To send a message to the console
	 */
	private void display(String msg) {
		System.out.println(msg);  
	}
	
	/*
	 * To send a message to the server
	 */
	void sendMessage(ChatMessage msg) {
		try {
			sOutput.writeObject(msg);
		}
		catch(IOException e) {
			display("Exception writing to server: " + e);
		}
	}

	/*
	 * When something goes wrong
	 * Close the Input/Output streams and disconnect 
	 */
	private void disconnect() {
		try { 
			if(sInput != null) sInput.close();
		}
		catch(Exception e) {} // do nothing
		try {
			if(sOutput != null) sOutput.close();
		}
		catch(Exception e) {} // do nothing
        try{
			if(socket != null) socket.close();
		}
		catch(Exception e) {} // do nothing
	}
	/*
	 * To start the Client in console mode use one of the following command
	 * > java Client
	 * > java Client user_Name
	 * > java Client user_Name port_Number
	 * > java Client user_Name port_Number server_Address
	 */
	public static void main(String[] args) {
	
		// default values
		int port_Number = 1500;
		String server_Address = "localhost";
		String user_Name = "Anonymous";

		switch(args.length) {
			// > java Client user_Name port_Number serverAddr
			case 3:
				server_Address = args[2];
			// > java Client user_Name port_Number
			case 2:
				try {
					port_Number = Integer.parseInt(args[1]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Client [user_Name] [port_Number] [server_Address]");
					return;
				}
			// > java Client user_Name
			case 1: 
				user_Name = args[0];
			// > java Client
			case 0:
				break;
			// invalid number of arguments
			default:
				System.out.println("Usage is: > java Client [user_Name] [port_Number] {server_Address]");
			return;
		}
		
		// create the Client object
		Client client = new Client(server_Address, port_Number, user_Name);
		// test to start the connection
		if(!client.start())
			return;
		
		// wait for messages from user
		Scanner scan = new Scanner(System.in);
		
		// loop forever for message from the user
		while(true) {
			System.out.print("> ");
			// read message from user
			String msg = scan.nextLine();
			// logout if message is LOGOUT
			if(msg.equalsIgnoreCase("LOGOUT")) {
				client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
				// break to disconnect
				break;
			}
			// shows the list of connected clients
			else if(msg.equalsIgnoreCase("WHOISIN")) {
				client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));				
			}
			else {	// default to regular message
				client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
			}
		}
		// done
		client.disconnect();	
	}

	class ListenFromServer extends Thread {

		public void run() {
			while(true) {
				try {
					String msg = (String) sInput.readObject();
					System.out.println(msg);
						
				}
				catch(IOException e) {
					display("Server has close the connection: " + e);
					break;
				}
				// error on String object
				catch(ClassNotFoundException e2) {
				}
			}
		}
	}
}
