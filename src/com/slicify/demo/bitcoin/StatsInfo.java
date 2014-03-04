package com.slicify.demo.bitcoin;

import java.util.Date;

public class StatsInfo {

	private String machine;
	private int bookingID;
	private int bidID;
	private String country;
	private int ecu;
	private Date lastUpdate;
	
	private String status; //generic status

	private String lastPerf;  //last performance stats
	private String chainsFound; //how many chains found/status
	

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
	
	public int getBidID() {
		return bidID;
	}
	
	public void setBidID(int bidID) {
		this.bidID = bidID;
	}

	public int getEcu() {
		return ecu;
	}
	
	public void setEcu(int ecu) {
		this.ecu = ecu;
		update();
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
		update();
	}

	public String getChainsFound() {
		return chainsFound;
	}
	
	public void setChainsFound(String chainsFound) {
		this.chainsFound = chainsFound;
		update();
	}
	
	public String getCountry() {
		return country;
	}
	
	public void setCountry(String country) {
		this.country = country;
		update();
	}
	
	public String getLastPerf() {
		return lastPerf;
	}
	
	public void setLastPerf(String lastPerf) {
		this.lastPerf = lastPerf;
		update();
	}	
	
	private void update()
	{
		// notify GUI to update
		setLastUpdate(new Date());
		PrimecoinGUI.Instance.updateGUI();
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	

}
