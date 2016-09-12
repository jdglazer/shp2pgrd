package compressor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import utils.FailedPgrdBuild;
import utils.FileModel;
import utils.RecordOutOfBoundsException;
import utils.ShortOverflow;

/**
 * 
 * A class to compress a .pgrd file to a minified version with loss in precision in the east-west direction
 * 
 * @author Joshua Glazer
 *
 */

public class PgrdMinifier extends FileModel {
	
/**
 * 
 * A class to model a pgrd segment from the pgrd file passed to the constructor 
 * 
 * @author joshua Glazer
 *
 */
	
	private class PgrdSegment {
		
		public float EAST_BOUND;
		
		public float WEST_BOUND;
		
		public int FORM_INDEX;
		
/**
 * Constructor
 * 
 * @param eb Easterly bound of segment as longitude
 * @param wb Westerly bound of segment as longitude
 * @param fi The index of the formation associated with the segment
 * 
 */
		
		public PgrdSegment( float eb, float wb, int fi) {
			
			EAST_BOUND = eb;
			
			WEST_BOUND = wb;
			
			FORM_INDEX = fi;
			
		}
	}
	
/**
 * 
 * A class to model a segment in the compress pgrd file
 * 
 * @author Joshua Glazer
 *
 */
	private class MinSegment {
		
		public float MIN_LON;
		
		public int NUM_POINTS;
		
		public int FORMATION_INDEX;
		
/**
 * 
 * Constructor
 * 
 * @param ml Longitude of the farthest west point.
 * @param np The number of points in the segment.
 * @param fi The index of the formation associated with the segment.
 * 
 */
		
		public MinSegment( float ml, int np, int fi ) {
			
			MIN_LON = ml;
			
			NUM_POINTS = np;
			
			FORMATION_INDEX = fi;
		}
		
	}
	
/**
 * 
 * A class to model a part of a given line
 * 
 * @author Joshua Glazer
 *
 */
	
	private class PgrdMinPart {
		
		public float START_LON;
		
		public int NUM_POINTS;
		
/**
 * 
 * Constructor.
 *  
 * @param start_lon The longitude of the western-most point in the part
 * @param num_points The total number of point in the part
 * 
 */
		
		public PgrdMinPart( float start_lon, int num_points ) {
			
			START_LON = start_lon;
			
			NUM_POINTS = num_points;
			
		}
		
	}
	
/**
 * 
 * A class to generate and store all information for a line in the compressed pgrd from the uncompressed pgrd passed to PgrdMinifier class 
 * 
 * @author joshua
 *
 */
	
	private class PgrdMinLine {
				
		public ArrayList<Integer> startParts;
		
		public ArrayList<Integer> overflows;
		
		public ArrayList<Integer> overflowIndices;
		
		public int lineIndex;
		
		public int LINE_SIZE;
		
		public int NUM_PARTS;
		
		public int [] PART_OFFSETS;
		
		public ArrayList<MinSegment> segments;
		
		public ArrayList<PgrdMinPart> PARTS;
		
/**
 * 
 * Constructor.
 * 
 * @param lineIndex The index of the line in the pgrd file to build compressed model for.
 * 
 * @param pSegs The array list of PgrdSegment's sorted from east to west ( with no overlapping segments allowed! )
 * 
 */
		
		public PgrdMinLine( int lineIndex, ArrayList<PgrdSegment> pSegs ) {
			
			this.lineIndex = lineIndex;
			
			startParts = new ArrayList<Integer>();
			
			segments = new ArrayList<MinSegment>();
			
			PARTS = new ArrayList<PgrdMinPart>();
			
			overflows = new ArrayList<Integer>();
			
			overflowIndices = new ArrayList<Integer>();
			
			fillSegments( pSegs );
			
			LINE_SIZE = getLineSize();
				
		}
/**
 * 
 * A function to clear object an rebuild line model
 * 
 * @param lineIndex The index of the line in the pgrd file to build compressed model for.
 * @param pSegs The array list of PgrdSegment's sorted from east to west ( with no overlapping segments allowed! )
 * 
 */
		
