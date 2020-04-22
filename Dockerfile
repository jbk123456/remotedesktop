 FROM java:8
   COPY . /var/www/java
   WORKDIR /var/www/java
  RUN mkdir bin; (cd src; javac -d ../bin `find . -type d | fgrep socketserver| sed 's|$|/*.java|'` && cp -r META-INF ../bin) && (cd bin; jar cfm ../remotedesktop.jar META-INF/MANIFEST.MF  .)
  CMD ["java", "-jar", "remotedesktop.jar", "--service=true"]
