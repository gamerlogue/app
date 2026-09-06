# Multiplatform App

### Android
To run the application on android device/emulator:
- open project in Android Studio and run imported android run configuration

To build the application bundle:
- run `./gradlew :androidApp:assembleDebug`
- find `.apk` file in `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

### Desktop
Run the desktop application: `./gradlew :desktopApp:run`  
Run the desktop **hot reload** application: `./gradlew :desktopApp:hotRun --auto`

### iOS
To run the application on iPhone device/simulator:
- Open `iosApp/iosApp.xcproject` in Xcode and run standard configuration
- Or use [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) for Android Studio

### Web Distribution
Build web distribution: `./gradlew :webApp:composeCompatibilityBrowserDistribution`  
Deploy a dir `webApp/build/dist/composeWebCompatibility/productionExecutable` to a web server

### JS Browser
Run the browser application: `./gradlew :webApp:jsBrowserDevelopmentRun`

### Wasm Browser
Run the browser application: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`  

# Credits
## Icons
<a href="https://www.flaticon.com/free-icons/gaming" title="gaming icons">Gaming icons created by Flat Icons - Flaticon</a>
Icon generated with: https://icon.kitchen/i/H4sIAAAAAAAAA11Sy07DMBD8l-VaKiepE7c3QIITJ3qi6mETb9wIJw5xAq2q_jtr94EgjizP7Hr25SN8oZ3Iw-oItXly1g2wgjtVZLxgBuUvl2OmSTFXm_WhJ6aaFg1FpwthBtQNdWPkXq6Apf3o-hhDBKVClZUU7JQElGnM84SRmAux-GMX86w4M6JcyjQyi2UWs5GllDIy8j9WUTdPy4XMI7OUieJfiUQolasimBcJFrmA0wywM5azzxLJgGuh_TgNTBxhvB5hh7YeXReqra4NSVHqLAkKpXnbYeyA_5yQL8xg3FFLGlY1Wk-hZ6_oP26w948mSCS1TNNQe--fI1HHLxLrZgxpwQu2NFhnpiDLfu7c0hrbxh7Y_vBI9E4U80Bt6DYwkYYVB_ZWoW26EKHiidAQB_RKfheUetd0I09nc4Q9rMQ8lTNg4fPhVm2xrElmHOXiVVy9wuHmdRnmaXvidFqnJxve1oZ7rAfXaI7bOM_7N5W8t1hF1Fs83PMb4c5tTz8mXxgekwIAAA
