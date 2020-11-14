/* Ryan Hagan CS 420G 4/29/18 Final Project
 * AwesomeChat: TCP Chat Server */ 

import java.io.*; 
import java.net.*;
import java.util.*; 
import java.text.*; 

public class server 
{
	private static ServerSocket serverSocket = null;
	private static Socket clientSocket = null;
	// This chat server can accept up to maxClientsCount clients' connections.
	private static final int maxNumberOfClients = 10;
	// an array of client Threads - creating space for the maxNumberOfClients to each have their own thread 
	private static final clientThread[] threads = new clientThread[maxNumberOfClients];

	public static void main(String args[]) 
	{
		// SET UP PORT ----------------------------------------------------------
		System.out.println("\n***************AWESOMECHAT SERVER***************\n************************************************\n");
		System.out.println("*** Which port would you like to use? ***"); 
		Scanner scan = new Scanner(System.in);
		int portNumber = scan.nextInt(); 

		// create a server socket - on the port the user specified - aka - what port do you want to listen on? 
		try 
		{
			serverSocket = new ServerSocket(portNumber);
			System.out.println("*** Listening on port: " + portNumber + " ***");
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		// START LISTENING ----------------------------------------------------------
		// accept a client request, assign that door to this particular client 
		while (true) 
		{
			try 
			{	
				// client comes knocking at the port # we're listening on, open a door for them 
				clientSocket = serverSocket.accept();

				int i = 0;

				// go through all the clients 
				for (i = 0; i < maxNumberOfClients; i++) 
				{
					// if we find a place in the array that inFromClient unassigned - if we have room for another client 
					if (threads[i] == null) 
					{
						// START A NEW THREAD FOR THIS CLIENT ----------------------------------------------------------
						// this starts the run() function 
						(threads[i] = new clientThread(clientSocket, threads)).start();
						break;
					}
				}

				// if we don't have room for another client 
				if (i == maxNumberOfClients) 
				{
					PrintStream outputStream = new PrintStream(clientSocket.getOutputStream());
					outputStream.println("*** The max number of clients have connected. Try again later. ***");
				}
			} 
			catch (IOException e) 
			{
				e.printStackTrace(); 
			}
		}
	} // end of main 
} // end of server 

class clientThread extends Thread 
{
	private String clientName = null;
	private BufferedReader inFromClient = null;
	private PrintStream outToClient = null;
	private Socket clientSocket = null;
	private final clientThread[] threads;
	private int maxClientsCount;

	public clientThread(Socket clientSocket, clientThread[] threads) 
	{
		this.clientSocket = clientSocket;
		this.threads = threads;
		maxClientsCount = threads.length;
	}

	// this function begins if .start() is called above - creates a new thread for a new client 
	public void run() 
	{
		int maxClientsCount = this.maxClientsCount;
		clientThread[] threads = this.threads;
      boolean allowedToEnter = false; 
		try 
		{
			// set up input & output streams 
			inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			outToClient = new PrintStream(clientSocket.getOutputStream());

			// AUTHENTICATE CLIENT -----------------------------------------------------
			String userSelectedName; 
			String userSelectedPassword; 
			
			// username
			outToClient.println("*** Enter your username ***");
			userSelectedName = inFromClient.readLine().trim();

			if (userSelectedName.isEmpty())
			{
				outToClient.println("You must enter a username");
			}

			outToClient.println("*** Welcome " + userSelectedName + "! You have successfully connected!\nEnter your message below and press Enter to send. To leave, type \"/quit\" and press Enter. ***");
			
			// set this thread's (this client's) name to the name the user selected
			synchronized (this) 
			{
				for (int i = 0; i < maxClientsCount; i++) 
				{
					if (threads[i] != null && threads[i] == this) 
					{
						clientName = userSelectedName; 
						break;
					}
				}
				
				// alert all other users a new user has arrived 
				for (int i = 0; i < maxClientsCount; i++) 
				{
					if (threads[i] != null && threads[i] != this) 
					{
						threads[i].outToClient.println("*** A new user " + userSelectedName + " entered! ***");
					}
				}
				
				// FACILITATE CHAT ----------------------------------------------------------
				try 
				{
					// as long as the client does not quit 
					while (true) 
					{
						// capture client's message to send 
						String clientMessage = inFromClient.readLine();
						
						// if the client types /quit, they leave the chat (break out of the loop)
						if (clientMessage.startsWith("/quit")) 
						{
							System.out.println(userSelectedName + " quit"); 
							break;
						}
						
						// prepare for time stamping 
						DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
						Date date = new Date();
						String formattedDate = dateFormat.format(date);

						// send the client's message to everyone 
						synchronized (this) 
						{
							for (int i = 0; i < maxClientsCount; i++) 
							{
								if (threads[i] != null && threads[i].clientName != null) 
								{
									threads[i].outToClient.println(formattedDate + ":<" + userSelectedName + ">\t" + clientMessage);
								}
							}
						}
					}
				} 
				
				// IF CLIENT LEAVES ----------------------------------------------------------
				// display notification on the server
				catch (NullPointerException e) 
				{
					System.out.println(userSelectedName + " quit");
				}

				// tell everyone who left 
				synchronized (this) 
				{
					for (int i = 0; i < maxClientsCount; i++) 
					{
						if (threads[i] != null && threads[i] != this && threads[i].clientName != null) 
						{
							threads[i].outToClient.println("*** " + userSelectedName + " has left! ***");
						}
					}
				}
				
				// say bye to the exiting client 
				outToClient.println("*** Goodbye " + userSelectedName + " ! Press Enter. ***");

				// make room for a new client to join 
				synchronized (this) 
				{
					for (int i = 0; i < maxClientsCount; i++) 
					{
						if (threads[i] == this) 
						{
							threads[i] = null;
						}
					}
				}
				inFromClient.close();
				outToClient.close();
				clientSocket.close();
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}
