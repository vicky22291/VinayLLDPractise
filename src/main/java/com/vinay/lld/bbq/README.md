## Question

Prompt: Implement a thread-safe bounded buffer (blocking queue) that multiple producers can add to and multiple consumers can remove from. 
The buffer should have a fixed maximum capacity. 
Provide two operations, for example: enqueue(item) to add an item, and dequeue() to remove an item. 
If the buffer is full, enqueue should block until space becomes available. 
If the buffer is empty, dequeue should block until an item is available. 

## Functional Requirements

1. The buffer should be bounded.
2. Support multiple producers and consumers.
3. Should support enqueue and dequeu

## Non-Functional Requirements

1. Should be fair
2. Should be interruptible

