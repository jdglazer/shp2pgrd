shp2pgrd

	A tool to convert from a polygon shapefile (and associated dbf file)
	to a minified polygon grid file
	
version: 1.0.1
author: Glazer, Joshua D.
contact: atrail2014@gmail.com

Developer notes

	*This is an unstable version for this software
	*More error handling is necessary and it is recommended that a layer
	 be added to verify the validity of the input shapefile file more thoroughly
	*More thorough annotation and html documentation will be included in next version
	 
Input File Type
    
    ESRI Shapefile of polygon type. As is indicated by Shapefile specifications
    a dbf file of the same name must exist in the same directory as ShapeFile
    
    ESRI Shapefile Specifications: https://www.esri.com/library/whitepapers/pdfs/shapefile.pdf 

Output File Type
	
	Minified Polygon Grid File ( *.min.pgrd )
		
		This file format is a raster type format that describes a 
		single feature for a geographical region by an index. The 
		format defined a grid with the interval between point being
		measured by latitude and longitude intervals. The grid of 
		points is compressed by reducing point identifier index
		redundancy. For example, rather than storing 300 of the same
		indexes adjacent to eachother the format stored a single index
		and a number of adjeacent points (of increasing latitude from
		the first point) for which this index applies. Thus what might 
		have been an array of 300 shorts, 600 Bytes, gets reduced to 5
		or 6 bytes.

		see PGRD_MIN_SPECS.pdf for file format specifications

Installing this Program

	Navigate to the main download folder, /shp2pgrd, and run install.sh as follows:
		
		./install.sh

	NOTE: You may need to change install.sh to an executable before running it. This can be accomplished with the following:

		chmod +x install.sh

Using this Program

	The implementation of this program is simple. It requires the use of
	the command "shp2pgrd" with 5 arguments.

	------------------------------------------------------------------------------------
	shp2pgrd convertFromShp convertToPgrd errorLogFile latitudeInterval longitudeInterval

	arguments:
	----------
		convertFromShp - The path and name of the polygon shapefile to convert from (NOTE: leave out .shp extension)
		convertToPgrd - The path and name of the new minified pgrd file to be created
		errorLogFile - The path and name of the error log file to be produced in conjunction with the file conversion
		latitudeInterval - The interval, in degrees latitude, between adjacent latitude lines
		longitudeInterval - The interval, in degrees longitude,	between neighboring points in the grid

	eg.
	----
	shp2pgrd /home/joshua/Documents/GeoBedrockData/MEgeol_dd/megeol_poly_dd /home/joshua/ME_bedrock error_log.txt .00018 .00018
	
