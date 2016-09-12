package utils;

public class FailedPgrdBuild extends Exception {
	
	public FailedPgrdBuild() {}

	public FailedPgrdBuild( String error_msg ) {
		
		super(error_msg);
	}
	
}
