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

import java.io.*;
import java.util.*;
import java.util.regex.*;


/**
 * Formats given string list according to 
 * inner commands
 * 
 * @author dr.Dobermann
 *
 */
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
	
	/** Returns latest footnote ID
	 * 
	 * @return latest footnote ID
	 */
	public int GetLastNoteID() {
		
		return lastNoteID++;
	}

	/**
	 * Returns the current page number
	 * 
	 * @return current page number
	 */
	public int GetLastPageNum() {
		
		return lastPageNum++;
	}
	
	/**
	 * Loads document from an input stream
	 * 
	 * @param input  -- input stream
	 * 
	 * @throws TFException
	 */
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
	
	/**
	 * Returns the last page of the document
	 * 
	 * @return Last page if there is any page
	 * 
	 * @throws TFException when the pages list is empty
	 */
	private Page GetLastPage()
		throws TFException {
		
		if ( pages.size() == 0 )
			throw new
				TFException( getID(), "There is no pages in TextFormatter!!!");
		
		return pages.get(pages.size() - 1);
	}
	
	/**
	 * Adds a new page into the document
	 * 
	 * @return newly added page
	 */
	public Page AddPage() 
		throws TFException {
		
		Page newPage = new Page(this);
		
		GetLastPage().Close(newPage);
		
		pages.add(newPage);
		
		return newPage;
	}
	
	/**
	 * Checks if there is controlling command in the begin of the line.
	 * If the command exists, it cut and processed
	 * 
	 * @param str  -- string to check
	 * 
	 * @return string without controlling command
	 * 
	 * @throws TFException
	 */
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
									     "[CheckAndExecCmd] Invalid page size [%d][%d]",
									     w, h);
					
					if ( h < height ) {
						height = h;
						AddPage();
					}
					
					width = w;
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
				GetLastPage().FeedLines(lines, true);
				
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
				GetLastPage().FeedLines(lines, false);
				
				str = params[0];	

				break;
				
			case "newpage" :
				AddPage();
				str = str.substring(m.end());	

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
				GetLastPage().SetPageNum(pNum);
				
				str = params[0];	

				break;
				
			case "pb" :
				GetLastPage().AddNewPara();
				str = str.substring(m.end());	

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
	
	/**
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
	 */
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
	
	/* 
	 * @see textformatter.TFExcDataLoad#getID()
	 */
	@Override
	public String getID() {
		
		return "TEXTFORMATTER: ";
	}

}
//-----------------------------------------------------------------------------

