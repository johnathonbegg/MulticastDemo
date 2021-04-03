import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Server extends Thread{
	MulticastSocket Sock = null; 
	//DatagramSocket dSock = null;
	byte[] buf = new byte[256];
	//0 multi reciever
	//1 multi sender
	int type; 
	String id;
	 private InetAddress group;
	 int counter= 0;
	
	private Server(String id, int type){
		this.id = id;
		this.type = type;
		
		
	}
	
int port = 5141;
	
	public static void main(String[] args) {
		
		
		
		// TODO Auto-generated method stub
		Thread rec1 =  new Server("1",0);
		
		//Thread sen = new Server("sender", 1);
		
		System.out.println("Starting system");
		
		
		rec1.start();
		
		//sen.start();
		
		

	}
	
	public void run(){
		if(type==0){
			recieveMulti();
		}
		else if(type ==1){
			try {
				multicast("hello " + counter);
				counter ++;
				Thread.sleep(1000L);
				multicast("hola"+ counter);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	 public void multicast(
   	      String multicastMessage) throws IOException {
       Sock = new MulticastSocket(port);
       group = InetAddress.getByName("230.0.0.0");
       buf = multicastMessage.getBytes();

       DatagramPacket packet 
         = new DatagramPacket(buf, buf.length, group, port);
       Sock.send(packet);
       Sock.close();
   }
	
	
	public void recieveMulti(){
		 System.out.println("Starting reciever: " + id);
	        try {
	        	Sock = new MulticastSocket(port);
	        	//("230.0.0.0")
		        InetAddress group = InetAddress.getByName("230.0.0.0");
		        Sock.joinGroup(group);
		        while (true) {
		            DatagramPacket packet = new DatagramPacket(buf, buf.length);
		            Sock.receive(packet);
		            String received = new String(
		              packet.getData(), 0, packet.getLength());
		            if ("end".equals(received)) {
		                break;
		            }
		            else{
		            	System.out.println(id + ": "+received);
		            }
		            		
		        }
				Sock.leaveGroup(group);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        Sock.close();
	        System.exit(0);
	}
	

}
