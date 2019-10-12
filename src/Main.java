



public class Main {
	
	public static void main (String ... args) {
		try {
			if (args.length == 3) {
				RegEx.findColor(args[0], args[1]);
			}else if (args.length == 2) {
				RegEx.find(args[0], args[1]);
			}else {
				System.err.println("argument missing try command : cmd REGEX FILEPATH");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}