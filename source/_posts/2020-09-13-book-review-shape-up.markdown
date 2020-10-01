---
layout: post
title: "Book Review: Shape Up"
date: 2020-09-13 15:31:45 -0700
comments: true
categories: books management
---

I have finally finished reading _Shape Up: Stop Running in Circles and Ship Work that Matters_ by Ryan Singer after many months.

It is a thought-provoking book. It has some good ideas for new approaches to running software projects. It is well written and clear, and I think Singer does a decent job of arguing for the processes and techniques he recommends. I just don't agree with some of his more foundational points and therefore I do not agree with the general thesis of the book.

More on my disagreements later; first, I'd like to point out the pieces from the book that I think are sound.

I think that the general concept of shaping work before working on it is excellent. The high-level idea is that you would make some rough designs and sketch out the flow and functionality of the work you're about to do. This helps flush out any major issues with the design before getting into the details. The [three properties](https://basecamp.com/shapeup/1.1-chapter-02#property-1-its-rough) of shaped work that I agree with are:

1. It's rough: the initial design should be at a high level.
2. It's solved: the solution to the problem has been well thought out. Open questions and rabbit holes have been resolved.
3. It's bounded: the scope - and what is out of scope - is clearly defined.

I find doing this type of design beneficial. It helps point out serious flaws with initial ideas.

Along with the general idea of shaping, Singer presents some ideas for creating rough designs - [breadboarding](https://basecamp.com/shapeup/1.3-chapter-04#breadboarding) and [fat marker sketches](https://basecamp.com/shapeup/1.3-chapter-04#fat-marker-sketches). Breadboarding (named after the breadboard used for prototyping electronic circuits) is a way of sketching out a flow - basically a more informal version of a flow chart. Fat marker sketches are rough sketches of a user interface design. They are meant to show all of the critical parts without dictating the final design. Both of these ideas seem useful, though perhaps not very original.

The one thing in this book that I think is original and could be useful in practice is the [hill chart](https://basecamp.com/shapeup/3.4-chapter-13#work-is-like-a-hill).

A hill chart is a convex curve that is used to communicate progress on a work item. As you are making progress on the item you're working on, you move a dot representing the item from left to right on the hill chart. If you are still figuring out the unknowns, approach, and design of the item you're working on, then you would place a dot on the uphill part of the hill chart. If you've figured things out and are simply carrying out the plan to finish the item, then you would move the dot to the right on the downhill part of the hill chart.

Now that we've covered the pieces in the book that I do agree with, I'd like to move on to the fundamental issue I have with the book. I would very much dislike being a developer or a designer at Basecamp. In the section [Who shapes](https://basecamp.com/shapeup/1.1-chapter-02#who-shapes), Singer paints a very clear picture of empowered shapers and indentured delivery teams. What I mean by _delivery team_ is a team that simply takes tasks as input and produces software as output. They are not responsible for solving business problems. They are only responsible for producting code that meets some requirements.

[Marty Cagan's writing](https://svpg.com/product-vs-feature-teams/) has influenced me greatly, and I have learned that my desire is for empowered product teams. The empowered product teams that Cagan describes, seem to be the direct opposite of the teams that Singer describes in this book. I am not a fan.

Another issue that I have with this book is its recommendation of [six week cycles](https://basecamp.com/shapeup/0.3-chapter-01#six-week-cycles). Singer claims, "six weeks is long enough to build something meaningful start-to-finish and short enough that everyone can feel the deadline looming from the start, so they use the time wisely." Maybe they don't have any procrastinators at Basecamp, but I think most would agree that a six week deadline doesn't loom very large. Anyway, I think Singer is missing the point of why you would want a shorter iteration. A shorter iteration gives a lot more opportunity to course correct...

Don't agree with:
- Six week cycles: https://basecamp.com/shapeup/0.3-chapter-01#six-week-cycles
- Who shapes: https://basecamp.com/shapeup/1.1-chapter-02
- Small batch vs. big batch
- Appetite is just a backwards estimate