/**
 * This class reads enclosed sentences from the input stream
 * 
 * @author dr.Dobermann
 *
 */
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
		
	/**
	 * Returns a raw sentence without controlling commands but with 
	 * decoration commands in it
	 * 
	 * @return raw sentence with decoration commands unprocessed
	 * 
	 * @throws TFException
	 */
	public String GetRawSentence(boolean getFullSentence) 
		throws TFException {

		final Pattern cmdName = Pattern.compile("^\\?(\\w+)");
		final Pattern sentenceEnd = Pattern.compile("\"\\?\"\\.|" +						   // "?".	
				                                    "\\.\"\\)\\W|\\?\"\\)\\W|!\"\\)\\W|" + // ."), ?"), !") at the end of sentence 
												    "\\.\\)\\W|\\?\\)\\W|!\\)\\W|" +       // .),  ?)   !)
												    "(!+\\?+)+\\W|(\\?+!+)+\\W|" +         // !?, ?!
													"\\.+\\W|\\?+\\W|!+\\W");              // ., ..., ?, !
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

		StringBuilder sb = new StringBuilder("");

		// if there is no need to look for a formal end of the sentence but just for the end of the line
		if ( !getFullSentence && uSen.length() > 0 ) {
			sb.append(uSen);
			uSen.delete(0, uSen.length());
			
			return sb.toString();
		}
		
		String str = ReadLine();
		
		while ( str != null ) {
			// if empty string is read and unfinished string is not empty,
			// return unfinished sentence and fire emptyLine flag.
			// if unfinished sentence is empty, return it
			if ( str.trim().length() == 0 ) {
				if ( uSen.length() > 0 ) {
					sb.append(uSen);
					uSen.delete(0, uSen.length());
					emptyLine = true;
					seReason = SEReason.SER_EMPTY_LINE;
				}
				
				break;
			}
			
			if ( getFullSentence ) {
				// Check on sentence end or command start.
				// If sentence end is occurred, return substring with it and
				// previous buffer. Rest of the string save as unfinished sentence
				m = sentenceEnd.matcher(str);
				if ( m.find() ) {
					if ( uSen.length() > 0 ) {
						sb.append(uSen);
						uSen.delete(0, uSen.length());
					}
					sb.append(str.substring(0, m.end()));
					uSen.append(str.substring(m.end()));
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
	
	/**
	 * Returns only non-empty sentence or 
	 * null in case of the end of the input stream is reached
	 * 
	 * @return non-empty sentence or null
	 * 
	 * @throws TFException
	 */
	public String GetNonEmptySentence(boolean getFullSentence) 
		throws TFException {
		
		String str;
		
		str = GetRawSentence(getFullSentence);
		
		while ( str != null && str.length() == 0 )
			str = GetRawSentence(getFullSentence);
		
		return str;
		
	}
	
	/**
	 * Reads new line from the input stream
	 * 
	 * @return String readed or null if there is an end of 
	 *         input stream reached 
	 */
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
	
	/**
	 * Replaces all aliases to their original values
	 * 
	 * @param str  -- string to process 
	 * 
	 * @return string with replaced aliases
	 */
	private String ReplaceAliases( String str ) {
		
		for ( String alias : aliases.keySet() )
			str.replaceAll(alias, aliases.get(alias));
		
		return str;
	}
	

	/**
	 * Adds new alias into aliases table or clears all aliases
	 * 
	 * @param newName -- new alias for oldName. If newName is empty, 
	 *                   then all aliases will be deleted 
	 * @param oldName -- old name for the alias
	 */
	public void SetAlias(String newName, String oldName) {
		
		if ( newName.length() == 0 ) {
			aliases.clear();
			return;
		}
		
		aliases.put(newName, oldName);
	}
	

	/* (non-Javadoc)
	 * @see textformatter.TFExcDataLoad#getID()
	 */
	@Override
	public String getID() {
		
		return "SENTENCE_READER: ";
	}

}
//-----------------------------------------------------------------------------


/**
 * Implements functionality of one page of the document
 * Consists one or zero header, few paragraphs and zero or few footnotes
 * 
 * @author dr.Dobermann
 */
class Page 
	implements TFExcDataLoad {
	
	private @Getter TextFormatter textFormatter;
	private @Getter int width = 72;
	private @Getter int height = 40;
	private @Getter int currWidth = width;
	private @Getter Para.PAlign align = Para.PAlign.PA_LEFT;
	
	private @Getter int pageNum;
	
	private @Getter int[] spaces = new int[] {0, 0};
	private @Getter int interval = 1;
	private @Getter int[] margins = new int[] {0, 0};
	private @Getter int indent = 3;
	
	private List<Para> paragraphs = new ArrayList<Para>();
	private @Getter int linesLeft = height;
	
	private @Getter int headerHeight = 0;
	private @Getter Para.PAlign headerAlign = Para.PAlign.PA_CENTER;
	private @Getter int headerLine;
	private Header header;
	
	private @Getter boolean isClosed = false;
	
	
	public Page(TextFormatter tf) 
		throws TFException {
		
		pageNum = tf.GetLastPageNum();
		
		textFormatter = tf;
		
		header = new Header( this );
		paragraphs.add( header );

		if ( headerHeight > 0 )
			header.ResetHeader();
		
		width = tf.getWidth();
		height = tf.getHeight();
		
		indent = tf.getIndent();
		interval = tf.getInterval();
		spaces[0] = tf.getSpaces()[0];
		spaces[1] = tf.getSpaces()[1];
		margins[0] = tf.getMargins()[0];
		margins[1] = tf.getMargins()[1];
		
		AddNewPara();
	}
	
	
	// Functionality
	//-------------------------------------------------------------------------
	
	/**
	 * Adds a new paragraph on the page. Before the addition it closes and 
	 * formatting the previous one.
	 */
	public void AddNewPara() 
		throws TFException {
		
		if ( paragraphs.size() > 0 )
			GetLastPara().Close();
		
		paragraphs.add(new Para(this, currWidth, align, interval, indent, spaces, margins));
	}
	
	/**
	 * Returns last paragraph on the page
	 * @return last paragraph. If there is no any, exception fires
	 * @throws TFException
	 */
	private Para GetLastPara()
		throws TFException {
		
		if ( paragraphs.size() == 0 )
			throw new
				TFException(getID(), "GetLastPara: There is no available paragraphs yet!");
		
		return paragraphs.get(paragraphs.size() - 1);
	}
	
	/**
	 * Adds a new string into the last paragraph
	 * 
	 * @param str  -- Decorated string to add
	 * 
	 * @throws TFException
	 */
	public void AddString(DecoratedStr str) 
		throws TFException {
		
		GetLastPara().AddString(str);
	}

	/**
	 * Close page and sent the rest of it to the next one
	 * 
	 * @param next -- Next page to consume the rest of the current page
	 */
	public void Close( Page next )
		throws TFException {

		Format();
		
		isClosed = true;
		
		if ( linesLeft >= 0 )
			return;
		
		while ( linesLeft < 0 ) {
			
			
		}
		
	}
	
	/**
	 * Formats page and calculate its actual length
	 * 
	 * @throws TFException
	 */
	private void Format() 
		throws TFException {
		
		boolean hasFootnote = false;
		linesLeft = height;
		
		for ( Para p : paragraphs ) {
			p.Format();
			linesLeft -= p.GetLinesCount(true, true);
			if ( p.GetFootnotesCount() > 0 )
				hasFootnote = true;
		}
		
		if ( hasFootnote )
			linesLeft--;		
	}
	
	/**
	 * Sets new paragraph width. Closes the current paragraph and adds a new one
	 * 
	 * @param newWidth -- new paragraphs width for next new paragraphs
	 * 
	 * @throws TFException
	 */
	public void SetWidth( int newWidth )
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		if ( newWidth > width )
			throw new
				TFException(getID(), 
						    "The new page width [%d] should not be greater than its initial widht [%d]!!!",
						    newWidth, width);
		
		currWidth = newWidth;
		
		AddNewPara();
	}
	
	/**
	 * Sets or deletes the page header
	 * 
	 * @param height
	 * @param line
	 * @throws TFException
	 */
	public void SetHeader() 
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		if ( textFormatter.getHeaderHeight() > 1 ) {
			
			headerHeight = textFormatter.getHeaderHeight();
			headerLine = textFormatter.getHeaderLine();
			headerAlign = textFormatter.getHeaderAlign();
			
			if ( header == null ) {
				header = new Header( this );
				paragraphs.add(0, header);
			}
		}
		else 
			headerHeight = 0;
		
		header.ResetHeader();
	}
	
	/**
	 * Feeds lines in the last paragraph
	 * 
	 * @param lines	       -- lines to feed
	 * @param withInterval -- add interval after lines or not
	 * 
	 * @throws TFException
	 */
	public void FeedLines( int lines, boolean withInterval ) 
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		GetLastPara().FeedLines(lines, withInterval);
		
		AddNewPara();
	}
	
	/**
	 * Sets new align setting for the next paragraphs
	 * 
	 * @param newAlign -- new align settings
	 * 
	 * @throws TFException
	 */
	public void SetAlign( Para.PAlign newAlign ) 
		throws TFException {

		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");
		
		align = newAlign;
		
		AddNewPara();		
	}
	
	/**
	 * Sets new paragraph settings
	 * 
	 * @param indent   -- new first line indent settings
	 * @param spaces   -- spaces before[0] and after[1] paragraph
	 * 
	 */
	public void SetParaSettings( int indent, int[] spaces ) 
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		if ( spaces[0] < 0 || spaces[1] < 0)
			throw new
			TFException( getID(),
					     "[SetParaSettings] Invalid spaces [%d] [%d]",
					     spaces[0], 
					     spaces[1] );
		
		
		if ( indent < 0 && margins[0] < -indent )
			throw new
				TFException( getID(),
						     "[SetParaSettings] Invalid negative indent [%d]." +
						     "Should be not less than left margin [%d]",
						     indent, 
						     margins[0] );
			 
		if ( indent > 0 && indent > currWidth )
			throw new
				TFException( getID(),
						     "[SetParaSettings] Indent [%d] is wider than current page width [%d].",
						     indent, 
						     currWidth );
		
		this.spaces[0] = spaces[0];
		this.spaces[1] = spaces[1];
		
		this.indent = indent;
		
		AddNewPara();	 
	}

	/**
	 * Sets margins settings for new paragraphs
	 * 
	 * @param  margins   -- new margins settings [0] - left, [1] - right
	 * 
	 * @throws TFException
	 */
	public void SetMargins( int[] margins ) 
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		if ( margins[0] < 0 || margins[1] < 0 || margins[0] + margins[1] > width )
			throw new
			TFException( getID(),
					     "[SetMargins] Invalid margins [%d] [%d]",
					     margins[0], 
					     margins[1] );
		
		this.margins[0] = margins[0];
		this.margins[1] = margins[1];
		
		AddNewPara();	 	
	}
	
	/**
	 * Sets new interval value for next paragraphs
	 * 
	 * @param newInt
	 * @throws TFException
	 */
	public void SetInterval( int newInt ) 
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		if ( newInt  < 0 || newInt > height )
			throw new
			TFException( getID(),
					     "[SetInterval] Invalid interval value [%d]!!!",
					     newInt );
		
		interval = newInt;
				
		AddNewPara();	 	
	}

	/**
	 * Sets new page number
	 * 
	 * @param pNum -- new page number
	 * 
	 * @throws TFException
	 */
	public void SetPageNum( int pNum ) 
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		if ( pNum < 0 )
			throw new
			TFException( getID(),
					     "[SetInterval] Invalid page number [%d]!!!",
					     pNum );
		
		pageNum = pNum;
		
		header.ResetHeader();
	}
	
	/**
	 * Adds a footnote to the last line of current paragraph
	 * 
	 * @param fnote -- an array of strings of footnote
	 * @param id    -- id of a new footnote
	 * @throws TFException
	 */
	public void AddFootnote( String[] fnote, int id )
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		DecoratedStr[] dfn = new DecoratedStr[fnote.length];
		
		for ( int f = 0; f < fnote.length; f++ ) 
			dfn[f] = ParaLine.PrepareString(fnote[f]);
		
		GetLastPara().AddFootnote(dfn, id);
	}
		
	/* 
	 * @see textformatter.TFExcDataLoad#getID()
	 */
	@Override
	public String getID() {
		
		return String.format("PAGE #%d3;", pageNum);
	}
	
}
//-----------------------------------------------------------------------------



