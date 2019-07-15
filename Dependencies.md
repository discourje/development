<b>Getting started:</b>
-

Discourje has a number of dependencies on other libraries:
- org.clojure/core.async: Core.async dependency to built on channel, put and take abstractions. 
- org.clojure/test.check: We added test.check to simplify concurrent tests. 
- slingshot: Enhanced throw-catch functionality.
- clj-uuid: The benefit with this library is that clj-uuid provides an easy way to get v1 and true namespaced v3 and v5 UUIDs.