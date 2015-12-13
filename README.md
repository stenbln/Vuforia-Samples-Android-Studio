# Vuforia-Samples-Android-Studio
This is a Android Vuforia Samples App ported for Android Studio.

Instructions:
1.Clone this repo
2.Obtain your Vuforia license key at https://developer.vuforia.com/targetmanager/licenseManager/licenseListing
3.Paste the key in "SampleApplicationSession.java" file which is located under app/src/main/java/com.qualcomm.vuforia.samples/SampleApplication/SampleApplicationSession in line 347
  paste it between the "" as the third parametar: 
  Vuforia.setInitParameters(mActivity, mVuforiaFlags, "");
4. Open the project in Android Studio
5. Click Build ->Clean Project
6. Click Build ->Rebuild Project
7. Click Run -> Run 
8. Choose your device from the list (Note: Run on the real device and not on the emulator because you need a camera)

