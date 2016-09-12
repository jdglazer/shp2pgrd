package compressor;

import java.io.IOException;

import utils.FailedPgrdBuild;

public class CompressIt {
//NEXT TO DO: change all literals in this to values passed in with the args parameter
//and write a file in default package to collect file conversion info and implement Convert and CompressIt class
//and make terminal commentary in these two class to be determined by final argument passed in args parameter to main functions
/**
 * 
 * @param args
 */
	public static void main( String [] args ) {
		
		try {
			
			PgrdFile pf = new PgrdFile(args[0]);
			
			PgrdMinifier pgrdmin = new PgrdMinifier( pf, args[1], "error_log.txt");
			
			try {
				
				pgrdmin.build( (float) Double.parseDouble( args[2] ), new PgrdMinifierEvent() {
					
					@Override
					public void onLineWriteComplete( int a, int n ) {
						
						if( a%50 == 0 )
							
							System.out.print(  " Progress: "+a + " of "+n+" lines complete.    \r" );
						
					}
					
					@Override
					public void onStartFileWrite() {
						
						System.out.println( '\r' + " Starting Pgrd compression...       " );
					}
					
					@Override
					public void onFinishFileWrite() {
						
						System.out.print( '\r'+" Pgrd compression complete!                          "+'\n' );
					}
					
				});
				
			}
			catch( FailedPgrdBuild fpb ) {
				
				System.out.println( "Error building file! Build aborted!: " + fpb.getMessage() );
			}
			/*
			long start = System.currentTimeMillis();
			pgrdmin.tester( 585 );
			
			System.out.println( System.currentTimeMillis() - start); */
			
		} catch( IOException ioe ) { ioe.printStackTrace(); } 
	}

}
