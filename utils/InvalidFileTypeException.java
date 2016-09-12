package utils;
public class InvalidFileTypeException extends Exception {
	
	InvalidFileTypeException() {}
	
	InvalidFileTypeException(String msg) {
		super(msg);
	}
}