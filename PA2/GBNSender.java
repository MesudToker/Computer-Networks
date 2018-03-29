/**
  * CS 421 Programming Assignment 2
  * Bilkent University
  * FALL 2017
  * 
  * @authors  Salih CAN & Ömer Mesud TOKER
  * 
  **/

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;


public class GBNSender {
	
	private static String receiverIP;
	private static int receiverPort;
	private static int N;
	private static int TO;
	private static int buffer_size;
	private static long file_size;
	private static int numberOfPackets;
	
	private static DatagramSocket clientSocket;
	private static Semaphore semaphore;
	private static Timer t;
	private static boolean endOfTransmission = false;
	private static byte[][] input;
	private static int snd_base;
	private static int seq_number;
	private static int ACKED;
	private static long begin, end, transmissionTime;
	
	
	// Thread for sending packets
	public class SendData extends Thread {
		
		public void sendMessage(int seqNumber) throws Exception {
			
			InetAddress IPAddress = InetAddress.getByName(receiverIP);
			byte[] packet = new byte[1026];
			
			packet[0] = (byte) ((seqNumber >> 8) & 0xFF);
			packet[1] = (byte) (seqNumber & 0xFF);
			
			for (int i = 2; i < 1026; i++)
				packet[i] = input[seqNumber-1][i-2];
			
			DatagramPacket sendPacket = new DatagramPacket(packet, 1026, IPAddress, receiverPort);
			clientSocket.send(sendPacket);
		}
		
		public void run() {
			
			try {
				while (!endOfTransmission) {
					
					if (seq_number < snd_base + N && seq_number <= numberOfPackets) {
						
						semaphore.acquire();
						
						if (seq_number == snd_base)
							startTimer();
						
						try {
							sendMessage(seq_number);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
						seq_number++;
						
						semaphore.release();
					}
					sleep(10);
				}
				
				if(t != null)
					t.cancel();
			
			}
			catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Thread for receiving ACKs
	public class ReceiveACK extends Thread {
		
		DatagramPacket dp;
		
		public int listenACK(byte[] data) {
			
			int ACKNumber = ((data[0] & 0xff) << 8) | (data[1] & 0xff);
			
			return ACKNumber;
		}
		
		public void run() {
			
			byte[] receivedData = new byte[2];
			dp = new DatagramPacket(receivedData, 2);
			
			try {
				
				while (!endOfTransmission) {
					
					clientSocket.receive(dp);
					
					ACKED = listenACK(receivedData);
					
					if (ACKED == numberOfPackets) {
						
						// transmission ends
						endOfTransmission = true;
						end = System.currentTimeMillis();
						transmissionTime = (end - begin)/1000;
						System.out.println();
						System.out.println("Transmission time: " + transmissionTime + " seconds");
						//errorCalculation();
					}
					else if (snd_base <= ACKED) {
						
						semaphore.acquire();
						
						snd_base = ACKED + 1;
						startTimer();
						
						semaphore.release();
					}
					sleep(10);
				}
				clientSocket.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public class Timeout extends TimerTask {
		
		public void run() {
			try {
				semaphore.acquire();
				seq_number = snd_base;
				semaphore.release();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void startTimer() {
		
		if (t != null)
			t.cancel();
		
		t = new Timer();
		t.schedule(new Timeout(), TO);
	}
	
	// error calculation method
	// to use this method, fill the paths correctly and uncomment line 123
	/*
	public void errorCalculation() {
		
		byte[][] in = new byte[numberOfPackets][buffer_size];
		byte[][] out = new byte[numberOfPackets][buffer_size];
		
		int count = 0;
		
		// read input file
		try (InputStream inStream = new FileInputStream("path of input file, input.bin");) {
			byte[] buffer = new byte[buffer_size];
			
			while (inStream.read(buffer) != -1) {
				for (int i = 0; i < buffer_size; i++)
					in[count][i] = buffer[i];
				count++;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		count = 0;
		
		// read output file
		try (InputStream outStream = new FileInputStream("path of output file, receive_out.bin");) {
			byte[] buffer = new byte[buffer_size];
			
			while (outStream.read(buffer) != -1) {
				for (int i = 0; i < buffer_size; i++)
					out[count][i] = buffer[i];
				count++;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		int error = 0;
		
		for (int i = 0; i < numberOfPackets; i++)
			for (int j = 0; j < buffer_size; j++)
				if (in[i][j] != out[i][j])
					error++;
		
		System.out.println("Error = " + error);
	}
	*/
	
	public GBNSender() {
		
		SendData sendData = new SendData();
		ReceiveACK receiveACK = new ReceiveACK();
		sendData.start();
		receiveACK.start();
	}
	
    public static void main(String[] argv) throws Exception {
		
		String path = argv[0];
		receiverIP = argv[1];
		String receiver_port = argv[2];
		String window_size = argv[3];
		String timeout = argv[4];
		
		receiverPort = Integer.parseInt(receiver_port);
		N = Integer.parseInt(window_size);
		TO = Integer.parseInt(timeout);
		
		semaphore = new Semaphore(1);
		clientSocket = new DatagramSocket();
		
		snd_base = 1;
		seq_number = 1;
		
		buffer_size = 1024;
		file_size = new File(path).length();
		numberOfPackets = (int)file_size/buffer_size;
		
		// input: double byte array whose rows are packet with size 1024
		input = new byte[numberOfPackets][buffer_size];
		
		int count = 0;
		
		// read binary file into double byte array
		InputStream inputStream = new FileInputStream(path);
		
		try {
			byte[] buffer = new byte[buffer_size];
			
			while (inputStream.read(buffer) != -1) {
				for (int i = 0; i < buffer_size; i++)
					input[count][i] = buffer[i];
				count++;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		// transmission begins
		begin = System.currentTimeMillis();
		
		new GBNSender();
    }
}