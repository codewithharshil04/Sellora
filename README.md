# Sellora

A multi-module Android freelance marketplace application connecting clients with freelancers, with a dedicated admin dashboard to manage the platform.

Designed in Figma: [Sellora Design File](https://www.figma.com/design/t3Oa1Xa55mQibq4yff4fp3/Sellora?node-id=0-1&p=f&t=RzaxcQICT3WHuKD2-0)

## Project Structure

This repository contains three independent Android applications:

- **Sellora** — Client-facing app. Clients browse services, review project briefs, make payments, and manage their projects.
- **SelloraPartner** — Freelancer-facing app. Freelancers set up services, manage project pages, and track their profile/dashboard.
- **SelloraAdmin** — Administrative dashboard for managing users, orders, and platform settings across both apps.

## Screenshots

### Client App
<!-- Add client flow screenshots here, e.g.: -->
<!-- ![Client Flow](./screenshots/client-flow.png) -->

### Freelancer App (SelloraPartner)
<!-- Add freelancer flow screenshots here, e.g.: -->
<!-- ![Freelancer Flow](./screenshots/freelancer-flow.png) -->

## Tech Stack

- **Language:** Kotlin
- **Build System:** Gradle with Kotlin DSL
- **Platform:** Android

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 11 or later
- Android SDK with latest build tools

### Building the Project

Each module can be built independently:

```bash
# Build Sellora (Client)
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

### Sellora (Client)

The main client-facing app. Users can sign up, browse service categories, view service details, submit project briefs, complete payments, and manage their profile and active projects.

### SelloraPartner (Freelancer)

The freelancer-facing app. Freelancers onboard, set up their profile, add and manage services, and track their project pages through a dashboard.

### SelloraAdmin

Administrative interface for system administrators to manage users, orders, and platform settings across the Sellora ecosystem.

## Development

This project uses standard Android development practices with Kotlin. Each module is structured as an independent Android application with its own build configuration.

## License

This project is licensed under the MIT License — see the [LICENSE](./LICENSE) file for details.
