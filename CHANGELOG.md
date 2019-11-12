# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
- Multi device support to allow connection of multiple simultaneous (usb) devices
- `DeviceController` to manage multiple device instances simultaneously
- `Device` classes to manage device types and functionality
- `Device.StateObserver` to allow external observation of device states
- `Device.FeatureObserver` to allow external observation of feature availability
- `DeviceUid` to uniquely identify physical devices beyond connection instances
- `CarduinoUsbDevice` to manage carduino devices and allow dynamic UID and feature detection
- Abstraction layer for feature based user interfaces
- Abstraction layer for master-detail-flow
- Device UX to allow the user to manage registered devices and their state and features
- many more ...

### Changed
- `GpsService` was replaced with `GpsController`
- Carduino serial implementation (Different packet types don't use specific implementations anymore)
- Updated external dependency versions
- many more ...

### Removed
- `WatchDogService` got obsolete with the new architecture
- `CarSystems` got obsolete with new `Variables`
 - many more ...

## [0.0.1]
### Added
- `Rules` to create user defined events
- Permanent `WatchDogService` to monitor and recover critical features and services
- `PowerManagementReceiver` to control power management functions
- `UsbStatusActivity` to handle device connections and permissions
- Permanent `GpsService` to support external USB gps receivers
- `CanSnifferActivity` to directly sniff the can bus
- Permanent `CarduinoService` to manage carduino connection
- `SerialConnectionManager` to maintain serial connection
- `SerialReader` along with `SerialPacket` (and descendants) to manage serial-data
- `OverlayWindow` to show climate control status
- `RemoteControl` to handle steering wheel button eventEntities

[Unreleased]: https://github.com/rampage128/cardroid/compare/v0.0.1...HEAD
[0.0.1]: https://github.com/rampage128/cardroid/releases/tag/v0.0.1