package com.slicify.demo.lux;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Demo of distributed raytracing using the GPL LuxRender raytracer.
 * 
 * LuxRenderDemo creates the Java Swing GUI, and creates a SliceManager to manage the render.
 * SliceManager splits the image into tiles, and books a number of Slicify Nodes to do the rendering. 
 *   It also starts an ImageDownload thread to download completed images
 * Each Node is managed by one RenderNode object, which manages the SSH communications session to the Node. This 
 *   1. sets up each node (installing required libraries etc)
 *   2. polls the SliceManager job queue for render jobs.
 *   3. renders each job, and uploads the resulting image to a MediaFire drive
 *  ImageDownload downloads the resulting images from MediaFire to the local machine.
 *  ImageSlice contains information about each image tile, such as its position and the associated raster buffer
 *  StatsInfo contains information about each Node, such as the status and number of completed renders
 *   
 * @author slicify
 *
 */
public class LuxRenderDemo extends JFrame {

	private static final long serialVersionUID = 1L;

    public static void main(String[] args) throws Exception {

    	Instance = new LuxRenderDemo();
    	Instance.start();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
	                Instance.setVisible(true);
            }
        });
    }

    public static LuxRenderDemo Instance;
    
    //configuration
    public static int imageWidth = 800;
    public static int imageHeight = 600;
	private static int widthSlices = 8;
	private static int heightSlices = 6;

	//mediafire application ID/key
	public static String mfId = "35850";
	public static String mfKey = "l6s31xjiat9s8ff3031ncnm9jn9i7884p71jtsno";    

	public static String slicifyUser;
	public static String slicifyPassword;
	public static String mfUser;
	public static String mfPassword;
	
    private GraphicsContainer GraphicsContainer = new GraphicsContainer();
    public StatsContainer StatsContainer = new StatsContainer(); 
	
    public LuxRenderDemo() throws Exception {
    }

    private void start() throws Exception {
    	
	    //init main window UI
        initUI();

        //popup login dialog
	    LoginDialog dlg = new LoginDialog(this, new JFrame(), "Slicify LuxRender Demo");
	    dlg.setLocationRelativeTo(null);
	    dlg.setVisible(true);
    }

	public void onLogin(String username, char[] password, String mfuser, char[] mfpass) throws Exception {
		
		slicifyUser = username;
		slicifyPassword = String.valueOf(password);
		mfUser = mfuser;
		mfPassword = String.valueOf(mfpass);
		
    	//initialise and start app
        GraphicsContainer.Manager = new SliceManager(imageWidth, imageHeight, widthSlices, heightSlices); 
	}


    private void initUI() {

    	//set window title
        setTitle("Slicify Node - LuxRender Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //add controls
        JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitter.setOneTouchExpandable(true);
        splitter.setDividerLocation(180);
        add(splitter);
        
        splitter.add(GraphicsContainer);
        splitter.add(StatsContainer);
        validate();

        //set size
        pack();
        setMinimumSize(new Dimension(imageWidth + getInsets().left + getInsets().right + splitter.getDividerSize(), imageHeight + getInsets().top + getInsets().bottom));
        //setResizable(false);
        pack();
        
        //centre on screen
        setLocationRelativeTo(null);
        setVisible(true);
    }

    //redraw screen when we have a new slice available
	public void updateGUI() {
		
		//notify swing to redraw screen
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				validate();
				repaint();
			}
		});
	}

}

class StatsContainer extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private Map<Integer, StatsInfo> Stats = new HashMap<Integer, StatsInfo>();
	
	private JTable table;
	private StatsTableModel tableData;
	
	public StatsContainer() {
		initUI();
	}

	private void initUI()
	{
		//create table and place in a pane
		tableData = new StatsTableModel();
		table = new JTable(tableData);
		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);		
		scrollPane.setPreferredSize(new Dimension(600,580));
		add(scrollPane);
	}

	public void addNodeStats(int bookingID, StatsInfo info)
	{
		Stats.put(bookingID, info);
		tableData.add(info);
		LuxRenderDemo.Instance.updateGUI();
	}
	
	public StatsInfo getNodeStats(int bookingID)
	{
		return Stats.get(bookingID);
	}
	
}

class StatsTableModel extends DefaultTableModel {
	
	private static final long serialVersionUID = 1L;

	private static String[] columnNames = {"Machine",
						            "Booking ID",
						            "Status",
						            "Render #",
						            "Avg Render Time"};

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
			return info.getMachine();
		case 1:
			return info.getBookingID();
		case 2:
			return info.getStatus();
		case 3:
			return info.getRenderCount();
		case 4:
			return info.getAvgRenderTime();
		}
		
		return null;
	}
	
}

class GraphicsContainer extends JPanel {

	private static final long serialVersionUID = 1L;
	public SliceManager Manager;
	
    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        
        //call manager to draw the (partially) completed image
        if(Manager != null)
        	Manager.drawImage(g);
    }
}

