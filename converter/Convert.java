package converter;
import utils.ShapeFile;

public class Convert {

/**
 * Converts a specified file and writes it to a specified location and under a specified name
 * 
 * @param args An array designed to carry the following information into the conversion process:
 *		arg[0] : The path and name of the shapefile to be converted (NOTE: do not include file extension in the path!)
 *		arg[1] : The path and name of the pgrd file to be written
 *		arg[2] : The name and extension of the error log file to be written to
 *		arg[3] : The interval between equal latitude lines
 */
	public static void main( String [] arg ) {
		
		try {
			ShapeFile shpf = new ShapeFile(arg[0]);

			PgrdBuilder builder = new PgrdBuilder(arg[1], arg[2], shpf );
		
			builder.build( shpf, Double.parseDouble( arg[3] ), 2 );

		} catch( Exception e ) { System.out.println("Exception thrown! : "+e.getMessage() ); }
	}


}
