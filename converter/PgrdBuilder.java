package converter;

import java.io.*;
import java.util.*;

import utils.FileBuildError;
import utils.FileModel;
import utils.PointPlaceException;
import utils.RecordOutOfBoundsException;
import utils.ShapeFile;

public class PgrdBuilder extends FileModel {

/**
 * Class to store file header data to be written in
 * 
 * @author Joshua Glazer
 *
 */
	
	private class FileHeader {
		
		public double MINIMUM_LATITUDE,
		
					  MAXIMUM_LATITUDE,
					  
					  MINIMUM_LONGITUDE,
					  
					  MAXIMUM_LONGITUDE,
					  
					  LATITUDE_INTERVAL;
		
		public int NUMBER_OF_LATITUDE_LINES,
		
				   LENGTH_OF_FORMATION_ID,
				   
				   LENGTH;
		
		public int [] LINE_START_POSITIONS_IN_FILE;
		
	/**
	 * clears the line start positions array for garbage collection
	 * 
	 */
		public void clear() {
			
			LINE_START_POSITIONS_IN_FILE = null;
		}
		
	}
	
/**
 * A class to store line headers to be written in
 * 
 * @author Joshua Glazer
 *
 */
	
	private class LineHeader {
		
		public int LENGTH, 
		
				   NUMBER_OF_FORMATIONS;
		
		public long START_OFFSET;
		
		public int [] FORMATION_START_INDICES;
		
	/**
	 * clears formation start indices array for garbage collection
	 * 
	 */
		public void clear() {
			
			FORMATION_START_INDICES = null;
			
			LENGTH = 0;
			
			NUMBER_OF_FORMATIONS = 0;
			
			START_OFFSET = 0;
		}
		
	}

/**
 * Stores header data to be written in
 * 
 * @author Joshua Glazer
 *
 */
	
	private class FormationHeader {
		
		public long START_OFFSET;
				   
		public int LENGTH;
		
		public void clear() {
			
			START_OFFSET = 0;
			
			LENGTH = 0;
		}
	}

/**
 * The file header data storage object
 * 
 */
	
	private FileHeader File_h;
	
/**
 * The line header data storage object
 * 
 */
	
	private LineHeader Line_h;
	
/**
 * The formation header data storage object
 * 
 */
	
	private FormationHeader Form_h;
	
	private EqualLatitudePointPlace elpp;
	
	private long writeOffset = 0;
	
	private BufferedWriter error_log;
	
	
/**
 * <<Constructor>> Constructor that takes the name of pgrd file to be written to
 * 	
 * @param pgrdName The name of the pgrd file to be written to
 * @param errorLogFile The name of a text file to write error messages to
 * @throws FileNotFoundException
 * @throws IOException
 * 
 */
	
	PgrdBuilder( String pgrdName, String errorLogFile, ShapeFile shp ) throws FileNotFoundException, IOException, RecordOutOfBoundsException {
		
		super( new RandomAccessFile[] {
				
				new RandomAccessFile( pgrdName+".pgrd", "rw" )
		});
		
		File_h = new FileHeader();
		
		Line_h = new LineHeader();
		
		Form_h = new FormationHeader();
		
		elpp = new EqualLatitudePointPlace( shp );
		
		error_log = new BufferedWriter( new FileWriter( new File( errorLogFile ), true ) );
			
	}
	
/**
 * A bus function used to capture and record all errors associated with thrown exceptions in the process of building the file
 * 
 * @param error_description A string describing the error
 * 
 */
	
	public void _error_log( String error_description ) {
		
		try {
			
			error_log.write( error_description + "\n" );
			
			error_log.flush();
			
		}
		catch( IOException ioe ) {}
		
	}

/**
 * Sets the header data for the file that can be set before file has been written
 * 
 * @param lat_int The change in latitude between successive latitude lines
 * @param sf The shape file from which header data comes
 * 
 */
	
	private void setHeaderData( double lat_int, int id_len, ShapeFile sf ) {
		
		File_h.LATITUDE_INTERVAL = lat_int;
		
		File_h.MAXIMUM_LATITUDE = sf.latMax;
		
		File_h.MAXIMUM_LONGITUDE = sf.lonMax;
		
		File_h.MINIMUM_LATITUDE = sf.latMin;
		
		File_h.MINIMUM_LONGITUDE = sf.lonMin;
		
		File_h.LENGTH_OF_FORMATION_ID = id_len;
		
		File_h.NUMBER_OF_LATITUDE_LINES = (int) ( ( sf.latMax - sf.latMin ) / lat_int );
		
		File_h.LINE_START_POSITIONS_IN_FILE = new int[ File_h.NUMBER_OF_LATITUDE_LINES ];
		
		File_h.LENGTH = 48 + 8*File_h.NUMBER_OF_LATITUDE_LINES;
		
	}
	
/**
 * Sets the header data and writes it to the file with the exception of the line offsets which have to be writen later
 * 
 * @param lat_int The latitude between successive lines 
 * @param id_len The length of the id data for each formation record in a line
 * @param sf The shape file used to build the pgrd
 * @throws IOException
 * 
 */
	