/**
 * Provides single paragraph functionality
 * Every single line might have position-linked decorations (such as bold,
 * italic and so on).
 *  
 * @author dr.Dobermann
 *
 */
class Para implements
	TFExcDataLoad {
	
	enum PAlign { 
					PA_AS_IS,
					PA_LEFT,
					PA_RIGHT,
					PA_CENTER,
					PA_FILL
				};
	
	protected @Getter PAlign align;
	
	private @Getter int width;
	protected @Getter Page page;
	
	// if paragraph is closed any addition to it
	// will fire TFException
	protected @Getter boolean closed = false;
	
	private @Getter int indent;
	private @Getter int[] spaces;
	private @Getter int[] margins;
	private @Getter int interval; // interval between formatted lines
	                              // 1 means no empty lines added between
	                              // lines 
	
	// the flag shows if the formatted lines are comply with the buffer
	protected @Getter boolean isInvalid = true;
	// buffer of unformatted ParaLines 
	protected List<ParaLine> buff = new ArrayList<ParaLine>();
	// list of formatted ParaLines
	protected List<ParaLine> lines = new ArrayList<ParaLine>();
	
	protected List<Footnote> footnotes = new ArrayList<Footnote>();
	
	public Para( Page page, 
			     int width,
			     PAlign align,
			     int interval,
			     int indent,
			     int[] spaces,
			     int[] margins) {
		
		this.page = page;
		this.width = width;
		this.align = align;
		this.interval = interval;
		this.indent = indent;
		this.spaces = spaces;
		this.margins = margins;
	}

	/**
	 * Closing the paragraph
	 */
	public void Close() 
		throws TFException {
		
		if ( isInvalid )
			Format();
		
		closed = true;
	}
	
	/**
	 * Adds string to a unformatted buffer and adds decorations to it
	 * 
	 * @param str 		-- string to add
	 * @param decors	-- DecoPair placed on the string
	 */
	public void AddString( DecoratedStr str ) 
		throws TFException {
		
		if ( closed )
			throw new 
				TFException(getID(), "[AddString] Paragraph already closed!!!");
		
		isInvalid = true;
		
		ParaLine pl = new ParaLine(this, str.str.length() + 2);
		pl.AddString(str);
		buff.add(pl);
		
		// add double spaces into the end of the every sentence
		if ( str.str.length() > 0 && align != Para.PAlign.PA_AS_IS )
			pl.AddString("  ", new Decor[0]);
	}
	
	/**
	 * Adds a footnote to the current string in the buffer
	 * 
	 * @param footnote 	   -- Footnote string array with decorations 
	 * @param fNoteID	   -- footnote ID
	 * 
	 * @throws TFException
	 */
	public void AddFootnote( DecoratedStr[] footnote, int fNoteID ) 
		throws TFException {

		if ( closed )
			throw new 
				TFException(getID(), "[AddString] Paragraph already closed!!!");
		
		isInvalid = true;
		
		if ( footnote.length > 0 ) {
			
			Footnote fnote = new Footnote(page, this, new int[] {-1, buff.size() - 1}, fNoteID, width);  // TODO: correct it to page.getWidth()
			for ( DecoratedStr fs : footnote )
				fnote.AddString(fs);
			
			footnotes.add(fnote);
		}

	}
	
	
	/**
	 * Formats the paragraph according the settings
	 * 
	 * @throws TFException
	 */
	public void Format() 
		throws TFException {
		
		if ( !isInvalid )
			return;
		
		lines.clear();
		
		ParaLine line, newLine;
		int maxLen, lineNo;
		
		for ( ParaLine pl : buff ) {
			
			System.out.printf( "ParaLine [%s]\n", pl.GetStr() );
			
			if ( pl.GetLength() == 0 ) {
				lines.add(pl);
				AddInterval();
				continue;
			}
			
			line = pl.Copy();
			lineNo = buff.indexOf(pl);

			
			while ( line.GetLength() > 0 ) {
				System.out.println( line.GetStr() );

				maxLen = width - 
						 margins[1] - 
						 (lines.size() == 0 ? margins[0] + indent : margins[0]);
								
				// add new line if lines list is empty or 
				//              if last line in lines is full or
				//              if the align is as_is
				if ( lines.size() == 0 ||
					 lines.get(lines.size() - 1).GetLength() == width ||
					 align == Para.PAlign.PA_AS_IS
					)
					lines.add(new ParaLine(this, width));
				
				newLine = lines.get(lines.size() - 1);
				
				if ( maxLen >= line.GetLength() ) {
					newLine.JoinLine(line);
					if ( Para.PAlign.PA_AS_IS != align && lineNo == buff.size() - 1 )
						AlignLine( newLine, 
								   line.GetLength() == 0 && lineNo == buff.size() - 1 );
					continue;
				}
				else { // add part of line from 0 up to last space in it
					int len = line.getBuff().substring(0, maxLen).lastIndexOf(' ');
					newLine.JoinLine(line.CutHead( len < 0 ? 0 : len ));

					LinkFootnotes(lineNo);
					
					if ( Para.PAlign.PA_AS_IS != align )
						AlignLine(newLine,
								 line.GetLength() == 0 && lineNo == buff.size() - 1); // is it the last line of paragraph?
					
					// if it's not fully processed last buffer line, then add new line into formatted lines list
					if ( !(line.GetLength() == 0  && lineNo == buff.size() - 1) )
						lines.add(new ParaLine(this, width));
				}
			}
		}
		
		if ( footnotes.size() > 0 )
			for ( Footnote fn : footnotes )
				fn.Format();
		
		isInvalid = false;
	}
	
	/**
	 * Adds extra empty lines in the end of the paragraph
	 * and closes it
	 * 
	 * @param lNum
	 * @param withInterval
	 */
	public void FeedLines( int lNum, boolean withInterval ) 
		throws TFException {
		
		if ( closed )
			throw new 
				TFException( getID(), 
						     "[AddString] Paragraph already closed!!!" );
		
		Format();
		
		for ( int l = 0; l < lNum; l++ ) {
			lines.add( new ParaLine( this, width ) );
			if ( withInterval && interval > 1 )
				for ( int i = 0; i < interval; i++ )
					lines.add(new ParaLine(this, width));
		}

		closed = true;
		
	}
	
	/**
	 * Links all footnotes which were transferred from buffer line to the 
	 * last formatted line 
	 * 
	 * @param buffLineNo -- number of buffer line processing
	 */
	private void LinkFootnotes( int buffLineNo ) {

		Footnote[] fl = GetFNotesOnLine(buffLineNo, false);
		ParaLine line = lines.get(lines.size() - 1);
		
		if ( fl.length > 0 ) {
			
			int pos = 0;
			
			Decor fNoteDecor = line.GetFirstDecorFrom(Decor.DeCmd.DCS_FNOTE, 0);
			
			while ( fNoteDecor != null ) {
				
				pos = fNoteDecor.getPos() + 1;
				
				for ( Footnote fn : fl )
					if ( fn.getNoteID() == Integer.parseInt(fNoteDecor.getData().toString()) )
						fn.SetLine(lines.size() - 1, false);
				
				fNoteDecor = line.GetFirstDecorFrom(Decor.DeCmd.DCS_FNOTE, pos);
			}
		}
		
	}
		
	/**
	 * Aligns the line according to align settings and adds necessary margins
	 * 
	 * @param pl         -- line to align
	 * @param lastLine   -- if it's the last line, it shouldn't be aligned in fill mode
	 * 
	 * @throws TFException
	 */
	protected void AlignLine( ParaLine pl, boolean lastLine ) 
		throws TFException {
		
		if ( align == Para.PAlign.PA_AS_IS )
			throw new
				TFException( getID(), "It's forbidden to align lines with PA_AS_IS setting on!!!");
		
		// remove leading or trailing spaces for every line except for the first one
		// if there is positive indent, then only trim the right side
		//if ( lines.size() == 1 && indent > 0 )
		//	pl.Trim(ParaLine.TrimSide.TS_RIGHT);
		//else
			pl.Trim(ParaLine.TrimSide.TS_BOTH);

		// adds left margins
		if ( margins[0] > 0 )
			pl.Pad(Para.PAlign.PA_LEFT, ' ', margins[0] + (lines.size() == 1 ? indent : 0) );
		
		int fillSpace = width - pl.GetLength() - margins[1] - (lines.size() == 1 ? indent : 0);
				
		switch ( align ) {
		
			case PA_AS_IS :
				break;
				
			case PA_LEFT :
				pl.Pad(Para.PAlign.PA_RIGHT, ' ', fillSpace);
				break;
			
			case PA_RIGHT :
				pl.Pad(Para.PAlign.PA_LEFT, ' ', fillSpace);
				break;
			
			case PA_CENTER :
				pl.Pad(Para.PAlign.PA_CENTER, ' ', fillSpace);
				break;
			
			case PA_FILL :
				
				if ( !lastLine && fillSpace > 0 ) { // do not align the last or full line
				
					Map<String, Integer[]> words = new HashMap<String, Integer[]>(); 
					boolean firstWord = true;
					// look for words
					Matcher m = Pattern.compile("[\\Wp{Punct}\\p{Blank}]*\\w+[\\p{Punct}\\p{Blank}]*").matcher(pl.getBuff());
				
					while ( m.find() )
					{
						if ( firstWord ) // we don't add spaces before the first word
							firstWord = false;
						else
							words.put(pl.getBuff().substring(m.start(), m.end()), new Integer[] {m.start(), 0});
					}
					
					int maxSpaces = 1;
					int pos, 
					    shift = 0;
					int tries = 0;
					String word;
					while ( fillSpace > 0 ) {
						
						word = words.keySet().toArray(new String[0])[(int)(Math.random() * (words.size() - 1))];
						pos = words.get(word)[0];
						
						if ( words.get(word)[1] < maxSpaces ) {
							words.put(word, new Integer[] {pos, maxSpaces});
							// insert space before the word
							pl.InsertChar(pos + shift++, ' '); 
							fillSpace--;
							tries = 0;
						}
						else 
							if ( tries++ > 2 ) {// if it not succeeded to find appropriate position three times 
								maxSpaces++;    // increase number of allowed spaces before the word.
								tries = 0;
							}
					}
				}
				break;
		}
		
		// adds right margins
		if ( margins[1] > 0 )
			pl.Pad(Para.PAlign.PA_RIGHT, ' ', margins[1]);
		
		AddInterval();

	}
	
	/**
	 * Adds an interval after the formatted line
	 * 
	 * @throws TFException
	 */
	private void AddInterval() 
		throws TFException {

		for ( int i = 1; i < interval;  i++ ) 
			lines.add(new ParaLine(this, width));
	}
	
	/**
	 * Looking for footnotes, linked to the line
	 * 
	 * @param line -- line number in buffered or formatted paragraph lines
	 * 
	 * @param inFormatted -- if true, the search starts in formatted lines,
	 * 						 if false only buffer is checked
	 * 
	 * @return an array of footnotes, linked to the line
	 */
	private Footnote[] GetFNotesOnLine( int line, boolean inFormatted )
	{
		if ( footnotes.size() == 0 )
			return new Footnote[0];
		
		List<Footnote> fnl = new ArrayList<Footnote>();
		
		for ( Footnote f : footnotes )
			if ( (inFormatted && f.getLineNo()[0] == line) ||
				 (!inFormatted && f.getLineNo()[1] == line) )
				fnl.add(f);
		
		return fnl.toArray(new Footnote[0]);
	}
	
	/**
	 * Returns an array of formatted lines
	 * 
	 * @param spaced -- Determines if empty string should be added as 
	 *                  it set up in spaces and interval settings. 
	 *                  Empty lines will be added when it's true.
	 *                   
	 * @return an array of ParaLines. Spaces and intervals added as empty ParaLines
	 * 
	 * @throws TFException
	 */
	public ParaLine[] GetLines( boolean spaced, boolean withFNotes )
		throws TFException 	{
		
		if ( isInvalid )
			Format();
			
		List<ParaLine> ll = new ArrayList<ParaLine>();
		
		if ( spaced )
			for ( int s = 0; s < spaces[0]; s++ )
				ll.add(new ParaLine(this, 1));
		
		for ( ParaLine line : lines ) {
			ll.add(line);
			if ( spaced && 
				 lines.indexOf(line) != lines.size() - 1 ) // do not add interval after last line
				for ( int i = 0; i < interval - 1; i++)
					ll.add(new ParaLine(this, 1));
		}
		
		if ( spaced )
			for ( int s = 0; s < spaces[1]; s++ )
				ll.add(new ParaLine(this, 1));
		
		if ( withFNotes && footnotes.size() > 0 ) {
			ll.add(new ParaLine(this, width));
			ll.get(ll.size() - 1).Pad(Para.PAlign.PA_LEFT, '-', width);
			
			for ( Footnote f : footnotes )
				ll.addAll(Arrays.asList(f.GetLines(spaced, false)));
		}
		
		return ll.toArray(new ParaLine[0]);
	}
	
	/**
	 * Returns number of lines in formatted or buffered zones
	 * 
	 * @param fromFormatted -- if it's true, count of formatted lines returns, 
	 *                         if it's false, count of buffered lines returns.
	 * @param withFootnotes -- if it's true the count of footnotes lines also takes
	 *                         into account
	 *                         
	 * @return number of paragraph lines
	 */
	public int GetLinesCount( boolean fromFormatted, boolean withFootnotes ) {
		
		int fnLines = 0;
		if ( !isInvalid && footnotes.size() > 0 )
			for ( Footnote fNote : footnotes )
				fnLines += fNote.GetLinesCount(fromFormatted, false);

		if ( !fromFormatted )
			return buff.size() + fnLines;
		
		
		return lines.size() + fnLines;
	}

	/**
	 * Returns the number of footnotes linked to the paragraph
	 * 
	 * @return number of linked footnotes
	 */
	public int GetFootnotesCount() {
		
		return footnotes.size(); 
	}

	/**
	 * Return the ParaLines of footnote 
	 * 
	 * @param idx -- footnote index to get
	 * 
	 * @return an array of ParaLines
	 * 
	 * @throws TFException
	 */
	public ParaLine[] GetFootnote( int idx ) 
		throws TFException {
		
		if ( idx < 0 || idx > footnotes.size() - 1 )
			throw new
				TFException( getID(), 
						     "[GetFootnote] Invalid footnote index %d!!!", 
						     idx );
		
		if ( isInvalid )
			Format();
		
		return footnotes.get(idx).GetLines(false, false);
	}
	
	@Override
	public String toString() {
		
		ParaLine[] pll;
		try {
			pll = GetLines(true, true);
		} 
		catch ( TFException e ) {
			throw new
				RuntimeException(e.getMessage());
		}
		
		StringBuilder sb = new StringBuilder();
		
		for ( ParaLine line : pll )
			if ( line.GetLength() == 0 )
				sb.append('\n');
			else {
				sb.append(line.GetStr());
				sb.append('\n');
			}
		
		return sb.toString();
			
	}
		
	/*
	 * @see textformatter.TFExcDataLoad#getID()
	 */
	@Override
	public String getID() {
		return "PARA: ";
	}
	
	
	
}
//-----------------------------------------------------------------------------

