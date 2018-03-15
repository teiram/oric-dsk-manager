# oric-dsk-manager

A little java application to manage Oric Microdisc Disk images. 

This is a work in progress and probably plagued with bugs.

List of current features:
- Supports Sedoric filesystems
- Opens old and new (MFM) dsk images
- Imports files from TAP archives
- Allows to define the target disk geometry (sides, tracks, sectors)
- Generates Sedoric bootable images
- Allows modification of the disk name and init string
- Exports files from a DSK image with a custom header if needed
- Automodifies file names to avoid clashing with already present files
- Disk usage bar

##Compilation
java and maven are needed. Once you have cloned the repository, just type:

    mvn install
    
##Running the application
Probably double clicking the generated jar would be enough. Otherwise, you can run it on a console by executing:

    java -jar oric-dsk-manager-jar-with-dependencies-0.1.jar
    
##Usage
Just drop archives from the host filesystem to the file list on the left side. They would be autodetected as old or new DSK images, TAPs or regular files. Menu entries are also provided for this function.

Selecting a file from the list shows the extended properties of the file on the right side. Change here the name or the flags of the selected file

Below the file properties some disk properties are shown and can be also changed: name, bootable and init string can be set at will.

Disk geometry can be also modified on the fly using the spinners below.

Once you are happy with the content of the disk, launch its generation by pressing the "Create Dsk" button or the menu entry "Save Dsk".

There's also a menu entry to open a Dsk. When selected, a whole Dsk will be imported, including geometry, disk name and init string, discarding the current content of the disk image.



