---
title: 'Are Migrations Good for Your Career?'
date: 2025-10-29T14:36:20-07:00
hero_image: "/images/migration-birds.png"
categories:
  - small platform
email_preface: |
  Hello again {{FirstName}},

  I've written another article for you. This one is about software system migrations. They're interesting for a couple of reasons, and, similar to other big challenges, I've found them worthwhile to tackle time again throughout my career.

  You can also read this [on my website]({{ .Page.Permalink }}).

  Enjoy! And rise to those challenges before you!

  Steve Oxley
---

What is the hardest problem in computer science? Phil Karlton famously quipped, "There are only two hard things in Computer Science: cache invalidation and naming things." Martin Fowler has a number of variations of this quote on [his blog](https://martinfowler.com/bliki/TwoHardThings.html). My favorite is, "There's two hard problems in computer science: we only have one joke and it's not funny."

On a more serious note, Matt Ranney from DoorDash posits in [his talk](https://yowcon.com/sydney-2022/sessions/2373/migrations-the-hardest-actual-problem-in-computer-science) from a few years ago that migrations may actually be the hardest problem in computing. I think he has a point. We have a number of recent senior engineer hires at Seeq, and I see a suggestion from one or the other of them approximately weekly of some big improvement we could make to our software. The enthusiasm they bring is infectious, but we're soon reminded how difficult migrations are when we start thinking, "But how do we get from what we have now to this new idea?"

Matt Ranney's talk focuses mostly on his experience at DoorDash migrating from a monolith to microservices architecture. However, he brings up that migrations are an interesting case:  they are hard problems that many senior engineers avoid. Ranney argues that these hard migration problems can be good for your career, but most engineers don't see it that way. Ranney quips that senior engineers don't like to work on hard problems, they like to work on greenfield problems.

This brings up some intersting questions. Are migrations hard? Are they worthwhile from a career development standpoint? Should senior engineers be excited about them? As I noted in [Conquer Your Main Quest](https://www.stevenoxley.com/blog/2025/07/24/conquer-your-main-quest/), one of the projects that I've worked on at Seeq was a migration of our Software-as-a-Service (SaaS) offering from Virtual Machines (VMs) to Kubernetes. We'll take that as a model to evaluate these questions. Certainly, other migrations may yield different results, but this specific example can be generalized to similar projects.

Was this migration from VMs to Kubernetes hard? It certainly was. It was the most difficult project I've worked on in my career, and there were a number of different aspects to its difficulty.

From a people management perspective it was difficult. I had to build a team dedicated to setting up our Kubernetes resources, and, as I described in [Conquer Your Main Quest](https://www.stevenoxley.com/blog/2025/07/24/conquer-your-main-quest/), help make bandwidth for them to make progress on the move to Kubernetes.

From a project management perspective it was very difficult. Probably the most difficult project management problem was coordinating the downtime of the migrations with customers. We were fortunate that we could take some downtime, since each customer has their own deployment of Seeq. But coordinating that downtime was its own challenge, and kudos to my colleague [Olivia Reagan](https://www.linkedin.com/in/olivia-reagan/) for doing the bulk of it. We had to determine a good date and time to migrate 200+ SaaS deployments. Then we had to reach out to each customer via email, informing them about our intentions to move their deployment of Seeq, and working with them to coordinate a time that worked for them. Aside from the coordination effort, the migration required a lot of energy to push forward. If at any point someone stopped actively pushing towards completion it very quickly slowed or stalled. Stalling could happen at any point - getting started on building the Kubernetes clusters, deploying the clusters, setting up observability tooling, deciding what testing to do prior to deploying customers, and on and on. Each step of the way required relentless focus on the end goal and making sure we were progressing towards it.

From a technical perspective the migration Kubernetes was also very difficult. We had to design and implement networks, the architecture of the infrastructure stack including both the clusters themselves and some of the common tooling and services deployed into the clusters, automation for the migration, etc., and multiply most of that by two, since we support both Azure and AWS as cloud providers. I learned an incredible amount in the past few years, and I feel better equipped than ever to solve technical problems that may come my way.

Just one example of the technical challenge was the automation required for the migration itself. We had hundreds of these to perform, so we couldn't have a human manually entering all of the commands necessary on both the existing VMs and the Kubernetes clusters to do these migrations. The automated solution we (my colleague [Austin Sharp](https://www.linkedin.com/in/austinrsharp/) and I) came up with was a "migration-controller," written in Python, and running in the destination Kubernetes clusters. It would essentially do the following:
- Watch Seeq tenant namespaces in the Kubernetes cluster (the destinations)
- Use a "seeq.com/migration-state" label to transition the tenant through the migration process:
    - Connect to the customer's VM (the source), and periodically run rsync to sync data from the source VM to the destination tenant namespace in Kubernetes
    - Wait for the maintenance time that had been arranged with the customer
    - Shut down Seeq on the VM
    - Do a final rsync
    - Start up Seeq in the Kubernetes namespace
    - Update DNS records to point to the new Kubernetes namespace
    - Done
This migration-controller was a fun project to work on, but it was certainly a challenge. None of us had written a Kubernetes controller before (but it definitely hasn't been our last), and there were some lessons to be learned - like how to use the Kubernetes watch APIs and the best arguments to rsync for our use case (the [PostgreSQL](https://www.postgresql.org/docs/current/backup-file.html) documentation even has a paragraph about an important rsync option when syncing their database files).

So to summarize, this migration was difficult in about every dimension I can think of, but was it worthwhile - especially from a career development standpoint? Yes, it was. First of all, this migration has put our whole company in a much better position to scale. We have now started doing things like fleet-wide automation, continuous delivery, and shared services that would have been monumental efforts prior to managing our SaaS deployments in Kubernetes. And naturally, when you are a key person working on something that significantly improves the position of the company, you tend to be rewarded, and I was. I received a promotion that was at least partially tied to the success of the migration. Not only that, but the experience and learning along the way has been a major benefit to me in both my current position and wherever my career may lead. I have grown considerably through the people management, project management, and technical challenges that I faced during this migration project.

Looking back on my experience at Seeq migrating our SaaS systems from VMs to Kubernetes, I strongly agree with Ranney that migrations are hard and yet they are worthwhile projects to get involved in. As I mentioned above, this probably won't be true of every migration, but I can think of other examples in my career where migrations had significant benefits for me and/or people around me. Are migrations the hardest problem in computer science? I don't think so, but they are the hardest problem that almost every professional software engineer will encounter at some point in their career. When you're faced with a big migration project, don't shy away and go look for something greenfield. The migration will be better; see it for the opportunity to boost your career that it is.
