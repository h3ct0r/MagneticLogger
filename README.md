# MagneticLogger

MagneticLogger is an app for Android to log the data comming from the serial port of a custom made Magnetometer with the following format:

> X:-14141.1, Y:-11025.9, Z:13099.9, T:22207.0

It also appends some information gathered from the device such as:
  - Global localization GPS/Glonass
  - Accelerometer
  - Giroscope
  - Compass
  - Date timestamp

### Mode of use

Just plug the FTDI USB port to an USB OTG cable and connect it to the android phone.

### Version

1.1

### Installation

Download the APK from the [releases link](https://nodejs.org/) and install it on an Android superior to version 4.4.4 (We have tested it on 5.0.1).

### Todos

 - Complete the real time graphs
 - Add options to append to last file saved
 - Reconnect on accidental disconnection
