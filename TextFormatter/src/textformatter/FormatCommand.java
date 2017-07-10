/**
 * 
 */
package textformatter;

/**
 * @author Dr.Dobermann
 * 
 * Describes formatting command interface  
 */
public interface FormatCommand {
	
	// returns command name
	public String GetName();
	
	// returns array of parameter's types
	public Class<?>[] GetParamTypes();
	// sets parameter value by parameter name
	public void SetParamValue( String pName, Object pValue );
	// returns list of parameters' names
	public String[] GetParamNames();
	// return parameter's regexp mask
	public String GetParamRegexpMask( String pName );
	
	// all command returns boolean flag as a result of action
	// if it's true, then everything are fine and command
	// execution might be continued
	// if it's false, execution should be terminated
	// actions which should be made before command apply
	public boolean PreAction();
	// command actions
	public boolean Action();
	// command's post actions if need be
	public boolean PostAction();
}
