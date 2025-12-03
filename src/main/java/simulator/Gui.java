package simulator;

import java.awt.event.ActionListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;

public class Gui extends JFrame implements ActionListener{
	
	JTextArea output;
	JPanel topPanel, bottomPanel;
	JButton run, step, stop, open, options;
	JTextField textField;
	JFileChooser fc;
	File f;
	JFrame optionsMenu;
	JScrollPane mainPanel;
	
	IntegratedSimulationEngine ise;
	
	public Gui() {
		ise = new IntegratedSimulationEngine();
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
		
		output = new JTextArea();
		
		topPanel = new JPanel();
		bottomPanel = new JPanel();
		mainPanel = new JScrollPane(output, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 10));
		bottomPanel.add(run);
		bottomPanel.add(step);
		bottomPanel.add(stop);
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
		this.setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == run) {
			ise.main(null);
		}
		else if(e.getSource() == step) {
			ise.runOneCycle();
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