	private void writeHeader( double lat_int, int id_len, ShapeFile sf ) throws IOException {
		
		setHeaderData( lat_int, id_len, sf );
		
		putDoubleAt( (short) 0, (long) 0 , File_h.MINIMUM_LATITUDE );
		
		putDoubleAt( (short) 0, (long) 8 , File_h.MINIMUM_LONGITUDE );
		
		putDoubleAt( (short) 0, (long) 16 , File_h.MAXIMUM_LATITUDE );
		
		putDoubleAt( (short) 0, (long) 24 , File_h.MAXIMUM_LONGITUDE );
		
		putDoubleAt( (short) 0, (long) 32 , File_h.LATITUDE_INTERVAL );
		
		putIntAt( (short) 0, (long) 40, File_h.NUMBER_OF_LATITUDE_LINES );
		
		putIntAt( (short) 0, (long) 44, File_h.LENGTH_OF_FORMATION_ID );
		
		putIntAt( (short) 0, (long) 48, File_h.LINE_START_POSITIONS_IN_FILE );
		
		writeOffset += File_h.LENGTH;
	}
/*************************************** THE FUNCTIONS THAT DO THE WRITING OF THE FILE ************************************/
// pool variables for writeLine and writePart functions
	ArrayList< Integer > enclosingRecords = new ArrayList< Integer >();
	
	ArrayList< Float > intersections = new ArrayList< Float >();
	
	double latitude;
	
/**
 * Writes a latitude line of data to the growing pgrd file
 * 
 * @param elpp An {@link EqualLatitudePointPlace} instance built with the same {@link ShapeFile} object passed to this function
 * @param sf The ShapeFile where the data for writing originates
 * @param num_line The latitude line number starting from the smallest latitude northward
 *  
 */
 	
	protected void writeLine( ShapeFile sf, int num_line )	{
		
		enclosingRecords.clear();
		
		Line_h.clear();
		
		try {
		
			putLongAt( (short) 0, 40 + num_line*8, writeOffset );
		}
		catch ( IOException ioe ) {
			
			_error_log( " Error writing line offset to header at: line "+num_line );
			
		}
		
		latitude = File_h.MINIMUM_LATITUDE + num_line*File_h.LATITUDE_INTERVAL;
			
		enclosingRecords = elpp.getEnclosingBoxes( latitude );
		
		//set header data
		Line_h.START_OFFSET = writeOffset;
		Line_h.NUMBER_OF_FORMATIONS = enclosingRecords.size();
		
		Line_h.FORMATION_START_INDICES = new int[ enclosingRecords.size() ];
		
		writeOffset += Line_h.NUMBER_OF_FORMATIONS*4 + 8;
		
		Line_h.LENGTH += Line_h.NUMBER_OF_FORMATIONS*4 + 8;
		
		for( int i = 0 ; i < enclosingRecords.size(); i++ ) {	
			
			Line_h.FORMATION_START_INDICES[ i ] = Line_h.LENGTH;
			
			try {			
				
			Line_h.LENGTH += writeFormation( sf, enclosingRecords.get( i ).intValue(), latitude );
			}
			
			catch( PointPlaceException ppe ) {
				
				_error_log( " Failed to write a formation due to PointPlaceException at: line "+num_line+ ", record "+i );
				
				continue;
				
			}
			
		}
		
		try {
			
			putIntAt( (short) 0, Line_h.START_OFFSET, Line_h.LENGTH );
			
			putIntAt( (short) 0, Line_h.START_OFFSET + 4, Line_h.NUMBER_OF_FORMATIONS );
			
			putIntAt( (short) 0, Line_h.START_OFFSET + 8, Line_h.FORMATION_START_INDICES );
		}
		catch( IOException ioe) {
			
			_error_log(" Failed to write line header for line number "+num_line );
		}

	}
	
/**
 * Writes a formation to a line and its header 
 * 
 * @param elpp An {@link EqualLatitudePointPlace} instance built with the same {@link ShapeFile} object passed to this function
 * @param sf The ShapeFile where the data for writing originates
 * @param formationIndex The index of the formation in the shape file
 * @param latitude The latitude of the line to write
 * @return returns the length of the formation section just written to the file
 * @throws PointPlaceException
 * 
 */
//****************************************************CHECK THIS FUNCTION TOMORROW************************************************************************************************************	
	public int writeFormation( ShapeFile sf, int formationIndex, double latitude ) throws PointPlaceException  {
		
		intersections.clear();
		
		intersections = elpp.interiorSegments( sf, formationIndex , (float) latitude );
		
		Form_h.clear();
		
		Form_h.START_OFFSET = writeOffset;
		
		try {
			
			putIntAt( (short) 0, writeOffset, intersections.size() >> 1 );
		
			writeOffset += 4;
			
			putShortAt( (short) 0, writeOffset, (short) formationIndex );
			
			writeOffset += File_h.LENGTH_OF_FORMATION_ID;
		
			putFloatAt( (short) 0, writeOffset, intersections );
			
			writeOffset += intersections.size()*4;
			
		}
		catch( IOException ioe ) {
			
			throw new PointPlaceException();
		}
		
		return (int) (writeOffset - Form_h.START_OFFSET);
	}

	
/**
 * Builds the pgrd file
 * 
 * @param elpp An instance instantiated from the same {@link ShapeFile} instance passed to the sf argument
 * @param sf The ShapeFile instance based on which the pgrd is to be written
 * @param lat_interval The interval between successive latitude lines 
 * @throws FileBuildError
 * 
 */
	public void build( ShapeFile sf, double lat_interval, int formation_index_size ) throws FileBuildError {
		
		System.out.println(" Beginning pgrd build...");
		
		latitude = File_h.MINIMUM_LATITUDE;
		
		try {
			
			writeHeader( lat_interval, formation_index_size, sf );
		}
		catch( IOException ioe ) {
			
			throw new FileBuildError("Failed to write header.");
		}
		
		String progress_base =  " of " + File_h.NUMBER_OF_LATITUDE_LINES + " lines complete";
		
		for( int k = 0; k < File_h.NUMBER_OF_LATITUDE_LINES; k++ ) {
			
			if( k % 50 == 0 && k!=0)
				System.out.print( '\r' +" "+ k + progress_base );
			
			writeLine( sf, k+1 );
		}
		
		System.out.println('\r' + " Pgrd build complete!               ");
		
	}
	
}
