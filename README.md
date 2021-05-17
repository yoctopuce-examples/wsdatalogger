# Retrieving recorded measures over the Internet

This is a demo application that illustrate the usage of the datalogger when the Yoctopuce device when the library is
working in WebSocket callback mode. This demo is part of a article that we have written  on our web site:
https://www.yoctopuce.com/EN/article/retrieving-recorded-measures-over-the-internet

To use this example you need a Yocto-MaxiDisplay and any Yoctopuce sensor (Yocto-Light-V3, Yocto-Amp, Yocto-Meteo, etc)
Then you need to install this Web application on a Java application server that support WebSocket (Tomcat 7 or 8,
Wildfly 8, etc), and configure the VirtualHub or the YoctoHub to use Yocto-API callback and enter the public URL of your
application.

