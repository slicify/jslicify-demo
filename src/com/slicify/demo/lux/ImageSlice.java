package com.slicify.demo.lux;

import java.awt.image.BufferedImage;

public class ImageSlice {

	//pixels
	public BufferedImage Image;

	//done or not?
	public boolean Rendered = false;
	
	public String Filename = null;
	
	//subimage dimensions
	public int startX;
	public int endX;
	public int startY;
	public int endY;
	
	public ImageSlice(int width, int height, int startX, int startY) {
		
		//create new image buffer
		Image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		//set the image dimensions
		this.startX = startX;
		this.endX = startX + width;
		this.startY = startY;
		this.endY = startY + height;
		
		//initialise it
		initialiseImage();
	}
	
	private void initialiseImage()
	{
		for(int i=0;i<Image.getWidth();i++)
			for(int j=0;j<Image.getHeight();j++)
			{
				if(i==0||i==Image.getWidth()-1||j==0||j==Image.getHeight()-1)
					Image.setRGB(i, j, toRGB(0,0,0));
				else
					Image.setRGB(i, j, toRGB((int)(Math.random()*256.0), (int)(Math.random()*256.0), (int)(Math.random()*256.0)));
			}
	}

	
	
	private static int toRGB(int red, int green, int blue)
	{
		int rgb = red;
		rgb = (rgb << 8) + green;
		rgb = (rgb << 8) + blue;
		return rgb;
	}
}
