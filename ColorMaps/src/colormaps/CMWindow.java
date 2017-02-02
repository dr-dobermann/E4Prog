package colormaps;

import java.awt.*;
import javax.swing.*;

public class CMWindow extends JFrame {

	private ColorMaps cm;
	private GrpColorMaps CMView;
	private JScrollPane scroller;

	public CMWindow(ColorMaps cm) {
		
		this.cm = cm;
		CMView = new GrpColorMaps(cm);
		scroller = new JScrollPane(CMView);
				
		setTitle("ColorMaps calculation");
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(scroller);
		pack();
	}
}
