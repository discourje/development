<b>Discourje</b>
-
- [Getting Started](GettingStarted.md)
- [Library Name](LibraryName.md)
- [Project information](ProjectInformation.md)

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

<i>See examples for each function for more info.</i>

As a proof of concept we choose to implement a protocol called the [Two buyer protocol](https://www.doc.ic.ac.uk/~yoshida/multiparty/multiparty.pdf) and extended it with recursion.
This simple protocol embeds all fundamental functionality a protocol language should support: sequencing, parallelisation, branching, recursion.

<i>See [TwoBuyerProtocol](src/discourje/TwoBuyerProtocol) for implementation.</i>

<b>Usage</b>
-
A protocol can be specified by the following constructs:
- sendM [action from to]: Specifies a `send monitor` which validates that the current communication through the protocol is a send action with name `action` from `from` to `to`.
- receiveM [action to from]: Specifies a `receive monitor` which validates that the current communication through the protocol is a receive action with name `action` to `from` from `to`.
- choice [trueBranch falseBranch]: Specifies a `choice monitor` which validates the first monitor is both the `true and false branch` and continues on target branch when an action is verified. 
- recursion [name protocol]: Specifies a `recursion monitor` which recurs or ends when the protocol encounters a `recur![:status recur]` or `recur![:status end]` Recur is matched by name and also supports nesting!.

```clojure
(defn- defineHelloWorldProtocol
  "This function will generate a vector with 2 monitors to send and receive the hello world message."
  []
  (vector
    (->sendM "helloWorld" "user" "world")
    (->receiveM "helloWorld" "world" "user")))
```
Then generate a protocol object with the specified roles and monitors:
```clojure
(defn generateHelloWorldProtocol
  "Generate the protocol, channels and set the first monitor active."
  []
  (generateProtocol ["user" "world"] (defineHelloWorldProtocol)))
  
  
;define the protocol
(def protocol (atom (generateHelloWorldProtocol)))
```

When the developer is satisfied with the defined protocol he/she can start on writing the code that follows the protocol.
The developer should implement a function with a `participant` as parameter.
The participant record implements the `role` protocol (native Clojure interface-like construct, not to be confused with any Discourje constructs!) which implements `send-to` and `receive-by` functions.
```clojure
;define the participants
(def user (discourje.core.core/->participant "user" protocol))
(def world (discourje.core.core/->participant "world" protocol))

(defn- sendToWorld
  "This function will use the protocol to send the Hello World! message to world."
  [participant]
  (println "Will now send Hello World! to world.")
  (send-to participant "helloWorld" "Hello World!" "world"))

(defn- receiveFromUser
  "This function will use the protocol to listen for the helloWorld message."
  [participant]
  (receive-by participant "helloWorld" "user"
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