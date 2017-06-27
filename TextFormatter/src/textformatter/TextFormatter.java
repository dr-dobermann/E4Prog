/********************************************************
* Project : TextFormatter
* 
* Filename: TextFormatter.java
* Package : textformatter
* 
* Author: dr.Dobermann (c) 2017
********************************************************/
package textformatter;



import lombok.*;
import lombok.extern.java.Log;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;
import java.io.IOException;
import java.nio.file.*;


@Log
public class TextFormatter {

	private @Getter int width = 72;
	private @Getter int fnWidth = width;
	private @Getter int height = 40;
	private @Getter int linesLeft = height;
	private @Getter Para.PAlign align = Para.PAlign.PA_LEFT;
	
	private @Getter int interval = 1;
	private @Getter int[] spaces = new int[] {0, 0};
	private @Getter int[] margins = new int[] {0, 0};
	private @Getter int indent = 3;
	
	private int NoteID = 1;
	private int PageNum = 1;
	
	private int emptyLinesCount = 0;

	private @Getter int headerHeight = 0;
	private @Getter Para.PAlign headerAlign = Para.PAlign.PA_CENTER;
	private @Getter int headerLine;
	private List<ParaLine> header = null;
	
	private String path = null;
	
	private Map<String, String> aliases = new HashMap<String, String>();

	final Pattern cmdName = Pattern.compile( "^\\?(\\b(size|align|par|margin|interval|feedlines|feed|newpage|left|header|pnum|pb|footnote|alias)\\b)?" );
	

	
	private @Getter boolean newPage = false;
	private @Getter int linesToFeed = 0;
	private @Getter int fnLines = 0;
	private @Getter boolean closeParagraph = false;
	
	private @Getter ParaLine currLine = null;
	
	private static TextFormatter stateKeeper = null;
	
	public static TextFormatter GetTextFormatter () {
		
		if ( stateKeeper == null )
			stateKeeper = new TextFormatter();
		
		return stateKeeper;
	}
	
	private TextFormatter() {}
	
	public void LoadDocument( String path ) {
		

		if ( !path.equals( this.path ) && 
			 !path.isEmpty() ) {
			
			this.path = path;

			try {
			
				Files.lines( Paths.get( this.path ) )
					.sequential()
					.map( l -> ReplaceAliases( l ) )
					.map( l -> ProcessCmd( l ) )
					.filter( l -> l != null )
					.map( l -> ParaLine.PrepareString( l ) )
					.flatMap( l -> FormatString( l ) )
					.forEach( line -> System.out.println( line.toString() ) );
			} catch ( IOException ex ) {
				ex.printStackTrace();
			}
		}		
	}
	
	/**
	 * Formats strings:
	 * 	- creates full sentences where it's suitable
	 * 	- filling footnotes
	 * 	- align strings according to settings align and current page width
	 * 
	 * @param pl -- incoming line
	 * 
	 * @return stream of formatted strings. Migth be empty 
	 */
	Stream<ParaLine> FormatString( ParaLine pl ) {
		
		ArrayList<ParaLine> lines = new ArrayList<>();
		
		lines.add( pl );
		
		return lines.stream();
	}
	
	/**
	 * Returns current footnote ID and increments it afterwards
	 * 
	 * @return current footnote ID
	 */
	public int GetFnoteID () {
		
		return NoteID++;
	}


	/**
	 * Replaces all aliases to their original values
	 * 
	 * @param str  -- string to process 
	 * 
	 * @return string with replaced aliases
	 */
	private String ReplaceAliases( String str ) {
		
		for ( String alias : aliases.keySet() )
			str.replaceAll( alias, aliases.get( alias ) );
		
		return str;
	}

	/**
	 * Adds new alias into aliases table or clears all aliases
	 * 
	 * @param newName -- new alias for oldName. If newName is empty, 
	 *                   then all aliases will be deleted 
	 * @param oldName -- old name for the alias
	 */
	public void SetAlias( String newName, String oldName ) {
		
		if ( newName.length() == 0 ) {
			aliases.clear();
			log.warning( "Clear all aliases" );
			
			return;
		}
		
		log.warning( String.format( "New alias [%s] for [%s]", newName, oldName ) );
		aliases.put( newName, oldName );
	}
	
