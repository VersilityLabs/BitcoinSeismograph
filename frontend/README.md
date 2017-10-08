# Bitcoin Seismograph Frontend

The frontend is written in Clojurescript, with the stylings written in SCSS.

## Getting started

To run the frontend, make sure you have [boot]() and [npm]() installed, and then run:

```bash
$ npm install
$ boot dev
```

This will install all required dependencies (we aren't using `:npm-deps`
because some of the dependencies contain no javascript and hence cause
compilation errors), compile the SCSS and start a development server.

For a minified build, run:

```bash
$ boot prod
```
