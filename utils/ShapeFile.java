package utils;
import java.io.*;


public class ShapeFile extends FileModel {
	
/**
 * stores the valid main shapefile extension
 * 
 */
	
	public final String SHP_EXTENSION = ".shp";
	
/**
 * stores the valid shape index file extension
 * 
 */
	
	public final String SHX_EXTENSION = ".shx";
	
/**
 * stores the valid shape database file extension
 * 
 */
	
	public final String DBF_EXTENSION = ".dbf";
	
/**
 * stores the ESRI code that should be found at the top of the shapefile, and it's index file
 * 
 */
	
	public final int ESRI_CODE = 9994;
	
	//reusable pool variables (prevents overcrowding of memory)
	
	private int recOff, recOff1;
	
	//File Meta variables	
	
/**
 * The ESRI Shapefile version declared in the file
 * 
 */
	
	public int version;
	
/**
 * The index associated with the shape type declared in the file
 *  
 */
	
	public int shapeType;
	
/**
 * The minimum latitude found in the shapefile
 * 
 */
	
	public double latMin;
	
/**
 * The minimum longitude found in the shapefile
 * 
 */
	
	public double lonMin;
				   
/**
 * 
 * The maximum latitude found in the shapefile
 * 
 */
	
	public double latMax;
				   
/**
 * The maximum longitude found in the shapefile
 * 
 */

	public double lonMax;
	
/**
 * Takes the names of the associated .shp, .shx, and .dbf files separately. It is important to
 * pass in the appropriately associated files to ensure the proper functioning of this object. This
 * constructor allows for differences between the names of the main, index and database files.
 * 
 * @param shp The file address of the main shape file
 * @param shx The file address of the shape index file 
 * @param dbf The file address of the shape database file
 * @throws InvalidFileTypeException
 * @throws FileNotFoundException
 * @throws IOException
 * @throws RecordOutOfBoundsException
 * 
 */

	public ShapeFile(String shp, String shx, String dbf) throws InvalidFileTypeException, 
																FileNotFoundException, 
																IOException, 
																RecordOutOfBoundsException {
		
		super(new FileInputStream[]{new FileInputStream(shp), new FileInputStream(shx), new FileInputStream(dbf)});
		
		verifyFileExt(shp.trim(), SHP_EXTENSION);
		
		verifyFileExt(shx.trim(), SHX_EXTENSION);
		
		verifyFileExt(dbf.trim(), DBF_EXTENSION); 
		
		if(!hasESRICode())
			
			throw new InvalidFileTypeException("no ESRI Code found (9994)");		
		
		writeFileMeta();
		
	}
	
/**
 * Takes the address and name of the shapefile (without any extension) and assumes associated 
 * database and index files have the same name in keeping with convention set forth in ESRI shapefile specifications
 * 
 * @param name
 * @throws InvalidFileTypeException
 * @throws FileNotFoundException
 * @throws IOException
 * @throws RecordOutOfBoundsException
 * 
 */
	
	public ShapeFile(String name) throws InvalidFileTypeException, FileNotFoundException, IOException, RecordOutOfBoundsException {
		
		this(name.trim()+".shp", name.trim()+".shx", name.trim()+".dbf");
	}
	
	//A function to get the index of the relevant shape file input stream component in the parent's fis list array
	
	private short fileIndex(String extension) {
		
		switch(extension) {
		
			case SHX_EXTENSION: return 1;
			
			case DBF_EXTENSION: return 2;
			
			default: return 0;
		}
	}
	
	//A function to verify esri 9994 code
	private boolean hasESRICode() throws IOException {
		
		return getIntFrom(  fileIndex( SHP_EXTENSION ), B_END,  0 ) == ESRI_CODE;
	}
	
	//A function to write file meta data
	private void writeFileMeta() throws IOException {
		
		fileLength = getIntFrom( fileIndex( SHP_EXTENSION ) , B_END, 24 );
		
		version = getIntFrom( fileIndex( SHP_EXTENSION ), L_END, 28 );
		
		shapeType = getIntFrom( fileIndex( SHP_EXTENSION ), L_END, 32 );
		
		lonMin = getDoubleFrom( fileIndex( SHP_EXTENSION ), L_END, 36 );
		
		latMin = getDoubleFrom( fileIndex( SHP_EXTENSION ), L_END, 44 );
		
		lonMax = getDoubleFrom( fileIndex( SHP_EXTENSION ), L_END, 52);
		
		latMax = getDoubleFrom( fileIndex( SHP_EXTENSION ), L_END, 60 );		
		
	}
	
