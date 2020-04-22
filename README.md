#remotedesktop
Remote Desktop Server, Proxy and HTML Viewer written in Java and JavaScript

##Building

```
$ mkdir bin; (cd src; javac -d ../bin `find . -type d | fgrep socketserver| sed 's|$|/*.java|'` && cp -r META-INF ../bin) && (cd bin; jar cfm ../remotedesktop.jar META-INF/MANIFEST.MF .)
```
or with docker:

```
$ docker build -t remotedesktop-httpserver .
```

##Testing with docker

Run the docker container with:

```
$ docker run -p6502:6502 remotedesktop-httpserver
$ java -jar remotedesktop.jar --host 172.17.0.1
```

Get the IP of the docker container, for example with:

```
$ ip a list docker0 | fgrep ine
# example: 172.17.0.1
```

Point your browser to ``http://172.17.0.1:6502``



