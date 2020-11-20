# Java Archiver

Compresses and decompresses file(s) & folder(s) present in the given folder.
Also, makes sure the compressed files are below the given size threshold or allotted JVM memory 

## Build

The app uses gradle as build tool. To build the app, simply run the command below:
```
./gradlew build
```
 Jar files can be found under `build/libs/`. 

To invoke tests separately run `./gradlew test`.

Test coverage report is generated under `/build/lib/jacoco/test/html/`

## Usage

After building. To get started, invoke `java -jar archiver-{version}-all.jar -h`.
This will print the available commands.

```bash
$ java -jar archiver-0.1-all.jar  --help                                                                                                                  ✔ │ 02:01:16 PM 
Usage: archiver [-htV] [COMMAND]
Archiver that compresses files/folders
  -h, --help      Show this help message and exit.
  -t, --test      Print test Message
  -V, --version   Print version information and exit.
Commands:
  compress    Compresses files given in the source directory to the destination
                directory
  decompress  Decompresses files given in the source directory to the
                destination directory
```


#####Commands:

Compress command usage
```
$ java -jar archiver-0.1-all.jar compress --help    
Usage: archiver compress [-hV] <source> <destination> <maxFileSize> <mode>
Compresses files given in the source directory to the destination directory
      <source>        Source folder to look for files to compress
      <destination>   Destination folder to output the compressed files
      <maxFileSize>   Max file size of the compressed file
      <mode>          Compression mode
  -h, --help          Show this help message and exit.
  -V, --version       Print version information and exit.

```
Decompress command usage
```
$ java -jar archiver-0.1-all.jar decompress  --help    
Usage: archiver decompress [-hV] <source> <destination> <mode>
Decompresses files given in the source directory to the destination directory
      <source>        Source folder to look for files to decompress
      <destination>   Destination folder to output the decompressed files
      <mode>          Compression mode
  -h, --help          Show this help message and exit.
  -V, --version       Print version information and exit.

```

## Design Constraints/Considerations: 

 1. Input files may be greater than the allocated JVM memory.
 2. Compressed files generated can also be very large more than given `maxFileSize` threshold
 3. Compression output should generate less files.
 4. Should be designed to in a way to support multiple compression algorithms.
 5. Make parallel calls while compression as much as possible.
 6. Should use JDK's implementation for compression/decompression (ie no third party libs)
 

