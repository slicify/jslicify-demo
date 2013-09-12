package com.slicify.demo.lux;

import java.io.InputStream;

import javax.imageio.ImageIO;

import com.slicify.MediaFireClient;

public class ImageDownload implements Runnable {

	private SliceManager Manager;
	
	public ImageDownload(SliceManager sliceManager) {
		Manager = sliceManager;
	}
	
	@Override
	public void run() {
		
		try
		{
			boolean running = true;
			while(running)
			{
				//get the next slice (blocks until at least one there)
				ImageSlice slice = Manager.ImageQueue.take();

				//try to download from mediafire
				InputStream pngStream = null;
				try
				{
					MediaFireClient mfClient = new MediaFireClient(LuxRenderDemo.mfUser, LuxRenderDemo.mfPassword, 
							LuxRenderDemo.mfId, LuxRenderDemo.mfKey);
					String quickKey = mfClient.getQuickKey(slice.Filename);
					
					pngStream = mfClient.downloadBLOB(quickKey);

					//read PNG output image
					slice.Image = ImageIO.read(pngStream);
									
					//notify parent to update display
					LuxRenderDemo.Instance.updateGUI();
				}
				catch(Exception e)
				{
					System.out.println(">>> mediafire error - retrying " + slice.Filename + " (" + e.getMessage() + ")");
					Thread.sleep(1000);
					
					//add back for reprocessing
					Manager.ImageQueue.add(slice);
				}
			}
			
		} catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("** Terminating image download thread");
		}
	}

	
}
