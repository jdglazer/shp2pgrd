package utils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.io.*;
import java.util.*;

public class FileModel {

// constant properties
	protected final String B_END = "b_end";
	
	protected final String L_END = "l_end";
	
/**
 * stores the length in bytes of the main shape file (*.shp)
 *
 */
	public int fileLength;
	
	
/**
 * An array containing all {@link FileOutputStream} objects to which the object will write data
 */
	
	protected RandomAccessFile [] raf_list;
	
/**
 * An array containing a mapped byte buffer for complex reading (and perhaps later writing operation)
 */
	protected MappedByteBuffer [] mapped_list;
	
	//set of reusable pool variables (prevents overcrowding of memory)
	protected byte [] pool_byteArray;
	
	protected ByteBuffer pool_ByteBuffer;
	
	private short closeIterator;
	
/**
 * Constructor for using the object for reading from a file
 * 
 * @param fis_add[] An array containing all file input streams 
 * 
 */
	
	public FileModel( FileInputStream [] fis_add ) throws IOException {
		
		this();
	
		mapped_list = new MappedByteBuffer[ fis_add.length ];
			
		for( int m = 0; m < fis_add.length; m++ )  
				
			mapped_list[ m ] = fis_add[ m ].getChannel().map( MapMode.READ_ONLY, 0,  fis_add[ m ].getChannel().size() );

				
		}
		

/**
 * Constructor for using the object for writing to a file
 * 
 * @param fos_add
 * 
 */
	
	public FileModel( RandomAccessFile [] raf_add ) {
		
		this();
		
		raf_list = raf_add;
		
		
	}

/**
 * A private constructor used for performing necessary instantiations in all public constructors
 * 
 */
	
	private FileModel() {
		
		pool_byteArray = new byte[50];
		
		pool_ByteBuffer = ByteBuffer.allocate(100);
		
		closeIterator = 0;
	}

/**
 * writes a specified number of bytes from a byte array to a file output stream
 * 
 * @param fos_index The index of the file output stream to write to
 * @param b The byte array from which data is written
 * @param len The number of bytes from beginning of array to write
 * @throws IOException
 * 
 */

	/*private void write(int fos_index, byte [] b, int len) throws IOException {
		
		fos_list[ fos_index ].write(b, 0, len);
	}*/
	
/**
 * prepares the byte buffer for reading values by either little or big endian
 * 
 * @param order A string indicating the byte order (L_END for Little Endian or anything else for Big Endian
 * 
 */
	private void setBuffer(String order) {
		
		pool_ByteBuffer.rewind();
		
		if( order.equals( L_END ) )
			
			pool_ByteBuffer.order( ByteOrder.LITTLE_ENDIAN );
		
		else 
			
			pool_ByteBuffer.order( ByteOrder.BIG_ENDIAN );
		
	}

/**
 * A function to close all file output streams 
 * 
 */
	
	private void closeRaf() {
		
		try {
			
			for(; closeIterator < raf_list.length; closeIterator++)
				
					raf_list[closeIterator].close();
			
		} catch(IOException ioe) {
			
			closeIterator++;
			
			closeRaf();
			
		}
		
	}
	
/**
 * Makes sure valid file extension are found
 * 
 * @param file_name The full name of the file
 * @param file_extension The file extension (with the period included)
 * @throws InvalidFileTypeException
 * 
 */
	
	protected void verifyFileExt( String file_name, String file_extension ) throws InvalidFileTypeException {
		
		if(!file_name.substring(file_name.length() - file_extension.length()).toLowerCase().equals(file_extension))
			
			throw new InvalidFileTypeException("INVALID EXTENSION");
	
	}
	
/**
 * 
 * Gets the read positions in the specified random access file
 * @param raf_num The index in the array of Random Access Files stored in the object
 * @return The read position in the random access file
 * @throws IOException
 * 
 */
	
	protected long getWOffset( short raf_num ) throws IOException {
		return raf_list[ raf_num ].getChannel().position();
	}

/**
 * 
 * Sets the read order of a specified random access file
 * 
 * @param mbbIndex The index (starting at 0) of the random access file to make changes to
 * @param order The read order (L_END or B_END)
 * 
 */
	
