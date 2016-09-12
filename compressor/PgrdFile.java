package compressor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import utils.FileModel;
import utils.RecordOutOfBoundsException;

/**
 * A class to allow a user to easily pull any necessary piece of information out of a .pgrd file
 * 
 * @author Glazer, Joshua D.
 *
 */
public class PgrdFile extends FileModel {
	
	public double MINIMUM_LATITUDE;
	
	public double MINIMUM_LONGITUDE;
	
	public double MAXIMUM_LATITUDE;
	
	public double MAXIMUM_LONGITUDE;
	
	public double LATITUDE_INTERVAL;
	
	public int NUMBER_LATITUDE_LINES;
	
	public int TYPE_OF_SEGMENT_IDENTIFIER;
	
/**
 * 
 * Construtor that takes FileInputStream
 * @param fis A file stream object leading to valid .pgrd file
 * @throws IOException
 * 
 */
				
	public PgrdFile( FileInputStream fis ) throws IOException {
		
		super( new FileInputStream[]{ fis } );
		
		PgrdHeaderModel();
		
	}

/**
 * 
 * Constructor that takes the .pgrd address as a string
 * @param pgrdFileAddress The .pgrd address as a string
 * @throws IOException
 * 
 */
	public PgrdFile( String pgrdFileAddress ) throws IOException {
		
		super(  new FileInputStream[]{ 
						new FileInputStream( new File( pgrdFileAddress ) )
					} );
		
		PgrdHeaderModel();
	}
	
/**
 * 
 * A helper function called in the constructor to instantiate header data in instance's properties
 * @throws IOException
 * 
 */
	private void PgrdHeaderModel() throws IOException {
		
		MINIMUM_LATITUDE = getDoubleFrom( (short) 0, B_END, 0L);
		MINIMUM_LONGITUDE = getDoubleFrom( (short) 0, B_END, 8L);
		MAXIMUM_LATITUDE = getDoubleFrom( (short) 0, B_END, 16L);
		MAXIMUM_LONGITUDE = getDoubleFrom( (short) 0, B_END, 24L);
		LATITUDE_INTERVAL = getDoubleFrom( (short) 0, B_END, 32L);
		NUMBER_LATITUDE_LINES = getIntFrom( (short) 0, B_END, 40L);;
		TYPE_OF_SEGMENT_IDENTIFIER = getIntFrom( (short) 0, B_END, 44L);;
	
	}

/**
 * 
 * A function to get the offset (in bytes) from the start of the file to a specified latitude line
 * 
 * @param lineIndex The index (starting at 0) of the line
 * @return The offset of the line in bytes
 * @throws RecordOutOfBoundsException
 * 
 */
	
	protected long lineOffset( int lineIndex ) throws RecordOutOfBoundsException {
		
		if( lineIndex >= NUMBER_LATITUDE_LINES || lineIndex < 0) {
			
			throw new RecordOutOfBoundsException();
		}
		
		return getLongFrom( (short)0, B_END, (long) ( lineIndex*8 + 48) );
	}
	
/**
 * 
 * The length of a specified latitude line in bytes 
 * 
 * @param lineIndex The index (starting at 0) of the line
 * @return The length of the line in bytes 
 * @throws RecordOutOfBoundsException
 * 
 */
	
	protected int lineSize( int lineIndex ) throws RecordOutOfBoundsException {
		
		return getIntFrom( (short) 0, B_END, lineOffset( lineIndex ) );
		
	}
	
/**
 * A function to determine the total number of formations in a line 
 * 
 * @param lineIndex The index (starting at 0) of the line
 * @return The number of formations represented in a given latitude line
 * @throws RecordOutOfBoundsException
 * 
 */
	
	public int numberFormations( int lineIndex ) throws RecordOutOfBoundsException {
		
		return getIntFrom( (short) 0, B_END, lineOffset( lineIndex ) + 4 );
	}
	
/**
 * Gets the offset in a line of a geologic formation
 * 
 * @param lineIndex The index (starting at 0) of the line
 * @param formationIndex The index (starting at 0) of the formation in a given line
 * @return The offset (in bytes), starting from the beginning of the line, of the formation
 * @throws RecordOutOfBoundsException
 * 
 */
	
	protected int formationOffset( int lineIndex, int formationIndex ) throws RecordOutOfBoundsException {
		
		if( formationIndex >= numberFormations( lineIndex ) || formationIndex < 0 ) {
			
			throw new RecordOutOfBoundsException(); 
		}
		
		return getIntFrom( (short) 0, B_END, ( lineOffset( lineIndex ) + 8 + ( 4*formationIndex ) ) );
	}

/**
 * 
 * Determines the number of segments in a given formation on a given line
 * 
 * @param lineIndex The index (starting at 0) of the line
 * @param formationIndex The index (starting at 0) of the formation in a given line
 * @return The number of segments in a formation
 * @throws RecordOutOfBoundsException
 * 
 */
	
	public int numberFormationSegments( int lineIndex, int formationIndex ) throws RecordOutOfBoundsException {
		
		return getIntFrom( (short) 0, B_END, (long) formationOffset( lineIndex , formationIndex ) + lineOffset( lineIndex ) );
	}
	
/**
 * 
 * Determines the identifier index of a formation as it is registered in the parent shapefile's dbf component
 * 
 * @param lineIndex The index (starting at 0) of the line
 * @param formationIndex The index (starting at 0) of the formation in a given line
 * @return The id index for the geologic formation
 * @throws RecordOutOfBoundsException
 * 
 */
	
	public short getFormationIndex( int lineIndex, int formationIndex ) throws RecordOutOfBoundsException {
		
		return getShortFrom( (short)0, B_END, (long) formationOffset( lineIndex , formationIndex ) + lineOffset( lineIndex ) + 4 );
		
	}
	
/**
 * 
 * A function to get the longitude bounds of a given segment in a formation
 * 
 * @param lineIndex The index (starting at 0) of the line
 * @param formationIndex The index (starting at 0) of the formation in a given line
 * @param segmentIndex The index (starting at 0) of the segement in a given formation of a line
 * @return A two part array with the western-most bound (as a longitude value) of the segment stored in the first part and the eastern-most (as a longitude value) in the second
 * @throws RecordOutOfBoundsException
 * 
 */
	
	public float [] getSegmentBounds( int lineIndex, int formationIndex, int segmentIndex ) throws RecordOutOfBoundsException {
		
		if( segmentIndex >= numberFormationSegments( lineIndex, formationIndex ) || segmentIndex < 0 ) {
			throw new RecordOutOfBoundsException();
		}
		
		return new float[] { getFloatFrom( (short)0, B_END, (long) formationOffset( lineIndex , formationIndex ) + lineOffset( lineIndex ) + 6 +8*segmentIndex),
							 getFloatFrom( (short) 0, B_END, (long) formationOffset( lineIndex , formationIndex ) + lineOffset( lineIndex ) + 10 + 8*segmentIndex)
						   };
	} 

}
