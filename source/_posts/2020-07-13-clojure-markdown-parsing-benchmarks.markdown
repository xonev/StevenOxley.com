---
layout: post
title: "Clojure Markdown Parsing Benchmarks"
date: 2020-07-13 10:45:32 -0700
comments: true
categories: clojure programming
---

I am working on setting up a new system for publishing content. I have a few different categories of content that I'm interested in creating. I'll have to determine exactly what the taxonomy will be, but the broad categories will probably be computers, mountain biking, and more personal stuff including relationships and religion. The first step towards this new system is just to replace the technology behind this website.

This website is currently generated statically using a very old version of [jekyll](https://jekyllrb.com/)/[octopress](http://octopress.org/). Static site generation is really nice, but I think I'm going to want to add some more interactive features like small applications. Therefore, I decided to replace this static site generation approach with a Clojure application.

Since these posts currently are all written in markdown and then parsed and rendered into HTML before being served statically via nginx, I wanted to check to see how expensive it would be to parse and render the markdown into HTML on every page load. To evaluate, I used a couple of handy Clojure libraries - [markdown-clj][1] and [criterium][2]. Using `markdown-clj` it was fairly trivial to replicate the functionality of the markdown processing of octopress. It is even has the ablility to parse the metadata at the top of the markdown files. For example, this is the metadata that I have at the top of this post:

```
---
layout: post
title: "Clojure Markdown Parsing Benchmarks"
date: 2020-07-13 10:45:32 -0700
comments: true
categories: clojure programming
---
```

To parse that metadata, I simply had to pass in the `:parse-meta? true` option when parsing, like this:

```
(md/md-to-html file-name writer :parse-meta? true :reference-links? true)
```

Then the metadata is parsed nicely into a map for me:

```
:metadata #ordered/map ([:layout "post"] [:title "Clojure Markdown Parsing Benchmarks"] [:date "2020-07-13 10:45:32 -0700"] [:comments true] [:categories "clojure programming"])
```

You can see more detail in the source code [on github][4].

Finally, I created an uberjar using `lein uberjar`, uploaded it to the DigitalOcean machine I intend to use, and ran the benchmark using criterium:

```
(crit/with-progress-reporting (crit/bench (md/parse-post "posts/2012-03-13-on-the-uncertainty-of-everything.md") :verbose))
```

Because of [this issue][3], I also had to call `flush` afterwards to get the output to display correctly. Again, you can see more detail [on github][5].

Once I ran the benchmark, criterium gave me some useful results:

```
Evaluation count : 3240 in 60 samples of 54 calls.
      Execution time sample mean : 19.447409 ms
             Execution time mean : 19.449686 ms
Execution time sample std-deviation : 909.567764 µs
    Execution time std-deviation : 928.443124 µs
   Execution time lower quantile : 18.431349 ms ( 2.5%)
   Execution time upper quantile : 21.718663 ms (97.5%)
                   Overhead used : 2.936410 ns

Found 5 outliers in 60 samples (8.3333 %)
        low-severe       4 (6.6667 %)
        low-mild         1 (1.6667 %)
 Variance from outliers : 33.6000 % Variance is moderately inflated by outliers
```

I can see there that it takes about 20ms to parse a typical markdown file for one of my blog posts. That would mean that, ignoring other overhead for serving a webpage, I could serve about 50 pages per second. That seems more than acceptable for the amount of traffic I expect to receive on this blog.

[1]: https://github.com/yogthos/markdown-clj
[2]: https://github.com/hugoduncan/criterium/
[3]: https://github.com/hugoduncan/criterium/issues/41
[4]: https://github.com/xonev/the-archive/blob/4fbdbcc8f08386a9ef893a21d69c2b74385c5ecf/websites/src/websites/markdown.clj
[5]: https://github.com/xonev/the-archive/blob/4fbdbcc8f08386a9ef893a21d69c2b74385c5ecf/websites/src/websites/core.clj
