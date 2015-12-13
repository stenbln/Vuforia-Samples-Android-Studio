# Vuforia-Samples-Android-Studio
This is a Android Vuforia Samples App ported from Eclipse to Android Studio.<br />
It is supposed that you already have Android SDK installed.
<br />
<br />
<b>Instructions:</b><br />
1.Clone this repo<br />
2.Obtain your Vuforia license key at https://developer.vuforia.com/targetmanager/licenseManager/licenseListing<br />
3.Paste the key in "SampleApplicationSession.java" file which is located in your project under<br /> <i>app/src/main/java/com.qualcomm.vuforia.samples/SampleApplication/SampleApplicationSession</i> in line 347<br />
  paste it between the "" as the third parametar: <br />
  Vuforia.setInitParameters(mActivity, mVuforiaFlags, <b>""</b>);<br />
4. Open the project in Android Studio<br />
5. Click Build ->Clean Project<br />
6. Click Build ->Rebuild Project<br />
7. Click Run -> Run <br />
8. Choose your device from the list (Note: Run on the real device and not on the emulator because you need a camera)<br />


Hope it can Help! Cheers
