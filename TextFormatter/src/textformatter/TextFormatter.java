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
import textformatter.Decor.DeCmd;
import textformatter.Para.PAlign;
import textformatter.Para.ParagraphCommand;
import textformatter.ParaLine.TrimSide;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

import java.io.IOException;
import java.nio.file.*;


@Log
public class TextFormatter {

	private @Getter int height = 40;
	private @Getter int linesLeft = height;
	private boolean firstLineOnPage = true;
	private int PageNum = 1;
	private int emptyLinesCount = 0;
	private int nonEmptyLinescount = 0;

	private ParaSettings currPSet = new ParaSettings( 72,
													  1, 
													  new int[] { 1, 1}, 
													  new int[] { 0, 0},
													  0,
													  PAlign.PA_LEFT );
	private ParaSettings newPSet = null;
	private @Getter Para.ParagraphCommand pCmd = ParagraphCommand.PC_NONE;
	
  private static final String fNoteFmt = "%3d) "; // TODO: Set different formats for a footnote identification
  private static final int fNoteFmtLen = String.format( fNoteFmt, 1 ).length();
  private ParaSettings fnPSet = 
    new ParaSettings( 72,
                      1, 
                      new int[] {0,1}, 
                      new int[] {fNoteFmtLen, 0}, 
                      -fNoteFmtLen, 
                      PAlign.PA_FILL );
  private int fNoteID = 1;
	private int fnLines = 0;
	private ParaLine currFNote = new ParaLine( fnPSet.getWidth() );
  // footnotes on the bottom of the current and the next pages
  private ArrayList<ParaLine> footnotes = new ArrayList<>();
  private ArrayList<ParaLine> nextPageFNotes = new ArrayList<>(); 

	private @Getter int headerHeight = 0;
	private @Getter Para.PAlign headerAlign = Para.PAlign.PA_CENTER;
	private @Getter int headerLine;
	private ArrayList<ParaLine> header = new ArrayList<>();
	
	private String path = null;
	
	private Map<String, String> aliases = new HashMap<String, String>();

	final Pattern cmdName = Pattern.compile( "^\\?(\\b(size|align|par|margin|interval|feedlines|feed|newpage|left|header|pnum|pb|footnote|alias)\\b)?" );
		
	private @Getter boolean newPage = false;
	private @Getter int linesToFeed = 0;
		
	private @Getter ParaLine currLine = new ParaLine( 0 );
	
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
					.flatMap( l -> ProcessLine( l ) )
					.forEach( line -> System.out.println( line.toString() ) );
				
				  ArrayList<ParaLine> lines = new ArrayList<>();
				  
				  // finish all unfinished lines
				  // TODO: Control the line counter
				  if ( currLine.GetLength() > 0 ) {
				    if ( currPSet.getAlign() == PAlign.PA_AS_IS )
				      lines.add( currLine );
				    else
				      lines.addAll( PushLinesFromBuffer( currLine, currPSet, false, !firstLineOnPage ) );
				  }
				    
				  if ( currFNote.GetLength() > 0 || footnotes.size() > 0 ) {
				    if ( footnotes.size() == 0 )
				      footnotes.add( GetDividerLine( fnPSet, '-') );
				    
				    footnotes.addAll( PushLinesFromBuffer( currFNote, fnPSet, false, true) );
				    
				    lines.addAll( footnotes );
				  }
				    
