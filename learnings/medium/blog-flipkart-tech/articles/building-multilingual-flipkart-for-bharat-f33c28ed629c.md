---
title: "Flipkart Goes Multilingual for Bharat"
subtitle: "How we are breaking language barriers to expand access to e-commerce"
author: "Shikhar Goel"
url: "https://blog.flipkart.tech/building-multilingual-flipkart-for-bharat-f33c28ed629c"
tags: ['Vernacular', 'Localization', 'Product Development', 'India', 'Machine Learning']
---

# Flipkart Goes Multilingual for Bharat

> How we are breaking language barriers to expand access to e-commerce

## Introduction

In the past decade, there has been a dramatic shift in how India shops. For a country steeped in a rich history of ‘bazaar shopping’, the benefits of e-commerce have slowly drawn Indian consumers away from brick and mortar stores towards the sheer convenience and choices that come with online shopping.

This switch certainly did not happen overnight. Metro users were the first to adopt e-commerce, thanks to good connectivity, sound knowledge of the internet, and relatively high disposable incomes. However, over the last few years, we have started seeing a new set of users emerge following a surge in network connectivity and lower data costs across the country, ensuring easy access to the internet by residents of Tier II cities and beyond, which we call ‘Bharat’.

These next set of customers are very different from the existing users. They have their own aspirations and challenges. One of their biggest challenges is the comprehension of language. As our interface was built in English, it was a challenge for them to use Flipkart. To help these users we launched Flipkart in Hindi last year and were overwhelmed by the response. We have now expanded our interface in three new Indic languages — Tamil, Telugu, and Kannada.

## Why Vernacular?

India is a massive country with more than 1.3 billion people and is very culturally diverse. There is a famous proverb in Hindi that captures this well,

> कोस-कोस पर बदले पानी , चार कोस पर वाणी..  
> (Like the taste of water, the language changes every few miles in India)

In fact, there are 22 scheduled languages, so there is no one language that fits all. Earlier there were mainly English internet users, so the apps were made to cater to them. But the evolution of Bharat has paved the way for ‘internet for all’. These new internet users prefer their native Indic languages over English.

