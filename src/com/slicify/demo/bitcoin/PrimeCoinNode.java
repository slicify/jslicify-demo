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
	private String MiningAddress;
	private StatsInfo StatsInfo;

	private static long INSTALL_TIMEOUT = 1000 * 60 * 45; //45 minute timeout to complete install - otherwise raise error
		
	public PrimeCoinNode(String username, int bookingID, String bookingOTP, String sudoPW, int cores, String miningAddress, StatsInfo statsInfo) {
		Username = username;
		BookingID = bookingID;
		OTP = bookingOTP;
		RootPW = sudoPW;
		Cores = cores;
		MiningAddress = miningAddress;
		StatsInfo = statsInfo;
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
		while(System.currentTimeMillis() < end && !connectStatus.equals("RUNNING") && sshClient.isConnected())
		{
			Log("Waiting for startup [" + connectStatus + "]");
			Thread.sleep(10000);
			if(connectStatus.contains("EXCEPTION"))
				throw new Exception("Connection threw exception");
		}

		//check if completed after timeout
		if(!connectStatus.equals("RUNNING"))
			throw new Exception("Couldnt complete startup in time: " + BookingID);

		//if completed, tail log to get info
		double oldEma = 0;
		double alpha = 0.1;
		double lastval = 0;
		while(sshClient.isConnected())
		{
			String logText = sshClient.expectLiteral("chains/d", true, 180000);  //should give a stats line once per minute. Throw error if nothing in 3 mins
			
			//no stats - show buffer, which should contain any error message
			//sometimes be^5r goes offline, or there was some other error
			if(!logText.contains("chains/d"))
			{
				Log("Timed out waiting for stats. Current buffer: " + logText);
				throw new Exception("Timed out waiting for stats");
			}
			
			//found a prime chain
			if(logText.contains("Probable prime chain found"))
			{
				Log("Probable prime chain found");
			}
			
			//print updated found prime chain stats
			if(logText.contains("-CH:"))
			{
				Pattern pattern = Pattern.compile("(?<=^)(.*-CH.*)(?=$)", Pattern.MULTILINE);
				Matcher matcher = pattern.matcher(logText);
				if (matcher.find())
				{					
					setChainsFound(matcher.group().trim());
				}
				else
					Log(logText);
			}

			//otherwise just extract chains/d figure as a benchmark
			Pattern pattern = Pattern.compile("(?<=5-chains/h,)(.*)(?=chains/d)");
			Matcher matcher = pattern.matcher(logText);

			if (matcher.find())
			{
			    String schainsDay = matcher.group().trim();
			    setLastPerf(schainsDay);
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
			String login = sshClient.connect(Username, OTP, 60000);
			if(!login.contains(sshClient.PROMPT))
			{
				System.out.println("Error connecting to machine. Login output is: " + login);
				throw new Exception("Error connecting to machine");			
			}
			
			setStatus("CONNECTED");
			System.out.println(BookingID + " connected");
			
			//apt-get update
			setStatus("UPDATING");
			sshClient.send("sudo tsocks apt-get -y update");
			System.out.println(BookingID + " finished sudo update");
			
			//library install
			setStatus("INSTALLING LIBS");
			sshClient.send("sudo tsocks apt-get install -y libboost1.48-all-dev libdb++-dev");
			sshClient.send("sudo tsocks apt-get install -f");									//fixes any dependency issues
			sshClient.send("sudo dpkg --configure -a");											//fixes any occasional problem with installing libs
			sshClient.send("sudo tsocks apt-get install -y libboost1.48-all-dev libdb++-dev");
			System.out.println(BookingID + " finished library install");

			//download, clean and run the exe
			setStatus("DOWNLOADING");
			sshClient.send("killall pm64-sgc"); 	 //make sure we shut down any zombie processes from earlier sessions
			sshClient.send("rm pm64-sgc*"); 	 	 //overwrite any earlier versions/downloads
			sshClient.send("tsocks wget https://secure.slicify.com/ClientDownloads/pm64-sgc.gz");
			sshClient.send("gunzip -f pm64-sgc.gz"); //force overwrite if file already exists
			sshClient.send("chmod +x pm64-sgc");
			
			System.out.println(BookingID + " launching miner");
			setStatus("RUNNING");
			
			String output = sshClient.send("tsocks ./pm64-sgc -poolip=176.34.128.129 -poolport=1337 " +
					"-pooluser=" + MiningAddress + " -poolpassword=PASSWORD -genproclimit=" + Cores, "Xolominer", false, true, 20000);
			
			if(!output.contains("Xolominer"))
			{
				System.out.println("Error launching miner. Output is: " + output);
				throw new Exception("Error launching miner");
			}
			
			setStatus("RUNNING");

		} catch (Exception e) {
			setStatus("EXCEPTION " + e.getMessage());
			Log("Exception setting up node " + e.getMessage());
			e.printStackTrace();
			stop();
		}
	}

	private void setStatus(String status) {
		connectStatus = status;
		StatsInfo.setStatus(status);		
	}

	private void setLastPerf(String lastPerf) {
		StatsInfo.setLastPerf(lastPerf);		
	}

	private void setChainsFound(String chainsFound) {
		StatsInfo.setChainsFound(chainsFound);		
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
