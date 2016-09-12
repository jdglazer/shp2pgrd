import java.io.File;

import compressor.CompressIt;

import converter.Convert;

/**
 * Contains the main function that performs the full conversion from shapefile to a minified pgrd file
 * 
 * @author Glazer, Joshua D.
 *
 */

public class Shp2pgrd {
	
/**
 * Constructor performs file conversion
 * 
 * @param parent_shp The path and shapefile name from which conversion will start (NOTE: Do NOT add .shp extension in file name)
 * @param pgrd_name The name of the file to be constructed
 * @param error_file The path, name, and extension of the error log file produced in conjunction with conversion
 * @param lat_interval The interval between neighboring lines of equal latitude ( in units of degrees latitude )
 * @param lon_interval The interval between neighboring points in degrees longitude
 */
	public Shp2pgrd( String parent_shp, String pgrd_name, String error_file, String lat_interval, String lon_interval ) {
		
		String [] initial_convert = new String[]{ parent_shp, pgrd_name, error_file, lat_interval, "false" };
		
		String [] final_compress = new String[] { pgrd_name+".pgrd", pgrd_name, lon_interval, "true"};
		
		System.out.println("-----------------------------------------------------");
	//File conversion
		Convert.main( initial_convert );
		
		CompressIt.main( final_compress );
		
	//code to delete temporary .pgrd file
		File file = new File( pgrd_name+".pgrd");
		
		if( file.exists() ) {
			
			file.deleteOnExit();
		}
		
		System.out.println( '\r'+" File conversion completed successfully!");
		System.out.println("-----------------------------------------------------");
		
	}
	
/**
 * The main function which performs the file conversion
 * @param args Contains all necessary information to convert from a shapefile to a minified pgrd file.
 * 				args[0] : parent shapefile path and name without .shp extension
 * 				args[1] : path and name of the pgrd to be built
 * 				args[2] : error log file path, name and extension
 * 				args[3] : latitude interval between neighboring lines ( in degrees latitude )
 * 				args[4] : longitude interval between neighboring lines ( in degrees longitude )
 */
	public static void main( String [] args ) {
		
		if( args.length == 5 ) {
			
			new Shp2pgrd( args[0], args[1], args[2], args[3], args[4] );
		}
		else {
			System.out.println("Invalid number of arguments!");
		}
		
	}
	
}