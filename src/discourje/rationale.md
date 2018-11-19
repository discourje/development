For the Two Buyer Protocol we are going to setup the participants as Threads. We have three participants;
Buyer1, Buyer2 and Seller. For each participant we need an input and output channel so we will end up with
6 channels in total.

We will create abstractions around take/put from core async to communicate between the participants. 
Our protocol will handle all logic to call each channel when they are supposed to. If a channel is being
used when the protocol does not prescribe, a monitor will throw an exception or log a message.