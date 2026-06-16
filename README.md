# Sellora

A comprehensive multi-module Android application system consisting of three main applications:

## Project Structure

- **Sellora**: Main customer-facing application
- **SelloraAdmin**: Administrative dashboard for managing the system
- **SelloraPartner**: Partner application for business collaborators

## Tech Stack

- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL
- **Platform**: Android

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 11 or later
- Android SDK with latest build tools

### Building the Project

Each module can be built independently:

```bash
# Build Sellora
cd Sellora
./gradlew build

# Build SelloraAdmin
cd SelloraAdmin
./gradlew build

# Build SelloraPartner
cd SelloraPartner
./gradlew build
```

### Running the Applications

1. Open the desired module folder in Android Studio
2. Sync the Gradle files
3. Connect an Android device or start an emulator
4. Run the application

## Module Descriptions

### Sellora
The main customer application for end users to interact with the Sellora platform.

### SelloraAdmin
Administrative interface for system administrators to manage users, orders, and platform settings.

### SelloraPartner
Partner application for business collaborators to manage their partnerships and interactions within the Sellora ecosystem.

## Development

This project uses standard Android development practices with Kotlin. Each module is structured as an independent Android application with its own build configuration.

## License

[Add your license information here]
