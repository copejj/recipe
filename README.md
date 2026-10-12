# Recipe Dribbler (Braindribbler Recipes)

Welcome to **Recipe Dribbler**, a modern server-side Java web application designed to help food enthusiasts view, scale, group, and share recipes seamlessly. The platform features dynamic scaling calculations, custom automated shopping list aggregations, and data-driven security-aware layouts.

---

## Project Specifications

The core architecture, access controls, database entity maps, and UI layout strategies are tracked under a centralized blueprint. 

### [View Master Architecture & System Specifications](docs/recipe-dribbler-specification.md)

*Click the link above to view the complete system breakdown, including navigation structures, role matrices, and instructions for AI prompt continuity.*

---

## Technical Stack & Dependencies

This platform is structured around a modular Java landscape optimized for speed, accessibility, and robust server-side rendering:

* **Backend Engine:** Spring Boot (Java JVM)
* **Security & Authorization:** Spring Security (RBAC / Custom Authority Mapping)
* **Template Engine:** Thymeleaf 3.1+ (Natural HTML5 Templates)
* **Frontend Enhancements:** DataTables (Dynamic Grid Filters) & jQuery 4.0+
* **Database Migration:** Flyway Database Migrations

---

## Repository Directory Layout

```text
├── .github/               # CI/CD Workflow Automations
├── docs/                  # System Architecture & Documentation
│   └── recipe-dribbler-specification.md  <-- Master System Blueprint
├── src/
│   ├── main/
│   │   ├── java/         # Spring Boot Source Controllers, Services, Entities
│   │   └── resources/
│   │       ├── templates/ # Thymeleaf Views and Layout Fragments
│   │       └── static/    # Application Stylesheets (CSS) and Javascript Assets
└── pom.xml                # Maven Dependencies & Project Object Model
```

---

## Quickstart Development Instructions

To spin up a local instance of the application for development or testing:

1. **Clone the Repository:**
   ```bash
   git clone https://github.com
   cd recipe-dribbler
   ```

2. **Compile and Package Codebase:**
   ```bash
   ./mvnw clean install
   ```

3. **Execute Runtime Application:**
   ```bash
   ./mvnw spring-boot:run
   ```
   *The server will initialize locally at `http://localhost:8080`*

---
© 2026 Braindribbler Recipes. Distributed under standard project guidelines.
