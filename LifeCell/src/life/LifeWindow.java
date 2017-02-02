package life;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class LifeWindow extends JFrame {

	private Flatland flatland;
	private LifeDisplay display;
	private JScrollPane scroller;
	private JButton nextGenBtn = new JButton("Next generation");

	public LifeWindow(Flatland flatland) {
		
		this.flatland = flatland;
		display = new LifeDisplay(flatland);
		scroller = new JScrollPane(display);
				
		nextGenBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				flatland.NextGeneration();
				display.ResetSize();
				scroller.repaint();
			}
		});

		setTitle("Flatland life cells view. Press Next generation button to continue");
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(scroller);
		add(nextGenBtn);
		pack();
	}
}