# Brochado Lab Pipeline Tool

Developed for the [Brochado Lab](https://www.brochadolab.com/).

## Development

The project has two parts, backend and frontend.

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

shadow-cljs uses the file [shadow-cljs.edn](frontend/shadow-cljs.edn) for configuration.

Due to the usage of [gosling.js](https://gosling-lang.org/) I ended up in a situation where I had to set up a non-standard build process.
Therefore, the frontend build not only uses shadow-cljs, but also [Vite](https://vite.dev/).
Vite uses [vite.config.js](vite.config.js) for configuration.

The process is like this: shadow-cljs compiles Clojurescript to Javascript.
Vite bundles external NPM dependencies and the compiled output of shadow-cljs into the Javascript file that is included in the [index.html](frontend/index.html) and puts everything in the folder [dist](frontend/dist).
The output folder `dist` needs to be included as a folder `public` in the [resources](resources) of the backend.

This project uses the [re-frame](https://day8.github.io/re-frame/) frontend framework.
re-frame is an opinionated way of writing a [Reagent](https://reagent-project.github.io/) based single-page application.
Reagent is the most commonly used Clojurescript wrapper for [React](https://react.dev/).
In general, you can start working with re-frame and Reagent without prior knowledge of React.
