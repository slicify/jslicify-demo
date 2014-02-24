package com.slicify.demo.bitcoin;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.slicify.SlicifyNode;

/**
 * Example console app for primecoin mining. Books any available nodes and sets them up with the beeeeer.org
 * pool primecoin miner.
 * 
 * Remember to change your mining address
 * This also downloads an older precompiled pool miner from our website. You can grab and recompile
 * the updated ones if you prefer.
 * 
 * @author slicify
 *
 */
public class PrimeCoinMining {

	public static String MiningAddress = "";
		
	public static Map<Integer, NodeSession> OpenBookings = new ConcurrentHashMap<Integer, NodeSession>();
	
	private static String Username;
	private static String Password;
	
	public static void main(String[] args) {
		
		//web services wrapper
		SlicifyNode node = new SlicifyNode();

		try {
			
			//get username/password
			InputStreamReader isr = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isr);
			
			System.out.println("http://www.slicify.com Example Primecoin Miner. Source at https://github.com/slicify/jslicify-demo\n");

			System.out.println("Enter your mining address [default AcxUi4AyTckaGxUKfpk1Xi6dxXf7AYVWf2]:");			
			String address = br.readLine();
			if(address.length() <= 0)
				address = "AcxUi4AyTckaGxUKfpk1Xi6dxXf7AYVWf2";
			MiningAddress = address;
			
			System.out.println("Enter your slicify.com username:");			
			Username = br.readLine();

			System.out.println("Enter your slicify.com password:");
			Password = br.readLine();

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
	}
		
	private static void newBooking(int bookingID) throws Exception 
	{
		//run this session in the background
		System.out.println("Starting new booking: " + bookingID);
		NodeSession session = new NodeSession(Username, Password, bookingID);

		//store session info
		OpenBookings.put(bookingID, session);
		session.start();
	}

	private static void closeBooking(int bookingID) 
	{	
		if(OpenBookings.containsKey(bookingID))
		{
			//close associated session
			System.out.println("Closing booking: " + bookingID);
			NodeSession session = OpenBookings.get(bookingID);
			session.close();
		}
	}



}
