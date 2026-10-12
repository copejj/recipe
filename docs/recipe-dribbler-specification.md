# Recipe Dribbler - Product Specification & System Architecture Blueprint

## System Identification
* **Application Name:** Recipe Dribbler (Braindribbler Recipes)
* **Target Stack:** Spring Boot, Spring Security, Thymeleaf, DataTables, jQuery
* **Document Version:** 1.0.0 (October 2026)

---

## 1. Master Navigation & Security Map

This table controls the global generation array for `GlobalNavigationAdvice` and dynamic layout visibility hooks.

| Main Menu Item | Submenu Options | Target Access Rule | Functional Path / View Target |
| :--- | :--- | :--- | :--- |
| **Home** | *None (Direct Link)* | Public / Guests | `/` (Public recipe browsing & layout landing) |
| **Dashboard** | *None (Direct Link)* | Authenticated Users | `/dashboard` (User home with profile stats) |
| **Browse Recipes** | Universal Search | Public / Guests | `/recipes/search` (Keyword/text filtering) |
| | Match by Ingredients | Public / Guests | `/recipes/match` (Filter by available items) |
| **My Cookbook** | Create New Recipe | Authenticated Users | `/recipes/create` (Interactive recipe wizard) |
| | Personal Recipes | Authenticated Users | `/recipes/personal` (View/edit owned recipes) |
| | Saved Binders | Authenticated Users | `/recipes/saved` (Recipes bookmarked from others) |
| **Meal Planner** | Schedule Calendar | Authenticated Users | `/planner/schedule` (Day/meal grid grouping) |
| | Shopping Lists | Authenticated Users | `/planner/shopping-list` (Mass/ounce aggregation) |
| **Community** | Cook Directory | Public / Guests | `/community/cooks` (Public user profiles) |
| | Social Connections | Authenticated Users | `/community/connections` (Mutual links, blocking) |
| **Admin Panel** | Users Panel | Authority: `can_manage_users` | `/admin/users` (Enable/disable & passwords) |
| | Security Logs | Authority: `can_manage_users` | `/admin/login-history` (Login audits) |
| | Role Management | Authority: `can_manage_roles`| `/admin/roles` (Promote/demote moderators) |
| | Recipe Ingredients | Authority: `can_moderate` | `/admin/ingredients` (Merge duplicates & units) |

---

## 2. Role Privilege Hierarchy

Clearance permissions cascade downward linearly. Higher tiers automatically inherit permissions from the classes beneath them.

1. **Super Users**
   * Authority Rule: `ROLE_SUPER_USER`
   * Targets: Can promote or demote standard system Administrators.
2. **Admins**
   * Authority Rule: `ROLE_ADMIN` (Inherits all Moderator actions)
   * Targets: Management of system accounts, hard profile state switches, forcing password changes.
3. **Moderators**
   * Authority Rule: `ROLE_MODERATOR` (Inherits all Account Holder actions)
   * Targets: Execution of duplication merges on ingredient items, updating measurement standard tables, toggling public recipes to private.
4. **Account Holders**
   * Authority Rule: `ROLE_USER` (Authenticated state)
   * Targets: Creation of custom recipes, generation of calendar menus, saving external recipes, processing social blocks.
5. **Public Guests**
   * Authority Rule: Unauthenticated
   * Targets: Read-only access to community items, temporary item aggregation, conversion previews.

---

## 3. Recipe Component Domains

### Ingredients Data Payload
* Standard Definition: `Amount` + `Measurement Standard` + `Ingredient Name String`
* Structural State: Persistent sequence positions mapping index variables (`order_index`) to preserve chronological entry flow through the schema.

### Cooking Views Structure
* **Overview:** Clean decoupled text segments rendering ingredients, sequential steps, and final author notes sequentially.
* **Cooking View:** Interleaved runtime format binding specific ingredients directly preceding the instruction blocks utilizing them.
* **List View:** Fully aggregated unique item output summing total measurements across multiple recipe targets into unified mass/volume metrics.

---

## 4. Context Template for Future Development Queries

*Developer Note: Copy and paste this exact block when starting a new session with an AI collaborator to maintain architectural consistency across your codebase.*

```text
System Context Prompt:
I am developing a Spring Boot and Thymeleaf platform named Recipe Dribbler. The system uses a data-driven menu strategy driven by a GlobalNavigationAdvice interceptor feeding structured arrays to layout fragments. 

Current Implementation Grounding:
1. Access logic maps across a precise hierarchy: Guest -> User -> Moderator -> Admin -> Super User.
2. Dynamic dropdown elements render cleanly via custom MenuItem objects wrapping child payload lists.
3. All feature paths, custom controllers, and security filter endpoints must strictly match the layout mapping file checked into `docs/recipe-dribbler-specification.md`.

Please build the next requested module while staying within these defined naming conventions and permission constraints.
```
