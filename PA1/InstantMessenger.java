/**
  * CS 421 Programming Assignment 1
  * Bilkent University
  * FALL 2017
  * 
  * @authors  Salih CAN & Ömer Mesud TOKER
  * 
  **/

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.ArrayList;


public class InstantMessenger {
	
	public static void GET_Request(String sourceURL, ArrayList<String> userName, ArrayList<String> userIP, ArrayList<String> userPort) throws Exception {
		
		userName.clear();
		userIP.clear();
		userPort.clear();
		
		Socket socket = new Socket(sourceURL, 80);
		DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		// GET request
		String packet = "GET " + "/userlist.txt" + " HTTP/1.1\r\n" + 
						"Host: " + sourceURL + "\r\n" + 
						"Content-Type: text/html\r\n" + 
						"Connection: close\r\n\r\n";
		
		
		outToServer.writeBytes(packet);
		
		String sentence = inFromServer.readLine();
		//System.out.println(sentence);
		String statusCode = sentence.substring(9, 12);
		
		if (!statusCode.equals("200")) {
			System.out.println("Status code error: Bad Request");
			return;
		}
		
		sentence = inFromServer.readLine();
		sentence = inFromServer.readLine();
		sentence = inFromServer.readLine();
		sentence = inFromServer.readLine();
		
		while (sentence != null) {
			
			if (!sentence.equals("")) {
				
				int a = sentence.indexOf("@");
				int b = sentence.indexOf(":");
				
				String name = sentence.substring(0,a);
				String IP = sentence.substring(a + 1, b);
				String port = sentence.substring(b + 1);
				
				userName.add(name);
				userIP.add(IP);
				userPort.add(port);
			}
			
			sentence = inFromServer.readLine();
		}
	}
	
	public static void sendMessage(String message, String destName, String destIP, int destPort) throws Exception {
		
		message = "" + destName + "~" + message;
		
		DatagramSocket clientSocket = new DatagramSocket();
		
		InetAddress IPAddress = InetAddress.getByName(destIP);
		byte[] sendData = new byte[256];
		sendData = message.getBytes();
		
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, destPort);
		clientSocket.send(sendPacket);
	}

    public static void main(String[] argv) throws Exception {
		
		String username = argv[0];
		String sourceURL = argv[1];
		String mode = argv[2];
		
		ArrayList<String> userName = new ArrayList<String>();
		ArrayList<String> userIP = new ArrayList<String>();
		ArrayList<String> userPort = new ArrayList<String>();
		
		
		if ("listen".equals(mode)) {
			
			Socket socket = new Socket(sourceURL, 80);
			DatagramSocket peerSocket = new DatagramSocket();
			
			int port = peerSocket.getLocalPort();
			
			BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
			
			// POST request
			String data = "REGISTER " + username + "@" + InetAddress.getLocalHost().getHostAddress() + ":" + port + "\r\n";
			
			String packet = "POST " + "/userlist.txt" + " HTTP/1.1\r\n" + 
							"Host: " + sourceURL + "\r\n" + 
							"Content-Type: text/html\r\n" + 
							"Content-Length: " + data.length() + "\r\n" +
							"Connection: close\r\n\r\n" + data + "\r\n";

			outToServer.writeBytes(packet);

			String sentence = read.readLine();
			String statusCode = sentence.substring(9, 12);
			
			if (!statusCode.equals("200")) {
				System.out.println("User already exists");
				return;
			}
			
			while (true) {
				DatagramPacket dp = new DatagramPacket(new byte[1024], 1024);
				peerSocket.receive(dp);
				String str = new String(dp.getData());
				int a = str.indexOf("~");
				String sendername = str.substring(0,a);
				str = str.substring(a + 1);
				System.out.println(sendername + ": " + str.trim());
			}
		}
		
		else if ("send".equals(mode)) {
			
			String[] users = new String[0];
			String command;
			Scanner scan = new Scanner(System.in);
			command = scan.nextLine();
			
			while (!command.equals("exit")) {
				
				String message = "";
				String rest = "";
				
				if (command.indexOf(" ") > -1) {
					int a = command.indexOf(" ");
					rest = command.substring(a+1);
					command = command.substring(0,a);
					
					if (rest.indexOf("\"") > -1) {
						message = rest.substring(rest.indexOf("\"") + 1);
						message = message.substring(0,message.indexOf("\""));
						rest = rest.substring(0,rest.indexOf("\""));
						
						if (rest.indexOf(" ") > -1) {
							rest = rest.substring(0,rest.indexOf(" "));
						}
						
						if (rest.indexOf("[") > -1) {
							rest = rest.substring(rest.indexOf("[") + 1);
							
							if (rest.indexOf("]") > -1) {
								rest = rest.substring(0,rest.indexOf("]"));
							}
							
							users = rest.split(",");
						}
					}
				}
				
				if (command.equals("list")) {
					
					GET_Request(sourceURL, userName, userIP, userPort);
					
					System.out.println("The online users are:");
					for (int i = 0; i < userName.size(); i++) {
						System.out.println(userName.get(i));
					}
					System.out.println();
				}
				
				else if (command.equals("unicast")) {
					
					GET_Request(sourceURL, userName, userIP, userPort);
					
					int index = userName.indexOf(rest);
					
					if (index > -1) {
						int port = Integer.parseInt(userPort.get(index));
						sendMessage(message, username, userIP.get(index), port);
						
						System.out.println("message sent to " + userName.get(index));
					}
					else {
						System.out.println("user " + rest + " is not found");
					}
					System.out.println();
				}
				
				else if (command.equals("broadcast")) {
					
					GET_Request(sourceURL, userName, userIP, userPort);
					
					for (int i = 0; i < userName.size(); i++) {
						
						int port = Integer.parseInt(userPort.get(i));
						sendMessage(message, username, userIP.get(i), port);
						
					}
					
					System.out.println();
				}
				
				else if (command.equals("multicast")) {
					
					GET_Request(sourceURL, userName, userIP, userPort);
					
					for (int i = 0; i < users.length; i++) {
						
						int index = userName.indexOf(users[i]);
						
						if (index > -1) {
							int port = Integer.parseInt(userPort.get(index));
							sendMessage(message, username, userIP.get(index), port);
							
							System.out.println("message sent to " + users[i]);
						}
						else {
							System.out.println("user " + users[i] + " is not found");
						}
					}
					System.out.println();
				}
				
				else {
					System.out.println("not a valid command type");
				}
				
				command = scan.nextLine();
			}
		}
		
		else {
			System.out.println("not a valid input type");
		}
    }
}