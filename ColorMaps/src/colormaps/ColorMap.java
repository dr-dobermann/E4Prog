package colormaps;


import java.util.*;
import javax.swing.*;

public class ColorMap {

	private ColorMaps cmap;
	

	
	public static void main(String[] args) {
		ColorMap cMap = new ColorMap();
		
		cMap.LoadData();
		
		//cMap.ConsoleTest();
		
		cMap.GraphTest();
	}
	
	public ColorMap() {
		cmap = new ColorMaps();
	}
	
	
	private void LoadData() {
		
		String states[] = {
							"WA", "ID", "MT", "ND", "MN", "WI",
							"MI", "NY", "VT", "ME", "OR", "NV",
							"UT", "WY", "SD", "IA", "IL", "OH",
							"PA", "NJ", "MA", "NH", "CA", "AZ",
							"CO", "NE", "MO", "IN", "WV", "DE",
							"CT", "RI", "NM", "KA", "AR", "VA",
							"MD", "OK", "LA", "MS", "TN", "NC",
							"DC", "TX", "AL", "GA", "SC", "FL",
						  };
		
		String borders[][] = {
								{"WA", "ID",},
								{"WA", "OR",},
								{"ID", "OR",},
								{"ID", "NV",},
								{"ID", "UT",},
								{"ID", "MT",},
								{"ID", "WY",},
								{"MT", "WY",},
								{"MT", "ND",},
								{"MT", "SD",},
								{"ND", "MN",},
								{"ND", "SD",},
								{"MN", "IA",},
								{"MN", "WI",},
								{"WI", "IA",},
								{"WI", "IL",},
								{"WI", "MI",},
								{"MI", "OH",},
								{"MI", "IN",},
								{"NY", "PA",},
								{"NY", "NJ",},
								{"NY", "CT",},
								{"NY", "MA",},
								{"NY", "VT",},
								{"VT", "MA",},
								{"VT", "NH",},
								{"ME", "NH",},
								{"OR", "CA",},
								{"OR", "NV",},
								{"NV", "CA",},
								{"NV", "AZ",},
								{"NV", "UT",},
								{"UT", "AZ",},
								{"UT", "CO",},
								{"UT", "WY",},
								{"WY", "CO",},
								{"WY", "NE",},
								{"WY", "SD",},
								{"SD", "NE",},
								{"SD", "IA",},
								{"IA", "NE",},
								{"IA", "MO",},
								{"IA", "IL",},
								{"IL", "IN",},
								{"OH", "IN",},
								{"OH", "KY",},
								{"OH", "WV",},
								{"OH", "PA",},
								{"PA", "WV",},
								{"PA", "MD",},
								{"PA", "DE",},
								{"PA", "NJ",},
								{"MA", "CT",},
								{"MA", "RI",},
								{"MA", "NH",},
								{"NM", "TX",},
								{"NM", "OK",},
								{"KA", "OK",},
								{"AR", "OK",},
								{"AR", "TX",},
								{"AR", "LA",},
								{"AR", "MS",},
								{"AR", "TN",},
								{"KY", "TN",},
								{"KY", "VA",},
								{"VA", "TN",},
								{"VA", "NC",},
								{"VA", "DC",},
								{"VA", "MD",},
								{"MD", "DC",},
								{"OK", "TX",},
								{"LA", "TX",},
								{"LA", "MS",},
								{"MS", "AL",},
								{"MS", "TN",},
								{"TN", "AL",},
								{"TN", "GA",},
								{"TN", "NC",},
								{"NC", "GA",},
								{"NC", "SC",},
								{"AL", "FL",},
								{"AL", "GA",},
								{"GA", "FL",},
								{"GA", "SC",},
							  };

		List<String[]> ld = new ArrayList<String[]>();
		
		for ( String[] str : borders )
			ld.add(str);
		
		cmap.LoadMesh(states, ld);
		
	}
	
	private void ConsoleTest() {
		
		System.out.println("States mesh\n"
				         + "=========================================================");
		for ( Node node : cmap.GetMesh().GetNodes() ) {
			System.out.println(node.GetName());
			for ( Link link : node.GetLinks() )
				System.out.println("    " + link.GetName());
		}
		
		cmap.SolveColorMaps();
		
		Map<String, ColorMaps.Color> scmap = cmap.GetColorMap();
		
		System.out.println("\n\nSolved color map followed:\n==================================================================");
		for ( String state : scmap.keySet() )
			System.out.printf(" Color for state %s is %s .\n", state, scmap.get(state).toString());
	}
	
	private void GraphTest() {
		cmap.SolveColorMaps();
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				CreateAndShowGUI(cmap);
			}
		});
		
	}
	
	private static void CreateAndShowGUI(ColorMaps cm) {
		CMWindow wnd = new CMWindow(cm);
		wnd.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		wnd.setVisible(true);
	}

}
