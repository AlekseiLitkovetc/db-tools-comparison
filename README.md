# DB tools comparison
<!-- markdownlint-disable MD033 -->

Let's compare [Skunk](https://typelevel.org/skunk/) vs [Doobie](https://tpolecat.github.io/doobie/) db tools for Scala.

## JDBC vs pure PostgreSQL protocol

Doobie is a pure functional JDBC library. This is a great library, but it still has some minuses:

* Using JDBC;
* Bad diagnostics (partly due to JDBC, partly due to Doobie);
* Auto-derivation of row codecs is probably bad - takes a long time to derive things we want
  * With Scala 3 Doobie does a better auto-derivation;

Skunk tries to solve these problems for PostgreSQL database:

* Speals the wire protocol directly (no driver, no indirection);
* Completly non-blocking (we don't have to worry about thread pools). All message handling is asynchronous;
* API mirrors the operational semantics of Postgres;
* Beautiful and comprehensive error reporting;
* Build with cats, cats-effect, fs2, scodec;

### Talking with PostgreSQL

Communication with PostgreSQL happens via a TCP Socket connection by exchanging byte array messages:

<img src="./images/Talking_with_PostgreSQL.png" alt="Talking with PostgreSQL" width="40%" height="40%"/>

And it's documented in PostgreSQL documentation - [Chapter 55. Frontend/Backend Protocol](https://www.postgresql.org/docs/current/protocol.html)

So, after reading the PostgreSQL documantation, if we try to implement communication with DB without JDBC, we probably go through the following steps:

1. Get a tool for communication via TCP by socket - [fs2.io.tcp.Socket](https://www.javadoc.io/doc/co.fs2/fs2-io_3/latest/index.html);
2. To read and write raw data (bytes) from PostgreSQL, implement a BitVectorSocket (use scodec for conversions);
3. Implement logic for reading different types of messages from PostgreSQL
   message = tag (1 byte) + length (4 bytes) + payload (<lenght> - 4 bytes)
4. Test implementation - start communication with PostgreSQL with a StartUp message (in order to get a DB session);
5. Go to the next abstraction level - implement `MessageSocket` ([skunk.net.MessageSocket](https://www.javadoc.io/doc/org.tpolecat/skunk-core_3/latest/index.html)) that speaks in terms of PostgreSQL messages;

At the end of implementation, a stack will look like this:

<img src="./images/Full_basic_stack_of_Skunk.png" alt="Full stack" width="25%" height="25%"/>

<font size = "2">This section is based on Rob Norris [talk](https://www.youtube.com/watch?v=NJrgj1vQeAI) on Scala Days 2019 about Skunk</font>

## Useful materials

[Practical FP in Scala](https://leanpub.com/pfp-scala) by [Gabriel Volpe](https://twitter.com/volpegabriel87). This book contains material about Skunk.

RockTheJVM article about Doobie - <https://blog.rockthejvm.com/doobie/>

Check out these materials from a DevInsideYou Scala blogger:

* [ ] <https://youtu.be/J5I_HEUKsF0?si=2-d4PWO9ZhiBPPCH>
* [ ] <https://youtu.be/kafn3qKd9Pw?si=7rq6qZnrPVe4sXAw>

## Benchmarks

[Typo](https://oyvindberg.github.io/typo/) has a blog with the following [article](https://oyvindberg.github.io/typo/blog/the-cost-of-implicits). It considers [Doobie](https://tpolecat.github.io/doobie/) and describes compile-time benchmarks. So, the article is pretty interesting.

---

We would like to write benchmarks too. So, we have to use best practices that have been tested a long time ago and have proven their reliability. I'm talking about *Java Microbenchmark Harness* (JMH).

In order to write benchmarks in a correct way, we have to count many things, and it definitely hard work to write a correct benchmark. Aleksei Shipilev greatly described what we must know while trying to write correct benchmarks [here](https://shipilev.net/blog/2014/nanotrusting-nanotime/). Also, you can check out his talk [here](https://www.youtube.com/watch?v=8pMfUopQ9Es).

After investigation of these materials, we decided to use JMH for writing benchmarks to compare Skunk and Doobie.

It would be great if we play with these parameters:

* java versions/graal vm/project Loom;
* Scala versions 2.12, 2.13, 3;
* different DB tools - Doobie (considering different jdbc drivers) vs Skunk;
* different query tests/examples (considering corner cases with empty queries), including ltrees;

To run the "Hello World" benchmark use the following command:

```shell
sbt 'db-benchmarks/Jmh/run -i 1 -wi 1 -f 1 -rf csv .*Hello.*'
```

### Skunk runtime errors

Play with the code block in [Experiments.scala](./db-examples/src/main/scala/ru/fsacala/dbtool/examples/Experiments.scala#L52) - `experiment02` to face with errors like these (or others):

<details>
  <summary>[error] Exactly one row was expected, but more were returned</summary>
  
  ```text
  [error] skunk.exception.SkunkException:
  [error] 🔥  
  [error] 🔥  Skunk encountered a problem related to use of unique
  [error] 🔥    at /home/aleksei/IdeaProjects/db-tools-comparison/db-examples/src/main/scala/ru/fsacala/dbtool/examples/Experiment.scala:71
  [error] 🔥  
  [error] 🔥    Problem: Exactly one row was expected, but more were returned.
  [error] 🔥       Hint: You used unique. Did you mean to use stream?
  [error] 🔥  
  [error] 🔥  The statement under consideration was defined
  [error] 🔥    at /home/aleksei/IdeaProjects/db-tools-comparison/db-examples/src/main/scala/ru/fsacala/dbtool/examples/Experiment.scala:62
  [error] 🔥  
  [error] 🔥    SELECT name, population
  [error] 🔥    FROM   country
  [error] 🔥    WHERE  name LIKE $1
  [error] 🔥  
  [error] 🔥  and the arguments were provided
  [error] 🔥    at /home/aleksei/IdeaProjects/db-tools-comparison/db-examples/src/main/scala/ru/fsacala/dbtool/examples/Experiment.scala:71
  [error] 🔥  
  [error] 🔥    $1 varchar    U%
  [error] 🔥  
  [error] skunk.exception.SkunkException: Exactly one row was expected, but more were returned.
  ```

</details>

<details>
  <summary>[error] Expected at most one result, more returned</summary>
  
  ```text
  [error] skunk.exception.SkunkException: 
  [error] 🔥  
  [error] 🔥  Skunk encountered a problem related to use of option
  [error] 🔥    at /home/aleksei/IdeaProjects/db-tools-comparison/db-examples/src/main/scala/ru/fsacala/dbtool/examples/Experiments.scala:72
  [error] 🔥  
  [error] 🔥    Problem: Expected at most one result, more returned.
  [error] 🔥       Hint: Did you mean to use stream?
  [error] 🔥  <!-- markdownlint-disable MD033 -->

  [error] 🔥  The statement under consideration was defined
  [error] 🔥    at /home/aleksei/IdeaProjects/db-tools-comparison/db-examples/src/main/scala/ru/fsacala/dbtool/examples/Experiments.scala:62
  [error] 🔥  
  [error] 🔥    SELECT name, population
  [error] 🔥    FROM   country
  [error] 🔥    WHERE  name LIKE $1
  [error] 🔥  
  [error] 🔥  and the arguments were provided
  [error] 🔥    at /home/aleksei/IdeaProjects/db-tools-comparison/db-examples/src/main/scala/ru/fsacala/dbtool/examples/Experiments.scala:72
  [error] 🔥  
  [error] 🔥    $1 varchar    U%
  [error] 🔥  
  [error] skunk.exception.SkunkException: Expected at most one result, more returned.
  ```

</details>
<br/>

These errors look exhaustive. A description contains a problem and even suggestions (hints) for fixing it.

### Doobie runtime error

Let's compare Skunk vs Doobie errors, check out these ones from Doobie for identical queries in [ExperimentDoobieError.scala](./db-examples/src/main/scala/ru/fsacala/dbtool/examples/ExperimentDoobieError.scala#L26):

<details>
  <summary>[error] Exactly one row was expected, but more were returned (description from Skunk)</summary>
  
  ```text
  [error] doobie.util.invariant$UnexpectedContinuation$: Expected ResultSet exhaustion, but more rows were available.
  [error]         at doobie.util.invariant$UnexpectedContinuation$.<clinit>(invariant.scala:26)
  [error]         at doobie.hi.resultset$.getUnique$$anonfun$1(resultset.scala:206)
  [error]         at cats.free.Free.step(Free.scala:77)
  [error]         at cats.free.Free.foldMap$$anonfun$1(Free.scala:164)
  [error]         at cats.data.KleisliFlatMap.tailRecM$$anonfun$1$$anonfun$1(Kleisli.scala:701)
  [error]         at cats.StackSafeMonad.tailRecM(StackSafeMonad.scala:37)
  [error]         at cats.StackSafeMonad.tailRecM$(StackSafeMonad.scala:34)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error]         at tailRecM$$anonfun$1 @ doobie.util.transactor$Transactor$$anon$4.apply$$anonfun$3(transactor.scala:165)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error]         at tailRecM$$anonfun$1 @ doobie.util.transactor$Transactor$$anon$4.apply$$anonfun$3(transactor.scala:165)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error]         at tailRecM$$anonfun$1 @ doobie.util.transactor$Transactor$$anon$4.apply$$anonfun$3(transactor.scala:165)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error]         at tailRecM$$anonfun$1 @ doobie.util.transactor$Transactor$$anon$4.apply$$anonfun$3(transactor.scala:165)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error]         at tailRecM$$anonfun$1 @ doobie.util.transactor$Transactor$$anon$4.apply$$anonfun$3(transactor.scala:165)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error]         at tailRecM$$anonfun$1 @ doobie.util.transactor$Transactor$$anon$4.apply$$anonfun$3(transactor.scala:165)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error] Nonzero exit code returned from runner: 1
  ```

</details>

<details>
  <summary>[error] Expected at most one result, more returned (description from Skunk)</summary>
  
  ```text
  [error] doobie.util.invariant$UnexpectedContinuation$: Expected ResultSet exhaustion, but more rows were available.
  [error]         at doobie.util.invariant$UnexpectedContinuation$.<clinit>(invariant.scala:26)
  [error]         at doobie.hi.resultset$.getOption$$anonfun$1(resultset.scala:218)
  [error]         at cats.free.Free.step(Free.scala:77)
  [error]         at cats.free.Free.foldMap$$anonfun$1(Free.scala:164)
  [error]         at cats.data.KleisliFlatMap.tailRecM$$anonfun$1$$anonfun$1(Kleisli.scala:701)
  [error]         at cats.StackSafeMonad.tailRecM(StackSafeMonad.scala:37)
  [error]         at cats.StackSafeMonad.tailRecM$(StackSafeMonad.scala:34)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error]         at tailRecM$$anonfun$1 @ doobie.util.transactor$Transactor$$anon$4.apply$$anonfun$3(transactor.scala:165)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error]         at tailRecM$$anonfun$1 @ doobie.util.transactor$Transactor$$anon$4.apply$$anonfun$3(transactor.scala:165)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error]         at tailRecM$$anonfun$1 @ doobie.util.transactor$Transactor$$anon$4.apply$$anonfun$3(transactor.scala:165)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error]         at tailRecM$$anonfun$1 @ doobie.util.transactor$Transactor$$anon$4.apply$$anonfun$3(transactor.scala:165)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error]         at tailRecM$$anonfun$1 @ doobie.util.transactor$Transactor$$anon$4.apply$$anonfun$3(transactor.scala:165)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error]         at tailRecM$$anonfun$1 @ doobie.util.transactor$Transactor$$anon$4.apply$$anonfun$3(transactor.scala:165)
  [error]         at flatMap @ doobie.WeakAsync$$anon$1.flatMap(WeakAsync.scala:29)
  [error] Nonzero exit code returned from runner: 1
  ```

</details>
<br/>

In comparison with Skunk, Doobie's error description looks poor.

# Bonus: options for storing hierarchical data in a relational database.

## materials:

[problem](https://stackoverflow.com/questions/4048151/what-are-the-options-for-storing-hierarchical-data-in-a-relational-database)

[nested set](https://www.sqlservercentral.com/articles/hierarchies-on-steroids-1-convert-an-adjacency-list-to-nested-sets)

[postgre ltree](https://www.postgresql.org/docs/current/ltree.html)

## examples in this project:

[sql ddl, dml](https://github.com/AlekseiLitkovetc/frm-comparison/blob/92ed91b9afb3a1d7709d482c18469add55eafaff/hierarchy/src/main/resources)

[skunk vs doobie for adjacency list](https://github.com/AlekseiLitkovetc/frm-comparison/blob/92ed91b9afb3a1d7709d482c18469add55eafaff/hierarchy/src/main/scala/adjacencylist)

[skunk ltree codec](https://github.com/AlekseiLitkovetc/frm-comparison/blob/92ed91b9afb3a1d7709d482c18469add55eafaff/hierarchy/src/main/scala/ltree/LtreeGoodExample.scala)
