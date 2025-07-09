## Problem Statement:
Implement a rate limiter that allows N requests every T seconds per client. Provide the following:

boolean allowRequest(String clientId) – Returns true if the request is allowed, false otherwise.

## Requirements:

1. Handle multiple clients concurrently.
2. Each client should have an independent rate limit window. 
3. Use appropriate concurrency primitives for thread-safe access. 
4. Write test cases for:
   1. Normal request patterns 
   2. Rate limit exceeded 
   3. Concurrent access from multiple clients