	/**
	 * Check the string and if it consists of a command, process the command
	 * and return null instead the string
	 * 
	 * @param str -- String to process
	 * 
	 * @return null if there is command in the string or string itself if there
	 *         is no command in the string
	 */
	public String ProcessCmd( String str ) {
		
		Matcher m = cmdName.matcher( str );
		String cmd = null;
		
		ArrayList<String> params;
		
		int lines;
		
		if ( !m.find() )
			return str;
		
		cmd = m.group( 1 );
		
		log.warning( String.format( "Got command [%s]", cmd ) ); // TODO: Change log level to config
			
		switch ( cmd ) {
		
		case "size" :
			params = GetCmdParams( str, "size", new Class[] { Integer.class, Integer.class } );
			int w, h;
			
			try {
				w = Integer.parseInt( params.get( 0 ) );
				h = Integer.parseInt( params.get( 1 ) );
			}
			catch ( NumberFormatException e ) {
				log.severe( "Invalid parameters for size command." );
				return null;
			}
			
			if ( h <= 0 || w <= 10 ) {
				log.severe( String.format( "Invalid page size W x H [%d][%d]",
								           w, h ) );
				return null;
			}
				
			if ( h != height ||
				 ( w != width && w > fnWidth ) ) {
					
				height = h;
				width = w;
				
				newPage = true;
			}
			else 
				if ( w < fnWidth )
					width = w;
			
			closeParagraph = true;
			
			break;
			
		case "align" : // align settings for the next paragraph
			params = GetCmdParams( str, "align", new Class[] { String.class } );

			try {
				
				align = Para.PAlign.valueOf( "PA_" + params.get( 0 ).toUpperCase() );
			}
			catch ( IllegalArgumentException ex )
			{
				log.severe( String.format( "Invalid align [%s]!!!", params.get( 0 ) ) );
				return null;
			}
			
			closeParagraph = true;
			
			break;
			
		case "par" :
			params = GetCmdParams( str, "par", new Class[] { Integer.class, Integer.class, Integer.class } );
			int ind, sB, sA;
			
			try {
				ind = Integer.parseInt( params.get( 0 ) );
				sB  = Integer.parseInt( params.get( 1 ) );
				sA  = Integer.parseInt( params.get( 2 ) );
			}
			catch ( NumberFormatException e ) {
				 log.severe( "Invalid parameter for PAR command! " );
				 return null;
			}

			if ( sB < 0 || sA < 0 ) {
				log.severe( String.format( "Invalid paragraphge settings -- indent:[%d], sB:[%d], sA:[%d]",
							     ind, sB, sA ) );
				return null;
			}
			
			indent = ind;
			spaces[0] = sB;
			spaces[1] = sA;
			
			closeParagraph = true;
			
			break;
			
		case "margin" :
			params = GetCmdParams( str, "margin", new Class[] { Integer.class, Integer.class } );
			int mL, mR;
			try {
				mL = Integer.parseInt( params.get( 0 ) );
				mR = Integer.parseInt( params.get( 1 ) );
			}
			catch ( NumberFormatException e ) {
				log.severe( "Casting error for margin command!!!" );
				return null;
			}
			
			if ( mL < 0 || mR < 0 || mL + mR > width ) {
				log.severe( String.format( "Invalid margins mL[%d], mR[%d]", mL, mR ) );
				return null;
			}
			margins[0] = mL;
			margins[1] = mR;
			
			closeParagraph = true;
			
			break;
			
		case "interval" :
			params = GetCmdParams( str, "interval", new Class[] { Integer.class } );
			int intr;
			try {
				intr = Integer.parseInt( params.get( 0 ) );
			}
			catch ( NumberFormatException e ) {
				log.severe( "Casting error for interval command!!!");
				return null;
			}
			
			if ( intr < 1 || intr > 10 ) {	// TODO: Add constant for max interval
				log.severe( String.format( "Invalid interval value [%d]", intr ) );
				return null;
			}
			interval = intr;
			
			closeParagraph = true;
			
			break;
			
		case "feedlines" :  // feed lines into the end of the last paragraph
						    // with intervals
			params = GetCmdParams( str, "feedlines", new Class[] { Integer.class } );
			try {
				lines = Integer.parseInt( params.get( 0 ) );					
			}
			catch ( NumberFormatException e ) {
				log.severe( "Casting error for feedlines command!!!");
				return null;
			}

			if ( lines < 1 ) {
				log.severe( "Invalid number of lines for feedlines!!!" );
				return null;
			}	

			linesToFeed = lines * interval;

			closeParagraph = true;
			
			break;
			
		case "feed" :
			params = GetCmdParams( str, "feed", new Class[] { Integer.class } );
			try {
				lines = Integer.parseInt( params.get( 0 ) );
			}
			catch ( NumberFormatException e ) {
				log.severe( "Casting error for feed command!!!" );
				return null;
			}

			if ( lines < 1 ) {
				log.severe( "Invalid number of lines for feed!!!" );
				return null;
			}
			
			linesToFeed = lines;

			closeParagraph = true;
			
			break;
			
		case "newpage" :			
			newPage = true;	

			break;
			
		case "left" :
			params = GetCmdParams( str, "left", new Class[] { Integer.class } );
			try {
				lines = Integer.parseInt( params.get( 0 ) );
			}
			catch ( NumberFormatException e ) {
				log.severe( "Casting error for left command!!!" );
				return null;
			}

			if ( lines < 1 ) {
				log.severe( "Invalid number of lines for left command!!!" );
				return null;
			}
			
			if ( linesLeft <= lines )
				newPage = true;

			break;
			
		case "header" :
			params = GetCmdParams( str, "header", new Class[] { Integer.class, Integer.class, String.class } );
			int hHeight, hLine;
			try {
				hHeight = Integer.parseInt( params.get( 0 ) );
				hLine   = Integer.parseInt( params.get( 1 ) );
			}
			catch ( NumberFormatException e ) {
				log.severe( "Casting error for header command!!!");
				return null;
			}
			
			if ( hHeight < 0 || 
				 hHeight > height || 
				 hLine > hHeight ||
				 hHeight > 10 ) {	// TODO: Add max header height constant
				
				log.severe( String.format( "Invalid header height [%d]", hHeight ) );
				return null;
			}
			try {
				headerAlign = Para.PAlign.valueOf( "PA_" + params.get( 2 ).toUpperCase() );
			}
			catch ( IllegalArgumentException ex ) {
				log.severe( String.format( "Invalid page header align [%s]!", params.get( 2 ) ) );
				return null;
			}
			
			headerHeight = hHeight;
			headerLine = hHeight <= hLine ? hHeight - 1 : hLine;
		
			SetHeader();

			break;
			
		case "pnum" :
			params = GetCmdParams( str, "pnum", new Class[] { Integer.class } );
			int pNum;
			try {
				pNum = Integer.parseInt( params.get( 0 ) );
			}
			catch ( NumberFormatException e ) {
				log.severe( "Casting error for pnum command!!!" );
				return null;
			}
			if ( pNum < 1 ) {
				log.severe( String.format( "Invalid page number [%d]", pNum ) );
				return null;
			}
			
			PageNum = pNum;
			SetHeader();
			
			break;
			
		case "pb" :			
			closeParagraph = true;
			
			break;
			
		case "footnote" :
			params = GetCmdParams( str, "footnote", new Class[] { Integer.class } );
			int fnNum;
			try {
				fnNum = Integer.parseInt( params.get( 0 ) );
			}
			catch ( NumberFormatException e ) {
				log.severe( "Casting error for footnote command!!!" );
				return null;
			}

			if ( fnNum < 1 ) {
				log.severe( String.format( "Invalid footnote lines number [%d]", fnNum ) );
				return null;
			}
			
			fnLines = fnNum;

			break;
			
		case "alias" :
			String sNew, sOld;
			
			params = GetCmdParams( str, "alias", new Class[0] );
			if ( params == null ) {
				SetAlias( "", "" );
				break;
			}
			else
				if ( params.size() == 1 || params.get( 0 ).length() == 0 )
					sOld = " ";
				else
					sOld = params.get( 1 );
			
				sNew = params.get( 0 );
			
			SetAlias( sNew, sOld );
			
			break;				
		}
			
			
		return null; 
	}

