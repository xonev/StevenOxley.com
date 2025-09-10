---
title: 'Trial and Error Driven Development'
date: 2025-09-09T21:18:48-07:00
hero_image: /images/trial-and-error-hammer.png
categories:
- small platform
email_preface: |
  Hi there {{FirstName}},

  As I was working on this one, I enjoyed reflecting back on when programming and building software was new and exciting! I remembered the experience of reading Code Complete for the first time, and experiencing new insights on almost every page. Well... building software isn't so new for me anymore, but it is still exciting! I hope you're enjoying your work, too.

  Steve Oxley

  You can also read this [on my website]({{ .Page.Permalink }}).
---

One of the things that I learned (fortunately) relatively early on in my career is that it's actually important to understand how things work.

I remember in college working on assignments where we would receive some framework code to work with. It wouldn't compile. I would bash against each compiler error until I would finally get it to compile. Then I'd add my changes. More bashing to get that to compile. I'd run it. It wouldn't give the right output. More changing things. More bashing. More running. Finally I'd get it to output the "happy path" result for the assignment. Then I'd turn it in and get a good grade. But I didn't really understand 90% of how the thing worked. I had mostly brute forced the assignment.

It wasn't until I read [*Code Complete*](https://en.wikipedia.org/wiki/Code_Complete) that I bought into the idea that I actually needed to understand what I was doing. I'm actually not sure what part of *Code Complete* convicted me that I was going about things the wrong way. It may have been in the chapter on debugging. McConnell presents debugging as an opportunity to learn something. To properly fix something, you need to understand it. And he contrasts that with "The Devil's Guide to Debugging":

> In Dante's vision of hell, the lowest circle is reserved for satan himself. In modern times, Old Scratch has agreed to share the lowest circle with programmers who don't learn to debug effectively. He tortures programmers by making them use these common debugging approaches:
>
> ***Find the defect by guessing*** To find the defect, scatter print statements randomly throughout a program. Examine the output to see where the defect is. If you can't find the defect with print statements, try changing things in the program until something seems to work. Don't back up the original version of the program, and don't keep a record of the changes you've made. Programming is more exciting when you're not quite sure what the program is doing. Stock up on cola and candy because you're in for a long night in front of the terminal.
>
> ***Don't waste time trying to understand the problem*** It's likely that the problem is trivial, and you don't need to understand it completely to fix it. Simply finding it is enough.
>
> ***Fix the error with the most obvious fix*** It's usually good just to fix the specific problem you see, rather than wasting a lot of time making some big, ambitious correction that's going to affect the whole program. This is a perfect example:
>
> ```
> x = Compute( y )
> if ( y = 17 )
>   x = $25.15 -- Compute() doesn't work for y = 17, so fix it
> ```
> Who needs to dig all the way into *Compute()* for an obscure problem with the value of *17* when you can just write a special case for it in the obvious place?

I'm embarrassed to say that all of those debugging approaches described me. I felt like it would take me too long to invest the time to dig to the bottom of why things weren't working as I expected. So I would take every shortcut possible to brute force my way through to the next problem. After I decided that the brute force approach wasn't working well, I found that - at first - debugging and programming were slower. It took me longer to figure out what was going on, and it could be fairly frustrating. But then when I solved the problem, I actually knew why the fix worked. Over time, these learnings started to pay off like any good investment. Future debugging took less time because I was more likely to understand the problem immediately.  The investment to understand each step of what I'm doing has paid off many times over throughout my career.

A recent example I saw of the brute force, trial and error approach was working with some colleagues on some [PromQL](https://prometheus.io/docs/prometheus/latest/querying/basics/) queries. Prometheus is pretty complicated; there are time series, range vectors, instant vectors. And there are functions and operators that work on these different data types. And you have to know which functions/operators output which data types and which functions/operators accept which data types. Also, once you put these queries in a dashboard you can use transformations to join queries together and perform other black magic. I'm still learning it. But Prometheus and its query language are a perfect example of the type of system where it's seemingly a lot easier to hammer away at the problem until you see what you (think you) want on the screen and then move on. However, when you dig a little deeper and start to try to understand how the query works, you can sometimes find that you weren't displaying what you thought or that there are errors in a calculation. In this particular instance, we were attempting to group an uptime report's table rows by customer ID. We had recently added customer ID as a label to the time series represented in the table. Almost all the rows in the table showed the new customer ID, but there was one that didn't. We couldn't explain why. After some discussion and investigation we realized we were joining a couple of different queries on a label value that would be the same whether it was from the old time series prior to adding the customer ID label or the new series after adding the customer ID label. We wouldn't have discovered that potential issue unless we had recognized that the results we were looking at didn't match the mental model we had of how the queries were supposed to work.

To summarize, if you're early in your career, I suggest you develop a sense for when you aren't fully understanding something. Question your assumptions about how things work, test your hypotheses, and only be satisfied when your hypotheses are verified. If you know someone early in their career send them this article so they can avoid the pain that I experienced of banging my head against the wall when I could have been using it to better understand how things work.