		public void build( int lineIndex, ArrayList<PgrdSegment> pSegs ) {
			
			clear();
			
			this.lineIndex = lineIndex;
			
			fillSegments( pSegs );
			
			LINE_SIZE = getLineSize();
			
		}
		
/**
 * 
 * A function to build the entire model for the Pgrd line short of the starting position in the file of the line
 * 
 * @param pSegs The sorted ArrayList of PgrdSegments for the line of interest (sorted from west to east).
 * 
 */
		
		private void fillSegments( ArrayList<PgrdSegment> pSegs ) {
			
			if( pSegs.size() > 0)
				
				startParts.add( new Integer( 0 ) );
			
			for( int l = 0; l + 1 < pSegs.size(); l++ ) {
				
				if( ( pSegs.get( l+1 ).WEST_BOUND - pSegs.get( l ).EAST_BOUND ) > longitudeInterval ) {
					
					startParts.add( new Integer( l+1 ) );
					
				}

			}
			
			segmentsToGrid( pSegs );
			
			NUM_PARTS = startParts.size();
			
			partBuilder();
			
		}
		
/**
 * 
 * A function to build a grid of points for a specific set of PgrdSegments
 * 
 * @param pSegs A ArrayList sorted from west to east of PgrdSegments ( must not overlap! )
 * 
 */
		
		private void segmentsToGrid( ArrayList<PgrdSegment> pSegs ) {
			
			float lastWestBound = 0 ;
			
			int numPoints = 0;
			
			for( int l = 0; l < pSegs.size(); l++ ) {
				
				if( newPart( l ) ) {
					
					lastWestBound = pSegs.get(l).WEST_BOUND;
				}					
				
				numPoints = (int) Math.floor( ( pSegs.get( l ).EAST_BOUND - lastWestBound )/longitudeInterval );
				
				segments.add( new MinSegment( lastWestBound+(float)longitudeInterval, numPoints, pSegs.get(l).FORM_INDEX ) );
				
				lastWestBound = (  numPoints* (float) longitudeInterval) + lastWestBound ; 

			}
			
		}
		
/**
 * 
 * A function store meta data for parts of a line
 * 
 */
		
		private void partBuilder() {
			
			for( int m = 0; m < NUM_PARTS; m++ ) {
				
				int upperBound, lowerBound, numPoints = 0;
				
				lowerBound = startParts.get( m ).intValue();
				
				if( m + 1 == startParts.size() ) {
					
					upperBound = segments.size();
				}
				else {
					upperBound = startParts.get( m + 1 ).intValue();
				}
				
				
				for( int n = lowerBound ; n < upperBound; n++ ) {
					
						numPoints += segments.get( n ).NUM_POINTS;
						
						if( ShortOverflow.overflowIndex( segments.get( n ).NUM_POINTS ) > 0 ) {
							
							overflows.add( new Integer( n ) );
							
							overflowIndices.add(
									
									new Integer( 
											
											ShortOverflow.overflowIndex( 
													
													segments.get( n ).NUM_POINTS
													
													)
													
											) 
									);
							
						}
				}
					
				float minLon = segments.get( startParts.get( m ).intValue() ).MIN_LON;
				
				PgrdMinPart pmp = new PgrdMinPart( minLon, numPoints );

				PARTS.add( pmp );
				
			}
		}
		
/**
 * 
 * Determines whether a given segment of the sorted PgrdSegment array list represents the wester-most start of a new part
 * 
 * @param index The index of the PgrdSegment in the array list
 * @return True if the index corresponds to the wester-most segment in a part of the line, false otherwise
 * 
 */
		
