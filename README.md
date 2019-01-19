<b>Discourje</b>
-
- [Getting Started](GettingStarted.md)
- [Library Name](LibraryName.md)
- [Project information](ProjectInformation.md)
- [ToDo](ToDo.md)
- [Dependencies](Dependencies.md)

<b>Introduction:</b>
-
Discourje is a library to describe communication between systems as protocols.
A protocol acts as an agreement on how participants interact with each other.
All communication between participants is monitored by the protocol to ensure the correct flow of communication.
When participants deviate from the specified protocol the communication will be logged, but will never block unless configured to throw exceptions.

Discourje is written in Clojure (v1.8.0) and is built as an abstraction layer on clojure.core.async.
Discourje extends Core.async channels, put and take functions with validation logic to verify if the correct communication flow is followed. 
Communication is blocking when desired (configure logging levels) and order among messages, and on channels is preserved.

<b>Current supported functionality:</b>
- 
- [Sequencing](src/discourje/examples/sequencing.clj)
- [Parallelisation](src/discourje/examples/parallelisation.clj)
- [Branching](src/discourje/examples/branching.clj)
- [Recursion](src/discourje/examples/recursion.clj)

<i>See examples for each function for more info.</i>

As a proof of concept we choose to implement a protocol called the [Two buyer protocol](https://www.doc.ic.ac.uk/~yoshida/multiparty/multiparty.pdf) and extended it with recursion.
This simple protocol embeds all fundamental functionality a protocol language should support: sequencing, parallelisation, branching, recursion.

<i>See [TwoBuyerProtocol](src/discourje/TwoBuyerProtocol) for implementation.</i>

<b>Usage</b>
-

<i>See [api](src/discourje/api/api.clj) for more info.</i>

A protocol can be specified by the following constructs:
-
- <b>monitor-send [action sender receiver]</b>: Specifies a `send monitor` which validates that the current communication through the protocol is a send action with name `action` from `sender` to `receiver`.
- <b>monitor-receive [action receiver sender]</b>: Specifies a `receive monitor` which validates that the current communication through the protocol is a receive action with name `action` to `receiver` from `sender`.
- <b>monitor-choice [trueBranch falseBranch]</b>: Specifies a `choice monitor` which validates the first monitor in both the `true and false branch` and continues on target branch when an action is verified. 
- <b>monitor-recursion [name protocol]</b>: Specifies a `recursion monitor` which recurs or ends when the protocol encounters a `do-recur[name]` or `do-end-recur[name]`. Recur is matched by name and also supports nesting!
- <b>do-recur [name]</b>: specifies a recursion back to the `monitor-recursion` matching its name.
- <b>do-end-recur [name]</b>: specifies an end in recursion matching the name of the `monitor-recursion`.

Safe Send and Receive abstractions:
-
- <b>send! [action value sender receiver]</b>: Calls send <i>function</i> to send `action` with `value` from `sender` to `receiver`.
- <b>s! [action value sender receiver]</b>: Calls Send <i>macro</i> to send `action` with `value` from `sender` to `receiver`.
- <b>recv! [action sender receiver callback]</b>: Calls receive <i>function</i> to receive `action` from `sender` on `receiver` invoking `callback`.
- <b>r! [action sender receiver callback]</b>: Calls receive <i>macro</i> to receive `action` from `sender` on `receiver` invoking `callback`.

For chaining send and receive functions the developer can use specialized macros that handle callbacks;
- <b>>s! [action function sender receiver]</b> Creates an anonymous function with 1 parameter and feeds it as input to `function`. Then sends `action` with value, result of the `function`, from `sender` to `receiver`.
- <b>s!> [action value sender receiver]</b> First calls Send <i>macro</i> to send `action` with `value` from `sender` to `receiver`. Second, also invokes `function-after-send`.
- <b>>s!> [action function sender receiver f]</b> First, creates an anonymous function with 1 parameter and feeds it as input to `function`. Then sends `action` with value, result of the `function`, from `sender` to `receiver`. Second, also invokes `function-after-send`.

For an example on chaining macros see:[Chaining](src/discourje/examples/macroChaining.clj).
<i>*Reminder: Macros are not first class. This means when you want to treat send and receive as first class objects, you should use the functions instead of macros.</i>

Logging
-
Discourje also allows two levels of logging when communication does not comply with the protocol:
- <b>Logging (not-blocking)</b>: Enable logging to print to the console when communication is invalid, this will not block communication.
- <b>Exceptions (blocking)</b>: Enable exception logging to log to the console and throw exceptions when communication is invalid, this will block communication.

<b>Default configuration: Exceptions!</b>

See [Logging](src/discourje/examples/logging.clj) for an example.
<i>*Logging levels are set as global configurations!</i>

Example: Hello World
-
```clojure
(defn- defineHelloWorldProtocol
  "This function will generate a vector with 2 monitors to send and receive the hello world message."
  []
  [(monitor-send "helloWorld" "user" "world")
    (monitor-receive "helloWorld" "world" "user")])
```
Then generate a protocol object with the specified roles and monitors:
```clojure
;define the protocol
(def protocol (generateProtocolFromMonitors (defineHelloWorldProtocol)))
```

The next step is to generate `participants` specified by name(unique) for the protocol. A participant object implements send and receive functions in order to communicate through Discourje.
```clojure
;define the participants
(def user (generateParticipant "user" protocol))
(def world (generateParticipant "world" protocol))
```
In the following example we implemented two functions which require a participant as (input) parameter.
In these functions we use the s! (send macro) and r! (receive macro) for communication.
```clojure
(defn- sendToWorld
  "This function will use the protocol to send the Hello World! message to world."
  [participant]
  (log "Will now send Hello World! to world.")
  (s! "helloWorld" "Hello World!" participant "world"))

(defn- receiveFromUser
  "This function will use the protocol to listen for the helloWorld message."
  [participant]
  (r! "helloWorld" "user" participant
              (fn [message]
                  (log (format "Received message: %s" message)))))
```

The developer is then able to communicate safely among participants.
```clojure
;start the `sendToWorld' function on thread and add `user' participant
(clojure.core.async/thread (sendToWorld user))
;start the `receiveFromUser' function on thread and add `world' participant
(clojure.core.async/thread (receiveFromUser world))
```

<i>See [hello world](src/discourje/examples/helloWorld.clj) for this example.</i>