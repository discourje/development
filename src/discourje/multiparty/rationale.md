The multiparty package allows all participants to act independently.
Each paricipants will Send and await for in/output. They will all be responsible for their own communication.
However the protocol will verify and guard the correct flow of messages in the conversation.

So this packages differs from `coordinatieByProtocol` in the following ways:

1: The protocol will only guard or monitor the conversation and log/throw exceptions when incorrect flow is detected.

2: Participants are responsible for executing their own functions and communication to other participants.

3: The protocol will instantiate the required channels for all communication needed. Participants will use a reference of the protocol and desired receiver to transfer data/functions.

 