/**
 * Implements functionality of a footnote
 * 
 * @author dr.Dobermann
 *
 */
class Footnote extends Para 
               implements TFExcDataLoad {
	
	private final static String fNoteFmt = " %d) ";
	private final static int fNoteFmtLen = String.format(fNoteFmt, 100).length(); // max 100 footnotes on the page
	
	public final static String fNoteMark = "%d";
	
	private @Getter int noteID;
	private @Getter Para para;		// related paragraph
	private @Getter int[] lineNo;	// line number in paragraph where link is
									//   0 -- the position in formatted paragraph
									//   1 -- the position in buffer
	
	/**
	 * @param page
	 * @param width
	 */
	public Footnote( Page page, Para para, int[] lineNo, int id, int width ) 
		throws TFException {
		
		super(page, 
			  width, 
			  Para.PAlign.PA_LEFT, 
			  1,
			  -fNoteFmtLen,
			  new int[] {0,0}, 
			  new int[] {fNoteFmtLen + 1, 0});
		
		noteID = id;		
		this.para = para;
		this.lineNo = lineNo;
		
		ParaLine pl = new ParaLine(this, super.getWidth()); 
		
		pl.AddString(String.format(fNoteFmt, noteID), new Decor[0]);
		buff.add(pl);
	}
	
	/**
	 * Sets a new line number for the footnote mark.
	 * if inBuffer is true, then line number is linked to buffer line,
	 * if not, then to the formatted lines
	 * 
	 * @param newLine   -- new line number
	 * @param inBuffer  -- if true, new line sets for buffer, if
	 * 					   false, then to the formatted lines
	 */
	public void SetLine( int newLine, boolean inBuffer ) {
		if ( inBuffer )
			lineNo[1] = newLine;
		else
			lineNo[0] = newLine;
	}
	
	@Override
	public void AddFootnote( DecoratedStr[] footnote, int fNoteID ) 
			throws TFException {} // footnote couldn't have footnote

	public void FeedLines( int lNum, boolean withInterval ) {}
	
	@Override
	public String getID() {
		return String.format("FOOTNOTE[%d]: ", noteID);
	}
	
}
//-----------------------------------------------------------------------------

