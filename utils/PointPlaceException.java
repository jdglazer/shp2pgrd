package utils;
public class PointPlaceException extends Exception {
	public PointPlaceException() {}
	
	public PointPlaceException(String pointOfFailure) {
		
		super(  pointOfFailure);
		
	}
}