package utils;
public abstract class ShortOverflow {
	
	public static final int MAX_SIGNED_SHORT = 32767;

	public static int overflowIndex( int num ) {
		
		return (int) num/(MAX_SIGNED_SHORT+1);
	}
	
	public static int overflowValue( int num ) {
		
		return (int) num%MAX_SIGNED_SHORT;
	}
	
	public static int overflowingShortAsInt( int overflowIndex, int overflowValue ) {
		
		return MAX_SIGNED_SHORT*overflowIndex + overflowValue;
	}
}
