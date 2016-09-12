package utils;
public class NoFileFoundException extends Exception {
	
	//default blank constructor
	NoFileFoundException() {}
	
	//constructor passes file name which can not be found to Exception message
	NoFileFoundException(String file_name) {
		super(file_name);
	};
}