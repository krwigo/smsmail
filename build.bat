cls

set ANDROID_HOME=C:\Android\Sdk
set PATH=%ANDROID_HOME%\tools;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\emulator;%PATH%

set JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot"
set PATH=%JAVA_HOME%\bin;%PATH%

set GRADLE_HOME=C:\Android\Gradle\gradle-8.14.3
set PATH=%GRADLE_HOME%\bin;%PATH%

REM taskkill /F /IM java.exe /T
REM rmdir /s /q "%USERPROFILE%\.gradle\caches"

call gradle.bat --stop
call gradle.bat clean --info
call gradle.bat assembleDebug --info

dir app\build\outputs\apk\debug

REM adb disconnect
REM adb disconnect 192.168.2.48:37645
REM adb pair 192.168.1.1:11111
REM adb connect 192.168.2.48:33319

adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell pm list packages com.example.smsmail package:com.example.smsmail

REM adb logcat | findstr com.example.smsmail
REM adb logcat *:E | findstr com.example.smsmail