/**
 * Implements a header of a page
 * 
 * @author dr.Dobermann
 *
 */
class Header extends Para
			 implements TFExcDataLoad {

	public Header(Page page) 
		throws TFException {

		super(page, 
			  page.getWidth(), 
			  page.getHeaderAlign(), 
			  1,
			  0,
			  new int[] {0,0}, 
			  new int[] {0, 0});

		ResetHeader();
	}
	
	// Functionality
	//-------------------------------------------------------------------------
	/**
	 * Renews header representation
	 * 
	 * @throws TFException
	 */
	public void ResetHeader() 
		throws TFException {

		buff.clear();
		lines.clear();
		super.isInvalid = true;
		
		if ( page.getHeaderHeight() == 0 )
			return;
		
		super.align = page.getHeaderAlign();
		
		final String sNum = String.format( "- %d -", page.getPageNum() );

		ParaLine pl;
		
		buff.clear();
		for ( int l = 0; l < page.getHeaderHeight(); l++ ) {
			if ( l == page.getHeaderLine() ) { 
				pl = new ParaLine( this, getWidth() );
				pl.AddString( sNum, new Decor[0] );
				super.AlignLine( pl, false );
			}
			else
				// add double dashed string as the last line 
				// of the header if the line has lover index
				if ( l == page.getHeaderHeight() - 1 && 
				     page.getHeaderHeight() > 1 && 
				     page.getHeaderLine() != page.getHeaderHeight() - 1 ) {
					
					pl = new ParaLine( this, getWidth() );
					pl.Pad( Para.PAlign.PA_LEFT, '=', getWidth() );
				}
				else
					pl = new ParaLine( this, getWidth() );
	
			buff.add( pl );					
		}
		
		Format();
	}
	
	public void AddString(DecoratedStr str) {}
	
	public void AddFootnote(DecoratedStr[] fnote, int fNoteID) {}

	public void FeedLines( int lNum, boolean withInterval ) {}
	
	@Override
	public String getID() {
		return String.format("HEADER4PAGE[%d]: ", super.page.getPageNum());
	}
}
//-----------------------------------------------------------------------------

/**
 * Represents a pair of string and its decorations
 * 
 * @author dr.Dobermann
 */
class DecoratedStr {
	public String str;
	public Decor[] dpl;
	
	public DecoratedStr (String str, Decor[] dpl) {
		this.str = str;
		this.dpl = dpl;
	}
}
//-----------------------------------------------------------------------------


