# Overview
A text file viewer for very very big size text file. Easy to open a file bigger than 50GB.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.riversun/llpad/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.riversun/llpad)

It is licensed under [MIT](https://opensource.org/licenses/MIT).

# Features
## *Gigabyte* class text files can be opened easily and lightly.

- <h3>Can I open 10GB text file? Yes, No problem.</h3>
- <h3>Can I open 50GB text file? Yes, My pleasure.</h3>
- <h3>Can I open 100GB text file? Welcome, try it</h3>


# Install
### For Windows
Download Windows installer package  
(all-in-one package)


Or You can download from maven or from Github and build yourself.

![screenshot](https://riversun.github.io/img/llpad_055.png
 "screenshot")

### For MacOS or Linux
You can download from maven or from Github and build yourself.

# Build & Run manually
## Create runnable jar including dependencies

```
$ mvn clean install
```

## Create installer package
(For now Windows Platform only supported)

```
> mvn clean install
> create_installer_for_windows.bat
```

installer created in package/outfiles folder

## Run from command line

```
java --add-exports java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED -jar .\llpad-0.5.6-jar-with-dependencies.jar
```

# Architecture Overview
- LLPAD is not read the entire file at once.
- So in order to correspond to a big file it reads a small part of the file into buffer(CachedArea).
- The area to be displayed in the text area is called viewArea and displays a part of the buffer.
- When the caret is moved, the viewArea also moves, and when the viewArea reaches the end of the buffer, the next area is read into the buffer.


![screenshot](https://riversun.github.io/img/llpad_split_loading.png
 "screenshot")

# Downloads

## maven
 - You can add dependencies to maven pom.xml file.

 ```xml
 <dependency>
   <groupId>org.riversun</groupId>
   <artifactId>llpad</artifactId>
   <version>0.5.5</version>
 </dependency>
 ```

