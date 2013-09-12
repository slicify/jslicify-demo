package com.slicify.demo.lux;

import java.io.IOException;
import com.slicify.NodeSSHClient;
import com.slicify.SlicifyNode;

public class RenderNode implements Runnable {

	private int BookingID;
	private SliceManager Manager;
	private Thread NodeAccessThread;
	
	//node command connection
	private SlicifyNode node = new SlicifyNode();
	private String bookingOTP;
	private String sudoPW;	
	
	//node SSH connection
	private NodeSSHClient sshClient = new NodeSSHClient();
	
	//stats info
	private StatsInfo statsInfo;
	
	public RenderNode(SliceManager sliceManager, Integer bookingID, String slicifyUser, String slicifyPassword, StatsInfo info) {
		Manager = sliceManager;
		BookingID = bookingID;
		node.setUsername(slicifyUser);
		node.setPassword(slicifyPassword);

		statsInfo = info;
		
		NodeAccessThread = new Thread(this);
		NodeAccessThread.start();
	}

	@Override
	public void run() {
		
		try
		{
	
			//wait for Node to be ready
			statsInfo.setStatus("Launching");
			node.waitReady(BookingID);
			
			//get login info
			bookingOTP = node.getBookingPassword(BookingID);
			System.out.println("Booking OTP:" + bookingOTP);					
			sudoPW = node.getSudoPassword(BookingID);
			System.out.println("Sudo PW:" + sudoPW);					

			//connect ssh session
			statsInfo.setStatus("Connecting SSH");
			sshClient.connect(LuxRenderDemo.slicifyUser, bookingOTP);

			//setup machine
			statsInfo.setStatus("Setting Up");
			setupNode();
			
			//process jobs - once the queue is empty, shut down
			int renderCount = 0;
			long totalTime = 0;
			while(Manager.JobQueue.peek() != null)
			{
				//get next job
				statsInfo.setStatus("Waiting");
				ImageSlice slice = Manager.JobQueue.take();

				//generate a temporary filename
				String newfilename = "slice-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId() + ".png";
				
				slice.Filename = newfilename;
				
				//render it
				statsInfo.setStatus("Rendering");
				long startTime = System.currentTimeMillis();
				render(slice);
				long endTime = System.currentTimeMillis();
				totalTime += (endTime - startTime);
				
				//retrieve image
				retrieveimage(slice);
				
				//update count
				renderCount ++;
				statsInfo.setRenderCount(renderCount);
				statsInfo.setAvgRenderTime((int)totalTime/renderCount);
			}
			statsInfo.setStatus("Finished");
		}
		catch(Exception e)
		{
			statsInfo.setStatus("Died");
			e.printStackTrace();
		}
		
		//cancel the booking when we've finished rendering
		try {
			node.cancelBooking(BookingID);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	private void setupNode() throws IOException {
		
		//library install
		statsInfo.setStatus("Setting Up - apt-get update");
		sshClient.send("sudo tsocks apt-get -y update", "[sudo] password for slicify: ");
		sshClient.send(sudoPW);
		statsInfo.setStatus("Setting Up - install libs");
		sshClient.send("sudo tsocks apt-get -y install freeglut3 freeglut3-dev unzip");
		
		//download & install plowshare to upload files to mediafire
		statsInfo.setStatus("Setting Up - install filesharing");
		sshClient.send("tsocks wget https://launchpad.net/plowshare/trunk/release/+download/plowshare-snapshot-git20120208.tar.gz");
		sshClient.send("tar -xvzf plowshare-snapshot-git20120208.tar.gz");
		sshClient.send("cd plowshare-snapshot-git20120208");
		sshClient.send("sudo ./setup.sh install");
		sshClient.send("cd ..");

		//Luxrender installation
		statsInfo.setStatus("Setting Up - install renderer");
		sshClient.send("tsocks wget http://www.luxrender.net/release/luxrender/1.2/linux/64/lux-v1.2.1-x86_64-sse2.tar.bz2");
		sshClient.send("tar xvjf lux-v1.2.1-x86_64-sse2.tar.bz2");
		sshClient.send("cd lux-v1.2.1-x86_64-sse2/");
		statsInfo.setStatus("Setting Up - install scene files");
		sshClient.send("mkdir scene");
		sshClient.send("cd scene", "scene$");
		sshClient.send("tsocks wget https://secure.slicify.com/ClientDownloads/luxtime-demo-scene.zip", "scene$");
		sshClient.send("unzip luxtime-demo-scene.zip", "scene$");
		sshClient.send("cd ..");
				
	}

	private void render(ImageSlice slice) throws IOException {
		
		//Luxrender
		int xSize = 800;
		int ySize = 600;
		
		//TODO - include effect of original screenwindow settings
		/*
		double scnXStart = -1;
		double scnXEnd = 1;
		double scnYStart = -0.75;
		double scnYEnd = 0.75;
	 	*/
		
		int imgWidth = (slice.endX - slice.startX);
		int imgHeight = (slice.endY - slice.startY);
		
		String resConfig = "echo -e \"\\\"integer xresolution\\\" [" + imgWidth + "]\\n\\\"integer yresolution\\\" [" + imgHeight + "]\" > scene/resolution.lxs";

		double xStart = ((-1.0 * xSize/2.0) + slice.startX) / (xSize/2.0);
		double xEnd = ((-1.0 * xSize/2.0) + slice.startX + imgWidth) / (xSize/2.0);
		double yStart = ((-1.0 * ySize/2) + imgHeight + slice.startY) / (-1.0 * ySize/2.0);
		double yEnd = ((-1.0 * slice.startY) + ySize/2.0) / (ySize/2.0);
		
		String windowConfig = "echo -e \"\\\"float screenwindow\\\" [" + xStart + " " + xEnd + " " + yStart + " " + yEnd + "]\" > scene/screenwindow.lxs";

		sshClient.send(resConfig);
		sshClient.send(windowConfig);

		//render
		sshClient.send("./luxconsole -o../" + slice.Filename + " scene/luxtime.lxs", "rendering done");

	}

	private void retrieveimage(ImageSlice slice) throws Exception {
		
		//post to mediafire (in background)
		sshClient.send("tsocks plowup -b " + LuxRenderDemo.mfUser + ":" + LuxRenderDemo.mfPassword + " mediafire " + slice.Filename + " >/dev/null 2>&1 &");

		//add download task to queue (in background)
		Manager.ImageQueue.put(slice);		
	}

	
}
