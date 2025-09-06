emulator -list-avds

emulator -avd Medium_Tablet

adb install app\build\outputs\apk\debug\app-debug.apk

adb shell am start -n com.example.smsmail/com.example.smsmail.MainActivity

REM adb logcat

REM adb emu sms send <PHONE_NUMBER> "Your message text"

REM adb emu sms send 123456789 "test message"

