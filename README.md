*(The content below should be updated.)*

# Discourje

> [**discourse**](https://www.lexico.com/definition/discourse) /ˈdɪskɔːs/<br>
> **1** Written or spoken communication or debate.<br>
> &nbsp;&nbsp;**1.1** A formal discussion of a topic in speech or writing.<br>
> &nbsp;&nbsp;**1.2** A connected series of utterances; a text or conversation.

## Introduction

Discourje is a library to describe communication between systems as protocols.
A protocol acts as an agreement on how participants interact with each other.
All communication between participants is monitored by the protocol to ensure the correct flow of communication.
When participants deviate from the specified protocol the communication will be logged, but will never block unless configured to throw exceptions.

Discourje is written in Clojure (v1.10.1) and is built as an abstraction layer on clojure.core.async.
Discourje extends Core.async channels, put and take functions with validation logic to verify if the correct communication flow is followed. 
Communication is blocking when desired (configure logging levels) and order among messages, and on channels is preserved.

<b>Current supported functionality:</b>
- 
- [Sequencing](src/discourje/examples/sequencing.clj)
- [Parallelism](src/discourje/examples/parallelization.clj)
- [Multicast](src/discourje/examples/multicast.clj)
- [Branching](src/discourje/examples/branching.clj)
- [Recursion](src/discourje/examples/recursion.clj)
- [Parameterized Recursion](src/discourje/examples/parameterizedRecursion.clj)
- [Custom Channels](src/discourje/examples/customChannels.clj)
- [Typed Messages](src/discourje/examples/typedMessages.clj)
- [Logging Levels](src/discourje/examples/logging.clj)
- [Various validation mechanisms](src/discourje/examples/predicates.clj)
- Validation on closing channels, all examples implement this!
- Nesting: Parallelism, Recursion and Branching constructs support nesting!
- Clojure.core.async `go-block` support!
- Clojure.core.async `put!` & `take!` support!

<i>See examples for each function for more info.</i>

As a proof of concept we choose to implement a protocol called the [Two buyer protocol](https://www.doc.ic.ac.uk/~yoshida/multiparty/multiparty.pdf) and extended it with recursion.
This simple protocol embeds all fundamental functionality a protocol language should support: sequencing, parallelisation, branching, recursion.

<i>See [TwoBuyerProtocol](src/discourje/TwoBuyerProtocol) for implementation.</i> 

## Usage

Using Discourje takes three simple steps:
- Define message exchange pattern
- Generate infrastructure (channels)
- Use Discourje put & take abstractions 

Step 1: A message exchange pattern can be specified by the following constructs
-
- <b>-->> [predicate/type/wildcard sender receiver]</b>: Specifies an `atomic-interaction` which validates that the current communication through the protocol is a send & receive with validation of pred/value/wildcard from `sender` to `receiver`.
- <b>choice [branch & more]</b>: Specifies a `choice form` which validates the first monitor in all branches and continues on target branch when an action is verified. Notice it supports variadic input arguments.
- <b>rec [name interaction & more]</b>: Specifies a `recursion form` which recurs when the protocol encounters a `continue [name]`. Recur is matched by name and also supports nesting! Note that the Rec name can be parameterized key-value-pairs.
- <b>continue [name]</b>: Specifies a recursion back to the matching `rec`, if it contains mapping they will be applied next iteration. See [Parameterized Recursion](src/discourje/examples/parameterizedRecursion.clj) for more info. 
- <b>par [parallel & more]</b>: Specifies a `parallel form` supports traversing multiple branches in parallel. Notice it supports variadic input arguments.
- <b>close [sender receiver infrastructure]|[channel]</b>: Specifies a `close form` that validates if a channel is allowed to be closed.

Step 2: Generate the infrastructure (channels)
-
- <b>add-infrastructure [message-exchange-pattern | message-exchange-pattern custom-channels]</b>: Generates the required channels and adds the MEP as validation, or adds the MEP to the given channels. Notice arity!

There are two options for generating the infrastructure. Either by allowing Discourje to generate all channels. This will generate the required channels specified in the MEP with a buffer size of 1.
Or by supplying the add-infrastructure macro with custom created channels. (see customChannels example)

Step 3: Use Discourje put & take abstractions
-
- <b>>!! [channel(s) message]</b>: Put function on channel(s), channels can be a vector of channels to support multicast (see examples!)
- <b><!! [channel]</b>:  Take operation of channel.
- <b><!!! [channel]</b>:  Take operation of channel when used with multicast it blocks until all receivers in the multicast have received their values.
- <b>>! [channel(s) message]</b>: Go-block Put macro on channel(s), channels can be a vector of channels to support multicast.
- <b><! [channel]</b>:  Go-block Take macro.
- <b>put! [channel message callback on-caller?]</b>: Discourje version of Clojure.core.async.put! function with callbacks. Supports all arities that core.async offers.
- <b>take! [channel callback on-caller?]</b>:  Discourje version of Clojure.core.async.take! function with callbacks. Supports all arities that core.async offers.

Logging
-
Discourje also allows four levels of logging when communication does not comply with the protocol:
- <b>none (not-blocking)</b>: No logs or exceptions
- <b>Logging (not-blocking)</b>: Enable logging to print to the console when communication is invalid, this will not block communication.
- <b>Exceptions (blocking)</b>: Enable exception logging to throw exceptions when communication is invalid, this will block communication.
- <b>Logging and Exceptions (blocking)</b>: Enable exception logging to log to the console and throw exceptions when communication is invalid, this will block communication.

<b>Default configuration: Exceptions!</b>

Logging configuration functions:
- <b>set-logging-none</b>: Disables logging of messages and exceptions.
- <b>set-logging</b>: Enables logging of messages, yet no exceptions.
- <b>set-logging-exceptions</b>: Enables throwing of exceptions (BLOCKING).
- <b>set-logging-and-exceptions</b>: Enables logging of messages and throwing exceptions (BLOCKING).
- <b>stop-logging</b>: Close the logging channel. <i>*We use a channel for showing logs to preserve order among logged messages and exceptions!</i>

Log functions:
- <b>log-message [message & more]</b>: Logs a `message` to the logging channel, takes a variable amount of input parameters which get concatenated!
- <b>log-error [type message & more]</b>: Throws an exception of type `type` with `message` to the logging channel, takes a variable amount of input parameters which get concatenated!

See [Logging](src/discourje/examples/logging.clj) for an example.

<i>*Logging levels are set as global configurations!</i>

## Example: Hello World

```clojure
;"This function will generate a mep with 1 interaction to send and receive the hello world string message and a close."
(def message-exchange-pattern
  (mep (-->> String "user" "world")
       (close "user" "world")))
```
Then generate the infrastructure:
```clojure
;setup infrastructure, generate channels and add monitor
(def infrastructure (add-infrastructure message-exchange-pattern))
```

The next step is to get the required channel for communication
```clojure
(def user-to-world (get-channel "user" "world" infrastructure))
```
In the following example we implemented two functions which represent user and world.
```clojure
(defn- send-to-world "This function will use the protocol to send the Hello World! message to world."
  [] (>!! user-to-world "Hello World!"))

(defn- receive-from-user "This function will use the protocol to listen for the helloWorld message."
  [] (let [message (<!! user-to-world)]
       (log-message "World received message: " message)
       (close! user-to-world)))
```

The developer is then able to communicate safely among participants.
```clojure
;start the `sendToWorld' function on thread
(clojure.core.async/thread (send-to-world))
;start the `receiveFromUser' function on thread
(clojure.core.async/thread (receive-from-user))
```

<i>See [hello world](src/discourje/examples/helloWorld.clj) for this example.</i>