	/**
	 * Re-creates a page header if headerHeight is not zero
	 */
	private void SetHeader() {
		
		header.clear();

		if ( headerHeight == 0 )
			return;
		
		final String sNum = String.format( "- %d -", GetPageNum() );
		
		ParaLine pl;
		
		for ( int l = 0; l < headerHeight; l++ ) {
			
			if ( l == headerLine - 1 ) {
				
				pl = new ParaLine( width );
				pl.AddString( sNum, new Decor[0] );
				switch ( align ) {
					case PA_LEFT :
						pl.Pad( Para.PAlign.PA_RIGHT, ' ', width - pl.GetLength() );
						break;
						
					case PA_RIGHT :
						pl.Pad( Para.PAlign.PA_LEFT, ' ', width - pl.GetLength() );
						break;
						
					default : 
						pl.Pad( Para.PAlign.PA_CENTER, ' ', width - pl.GetLength() );
						break;
				}
				
			}
			else
				// add a double dashed string as the last line 
				// of the header if there is space for it
				if ( l == headerHeight - 1 && 
				     headerHeight > 1 && 
				     headerLine != headerHeight ) {
					
					pl = new ParaLine( width );
					pl.Pad( Para.PAlign.PA_LEFT, '=', width );
				}
				else {
					pl = new ParaLine( width );
					pl.AddString( "  ", new Decor[0] );
				}
	
			header.add( pl );
		}
	}


	/**
	 * Returns current page number and increment it afterwards
	 * 
	 * @return current page number
	 */
	private int GetPageNum() {
		
		return PageNum++;
	}

