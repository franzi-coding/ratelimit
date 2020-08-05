# Challenge Rate Limiter

coding challenge

## Build and run


```bash
1. go into project folder
2. run "./gradlew bootJar" or "gradle bootJar"
3. run "java -jar build/libs/challenge-0.0.1-SNAPSHOT.jar"
4. run "curl localhost:8080 -v", you should get a response "Hello, Successfully retrieved request"
5. change the "api.limit.requests" value from 100 to 1 in the application.properties file and repeate step 2 and 3.
When you send two requests, you should see that the 2nd request was not permitted with "Try again in ... seconds"

```


## Run tests
go into project folder

```bash
./gradlew test
```

## How does the rate limiting work
The rate limiting mechanism is enforced by a request filter which maintains a rate limiting strategy. 
The currently used strategy checks the number of requests in a sliding window. A sliding window means, that the begin 
of the rate limit time window starts with the first tracked request and not with every full hour.

When a user requests the api, the strategy looks up the users IP address in a map.  
The map contains a queue for each ip address which itself contains all the request times for the ip address.
When the size of the queue exceeds the limit then the request is not permitted. 
If the size is not exceeded, then the current request time is added to the queue and the request is permitted.
Before the size of the queue is compared with the rate request limit, the strategy performs a cleanup on the current 
selected queue to delete all existing request times which are older than the current time minus the given rate limit time.

```
Example of the strategy: 
given rate limit:                       100 requests per 60 minutes

current user request:                    IP: 127.0.0.1
                                         01-01-2020 13:15

existing map (<String,Queue>):           192.168.0.1-> ["01-01-2020 6:15"]
                                         127.0.0.1-> ["01-01-2020 12:05", "01-01-2020 12:18"]

1. step: IP look up in map returns Queue ["01-01-2020 12:05", "01-01-2020 12:18"] 
2. step: cleanup of the queue, result:   ["01-01-2020 12:18"]
3. step: size of queue < rate limit?     true  
4. step: add request and permit request  ["01-01-2020 12:18", "01-01-2020 13:15"]   

```

Data Structure: The strategy uses a concurrent hash map which ensures that multiple reads can be performed at the same time without blocking.
The map allows a high efficient lookup in O(1). 
The value of the map is a queue with a fixed size on initialization and once the queue is full no more elements can be inserted (ArrayBlockingQueue).
This works efficiently because each user has it's own queue. The time consumption is 
O(1) access for peek which is required to look up the oldest entry in the queue.

Scalability: The strategy consists of an own scheduled cleanup task, which is currently running every day and 
which reduces the memory consumption on high load.

Maintainability: Rate limit values and the timing of the cleanup task can be easily adapted by changing the values in a property file.


## Tradeoffs
- ipMap is stored in memory, which supports a very quick lookup, because no database connection is needed. 
 Because the data is stored in memory, the data is lost with each deployment. 
 That means, that when a user is requesting the api during the deployment period, the user could exceed the allowed amount of requests.


- Memory consumption of map per user: 
 ```
 approx. 404 bytes/userIp
 e.g. 4mio users = approx. 1616 MBs 
 calculation: ip (32 bit) + (request limit per user 100 * size of datetime (32bit))
 ```

- Memory consumption: The strategy provides its own scheduled cleanup task to reduce the memory consumption. 

- when working with multiple server instances, a load balancer could distribute requests with ip ranges to particular server instances to ensure a high scalability 
- a more flexible solution for multiple server instances would be a key value store. With a given IP the queue of requests could be queried by the server. This solution would need an
 insert policy to handle edge cases like saving data for the the same user at the same time. The key value store should be cleaned after a certain period of time to avoid to much memory consumption.

- when the rate limit is changed and the amount of allowed requests is very high per hour e.g 1000, a different strategy could be used to save memory and time 
instead of a queue a pair could be used to track the request time. The pair could save the date time when the request limit ends (the time from the first request + rateLimitMinutes) and the amount of current requests
e.g IP->{2020-08-07 08:35:16, 2}



