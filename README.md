[circleci-badge]: https://circleci.com/gh/hodur-org/hodur-datomic-schema.svg?style=shield&circle-token=62ae347c27855ff99f24e47c2cea3719cc4ca13d
[circleci]: https://circleci.com/gh/hodur-org/hodur-datomic-schema
[clojars-badge]: https://img.shields.io/clojars/v/hodur/datomic-schema.svg
[clojars]: http://clojars.org/hodur/datomic-schema
[datomic]: https://docs.datomic.com/
[github-issues]: https://github.com/hodur-org/hodur-datomic-schema/issues
[hodur-engine]: https://github.com/hodur-org/hodur-engine
[hodur-engine-clojars-badge]: https://img.shields.io/clojars/v/hodur/engine.svg
[hodur-engine-clojars]: http://clojars.org/hodur/engine
[hodur-engine-definition]: https://github.com/hodur-org/hodur-engine#model-definition
[hodur-engine-started]: https://github.com/hodur-org/hodur-engine#getting-started
[license-badge]: https://img.shields.io/badge/license-MIT-blue.svg
[license]: ./LICENSE
[logo]: ./docs/logo-tag-line.png
[motivation]: https://github.com/hodur-org/hodur-engine/blob/master/docs/MOTIVATION.org
[plugins]: https://github.com/hodur-org/hodur-engine#hodur-plugins
[status-badge]: https://img.shields.io/badge/project%20status-beta-brightgreen.svg

# Hodur Datomic Schema

[![CircleCI][circleci-badge]][circleci]
[![Clojars][hodur-engine-clojars-badge]][hodur-engine-clojars]
[![Clojars][clojars-badge]][clojars]
[![License][license-badge]][license]
![Status][status-badge]

![Logo][logo]

Hodur is a descriptive domain modeling approach and related collection
of libraries for Clojure.

By using Hodur you can define your domain model as data, parse and
validate it, and then either consume your model via an API making your
apps respond to the defined model or use one of the many plugins to
help you achieve mechanical, repetitive results faster and in a purely
functional manner.

> This repo is the Datomic schema plugin that leverages Hodur models
> as Datomic schemas.  For a list of what you can do once your model
> is in Hodur [check here][plugins].

## Motivation

For a deeper insight into the motivations behind Hodur, check the
[motivation doc][motivation].

## Getting Started

Hodur has a highly modular architecture. [Hodur Engine][hodur-engine]
is always required as it provides the meta-database functions and APIs
consumed by plugins.

Therefore, refer the [Hodur Engine's Getting
Started][hodur-engine-started] first and then return here for
Datomic-specific setup.

After having set up `hodur-engine` as described above, we also need to
add `hodur/datomic-schema`, a plugin that creates Datomic Schemas out
of your model to the `deps.edn` file:

``` clojure
  {:deps {hodur/engine         {:mvn/version "0.1.6"}
          hodur/datomic-schema {:mvn/version "0.1.0"}}}
```

You should `require` it any way you see fit:

``` clojure
  (require '[hodur-datomic-schema.core :as hodur-datomic])
```

Let's expand our `Person` model above by "tagging" the `Person` entity
for Datomic. You can read more about the concept of tagging for
plugins in the sessions below but, in short, this is the way we, model
designers, use to specify which entities we want to be exposed to
which plugins.

``` clojure
  (def meta-db (hodur/init-schema
                '[^{:datomic/tag-recursive true}
                  Person
                  [^String first-name
                   ^String last-name]]))
```

The `hodur-datomic-schema` plugin exposes a function called `schema`
that generates your model as a Datomic schema payload:

``` clojure
  (def datomic-schema (hodur-datomic/schema meta-db))
```

When you inspect `datomic-schema`, this is what you have:

``` clojure
  [{:db/ident       :person/first-name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :person/last-name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}]
```

Assuming the Datomic client API is bound to `datomic`, and your
connection to the Database cluster is bound to `db-conn`, you can
simply transact your schema like this:

``` clojure
  (datomic/transact db-conn {:tx-data datomic-schema})
```

## Model Definition

All Hodur plugins follow the [Model
Definition][hodur-engine-definition] as described on Hodur [Engine's
documentation][hodur-engine].

## Attribute Naming Convention

Datomic's foundational modeling unit is the atom. An atom is the
relationship between an Entity, an Attribute, its Value, and the Time
at which the combination was valid.

With such an approach, Datomic attributes become central to the
modeling effort and Hodur takes advantage of that.

A Datomic attribute such as `:person/name` will be defined in Hodur as
the `name` attribute of the `Person` entity:

``` clojure
  [Person
   [^String name]]
```

In the example above we are describing the Datomic attribute
`:person/name` a little more than just by giving its name. By tagging
it as a `String`, the schema generator understands the above as:

``` clojure
  [{:db/ident :person/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}]
```

All naming conversions by this plugin are for a kebab-cased approach.

## Attribute Properties

Datomic attributes can have a series of properties. In order to
utilize them simple mark the Hodur attribute as described in the table
below:

