package bully;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

public class Bullydemo {
	MulticastSocket msock;
	private int port = 5141;
	private int myid;
	private static boolean coordinator;
	private InetAddress group;
	private int numprocesses;
	DatagramPacket packet;
	static Thread handler;
	static Thread listener; 
	static Timer timer;
	static TimerTask awaitHeartbeat;

	static Object lock = new Object();

	// messages between servers
	private String[] comms = new String[] { "JOIN", "NEWID:", "HEARTBEAT:", "ELECTION:", "AWK", "COORDINATOR" };

	byte[] buf = new byte[256];

	private int timeout = 3000; // 3 seconds

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Bullydemo demo = new Bullydemo();
		demo.initialize();

	}

	public Bullydemo() {
		packet = new DatagramPacket(buf, buf.length);
		try {
			msock = new MulticastSocket(port);
			group = InetAddress.getByName("230.0.0.0");
			// set timeout to 3 seconds
			msock.setSoTimeout(timeout);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//define threads
		
		awaitHeartbeat = new TimerTask(){

			@Override
			public void run() {
				//will run if heartbeat not heard in alloted time
				String message = comms[5] + myid;
				byte[] b = new byte[256];
				b = message.getBytes();
				
				DatagramPacket pack = new DatagramPacket(b,b.length,group,port);
				try {
					//sending election signal with my id
					msock.send(pack);
					
					//wait for at least 1 awknoledgement. 
					//if now AWK recived become coordinator
					//if awk recived, expect
					msock.receive(pack);
					
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					//now awk means i am coordinaror
				}
				
			}
			
		};
			//cordinates with other servers
		handler = new Thread() {
			public void run() {
				System.out.println("Listening for other processes");
				// listen for new servers joining.
				while (true) {
					byte[] buf = new byte[256];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					try {
						msock.receive(packet);
						String msg = new String(packet.getData(), 0, packet.getLength());

						System.out.println(msg);

						if (msg.contains(comms[0])) {
							
							System.out.println("new process joining");
							synchronized (lock) {
								String send = comms[1] + numprocesses;
								numprocesses++;
								buf = send.getBytes();
								packet = new DatagramPacket(buf,buf.length);
								
								msock.send(packet);
							}

						} // if other process sends an election while
							// Coordinator is still tunning
							// announce that I am coordinator
						else if (msg.contains(comms[3])) {
							// in this case it is possible that other
							// servers do not know
							// the coordinator is runniing. anncounc that i
							// am still the coordinator
							buf = comms[5].getBytes();
							packet = new DatagramPacket(buf, buf.length, group, port);
							msock.send(packet);
						}

					} catch (IOException e) {
							System.out.println("nothing heard");
					}

				}
			}
		};
		
		listener = new Thread(){
			public void run(){
				try {
					DatagramPacket p = new DatagramPacket(buf,buf.length);
					msock.receive(p);
					String msg = new String(p.getData(), 0, p.getLength());
					
					//recieve ID:# - indicates new server has been added
					if(msg.contains(comms[1])){
						int temp = msg.indexOf(':') + 1;
						String convert = msg.substring(temp);
						numprocesses = Integer.parseInt(convert);
					}
					//recive heatbeat -- coordinator is alive, check vecotr clock
					else if(msg.contains(comms[2])){
						timer.cancel();
						timer.purge();
						//reset timer
						timer = new Timer();
						timer.scheduleAtFixedRate(awaitHeartbeat, timeout, timeout);
						//update vector clock
						int temp = msg.indexOf(':') + 1;
						String convert = msg.substring(temp);
						int t = Integer.parseInt(convert);
						
					}
					//recivee election
					else if(msg.contains(comms[3])){
						timer.cancel();
						timer.purge();
						
						int temp = msg.indexOf(':') + 1;
						String convert = msg.substring(temp);
						temp = Integer.parseInt(convert);
						
						if(myid < temp){
							doElection();
						}
						else{
							//else wait for coordinator
							
						}
						timer.scheduleAtFixedRate(awaitHeartbeat, timeout, timeout);
					};
					
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//no responces 
					e.printStackTrace();
				}
				
			}
		}
		;

	}
	
	public void doElection() throws IOException{
		
		String msg = comms[3] + myid;
		System.out.println("joining election - " +msg);
		byte[] buff = new byte[256];
		buff = msg.getBytes();
		
		DatagramPacket p = new DatagramPacket(buff, buff.length, group, port); 
		msock.send(p);
		
		msock.receive(p);
		msg = new String(p.getData(),0,p.getLength());
		
		
	}

	

	public void initialize() {

		try {
			// inform coordinator i am joining
			buf = comms[0].getBytes();
			packet = new DatagramPacket(buf, buf.length, group, port);
			msock.send(packet);

			boolean setID = false;
			while (!setID) {
				// attempt to recive a message
				// if i recieve nothing then the timeout will catch the
				// exception
				// and make this process the coordinator, otherwise another
				// process is running
				// and may be the coordinaor

				msock.receive(packet);
				String message = new String(packet.getData(), 0, packet.getLength());

				if (message.contains(comms[1])) {
					int temp = message.indexOf(':') + 1;
					String convert = message.substring(temp);
					System.out.println(convert);

					myid = Integer.parseInt(convert);
					setID = true;
					
					
					
					timer.scheduleAtFixedRate(awaitHeartbeat, timeout, timeout);
					
					
				}
				System.out.println(message);


			}

		} catch (SocketException e) {
			e.printStackTrace();
			//Coordinator();
			
		}
		// buf = multicastMessage.getBytes();
		catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			Coordinator();
		}
	}
	
	public void Coordinator(){
		System.out.println("I AM COORDINATOR");
		// timeout
		coordinator = true;
		// this is first process
		numprocesses = 1;
		myid = 0;

		// start heartbeat
		TimerTask heartbeat = new TimerTask() {

			@Override
			public void run() {
				// TODO heartbeat can also indicate current vecotr clock to other proccesses. 
				buf = comms[2].getBytes();
				DatagramPacket packet = new DatagramPacket(buf, buf.length, group, port);
				try {
					msock.send(packet);
					System.out.println("heartbeat sent");
				} catch (IOException e) {
					
					e.printStackTrace();
				}
			}

		};
		timer = new Timer();
		timer.scheduleAtFixedRate(heartbeat, timeout / 2, timeout / 2);

		// listen for new servers to joiin network
		
		handler.start();
	}

}