	          lines.forEach( line -> System.out.println( line.toString() ) );

			} catch ( IOException ex ) {
				ex.printStackTrace();
			}
		}		
	}
	
	/**
	 * Processes lines:
	 * 	- creates full sentences where it's suitable
	 * 	- filling footnotes
	 * 	- align strings according to settings align and current page width
	 * 
	 * @param pl -- incoming line
	 * 
	 * @return stream of formatted strings. Might be empty 
	 */
	Stream<ParaLine> ProcessLine( ParaLine pl ) {
		
		ArrayList<ParaLine> lines = new ArrayList<>();

    // close page if needed
    if ( newPage || linesLeft <= 0 ) {
      
      lines.addAll( ClosePage( pl, true ) );
      
      return lines.stream();
    }
		
		//remove empty lines if alignment isn't set to PA_AS_IS
		if ( pl.GetLength() == 0 ) {
		  
			if ( currPSet.getAlign() != Para.PAlign.PA_AS_IS ) {
			  			  
				if ( ++emptyLinesCount > 1 ) {  // if there is more than 1 empty lines in a row, close the paragraph
					pCmd = ParagraphCommand.PC_END;
					newPSet = currPSet.Copy();
				}
				else  {						 // if there is first empty line, close current sentence
					if ( currLine.GetLength() > 0 ) {
		
						lines.addAll( PushLinesFromBuffer( currLine,
						                                   currPSet,
						                                   true,
						                                   nonEmptyLinescount == 0 ) );
					}
				}

        nonEmptyLinescount = 0;

				return lines.stream();
			}
			else { 
					if ( linesLeft-- > 0 )
						lines.add( pl );
					
					firstLineOnPage = false;
					
					return lines.stream();
				}
		}
		else 
			emptyLinesCount = 0;
				
		//check if there is open footnote exists
		if ( fnLines > 0 ) {
			
			AddFootnoteLine( pl );
			
			if ( --fnLines == 0 )
				CloseFootnote();
			
			return lines.stream();
		}
        
		// process paragraph break state
		if ( pCmd == ParagraphCommand.PC_END ) {
						
			if ( currLine.GetLength() > 0 && linesLeft > 0 )
				lines.addAll( PushLinesFromBuffer( currLine, currPSet, true, nonEmptyLinescount == 0 ) );
				     
      // add spaces after the current paragraph
      if ( !firstLineOnPage &&
          currPSet.getAlign() != Para.PAlign.PA_AS_IS)
        lines.addAll( AddEmptyLines( currPSet.getSpaces()[1]) );
	    pCmd = ParagraphCommand.PC_NONE;
	    
	    // feed empty lines if needed
	    if ( linesToFeed > 0 && !firstLineOnPage )
	      lines.addAll( AddEmptyLines( linesToFeed ) );
	    
      // addition spaces before a new paragraph
      // shouldn't be made before the first line on the page
      if ( !firstLineOnPage &&
           currPSet.getAlign() != Para.PAlign.PA_AS_IS ) {
        
        lines.addAll( AddEmptyLines( currPSet.getSpaces()[0] ) );
      }
	    
			firstLineOnPage = false;
			currPSet = newPSet;
			nonEmptyLinescount = 0;
			currPSet = newPSet;
		}
					
		// process usual line
		if ( currPSet.getAlign() != Para.PAlign.PA_AS_IS ) {
			
			pl.Trim( ParaLine.TrimSide.TS_LEFT );
			
			currLine.AddPline( pl );
			
			// push all full lines from the buffer until the buffer length
			// would be less than the current paragraph width
			while ( --linesLeft > 0 && 
			        currLine.GetLength() > currPSet.GetTextWidht( nonEmptyLinescount == 0 ) ) {
				
			  lines.add( currLine.CutFormattedLine( currPSet.getAlign(), 
			                                        currPSet.GetTextWidht( nonEmptyLinescount++ == 0 ) ) );
				currLine.Trim( TrimSide.TS_LEFT );
			}
			
		}
		else { 
		  if ( linesLeft-- > 0 )
		    lines.add( pl );
		  
		  return lines.stream();
		}
		
    firstLineOnPage = false;
		
		return lines.stream();
	}

	/**
	 * Closes current page and starts a new one if needed
	 * 
	 * @param pl           -- current ParaLine from the lines stream
	 * @param startNewPage -- starts a new page if true
	 * 
	 * @return a lines array with lines finished current page and 
	 *         a new one if created
	 */
	private ArrayList<ParaLine> ClosePage( ParaLine pl, boolean startNewPage ) {

	  ArrayList<ParaLine> lines = new ArrayList<>();

    // flush footnotes onto the page
    lines.addAll( footnotes );
    footnotes.clear();
    if ( nextPageFNotes.size() > 0 ) {
      footnotes.addAll( nextPageFNotes );
      nextPageFNotes.clear();
    }
      
    // add page break decoration on the last line on page
    if ( lines.size() == 0 )
      lines.add( ParaLine.PrepareString( "" ) );
      
    ParaLine pel = lines.get( lines.size() - 1 );
      
    pel.InsertDecor( DeCmd.DCS_PAGE_END, pel.GetLength(), PageNum );
    pel.InsertDecor( DeCmd.DCE_PAGE_END, pel.GetLength(), null );
      
    // flush page header if it exists  
    if ( startNewPage && headerHeight > 0 ) {
      lines.addAll( header );
      
      // make header for next page
      SetHeader();
    }
    else
      GetPageNum();
      
    // reset a free lines counter 
    linesLeft = height - headerHeight;
    newPage = false;
    firstLineOnPage = true;
    nonEmptyLinescount = 0;
    emptyLinesCount = 0;
    
    if ( currPSet.getAlign() != PAlign.PA_AS_IS ) {
        
      if ( pCmd == ParagraphCommand.PC_END )
        currPSet = newPSet;
        
      pCmd = ParagraphCommand.PC_NONE;
  
      if ( pl.GetLength() > 0 ) {
          
        currLine.AddPline( pl );
  
        // push all full lines from the buffer until the buffer length
        // would be less than the current paragraph width
        while ( linesLeft > 0 && 
                currLine.GetLength() > currPSet.GetTextWidht( nonEmptyLinescount == 0 ) ) {
            
        lines.add( currLine.CutFormattedLine( currPSet.getAlign(), 
                                              currPSet.GetTextWidht( nonEmptyLinescount++ == 0 ) ) );
        currLine.Trim( TrimSide.TS_LEFT );
        linesLeft--;
        firstLineOnPage = false;
          }
        }
    }
    else {
      if ( pl != null ) {
        lines.add( pl );
        linesLeft--;
      }
    }
	  
    return lines;
  }

  /**
	 * Pushes given buffer into list of lines
	 * 
	 * @param buff
	 * @param ps
	 * @param reduceFreeLinesCounter
	 * @param withFirstLine
	 * @return
	 */
	private ArrayList<ParaLine> 
	  PushLinesFromBuffer( ParaLine buff,
	                       ParaSettings ps,
		                     boolean reduceFreeLinesCounter, 
		                     boolean withFirstLine ) {

		
		ArrayList<ParaLine> newLines = new ArrayList<>();
		
		// put first line if need be
		if ( buff.GetLength() > 0 &&
		     withFirstLine && 
			   ( !reduceFreeLinesCounter || linesLeft > 0 ) )
			newLines.add( buff.CutFormattedLine( ps.getAlign(), 
						 			 			 ps.GetTextWidht( withFirstLine ) ) );
		
		// put the rest of lines
		if ( buff.GetLength() > 0 && 
		     ( !reduceFreeLinesCounter || linesLeft  > 0 ) )
			newLines.addAll( buff.Split( ps.getAlign(),
					                     ps.GetTextWidht( false ), 
					                     reduceFreeLinesCounter ? linesLeft : -1 ) );
		
		
		AddMarginsTo( newLines, ps, withFirstLine );
		
		if ( reduceFreeLinesCounter )
			linesLeft -= newLines.size();
		
		return newLines;
	}

	/**
	 * Adds margins around the lines
	 * 
	 * @param lines     -- lines to process
	 * @param ps	    -- paragraph settings
	 * @param withFirst -- use indent for first line in an array
	 * 
	 */
	private void AddMarginsTo( ArrayList<ParaLine> lines, ParaSettings ps, boolean withFirst ) {
		
		for ( int l = 0; l < lines.size(); l++ ) {
			lines.get( l ).Pad( PAlign.PA_LEFT, 
		                      ' ', 
					                ps.getMargins()[0] + 
					            	( withFirst && l == 0 ? ps.getIndent() : 0 ) );
			if ( l < lines.size() - 1 ) 
			lines.get( l ).Pad( PAlign.PA_RIGHT, 
					                ' ', 
					                ps.getMargins()[1] );
		}
	}

	/**
	 * Adds empty lines to the page
	 * 
	 * @param i -- empty lines count
	 */
	private ArrayList<ParaLine> AddEmptyLines( int i ) {

		ArrayList<ParaLine>spacer = new ArrayList<ParaLine>();
		
		while ( linesLeft-- >= 1 && i-- >= 1 )
			spacer.add( ParaLine.PrepareString( "" ) );
		
		return spacer;
	}

	/**
	 * Returns current footnote ID and increments it afterwards
	 * 
	 * @return current footnote ID
	 */
	public int GetFnoteID () {
		
		return fNoteID++;
	}

	/**
	 * Adds a line to the current footnote.
	 * Every line will be cut and aligned according
	 * to the footnote width.
	 * 
	 * @param pl
	 */
	private void AddFootnoteLine( ParaLine pl ) {
		
		if ( currFNote.GetLength() == 0 )
			currFNote = new ParaLine( String.format( fNoteFmt, fNoteID ) );

		currFNote.AddPline( ParaLine.PrepareString( " " ) );
		
		pl.Trim( TrimSide.TS_LEFT );
	
		currFNote.AddPline( pl );
	}

	/**
	 * Closes current footnote
	 * 
	 * Flushes the current footnote buffer into a page footnote buffer
	 */
	private void CloseFootnote() {

		Footnote fnote = new Footnote( GetFnoteID() );
		
		fnote.getLines().addAll( PushLinesFromBuffer( currFNote, 
		                                              fnPSet, 
		                                              false, 
		                                              true) );
		
		// add footnote decoration to the last text line
		currLine.InsertDecor( Decor.DeCmd.DCS_FNOTE, currLine.GetLength(), fnote );
		currLine.AddPline( ParaLine.PrepareString( String.format( " %d)", fnote.getNoteID() ) ) );
		currLine.InsertDecor( DeCmd.DCE_FNOTE, currLine.GetLength(), null );
		log.info( currLine.toString() );
		
		// add footnote lines to the current page
		// if it's the first footnote, the separator should be added first
		if ( footnotes.size() == 0 && linesLeft >= 3 ) {  // one line for the line with footnote link
														  // one or zero line for the footnote header
													      // one line for footnote itself
			footnotes.add( GetDividerLine( fnPSet, '-') );
			linesLeft--;
		}
		
		// if there are not enough lines to fit a line with footnote link and footnote itself
		// new page should be started
		if ( linesLeft < 2 ) 
			newPage = true;
		else
			for ( ParaLine pl : fnote.getLines() )
			  if ( linesLeft > 1 ) { // one free line should left on the page to 
			                         // put the line with footnote link
			    // add to the page footnote as many lines as possible
			    footnotes.add( pl );
			    if ( linesLeft > 1 )
			      linesLeft--;
			  }
			  else
			    nextPageFNotes.add( pl );
	}

	/**
	 * Creates and returns a ParaLine formed as a line of char ch
	 * @param ps  -- ParaSettings for width of the 
	 * @param ch  -- Character to fill the line
	 * 
	 * @return line of character ch of width ps.
	 */
	private ParaLine GetDividerLine( ParaSettings ps, char ch ) {
	  
    ParaLine pl = new ParaLine( ps.getWidth() );
    
    pl.Pad( PAlign.PA_LEFT, ch, ps.getWidth() );
    
    return pl;
  }

  /**
	 * Replaces all aliases to their original values
	 * 
	 * @param str  -- string to process 
	 * 
	 * @return string with replaced aliases
	 */
	private String ReplaceAliases( String str ) {
		
		for ( String alias : aliases.keySet() ) {
			str = str.replaceAll( alias, aliases.get( alias ) );
		}
		
		return str;
	}

	/**
	 * Adds new alias into aliases table or clears all aliases
	 * 
	 * @param newName -- new alias for oldName. 
	 *                   if newName is empty, 
	 *                      then all aliases will be deleted
	 *                   if there is an alias with the same name already exists, 
	 *                      the error is logged and a new alias is ignored
	 *                   if there is alias for oldName already exists,
	 *                      the old one removed and new one saved
	 *                       
	 * @param oldName -- old name for the alias
	 */
	public void SetAlias( String newName, String oldName ) {
		
		if ( newName.length() == 0 ) {
			
			aliases.clear();
			log.info( "Clear all aliases" );
			
			return;
		}
		
		if ( aliases.containsKey( newName ) ) {
		
			log.warning( String.format( "Alias [%s] already exists!!!", newName ) );
			return;
		}
		
		for ( String key : aliases.keySet() )
			if ( aliases.get( key ) == oldName ) {
				
				log.info( String.format( "Alias [%s] for [%s] will be replaced for [%s]",
						                 key, oldName, newName ) );
				aliases.remove( key );
			}
		

		if ( newName != oldName ) {

			aliases.put( newName, oldName );
			log.info( String.format( "New alias [%s] for [%s]", newName, oldName ) );
		}
		else 
			log.info( "Alias is equivalent to the value. Ignored." );
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
		
		log.info( String.format( "Got command [%s]", cmd ) ); // TODO: Change log level to config
			
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
  			
  			newPSet = currPSet.Copy();
			
  			if ( h != height ||
  			     ( w != currPSet.getWidth() && w > fnPSet.getWidth() ) ) {
  					
  				height = h;
  				newPSet.setWidth( w );
  				
  				newPage = true;
  			}
  			else 
  				if ( w < fnPSet.getWidth() )
  					newPSet.setWidth( w );
  			
  			pCmd = ParagraphCommand.PC_END;
  			
  			break;
			
  		case "align" : // align settings for the next paragraph
  			params = GetCmdParams( str, "align", new Class[] { String.class } );
  
  			newPSet = currPSet.Copy();
  			
  			try {
  				newPSet.setAlign( Para.PAlign.valueOf( "PA_" + params.get( 0 ).toUpperCase() ) );
  			}
  			catch ( IllegalArgumentException ex )
  			{
  				log.severe( String.format( "Invalid align [%s]!!!", params.get( 0 ) ) );
  				return null;
  			}
  			
  			pCmd = ParagraphCommand.PC_END;
  			
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
  
  			newPSet = currPSet.Copy();
  			
  			newPSet.setIndent( ind );
  			newPSet.setSpaces( new int[] { sB, sA } );
  			
  			pCmd = ParagraphCommand.PC_END;
  			
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
  			
  			if ( mL < 0 || mR < 0 || mL + mR > currPSet.getWidth() ) {
  				log.severe( String.format( "Invalid margins mL[%d], mR[%d]", mL, mR ) );
  				return null;
  			}
  			
  			newPSet = currPSet.Copy();
  			newPSet.setMargins( new int[] { mL, mR } );
  			
  			pCmd = ParagraphCommand.PC_END;
  			
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
  			
  			newPSet = currPSet.Copy();
  			newPSet.setInterval( intr );
  			
  			pCmd = ParagraphCommand.PC_END;
  			
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
  
  			linesToFeed = lines * currPSet.getInterval() ;
  
  			pCmd = ParagraphCommand.PC_END;
  			
  			break;
  			
  		case "feed" :   // feed lines with no interval
  		 
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
  
  			pCmd = ParagraphCommand.PC_END;
  			
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
  			newPage = true;
  
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
  			pCmd = ParagraphCommand.PC_END;
  			
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
  			if ( params.size() == 0 ) {
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
				
				pl = new ParaLine( fnPSet.getWidth() );
				pl.AddString( sNum, new Decor[0] );
				switch ( headerAlign ) {
					case PA_LEFT :
						pl.Pad( Para.PAlign.PA_RIGHT, ' ', fnPSet.getWidth() - pl.GetLength() );
						break;
						
					case PA_RIGHT :
						pl.Pad( Para.PAlign.PA_LEFT, ' ', fnPSet.getWidth() - pl.GetLength() );
						break;
						
					default : 
						pl.Pad( Para.PAlign.PA_CENTER, ' ', fnPSet.getWidth() - pl.GetLength() );
						break;
				}
				
			}
			else
				// add a double dashed string as the last line 
				// of the header if there is space for it
				if ( l == headerHeight - 1 && 
				     headerHeight > 1 && 
				     headerLine != headerHeight ) {
					
					pl = new ParaLine( fnPSet.getWidth() );
					pl.Pad( Para.PAlign.PA_LEFT, '=', fnPSet.getWidth() );
				}
				else {
					pl = new ParaLine( fnPSet.getWidth() );
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
	private ArrayList<String> GetCmdParams( String str, String cmd, Class<?>[] pTypes ) {
		
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
			
			log.info( String.format( "Got parameters %s for command [%s].", params, cmd ) );
			
			return params;
		}
		
		Matcher m = Pattern.compile( pSrchStr.toString() ).matcher( str );
		
		if ( !m.find() ) {
			log.severe( String.format( "Could not find command [%s] in the string \"%s\"",
									  cmd, str ) );
			return null;
		}
		
		for ( String s : str.substring( m.end() ).split( "\\W+") )
			if ( s.length() > 0 )
				params.add( s );
		
		log.info( String.format( "Got parameters %s for command [%s].", params, cmd ) );
		
		return params;
	}	
}

