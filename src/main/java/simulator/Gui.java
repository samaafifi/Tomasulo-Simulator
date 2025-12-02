package simulator;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JFileChooser;

public class Gui extends JFrame implements ActionListener{
	
	JLabel output;
	JPanel topPanel, mainPanel, bottomPanel, cachePanel, latencyPanel;
	JButton run, step, stop, open, options;
	JTextField textField;
	JFileChooser fc;
	File f;
	JFrame optionsMenu;
	
	double cacheHit, cacheLatency, cachePenalty, ALULatency, MULLatency, DIVLatency, LSLatency, BranchLatency; //to be moved?
	
	public Gui() {
		run = new JButton("Run");
		step = new JButton("Step");
		stop = new JButton("Stop");
		open = new JButton("Open");
		options = new JButton("Options");
		run.setFocusable(false);
		step.setFocusable(false);
		stop.setFocusable(false);
		open.setFocusable(false);
		options.setFocusable(false);
		//run.addActionListener(e -> System.out.print("running"));
		run.addActionListener(this);
		step.addActionListener(this);
		stop.addActionListener(this);
		open.addActionListener(this);
		options.addActionListener(this);
		
		textField = new JTextField(); //textField.getText()
		
		fc = new JFileChooser();
		
		output = new JLabel();
		
		topPanel = new JPanel();
		bottomPanel = new JPanel();
		mainPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 10));
		bottomPanel.add(run);
		bottomPanel.add(step);
		bottomPanel.add(stop);
		mainPanel.setBackground(Color.GRAY);
		mainPanel.add(output);
		topPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 7, 7));
		topPanel.add(open);
		topPanel.add(options);
		
		this.setTitle("Micro Project");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//this.setLayout(new BorderLayout()); .setBounds(x, y, w, h)
		this.add(topPanel, BorderLayout.NORTH);
		this.add(mainPanel, BorderLayout.CENTER);
		this.add(bottomPanel, BorderLayout.SOUTH);
		this.setSize(1200, 700);
		this.setLocationRelativeTo(null);
		//this.pack();
		this.setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == run) {
			//
		}
		else if(e.getSource() == step) {
			//
		}
		else if(e.getSource() == stop) {
			//
		}
		else if(e.getSource() == open) {
			if(fc.showOpenDialog(null) == 0) {
				f = new File(fc.getSelectedFile().getAbsolutePath());
			}
		}
		else {
			new Options();
		}
	}

}
