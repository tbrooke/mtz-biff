# Agent Guidelines for Biff/Clojure Codebase

## Build & Development Commands
- Start dev server: `clj -M:dev dev`
- Run all tests: `(clojure.test/run-all-tests #"admin.com.*-test")` in REPL
- Run single test namespace: `(clojure.test/run-tests 'admin.com-test)` in REPL
- Generate CSS: `clj -M:dev css`
- Clean build: `clj -M:dev clean`

## Code Style & Conventions
- **Namespaces**: Use kebab-case for filenames, dot-separated hierarchical namespaces (e.g., `admin.com.app`)
- **Imports**: Group requires logically, use `:as` aliases for common libraries (e.g., `[com.biffweb :as biff]`)
- **Formatting**: Follow cljfmt rules defined in `cljfmt-indents.edn`, use 2-space indentation
- **Functions**: Use kebab-case naming, prefer pure functions, docstrings for public APIs
- **Data**: Use qualified keywords for database fields (`:user/email`, `:msg/text`)
- **Error Handling**: Use Biff's built-in error handling, prefer explicit error returns over exceptions
- **Routes**: Define routes as nested vectors with middleware, group by functionality in modules
- **Database**: Use XTDB queries with `q` macro, transactions with `biff/submit-tx`
- **UI**: Use Rum for server-side rendering, HTMX for dynamic interactions
- **Tests**: Use `deftest` with descriptive names, `with-open` for database setup, `mg/generate` for test data

## Architecture
- Modular design with separate namespaces for features (app, cms, uploads, etc.)
- Component-based system startup with `biff/use-*` components
- Schema validation using Malli with centralized registry