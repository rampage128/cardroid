# CarDroid

> Android app to connect to your car using an arduino over USB

There is many ways to build a [carputer](https://en.wikipedia.org/wiki/Carputer).
One of the more popular ways is to utilize an Android device. Unfortunately there is no standard for 
car integration, which means dealing with many different apps and hardware parts.

This project aims for unifying different car-related functions in one app, and providing an easy and 
yet substantial car integration with pleasing and extensible user experience.

This is achieved by using an [Arduino (Nano)](https://www.arduino.cc/en/Main/ArduinoBoardNano) 
as interface hardware between Android and car.

__Please make sure to read the [requirements](#requirements)__!

## Background
CarDroid started as a small project to replace the obsolete but monolithic entertainment system
in a [Nissan 370z](https://de.wikipedia.org/wiki/Nissan_370Z). In this car the entertainment system
is integrated with a lot of car functions. Especially the climate-controls can only be operated via
 CAN-Bus, because the physical buttons are connected to the entertainment system.

## Requirements

- An Android device with USB OTG support and the matching Android version
  - Minimum: Android 5 (API 21)  
  - Recommended: Android 7.1 (API 25)
- [Arduino Nano](https://www.arduino.cc/en/Main/ArduinoBoardNano) or 
  [other Arduino board](https://www.arduino.cc/en/Main/Products#entrylevel) 
- The [niscan project](https://github.com/rampage128/niscan) running on your Arduino board
- __A nissan 370z__ ... The app doesn't care, but currently niscan only supports that car!

## Installation
### Releases

Currently no releases, you have to build the project [From source](#from-source)

### From source

1. Get your favourite Android IDE ([Anrdoid Studio](https://developer.android.com/studio/index.html) 
recommended)
2. Clone the repository   
   ```
   git clone https://github.com/rampage128/cardroid.git
   ```
3. Compile and install on your Android

## Usage

The app provides multiple interaction models to integrate into the car

- __Floating overlay__ to view and control the climate controls.  
  Active all the time to allow quick access to car functions.
- __Planned: Home screen widgets__ to allow visualization of car data on the Android home screen.
- __Planned: Different activities__ to control different aspects of the car.

Once you connect the Arduino board to your Android device, it will ask to open the App.

To use the overlay, you have to grant permission to draw over other windows. This can also be done
in the application settings.

## Contribute

Feel free to [open an issue](https://github.com/rampage128/cardroid/issues) or submit a PR

Also, if you like this or other of my projects, please feel free to support me using the Link below.

[![Buy me a beer](https://img.shields.io/badge/buy%20me%20a%20beer-PayPal-green.svg)](https://www.paypal.me/FrederikWolter/1)

## Dependencies

- [UsbSerial](https://github.com/felHR85/UsbSerial) to communicate with Arduino