From a[ report](https://assets.kpmg/content/dam/kpmg/in/pdf/2017/04/Indian-languages-Defining-Indias-Internet.pdf) published by KPMG, it is projected that by 2021, there will be 735 million internet users in India and 536 million (~73%) of them will be Indic language internet users, meaning 9 out of every 10 new internet users will be vernacular users. As a home grown company, it was important for us to cater to the needs of this new segment.

## Where did we start?

Before we began, we wanted to gain an in-depth understanding of the market and get an insightful overview of the life of these users

To answer these questions, we carried out an in-depth analysis across multiple cities — Coimbatore, Mysore, Visakhapatnam, and Salem. The research was an eye-opener as a lot of our assumptions were proved wrong. Also, their inability to comprehend English affected their confidence to shop online independently thus shying away from the app. Following is a quote from a user in Coimbatore,

> _Using the App in Tamil is convenient and it easily registers in my mind. If it’s in English, I can read, but I always have a doubt whether I have understood it correctly._

We realised that the needs and expectations of this user segment were very different from that of our existing users. Broadly speaking, these users expect

## What was the challenge?

Every language has its own nuances. We wanted to understand and capture these nuances to design the right experience.

We carried out a second study to better understand this key insight to help us define the right tone for each of these languages.

## Nuances in translation

We identified that there were a lot of differences in the way users of a region wanted translations or how their language sounded. While users in North India wanted a partial translation of the app (a mix of both Hindi and English), users in the South wanted a far higher percentage of the app to be translated into their respective languages.

Even translation nuances differed a lot between the three south indian states. For instance, Kannada speakers wanted colors to be transliterated. This meant ‘blue’ in English would remain ಬ್ಲೂ (blue) in Kannada instead of ನೀಲಿ (_neeli_), the translated word. Whereas, Tamil and Telugu speakers wanted colours to be translated as நீலம் (_neelam_) and నీలం (_neelam_) respectively.

Apart from the differences in translation expectations, there were also some challenges while transliterating English words in Indic languages. As few Indic languages do not have certain English sounds, certain English words cannot be written as is, in the Indic language. For example, in Tamil, there is no phonetic equivalent for the letter ‘F’ (such as the ‘F’ in Flipkart), when it is transliterated it reads as பிளிப்கார்ட் (_Piḷipkārṭ_) which doesn’t sound right!

## Striking the right tone

In terms of the tone, we understood the following expectations:

By the end of the second study, we realized that this was a challenge with a ‘one size fits all’ solution. It was critical to understand these nuances and address them in our approach. The success of this project would lie in being able to cater to our audience in a language that they not only understand but also feel comfortable with, thus building trust.

> As Nelson Mandela once said, “If you talk to a man in a language he understands, that goes to his head. If you talk to him in his language, that goes to his heart.”

## Building the tech stack

Introducing vernacular support involved changes from the ground up. Over 20 teams collaborated to make this possible. Every Service, User Interface (UI), Reporting platform and Operations procedure programmed for English had to be modified and scaled up.

We have built a vernacular platform in Flipkart that forms the backbone of localization and translation of the app. The capability can be scaled up to support any number of languages at once.

## Vernacular Platform Services

The core capability of this platform is to display localized app to the user, detect language for a word and provide runtime translations.

### Language Detection

Users prefer to write in their own language and may either use the native script or Roman script _(Hinglish etc)_. Users also use multiple languages at times; for example, when providing review, a user may switch from Hindi and English, which is known as Code-mixing. We have built an in-house machine learning model to detect the language of the text for various Indian languages along with detecting code-mixing and identifying the relevant parts of the text in each language. Further downstream processes are chosen to perform moderation, translation, sentiment analysis, etc based on the detected language.

### Localization

The Localization system stores all static strings that are displayed to the end-user in one common place to build the support for new languages in a scalable way.

### Transliteration

Transliteration is a system of converting text from one language to another based on phonetic similarity, such that the pronunciation of the word remains the same in both the languages. For example, the word “Brand” can be transliterated as “ब्रांड” in Hindi.

We have built in-house deep learning models for detecting which words need to be transliterated given the context and provide the correct transliteration with very high accuracy for English to Indic languages and vice versa. It is being used to transliterate certain product categories and user addresses.

### Translation

We are building an in-house neural machine translation model to translate various types of data in e-commerce such as catalog data, search queries, user generated content. The translation system is based on a sequence to sequence translation model using the transformer architecture shown below:

_The transformer architecture. Image source: _[_Encoder-decoders in Transformers_](https://medium.com/huggingface/encoder-decoders-in-transformers-a-hybrid-pre-trained-architecture-for-seq2seq-af4d7bf14bb8)

## On-boarding Solution

We are building multi-tenancy in the vernacular platform to help other services to onboard on to the platform with relative ease. These services can benefit from the existing localization platform, auto translation and human QC & translation capabilities. This will help other services to launch their vernacular experiences within weeks instead of months. It will also help them to scale up new languages automatically as soon as a support for a new language is launched.

## Way Forward…

With all our features for localization including language store, machine translations, language detection etc

With each new language we support for Bharat, we aim to build digital confidence through accessibility, awareness, and assistance. The new users we welcome, can enjoy the benefits and convenience of online shopping in their own languages. We hope that making our platform easy to use will remove friction in using e-commerce. After all, we know that all we need to do to anticipate our next language is follow where the water flows.


---

**_Related:_**_ Another Flipkart initiative for Bharat that explores how we’re using voice to support users across dialects and languages, _[_The future of voice-powered shopping in the land of language: Flipkart’s journey to build a conversational assistant platform for India’s next 200 million e-commerce users_](https://tech.flipkart.com/the-future-of-voice-powered-shopping-in-the-land-of-language-db50c99edd77)

---
**Tags:** Vernacular · Localization · Product Development · India · Machine Learning
