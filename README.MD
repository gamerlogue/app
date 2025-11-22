# Compose Multiplatform Application

## Before running!
 - install JDK 17 or higher on your machine  
 - add `local.properties` file to the project root and set a path to Android SDK there  

### Android
To run the application on android device/emulator:  
 - open project in Android Studio and run imported android run configuration  

To build the application bundle:  
 - run `./gradlew :composeApp:assembleDebug`  
 - find `.apk` file in `composeApp/build/outputs/apk/debug/composeApp-debug.apk`  

Run android UI tests on the connected device: `./gradlew :composeApp:connectedDebugAndroidTest`

### Web Distribution
Build web distribution: `./gradlew :composeApp:composeCompatibilityBrowserDistribution`  
Deploy a dir `composeApp/build/dist/composeWebCompatibility/productionExecutable` to a web server  

### JS Browser
Run the browser application: `./gradlew :composeApp:jsBrowserDevelopmentRun --continue`  
Run browser UI tests: `./gradlew :composeApp:jsBrowserTest`  

### Wasm Browser
Run the browser application: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continue`  
Run browser UI tests: `./gradlew :composeApp:wasmJsBrowserTest`  

Icon generated with https://icon.kitchen/i/H4sIAAAAAAAAAz2PsQ6CMBCG3%2BVcHVDRIKuDk5NsxpijPUpDoaQFjCG8u9caXNrc16%2F3380woRnJQz6DRNcUNbUEeYXG0xYqVXx6LkG3qAgCuBjdoxuC74kvcHbsJD9pYTsuJy3JKmzphT4IyxbKtYtyKDV1A9tDyJH%2FoFLda4yO0E6YX9QNfQP54MYoXNfPnCyssW7H9iYrxSFF1iPaB5Qk51OWMcJOcac8PS48RGvlaMKeD%2BbSWR1ntp7PN5XwXL6pqwTiCgEAAA%3D%3D
