package com.pmnm.risk.network;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FileServer extends Thread {
	
	private int serverCapacity; // Exactly how many people will connect.
	private AtomicInteger threadsFinished = new AtomicInteger(0); // Used for synchronisation.
	private ServerSocket server;
	Socket clientSock;
	
	private List<Socket> connections; // Socket is basically connection between two computers.
	private List<DataOutputStream> outputs;
	
	private List<DataInputStream> inputs;
	private List<Thread> streamThreads; // Socket listeners.
	private List<Boolean> isThreadFinished; // Used to stop threads that finished listening.
	
	public FileServer(int serverCapacity) {
		this.serverCapacity = serverCapacity;
		connections = new ArrayList<>();
		outputs = new ArrayList<>();
		inputs = new ArrayList<>();
		streamThreads = new ArrayList<>();
		isThreadFinished = new ArrayList<>();
	}
	
	public static void main(String[] args) {
		new Thread(new FileServer(1)).start();
		System.out.println("***********************");
	}

	
	public void run() {
		try (ServerSocket sv = new ServerSocket(27015, serverCapacity)) {
			server = sv;
			// Auto connect to server.
			// User specifies the name ("HOST")
			System.out.println("egegegegegeegegegegegegegegege");
			// Wait for connections to be made.
			
		
			waitForConnection();
			
			// Loop forever to get chat and MP. Finish when everyone leaves.
			whileChatting();
			
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			closeCrap();
		}
	}

	
	private void waitForConnection() throws IOException {
		// Wait for all connections. Exactly the value of serverCapacity connections
		// must be made.
		for (int i = 0; i < serverCapacity; i++) {
			Socket connection = server.accept();
			connections.add(connection);
			setupStreams(connection);
		}
		System.out.println("Streams are created");
	}

	// *************************************************************************
		private void setupStreams(Socket socket) throws IOException {
			// Set up output and input stream to communicate with others.
			// Output stream for sending.
			// Input stream for receiving.
			outputs.add(new DataOutputStream(socket.getOutputStream()));
			inputs.add(new DataInputStream(socket.getInputStream()));
		}
		
		// *************************************************************************
		private void whileChatting() {
			// Tell everyone they have connected successfully.
			for (int i = 0; i < serverCapacity; i++) {
				final int ii = i;
				// Create threads that will listen for input streams.
				streamThreads.add(new Thread(() -> {
					DataInputStream input = inputs.get(ii);
					while (!isThreadFinished.get(ii)) {
						try {
							
							FileOutputStream fos = new FileOutputStream("testfile.jpg");
							byte[] buffer = new byte[90000];
							
							int filesize = 90000; // Send file size in separate msg
							int read = 0;
							int totalRead = 0;
							int remaining = filesize;
							while((read = inputs.get(ii).read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
								totalRead += read;
								remaining -= read;
								System.out.println("read " + totalRead + " bytes.");
								fos.write(buffer, 0, read);
							}
							
							fos.close();
							
							Thread.sleep(200);
						}catch (IOException ex) {
							ex.printStackTrace();
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
							ex.printStackTrace();
						}
					}
					// Increment the amount of threads finished. If threadFinished ==
					// serverCapacity, everyone left the game.
					threadsFinished.incrementAndGet();
					if (threadsFinished.get() == serverCapacity) {
						synchronized (this) {
							notifyAll();
						}
					}
				}));
				// Initialise threadFinished list to true. If a thread's finished value is false
				// it will loop forever, else it will shut down, stopping listening for more
				// input.
				isThreadFinished.add(false);
			}
			// Start all threads.
			streamThreads.forEach(thread -> thread.start());
			try {
				synchronized (this) {
					while (threadsFinished.get() < serverCapacity) {
						// Wait until all threads exited.
						wait();
					}
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				ex.printStackTrace();
			}
		}
		
		
		// *************************************************************************
		private void closeCrap() {
			// Close streams and sockets before closing the program.
			outputs.forEach(output -> {
				try {
					output.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			});
			inputs.forEach(input -> {
				try {
					input.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			});
			connections.forEach(connection -> {
				try {
					connection.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			});
		}
		
}