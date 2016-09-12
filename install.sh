#!/bin/sh

j="/*.java"
b="/bin"
util_path="./utils"
convert_path="./converter"
compress_path="./compressor"
parent_folder="/shp2pgrd"
main_implement_class=" Shp2pgrd"

echo "Enter a directory path for install: "
read install_location

# Make sure install location is valid
if [ -d $install_location ]; then

    echo "Building parent directories..."
    
    mkdir -p $install_location$parent_folder
    install_location1=$install_location$parent_folder$b
    mkdir -p $install_location
    
    echo "Copying files..."
    
# Copy non-compilable resource files
    cp PGRD_MIN_SPECS.pdf -d $install_location$parent_folder
    cp README.md -d $install_location$parent_folder
    
    echo "Installing..."
    
    javac -d $install_location1 $util_path$j $convert_path$j $compress_path$j *.java
    compile_status=$?
    
# Make sure javac compiled source code cleanly
    if [ $compile_status==0 ]
    then
    
# Writes alias 'shp2pgrd'     
        if [ -f ~/.bashrc ]
        then
            program_run_command="java -classpath $install_location1 $main_implement_class"
            
            echo "alias shp2pgrd='$program_run_command'" >> ~/.bashrc
        fi
        
        echo "Installation Complete!"
        echo "PLEASE NOTE: Certain systems require restarting computer to be able to use shp2pgrd program."
        
    else
    
        echo "Woops! Installation failed!"
         
    fi  
 else
 
    echo $install_location" is not a valid directory. Please re-run install.sh and provide a valid directory path."
 fi
