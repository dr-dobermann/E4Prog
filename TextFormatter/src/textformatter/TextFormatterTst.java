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

		testPrepareString();
	}
	
	private static void testPrepareString() {
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
	
}

// end of sentence look up
//String str = "This  is  a   test string  with some sentences in it ... Is this a last sentence? No!" +
//        "(There might be more.) How lond could it countinue??? What's going on?!!?!" +
//        "Could anybody expalain me what happened here?!!!";
//Matcher m = Pattern.compile("\\.\"\\)|\\?\"\\)|!\"\\)|" + 
//        "\\.\\)|\\?\\)|!\\)|" +
//        "(!+\\?+)+|(\\?+!+)+|" +
//        "\\.+|\\?+|!+").matcher(str);


// ?align command
//Matcher m = Pattern.compile("\\?align +(as_is|condenced|proportional)").matcher(str);