<b>Discourje</b>
-
- [Getting Started](GettingStarted.md)
- [Library Name](LibraryName.md)
- [Project information](ProjectInformation.md)
- [ToDo](ToDo.md)

<b>Introduction:</b>
-
Discourje is a library to describe communication between systems as protocols.
A protocol acts as an agreement on how participants interact with each other.
All communication between participants is monitored by the protocol to ensure the correct flow of communication.
When participants deviate from the specified protocol, the communication will not be allowed to proceed.

Discourje is written in Clojure (v1.8.0) and is built as an abstraction layer on clojure.core.async.
Discourje extends Core.async channels, put and take functions with validation logic to verify if the correct communication flow is followed. 
Communication is never blocking and order among messages, and on channels is preserved.

<b>Current supported functionality:</b>
- 
- [Sequencing](src/discourje/examples/sequencing.clj)
- [Parallelisation](src/discourje/examples/parallelisation.clj)
- [Branching](src/discourje/examples/branching.clj)
- [Recursion](src/discourje/examples/recursion.clj)
- Queueing of messages on channels
- Queueing of receive actions on channels when no data is available yet (order preserved)

<i>See examples for each function for more info.</i>

As a proof of concept we choose to implement a protocol called the [Two buyer protocol](https://www.doc.ic.ac.uk/~yoshida/multiparty/multiparty.pdf) and extended it with recursion.
This simple protocol embeds all fundamental functionality a protocol language should support: sequencing, parallelisation, branching, recursion.

<i>See [TwoBuyerProtocol](src/discourje/TwoBuyerProtocol) for implementation.</i>

<b>Usage</b>
-
A protocol can be specified by the following constructs:
- <b>monitor-send [action sender receiver]</b>: Specifies a `send monitor` which validates that the current communication through the protocol is a send action with name `action` from `sender` to `receiver`.
- <b>monitor-receive [action receiver sender]</b>: Specifies a `receive monitor` which validates that the current communication through the protocol is a receive action with name `action` to `receiver` from `sender`.
- <b>monitor-choice [trueBranch falseBranch]</b>: Specifies a `choice monitor` which validates the first monitor in both the `true and false branch` and continues on target branch when an action is verified. 
- <b>monitor-recursion [name protocol]</b>: Specifies a `recursion monitor` which recurs or ends when the protocol encounters a `do-recur[name]` or `do-end-recur[name]`. Recur is matched by name and also supports nesting!
- <b>do-recur [name]</b>: specifies a recursion back to the `monitor-recursion` matching its name.
- <b>do-end-recur [name]</b>: specifies an end in recursion matching the name of the `monitor-recursion`.

Safe Send and Receive abstractions:
- <b>send! [action value sender receiver]</b>: Calls send <i>function</i> to send `action` with `value` from `sender` to `receiver`.
- <b>s! [action value sender receiver]</b>: Calls Send <i>macro</i> to send `action` with `value` from `sender` to `receiver`.
- <b>recv! [action sender receiver callback]</b>: Calls receive <i>function</i> to receive `action` from `sender` on `receiver` invoking `callback`.
- <b>r! [action sender receiver callback]</b>: Calls receive <i>macro</i> to receive `action` from `sender` on `receiver` invoking `callback`.

<i>*Reminder: Macros are not first class. This means when you want to treat send and receive as first class objects, you should use the functions instead of macros.</i>

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
(def protocol (generateProtocol (defineHelloWorldProtocol)))
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
  (println "Will now send Hello World! to world.")
  (s! "helloWorld" "Hello World!" participant "world"))

(defn- receiveFromUser
  "This function will use the protocol to listen for the helloWorld message."
  [participant]
  (r! "helloWorld" "user" participant
              (fn [message]
                  (println (format "Received message: %s" message)))))
```

The developer is then able to communicate safely among participants.
```clojure
;start the `sendToWorld' function on thread and add `user' participant
(clojure.core.async/thread (sendToWorld user))
;start the `receiveFromUser' function on thread and add `world' participant
(clojure.core.async/thread (receiveFromUser world))
```

<i>See [hello world](src/discourje/examples/helloWorld.clj) for a this example.</i>