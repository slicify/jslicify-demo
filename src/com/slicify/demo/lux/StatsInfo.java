package com.slicify.demo.lux;

public class StatsInfo {

	private String machine;
	private int bookingID;
	private String status;
	private int renderCount;
	private double avgRenderTime;
	

	public String getMachine() {
		return machine;
	}
	
	public void setMachine(String machine) {
		this.machine = machine;
		update();
	}
	
	public int getBookingID() {
		return bookingID;
	}
	
	public void setBookingID(int bookingID) {
		this.bookingID = bookingID;
		update();
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
		update();
	}
	
	public int getRenderCount() {
		return renderCount;
	}
	
	public void setRenderCount(int renderCount) {
		this.renderCount = renderCount;
		update();
	}
	
	public double getAvgRenderTime() {
		return avgRenderTime;
	}
	
	public void setAvgRenderTime(double avgRenderTime) {
		this.avgRenderTime = avgRenderTime;
		update();
	}	

	private void update()
	{
		// notify GUI to update
		LuxRenderDemo.Instance.updateGUI();
	}

}
