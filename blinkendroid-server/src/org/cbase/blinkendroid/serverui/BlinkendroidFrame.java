package org.cbase.blinkendroid.serverui;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.sound.sampled.ReverbType;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;

import org.cbase.blinkendroid.player.bml.BLMHeader;
import org.cbase.blinkendroid.player.bml.BLMManager.BLMManagerListener;
import org.cbase.blinkendroid.player.image.ImageHeader;
import org.cbase.blinkendroid.player.image.ImageManager.ImageManagerListener;

public class BlinkendroidFrame extends JFrame implements ImageManagerListener,
	BLMManagerListener {

    private static final long serialVersionUID = 1L;
    BlinkendroidSwingServer server;

    // Controls
    private JButton startStopButton = null;
    private JComboBox moviesList = null;
    private JComboBox imagesList = null;
    private JLabel titleLbl, moviesLbl, imagesLbl;
    private JList clientsList;


    public void imagesReady() {
	JOptionPane.showMessageDialog(this, "Images Ready");

	DefaultComboBoxModel imgCbModel = (DefaultComboBoxModel)imagesList.getModel();
	imgCbModel.removeAllElements();
	
	for (ImageHeader imgHead : server.getImageManager().getImageHeader()) {
	    imgCbModel.addElement(imgHead);
	}
    }


    public void moviesReady() {
	JOptionPane.showMessageDialog(this, "Movies Ready");

	DefaultComboBoxModel movCbModel = (DefaultComboBoxModel)moviesList.getModel();
	movCbModel.removeAllElements();
	
	for (BLMHeader movHead : server.getBlmManager().getBlmHeader()) {
	    movCbModel.addElement(movHead);
	}
    }

    /**
     * This is the default constructor
     * 
     * @param server
     */
    public BlinkendroidFrame(BlinkendroidSwingServer server) {
	super();
	this.server = server;
	initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {

	this.setTitle("Blinkendroid SwingUI");

	titleLbl = new JLabel("Blinkendroid SwingUI");
	moviesLbl = new JLabel("Movies:");
	imagesLbl = new JLabel("Images:");

	clientsList = new JList(new String[] { "Client1", "Client2" });

	ActionListener actionListener = new ButtonActionListener();
	imagesList = new JComboBox(new String[] { "Images" });
	imagesList.setActionCommand(Commands.IMAGES_SELECTION.toString());
	imagesList.addActionListener(actionListener);
	moviesList = new JComboBox(new String[] { "Movies" });
	moviesList.setActionCommand(Commands.MOVIES_SELECTION.toString());
	moviesList.addActionListener(actionListener);

	startStopButton = new JButton("start Server");
	startStopButton.setActionCommand(Commands.START_STOP.toString());
	startStopButton.addActionListener(actionListener);

	this.setSize(350, 370);
	this.setResizable(false);

	Container jContentPane = getContentPane();
	jContentPane.setLayout(null);

	titleLbl.setLocation(0, 0);
	titleLbl.setSize(150, 20);

	moviesLbl.setLocation(0, 30);
	moviesLbl.setSize(100, 20);

	imagesLbl.setLocation(0, 60);
	imagesLbl.setSize(100, 20);

	moviesList.setLocation(120, 30);
	moviesList.setSize(200, 20);

	imagesList.setLocation(120, 60);
	imagesList.setSize(200, 20);

	clientsList.setLocation(120, 90);
	clientsList.setSize(200, 200);

	startStopButton.setLocation(120, 310);
	startStopButton.setSize(200, 30);

	jContentPane.add(titleLbl);
	jContentPane.add(moviesLbl);
	jContentPane.add(moviesList);
	jContentPane.add(imagesLbl);
	jContentPane.add(imagesList);
	jContentPane.add(clientsList);
	jContentPane.add(startStopButton);

    }

    private enum Commands {
	START_STOP, MOVIES_SELECTION, IMAGES_SELECTION;
    }

    private class ButtonActionListener implements ActionListener {


	public void actionPerformed(ActionEvent e) {

	    switch (Commands.valueOf(e.getActionCommand())) {
	    case START_STOP:
		if (!server.isRunning()) {
		    ((JButton) e.getSource()).setText("stop Server");
		    server.start();
		} else {
		    ((JButton) e.getSource()).setText("start Server");
		    // server.stop();
		}
		break;
	    case IMAGES_SELECTION:
		Object selectedImage = ((JComboBox) e.getSource()).getModel().getSelectedItem();
		
		if(selectedImage == null || !server.isRunning()) {
		    return;
		}
		
		if(selectedImage instanceof ImageHeader) {
		    System.out.println("lala" + ((ImageHeader)selectedImage).height);
		    server.switchImage((ImageHeader)selectedImage);
		}
		
		break;
	    case MOVIES_SELECTION:
		Object selectedMovie = ((JComboBox) e.getSource()).getModel().getSelectedItem();
		
		if(selectedMovie == null || !server.isRunning()) {
		    return;
		}
		
		if(selectedMovie instanceof BLMHeader) {
		    System.out.println(((BLMHeader)selectedMovie).author);
		    server.switchMovie((BLMHeader)selectedMovie);
		}
		break;
	    }

	}

    }

}
