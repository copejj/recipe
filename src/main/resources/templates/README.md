# Brain Dribbler Recipes Template Playbook

Use these standardized starter templates when building new views for the application. They ensure perfect integration with the global navbar, automatic stylesheet bundling, and strict compliance with the core layout engine constraints.

---

## 1. Standard Content View Component (`standard-page.html`)
Use this blueprint for public directory listings, general informational pages, recipe galleries, or indexing lists where a standard header, section titles, and action grids are required.

```html
<!DOCTYPE html>
<html xmlns:th="http://thymeleaf.org"
      xmlns:layout="http://ultraq.net.nz"
      layout:decorate="~{fragments/layout}"
      lang="en">
<head>
    <!-- 1. Customize your browser tab title text string here -->
    <title>Page Title - Brain Dribbler Recipes</title>
    
    <!-- 2. Mount your screen component style bundles cleanly inside this hook -->
    <th:block layout:fragment="page_styles">
        <!-- <link rel="stylesheet" th:href="@{/css/recipes/view.css}" /> -->
    </th:block>
</head>

<body>
    <!-- 3. Renders inside the standard page banner template. Do not change tag to div -->
    <h1 layout:fragment="page-title">Dynamic Header Display Title</h1>
    <p layout:fragment="page-subtitle">Dynamic subtext baseline describing the primary interface role or system domain.</p>
    
    <!-- 4. Primary functional content workspace area -->
    <main layout:fragment="content">
```

```html
        <!-- Drop view-specific HTML templates layout loops here -->
    </main>

    <!-- 5. Optional script array down-stream mounting anchor hook -->
    <th:block layout:fragment="page_scripts">
        <!-- <script th:src="@{/js/recipes/view.js}" defer></script> -->
    </th:block> 
</body>
</html>
```

---

## 2. Narrow Control Panel Template (`control-page.html`)
Use this compressed framework layout for absolute focus nodes like **Login interfaces, Registration pipelines, or focused modal forms**. It forces zero-gap viewport alignment and triggers the engine parameter flag combinations `isNarrow=true` and `isHeaderless=true`.

```html
<!DOCTYPE html>
<html xmlns:th="http://thymeleaf.org"
      xmlns:layout="http://ultraq.net.nz"
      layout:decorate="~{fragments/layout}"
      th:with="isNarrow=true,isHeaderless=true"
      lang="en">
<head>
    <title>Control Hub Authorization</title>
    
    <th:block layout:fragment="page_styles">
        <link rel="stylesheet" th:href="@{/css/admin/auth.css}" />
    </th:block>
</head>

<body>
    <!-- Main focus interface payload card container block -->
    <main layout:fragment="content" class="page-card auth-layout">
        <div class="auth-container">
```

```html
            <!-- Mount focus panels, inputs, payload tokens, and workflows here -->

        </div>
    </main>
    
    <!-- Optional: Keeps script interactions isolated downstream -->
    <th:block layout:fragment="page_scripts">
        <!-- <script th:src="@{/js/admin/auth.js}" defer></script> -->
    </th:block> 
</body>
</html>
```
