# Summary of Query Types
The simple query protocol (i.e., Session#execute) is slightly more efficient in terms of message exchange, so use it if:

* Your query has no parameters; and
* you are querying for a small number of rows; and
* you will be using the query only once per session.

The extend query protocol (i.e., Session#prepare) is more powerful and more general, but requires additional network exchanges. Use it if:
* Your query has parameters; and/or
* you are querying for a large or unknown number of rows; and/or
* you intend to stream the results; and/or
* you will be using the query more than once per session.
