package converter;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Calendar;

import utils.PointPlaceException;
import utils.RecordOutOfBoundsException;
import utils.ShapeFile;

/**
 * <p>A class which performs the functions of fitting points into polygons in a shapefile such that the points
 * may have values attached to them. This class can go no further than attaching points to specific shapefile records,
 * but does most of the heavy lifting involved with fitting points into the shapefile. It also has tools to fit 
 * a latitude line into a record by determining the longitude range over which that line falls within a record polygon's 
 * interior region.
 * 
 * @author Joshua Glazer
 * @version 1.0
 *
 */

/*
 * 2/4 TASKS
 * ---------
 * + create a special pool for Integer and Double objects to be used in getEnclosingBoxes function and isEnclosing function
 * + figure out how to work with thrown exceptions in constructor, isEnclosing, and enclosingRecords functions
 * + continue writing isEnclosing function and be sure to store intersection longitudes in increasing order for use
 * 	 determining length for which latitude line exists inside polygon (store these in an ArrayList<Double> property
 * 	 and use recycled Doubles
 * 
 */
public class EqualLatitudePointPlace {
	
	private double [][] boundBoxes;
	
	public ArrayList<Integer> encloseRecords;
	
	//pool variables to control use of memory
	
		//pool variables for setEndIndex function
		private boolean indexNotFound;
		private int uI;
		private int mI;
		private int lI;
		private int eLen;
		
		//pool variable for east intersect

		
		//pool variables for inPolyPart
		private float [] point_1;
		private float [] point_2;
		private float [] midpoint;
		private float il;
		public ArrayList<Float> intersects = new ArrayList<Float>();
		private int numPoints;
		
		//pool variables for isEnclosing function
		private int numParts;
		
		//pool variables for enclosing record
		private int encloseRec;
		
		private ArrayList<Integer> intersectSegs = new ArrayList<Integer>();
		
		
/**
 * The only valid constructor function for this object.
 * 
 * @param sf {@link ShapeFile} instance containing connections to *.shp, *.shx, *.dbf in which points are to be placed
 * @throws IOException
 * @throws RecordOutOfBoundsException
 * 
 */
		
	public EqualLatitudePointPlace( ShapeFile sf ) throws IOException, RecordOutOfBoundsException {
		
		int recNum = sf.recordCount();
		
		encloseRecords = new ArrayList<Integer>();
		
		boundBoxes = new double[recNum][5];
		
		for(int k = 0; k < recNum; k++) {
			
			boundBoxes[k][0] = (double) k;
			
			boundBoxes[k][1] = sf.minLat(k);
			
			boundBoxes[k][2] = sf.minLon(k);
			
			boundBoxes[k][3] = sf.maxLat(k);
			
			boundBoxes[k][4] = sf.maxLon(k);
			
		}

        Arrays.sort(boundBoxes, new Comparator<double[]>() {
            @Override
            public int compare(double[] coors1, double[] coors2) {
            	
                return Double.compare(coors1[1], coors2[1]);
                
            } 
        }); 

	}
	
	
/**
 * Determines the first point in the boundBoxes array for which the minimum latitude is greater than the
 * latitude passed in as a parameter.
 * 
 * @param latitude
 * 
 */
	
	private void setEndIndex(double latitude) {
		
		if( boundBoxes[0][1] > latitude) eLen =  -1 ;
		
		//upper index: uI, lower index: lI, middleIndex: mI
		
		uI = boundBoxes.length - 1;
		mI = -1;
		lI = 0;
		indexNotFound = true;
		
		while(indexNotFound && (uI - lI) > 1) {
			
			mI = ((uI - lI) >> 1) + lI;
			
			if(boundBoxes[mI][1] < latitude)  {
				lI = mI;
			}
			
			else if(boundBoxes[mI][1] > latitude) 
				uI = mI;
			
			else 
				indexNotFound = false;
			
		}
		
		if(!indexNotFound) eLen = mI;
		
		if(boundBoxes[lI][1] < latitude )
			eLen = uI;
		else 
			eLen = lI;		
		
	}
	
	//pool variables for getEnclosingBoxes function
	
/**
 * Finds the indexes associated with the records in the original shape file in which the latitude line
 * fed to the parameter fits. Stores the values in the encloseRecords ArrayList.
 * 
 * @param latitude 
 * 
 */
	