/**
 * Consists of information about decoration command
 * Every decoration command linked to a line position.
 * It could start decoration or end decoration
 * 
 * Decoration could also has a data related to it. Usually 
 * starting decoration has it. The content of the data depends on 
 * type of decoration. 
 * For example, footnote decoration has a footnote index in data
 * as a string.
 * 
 * @author dr.Dobermann
 */
class Decor 
	implements TFExcDataLoad,
			   Comparator<Decor> {
	
	// Decoration command. DCS means starting command, DCE means ending command 
	public enum DeCmd {
						DCS_BOLD,        DCE_BOLD, 
						DCS_ITALIC,      DCE_ITALIC,
						DCS_UNDERLINE,   DCE_UNDERLINE,
						DCS_UPIDX,       DCE_UPIDX,
						DCS_DNIDX,       DCE_DNIDX,
						DCS_FNOTE,       DCE_FNOTE;
		
		public DeCmd GetOpposite( DeCmd dec ) {
			switch ( dec ) {
				case DCS_BOLD      : return DCE_BOLD;
				case DCS_ITALIC    : return DCE_ITALIC;
				case DCS_UNDERLINE : return DCE_UNDERLINE;
				case DCS_UPIDX     : return DCE_UPIDX;
				case DCS_DNIDX     : return DCE_DNIDX;
				case DCS_FNOTE     : return DCE_FNOTE;
				
				case DCE_BOLD      : return DCS_BOLD;
				case DCE_ITALIC    : return DCS_ITALIC;
				case DCE_UNDERLINE : return DCS_UNDERLINE;
				case DCE_UPIDX     : return DCS_UPIDX;
				case DCE_DNIDX     : return DCS_DNIDX;
				case DCE_FNOTE     : return DCS_FNOTE;
				
			}
			return dec;
		}
		
		public boolean IsEnding( DeCmd dec ) {
			
			return dec.name().matches("DCE.*");
		}
	};
	
	private @Getter ParaLine line;
	private @Getter DeCmd cmd;
	private @Getter int pos;
	private @Getter Object data;
	
	public Decor(ParaLine owner, DeCmd cmd, int pos, Object data) throws TFException {
		this.line = owner;
		this.cmd = cmd;
		this.data = data;
		
		if ( pos > line.GetLength() )
			throw new 
				TFException(getID(), 
						    "The given decoration position [%d] is out of bounds of the paraLine width [%d]!!!", 
								pos, line.GetLength());
		this.pos = pos;
	}
	
	public Decor(DeCmd cmd, int pos, Object data) {
		this.line = null;
		this.cmd = cmd;
		this.data = data;
		this.pos = pos;

	}
	
	/**
	 * Shifts decoration position on given shift
	 * @param shift -- number of positions to shift. Might be negative
	 */
	public void ShiftPos(int shift) throws TFException {
		
		int newPos = pos + shift;
		if ( newPos < 0 || ( line != null && newPos > line.GetLength()) )
			throw new 
				TFException(getID(), 
							"Could not shift beyond the line bounds!!!");
		
		pos = newPos;
	}

	/**
	 * Sets new decoration's owner and new position
	 * @param newPL   -- new decor's owner
	 * @param newPos  -- new position inside a new owner
	 * @throws TFException
	 */
	public void SetLine(ParaLine newPL, int newPos) 
		throws TFException {
		if ( newPL == line )
			return;
		
		line = newPL;
		
		if ( newPos < 0 || newPos > line.GetLength() )
			throw new
				TFException(getID(), "New position [%d] is out of bounds of the line!!!", newPos);
		
		pos = newPos;
	}

	/**
	 * Returns a tail of the DecoPair array which element's pos is bigger than fromPos.
	 * Positions of the tail elements aligned by fromPos
	 * 
	 * @param dpl		-- initial DecoPair array
	 * @param fromPos   -- position to align from.
	 *  
	 * @return new array formed form an initial one (dpl)
	 *  
	 * @throws TFException
	 */
	public static Decor[] DropTail ( Decor[] dpl, int fromPos ) {

		List<Decor> res = new ArrayList<Decor>();
		
		for ( Decor d : dpl )
			if ( d.pos > fromPos )
				res.add(new Decor(d.cmd, d.pos - fromPos, d.data));
		
		return res.toArray(new Decor[0]);
	
	}
	
	/**
	 * Gets first part of DecoPair array and return it
	 * @param dpl     -- initial DecoPair array
	 * @param maxIdx  -- maximum index to slice
	 * @return new array of dpl elements from 0 to maxIdx
	 */
	public static Decor[] GetMaxIdx ( Decor[] dpl, int maxIdx ) {
		
		List<Decor> res = new ArrayList<Decor>();
		
		for ( int d = 0; d <= maxIdx; d++ )
			res.add(dpl[d]);
		
		return res.toArray(new Decor[0]);
	}
	
	/**
	 * Gets first pairs of DecoPair array, which pos is less or equal to maxPos
	 *  
	 * @param dpl    -- initial DecoPair array
	 * @param maxPos -- maximum position to select resulting elements
	 * 
	 * @return array of DecoPair, which position is less or equal to maxPos 
	 */
	public static Decor[] GetMaxPos ( Decor[] dpl, int maxPos ) {
		
		List<Decor> res = new ArrayList<Decor>();
		
		for ( Decor d : dpl )
			if ( d.pos <= maxPos )
				res.add(d);
		
		return res.toArray(new Decor[0]);
	}
	
	/* 
	 * @see textformatter.TFExcDataLoad#getID()
	 */
	@Override
	public String getID() {
		return String.format("DECOR[%d]: ", pos);
	}

	@Override
	public int compare(Decor o1, Decor o2) {
		if ( o1.pos < o2.pos )
			return -1;
		
		if ( o1.pos > o2.pos )
			return 1;
		
		return 0;
	}
    
	
}
//-----------------------------------------------------------------------------

	
/**
 * Describes one line of a paragraph 
 * Consist the line itself and all related decorations
 * 
 * @author dr.Dobermann
 *
 */