	/**
	 * Looking for command parameters in the string
	 * 
	 * @param str		-- checked string
	 * @param cmd		-- command name for the error messages
	 * @param pTypes    -- array with parameter types.
	 *                     if it's empty then as many parameters will be returned as they are
	 * 
	 * @return an string list with parameters.
	 * 
	 * @throws TFException
	 */
	private ArrayList<String> GetCmdParams( String str, String cmd, Class[] pTypes ) {
		
		StringBuilder pSrchStr = new StringBuilder( "^\\?(\\b" ).append(cmd).append( "\\b){1}" );
		ArrayList<String> params = new ArrayList<>();
		
		if ( pTypes.length > 0 ) {
			for ( int p = 0; p < pTypes.length; p++ )
				if ( pTypes[p] == Integer.class )
					pSrchStr.append( " +(\\d+)" );
				else
					pSrchStr.append( " +(\\w+)" );
			
			Matcher m = Pattern.compile( pSrchStr.toString() ).matcher( str );
			
			if ( !m.find() ) {
				log.severe( String.format( "Could not find command with parameters for command [%s] in the string \"%s\"",
										  cmd, str ) );
				return null;
			}
	
					
			for ( int i = 2; i <= m.groupCount(); i++ )
				params.add( m.group( i ) );
			
			log.config( String.format( "Got params %s for command [%s].", params, cmd ) ); // TODO: Change severity level to config
			
			return params;
		}
		
		Matcher m = Pattern.compile( pSrchStr.toString() ).matcher( str );
		
		if ( !m.find() ) {
			log.severe( String.format( "Could not find command with parameters for command [%s] in the string \"%s\"",
									  cmd, str ) );
			return null;
		}
		
		
		return params;
	}	
}


