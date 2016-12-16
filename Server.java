import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
	
	private static int uniqueId;	// an ID for each connection
	
	private ArrayList<ClientThread> al;	// an ArrayList to keep the list of the Client
	
	private SimpleDateFormat date;	//displaying time
	
	private int port_no;	// the port number used for listening
	
	private boolean continue_server;	// for stopping the server
	

	/*
	 *  server constructor that receive the port number to listen, as parameter on console
	*/
	public Server(int port_no) {
		
		this.port_no = port_no;
		
		date = new SimpleDateFormat("HH:mm:ss");	// to display time in the format hh:mm:ss
		
		al = new ArrayList<ClientThread>();	// ArrayList for the Clients
	}
	
	public void start() {
	
		continue_server = true;
		
		/* create socket server and wait for connection requests */
		try 
		{

			ServerSocket serverSocket = new ServerSocket(port_no);	// the socket used by the server
			
			while(continue_server) 	// infinite loop to wait for connections
			{
				
				display("Server waiting for Clients on port_no " + port_no + ".");	// display message
				
				Socket socket = serverSocket.accept();  	// accepts a connection
				
				if(!continue_server)	// if server stops or shuts down
					break;
					
				ClientThread t = new ClientThread(socket);  // make a thread of the connection
				al.add(t);									// save it in the ArrayList
				t.start();
			}
			
			// if server stops or shuts down
			try {
				
				serverSocket.close();	//close server
				
				//close all clients
				for(int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
					tc.sInput.close();
					tc.sOutput.close();
					tc.socket.close();
					}
					catch(IOException ioE) {
						// do nothing
					}
				}
			}
			catch(Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}
		
		// Random errors
		catch (IOException e) {
            String msg = date.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}		
    
	protected void stop() {
	
		continue_server = false;
		// connect to myself as Client to exit statement 
		try {
			new Socket("localhost", port_no);
		}
		catch(Exception e) {
			// do nothing
		}
	}
	
	private void display(String msg) {
	
		String time = date.format(new Date()) + " " + msg;
		System.out.println(time);
	}
	
	/*
	 *  to broadcast a message to all Clients
	 */
	private synchronized void broadcast(String message) {
		
		// add time and new line to the message
		String time = date.format(new Date());
		String final_message = time + " " + message + "\n";
		System.out.print(final_message);
		
		// we loop in reverse order to remove a client that has disconnected
		for(int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);
			// try to write to the Client 
			if(!ct.writeMsg(final_message)) {
				al.remove(i);	// it fails then remove it from the list
				display("Disconnected Client " + ct.username + " removed from list.");
			}
		}
	}

	// for a client who logs off using the LOGOUT message
	synchronized void remove(int id) {
		// scan the array list until we find the corresponding Id
		for(int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			// matched
			if(ct.id == id) {
				al.remove(i);
				return;
			}
		}
	}
	
	public static void main(String[] args) {
		
		int port_Number = 1500;	// start server on port_no 1500 unless a port_Number is specified 
		switch(args.length) {
			case 1:
				try {
					port_Number = Integer.parseInt(args[0]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Server [port_Number]");
					return;
				}
			case 0:
				break;
			default:
				System.out.println("Usage is: > java Server [port_Number]");
				return;
				
		}
		// create a server object and start it
		Server server = new Server(port_Number);
		server.start();
	}

	/** One instance of this thread will run for each client */
	class ClientThread extends Thread {
		
		// the socket where to listen/talk
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		
		int id;
		String username;
		ChatMessage msg;
		String date1;

		// Constructor
		ClientThread(Socket socket) {
			
			id = ++uniqueId;
			this.socket = socket;
			/* Creating both Data Stream */
			System.out.println("Thread trying to create Object Input/Output Streams");
			try
			{
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
				// read the username
				username = (String) sInput.readObject();
				display(username + " just connected.");
			}
			catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			// Error wrt string object
			catch (ClassNotFoundException e) {
			}
            date1 = new Date().toString() + "\n";
		}

		public void run() {
			// to loop until LOGOUT
			boolean continue_server = true;
			while(continue_server) {
				// read a String (which is an object)
				try {
					msg = (ChatMessage) sInput.readObject();
				}
				catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					break;				
				}
				catch(ClassNotFoundException e2) {
					break;
				}
				// the message part of the ChatMessage
				String message = msg.getMessage();

				switch(msg.getType()) {

				case ChatMessage.MESSAGE:
					broadcast(username + ": " + message);
					break;
				case ChatMessage.LOGOUT:
					display(username + " disconnected with a LOGOUT message.");
					continue_server = false;
					break;
				case ChatMessage.WHOISIN:
					writeMsg("List of the users connected at " + date.format(new Date()) + "\n");
					// scan all the users connected
					for(int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						writeMsg((i+1) + ") " + ct.username + " since " + ct.date1);
					}
					break;
				}
			}
			// remove myself from the arrayList containing the list of the
			// connected Clients
			remove(id);
			close();
		}
		
		// try to close everything
		private void close() {
			// try to close the connection
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(String msg) {
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch(IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}
	}
}
