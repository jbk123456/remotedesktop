#remotedesktop
Remote Desktop Server, Proxy and HTML Viewer written in Java and JavaScript

##Building

```
mvn install
```

##Running

On a machine accessible to both, the display server and the client, type:

```
mylinuxserver.mydomain.com$ nohup java -jar remotedesktop.jar --service=true &
```

Start the capturing process:

```
mywindowsserver.mydomain.com$ java -jar remotedesktop.jar --host=mylinuxserver.mydomain.com
```

Point your browser to ``http://mylinuxserver.mydomain.com:6502`` to access the desktop
on ``mywindowsserver.mydomain.com``.



