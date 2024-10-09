---
layout: post
title: "Clojure Web Application Building Blocks"
date: 2020-07-17 13:43:09 -0700
comments: true
categories:
  - clojure
  - programming
---

As I've mentioned before, I'm rebuilding this website using Clojure.

When starting a new project, I find you often have two choices: start with a batteries-included framework or build up the framework yourself from your own code and any libraries you might want to leverage. I usually choose the first when I want to get something up and running quickly. I choose the second when I want to learn as much as I can and have fun tinkering along the way.

This time, I am choosing to build up the framework of my application myself. In the past I have used a baterries-included web application template, [chestnut](https://github.com/plexus/chestnut), but this time I decided not to go that route. One reason I decided not to use chestnut is that I don't think I'll need any client-side cljs code. Chestnut's main use case is compiling and reloading cljs.

The only functionality I want in the first iteration of my framework is markdown to HTML parsing, an HTTP server to serve that HTML, and live reloading (reloading of the page when something server-side changes).

To support those features, I have started evaluating the libraries I want to use:

- These are micro-frameworks for managing dependencies, keeping things loosely coupled, and setting up a [reloaded workflow](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded) for development. Integrant is currently the frontrunner.
  - [Integrant](https://github.com/weavejester/integrant)
  - [Mount](https://github.com/tolitius/mount)
  - [Component](https://github.com/stuartsierra/component) - I've used Component once before, but Integrant claims to solve some of the difficulties I had with Component.
- [Ring](https://github.com/ring-clojure/ring) - the *de facto* standard Clojure web application library.
- I have decided to use bidi as a routing library because I don't like Compojure.
  - [bidi](https://github.com/juxt/bidi) - a routing library that just uses data structures for defining routes.
  - [compojure](https://github.com/weavejester/compojure) - a routing library which I find distasteful due to its unnecessary use of macros.
- [hawk](https://github.com/wkf/hawk) for watching files to kick off live reloading.

I would like to keep the libraries I'm using fairly minimal. I may add some more to this list for things like generating HTML, but I'd like to write a quite a bit of code myself.

I feel good about choosing to build up the framework of my application myself. Getting to know each piece may help me develop the application even more quickly in the long run.
