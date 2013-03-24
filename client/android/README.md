ReMuCo Android Client
=====================

Using
-----
In order to use this ReMuCo Android version, first compile the code to an APK. 
Next install the APK on your phone. After installing it, use the menu button 
and press connect. You will then be able to connect to a running ReMuCo server.

Up-to-date Instruction on how to do compile the code are given below.
Instruction on how to copy the APK can be found at the ReMuCo wiki:
  http://code.google.com/p/remuco/wiki/Android#Run_the_client_on_a_real_device




Compiling and installation
--------------------------

In the following instructions the assumption is that you have installed the 
Android SDK, and that you are following the instructions on a Unix system.
More information on the Android SDK can be found at http://developer.android.com/sdk/index.html .

The following code creates a symbolic link to the ReMuCo client core code, which is
required in order to compile the Android apk.

```bash

    #cd to your remuco root folder
    cd remuco
    
    #create a (relative) symbolink link
    cd client/android/src/remuco/client
    ln -s ../../../../common/src/remuco/client/common common
```

Compiling can be done with the following commands

```bash

    #cd to your remuco root folder
    cd remuco

    #enter the android directory
    cd client/android

    #update project settings (has to be done only once)
    android update project --name Remuco --target 4 --path .

    #compile
    ant debug
```

After compiling, the output files can be found in the `bin` subdirectory.


Bugs
----
If you find any bugs, please report them :)
For more information, please read http://code.google.com/p/remuco/wiki/Issues.

Future work
-----------
The ReMuCo  Android provices basic functionality. However, the client is not finished yet.
There are always improvements to be made.
If you wish, you can add them yourself and send your contribution on Git to the ReMuCo team.

Below you can find a list of future work items, feel free to add or implement  other improvements as well.

* Distinct "Connection failed" message instead of "Disconnected" if the connection fails.
* Autoconnect does not work the first time the Android client is started (Android service
  is not started, and thus connecting fails)
* The ReMuCo service never stops (workarounds: phone reset or application kill)
* Widget is not working properly (connection detection problems)
* Search dialog (move to Fragment, improve user experience)
* Add support for default list actions, currently clicking on playlist/queue actions do not
  perform an action.
* Connection profiles (easily select previous connection configurations)
* Disable/hide elements not provided by the service (e.g. not all players provide track rating)
* Streaming from player to ReMuCo client (stream MPD audio, VLC audio/video to ReMuCo)
* Reflect player side list changes (playlist/queue) immediately in the android client.
* Allow setting Volume by manually pressing the volume control seekbar.
* Let the ReMuCo server inform which lists are available (now several lists co-exist, while
  some are not implemented, or distinction is not intuitive (e.g. playlist <--> queue).