		private boolean newPart( int index ) {
			
			boolean ret = false;
			
			for( int k = 0; k < startParts.size(); k++ ) {
				
				if( startParts.get(k).intValue() == index ) {
					
					ret = true;
					
					break;
				}
				
			}
			
			return ret;
		}
		
/**
 * 
 * A function to get the number of parts in a given segment
 * 
 * @param partNum The index of the part
 * @return The number of parts in a segment or -1 if invalid argument is given
 * 
 */
		
		public int getNumSegments( int partNum ) {
			
			if( partNum < ( NUM_PARTS - 1 ) && partNum >= 0 ) {
				
				return startParts.get( partNum + 1 ).intValue() - startParts.get( partNum ).intValue();
				
			}
			else if( partNum + 1 == NUM_PARTS ) {
				
				return segments.size() - startParts.get( partNum ).intValue();
				
			}
			else
				return -1;
			
		}
		
		private int getLineSize() {
			
			return 12+NUM_PARTS*12+segments.size()*4+overflows.size()*6;
			
			
		}
		
/**
 * 
 * clears latitude line model stored in objects properties
 * 
 */
		
		public void clear() {
			
			LINE_SIZE = 0;
			
			NUM_PARTS = 0;
			
			PART_OFFSETS = null;
			
			PARTS.clear();
			
			startParts.clear();
			
			segments.clear();
			
			overflows.clear();
			
			overflowIndices.clear();
			
		}
		
	}
	
/**
 * 
 * Maintains the write offset in the new file being built
 * 
 */
	
	private long writeOffset;
	
/**
 * 
 * Stores the error_log file writtinf buffer
 * 
 */
	
	private BufferedWriter error_log;
	
/**
 * 
 * Stores the PgrdFile associate with the .pgrd to be converted
 * 
 */
	
	private PgrdFile pgrd;
	
/**
 * 
 * The longitude interval between neighboring points
 * 
 */
	
	private double longitudeInterval = .00018;
	
	//pool/singleton variables
	
/**
 * 
 * An array list to keep an ordered list of all of the segments of a single latitude line in pgrd file ( ordered from west to east )
 * 
 */
	
	private ArrayList< PgrdSegment > orderedLineSegments= new ArrayList< PgrdSegment >();
	
/**
 * 
 * A singleton instance to store a model of compressed latitude line from PgrdFile
 * 
 */
	
	private PgrdMinLine minLine;
	
/**
 * 
 * Constructor
 * 
 * @param pgrdFile An instance of a pgrdFile to be compressed 
 * @param pgrdCompressedFile The name of the compressed file without extensions
 * @param errorLogFile The name with a .txt extension of the error log file. File will be created if it doesn't exist
 * @throws FileNotFoundException
 * @throws IOException
 * 
 */
	public PgrdMinifier( PgrdFile pgrdFile, String pgrdCompressedFile, String errorLogFile ) throws FileNotFoundException, IOException {
		
		super( new RandomAccessFile[] {
				
				new RandomAccessFile( pgrdCompressedFile+".min.pgrd", "rw")
		} );
		
		error_log = new BufferedWriter( new FileWriter( new File( errorLogFile ), true ) );
		
		pgrd = pgrdFile;
		
		writeOffset = 0L;
	}
	
/**
 * 
 * A function to build and write the header of the compressed .pgrd file.
 * 
 * @param longitudeInterval The longitude interval between neighboring points in the compressed file
 * @return True if the header was successfully written
 * 
 */
	private boolean buildCompressedHeader( double longitudeInterval ) {
		
		this.longitudeInterval = longitudeInterval;
		
		double [] headerDoubles = new double[] { pgrd.MINIMUM_LATITUDE,
												 pgrd.MINIMUM_LONGITUDE,
												 pgrd.MAXIMUM_LATITUDE,
												 pgrd.MAXIMUM_LONGITUDE,
												 pgrd.LATITUDE_INTERVAL,
												 longitudeInterval 	};
		
		int [] headerInts = new int[2 + pgrd.NUMBER_LATITUDE_LINES]; 
		
		Arrays.fill( headerInts, 0 );
		
		headerInts[0] = pgrd.NUMBER_LATITUDE_LINES;
		
		headerInts[1] = pgrd.TYPE_OF_SEGMENT_IDENTIFIER;
		
		try {
			
			putDoubleAt( (short) 0, 0, headerDoubles );
			
			i( 48 );
			
		} catch( IOException ioe ) {
			
			_error_log( "Error, could not write first 48 bytes of file header." );
			
			return false;
			
		}
		
		try {
			
			putIntAt( (short) 0, writeOffset, headerInts );
			
		} catch( IOException ioe ) {
			
			_error_log( "Error, could not write following bytes of header: 47 - "+(pgrd.NUMBER_LATITUDE_LINES*4)+56 );
			
			return false; 
			
		}

		i( 8 + pgrd.NUMBER_LATITUDE_LINES*4 );
		
		return true;
		
	}
	
/**
 * 
 * A function order the segments of a given line in the pgrd file passed to the constructor.
 * 
 * @param lineIndex The index of the line to pull segments from
 * @return True if the ordering was successful, false if not
 * 
 */
	
