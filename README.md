<div align="center">

<img width="100%" src="https://capsule-render.vercel.app/api?type=waving&color=0:1A3A6B,50:3A6EA5,100:7B93B4&height=200&section=header&text=MoldSim&fontSize=80&fontColor=fff&animation=twinkling&fontAlignY=35&desc=Archive+Mold+Risk+Simulator&descAlignY=55&descSize=25"/>

[![Typing SVG](https://readme-typing-svg.demolab.com?font=Fira+Code&weight=700&size=24&duration=3000&pause=1000&color=3A6EA5&center=true&vCenter=true&multiline=false&repeat=true&width=800&height=80&lines=2D+Cellular+Automaton+on+JavaFX;Simulate+mold+growth+in+real+time;Multi-wall+navigation+%26+spore+propagation;Alerts%2C+sensors+%26+PDF+export)](https://git.io/typing-svg)

<p>
  <img src="https://img.shields.io/badge/Java-21-1A3A6B?style=for-the-badge&logo=openjdk&logoColor=white&labelColor=0D1F3C"/>
  <img src="https://img.shields.io/badge/JavaFX-21-3A6EA5?style=for-the-badge&logoColor=white&labelColor=0D1F3C"/>
  <img src="https://img.shields.io/badge/iText-5.5.13-7B93B4?style=for-the-badge&logoColor=white&labelColor=0D1F3C"/>
  <img src="https://img.shields.io/badge/MVC-Architecture-4A86C8?style=for-the-badge&labelColor=0D1F3C"/>
  <img src="https://img.shields.io/badge/CY_Tech-ING1--GI-3A6EA5?style=for-the-badge&labelColor=0D1F3C"/>
  <img src="https://img.shields.io/badge/2025--2026-Génie_Logiciel-1A3A6B?style=for-the-badge&labelColor=0D1F3C"/>
</p>

</div>

---

## Overview

**MoldSim** is a 2D cellular automaton simulator developed as part of the *Projet Génie Logiciel* module at CY Tech (ING1-GI, 2025–2026).

The application simulates mold growth on the walls of an archive room. Each cell evolves over time based on environmental conditions (humidity, temperature, ventilation) and wall material. The user can interact in real time, place shelves, trigger external events, navigate between the four walls of the room, and export reports.

> **Architecture:** Model-View-Controller (MVC) — Java 21 — JavaFX — Git

---

## Features

<details open>
<summary><b>Simulation</b></summary>
<br>

| Feature | Description |
|---|---|
| **Wall configuration** | Configure material and dimensions (meters) for each of the 4 walls at startup |
| **Play / Pause / Step** | Start, pause, resume, or advance the simulation one week at a time |
| **Speed control** | Adjust simulation speed with a slider (0.1s to 1.0s per step) |
| **Time travel** | Navigate back to any previous state using the time slider or Previous button |
| **Mold lifecycle** | `HEALTHY` → `DEPOSITED_SPORE` → `INFECTED` → `SPORULATING` → `DEAD` |

</details>

<details open>
<summary><b>Environment</b></summary>
<br>

| Feature | Description |
|---|---|
| **Environment controls** | Adjust humidity, temperature, and ventilation in real time |
| **Wall material** | Concrete, Wood, Plaster, Brick — each affects mold spread rate differently |
| **Mold species** | Cladosporium, Aspergillus, Stachybotrys — distinct survival conditions and growth rates |

</details>

<details open>
<summary><b>Interaction & Data</b></summary>
<br>

| Feature | Description |
|---|---|
| **Draw tools** | Paint or erase mold using Point, Brush, or Rectangle modes |
| **Shelf placement** | Place shelves with dimensions (meters) and value (LOW / MEDIUM / HIGH / CRITICAL) |
| **External events** | Water Leak, HVAC Failure, Window Opened, Wall/Shelf anti-mold treatment |
| **Multi-wall navigation** | Switch between North, South, East, West walls — spores propagate between adjacent walls |
| **Live statistics** | Infected cell count, infection rate, and risk level updated every step |
| **Sensor & alerts** | MoldSensor triggers alerts with automatic recommendations |
| **Save / Load** | Save and restore the full simulation state (binary `.sim` format) |
| **PDF export** | Export a full statistics and alert history report |

</details>

---

## Project Structure

```
src/
└── moldsim/
    ├── Main.java
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
    │   ├── WallMaterial.java            # Wall material enum
    │   ├── CellState.java               # Cell state enum (5 states)
    │   ├── Statistics.java              # Grid statistics computation
    │   ├── SimulationSnapshot.java      # Immutable multi-wall state for time travel
    │   ├── SimulationState.java         # Full simulation state for binary save/load
    │   ├── SensorEvent.java             # Alert event (GLOBAL or SHELF type)
    │   ├── LocationContext.java         # Room/wall display name
    │   ├── RecommendationEngine.java    # Generates action recommendations from alerts
    │   ├── PdfExporter.java             # PDF report generation (iText)
    │   ├── BinaryExporter.java          # Binary save/load (.sim files)
    │   ├── EventManager.java            # Applies external events
    │   ├── ExternalEvent.java           # External event enum
    │   ├── GridScale.java               # Meter <-> cell conversion (1 cell = 5 cm)
    │   ├── WallContext.java             # Bundles a Wall + Shelves + SimulationController
    │   └── WallManager.java             # Manages navigation between the 4 WallContexts
    └── view/
        ├── MainView.java                # Main UI layout (sidebars, controls, navigation)
        ├── GridView.java                # Grid canvas rendering and interaction modes
        ├── WallPreviewView.java         # Compact preview of adjacent walls
        └── WallConfigDialog.java        # Startup wall configuration dialog
lib/
    └── itextpdf-5.5.13.3.jar
```

---

## Requirements

<p>
  <img src="https://img.shields.io/badge/Java_JDK-21+-1A3A6B?style=for-the-badge&logo=openjdk&logoColor=white&labelColor=0D1F3C"/>
  <img src="https://img.shields.io/badge/JavaFX_SDK-21-3A6EA5?style=for-the-badge&logoColor=white&labelColor=0D1F3C"/>
</p>

> JavaFX is **not** bundled with the JDK — download it separately at [gluonhq.com/products/javafx](https://gluonhq.com/products/javafx/)

---

## Installation & Build

### 1. Clone the repository

```bash
git clone https://github.com/<your-repo>/moldsim.git
cd moldsim
```

### 2. Set your JavaFX path

| OS | Example path |
|---|---|
| Windows | `H:\javafx-sdk-21\lib` |
| macOS / Linux | `/opt/javafx-sdk-21/lib` |

### 3. Compile

**Windows (PowerShell)**
```powershell
javac --module-path "<PATH_TO_JAVAFX>" --add-modules javafx.controls,javafx.graphics `
  -cp "lib\*" -d out `
  (Get-ChildItem -Recurse -Filter "*.java" src | % { $_.FullName })
```

**macOS / Linux**
```bash
javac --module-path "<PATH_TO_JAVAFX>" --add-modules javafx.controls,javafx.graphics \
  -cp "lib/*" -d out $(find src -name "*.java")
```

---

## Running the Application

**Windows (PowerShell)**
```powershell
java --module-path "<PATH_TO_JAVAFX>" --add-modules javafx.controls,javafx.graphics `
  -cp "out;lib\*" moldsim.Main
```

**macOS / Linux**
```bash
java --module-path "<PATH_TO_JAVAFX>" --add-modules javafx.controls,javafx.graphics \
  -cp "out:lib/*" moldsim.Main
```

---

## Usage

| Step | Action |
|---|---|
| 1 | **Startup** — configure material and dimensions (meters) for each wall. Scale: 1 cell = 5 cm x 5 cm. |
| 2 | **Draw** — select Point / Brush / Rectangle tool, then use Add Mold or Treat Wall Zone mode. |
| 3 | **Simulate** — use Play, Pause, Step, Previous and the Speed slider. |
| 4 | **Environment** — adjust Humidity, Temperature and Ventilation sliders in real time. |
| 5 | **External events** — trigger Water Leak, HVAC Failure, Open Window or apply treatments. |
| 6 | **Shelves** — click New Shelf, set dimensions and value, place on grid. Right-click to remove. |
| 7 | **Navigate walls** — use the arrow buttons to switch between the 4 walls. |
| 8 | **Save / Load** — persist or restore the full simulation state as a `.sim` file. |
| 9 | **Export** — click Export PDF to generate a statistics and alert history report. |

---

## Team

<div align="center">

| Name | Role |
|---|---|
| **Asma Kajeiou** | Cell model, mold propagation logic, mold lifecycle (SPORULATING, DEPOSITED_SPORE) |
| **Brice Faviere** | UI design (MainView, GridView), draw tools, wall preview (WallPreviewView) |
| **Matheo Kannengieser** | GridController, multi-wall navigation (WallManager, WallContext), inter-wall spore propagation |
| **Damien Di Martino** | SimulationController, sensors, alerts, external events (EventManager) |
| **Damien Fernandes** | PDF export, binary save/load (BinaryExporter, SimulationState), snapshots, integration & testing |

**Tutor:** D. Zaouche — **Academic year:** 2025–2026 — CY Tech ING1-GI

</div>

---

<img width="100%" src="https://capsule-render.vercel.app/api?type=waving&color=0:1A3A6B,50:3A6EA5,100:7B93B4&height=120&section=footer"/>
