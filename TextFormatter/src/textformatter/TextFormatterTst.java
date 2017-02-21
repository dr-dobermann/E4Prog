/********************************************************
* Project : TextFormatter
* 
* Filename: TextFormatterTst.java
* Package : textformatter
* 
* Author: dr.Dobermann (c) 2017
********************************************************/
package textformatter;

import java.util.regex.*;

import textformatter.Para.PAlign;

import java.io.*;

/**
 * Test class for the project
 * 
 * @author dr.Dobermann
 */
public class TextFormatterTst {

	/**
	 * Tests it all
	 * @param args
	 */
	public static void main(String[] args) {

		// TestPrepareString();
		
		// ReaderTest();
		
		TestPara();
		
		HeaderTest();
		
	}

	private static void TestRegExp() {
		String str = "?align condencedThis  is  a   test &B+string  with&B- some sentences in it ... Is this a last sentence? No!" +
                "(There might be more.) How long could it countinue??? What's &U+going&U- on?!!?!" +
                "Could anybody expalain&F+{test} me&F- what happened here?!!!";
	
		//System.out.println("[" + str.matches("\\.?.+") + "]");
	
		Matcher m = Pattern.compile("\\&(\\w?)([\\+|-]?)(\\{(\\w*)\\})?+").matcher(str);
		while ( m.find() ) {
		
			//System.out.println(m.groupCount());
			//System.out.println(m.start(0));
			//System.out.println(m.end(0));
			//System.out.println(m.toString());
			System.out.println(str.substring(m.start(), m.end()) + " ");
			
			for ( int i = 1; i <= m.groupCount(); i++ )
				System.out.printf("Group #%d is %s at %d\n", i, m.group(i), m.start(i));
		}
	}
	
	private static void TestPrepareString() {
		try {
			DecoratedStr dStr = ParaLine.PrepareString("This is a &U+test&U- string for &B+PrepareString&B- testing!&F+{1}1&F-");
			
			System.out.printf("DecoratedStr.str is \n[%s]\n ", dStr.str);
			for ( int l = 0; l < dStr.str.length(); l++ )
				System.out.print(l%10);
			for ( Decor dp : dStr.dpl )
				System.out.printf("\nDecoration [%s] found at [%d] with data [%s]", 
								  dp.getCmd().toString(), 
								  dp.getPos(),
								  dp.getData() == null ? "null" : dp.getData().toString());
		}
		catch ( TFException e) {
			System.out.println("Exception fired: " + e.getMessage());
		}
		
	}
	
