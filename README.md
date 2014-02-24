jslicify-demo
=============

Demo files for http://www.slicify.com


com.slicify.demo.bitcoin
--------------------
PrimeCoinMining.java

This is a fairly complete example of running primecoin mining across a number of Slicify nodes.
You can also grab the PrimecoinMinerDemo.bat / PrimecoinMinerDemo.jar files if you don't want to recompile

* Run the program, and enter your slicify username/password
* Enter some bids manually in the website
* The program will automatically pick up any bookings that you get. For each booking:
* Run apt-get update, apt-get to install the libraries, download the mining client, and restart it
* Scan for [STATS] records and print out the summary info (chains/d)
* Detect 'force disconnect' messages, and other errors and automatically restart the client session.


com.slicify.demo.pov
--------------------
POVRayRender.java

This is a simple minimal/command line demo running POVRay renderer on one node.

* Enter your slicify & mediafire username/password
* Books a single node
* Connects to the node using SSH
* Installs povray 3.6
* Installs plowshare (connector for mediafire)
* Runs a test render (povray -iscenes/advanced/benchmark.pov -H768 -W1024)
* Uploads the result to your mediafire account
* Calculates timing info, so you can see the breakdown of setup/rendering/file transfer times


com.slicify.demo.lux
--------------------
LuxRenderDemo.java

This is a slightly more complex demo running LuxRender renderer over multiple nodes.

* Enter your slicify & mediafire username/password
* Books multiple nodes
* Connects/installs LuxRender on each node
* Slices a test render image into multiple segments, running different segments on different nodes
* Uploads the resulting partial renders to MediaFire
* Downloads them back to the local PC and reassembles them 

Runnable JAR
------------
If you just want to try the demo, just download the precompiled LuxRenderDemo.jar and LuxRenderDemo.bat (requires Java 1.7).