	protected void readOrder( int mbbIndex, String order ){
		
		if( order.equals(L_END) )
			
			mapped_list[ mbbIndex ].order( ByteOrder.LITTLE_ENDIAN ); 
		
		else 
			
			mapped_list[ mbbIndex ].order( ByteOrder.BIG_ENDIAN );
			
	}

/**
 * 
 * Gets a short from a specified position in a file
 * 
 * @param mbbIndex The index of the random access file to query
 * @param order The endian order of the read
 * @param offset The offset (in bytes) in the file from the start of the file to begin read from
 * @return Short value at specified offset
 * 
 */
	
	protected short getShortFrom( short mbbIndex, String order, long offset ) {
		
		readOrder( mbbIndex , order );
		
		return mapped_list[ mbbIndex ].getShort( (int) offset );
	} 
	
/**
 * Gets either the Little or Big Endian integer from a specified position in one of the file input streams
 * 
 * @param fisIndex The index of the {@link FileInputStream} from which the file data is drawn
 * @param order Either L_END or B_END for Little Endian or Big Endian, respectively
 * @param offset The offset in the file from which to begin the read
 * @return Four byte integer at the specified offset
 * 
 */
	
	protected int getIntFrom( short mbbIndex, String order, long offset ) {
		
		readOrder( mbbIndex, order );
		
		return mapped_list[ mbbIndex ].getInt( (int) offset );

	}
	
/**
 * 
 * Gets a long from a file 
 * 
 * @param mbbIndex The index of the random access file 
 * @param order The endian order (L_END or B_END)
 * @param offset The offset (in bytes) from the start of the file
 * @return A long value from a file
 * 
 */
	
	protected long getLongFrom( short mbbIndex, String order, long offset ) {
		
		readOrder( mbbIndex, order );
		
		return mapped_list[ mbbIndex ].getLong( (int) offset );
	}

	protected float getFloatFrom( short mbbIndex, String order, long offset ) {
		
		readOrder( mbbIndex, order );
		
		return mapped_list[ mbbIndex ].getFloat( (int) offset );
	}
/**
 * Gets either the Little or Big Endian double from a specified position in one of the file input streams
 * 
 * @param fisIndex The index of the {@link FileInputStream} from which the data is drawn
 * @param order Either L_END or B_END for Little Endian or Big Endian, respectively
 * @param offset The offset in the file from which to begin the read
 * @return Eight byte double at the specified offset
 * @throws IOException
 * 
 */
	
	protected double getDoubleFrom( short mbbIndex, String order, long offset ) throws IOException {
		
		readOrder( mbbIndex, order );
		
		return mapped_list[ mbbIndex ].getDouble( (int) offset );
	}

/**
 * Gets an int from a bufferized version of the file
 * 
 * @param index The offset in the file in bytes
 * @return An int from the specified index in the file
 * 
 */
	
	protected int getInt( int index ) {
		
		return mapped_list[ 0 ].getInt( index );
	}

/**
 * Gets an int from a specified map byte buffer from a specified offset in the file
 * 
 * @param index The offset in the file in bytes
 * @return An int from the specified index in the file
 * 
 */
	
	protected int getInt( int mbbIndex, int index ) {
		
		return mapped_list[ mbbIndex ].getInt( index );
	}
	
	protected void putShortAt( short rafIndex, long offset, short value ) throws IOException {

		raf_list[ rafIndex ].seek( offset );
		
		raf_list[ rafIndex ].writeShort( value );	
	}
	
	protected long getFileLength() throws IOException {
		return raf_list[0].length();
	}
/**
 * A function to write an integer to a specified offset in a file output stream
 * 
 * @param rafIndex The index associated with the random access file
 * @param order The byte order. L_END for Little Endian and B_End for Big Endian
 * @param offset The offset in the file write or overwrite
 * @param value The integer value to write to the file
 * @throws IOException
 * 
 */

	protected void putIntAt( short rafIndex, long offset, int value ) throws IOException {
		
		raf_list[ rafIndex ].seek( offset );
		
		raf_list[ rafIndex ].writeInt( value );	
	}

/**
 * puts an array of integers into the file starting at a specified offset
 * 
 * @param rafIndex The index associate with the random access
 * @param offset The offset in the file from the start
 * @param values[] The set of integers to write in
 * @throws IOException
 * 
 */
	
