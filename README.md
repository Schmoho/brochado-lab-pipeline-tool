# Brochado Lab Pipeline Tool

Developed for the [Brochado Lab](https://www.brochadolab.com/).

## Development

The project has two parts, backend and frontend. A significant portion of code that is in principle independent of this project is in [common](src/common/schmoho), frontend components that might be useful in other contexts are in [components](src/frontend/schmoho/components).

The two parts use different tools for building and compilation, and are therefore discussed separately.

The project is written in [Clojure](https://clojure.org/) and [Clojurescript](https://clojurescript.org/).

VSCode supports Clojure(script) development via [Calva](https://calva.io/).
An IntelliJ-based alternative is [Cursive](https://cursive-ide.com/).

In any case you will want to familiarize yourself with how to set up a [REPL](https://clojure.org/guides/repl/introduction) for both project parts.
Some details in the respective sections.

### Backend

The backend is written in [Clojure](https://clojure.org/) and uses [Leiningen](https://leiningen.org/) as the build tool.
See the respective websites for installation instructions.

Leiningen uses the file [project.clj](project.clj) for configuration of the build.
That includes external dependencies, source and resource path definitions etc.

#### Compilation

You can run `lein build` in the when you are "next to" the [project.clj](project.clj) to compile the project.

#### REPL

Your development environment of choice likely provides a complete mechanism to set up a REPL for this project.
Alternatively, you can run `lein repl` to start an nREPL server to which you can connect with a REPL client, for example from your editor.

### Frontend

The backend is written in [Clojurescript](https://clojurescript.org/) and uses [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html) as the compiler and build tool.

shadow-cljs uses the file [shadow-cljs.edn](shadow-cljs.edn) for configuration.
NPM dependencies are installed directly via `npm`, see [package.json](package.json).

The process is like this: shadow-cljs compiles Clojurescript to Javascript.
The folder [resources/frontend](resources/frontend) needs to be included as a folder `public` in the [resources](resources/backend) of the backend.

This project uses the [re-frame](https://day8.github.io/re-frame/) frontend framework.
re-frame is an opinionated way of writing a [Reagent](https://reagent-project.github.io/) based single-page application.
Reagent is the most commonly used Clojurescript wrapper for [React](https://react.dev/).
In general, you can start working with re-frame and Reagent without prior knowledge of React.
