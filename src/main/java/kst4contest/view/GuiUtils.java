package kst4contest.view;

public class GuiUtils {

	/**
	 * Checks wheter the input value of the String is numeric or not, true if yes
	 * TODO: Move to a utils class for checking input values by user... 
	 * @param str
	 * @return
	 */
	static boolean isNumeric(String str){
        return str != null && str.matches("[0-9.]+");
    }
	
}