	protected void putIntAt( short rafIndex, long offset, int [] values ) throws IOException {
		
		for(int j = 0; j < values.length; j++) 
			
			putIntAt( rafIndex, offset+j*4, values[j] );
		
	}

/**
 * puts an ArrayList of integers into the file starting at a specified offset
 * 
 * @param rafIndex The index associate with the random access file
 * @param offset The offset in the file from the start
 * @param values The ArrayList containing the set of Integers to write in
 * @throws IOException
 * 
 */
	protected void putIntAt( short rafIndex, long offset, ArrayList<Integer> values) throws IOException {
		
		for(int l = 0; l < values.size(); l++) 
			
			putIntAt(rafIndex, offset+l*4, values.get(l).intValue());
	}

/**
 * A function to put a double value at a given offset in a file
 * 
 * @param rafIndex The index of the random access file as it was passed to the constructor
 * @param offset The offset in the file 
 * @param value The double value to be written
 * @throws IOException
 * 
 */
	
	protected void putDoubleAt( short rafIndex, long offset, double value ) throws IOException {
		
		raf_list[ rafIndex ].seek( offset );
		
		raf_list[ rafIndex ].writeDouble( value );	
	}

/**
 * Puts a set of double in a file starting at a given offset
 * 
 * @param rafIndex The index of the random access file as it was passed to the constructor
 * @param offset The offset in the file 
 * @param values The set of double to write to the file (as an array)
 * 
 */
	
	protected void putDoubleAt( short rafIndex, long offset, double [] values ) throws IOException {
		
		for(int j = 0; j < values.length; j++) 
			
			putDoubleAt( rafIndex, offset+j*8, values[j] );
		
	}

/**
 * Puts a set of doubles in a file starting at a given offset
 * 
 * @param rafIndex The index of the random access file as it was passed to the constructor
 * @param offset The offset in the file 
 * @param values The set of double to write to the file (as an ArrayList)
 * 
 */
	
	protected void putDoubleAt( short rafIndex, long offset, ArrayList<Double> values ) throws IOException {
		
		for(int l = 0; l < values.size(); l++) 
			
			putDoubleAt( rafIndex, offset+l*8, values.get(l).doubleValue() );
	}

/**
 * Writes a float value in the file at the speccified offset 
 * 
 * @param rafIndex The index of the random access file as it was passed to the constructor
 * @param offset The offset in the file 
 * @param value The float value to be written to the file
 * @throws IOException
 * 
 */
	
	protected void putFloatAt( short rafIndex, long offset, float value ) throws IOException {
		
		raf_list[ rafIndex ].seek( offset );
		
		raf_list[ rafIndex ].writeFloat( value );
		
	}

/**
 * 
 * Writes a set of float values into the file starting at the specified file offset 
 * 
 * @param rafIndex The index of the random access file as it was passed to the constructor
 * @param offset The offset in the file 
 * @param values An array of float value to be written to the file
 * @throws IOException
 * 
 */
	
	protected void putFloatAt( short rafIndex, long offset, float [] values ) throws IOException {
		
		for( int m = 0; m < values.length; m++ ) 
			
			putFloatAt( rafIndex, offset + m*4, values[m] );
	}

/**
 * Writes a set of float values from an ArrayList into the file at the specified offset 
 * 
 * @param rafIndex The index of the random access file as it was passed to the constructor
 * @param offset The offset in the file 
 * @param values The ArrayList of float values to be written to the file
 * @throws IOException
 * 
 */
	
	protected void putFloatAt( short rafIndex, long offset, ArrayList<Float> values ) throws IOException {
		
		for( int m = 0; m < values.size(); m++ ) 
			
			putFloatAt( rafIndex, offset + m*4, values.get( m ).floatValue() );
	}
	
/**
 * A function that writes a long integer value into a specified offset in the file	
 * @param rafIndex The index of the random access file as it was passed to the constructor
 * @param offset The offset (in bytes) from the start of the file at which write operation begins
 * @param value The long value to write into the file
 * @throws IOException
 */
	
	protected void putLongAt( short rafIndex, long offset, long value ) throws IOException {
		
		raf_list[ rafIndex ].seek( offset );
		
		raf_list[ rafIndex ].writeLong( value );
	}
	
/**
 * 
 * A function that writes a set of long values into a file
 * 
 * @param rafIndex The index of the the random access file as it was passed to the constructor
 * @param offset The offset (in bytes) from the start of the file at which write operation begins
 * @param values A long array containing the set of values to write into the file
 * @throws IOException
 * 
 */
	
	protected void putLongAt( short rafIndex, long offset, long [] values ) throws IOException{
		
		for( int n = 0; n < values.length ; n++ ) 
		
			putLongAt( rafIndex, offset + n*8, values[n] );
	}
	
/**
 * Closes the file input streams and output streams managed by the object
 * 
 */
	
	public void close() {
		
		if(null != raf_list) closeRaf();
		
	}
	
}