	private static void TestPara() {
		
		String p1[] = new String[] {
				 "Today's dairy.",
			   };
		String p2[] = new String[] {
				 "Today's morning started not as usual. The alarm sounded as always at 6am but I didn't get up and just asked my wife to open the door and walk our dog out.",
				 "After her return we continued to sleep and got up late almost at 9am. I went to my work room and tried to make usual morning exercise but not succeeded with them.",
				 "Every day I do 3 series of 30 push-ups. But today I could only make 20 in my first try. I thought it's possible not to do morning exercises today. I meditated my everyday's 10 minutes1",
				 "and went to the shower. After the shower I felt better and did another 3 series of push-up of 20, 20 and 30 push-up accordingly.", 
				};
		String n2[] = new String[] {
				 "I use an Android App \"Calm\" for meditation"	
				};
		String p3[] = new String[] {
				 "Then my wife made a breakfast and invited us to share it. After a breakfast I went on upper floor to yesterday's DOTa DAC games between Onyx vs Complexity and NP vs DC. I hoped Onyx", 
				 "could win but they lost. DC should win and they did.", 
				};
		String p4[] = new String[] {
				 "Today I bought an exercise band kit to make an extended work-out. Then I helped my wife to make the Mike's room.", 
					};
		String p5[] = new String[] {
				 "In afternoon we went out with Mike. I threw him his puller many times. After we notices he was starting weary, I went to the car and my wife made exercises with him.", 
				 "Then we returned back. Mike had his lunch, so did we.",  
				};
		try {
			Para P1 = new Para( null, 60, Para.PAlign.PA_RIGHT, 1, 0, new int[] {0,1}, new int[] {0, 0} );
			P1.AddString( ParaLine.PrepareString( p1[0] ) );
			
			Para P2 = new Para( null, 60, Para.PAlign.PA_FILL, 1, 5, new int[] {0,1}, new int[] {5, 0} );
			P2.AddString( ParaLine.PrepareString( p2[0] ) );
			P2.AddString( ParaLine.PrepareString( p2[1] ) );
			
			ParaLine pl = new ParaLine( P2, p2[2].length() );
			pl.AddString( p2[2], new Decor[0] );
			pl.InsertDecor(Decor.DeCmd.DCS_FNOTE, pl.GetLength() - 1, 1);
			pl.InsertDecor(Decor.DeCmd.DCE_FNOTE, pl.GetLength(), null);
			P2.AddString( pl.GetDecoratedStr() );
			P2.AddFootnote( new DecoratedStr[] { ParaLine.PrepareString( n2[0] ) }, 1);
			P2.AddString( ParaLine.PrepareString( p2[3] ) );
			
			Para P3 = new Para( null, 50, Para.PAlign.PA_CENTER, 1, 5, new int[] {1,3}, new int[] {10, 10} );
			P3.AddString( ParaLine.PrepareString( p3[0] ) );
			P3.AddString( ParaLine.PrepareString( p3[1] ) );
			
			Para P4 = new Para( null, 50, Para.PAlign.PA_RIGHT, 1, 0, new int[] {0,0}, new int[] {0, 0} );
			P4.AddString( ParaLine.PrepareString( p4[0] ) );
			
			Para P5 = new Para( null, 50, Para.PAlign.PA_FILL, 1, -5, new int[] {0,0}, new int[] {10, 0} );
			P5.AddString( ParaLine.PrepareString( p5[0] ) );
			P5.AddString( ParaLine.PrepareString( p5[1] ) );
			
			Para[] ps = new Para[] {P1, P2, P3, P4, P5, };
			
			for ( int p = 0; p < ps.length; p++ ) {
				ps[p].Format();
				System.out.println( ps[p].toString() );	
			}
		}
		catch ( TFException e ) {
			System.out.println( "Something went wrong!\n" + e.getMessage() );
		}

	}
	
	private static void ReaderTest() {
		
		BufferedReader reader;
		SentenceReader sReader;
		
		int count = 10;
		
		try { 
			reader = new BufferedReader(new 
										FileReader(new 
												File("C:\\wrk\\development\\java\\Etudes4Programmers\\TextFormatter\\src\\textformatter\\task.description.txt")));
		}
		catch ( FileNotFoundException e ) {
			System.out.println("File not found!!!" + e.getMessage());
			return;
		}
		
		try {
			sReader = new SentenceReader(reader);
			
			String str = sReader.GetRawSentence(true);
			
			
			while ( str != null ) {
				System.out.println(str);
				//System.out.println("");
				
				str = sReader.GetRawSentence( count-- > 0 ? true : false );
			}
		}
		catch ( TFException e ) {
			System.out.printf("!Textformatter exception!\n%s.", e.getMessage());
			return;
		}
		finally {
			
			try {
				reader.close();
			}
			catch ( IOException e ) {
				System.out.println("Could not close the stream!" + e.getMessage());
			}
			
		}
	}

	private static void HeaderTest() {
		
		try { 
			Page pg = new Page(null);
		
			System.out.println( pg.getHeader().toString() );
			
			pg.SetPageNum(15);

			System.out.println( pg.getHeader().toString() );
		}
		catch ( TFException e ) {
			System.out.printf("Something went wrong!\n %s" , e.getMessage() );
		}
	}
}
