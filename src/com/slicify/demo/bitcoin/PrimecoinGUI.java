package com.slicify.demo.bitcoin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

public class PrimecoinGUI extends JFrame{

	private static final long serialVersionUID = 1L;

	public static PrimecoinGUI Instance;

	public static String slicifyUser;
	public static String slicifyPassword;
	public static String miningAddress;
	
	private static String TITLE = "Slicify - Primecoin Miner Client ";

    public static void main(String[] args) throws Exception {

    	//set native style
    	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    	
    	Instance = new PrimecoinGUI();
    	Instance.start();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
	                Instance.setVisible(true);
            }
        });
    }

    public StatsContainer StatsContainer = new StatsContainer();
    public StatsContainer HistStatsContainer = new StatsContainer();
    public PrimeCoinMining Manager = null;
    private double Balance = 0;

    private void start() throws Exception {
    	
	    //init main window UI
        initUI();

        //popup login dialog
	    LoginDialog dlg = new LoginDialog(this, new JFrame(), "Slicify Primecoin Miner Client");
	    dlg.setLocationRelativeTo(null);
	    dlg.setVisible(true);
    }

    private void initUI() {

    	//set window title
        setTitle(TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(2,1));
        add(StatsContainer);
        add(HistStatsContainer);
        validate();
        pack();
        
        //centre on screen
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    //redraw screen when something changes
	public void updateGUI() {
		
		//notify swing to redraw screen
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				StatsContainer.cleanOld();
				validate();
				repaint();
			}
		});
	}
	
	public void updateBalance(double newBal)
	{
		Balance = newBal;
		
		//notify swing to redraw screen
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
		        setTitle(TITLE + " (Current Balance = $ " + Balance + " )");
			}
		});
	}

    
	/*
	 * Login dialog.
	 */
    public class LoginDialog extends JDialog implements ActionListener {
    	
    	private static final long serialVersionUID = 1L;

    	private PrimecoinGUI Demo;

    	private JTextField Username = new JTextField();
    	private JPasswordField Password = new JPasswordField();

    	private JTextField Address = new JTextField("AcxUi4AyTckaGxUKfpk1Xi6dxXf7AYVWf2");

    	public LoginDialog(PrimecoinGUI primecoinGUI, JFrame parent, String title) {		  
    	    super(parent, title, true);
    	    Demo = primecoinGUI;
    	    
    	    if (parent != null) {
    	      Dimension parentSize = parent.getSize(); 
    	      Point p = parent.getLocation(); 
    	      setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
    	    }
    	    
    	    JPanel messagePane = new JPanel();

    		//add fields
    		GridLayout layout = new GridLayout(4, 2);
    		messagePane.setLayout(layout);
    		
    		messagePane.add(new JLabel("Slicify Username"));
    		messagePane.add(Username);
    		messagePane.add(new JLabel("Slicify Password"));
    		messagePane.add(Password);

    		messagePane.add(new JLabel("Mining Address"));
    		messagePane.add(Address);
    	    
    	    getContentPane().add(messagePane);


    	    JPanel buttonPane = new JPanel();
    	    JButton button = new JButton("OK"); 
    	    buttonPane.add(button); 
    	    button.addActionListener(this);
    	    getContentPane().add(buttonPane, BorderLayout.SOUTH);
    	    
    	    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    	    pack(); 
    	  }
    	
    	  public void actionPerformed(ActionEvent e) {
    	    setVisible(false); 
    		try {
    				Demo.onLogin(Username.getText(), Password.getPassword(), Address.getText());
    			} catch (Exception e1) {
    				e1.printStackTrace();
    			}
    	    dispose(); 
    	  }
	}
    
	public void onLogin(String username, char[] password, String miningaddress) throws Exception {
		
		slicifyUser = username;
		slicifyPassword = String.valueOf(password);
		miningAddress = miningaddress;
		
    	//initialise and start app
		PrimecoinGUI.Instance.Manager = new PrimeCoinMining(slicifyUser, slicifyPassword, miningaddress);
	}

	class StatsContainer extends JPanel {
    	
    	private static final long serialVersionUID = 1L;
   	
    	private JTable table;
    	public StatsTableModel tableData;
    	
    	public StatsContainer() {
    		initUI();
    	}

    	
    	public void cleanOld() {
    		//move dead entries to historical list
    		for(StatsInfo info : tableData.data)
    		{
    			if(info.getStatus().contains("SHUTTING DOWN"))
    			{
    				HistStatsContainer.tableData.add(info);
    				StatsContainer.tableData.data.remove(info);
    			}
    		}
		}

		private void initUI()
    	{
    		//fill parent
            setLayout(new GridLayout());

            //create table and place in a pane
    		tableData = new StatsTableModel();
    		table = new JTable(tableData);
    		table.setAutoCreateRowSorter(true);
    		JScrollPane scrollPane = new JScrollPane(table);
    		table.setFillsViewportHeight(true);
    		scrollPane.setPreferredSize(new Dimension(1024,768/2));
    		add(scrollPane);
    	}

    	public void addNodeStats(int bookingID, StatsInfo info)
    	{
    		tableData.add(info);
    		PrimecoinGUI.Instance.updateGUI();
    	}
    	    	
    }

	private static final String[] columnNames = {
        "Booking ID",
		"Machine",
		"ECU",
        "Country",
        "Status",
        "Chains/d",
        "Chains Found",
        "Last Update"};


    class StatsTableModel extends DefaultTableModel {
    	
    	private static final long serialVersionUID = 1L;

    	private List<StatsInfo> data = new ArrayList<StatsInfo>();
    	
    	@Override
    	public String getColumnName(int column) {
    		return columnNames[column];
    	}
    	
    	public void add(StatsInfo info) {
    		data.add(info);
    	}

    	@Override
    	public int getColumnCount() {
    		return columnNames.length;
    	}
    	
    	@Override
    	public int getRowCount() {
    		return data == null ? 0 : data.size();
    	}
    	
    	@Override
    	public Object getValueAt(int row, int column) {
    		StatsInfo info = data.get(row);
    		
    		switch(column)
    		{
    		case 0:
    			return info.getBookingID();
    		case 1:
    			return info.getMachine();
    		case 2:
    			return info.getEcu();
    		case 3:
    			return info.getCountry();
    		case 4:
    			return info.getStatus();
    		case 5:
    			return info.getLastPerf();
    		case 6:
    			return info.getChainsFound();
    		case 7:
    			return info.getLastUpdate();
    		}
    		
    		return null;
    	}
    	
    }

}
