---
title: "Building Personalized Search Autosuggestion"
author: "Krishan Goyal"
date: "May 7, 2021"
url: "https://blog.flipkart.tech/building-personalized-autosuggestion-9e705d5bf5f8"
tags: ['Search', 'Autocomplete', 'Solr', 'Machine Learning', 'Backend']
---

# Building Personalized Search Autosuggestion

## What is Autosuggest?

At Flipkart, our goal is to minimize user effort to find the desired product. Autosuggest can be as simple as anticipating the rest of the letters in the word you’re typing.

For example, if you type the letters “sh” into a search box, “shoes”, “shirts”, “shorts” could be a few suggestions you might see.

It can also provide meaningful search suggestions based on the string you type. For example, if you type in “sh”, it can predict “shoes for women”, “shirts under 2000”. The suggested query need not even start with “sh”- “casual shorts”, “Nike shoes” are also valuable suggestions to users in certain situations.

The goal of autosuggest as a feature is to help the users type the right query quickly. Aside from saving the users their typing effort, it can also nudge towards the right query for the user’s shopping instance — which sometimes even the user is unaware of.

## Personalized Autosuggestion

Of the countless suggestions related to a typed query from the user, we display a selective few based on a [ranking function](https://en.wikipedia.org/wiki/Ranking_(information_retrieval)). If the user typed ‘sh’ in the search box, to predict the user-intended query correctly, the ranking function determines scores of suggestions based on parameters such as quality, prefix, and user data:

Of these, the user dependent aspects contribute to personalization. This article focuses on leveraging the recent searches of the user to improve suggestion prediction.

The following illustration displays how a user changes the searches more than once before deciding on one of them:

This is because users explore and compare multiple products before making a final selection. Autosuggest feature uses these recent searches to learn how the user changes the search queries and renders the next personalized auto suggestion accordingly.

## Personalization in autosuggestion — The Journey

Every user follows different shopping patterns where there is no overlap of the journey sequence across user profiles. Our aim is to personalise auto-suggestions for every user and not rely on pre-defined cohorts which don’t capture the required user-specific nuances.

The functional problem statement of personalizing autosuggest, translates to converting the scoring aspects into numerical features and defining a ranking function for the final scores of suggested queries.

The various stages involved are understanding the user intent, featurization, predictive modeling and the architecture to enable personalization.

### Understanding the user intent

We began with understanding users’ categorical intent through the Flipkart classification of products. The Flipkart catalog hierarchical structure has over 5000 categories organized as a tree.

We leveraged the Flipkart product classification tree to get the relationship understanding between categories and gauge the user intent. The closer the nodes are in a tree, the higher the similarity between them. For example, ‘Shoes and Sandals’ are more related than ‘Clothing and Footwear’ which are more related than ‘Fashion and Electronics’.

### Featurizing the query formulations

Let’s assume a user is trying to formulate a search query and we know his previous searches. We derive the [Features](https://en.wikipedia.org/wiki/Feature_(machine_learning)) based on the category relationships of previous searches and the plausible suggestions.

### Personalizing auto-suggestions using predictive models

As we began, we arrived at the autosuggestions manually, so we could use the lessons to fine tune the features and progressively scale up to the requirement. The following initial steps proved beneficial to further optimize our features:

For further optimization, we worked on implementing a predictive model which can also enable addition of more features.

Our training data for the model comprises the following parameters:

This training data is obtained by logging all the viewed suggestions for every prefix entered by the user. We ingest 1B+ events as part of this.

The models utilize the features to increase the score of clicked suggestions and decrease the score of un-clicked suggestions.

As the feature relationships are nonlinear, simple linear models did not prove effective. We worked on a [XgBoost model](https://towardsdatascience.com/https-medium-com-vishalmorde-xgboost-algorithm-long-she-may-rein-edd9f99be63d), which is an implementation of multiple additive [decision trees](https://towardsdatascience.com/a-guide-to-decision-trees-for-machine-learning-and-data-science-fe2607241956).

We observed that explainability is a challenge with nonlinear models — we can’t reason the decision made in a certain way. We use [feature importance](https://towardsdatascience.com/the-mathematics-of-decision-trees-random-forest-and-feature-importance-in-scikit-learn-and-spark-f2861df67e3) as a measure to understand the effectiveness of a feature . If we see the results differing from our theoretical understanding, we debug further.

Often we see models work well during development but don’t perform in production because of issues like sampling bias. By measuring the feature distribution gap in training and inference path, we identify and take steps to reduce the gap to match model performance in production with expectations.

### Component Architecture — A quick look

[Solr](https://solr.apache.org/guide/7_7/overview-of-searching-in-solr.html) forms the backbone of our search index and provides extensive capabilities to run complex ranking functions at large scale. Solr uses Lucene internally, which in simple terms contains a map of tokens (or words) to documents (or query).

Ranking millions of suggestions is expensive, and the runtime processing under autosuggest latency constraints (< 20 ms) is not feasible. Thus we cache the suggestions per prefix and only fall back on our search index on a cache miss.

One of the first concerns with personalisation is realising you can’t rely on cache to solve your problems.

### Working with latency constraints in personalising autosuggestions

As the latency is dependent on the number of suggestions we need to score, we restrict the number of suggestions to achieve latency targets.

A suggestion can be personalized or non-personalized based on the search categories of the previous user searches.

For example, if a user searches ‘Red shoes’ followed by ‘Nike shoes’, and then types the letter ‘a’ in the search box, ‘adidas shoes’ is a personalized suggestion, but ‘apple laptops’ is a non-personalized suggestion.

We cache the non-personalized suggestions because they are independent of the previous searches while we take only the personalized suggestions for scoring at runtime.

Of course, the architecture itself isn’t always enough and we did many low level optimisations to meet the latency guardrails.

The following are the optimizations that we did to scale up with the complexity of the predictive model:

Now, we can run complex models (over 100 trees to score a suggestion).

## What is the new autosuggest experience?

With the user’s previous searches, we can show more meaningful suggestions to the users. The personalized auto suggestions are more impactful in the later searches of a journey (>= Search #3) as we get better at understanding the user intent.

## What Next ?

The journey to personalise Autosuggest has just started. There is so much more to understand about a user as we move forward:

## Conclusion

The continued complexity to handle variety of ranking models at high scale and low latency — alongside other aspects of Autosuggest such as an ever-increasing index size, reacting in real time, enabling spell correction and language translation on the go will keep pushing the tech solutions forward.

---
**Tags:** Search · Autocomplete · Solr · Machine Learning · Backend