/*
import lombok.*;

/*

import java.io.*;
import java.util.*;
import java.util.regex.*;


*//**
 * Formats given string list according to 
 * inner commands
 * 
 * @author dr.Dobermann
 *
 *//*
public class TextFormatter 
	implements TFExcDataLoad {
	
	private @Getter int width = 72;
	private @Getter int height = 40;
	
	private @Getter Para.PAlign align = Para.PAlign.PA_LEFT;
	private @Getter boolean getFullSentence = true;
	private @Getter int interval = 1;
	private @Getter int[] spaces = new int[] {0, 0};
	private @Getter int[] margins = new int[] {0, 0};
	private @Getter int indent = 3;
	
	private int lastNoteID = 1;
	private int lastPageNum = 1;
	
	private SentenceReader reader;
	private int emptyLinesCount = 0;

	private @Getter int headerHeight = 0;
	private @Getter Para.PAlign headerAlign = Para.PAlign.PA_CENTER;
	private @Getter int headerLine;
	
	private List<Page> pages = new ArrayList<Page>();
	
	public List<String> fnotes = new ArrayList<String>();
	private int fnLines = -1;
	private SentenceReader.SEReason fnLastReadCode;
	private SentenceReader.SEReason prevStrStatus;
	
	// Constructors
	//-------------------------------------------------------------------------
	
	public TextFormatter() {
		
	}
	
	// Functionality
	//-------------------------------------------------------------------------
	
	*//** Returns latest footnote ID
	 * 
	 * @return latest footnote ID
	 *//*
	public int GetLastNoteID() {
		
		return lastNoteID++;
	}

	*//**
	 * Returns the current page number
	 * 
	 * @return current page number
	 *//*
	public int GetLastPageNum() {
		
		return lastPageNum++;
	}
	
	*//**
	 * Loads document from an input stream
	 * 
	 * @param input  -- input stream
	 * 
	 * @throws TFException
	 *//*
	public void LoadDocument(BufferedReader input) 
		throws TFException,
			   TFInvalidParameterCount {
		
		String tmp = "";
		
		if ( input != null )
			reader = new SentenceReader(input);
		
		if ( pages.size() > 0 )
			pages.clear();
		
		pages.add(new Page(this));
		
		prevStrStatus = reader.getSeReason();
		String str = reader.GetRawSentence(getFullSentence);
		
		while ( str != null ) {
			
			if ( fnLines > 0 ) { // all commands and empty lines 
				                 // in a footnote are ignored
				if ( str.length() > 0 ) {
					fnotes.add(str);
					fnLines--;
					
					if ( fnLines == 0 ) { // after fetching of the all lines of
						                  // the footnote, prepare footnote
						                  // linked string and add it on the page
						// insert footnote decoration into the string
						str = tmp;
						str += String.format("&F+{%d}%d&F-", lastNoteID, lastNoteID);
						
						if ( fnLastReadCode == SentenceReader.SEReason.SER_CMD )
							tmp = reader.GetNonEmptySentence(getFullSentence);
							if ( tmp != null )
								str += tmp;	
					}
					continue;	                
				}
			}
			else
				str = CheckAndExecCmd(str);
			
			// if there was footnote command
			// save the string for the future and start 
			// fetch strings of footnote
			if ( fnLines > 0 && fnLastReadCode == SentenceReader.SEReason.SER_CMD ) {
				tmp = str; // 
				continue;
			}
			
			
			if ( str.length() == 0 && ++emptyLinesCount == 2)
				if ( align != Para.PAlign.PA_AS_IS )
					GetLastPage().AddNewPara();
				else
					GetLastPage().AddString(ParaLine.PrepareString(str));
			
			GetLastPage().AddString(ParaLine.PrepareString(str));

			// if there is non-empty buffer of footnote strings,
			// upload it as a footnote linked to the last paragraphs line 
			if ( fnotes.size() > 0 ) {
				GetLastPage().AddFootnote(fnotes.toArray(new String[0]), GetLastNoteID());
				fnotes.clear();
			}
			
			prevStrStatus = reader.getSeReason();
			str = reader.GetRawSentence(getFullSentence);
		}
	}
	
	*//**
	 * Returns the last page of the document
	 * 
	 * @return Last page if there is any page
	 * 
	 * @throws TFException when the pages list is empty
	 *//*
	private Page GetLastPage()
		throws TFException {
		
		if ( pages.size() == 0 )
			throw new
				TFException( getID(), "There is no pages in TextFormatter!!!");
		
		return pages.get(pages.size() - 1);
	}
	
	*//**
	 * Adds a new page into the document
	 * 
	 * @return newly added page
	 *//*
	public Page AddPage() 
		throws TFException {
		
		Page oldPage = GetLastPage(),
			 newPage = new Page(this);

		pages.add(newPage);
		
		oldPage.Close(newPage);
		
		return newPage;
	}
	
	*//**
	 * Checks if there is controlling command in the begin of the line.
	 * If the command exists, it cut and processed
	 * 
	 * @param str  -- string to check
	 * 
	 * @return string without controlling command
	 * 
	 * @throws TFException
	 *//*
	private String CheckAndExecCmd(String str) 
		throws TFException,
			   TFInvalidParameterCount {
		
		final Pattern cmdName = Pattern.compile( "^\\?(\\w+)" );
		
		Matcher m;
		
		if ( !str.matches( "^\\?\\w+.*$" ) ) 
			return str;
		
		m = cmdName.matcher( str );
		if ( !m.find() )
			throw new
				TFException( getID(), 
						     "[CheckAndExecCmd] Could not find command in string [%s]!!!", 
						     str );
		
		String cmd = m.group( 1 );
		String[] params;
		int lines;
		
		switch ( cmd ) {
			case "size" :
				params = GetCmdParams( str, "\\?size +(\\d+) +(\\d+)", "size", 2, false );
				int w, h;
				try {
					w = Integer.parseInt( params[1] );
					h = Integer.parseInt( params[2] );
				}
				catch ( NumberFormatException e ) {
					throw new
						TFException( getID(),
								     "Casting error for size command!!!");
				}
				
					if ( h <= 0 || w <= 0 )
						throw new
							TFException( getID(), 
									     "[CheckAndExecCmd] Invalid page size W x H [%d][%d]",
									     w, h);
					
					width = w;
					if ( h != height || 
						 w > GetLastPage().getWidth() ) {
						
						height = h;
						AddPage();
					}
					else 
						GetLastPage().SetWidth( w );

					str = params[0];	
				
				break;
				
			case "align" : // align settings for the next paragraph
				params = GetCmdParams(str, "\\?align +(\\w+)", "align", 1, false );
				
				switch ( params[1] ) {
					case "as_is" :
						align = Para.PAlign.PA_AS_IS;
						getFullSentence = false;
						break;
					case "left" :
						align = Para.PAlign.PA_LEFT;
						getFullSentence = true;
						break;
					case "center" :
						align = Para.PAlign.PA_CENTER;
						getFullSentence = true;
						break;
					case "right" :
						align = Para.PAlign.PA_RIGHT;
						getFullSentence = true;
						break;
					case "fill" :
						align = Para.PAlign.PA_FILL;
						getFullSentence = true;
						break;
					default :
						throw new
							TFException( getID(),
										 "[CheckAndExecCmd] Invalid align parameter [%s]!!!",
										 params[1] );
					}
				
				GetLastPage().SetAlign( align );
				
				str = params[0];	
				
				break;
				
			case "par" :
				params = GetCmdParams( str, "\\?par +(\\d+) +(\\d+) +(\\d+)", "par", 3, false );
				int ind, sB, sA;
				
				try {
					ind = Integer.parseInt( params[1] );
					sB  = Integer.parseInt( params[2] );
					sA  = Integer.parseInt( params[3] );
				}
				catch ( NumberFormatException e ) {
					throw new
					TFException( getID(),
							     "Casting error for par command!!!");
				}

				if ( sB < 0 || sA < 0 )
					throw new
						TFException( getID(), 
								     "[CheckAndExecCmd] Invalid paragraphge settings -- indent:[%d], sB:[%d], sA:[%d]",
								     ind, sB, sA );
				indent = ind;
				spaces[0] = sB;
				spaces[1] = sA;
				
				GetLastPage().SetParaSettings( indent, spaces );
					
				str = params[0];	

				break;
				
			case "margin" :
				params = GetCmdParams( str, "\\?margin +(\\d+) +(\\d+)", "margin", 2, false );
				int mL, mR;
				try {
					mL = Integer.parseInt( params[1] );
					mR = Integer.parseInt( params[2] );
				}
				catch ( NumberFormatException e ) {
					throw new
					TFException( getID(),
							     "Casting error for margin command!!!");
				}
				
				if ( mL < 0 || mR < 0 || mL + mR > width )
					throw new
						TFException( getID(), 
								     "[CheckAndExecCmd] Invalid margins mL[%d], mR[%d]",
								     mL, mR );
				margins[0] = mL;
				margins[1] = mR;
				
				GetLastPage().SetMargins( margins );

				str = params[0];	

				break;
				
			case "interval" :
				params = GetCmdParams( str, "\\?interval +(\\d+)", "interval", 1, false );
				int intr;
				try {
					intr = Integer.parseInt( params[1] );
				}
				catch ( NumberFormatException e ) {
					throw new
					TFException( getID(),
							     "Casting error for interval command!!!");
				}
				
				if ( intr < 1 || intr > height )
					throw new
						TFException( getID(), 
								     "[CheckAndExecCmd] Invalid interval value [%d]",
								     intr );
				interval = intr;
				
				GetLastPage().SetInterval( interval );

				str = params[0];	

				break;
				
			case "feedlines" :  // feed lines into the end of the last paragraph
							    // with intervals
				params = GetCmdParams( str, "\\?feedlines +(\\d+)", "feedlines", 1, false );
				try {
					lines = Integer.parseInt(params[1]);					
				}
				catch ( NumberFormatException e ) {
					throw new
					TFException( getID(),
							     "Casting error for feedlines command!!!");
				}

				if ( lines < 1 )
					throw new
						TFException( getID(),
								     "[CheckAndExecCmd] Invalid number of lines for feedlines!!!" );
				GetLastPage().FeedLines( lines, true );
				
				str = params[0];	

				break;
				
			case "feed" :
				params = GetCmdParams( str, "\\?feed +(\\d+)", "feed", 1, false );
				try {
					lines = Integer.parseInt(params[1]);
				}
				catch ( NumberFormatException e ) {
					throw new
					TFException( getID(),
							     "Casting error for feed command!!!");
				}

				if ( lines < 1 )
					throw new
						TFException( getID(),
								     "[CheckAndExecCmd] Invalid number of lines for feed!!!" );
				GetLastPage().FeedLines( lines, false );
				
				str = params[0];	

				break;
				
			case "newpage" :
				AddPage();
				str = str.substring( m.end() );	

				break;
				
			case "left" :
				params = GetCmdParams( str, "\\?left +(\\d+)", "left", 1, false );
				try {
					lines = Integer.parseInt(params[1]);
				}
				catch ( NumberFormatException e ) {
					throw new
					TFException( getID(),
							     "Casting error for left command!!!");
				}

				if ( lines < 1 )
					throw new
						TFException( getID(),
								     "[CheckAndExecCmd] Invalid number of lines for left command!!!" );
				
				if ( GetLastPage().getLinesLeft() <= lines )
					AddPage();
				
				str = params[0];	

				break;
				
			case "header" :
				params = GetCmdParams( str, "\\?header +(\\d+) +(\\d+) +(\\w+)", "header", 3, false );
				int hHeight, hLine;
				try {
					hHeight = Integer.parseInt( params[1] );
					hLine   = Integer.parseInt( params[2] );
				}
				catch ( NumberFormatException e ) {
					throw new
					TFException( getID(),
							     "Casting error for header command!!!");
				}
				
				if ( hHeight < 0 || hHeight > height || hLine > hHeight )
					throw new
						TFException( getID(), 
								     "[CheckAndExecCmd] Invalid header height [%d]",
								     hHeight );
				switch ( params[3] ) {
					case "left" : 
						headerAlign = Para.PAlign.PA_LEFT;
						break;
						
					case "right" :
						headerAlign = Para.PAlign.PA_RIGHT;
						break;
						
					case "center" :
						headerAlign = Para.PAlign.PA_CENTER;
						break;
						
					default : 
						throw new
						TFException( getID(), 
								     "[CheckAndExecCmd] Invalid header alignment value [%s]",
								     params[3] );
				}
				
				headerHeight = hHeight;
				headerLine = hHeight <= hLine ? hHeight - 1 : hLine;
			
				GetLastPage().SetHeader();

				str = params[0];	

				break;
				
			case "pnum" :
				params = GetCmdParams( str, "\\?pnum +(\\d+)", "pnum", 1, false );
				int pNum;
				try {
					pNum = Integer.parseInt(params[1]);
				}
				catch ( NumberFormatException e ) {
					throw new
					TFException( getID(),
							     "Casting error for pnum command!!!");
				}
				if ( pNum < 1 )
					throw new
						TFException( getID(),
								     "[CheckAndExecCmd] Invalid page number [%d]",
								     pNum );
				
				lastPageNum = pNum;
				GetLastPage().SetPageNum( pNum );
				
				str = params[0];	

				break;
				
			case "pb" :
				GetLastPage().AddNewPara();
				
				str = str.substring( m.end() );	

				break;
				
			case "footnote" :
				params = GetCmdParams( str, "\\?footnote +(\\d+)", "footnote", 1, false );
				int fnNum;
				try {
					fnNum = Integer.parseInt(params[1]);
				}
				catch ( NumberFormatException e ) {
					throw new
					TFException( getID(),
							     "Casting error for footnote command!!!");
				}
				
				fnLines = fnNum;
				fnLastReadCode = prevStrStatus;
				
				str = params[0];	

				break;
				
			case "alias" :
				String sNew, sOld;
				
				params = GetCmdParams( str, "\\?alias +(\\w+) *(\\w*) *$", "footnote", 2, true );
				if ( params.length == 1 ) {
					reader.SetAlias( "", "" );
					str = str.substring( m.end() );
					break;
				}
				else
					if ( params.length == 2 || params[2].length() == 0 )
						sOld = " ";
					else
						sOld = params[2];
				
					sNew = params[1];
				
				reader.SetAlias( sNew, sOld );
					
				str = params[0];
				
				break;				
		}
		
		return str;
	}
	
	*//**
	 * Looking for command parameters in the string
	 * 
	 * @param str		-- checked string
	 * @param searchStr -- the pattern for the command. It should include command and leading ? sign.
	 * 					   Every parameter in the pattern should be enclosed in the () pair 
	 * @param cmd		-- command name for the error messages
	 * @param paraNum	-- number of parameters
	 * 
	 * @return an string array with parameters. First element is the rest of the source string
	 *         without the command and it's parameters
	 * 
	 * @throws TFException
	 *//*
	private String[] GetCmdParams(String str, String searchStr, String cmd, int paraNum, boolean ignorePNum) 
		throws TFException,
	           TFInvalidParameterCount {
		
		List<String> params = new ArrayList<String>();
		
		Matcher m = Pattern.compile( searchStr ).matcher( str );
		
		if ( !m.find() )
			throw new
				TFInvalidParameterCount(
						String.format( "TEXTFORMATTER: [GetCmdParams] Could not find parameters for command [%s] in the string \"%s\"",
									  cmd, str )
						);
		
		if ( !ignorePNum && m.groupCount() != paraNum + 1 )
			throw new
				TFException(getID(), 
							"[GetCmdParams] Invalid parameters count for command [%s]!!!\n Found %d instead of %d.",
							cmd, m.groupCount() - 1, paraNum);
		
		params.add(str.substring(m.end()));
		
		for ( int i = 1; i <= m.groupCount(); i++ )
			params.add(m.group(i));
		
		
		return params.toArray(new String[0]);
	}
	
	 
	 * @see textformatter.TFExcDataLoad#getID()
	 
	@Override
	public String getID() {
		
		return "TEXTFORMATTER: ";
	}

}
//-----------------------------------------------------------------------------

*//**
 * This class reads enclosed sentences from the input stream
 * 
 * @author dr.Dobermann
 *
 *//*
class SentenceReader 
	implements TFExcDataLoad {
	
	private @Getter BufferedReader input;
	private Map<String, String> aliases = new HashMap<String, String>();
	private StringBuilder uSen = new StringBuilder();
	private boolean emptyLine = false;

	// sentence end reason
	enum SEReason {
					SER_UNKNOWN,
					SER_PUNCT,
			  		SER_CMD,
			  		SER_EMPTY_LINE,
			  		SER_EOL,
			  		SER_STREAM_END
	};
  		
  	private @Getter SEReason seReason = SEReason.SER_UNKNOWN;
  		
	// Constructors
	//-------------------------------------------------------------------------
	public SentenceReader ( BufferedReader input ) {
		
		this.input = input;	
	}
	
		
	// Functionality
	//-------------------------------------------------------------------------
		
	*//**
	 * Returns a raw sentence without controlling commands but with 
	 * decoration commands in it
	 * 
	 * @return raw sentence with decoration commands unprocessed
	 * 
	 * @throws TFException
	 *//*
	public String GetRawSentence( boolean getFullSentence ) 
		throws TFException {

		final Pattern cmdName = Pattern.compile( "^\\?(\\w+)" );
		final Pattern sentenceEnd = Pattern.compile( "\"\\?\"\\.(?!,)\\W*|" +					  // "?, ".  it excludes strings as ".,"	
				                                     "\\.\"\\)\\W*|\\?\"\\)\\W*|!\"\\)\\W*|" +    // ."), ?"), !") at the end of sentence 
												     "\\.\\)\\W*|\\?\\)\\W*|!\\)\\W*|" +          // .),  ?)   !)
												     "(!+\\?+)+\\W*|(\\?+!+)+\\W*|" +             // !?,  ?!
													 "\\.+(?!,)\\W*|\\?+\\W*|!+\\W*" );           // ., ..., ?, !
		Matcher m;
		
		if ( seReason == SEReason.SER_STREAM_END )
			return null;
		
		seReason = SEReason.SER_UNKNOWN;
		
		// if there is empty string were read before, 
		// return it
		if ( emptyLine ) {
			emptyLine = false;
			seReason = SEReason.SER_EMPTY_LINE;
			return "";
		}

		StringBuilder sb = new StringBuilder( "" );

		// if there is no need to look for a formal end of the sentence but just for the end of the line
		if ( !getFullSentence && uSen.length() > 0 ) {
			sb.append( uSen );
			uSen.delete( 0, uSen.length() );
			
			return sb.toString();
		}
		
		String str = ReadLine();
		
		while ( str != null ) {
			
			// if empty string is read and unfinished string is not empty,
			// return unfinished sentence and fire emptyLine flag.
			// if unfinished sentence is empty, return it
			if ( str.trim().length() == 0 ) {
				
				if ( uSen.length() > 0 ) {
					sb.append( uSen );
					uSen.delete( 0, uSen.length() );
					emptyLine = true;
					seReason = SEReason.SER_EMPTY_LINE;
				}
				
				break;
			}
			
			if ( getFullSentence ) {
				
				// Check on sentence end or command start.
				// If sentence end is occurred, return substring with it and
				// previous buffer. Rest of the string save as unfinished sentence
				m = sentenceEnd.matcher( str );
				if ( m.find() ) {
					if ( uSen.length() > 0 ) {
						sb.append( uSen );
						uSen.delete( 0, uSen.length() );
					}
					sb.append( str.substring( 0, m.end() ) );
					uSen.append( str.substring( m.end() ) );
					seReason = SEReason.SER_PUNCT;
					
					break;
				}
				
				// If command starts, and unfinished sentence is not empty, return it
				m = cmdName.matcher(str);
				if ( m.find() ) {
					if ( uSen.length() > 0 ) {
						sb.append(uSen);
						uSen.delete(0, uSen.length());
						uSen.append(str);
						seReason = SEReason.SER_CMD;
						break;
					}	
				}
				
				// append the str to an unfinished sentence
				// and read next line;
					uSen.append(str);
					str = ReadLine();
			}
			else {
					sb.append(str);
					seReason = SEReason.SER_EOL;
					break;
				}
		}
		
		if ( str == null ) {
			if ( uSen.length() > 0 ) {
				sb.append(uSen);
				uSen.delete(0, uSen.length());
			}

			seReason = SEReason.SER_STREAM_END;
			return null;
		}
		
		return sb.toString();
	}
	
	*//**
	 * Returns only non-empty sentence or 
	 * null in case of the end of the input stream is reached
	 * 
	 * @return non-empty sentence or null
	 * 
	 * @throws TFException
	 *//*
	public String GetNonEmptySentence(boolean getFullSentence) 
		throws TFException {
		
		String str;
		
		str = GetRawSentence(getFullSentence);
		
		while ( str != null && str.length() == 0 )
			str = GetRawSentence(getFullSentence);
		
		return str;
		
	}
	
	*//**
	 * Reads new line from the input stream
	 * 
	 * @return String readed or null if there is an end of 
	 *         input stream reached 
	 *//*
	private String ReadLine() {
		
		String str;
		try {
			str = input.readLine();
			if ( str != null && str.length() > 0 && aliases.size() > 0 )
				str = ReplaceAliases(str);
		}
		catch ( IOException e ) {
			throw new RuntimeException(e.getMessage());
		}

		return str;
	}
	
	*//**
	 * Replaces all aliases to their original values
	 * 
	 * @param str  -- string to process 
	 * 
	 * @return string with replaced aliases
	 *//*
	private String ReplaceAliases( String str ) {
		
		for ( String alias : aliases.keySet() )
			str.replaceAll(alias, aliases.get(alias));
		
		return str;
	}
	

	*//**
	 * Adds new alias into aliases table or clears all aliases
	 * 
	 * @param newName -- new alias for oldName. If newName is empty, 
	 *                   then all aliases will be deleted 
	 * @param oldName -- old name for the alias
	 *//*
	public void SetAlias(String newName, String oldName) {
		
		if ( newName.length() == 0 ) {
			aliases.clear();
			return;
		}
		
		aliases.put(newName, oldName);
	}
	

	 (non-Javadoc)
	 * @see textformatter.TFExcDataLoad#getID()
	 
	@Override
	public String getID() {
		
		return "SENTENCE_READER: ";
	}

}
//-----------------------------------------------------------------------------



*//**
 * TextFormatter exception
 * @author dr.Dobermann
 *
 *//*
class TFException extends Exception {
	
	*//**
	 * 
	 *//*
	private static final long serialVersionUID = 1L;

	public TFException(String id, String str) {
		super(id + str);
	}

	public TFException(String id, String str, Object ... args) {
		super(String.format(id + str, args));
	}
}
//-----------------------------------------------------------------------------

class TFInvalidParameterCount extends Exception {
	
	*//**
	 * 
	 *//*
	private static final long serialVersionUID = 1L;

	public TFInvalidParameterCount(String str) {
		super(str);
	}
}

*//**
 * Interface for object ID generation
 *  
 * @author dr.Dobermann
 *//*
interface TFExcDataLoad {
	public String getID(); 
}*/