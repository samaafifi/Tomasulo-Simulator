package simulator;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPanel;

public class Options extends JFrame implements ActionListener{
	
	JPanel cachePanel, latencyPanel, submitPanel;
	JButton cb, lb;
	JTextField cht, clt, cpt, lalut, lmult, ldivt, lbart, llst;
	
	double cacheHit, cacheLatency, cachePenalty, ALULatency, MULLatency, DIVLatency, LSLatency, BranchLatency;
	
	public Options() {
		cb = new JButton("Submit cache");
		lb = new JButton("Submit latency");
		cb.setFocusable(false);
		lb.setFocusable(false);
		cb.addActionListener(this);
		lb.addActionListener(this);
		
		cht = new JTextField("Cache Hit");
		clt = new JTextField("Cache Latency");
		cpt = new JTextField("Cache Penalty");
		
		lalut = new JTextField("ALU Latency");
		lmult = new JTextField("MUL Latency");
		ldivt = new JTextField("DIV Latency");
		lbart = new JTextField("BEQ Latency");
		llst = new JTextField("LD/SD Latency");
		
		cachePanel = new JPanel();
		latencyPanel = new JPanel();
		submitPanel = new JPanel();
		submitPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 100, 5));
		submitPanel.add(lb);
		submitPanel.add(cb);
		latencyPanel.setLayout(new GridLayout(3, 2));
		latencyPanel.add(lalut);
		latencyPanel.add(lmult);
		latencyPanel.add(ldivt);
		latencyPanel.add(lbart);
		latencyPanel.add(llst);
		cachePanel.setLayout(new GridLayout(2, 2));
		cachePanel.add(cht);
		cachePanel.add(clt);
		cachePanel.add(cpt);
		
		this.setTitle("Options");
		this.setSize(700, 500);
		this.setLocationRelativeTo(null);
		this.add(latencyPanel, BorderLayout.WEST);
		this.add(cachePanel, BorderLayout.EAST);
		this.add(submitPanel, BorderLayout.SOUTH);
		//this.setLayout(new FlowLayout(FlowLayout.CENTER));
		this.pack();
		this.setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == cb) {
			try {
				cacheHit = Double.parseDouble(cht.getSelectedText());
	        } catch (NumberFormatException er) {}
			try {
				cacheLatency = Double.parseDouble(clt.getSelectedText());
	        } catch (NumberFormatException er) {}
			try {
				cachePenalty = Double.parseDouble(cpt.getSelectedText());
	        } catch (NumberFormatException er) {}
			//
		}
		else {
			try {
				ALULatency = Double.parseDouble(lalut.getSelectedText());
	        } catch (NumberFormatException er) {}
			try {
				MULLatency = Double.parseDouble(lmult.getSelectedText());
	        } catch (NumberFormatException er) {}
			try {
				DIVLatency = Double.parseDouble(ldivt.getSelectedText());
	        } catch (NumberFormatException er) {}
			try {
				LSLatency = Double.parseDouble(llst.getSelectedText());
	        } catch (NumberFormatException er) {}
			try {
				BranchLatency = Double.parseDouble(lbart.getSelectedText());
	        } catch (NumberFormatException er) {}
			//
		}
	}

}
