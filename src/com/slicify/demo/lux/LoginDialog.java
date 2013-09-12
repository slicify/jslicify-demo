package com.slicify.demo.lux;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Ask for users login details
 * 
 * @author slicify
 *
 */
public class LoginDialog extends JDialog implements ActionListener {
	
	private static final long serialVersionUID = 1L;

	private LuxRenderDemo Demo;

	private JTextField Username = new JTextField();
	private JPasswordField Password = new JPasswordField();

	private JTextField MFUsername = new JTextField();
	private JPasswordField MFPassword = new JPasswordField();

	public LoginDialog(LuxRenderDemo demo, JFrame parent, String title) {		  
	    super(parent, title, true);
	    Demo = demo;
	    
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

		messagePane.add(new JLabel("Mediafire Username"));
		messagePane.add(MFUsername);
		messagePane.add(new JLabel("Mediafire Password"));
		messagePane.add(MFPassword);
	    
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
				Demo.onLogin(Username.getText(), Password.getPassword(), MFUsername.getText(), MFPassword.getPassword());
			} catch (Exception e1) {
				e1.printStackTrace();
			}
	    dispose(); 
	  }
	}