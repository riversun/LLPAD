# Overview
A text file viewer for very very big size text file. Easy to open a file bigger than 50GB.

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

[![screenshot](https://riversun.github.io/img/llpad_icon.png
 "screenshot")](http://riversun.org/downloads/LLPAD-0.5.5.zip)
[download LLPAD v0.5.5](http://riversun.org/downloads/LLPAD-0.5.5.zip)

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

## Run from command line
```
$ java -jar llpad-0.5.5-jar-with-dependencies.jar
```
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