	private boolean orderLineSegments ( int lineIndex ) {
		
		if( lineIndex >= pgrd.NUMBER_LATITUDE_LINES || lineIndex < 0 ) {
			
			return false;
			
		}
		
		int numFormations = 0;
		
		try {
			
			numFormations = pgrd.numberFormations( lineIndex );
		} 
		catch( RecordOutOfBoundsException robe ) {
			
			_error_log( "line "+lineIndex+": Failed to write line due to an invalid index passed to numberFormations function" );
			
			return false;
		}
		
		orderedLineSegments.clear();
		
		
		for( int i = 0; i < numFormations ; i++ ) {
			
			//FIND A WAY TO STORE GEO FORMATION INDEX IN PARALLEL WITH THE SEGMENTS
			
			int numSegments = 0;
			
			int ind;
			
			try {
				
				numSegments = pgrd.numberFormationSegments( lineIndex, i );
				
				ind = pgrd.getFormationIndex(lineIndex, i);
			}
			catch( RecordOutOfBoundsException robe ) {
				
				_error_log( "Failed to determine the number of segments for line "+lineIndex+", formation "+i+"." );
				
				continue;
				
			}
			
			for( int j = 0; j < numSegments ; j++ ) {
				
				float [] f;
				
				try {
					
					f = pgrd.getSegmentBounds( lineIndex, i, j );
					
					
				}
				catch( RecordOutOfBoundsException robe ) {
					
					_error_log( "line "+lineIndex+", formation "+i+", segment "+j+": Invalid indice passed to getSegmentBounds" );
					
					continue;
				}
				
				orderedLineSegments.add( new PgrdSegment( f[1], f[0], ind ) );				
			}
			
			Collections.sort( orderedLineSegments, new Comparator< PgrdSegment >() {
				
				@Override
				public int compare( PgrdSegment s1, PgrdSegment s2 ) {
					
					return Float.compare(s1.EAST_BOUND, s2.EAST_BOUND);
					
				}
				
			});
						
		}
		
		return true;
		
	}

/**
 * 
 * A function build a compressed model of a latitude line in a pgrd file and write line to parent file
 * 
 * @param lineIndex The index of the line in the pgrd file to build a model for
 * 
 */
	
