package com.slicify.demo.bitcoin;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.slicify.SlicifyNode;

public class PrimeCoinMining implements Runnable  {

	public String MiningAddress = "";
		
	public Map<Integer, NodeSession> OpenBookings = new ConcurrentHashMap<Integer, NodeSession>();
	
	private String Username;
	private String Password;
	
	public PrimeCoinMining(String slicifyUser, String slicifyPassword, String miningaddress) throws Exception 
	{
		//validate
		if(miningaddress.length() <= 0)
			throw new Exception("Set the correct mining address");
		
		MiningAddress = miningaddress;
		Username = slicifyUser;
		Password = slicifyPassword;

		//start background thread
		Thread restThread = new Thread(this);
		restThread.start();
	}

	@Override
	public void run()
	{
		
		try {
			
			//web services wrapper
			SlicifyNode node = new SlicifyNode();

			//set your website login
			node.setUsername(Username);
			node.setPassword(Password);
			
			List<Integer> bidIDs = node.getActiveBidIDs();

			if(bidIDs.size() == 0)
			{
				System.out.println("No bids set - login manually to the Slicify website and add some active bids");
				System.out.println("They will get picked up automatically by this program - no need to restart");
				System.out.println("Don't forget to cancel/inactivate all your bids after you have finished");
			}
			
			Thread.currentThread().setName("REST Booking Loop");
			while(true)
			{			
				try
				{
					//show latest account balance
					PrimecoinGUI.Instance.updateBalance(node.getAccountBalance());
	
					//get latest booking list from the server
					List<Integer> bookings = node.getActiveBookingIDs();
					System.out.println("** Active bookings: " + bookings.size() + " " + bookings);
					
					for(Integer id : bookings)
					{
						//new bookings
						if(!OpenBookings.keySet().contains(id))
						{
							newBooking(id);
						}
					}
					
					Iterator<Integer> iterator = OpenBookings.keySet().iterator();
					while(iterator.hasNext())
					{
						int id = iterator.next();
						if(!bookings.contains(id))
						{
							//closed booking
							closeBooking(id);
						}
					}

				}
				catch(Exception e)
				{
					System.out.println("Exception in booking loop " + e.getMessage());
				}
				
				//Check again 1 min later
				Thread.sleep(1 * 60 * 1000);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Exception in main booking loop");
		System.exit(0);
	}
		
	private void newBooking(int bookingID) throws Exception 
	{
		//run this session in the background
		System.out.println("Starting new booking: " + bookingID);

		//get REST API link
		SlicifyNode node = new SlicifyNode();
		node.setUsername(Username);
		node.setPassword(Password);		
		
		//create a basic stats container
		StatsInfo info = new StatsInfo();
		info.setBookingID(bookingID);
		info.setStatus("STARTED");
		info.setMachine(node.getMachineSpec(bookingID).trim());
		info.setBidID(node.getBookingBidID(bookingID));
		info.setCountry(node.getBookingCountry(bookingID));
		info.setEcu(node.getECU(bookingID));

		PrimecoinGUI.Instance.StatsContainer.addNodeStats(bookingID, info);
		NodeSession session = new NodeSession(this, Username, Password, bookingID, info);

		//store session info
		OpenBookings.put(bookingID, session);
		session.start();
	}

	private void closeBooking(int bookingID) 
	{	
		if(OpenBookings.containsKey(bookingID))
		{
			//close associated session
			System.out.println("Closing booking: " + bookingID);
			NodeSession session = OpenBookings.get(bookingID);
			session.StatsInfo.setStatus("TERMINATED");
			session.close();
		}
	}



}