	//a function to the offset of a given record in the file
	//CHANGE BACK TO PRIVATE ************************************************************************************************************
	public int recordOffset(int indexOfRec) throws IOException, RecordOutOfBoundsException {
		
		if(indexOfRec >= recordCount() || 0 > indexOfRec)
			
			throw new RecordOutOfBoundsException();
		
		return getIntFrom( fileIndex(SHX_EXTENSION), B_END, 100+indexOfRec*8 ) * 2;
		
	}
	
	//A function make sure the point offset is valid for a given part of a record
	private void _vPointOffset(int recordIndex, int partIndex, int pointIndex) throws RecordOutOfBoundsException, IOException{
		
		if( partLength(recordIndex, partIndex) <= pointIndex || pointIndex < 0)
			
			throw new RecordOutOfBoundsException();
	}
	
/**
 * throws an exception if an invalid record index is passed in
 * 
 * @param recordIndex
 * @throws RecordOutOfBoundsException
 * @throws IOException
 * 
 */
	
	private void _vRecordIndex(int recordIndex) throws RecordOutOfBoundsException, IOException {
		
		if( recordIndex >= recordCount() || recordIndex < 0 )
			
			throw new RecordOutOfBoundsException();
	}
	
/**
 * 
 * gets the offset of a specified part in a record
 * 
 * @param recordIndex THe index of the shape file record (starts at 0)
 * @param partIndex The index of the record part 
 * @return An integer offset of a part in the file 
 * @throws RecordOutOfBoundsException
 * @throws IOException
 * 
 */
	
	private int partOffset(int recordIndex, int partIndex) throws RecordOutOfBoundsException, IOException {
		
		if(partCount(recordIndex) <= partIndex || partIndex < 0)
			
			throw new RecordOutOfBoundsException();
		
		recOff1 = recordOffset(recordIndex) + 52;
		
		return recOff1 + ( getIntFrom( fileIndex( SHP_EXTENSION ) , L_END, recOff1+(partIndex*4))*16 ) + ( partCount( recordIndex )*4 );
		
		
	}

	/**
	 * 
	 * @param recordIndex
	 * @return
	 * @throws RecordOutOfBoundsException
	 * @throws IOException
	 */
	private double getCoorExtrema(int recordIndex, int extrema_offset) throws RecordOutOfBoundsException, IOException {
		
		_vRecordIndex(recordIndex);
		
		return getDoubleFrom( fileIndex( SHP_EXTENSION ), 
							  L_END, 
							  recordOffset( recordIndex ) + 12 + extrema_offset );
	}
	
/**
 * Gets the total number of records in the .shp file
 * 
 * @return integer number of records in the .shp file
 * @throws IOException
 * 
 */
	public int recordCount() throws IOException {
		
		return (int) (mapped_list[ fileIndex( SHX_EXTENSION ) ].capacity() - 100) / 8;
	}
	
/**
 * Determines the length of a given record 
 * 
 * @param recordIndex The index of a record
 * @return integer Length of record in bytes (not including 8 byte record header)
 * @throws IOException
 * 
 */
	
	public int recordLength(int recordIndex) throws IOException {
		
		return getIntFrom( fileIndex( SHX_EXTENSION ), B_END, 104 + 8*recordIndex );
	}
	
	
/**
 * Determines the number of parts in a polygon record header
 * 
 * @param recordIndex The index of the record in the file (starts at index of 0)
 * @return Integer number of parts in the record
 * @throws IOException
 * @throws RecordOutOfBoundsException if an invalid recordIndex argument is supplied
 * 
 */
	