| Hodur marker           | Datomic equivalent | Notes                                       |
|------------------------|--------------------|---------------------------------------------|
| `:datomic/unique`      | `:db/unique`       | `:db.unique/identity` or `:db.unique/value` |
| `:datomic/index`       | `:db/index`        | `true` or `false` (default)                 |
| `:datomic/fulltext`    | `:db/fulltext`     | `true` or `false` (default)                 |
| `:datomic/isComponent` | `:db/isComponent`  | `true` or `false` (default)                 |
| `:datomic/noHistory`   | `:db/noHistory`    | `true` or `false` (default)                 |

You can find more details about each of these on the [Datomic
documentation][datomic]. Do be aware that the modeling options are
slightly different between Datomic Cloud and Datomic On-Prem.

## Scalar Types and Finer Grained Control

The table below shows how Hodur's primitive scalar types are mapped by
default to Datomic scalar types:

| Hodur Scalar | Datomic Scalar     |
|--------------|--------------------|
| `String`     | `:db.type/string`  |
| `Float`      | `:db.type/float`   |
| `Integer`    | `:db.type/long`    |
| `Boolean`    | `:db.type/boolean` |
| `DateTime`   | `:db.type/instant` |
| `ID`         | `:db.type/uuid`    |

If you need to have access to specific types you can override Hodur's
automatic behavior by using the marker `:datomic/type`:

``` clojure
  [ExampleEntity
   [^{:datomic/type :db.type/keyword}
    keyword-type
    ^{:datomic/type :db.type/uri}
    uri-type
    ^{:datomic/type :db.type/double}
    double-type
    ^{:datomic/type :db.type/bigdec}
    bigdec-type]]
```

Each of the attributes above are now using Datomic-specific
scalars. The schema below is a result of the definition above:

``` clojure
  [{:db/ident       :example-entity/bigdec-type
    :db/valueType   :db.type/bigdec
    :db/cardinality :db.cardinality/one}
   {:db/ident       :example-entity/double-type
    :db/valueType   :db.type/double,
    :db/cardinality :db.cardinality/one}
   {:db/ident       :example-entity/keyword-type
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}
   {:db/ident       :example-entity/uri-type
    :db/valueType   :db.type/uri
    :db/cardinality :db.cardinality/one}]
```

## Cardinality

The `:cardinality` marker is respected by Hodur Datomic Schema in
relation to `one` vs. `many`. Datomic does not have the finer control
that Hodur has so the plugin simply converts cardinalities to either
`:db.cardinality/one` or `:db.cardinality/many`.

In the example below the attribute `supervisor` has a cardinality of 1
to an `Employee` while the attribute `co-workers` has a complex
cardinality of `0` to `n` to potentially many `Employee`:

``` clojure
  [Employee
   [^String name
    ^Employee supervisor
    ^{:type Employee
      :cardinality [0 n]}
    co-workers]]
```

Hodur will infer that:

- `:employee/supervisor` is a `:db.type/ref` of `:db.cardinality/one`
- `:employee/co-workers` is a `:db.type/ref` of `:db.cardinality/many`

The above definition leads to:

``` clojure
  [{:db/ident       :employee/co-workers,
    :db/valueType   :db.type/ref,
    :db/cardinality :db.cardinality/many}
   {:db/ident       :employee/name,
    :db/valueType   :db.type/string,
    :db/cardinality :db.cardinality/one}
   {:db/ident       :employee/supervisor,
    :db/valueType   :db.type/ref,
    :db/cardinality :db.cardinality/one}]
```

## Special Treatments

### Interfaces and Unions

Interfaces proper do not exist in Datomic so they are ignored in
Hodur's Datomic plugin.

Unions are also do not exist in Datomic per se. Hodur's Datomic plugin
also ignores them. However, they can be emulated by the resulting
Datomic schema. Datomic's `:db.type/ref` will be used if you specify a
union. Therefore, it is possible to relate that attribute to any kind
of Datomic entity. Of course, this wouldn't respect the boundaries of
the union you specified but, if you really need unions, that's a way
to go about it.

### Enums

The traditional way to use enums in Datomic is to create one
`:db/ident` for each option.

This is the route that Hodur takes then. Example:

``` clojure
  [Person
   [^String name
    ^Gender gender]

   ^:enum
   Gender
   [MALE FEMALE PREFER_NOT_TO_REPORT]]
```

This is also a good example of how naming conventions are used:

``` clojure
  [{:db/ident :gender/female}
   {:db/ident :gender/male}
   {:db/ident :gender/prefer-not-to-report}
   {:db/ident :person/gender
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :person/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}]
```

### Parameters

Field parameters do not exist on Datomic so they are ignored.

## Documentation and Deprecation

Because of Datomic's central modeling around attributes, only field
documentation and deprecation is parsed. Enum entries can also be
documented/deprecated as they are considered fields in Hodur.

All fields marked with `:doc` will immediately get a `:db/doc` entry
in their Datomic schema definition.

If a field is marked as deprecated with `:deprecation` a deprecation
note is added to Datomic's `:db/doc` entry.

## Bugs

If you find a bug, submit a [GitHub issue][github-issues].

## Help!

This project is looking for team members who can help this project
succeed! If you are interested in becoming a team member please open
an issue.

## License

Copyright © 2019 Tiago Luchini

Distributed under the MIT License (see [LICENSE][license]).
