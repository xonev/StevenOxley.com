---
layout: post
title: "Book Review of Shape Up: The Hipster's Waterfall"
slug: book-review-shape-up
date: 2020-10-19 23:00:00 -0700
comments: true
categories:
  - books
  - management
---

I have finally finished reading _Shape Up: Stop Running in Circles and Ship Work that Matters_ by Ryan Singer after many months. Now that I've made it through the book, I would subtitle it "The Hipster's Waterfall."

To be fair, it is a thought-provoking book. It has some good ideas for new techniques in managing software projects. It is well written and clear, and I think Singer does a decent job of arguing for the processes and techniques he recommends. I just don't agree with some of his more foundational points and therefore I do not agree with the general thesis of the book.

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

[Marty Cagan's writing](https://svpg.com/product-vs-feature-teams/) has influenced me greatly, and I have learned that my desire is for empowered product teams. The empowered product teams that Cagan describes seem to be the direct opposite of the teams that Singer describes in this book. Basecamp doesn't have a cohesive team that collaborates amongst themselves and with customers to reach a solution to a business problem. Basecamp has a team that decides on which business problem to solve, designs a solution that is meant to solve the problem, and then hands it off to other people to implement. I am not a fan.

In his articles, Cagan points out that there are four risks associated with creating or improving a product:
- *Value* risk (will people buy it, or choose to use it?)
- *Usability* risk (can users figure out how to use it?)
- *Feasibility* risk (can we build it with the time, skills, and technology we have?)
- *Business Viability* risk (will this solution work for the various dimensions of our business?)

With an empowered product team, the team is responsible for all of these risks. The team's Product Manager is responsible for value and business viability. The Designer is responsible for usability. The Technical Lead is responsible for feasibility. This works well because each responsibility is owned by someone on the team who can either communicate directly with the people doing the implementation or perform the implementation themself.

In the process that Basecamp uses, all of these responsibilities partially or wholly lie with the shapers, and the actual implementation of the solution is handed off to another team - a delivery team. I think a good term for this type of process is _insourcing_. The shapers identify a problem their customers have, design a solution to the problem, and decide whether it's usable, feasible, and a fit with the business. Then they hand the work off to their insourced design and development teams to implement the solution.

Another issue that I have with this book is its recommendation of [six week cycles](https://basecamp.com/shapeup/0.3-chapter-01#six-week-cycles). Singer claims, "six weeks is long enough to build something meaningful start-to-finish and short enough that everyone can feel the deadline looming from the start, so they use the time wisely."

Maybe they don't have any procrastinators at Basecamp, but I think most would agree that a six week deadline doesn't loom very large. Anyway, I think Singer is missing the point of why you would want a shorter iteration. A shorter iteration provides more frequent opportunities to interact with the customer and course correct.

The other benefit of shorter iterations is smaller batches of work. There is power in [small batches](http://www.startuplessonslearned.com/2011/09/power-of-small-batches.html)

There is a name for a process that features up-front design which is handed off for development and long feature development cycles. The name for this process is waterfall. Basecamp uses waterfall with some minor adjustments to make it more palatable.

The interesting thing is that Basecamp is clearly making waterfall work for them. There is no denying they have a successful company, so no one is really in a place to criticize their approach. I personally wouldn't enjoy working there, but when you have bootstrapped your company and are profitable you have a lot of flexibility to run things the way you want. And therein lies the true success of Basecamp.

So, in summary, I wouldn't recommend the overall process that Singer recommends in _Shape Up_, but there were still some tidbits of wisdom within. I don't recommend reading this one.
