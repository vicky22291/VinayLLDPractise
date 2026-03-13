---
title: "Data Engineers of Netflix — Interview with Kevin Wylie"
author: "Netflix Technology Blog"
date: "Jul 14, 2021"
url: "https://netflixtechblog.com/data-engineers-of-netflix-interview-with-kevin-wylie-7fb9113a01ea"
tags: ['Data Engineering', 'Data', 'Big Data', 'Analytics', 'Culture']
---

# Data Engineers of Netflix — Interview with Kevin Wylie

**This post is part of our ******“Data Engineers of Netflix”****** series, where our very own data engineers talk about their journeys to ******Data Engineering @ Netflix******.**

![image](../images/b84b3e682ac9e8c1.png)

[**Kevin Wylie**](https://www.linkedin.com/in/kevinwylie/)** is a Data Engineer on the Content Data Science and Engineering team.** In this post, Kevin talks about his extensive experience in content analytics at Netflix since joining more than 10 years ago.

Kevin grew up in the Washington, DC area, and received his undergraduate degree in Mathematics from Virginia Tech. Before joining Netflix, he worked at MySpace, helping implement page categorization, pathing analysis, sessionization, and more. In his free time he enjoys gardening and playing sports with his 4 kids.

**His favorite TV shows:** Ozark, Breaking Bad, Black Mirror, Barry, and Chernobyl

Since I joined Netflix back in 2011, my favorite project has been designing and building the first version of our entertainment knowledge graph. The knowledge graph enabled us to better understand the trends of movies, TV shows, talent, and books. Building the knowledge graph offered many interesting technical challenges such as entity resolution (e.g., are these two movie names in different languages really the same?), and distributed graph algorithms in Spark. After we launched the product, analysts and scientists began surfacing new insights that were previously hidden behind difficult-to-use data. The combination of overcoming technical hurdles and creating new opportunities for analysis was rewarding.

## Kevin, what drew you to data engineering?

I stumbled into data engineering rather than making an intentional career move into the field. I started my career as an application developer with basic familiarity with SQL. I was later hired into my first purely data gig where I was able to deepen my knowledge of big data. After that, I joined MySpace back at its peak as a data engineer and got my first taste of data warehousing at internet-scale.

> _What keeps me engaged and enjoying data engineering is giving super-suits and adrenaline shots to analytics engineers and data scientists._

When I make something complex seem simple, or create a clean environment for my stakeholders to explore, research and test, I empower them to do more impactful business-facing work. I like that data engineering isn’t in the limelight, but instead can help create economies of scale for downstream analytics professionals.

## What drew you to Netflix?

My wife came across the Netflix job posting in her effort to keep us in Los Angeles near her twin sister’s family. As a big data engineer, I found that there was an enormous amount of opportunity in the Bay Area, but opportunities were more limited in LA where we were based at the time. So the chance to work at Netflix was exciting because it allowed me to live closer to family, but also provided the kind of data scale that was most common for Bay Area companies.

The company was intriguing to begin with, but I knew nothing of the talent, culture, or leadership’s vision. I had been a happy subscriber of Netflix’s DVD-rental program (no late fees!) for years.

> **After interviewing, it became clear to me that this company culture was different than any I had experienced.**

I was especially intrigued by the trust they put in each employee. Speaking with fellow employees allowed me to get a sense for the kinds of people Netflix hires. The interview panel’s humility, curiosity and business acumen was quite impressive and inspired me to join them.

I was also excited by the prospect of doing analytics on movies and TV shows, which was something I enjoyed exploring outside of work. It seemed fortuitous that the area of analytics that I’d be working in would align so well with my hobbies and interests!

## Kevin, you’ve been at Netflix for over 10 years now, which is pretty incredible. Over the course of your time here, how has your role evolved?

When I joined Netflix back in 2011, our content analytics team was just 3 people. We had a small office in Los Angeles focused on content, and significantly more employees at the headquarters in Los Gatos. The company was primarily thought of as a tech company.

At the time, the data engineering team mainly used a data warehouse ETL tool called Ab Initio, and an MPP (Massively Parallel Processing) database for warehousing. Both were appliances located in our own data center. Hadoop was being lightly tested, but only in a few high-scale areas.

Fast forward 10 years, and Netflix is now the leading streaming entertainment service — serving members in over 190 countries. In the data engineering space, very little of the same technology remains. **Our data centers are retired, Hadoop has been replaced by Spark, Ab Initio and our MPP database no longer fits our big data ecosystem.**

In addition to the company and tech shifting, my role has evolved quite a bit as our company has grown. When we were a smaller company, the ability to span multiple functions was valued for agility and speed of delivery. The sooner we could ingest new data and create dashboards and reports for non-technical users to explore and analyze, the sooner we could deliver results. But now, we have a much more mature business, and many more analytics stakeholders that we serve.

For a few years, I was in a management role, leading a great team of people with diverse backgrounds and skill sets. However, I missed creating data products with my own hands so I wanted to step back into a hands-on engineering role. My boss was gracious enough to let me make this change and focus on impacting the business as an individual contributor.

As I think about my future at Netflix, what motivates me is largely the same as what I’ve always been passionate about. I want to make the lives of data consumers easier and to enable them to be more impactful. As the company scales and as we continue to invest in storytelling, the opportunity grows for me to influence these decisions through better access to information and insights. The biggest impact I can make as a data engineer is creating economies of scale by producing data products that will serve a diverse set of use cases and stakeholders.

> If I can build beautifully simple data products for analytics engineers, data scientists, and analysts, we can all get better at Netflix’s goal: entertaining the world.

## Learning more

Interested in learning more about data roles at Netflix? You’re in the right place! Keep an eye out for our open roles in Data Science and Engineering by visiting our jobs site [here](https://jobs.netflix.com/search?team=Data+Science+and+Engineering). Our [culture](https://www.instagram.com/wearenetflix/?hl=en) is key to our impact and growth: read about it [here](https://jobs.netflix.com/culture). To learn more about our Data Engineers, check out our chats with [Dhevi Rajendran](https://netflixtechblog.medium.com/data-engineers-of-netflix-interview-with-dhevi-rajendran-a9ab7c7b36e5) and [Samuel Setegne](https://netflixtechblog.medium.com/data-engineers-of-netflix-interview-with-samuel-setegne-f3027f58c2e2).

---
**Tags:** Data Engineering · Data · Big Data · Analytics · Culture