	public int partCount( int recordIndex ) throws IOException, RecordOutOfBoundsException {
		
		return getIntFrom( fileIndex(  SHP_EXTENSION ), L_END, recordOffset( recordIndex )+8+36 );
		
	}
	
/**
 * Gets the length of a part of a record in points
 * 
 * @param recordIndex The record index (starts at index of 0)
 * @param partIndex The index of the part (starts at index of 0)
 * @return the length of a part in 16 byte latitude and longitude points
 * @throws IOException
 * @throws RecordOutOfBoundsException Invalid partIndex or recordIndex arguments supplied
 * 
 */
	
	public int partLength(int recordIndex, int partIndex) throws IOException, RecordOutOfBoundsException {
		
		if( partCount(recordIndex) <= partIndex || 0 > partIndex )
			
			throw new RecordOutOfBoundsException();
		
		recOff = recordOffset(recordIndex);
		
		if( partCount( recordIndex ) == 1 ) 
						
			return getIntFrom( fileIndex(  SHP_EXTENSION ), L_END, recOff+48 );
		
		
		if( partCount( recordIndex ) == partIndex+1 ) 
			
			 return getIntFrom( fileIndex( SHP_EXTENSION ), L_END, recOff+48 ) - 
					 getIntFrom( fileIndex(  SHP_EXTENSION ), L_END, recOff+52+partIndex*4 );
		
		else 
			
			return getIntFrom( fileIndex( SHP_EXTENSION ), L_END, recOff+56+partIndex*4 ) - 
			  			getIntFrom( fileIndex( SHP_EXTENSION ), L_END, recOff+52+partIndex*4 );
	
	}
	
/**
 * Gets a specified pair of coordinates from a part of a record. 
 * 
 * @param recordIndex The index of the record (starts at 0)
 * @param partIndex The index of the part (starts at 0)
 * @param pointIndex The index of the point (starts at 0)
 * @return Two part double array contain the latitude of the point in the first part and the longitude in the second 
 * 			(ie. getLatLon(...)[0] for latitude, getLatLon(...)[1] for longitude)
 * @throws RecordOutOfBoundsException Invalid recordIndex, partIndex, and/or pointIndex argument(s) supplied
 * @throws IOException
 * 
 */
	
	public float [] getLatLon(int recordIndex, int partIndex, int pointIndex) throws RecordOutOfBoundsException, IOException {
		
		_vPointOffset(recordIndex, partIndex, pointIndex);
		
		recOff = partOffset( recordIndex, partIndex );
		
		recOff += pointIndex*16;
		
		return new float[]{
				
				(float) getDoubleFrom( fileIndex(  SHP_EXTENSION ), L_END, recOff + 8 ),
				
				(float) getDoubleFrom( fileIndex(  SHP_EXTENSION ), L_END, recOff )
				
				};
		
	} 
	
/**
 * Gets the minimum latitude extreme for a given record
 * 
 * @param recordIndex
 * @return the minimum latitude of a record
 * @throws RecordOutOfBoundsException
 * @throws IOException
 * 
 */
	
	public double minLat(int recordIndex) throws RecordOutOfBoundsException, IOException {
		
		return getCoorExtrema(recordIndex, 8);
	}
	
/**
 * Gets the maximum latitude extreme for a given record
 * 
 * @param recordIndex
 * @return the maximum latitude of a record
 * @throws RecordOutOfBoundsException
 * @throws IOException
 * 
 */
	
	public double maxLat(int recordIndex) throws RecordOutOfBoundsException, IOException {
		
		return getCoorExtrema(recordIndex, 24);
	}
	
/**
 * Gets the minimum longitude extreme for a given record
 * 
 * @param recordIndex
 * @return returns the minimum longitude of a given record 
 * @throws RecordOutOfBoundsException
 * @throws IOException
 * 
 */
	
	public double minLon(int recordIndex) throws RecordOutOfBoundsException, IOException {
		
		return getCoorExtrema(recordIndex, 0);
	}
	
/**
 * Gets the maximum longitude extreme for a given record
 * 
 * @param recordIndex
 * @return returns the maximum longitude of a record
 * @throws RecordOutOfBoundsException
 * @throws IOException
 * 
 */
	
	public double maxLon(int recordIndex) throws RecordOutOfBoundsException, IOException {
		
		return getCoorExtrema(recordIndex, 16);
	}
	
}