class ParaLine 
	implements TFExcDataLoad {
	
	public enum TrimSide {
		TS_LEFT,
		TS_RIGHT,
		TS_BOTH
	};
	
	private @Getter Para para;
	private @Getter StringBuilder buff;
	private @Getter int width;
	
	private List<Decor> decors = new ArrayList<Decor>();
	
	public ParaLine(Para owner, int width) {
		
		this.para = owner;
		
		this.width = width == -1 ? para.getWidth() : width; 
		
		buff = new StringBuilder(this.width);
	}
	
	public Decor[] GetDecors() {
		
		return decors.toArray(new Decor[0]);
	}
	
	/**
	 * Shift all decorations of the line started from 'after' position
	 * @param after -- position after which the shift started
	 * @param shift -- shift size
	 */
	public void ShiftDecors(int after, int shift) 
			throws TFException {
		
		if ( after < 0 || after > buff.length() )
			throw new
				TFException(getID(), 
					"Shift point [%d] is out of line bounds!!!", after);
		
		for ( Decor dec : decors )
			if ( dec.getPos() >= after )
				dec.ShiftPos(shift);
	}
	
	/**
	 * Drops tail of size length to the new ParaLine
	 * @param length -- length of the tail to drop off
	 * @return new ParaLine with tail of this one
	 */
	public ParaLine DropTail(int length) 
		throws TFException {
		
		if ( length > buff.length() )
			throw new
				TFException(getID(), "The tail length is bigger than the actual line length!!!");
		
		ParaLine pl = new ParaLine(para, width);
		
		// send the tail to a new ParaLine
		pl.buff.append(buff.substring(buff.length() - length, buff.length()));
		
		// sends the related decors as well
		for ( Decor dec : decors )
			if ( dec.getPos() >= buff.length() - length )
				dec.SetLine(pl, dec.getPos() - length + 1);
		
		// delete the tail
		buff.delete(buff.length() - length, buff.length());
		
		// delete the related decors as well
		for ( int d = 0; d < decors.size(); d++ )
			if ( decors.get(d).getLine() != this )
				decors.remove(d);
		
		return pl;
	}
	
	/**
	 * Cuts the begin of the ParaLine and returns it as a new ParaLine
	 * The rest of the initial are kept in old ParaLine
	 * The relevant decorations are also going to the new ParaLine
	 *   
	 * @param length -- length of the head to cut
	 * 
	 * @return new ParaLine with length symbols
	 * 
	 * @throws TFException
	 */
	public ParaLine CutHead( int length ) 
		throws TFException {
		
		ParaLine pl = new ParaLine( para, width );
		if ( length == 0 )
			return pl;
		
		if ( length > buff.length() )
			throw new
				TFException(getID(), "The head length is bigger than the actual line length!!!");
		
		pl.buff.append(buff.substring(0, length));
		for ( Decor dec : decors )
			if ( dec.getPos() < length ) {
				pl.InsertDecor(dec.getCmd(), dec.getPos(), dec.getData());
				decors.remove(dec);
			}
		
		buff.delete(0, length);
		ShiftDecors(0, length);
		
		return pl;
	}

	/**
	 * Joins a new ParaLine.
	 * If its length is longer than the free rest of the existed one,
	 * only part of it will be added.
	 * The rest of new ParaLine would be returned
	 * 
	 * @param pl -- new ParaLine to add
	 * 
	 * @return the rest of new ParaLine. it might have a zero length
	 */
	public ParaLine JoinLine( ParaLine pl ) 
		throws TFException {
		
		if ( pl.GetLength() == 0 )
			return new ParaLine(para, 1);
		
		int offset = buff.length(),
			len = pl.GetLength() > width - buff.length() ? width - buff.length() : pl.GetLength();
		
		for ( int p = 0; p < len; p++ ) {
			buff.append(pl.buff.charAt(p));
			for ( Decor dec : pl.GetDecorAt(p) )
				InsertDecor(dec.getCmd(), dec.getPos() + offset, dec.getData());
		}
		
		if ( len < pl.GetLength() )
			pl.CutHead(len);
		else
			{
				pl.buff.delete(0, len);
				pl.decors.clear();
			}
		
		return pl;
	}
	
	/**
	 * Creates a copy of ParaLine
	 * 
	 * @return Copy of existed ParaLine
	 * 
	 * @throws TFException
	 */
	public ParaLine Copy() 
		throws TFException {
		
		ParaLine pl = new ParaLine(para, width);
		
		pl.buff.append(buff);
		
		for ( Decor dec : decors )
			pl.InsertDecor(dec.getCmd(), dec.getPos(), dec.getData());
		
		return pl;		
	}
	
	/**
	 * Inserts one character ch into pos position
	 * @param pos   -- position inside or at the end of buffer
	 * @param ch	-- character to insert
	 * @throws TFException
	 */
	public void InsertChar(int pos, char ch) 
		throws TFException {
		
		if ( pos < 0 ||
			 pos > buff.length() ||
			 pos >= width )
			throw new
				TFException(getID(), "Invalid position [%d] for char inserting!", pos);
		
		if ( pos == buff.length() )
			buff.append(ch);
		else {
			buff.insert(pos, ch);
			ShiftDecors(pos, 1);
		}
	}
	
	/**
	 * Pads ParaLine with len number of char ch from align side
	 * @param align  -- side to add. Could be Para.PAlign.PA_LEFT, Para.PAlign.PA_RIGHT
	 *                  and Para.PAlign.PA_CENTER
	 * @param ch     -- char to pad
	 * @param len    -- number of chars
	 */
	public void Pad(Para.PAlign align, char ch, int len) 
		throws TFException {
		
		if ( align == Para.PAlign.PA_CENTER) {
			Pad(Para.PAlign.PA_LEFT, ch, len/2);
			Pad(Para.PAlign.PA_RIGHT, ch, len - len/2);
		}
		
		for ( int i = 0; i < len; i++ )
			switch ( align ) {
				case PA_LEFT :
					InsertChar(0, ch);
					break;
					
				case PA_RIGHT :
					InsertChar(buff.length(), ch);
					break;
					
				case PA_CENTER :
				case PA_AS_IS :
				case PA_FILL :
					break;
			}
	}	
	
	/**
	 * Trims extra spaces from begin, end or both sides of the ParaLine
	 * @param side  -- Select side for trimming @see textformatter.ParaLine.TrimSide  
	 */
	public void Trim( ParaLine.TrimSide side ) 
		throws TFException {
		
		if ( ParaLine.TrimSide.TS_BOTH == side ) {
			Trim(ParaLine.TrimSide.TS_LEFT);
			Trim(ParaLine.TrimSide.TS_RIGHT);
			
			return;
		}
		
		int pos;
		
		for ( int p = 0; p < buff.length(); p ++ ) {
			pos = ParaLine.TrimSide.TS_RIGHT == side ? buff.length() - p - 1 : p;
			
			if ( buff.charAt(pos) == ' ' ) {
				
				buff.deleteCharAt(pos);
				ShiftDecors(pos, -1);
			
				if ( ParaLine.TrimSide.TS_RIGHT == side )
					p--;
			}
			else
				break;
		}
		
		
	}
	
	/**
	 * Append new string str at the end of the ParaLine and adds Decorations for its begin
	 * @param str     -- string to append
	 * @param decors  -- array of decorations to insert in the begin of the appended string
	 * 
	 * @throws TFException
	 */
	public void AddString( String str, Decor[] decors)
		throws TFException {
		
		if ( str.length() > width - buff.length() )
			throw new
				TFException( getID(), "[AddString] String [%s] is too long for this ParaLine. Only %d symbols left", str, width - buff.length());
		
		int pos = buff.length();
		
		buff.append(str);
		
		for ( Decor d : decors ) 
			InsertDecor(d.getCmd(), d.getPos() + pos, d.getData());
	}
	
	/**
	 * Adds a new decorated string to the ParaLine
	 *  
	 * @param dStr -- Decorated string to add
	 * 
	 * @throws TFException
	 */
	public void AddString(DecoratedStr dStr) 
		throws TFException {
		
		if ( dStr.str.length() > width - buff.length() )
			throw new
				TFException( getID(), "[AddString] Decorated string [%s] is too long for this ParaLine. Only %d symbols left", 
									  dStr.str, width - buff.length());
		
		int pos = buff.length();
		
		buff.append(dStr.str);
		
		for ( Decor d : dStr.dpl )
			InsertDecor( d.getCmd(), d.getPos() + pos, d.getData());
	}	
	
	/**
	 * Adds new decoration on ParaLine if the position is out of 
	 * ParaLine length, it added to the end of the ParaLine
	 * 
	 * @param dec    -- Decoration command to add
	 * @param pos	 -- position of decoration command starts at
	 * @throws TFException
	 */
	public void InsertDecor( Decor.DeCmd dec, int pos, Object data)
		throws TFException	{
		
		if ( pos > buff.length() )
			pos = buff.length();
		
		decors.add(new Decor(this, dec, pos, data));
		
		decors.sort(decors.get(0));
	}
	
	
	/**
	 * Returns an array of decorations linked to the position
	 * 
	 * @param pos  -- position to check at
	 * 
	 * @return array of linked to position decorations. Might be empty 
	 */
	public Decor[] GetDecorAt( int pos ) {
		
		List<Decor> dl = new ArrayList<Decor>();
		
		for ( Decor dec : decors )
			if ( dec.getPos() == pos )
				dl.add(dec);
		
		return dl.toArray(new Decor[0]);
	}

	
	/**
	 * Returns string enclosed by two decoration commands cmd1 and cmd2.
	 * Search starts from position fromPos
	 * 
	 * @param cmd1    -- opening command 
	 * @param cmd2    -- closing command
	 * @param fromPos -- search starting point in ParaLine
	 *  
	 * @return String enclosed between cmd1 and cmd2 or empty string if 
	 *         search fails
	 * 
	 * @throws TFException
	 */
	public String GetStringBetweenDecors ( Decor.DeCmd cmd1,
			                               Decor.DeCmd cmd2,
			                               int fromPos)
		throws TFException {
		
		if ( fromPos < 0 || fromPos > buff.length() - 1 )
			throw new
				TFException( getID(), "GetStringBetweenDecors: fromPos is out of bounds!!!");
		
		int sPos = 0, 
			ePos = 0;
		Decor[] dl = decors.toArray(new Decor[0]);
		
		Arrays.sort(dl, dl[0]);
		
		for ( Decor d : dl )
			if ( d.getCmd() == cmd1 && d.getPos() >= fromPos )
				sPos = d.getPos();
			else 
				if ( d.getCmd() == cmd2 && d.getPos() >= fromPos )
					ePos = d.getPos();
		
		return buff.substring(sPos, ePos);
	}
	
	/**
	 * Returns first decoration object in the ParaLine substring
	 * @param dc    -- Decoration command to look for
	 * @param from  -- substring starting point
	 * 
	 * @return First decoration object found in ParaLine substring
	 */
	public Decor GetFirstDecorFrom(Decor.DeCmd dc, int from) {
		
		for ( Decor dec : decors )
			if ( dec.getCmd() == dc && dec.getPos() >= from )
				return dec;
		
		return null; 
	}
	
	/**
	 * Returns length of the buffer
	 * @return buffer length
	 */
	public int GetLength() {
		
		return buff.length();
	}
	
	
	public String GetStr() {
		
		return buff.toString();
		
	}
	
	
	/**
	 * Processes the input string with decoration command embedded 
	 * into it and creates new DecoratedStr
	 * 
	 * @param str -- string to process
	 * 
	 * @return new DecoratedStr
	 * 
	 * @throws TFException
	 */
	public static DecoratedStr PrepareString(String str)
		throws TFException {
		
		StringBuilder sb = new StringBuilder(str.length());
		int lastPos = 0,
			compensator = 0;
		
		List<Decor> decors = new ArrayList<Decor>();
		Decor.DeCmd dCmd;
		
		Matcher m = Pattern.compile("\\&(\\w?)([\\+|-]?)(\\{(\\w*)\\})?+").matcher(str);
		
		while ( m.find() ) {
			sb.append(str.substring(lastPos, m.start()));
			
			switch ( m.group(1) ) {
				case "B" : 
					if ( m.group(2).charAt(0) == '+' )
						dCmd = Decor.DeCmd.DCS_BOLD;
					else
						dCmd = Decor.DeCmd.DCE_BOLD;
					break;

				case "I" : 
					if ( m.group(2).charAt(0) == '+' )
						dCmd = Decor.DeCmd.DCS_ITALIC;
					else
						dCmd = Decor.DeCmd.DCE_ITALIC;
					break;

				case "U" : 
					if ( m.group(2).charAt(0) == '+' )
						dCmd = Decor.DeCmd.DCS_UNDERLINE;
					else
						dCmd = Decor.DeCmd.DCE_UNDERLINE;
					break;

				case "X" : 
					if ( m.group(2).charAt(0) == '+' )
						dCmd = Decor.DeCmd.DCS_UPIDX;
					else
						dCmd = Decor.DeCmd.DCE_UPIDX;
					break;

				case "x" : 
					if ( m.group(2).charAt(0) == '+')
						dCmd = Decor.DeCmd.DCS_DNIDX;
					else
						dCmd = Decor.DeCmd.DCE_DNIDX;
					break;
					
				case "F" : 
					if ( m.group(2).charAt(0) == '+')
						dCmd = Decor.DeCmd.DCS_FNOTE;
					else
						dCmd = Decor.DeCmd.DCE_FNOTE;
					break;
					
				default :
					throw new
						TFException("PARALINE", 
								String.format("[PerpareString] Invalid decoration command [%s] at position [%d] in str\n\"%s\"", 
											  m.group(1), m.start(), str));
			}
			
			decors.add(new Decor(dCmd, m.start() - compensator, m.group(4)));
				
			
			// compensation offset for the DecoPair position
			// it should be decreased for the length of previous match
			compensator += m.group().length();
			
			lastPos = m.end();
		}
		
		if ( lastPos < str.length() )
			sb.append(str.substring(lastPos));
		
		
		return new DecoratedStr(sb.toString(), decors.toArray(new Decor[0]));
	}
	
	
	/* 
	 * @see textformatter.TFExcDataLoad#getID()
	 */
	@Override
	public String getID() {
		return "PARALINE: ";
	}
}
//-----------------------------------------------------------------------------



/**
 * TextFormatter exception
 * @author dr.Dobermann
 *
 */
class TFException extends Exception {
	
	/**
	 * 
	 */
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
	
	public TFInvalidParameterCount(String str) {
		super(str);
	}
}

/**
 * Interface for object ID generation
 *  
 * @author dr.Dobermann
 */
interface TFExcDataLoad {
	public String getID(); 
}