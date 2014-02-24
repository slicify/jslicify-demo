package com.slicify.demo.bitcoin;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.slicify.NodeSSHClient;

/**
 * Sets up, runs and monitors a Primcoin mining node.
 * 
 * @author slicify
 *
 */
public class PrimeCoinNode implements Runnable {

	private NodeSSHClient sshClient = new NodeSSHClient();
	
	private String Username;
	private int BookingID;
	private String OTP;
	private String RootPW;
	private int Cores;

	private static long INSTALL_TIMEOUT = 1000 * 60 * 45; //45 minute timeout to complete install - otherwise raise error
		
	public PrimeCoinNode(String username, int bookingID, String bookingOTP, String sudoPW, int cores) {
		Username = username;
		BookingID = bookingID;
		OTP = bookingOTP;
		RootPW = sudoPW;
		Cores = cores;
	}

	public void start() throws Exception {
	
		//run SSH thread in background, so we can monitor for timeouts etc.
		Thread sshThread = new Thread(this);
		sshThread.setName("SSH setup " + BookingID);
		sshThread.start();
		
		long start = System.currentTimeMillis();
		long end = start + INSTALL_TIMEOUT;
		
		//loop while waiting for startup
		while(System.currentTimeMillis() < end && !sshClient.isConnected())
		{
			Thread.sleep(1000);
		}
		
		Log("SSH Connected [" + connectStatus + "]");
		while(System.currentTimeMillis() < end && !connectStatus.equals("COMPLETE") && sshClient.isConnected())
		{
			Log("Waiting for startup [" + connectStatus + "]");
			Thread.sleep(10000);
			if(connectStatus.equals("EXCEPTION"))
				throw new Exception("Connection threw exception");
		}

		//check if completed after timeout
		if(!connectStatus.equals("COMPLETE"))
			throw new Exception("Couldnt complete startup in time: " + BookingID);

		//if completed, tail log to get info
		double oldEma = 0;
		double alpha = 0.1;
		double lastval = 0;
		while(sshClient.isConnected())
		{
			String logText = sshClient.expectLiteral("chains/d", true);
			
			Pattern pattern = Pattern.compile("(?<=5-chains/h,)(.*)(?=chains/d)");
			Matcher matcher = pattern.matcher(logText);

			if (matcher.find())
			{
			    String schainsDay = matcher.group().trim();
			    double chainsDay = Double.parseDouble(schainsDay);
			    
			    //stop exection if our average p/s rate drops below 950
			    if(oldEma == 0)
			    {
			    	oldEma = chainsDay;
			    	lastval = chainsDay;
			    }
			    else
			    {
			    	oldEma = alpha * (lastval) + (1 - alpha) * oldEma;
			    	lastval = chainsDay;
			    }

			    System.out.println(SimpleDateFormat.getDateTimeInstance().format(new Date()) + " >>> " + BookingID + " = " + chainsDay + "(" + oldEma + ")");
			}
			
			//detect "force reconnect if possible!" and restart
			if (logText.contains("force reconnect if possible!"))
			{
				//this will abort the current shell. It will then get reopened 1 min later
			    System.out.println(SimpleDateFormat.getDateTimeInstance().format(new Date()) + " >>> " + BookingID + " force disconnect");
				throw new Exception("Force reconnect");
			}
		}
	}

	private volatile String connectStatus = "UNINITIALISED";
	
	public void run()
	{
		//connect
		try {
			
			//debugging to console
			sshClient.setConsoleLogging(false);
			//sshClient.setOutputStream(System.out);
			
			//connect to node
			sshClient.connect(Username, OTP);
			connectStatus = "CONNECTED";
			System.out.println(BookingID + " connected");
			
			//apt-get update
			connectStatus = "UPDATING";
			sshClient.send("sudo tsocks apt-get update -y", "[sudo] password for slicify: ");
			sshClient.send(RootPW);
			System.out.println(BookingID + " finished sudo update");
			
			//library install
			connectStatus = "INSTALLING LIBS";
			sshClient.send("sudo tsocks apt-get install -y libboost1.48-all-dev libdb++-dev");
			sshClient.send("sudo tsocks apt-get install -f");									//fixes any dependency issues
			sshClient.send("sudo tsocks apt-get install -y libboost1.48-all-dev libdb++-dev");
			System.out.println(BookingID + " finished library install");

			//download, clean and run the exe
			connectStatus = "DOWNLOADING";
			sshClient.send("tsocks wget https://secure.slicify.com/ClientDownloads/pm64-sgc.gz");
			sshClient.send("gunzip -f pm64-sgc.gz"); //force overwrite if file already exists
			sshClient.send("chmod +x pm64-sgc");
			sshClient.send("killall pm64-sgc"); 	 //make sure we shut down any zombie processes from earlier sessions
			
			System.out.println(BookingID + " launching miner");
			connectStatus = "RUNNING";
			
			sshClient.send("tsocks ./pm64-sgc -poolip=176.34.128.129 -poolport=1337 " +
					"-pooluser=" + PrimeCoinMining.MiningAddress + " -poolpassword=PASSWORD -genproclimit=" + Cores, "[STATS]", false, true);
			
			connectStatus = "COMPLETE";

		} catch (IOException e) {
			connectStatus = "EXCEPTION";
			Log("Exception setting up node " + e.getMessage());
			e.printStackTrace();
			stop();
		}
	}

	public void stop() {
		try {
			sshClient.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private void Log(String output) {
		System.out.println(BookingID + " " + output); 	
	}

}
