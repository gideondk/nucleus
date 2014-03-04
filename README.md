<p align="center">
  <img width="280px" src="https://dl.dropboxusercontent.com/u/57984069/2014-03-04%2013_27_08.gif" alt="Nucleus"/><br /><br />
</p>
  
  
  
  
Nucleus is a small reactive RPC-like stack build on Akka IO through [Sentinel](http://github.com/gideondk/sentinel). It's positioned as a light alternative (both in code as performance) to the more featured stacks like [Finagle](https://github.com/twitter/finagle).

It supports basic request and response based commands, but is also able to stream requests and responses between two end-points. The current structure of Nucleus allows client to server communication but will be extended with bi-directional requests in the very near future, making it able to inverse control between server and client after connection.

Its protocol makes it easy to implement in other programming languages and is loosely modelled to the [BERT-RPC](http://bert-rpc.org/) spec. It uses Erlang's [External Term Format](http://erlang.org/doc/apps/erts/erl_ext_dist.html) to serialise and deserialize content to be send over the line.

The inner workings are build to be as type safe as possible, while the external API makes it easy to be used in other (dynamic) languages. **Nucleus** as a project focusses on the creation of higher level reactive services with a small footprint.

**Nucleus is the successor of the earlier developed "Bark"** 

## Status
Nucleus is currently being used in services with low overhead in terms of message sizes. The Play Iteratee powered "stream" and "process" functions should be able to handle larger chunked payloads, though there currently is any broad information available on the performance of these routes with larger chunk sizes (but should, by theory, be sufficient)

In the future the (de)serialisation stack will be extended with more complex types, just as longer running (async) messages will be handled in a less (socket) blocking way (using callbacks or better…)


**Currently available in Nucleus:**

* Easy to use DSL to implement services using multiple modules and functions;
* Full actor based stack through Akka IO and Sentinel;
* Type class based (de)serialisation to Erlang Term Format;
* Separate functionality to create `call` and `cast` messages (request -> response or fire and forget) 
* Functionality to stream (through Play `Enumerators`) from between two end-points in bi-directional ways); 
* Build in supervision (and restart / reconnection functionality) on both server and client for a defined number of workers;

**The following is currently missing in Nucleus, but will be added soon:**

* Server to client communication (architecture already available through Sentinel, but no direct implementation available);
* Better handling of async messages (the current implementation doesn't block processing, but does block socket communication);
* More solid test cases.

## Installation
You can install Nucleus through source (by publishing it into your local Ivy repository):

```bash
./sbt publish-local
```

Or by adding the repo:
<notextile><pre><code>"gideondk-repo" at "https://raw.github.com/gideondk/gideondk-mvn-repo/master"</code></pre></notextile>

to your SBT configuration and adding the `SNAPSHOT` to your library dependencies:

<notextile><pre><code>libraryDependencies ++= Seq(
	"nl.gideondk" %% "nucleus" % "0.1.0"
)
</code></pre></notextile>


## Processor
The architecture of Nucleus is based on a message processor, shared between both client and server. This message processor decides on the appropriate action for every request or response received through a TCP connection.

While responses or stream chunks are consumed as a expected result of a request, requests are routed into a module structure. Within this module structure, functions can be defined to handle the `call`, `cast`, `stream` or `process` requests.

Each of the functions accept a number of arguments and return the appropriate result for that type of function. Function arguments are automatically parsed from `ByteStrings` sent over the wire and results are subsequently converted to the appropriate binary representation. 

(De)serialisation is done through the ETF protocol. For every type `T` in the arguments received or in the send back to the requester, a `ETFConverter` type class should be defined for non-default types.

### Functions
##### Call function
Call functions are used to model *normal* request and response based commands. A `call` function accepts zero or multiple arguments are returns a `Future[T]` as a result. 

`A => Future[B]`

##### Cast function
Call functions are used to model fire-and-forget request . A `cast` function accepts one or multiple arguments are returns a `Future[Unit]` as a result.

`A => Future[Unit]`

##### Stream function
Stream functions are used to model streaming responses. A `stream` function accepts zero or multiple arguments are returns a `Future[Enumerator[B]]` containing the response chunks as a result.

`A => Future[Enumerator[B]]`

##### Process function
Process functions are used to model streaming requests. A `process` function accepts zero or multiple arguments are returns a `Future[A]` as the result of the processed stream.

`A => Enumerator[B] => Future[C]`

### Modules
Modules are created by implementing the `Routing` trait:

```scala
object CacheServer extends Routing {
	val modules = module("cache") {
		cast("set")((key: String, value: String) ⇒ actor ! SetCache(key, value)) ~
		call("get")((key: String) ⇒ (actor ? GetCache(key)).mapTo[CacheResult].map(_.v.getOrElse("")))
  }
}
```

Within a module, the `~` function is used on a function to combine multiple functions within a module. The same function can be used to implement multiple modules within the service.

## Server
After the creation of a module structure, a server can be initialised by passing the module to a Server in combination with the name of the service:

```scala
val server = Server("Cache Service", CacheServer.modules)(serverSystem)
```

After initialisation, the server can be be started by using the `run(port: Int)` command.

### Client
A client can be initialised and connected, based on the server's hostname, port and the number of workers which should be available within the client: 

```scala
val client = Client("localhost", 8888, 4, "Cache client")(clientSystem)
```

The `call`, `cast`, `stream` and `process`, or `?`, `!`, `?-->` and `?<--` functions can be used to call a function with the specified arguments: 

```scala
(client |?| "cache" |/| "get") ? "A"
```

The `|?|` function is used to enter a specific module on the server, while the `|/|` function is used to specify the to-be-called function on the server.

#### Tasks
Calling a function returns a `Task[T]`. Task has (co)monadic behaviour which wraps a `IO[Future[A]]`. 

Use `run` to expose the `Future[A]`, or use `start(d: Duration)` to perform IO and wait (blocking) on the future.

#### Deserialization
The Task returned by the Client contains a `ClientResult` or `ClientStreamResult` which contains the `ByteString` representation of the received frame / frames from the server combined with the ability `as[T]` to deserialize the `ByteString` to the expected type through the type classes (passing a `Option` of the type, depending on the success of the deserialization).

Importing `nl.gideondk.nucleus._` results in a loaded implicit which makes it possible to directly call `.as[T]` on a Task. This makes it possible to make the following (blocking) call which directly returns the expected String:

```scala
callTask.as[String].copoint
res0: String

streamChunks.as[Int].copoint
res1: Enumerator[Int]

```


The current DSL isn't optimal for plain usage (and is rather verbose and fuzzy), but is designed to be used within environment where the actual usage of the `Client` is abstracted on a higher level.

# License
Copyright © 2014 Gideon de Kok

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
