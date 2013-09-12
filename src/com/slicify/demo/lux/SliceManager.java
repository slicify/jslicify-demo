package com.slicify.demo.lux;

import java.awt.Graphics;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JOptionPane;

import com.slicify.SlicifyNode;

public class SliceManager {

	private int xSlices;
	private int ySlices;
	private int xSliceSize;
	private int ySliceSize;
	
	//each individual subset image
	private ImageSlice[][] imageSlices;
	
	private List<RenderNode> renderNodes = new LinkedList<RenderNode>();
	
	//access to the website for booking/control
	private SlicifyNode Node = new SlicifyNode();
	public BlockingQueue<ImageSlice> JobQueue = new LinkedBlockingQueue<>();
	public BlockingQueue<ImageSlice> ImageQueue = new LinkedBlockingQueue<>();
	
	public SliceManager(int totalWidth, int totalHeight, int widthSlices, int heightSlices) throws Exception {

		//set generate properties
		xSlices = widthSlices;
		ySlices = heightSlices;
		xSliceSize = totalWidth / widthSlices;
		ySliceSize = totalHeight / heightSlices;
		
		//initialise slices
		imageSlices = new ImageSlice[xSlices][ySlices];
		for(int i=0;i<xSlices;i++)
			for(int j=0;j<ySlices;j++)
				imageSlices[i][j] = new ImageSlice(xSliceSize, ySliceSize, xSliceSize * i, ySliceSize * j);
		
		try
		{
			//queue work
			queueWork();
	
			//start rendering threads to process the queue
			startRenderInstances();
			
			//start pull thread to pull any rendered images back to here
			startPullThread();
			
		} catch(Exception e)
		{
			JOptionPane.showMessageDialog(null, "Error during initialisation: [" + e.getClass().getName() + "]: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void drawImage(Graphics g) {	
		for(int i=0;i<xSlices;i++)
			for(int j=0;j<ySlices;j++)
				g.drawImage(imageSlices[i][j].Image, i*xSliceSize, j*ySliceSize, null);
	}

	/**
	 * Queue up the work
	 */
	private void queueWork() {
		//add all rendering slices to a global queue
		try {
			for(int i=0;i<imageSlices.length;i++)
				for(int j=0;j<imageSlices[0].length;j++)
						JobQueue.put(imageSlices[i][j]);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
	}

	/**
	 * Query Slicify for available machines, and start a RenderNode for each one
	 */
	private void startRenderInstances() throws Exception {

		//set login name/password
		Node.setUsername(LuxRenderDemo.slicifyUser);
		Node.setPassword(LuxRenderDemo.slicifyPassword);
		
		//try to book as many machines as possible
		boolean moreMachines = true;
		while(moreMachines && renderNodes.size() < (xSlices * ySlices))
		{
			// Book a machine - you can change the values here to specify the particular criteria you want
			int bookingID = Node.bookNode(1, 1, 1, 64);
			if(bookingID <= 0) {
				moreMachines = false;
			}
			else {
				
				//create a basic stats container
				StatsInfo info = new StatsInfo();
				info.setBookingID(bookingID);
				info.setMachine(Node.getMachineSpec(bookingID));
				info.setAvgRenderTime(0);
				info.setRenderCount(0);
				LuxRenderDemo.Instance.StatsContainer.addNodeStats(bookingID, info);

				//for each booked machine, create a RenderNode proxy to manage it
				renderNodes.add(new RenderNode(this, bookingID, LuxRenderDemo.slicifyUser, LuxRenderDemo.slicifyPassword, info));
			}
		}

		if(renderNodes.size() <= 0)
		{
			JOptionPane.showMessageDialog(null, "No Nodes available");
			//throw new Exception("no machines available");
		}		
	}

	private void startPullThread() {
		
		Thread pullThread = new Thread(new ImageDownload(this));
		pullThread.start();		
	}


}