	protected boolean writeCompressedLine( int lineIndex ) {
		
		int startLineOffset = (int) writeOffset;
		
		orderLineSegments( lineIndex );
		
		if( minLine != null ) {
			
			minLine.clear();
			
			minLine.build( lineIndex, orderedLineSegments );
		}
		else
			minLine = new PgrdMinLine( lineIndex, orderedLineSegments );
		
		try {
			
			//Write line offset in file header
			putIntAt( (short)0, 56 + 4*lineIndex, (int) writeOffset );
			
			//write length of line in bytes in line header
			putIntAt( (short)0, writeOffset, minLine.LINE_SIZE );
			
			i( 4 );
			
			//write number of parts to line header
			putIntAt( (short)0, writeOffset, minLine.NUM_PARTS );
			
			i( 4 );
			
			//write the number of short overflows in segment lengths
			putIntAt( (short)0, writeOffset, minLine.overflows.size() );
			
			i( 4 );
			
			//write array of segments indices in line header with short value overflow condition met (value of number of points > 32767 )
			if( minLine.overflows.size() > 0 ) {
				
				for( int l = 0; l < minLine.overflows.size(); l++ ) {
					
					putIntAt( (short)0, writeOffset, minLine.overflows.get( l ).intValue() );
					
					i( 4 );
					
					putShortAt( (short)0, writeOffset, minLine.overflowIndices.get( l ).shortValue() );
					
					i( 2 );
					
					
				}
			}
			
			//writes array to store offsets in line for each part
			int [] blank = new int[ minLine.NUM_PARTS ];
			
			Arrays.fill( blank, 0 );
			
			putIntAt( (short)0, writeOffset, blank );
			
			i( minLine.NUM_PARTS*4 );
			
			//write parts 
			for( int j = 0; j < minLine.NUM_PARTS; j++ ) {
				
				//writes start position in line of each part
				putIntAt( (short) 0, startLineOffset + 12 + 4*j + 6*minLine.overflows.size(), (int) writeOffset - startLineOffset );
				
				//write starting longitude in part header
				putFloatAt( (short) 0, writeOffset, minLine.PARTS.get( j ).START_LON );
					
				
				i( 4 );
				
				//writes the number of points in a given part
				putIntAt( (short)0, writeOffset, minLine.PARTS.get( j ).NUM_POINTS );
				
				i( 4 );
				
				int startIndex = minLine.startParts.get( j ).intValue();
				
				int endIndex = startIndex + minLine.getNumSegments( j );
				
				for( int k = startIndex ; k < endIndex; k++) {
					
					//writes formation index
					putShortAt( (short) 0, writeOffset, (short) minLine.segments.get( k ).FORMATION_INDEX );
					
					i( 2 );
					
					//writes number of points formation lives for
					putShortAt( (short) 0, writeOffset, (short) ShortOverflow.overflowValue( minLine.segments.get( k ).NUM_POINTS ) );
					
					i( 2 );
					
				}
				
			}
			
		}
		catch( IOException ioe ) {
			
			_error_log( "Failed to write line number "+lineIndex+"\n" );
			
			return false;
			
		}
		
		return true;
	}
	
	public void build( float longInt, PgrdMinifierEvent pme ) throws FailedPgrdBuild {
		
		int num_lines = pgrd.NUMBER_LATITUDE_LINES;
		
		buildCompressedHeader( longInt );
		
		pme.onStartFileWrite();
		
		for( int a = 0 ; a < num_lines; a++ ) {

			if( !writeCompressedLine( a ) )
				
				throw new FailedPgrdBuild();
			
			
			pme.onLineWriteComplete( a, num_lines );
		}
		
		pme.onFinishFileWrite();	
	}
	
/**
 * 
 * A function to succinctly change the offset of the write position in compressed file.
 * 
 * @param a The added offset to change the file offset by
 * 
 */
	
	protected void i( int a ) {
		
		writeOffset += a;
	}
	
/**
 * 
 * A bus function used to capture and record all errors associated with thrown exceptions in the process of building the file
 * 
 * @param error_description A string describing the error
 * 
 */
		
	private void _error_log( String error_description ) {
		
		try {
			
			error_log.write( error_description + "\n" );
			
			error_log.flush();
			
		}
		catch( IOException ioe ) {}
		
	}
	
/**
 * 
 *  A tester function for testing objects private and/or protected members
 *  	
 * @param num
 * 
 */
	
	public void tester(int num) {
		
		writeCompressedLine( num );
		
	}		
		
}