	public ArrayList<Integer> getEnclosingBoxes( double latitude ) {
		
		setEndIndex( latitude );
		
		encloseRecords.clear();
		
		for(int l = 0; l < eLen; l++) {
			
			if(boundBoxes[l][3] >= latitude) 
					
				encloseRecords.add(new Integer((int) boundBoxes[l][0]));
			
		}
		
		return encloseRecords;
	}

/**
 * Takes a latitude, longitude point and tests whether it fits into a polygon defined by a 
 * specific part in a shape file polygon record
 * 
 * @param sf
 * @param recordI The index of the main polygon record (starts with 0)
 * @param partI The index of the part of the polygon record (starts with 0)
 * @param latitude The latitude of the point
 * @param longitude The longitude of the point
 * @return returns true if the point fits into the polygon, false otherwise
 * @throws PointPlaceException
 * 
 */
	protected void partIntersects(ShapeFile sf, int recordI, int partI, float latitude) throws PointPlaceException {
		
		try {
			
			numPoints = sf.partLength(recordI, partI);
		
			for(int m = 0; m + 1 < numPoints; m++) {
				
				if( m > 0 ) {
					
					point_1 = Arrays.copyOf( midpoint, 2 );
					
					midpoint = Arrays.copyOf( point_2, 2 );

				}
				else {
					
					point_1 = sf.getLatLon(recordI, partI, ( m==0 ? numPoints - 2 : m-1) ) ;
					
					midpoint = sf.getLatLon( recordI, partI, m );

				}
				
				point_2 = sf.getLatLon(recordI, partI, m+1);

				if((midpoint[0] >= latitude && point_2[0] < latitude) || (midpoint[0] <= latitude && point_2[0] > latitude)) {
					
					il = getIntersectLon( latitude, point_1, midpoint, point_2 );
					
					if( validLon( il ) )
						
						intersects.add( new Float( il ) );
				 
				}	
			}
	
		}
		catch(IOException ioe) {
			
			throw new PointPlaceException( " latitude: " +Double.toString(latitude)	 );
		}
		
		catch(RecordOutOfBoundsException robe) {
			
			throw new PointPlaceException( " latitude: " +Double.toString(latitude)	 );
		}
		
	}
	
/**
 * Determines if a given latitude, longitude point fits into the polygon defined by a given record in the .shp file
 * 
 * @param sf The {@link ShapeFile} instance containing relevant connection to the .shp file of interest
 * @param recordIndex The index of the polygon record in the file where the 0 index represents the first record
 * @param latitude The latitude of the point being tested
 * @param longitude The longitude of the point being tested
 * @return returns a boolean indicating whether point specified by parameters is found inside polygon defined by record
 * 
 */
	
	public ArrayList<Float> interiorSegments(ShapeFile sf, int recordIndex, float latitude) throws PointPlaceException {
		
		intersects.clear();
		
		try {
			
			numParts = sf.partCount( recordIndex );
			
			for(int l = 0 ; l < numParts;  l++) {
				
				partIntersects( sf, recordIndex, l, latitude );

			}	
			
			Collections.sort( intersects );
			
			//System.out.println( recordIndex+": "+intersects.size()%2 );
			
		}
		catch(IOException ioe) { 
			
			throw new PointPlaceException(" Record Error: " + Integer.toString(recordIndex));
		}		
		
		catch(RecordOutOfBoundsException robe) {
			
			throw new PointPlaceException(" Record Error: " + Integer.toString(recordIndex));
		}
		
		catch(PointPlaceException ppe) { 
			
			throw ppe; 
		}
		
		return intersects;
	}

/**
 * Determines the the intersection longitude of a latitude line with a line segment defined by two points
 * 
 * @param latLine
 * @param point1 A two part double array storing the latitude of the first point as the first element and the longitude as the second.
 * @param midpoint A two part double array storing the latitude of the first point as the second element and the longitude as the second.
 * @return A double value intersecting longitude
 */
	public float getIntersectLon(float latLine, float [] point1, float [] midpoint, float point2[] ) {
		
		//System.out.println( midpoint[0]+", "+midpoint[1]+'\n' );
		
		if( latLine == midpoint[0] ) {
	
			if( ( midpoint[0] > point1[0] && midpoint[0] > point2[0] ) || (midpoint[0] < point1[0] && midpoint[0] < point2[0]) )
				
				return 180.1F;
		}
		if(point1[1] == midpoint[1])
			
			return point1[1];
		
		if (point1[0] == midpoint[0] )

			return 180.1F;
		
		return (((point2[1] - midpoint[1])/(point2[0] - midpoint[0]))*(latLine-midpoint[0])) + midpoint[1];
		
		
	}
	
/**
 * Verifies that longitude value passed in is valid
 * 
 * @param lon longitude ( range -180.0 to 180.0 )
 * @return true if valid longitude passed in, false otherwise
 */
	public static boolean validLon( float lon ) {
		
		return Math.abs( lon ) <= 180.0;
	}
	
/**
 * Determines that latitude value passed in is valid
 * @param lat latitude (-90.0 to 90.0 )
 * @return true of valid latitude passed in, false otherwise
 */
	public static boolean validLat( float lat ) {
		
		return Math.abs( lat ) <= 90.0;
	}
	
/** TEST CODE:
  *-----------
	public void tester(ShapeFile sf, int recordI, double latitude) throws PointPlaceException{
		
		long t = Calendar.getInstance().getTime().getTime();
		interiorSegments(sf, recordI, (float) latitude);
		System.out.println((Calendar.getInstance().getTime().getTime() - t)/1000.0);
		System.out.println(intersects.size());
		float sumlon = 0;
		for(int i = 0; i < intersects.size()/2; i++ ) {
			sumlon += intersects.get( i*2 ).floatValue() - intersects.get( i*2).floatValue();
		}
		
		System.out.println(sumlon);
		
	}
*/
}
