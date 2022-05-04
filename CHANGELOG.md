# Bulwark - Changelog

All notable changes to [tech.gojek/bulwark](https://clojars.org/tech.gojek/bulwark) will be documented in this file.

## [Unreleased]

## [1.2.1]
### Added
- check if fallback func is running in separate thread before clearing MDC

## [1.2.0]
### Added
- `breaker-request-volume-threshold` config parameter.

## [1.1.0]
### Added
- `with-hystrix-async` macro to run a Hystrix command asynchronously.

## [1.0.1]
### Fixed
- NullPointerException when capturing parent thread logging context

## [1.0.0]
### Added
- [tech.gojek/bulwark](./project.clj) Clojure Library
- FOSS Stepping Stones
- Initial Commit

[Unreleased]: https://github.com/gojek/bulwark/compare/v1.2.0...master
[1.2.0]: https://github.com/gojek/bulwark/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/gojek/bulwark/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/gojek/bulwark/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/gojek/bulwark/compare/10fe6b4e6ab8aad5368c37798c5e0a1a4d499310...v1.0.0
