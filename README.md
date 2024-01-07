# DB tools comparison

Let's compare [Skunk](https://typelevel.org/skunk/) vs [Doobie](https://tpolecat.github.io/doobie/) db tools for Scala.

So, in order to do it, I am going to cover the following points: 
* Consider main difference - doobie that is using jdbc vs skunk that is using pure postgresql protocol; 
* Try to work with benchmarks
	* Understand what kind of benchmarks we need for comparison:
		* Compile time benchmarks;
		* Execution time benchmarks;
	* Choose tools for performing benchmarks;
* Create an example project for Skunk and Doobie;
* Compare query building, transactions, sessions pools and any tools for them, error logging;
* Try to work with ltree structure;

## JDBC vs pure PostgreSQL protocol
TBD

* [ ] Rob Norris talk on Scala Days 2019 about Skunk 
    https://www.youtube.com/watch?v=NJrgj1vQeAI
    * [ ] Add link on PostgreSQL doc about pure protocol;
* [ ] Check out materials about Skunk in the book [Practical FP in Scala](https://leanpub.com/pfp-scala) by [Gabriel Volpe](https://twitter.com/volpegabriel87).

Check out these links found on the internet while searching Skunk vs Doobie:
* [ ] Investigate this - https://www.libhunt.com/compare-skunk-vs-doobie
* [ ] and this - https://www.baeldung.com/scala/skunk-postgresql-driver

Check out these materials from a DevInsideYou Scala blogger:
* [ ] https://youtu.be/J5I_HEUKsF0?si=2-d4PWO9ZhiBPPCH
* [ ] https://youtu.be/kafn3qKd9Pw?si=7rq6qZnrPVe4sXAw

Think about these questions:
* Why has there been no more info about Skunk since 2021?
* Why do we not have much information about Skunk?
* Do we know some projects that are using Skunk?
* Can we use the ltree schema type from Skunk in Doobie?

## Benchmarks
[Typo](https://oyvindberg.github.io/typo/) has a blog with the following [article](https://oyvindberg.github.io/typo/blog/the-cost-of-implicits). It compares [Skunk](https://typelevel.org/skunk/) vs [Doobie](https://tpolecat.github.io/doobie/) DB tools for Scala and describes compile-time benchmarks. 

Personally, I'm not sure that these benchmarks are correct due to the following reasons:
* using only a timestamp as a test parameter;
* timestamp cost might be greater than the actual compile work;
* benchmarks run only 3 times;
* benchmarks run without warm-ups;
* [bleep](https://bleep.build/docs/my-build-does-more/) building tool is used, but not the widely used [sbt](https://www.scala-sbt.org/);

From my point of view, we have to use best practices that have been tested a long time ago and have proven their reliability. I'm talking about *Java Microbenchmark Harness* (JMH).

In order to write benchmarks in a correct way, we have to count many things, and it definitely hard work to write a correct benchmark. Aleksei Shipilev greatly described what we must know while trying to write correct benchmarks [here](https://shipilev.net/blog/2014/nanotrusting-nanotime/). Also, you can check out his talk [here](https://www.youtube.com/watch?v=8pMfUopQ9Es).

After investigation of these materials, I decided to use JMH for writing benchmarks to compare Skunk and Doobie. Another question: what do we want to compare? I think the first thing we have to compare is compile-time, as it was done in the Typo article. The second thing is the performance of fetching results from a DB. 

Summing it up, we want to test the following things:
* compile time;
* performance of fetching data records (this point is under the question; maybe it isn't worth to test it because we might test only DB's performance but not the performance of the db tools - so this point have to be considered additionally before implementing it);
	* Memory benchmark? (https://github.com/sbt/sbt-jmh#using-java-flight-recorder--async-profiler);
	* Garbage collector and how much cleaning was done (add `-prof gc` while running a benchmark);

In order to write benchmarks for Scala using JMH, we can use the following tools:
* [sbt-jmh](https://github.com/sbt/sbt-jmh) - to measure performance;
* [compiler-benchmark](https://github.com/scala/compiler-benchmark) - to measure compile time;

For compile-time benchmarks, I am going to count and play with these parameters:
* java versions/graal vm;
* Scala versions 2.12, 2.13, 3;
* different DB tools - Doobie (considering different jdbc drivers) vs Skunk;
* different query tests/examples (considering corner cases with empty queries), including ltrees;

## Example project
There is an example project for Skunk and Doobie - https://github.com/AlekseiLitkovetc/db-tools-comparison

TBD:
* [x] Add query examples for Skunk and investigate its API;
* [ ] Add similar examples for Doobie;
* [ ] Do exercises from the Skunk tutorial https://typelevel.org/skunk/tutorial/ and check how errors are logged;
* [ ] After that, do the same with Doobie and check its error logging logic (does it exist?);