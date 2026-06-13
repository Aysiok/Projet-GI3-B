# MoldSim — Archive Mold Risk Simulator

> A JavaFX simulation application for visualizing mold propagation on archive room walls in real time.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Installation & Build](#installation--build)
- [Running the Application](#running-the-application)
- [Usage](#usage)
- [Team](#team)

---

## Overview

**MoldSim** is a 2D cellular automaton simulator developed as part of the *Projet Génie Logiciel* module at CY Tech (ING1-GI, 2025–2026).

The application simulates mold growth on the walls of an archive room. Each cell on the grid evolves over time based on environmental conditions (humidity, temperature, ventilation) and wall material. The user can interact with the simulation in real time, place shelves containing documents, trigger external events, navigate between the four walls of the room, and export reports.

---

## Features

| Feature | Description |
|---|---|
| **Wall configuration** | Configure material and dimensions (in meters) for each of the 4 walls at startup |
| **Play / Pause / Step** | Start, pause, resume, or advance the simulation one week at a time |
| **Speed control** | Adjust simulation speed with a slider (0.1s to 1.0s per step) |
| **Time travel** | Navigate back to any previous state using the time slider or Previous button |
| **Environment controls** | Adjust humidity, temperature, and ventilation in real time |
| **Wall material** | Choose from Concrete, Wood, Plaster, or Brick — affects mold spread rate |
| **Mold species** | Select the mold species (Cladosporium, Aspergillus, Stachybotrys) |
| **Mold lifecycle** | Cells progress through HEALTHY → DEPOSITED_SPORE → INFECTED → SPORULATING → DEAD |
| **Draw tools** | Paint or erase mold manually using Point, Brush, or Rectangle drawing modes |
| **Shelf placement** | Place shelves with configurable dimensions (meters) and document value (LOW/MEDIUM/HIGH/CRITICAL) |
| **External events** | Trigger Water Leak, HVAC Failure, or Window Opened events; apply wall/shelf anti-mold treatment |
| **Multi-wall navigation** | Switch between North, South, East, and West walls; spores propagate between adjacent walls |
| **Live statistics** | Infected cell count, infection rate, and risk level updated every step |
| **Sensor & alerts** | MoldSensor monitors contamination and triggers alerts with recommendations |
| **Save / Load** | Save and restore the full simulation state (binary .sim format) |
| **PDF export** | Export a full statistics and alert report as a PDF at any point |

---

## Project Structure

```
src/
└── moldsim/
    ├── Main.java                        # Application entry point
    ├── controller/
    │   ├── GridController.java          # Main controller (Play/Pause, shelf logic, wall navigation)
    │   ├── SimulationController.java    # Simulation engine, step logic, spore germination
    │   └── AlertController.java        # Alert handling and recommendations
    ├── model/
    │   ├── Cell.java                    # Individual grid cell (state, mold level, age, material)
    │   ├── Wall.java                    # 2D grid of cells with contamination rate
    │   ├── ArchiveRoom.java             # Room with 4 walls and environment
    │   ├── Environment.java             # Humidity, temperature, ventilation
    │   ├── Shelf.java                   # Shelf with plank count and document value
    │   ├── MoldSensor.java              # Contamination sensor with per-shelf tracking
    │   ├── MoldSpecies.java             # Mold species enum (survival conditions, growth rate)
    │   ├── WallMaterial.java            # Wall material enum (CONCRETE, WOOD, PLASTER, BRICK, DOCUMENT)
    │   ├── CellState.java               # Cell state enum (HEALTHY, DEPOSITED_SPORE, INFECTED, SPORULATING, DEAD)
    │   ├── Statistics.java              # Grid statistics computation (trend, mold levels)
    │   ├── SimulationSnapshot.java      # Immutable multi-wall state for time travel
    │   ├── SimulationState.java         # Full simulation state for binary save/load
    │   ├── SensorEvent.java             # Alert event (GLOBAL or SHELF type)
    │   ├── LocationContext.java         # Room/wall display name
    │   ├── RecommendationEngine.java    # Generates action recommendations from alerts
    │   ├── PdfExporter.java             # PDF report generation (iText)
    │   ├── BinaryExporter.java          # Binary save/load (.sim files)
    │   ├── EventManager.java            # Applies external events (water leak, HVAC, treatment)
    │   ├── ExternalEvent.java           # External event enum
    │   ├── GridScale.java               # Meter ↔ cell conversion utilities
    │   ├── WallContext.java             # Bundles a Wall + Shelves + SimulationController
    │   └── WallManager.java             # Manages navigation between the 4 WallContexts
    └── view/
        ├── MainView.java                # Main UI layout (sidebars, controls, navigation)
        ├── GridView.java                # Grid canvas rendering and interaction modes
        ├── WallPreviewView.java         # Compact preview of adjacent walls
        └── WallConfigDialog.java        # Startup wall configuration dialog (meters + material)
lib/                                     # External libraries (iTextPDF)
```

---

## Requirements

- **Java** 21 or later
- **JavaFX SDK** 21 ([Download here](https://gluonhq.com/products/javafx/) — choose SDK, Windows/macOS/Linux)

---

## Installation & Build

### 1. Clone the repository

```bash
git clone https://github.com/<your-repo>/moldsim.git
cd moldsim
```

### 2. Set your JavaFX path

Replace `<PATH_TO_JAVAFX>` in the commands below with the path to your JavaFX SDK `lib` folder.

**Example paths:**
- Windows: `H:\javafx-sdk-21\lib`
- macOS/Linux: `/opt/javafx-sdk-21/lib`

### 3. Compile

**Windows (PowerShell):**
```powershell
javac --module-path "<PATH_TO_JAVAFX>" --add-modules javafx.controls,javafx.graphics -cp "lib\*" -d out (Get-ChildItem -Recurse -Filter "*.java" src | % { $_.FullName })
```

**macOS / Linux:**
```bash
javac --module-path "<PATH_TO_JAVAFX>" --add-modules javafx.controls,javafx.graphics -cp "lib/*" -d out $(find src -name "*.java")
```

---

## Running the Application

**Windows (PowerShell):**
```powershell
java --module-path "<PATH_TO_JAVAFX>" --add-modules javafx.controls,javafx.graphics -cp "out;lib\*" moldsim.Main
```

**macOS / Linux:**
```bash
java --module-path "<PATH_TO_JAVAFX>" --add-modules javafx.controls,javafx.graphics -cp "out:lib/*" moldsim.Main
```

---

## Usage

1. **At startup** — a configuration dialog appears. Set the material and dimensions (in meters) for the North wall. You can then apply the same config to all 4 walls, or configure each one individually. Scale: 1 cell = 5 cm × 5 cm.
2. **Interact with the grid** — select a draw tool (Point / Brush / Rectangle) and use **Add Mold** or **Treat Wall Zone** mode to paint or erase contamination.
3. **Control the simulation** — use **Play**, **Pause**, **Step**, and the **Speed** slider. The **Time** slider lets you return to any previous state; **Previous** steps back one week.
4. **Adjust the environment** — move the Humidity, Temperature, and Ventilation sliders to influence mold spread in real time.
5. **Trigger external events** — use the right sidebar to apply Water Leak (infects a zone + raises humidity), HVAC Failure (cuts ventilation), Open Window (boosts ventilation), or anti-mold treatment on a wall zone or a specific shelf.
6. **Add shelves** — click **New Shelf**, set dimensions (meters) and document value, then click on the grid to place it. Right-click a shelf to remove it.
7. **Navigate walls** — use the **◀** / **▶** buttons to switch between North, East, South, and West walls. Adjacent wall edges share spore propagation.
8. **Save / Load** — use **Save** and **Load** to persist the full simulation state as a `.sim` binary file.
9. **Export** — click **Export PDF** to generate a PDF report containing statistics and alert history.

---

## Team

| Name | Role |
|---|---|
| Brice Faviere | UI design (MainView, GridView), draw tools, wall preview |
| Matheo Kannengieser | GridController, multi-wall navigation (WallManager, WallContext), inter-wall spore propagation |
| Asma Kajeiou | Cell model, mold propagation logic, mold lifecycle (SPORULATING, DEPOSITED_SPORE) |
| Damien Fernandes | SimulationController, sensors, alerts, external events (EventManager) |
| Damien Di Martino | PDF export, binary save/load (BinaryExporter, SimulationState), snapshots, integration & testing |

**Tutor:** D. Zaouche
**Academic year:** 2025–2026 — CY Tech ING1-GI
