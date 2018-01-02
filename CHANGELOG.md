# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
- Permanent `WatchDogService` to monitor and recover critical features and services
- `PowerManagementReceiver` to control power management functions
- `UsbStatusActivity` to handle device connections and permissions
- Permanent `GpsService` to support external USB gps receivers
- `CanSnifferActivity` to directly sniff the can bus
- Permanent `CarduinoService` to manage carduino connection
- `SerialConnectionManager` to maintain serial connection
- `SerialReader` along with `SerialPacket` (and descendants) to manage serial-data
- `OverlayWindow` to show climate control status
- `RemoteControl` to handle steering wheel button events

[Unreleased]: https://github.com/rampage128/cardroid
