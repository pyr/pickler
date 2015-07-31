pickler: just enough pickle for graphite
========================================

[![Build Status](https://secure.travis-ci.org/pyr/pickler.png)](http://travis-ci.org/pyr/pickler)


## Installation

To install pickler, add the following dependency to your `project.clj`
file:

```
[spootnik/pickler "0.1.6"]
```

## Usage

A simple namespace which exposes twos functions `raw->ast` and
`ast->metrics`

* `raw->ast`: generates an AST for pickle data from a bytebuffer, the
  AST is just a list of maps containing at least a `:type` key storing
  the pickle opcode.
* `ast->metrics`: generates a list of graphite metrics from an AST,
  the list is a list of 3-tuples containing, the metric timestamp and
  name of the datapoint


Wonder who would be stupid enough to write this ? Check-out [cyanite](https://github.com/pyr/cyanite)

# License

Copyright © 2014, 2015 Pierre-Yves Ritschard

Distributed under the MIT License
