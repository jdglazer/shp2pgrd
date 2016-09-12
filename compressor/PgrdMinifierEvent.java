package compressor;

public interface PgrdMinifierEvent {
	
	public void onLineWriteComplete( int a, int numLines );
	
	public void onStartFileWrite();
	
	public void onFinishFileWrite();

}
