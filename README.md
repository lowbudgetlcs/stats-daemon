# stats-daemon

This service is responsible for inserting participant stats into the database,
as well as updating relevant google sheets such as stats or standings.

The most important thing to know about Kotlin: It can use ANY Java library.
RabbitMQ doesn't provide a kotlin library- we instead use the Java one, and it
just...works!

For HTTP requests, I suggest Ktor Client. Ktor is the HTTP library I use in the 
api_gate, but it has bindings for making requests as well. It's very good!

ANy database related stuff is handled by SQLDelight. I LOVE SQLDELIGHT!! You write 
SQL in TABLE.sq files, located in src/main/sqldelight/queries. They look like this:
```sqldelight
selectAll:
  SELECT * FROM games;
```
Then, in your code you do this:
```kotlin
val gameQueries = db.gameQueries
val games = gameQueries.selectAll()
```
This is so powerful. All the queries are raw SQL so you get maximum customization, but 
it's all type checked with the migrations in sqldelight/migrations. Check the 
tournament_engine for more examples of this- it's easy to pick up.

There is a decent Java library for the Riot API, named R4J. It's a bit weird, but it works
and that's what matters. Make sure you put the Riot Token in .env. I am not super familiar 
with this yet, and the documentation sucks. I've been relying on Intellij intellisense for 
riot stuff.

### Running/Testing

Since this relies on a messageq, testing locally is sorta annoying. It might be a good idea to 
just assume that reading off the queue works (I'll handle it), and just test by directly invoking 
whatever functions you write. I'll connect everything together once it's ready. I've included docker 
instructions below in case you want to try it, but it isn't necessary. I also linked docs for all the
libraries I recommend. Take it slow! Gl king.

### Docker

There are two ways to run this app's container.

`docker build -t stats-daemon:latest .`
`docker run --name stats-daemon stats-daemon:latest`

or (my preferred method)

`docker-compose -f compose.local.yaml up`

### Requirements

Since this app reads messages off of a messageq, there needs to be a messageq
running. The easiest way to do this is with docker:

`docker-compose -f rabbitmq.yaml up -d`

or running the rabbitmq container directly:

`docker run -it --rm --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management`

### References

[Rabbitmq Java tutorials](https://www.rabbitmq.com/tutorials/tutorial-one-java)
[SQLDelight (type-safe SQL)](https://cashapp.github.io/sqldelight/2.0.2/)
[KTOR (http client
library)](https://ktor.io/docs/client-create-new-application.html)
[Docker on Windows](https://docs.docker.com/desktop/install/windows-install/)
[Docker Compose](https://docs.docker.com/compose/gettingstarted/)




