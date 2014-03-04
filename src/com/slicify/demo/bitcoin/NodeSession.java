package com.slicify.demo.bitcoin;

import com.slicify.SlicifyNode;

/**
 * This is created once for each booking ID, and connects, sets up an monitors
 * the connection to that machine. It will also retry the connection if the 
 * connection fails for any reason.
 * 
 * @author slicify
 *
 */
public class NodeSession implements Runnable {

	public String Username;
	public String Password;
	public int BookingID;
	public int BidID;
	public StatsInfo StatsInfo;

	private PrimeCoinMining Manager;
	private PrimeCoinNode session;
	private Thread runThread;
	
	public NodeSession(PrimeCoinMining manager, String username, String password, int bookingID, StatsInfo statsinfo) {
		Manager = manager;
		Username = username;
		Password = password;
		BookingID = bookingID; 
		StatsInfo = statsinfo;
	}

	public void start() throws Exception {

		SlicifyNode node = new SlicifyNode();
		node.setUsername(Username);
		node.setPassword(Password);
		
		runThread = new Thread(this);
		runThread.setName("SSH Monitor " + BookingID);
		runThread.start();
	}
	
	
	@Override
	public void run() {

		boolean OK = true;
		while(OK)
		{
			SlicifyNode node = new SlicifyNode();
			node.setUsername(Username);
			node.setPassword(Password);
	
			try {			
				//wait until node is ready for access
				Log("Waiting for machine");
				node.waitReady(BookingID);		
				
				//get machine specs and login info
				String machineSpec = node.getMachineSpec(BookingID);
				Log("Machine spec:" + machineSpec);
				int cores = node.getCoreCount(BookingID);
				Log("Cores:" + cores);
				int ecu = node.getECU(BookingID);
				Log("ECU:" + ecu);
	
				String bookingOTP = node.getBookingPassword(BookingID);
				Log("Booking OTP:" + bookingOTP);					
				String sudoPW = node.getSudoPassword(BookingID);
				Log("Sudo PW:" + sudoPW);					
	
				//PrimeCoinNode to manage each node
				session = new PrimeCoinNode(Username, BookingID, bookingOTP, sudoPW, cores, Manager.MiningAddress, StatsInfo);
				session.start();	
				
			} catch (Error | Exception e) {
				Log("Exception - shutting down " + e.getMessage());
				StatsInfo.setStatus("SHUTTING DOWN:" + e.getMessage());
				e.printStackTrace();
				OK = false;
				close();
			}		
		}
	}

	public void close() {
		
		//TODO graceful kill
		try
		{		
			if(session != null)
				session.stop();
		}
		catch(Exception e) {}
		
		//delete ourselves from connection list
		Log("Removing openbooking entry");
		Manager.OpenBookings.remove(BookingID);

		//try to shut down background thread
		try
		{		
			if(runThread != Thread.currentThread() && runThread != null)
				runThread.stop();
		}
		catch(Error e) {}
	}

	private void Log(String output) {
		System.out.println(BookingID + " " + output); 	
	}

}