---
title: "Netflix Public Bug Bounty, 1 year later"
author: "Netflix Technology Blog"
date: "Mar 21, 2019"
url: "https://netflixtechblog.com/netflix-public-bug-bounty-1-year-later-8f075b49d1cb"
tags: ['Security']
---

# Netflix Public Bug Bounty, 1 year later

by [Astha Singhal](https://twitter.com/astha_singhal) (Netflix Application Security)

As Netflix continues to create entertainment people love, the security team continues to keep our members, partners, and employees secure. The security research community has partnered with us to improve the security of the Netflix service for the past few years through our responsible disclosure and bug bounty programs. A year ago, we launched our public bug bounty [program](https://medium.com/netflix-techblog/netflixbugbounty-ae3bf4489def) to strengthen this partnership and enable researchers across the world to more easily participate.

When we decided to go public with our bug bounty, we revamped our program terms to bring even more targets (including Netflix streaming mobile apps) in scope and set clearer guidelines for researchers that participate in our program. We have always tried to prioritize a good researcher experience in our program to keep the community engaged. For example, we maintain an average triage time of less than 48 hours for issues of all severity. Since the public launch, **we have engaged with 657 researchers from around the world. We have collectively rewarded over $100,000 for over 100 valid bugs** in that time.

We wanted to share a few interesting submissions that we have received over the last year:

- We choose to [focus](https://www.youtube.com/watch?v=L1WaMzN4dhY) our security resources on applications deployed via our infrastructure paved road to be able to scale our services. The bug bounty has been great at shining a light on the parts of our environment that may not be on the paved road. A researcher found an application that ran on an older Windows server that was deployed in a non-standard way making it difficult for our automated visibility services to detect it. The system had significant issues that we were grateful to hear about so we could retire the system.
- We received a report from a researcher that found a service they didn’t believe should be available on the internet. The initial finding seemed like a low severity issue, but the researcher asked for permission to continue to explore the endpoint to look for more significant issues. We love it when researchers reach out to coordinate testing on an issue. We looked at the endpoint and determined that there was no risk of access to Netflix internal data, so we allowed the researcher to continue testing. After further testing, the researcher found a remote code execution that we then rewarded and remediated with a higher priority.
- We always perform a full impact analysis to make sure we reward the researcher based on the actual impact vs demonstrated impact of any issue. In one example of this, a researcher found an endpoint that allowed access to an internal resource if the attacker knew a randomly generated identifier. The researcher had found the identifier via another endpoint, but had not explicitly linked the two findings. Given the potential impact, we asked the researcher to stop further testing. As we tested the first endpoint ourselves, we discovered this additional impact and issued a reward in accordance with the higher priority.

Over the past year, we have received various high quality submissions from researchers, and we want to continue to engage with them to improve Netflix security. [Todayisnew](https://bugcrowd.com/todayisnew) has been the highest earning researcher in our program over the last year. We recently revisited our overall reward ranges to make sure we are competitive with the market for our risk profile. In 2019, we also started publishing quarterly program updates to highlight new product areas for testing. Our goal is to keep the Netflix program an interesting and fresh target for bug bounty researchers.

Going into next year, our goal is to maintain the quality of the researcher experience in our program. We are also thinking about how to extend our bug bounty coverage to our studio app ecosystem. Last year, we conducted a bug bash specifically for some of our studio apps with researchers across the world. We found some significant issues through that and are exploring extending our program to some of our studio production apps in 2019. We thank all the researchers that have engaged in our program and look forward to continued collaboration with them to secure Netflix.

---
**Tags:** Security
