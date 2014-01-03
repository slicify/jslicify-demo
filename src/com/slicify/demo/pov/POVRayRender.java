package com.slicify.demo.pov;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.slicify.NodeSSHClient;
import com.slicify.SlicifyNode;

public class POVRayRender {
	
	public static void main(String[] args) {
		
		//web services wrapper
		SlicifyNode node = new SlicifyNode();
		int bookingID = -1;

		//timing
		SortedMap<Long, String> times = new TreeMap<Long, String>();
		
		try {
			
			//get username/password
			InputStreamReader isr = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isr);
			System.out.println("Enter your slicify.com username:");			
			String username = br.readLine();

			System.out.println("Enter your slicify.com password:");
			String password = br.readLine();

			System.out.println("Enter your mediafire username:");			
			String mfUser = br.readLine();

			System.out.println("Enter your mediafire password:");
			String mfPassword = br.readLine();

			//set your website login
			node.setUsername(username);
			node.setPassword(password);

			times.put(System.currentTimeMillis(), "Start");
			
			// Book a machine - you can change the values here to specify the particular criteria you want			
			bookingID = node.bookNode(2, 1024, 0.01, 64, 10);
			
			if(bookingID < 0)
				System.out.println("Error thrown from booking call");
			else if(bookingID == 0)
				System.out.println("No machines available");
			else
				System.out.println("BookingID:" + bookingID);
			
			if(bookingID > 0)
			{
				//wait until node is ready for access
				node.waitReady(bookingID);
				
				//get login info
				String bookingOTP = node.getBookingPassword(bookingID);
				System.out.println("Booking OTP:" + bookingOTP);					
				String sudoPW = node.getSudoPassword(bookingID);
				System.out.println("Sudo PW:" + sudoPW);					

				//Send some commands via the SSH command session
				NodeSSHClient sshClient = new NodeSSHClient();
				sshClient.connect(username, bookingOTP);

				times.put(System.currentTimeMillis(), "VM Ready");

				//download & install povray renderer
				sshClient.send("tsocks wget http://www.povray.org/redirect/www.povray.org/ftp/pub/povray/Official/Linux/povlinux-3.6.tgz");
				sshClient.send("tar xvfz povlinux-3.6.tgz");
				sshClient.send("cd povray-3.6/");
				sshClient.send("sudo ./install -no-arch-check", "[sudo] password for slicify: "); //avoid povray install error on x64
				sshClient.send(sudoPW);
				sshClient.send("cd ..");
				
				//download & install plowshare to upload files to mediafire
				sshClient.send("tsocks wget https://launchpad.net/plowshare/trunk/release/+download/plowshare-snapshot-git20120208.tar.gz");
				sshClient.send("tar -xvzf plowshare-snapshot-git20120208.tar.gz");
				sshClient.send("cd plowshare-snapshot-git20120208");
				sshClient.send("sudo ./setup.sh install");

				//run povray demo
				sshClient.send("cd ../povray-3.6/");

				times.put(System.currentTimeMillis(), "Setup Complete");

				sshClient.send("povray -iscenes/advanced/benchmark.pov -H768 -W1024");

				times.put(System.currentTimeMillis(), "Render Complete");

				//upload resulting render to mediafire
				sshClient.send("tsocks plowup -b " + mfUser + ":" + mfPassword + " mediafire benchmark.png");

				times.put(System.currentTimeMillis(), "Upload Complete");

				//disconnect
				sshClient.disconnect();
				
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			
			if(node != null && bookingID > 0) 
			{
				// Cancel booking again
				try {
					node.cancelBooking(bookingID);
					System.out.println("Booking cancelled, session terminating");
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Booking may not be cancelled - check from www.slicify.com web site");
				}
			}
		}
		
		
		long startTime = 0;
		long prevTime = 0;
		for(Entry<Long, String> timeKey : times.entrySet())
		{
			//start time will be the first key in the set
			if(startTime == 0)
				startTime = timeKey.getKey();
			if(prevTime == 0)
				prevTime = timeKey.getKey();

			double stageTime = (timeKey.getKey() - prevTime) / 1000.0;
			System.out.println(timeKey.getValue() + " = " + Math.round(stageTime) + " secs");
			prevTime = timeKey.getKey();
		}
		
		double opTime = (prevTime - startTime) / 60000.0;
		System.out.println("Start to finish operation time: "  + Math.round(opTime) + " mins");
	}

}
