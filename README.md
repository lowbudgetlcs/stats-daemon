# stats-daemon

This service is responsible for inserting participant stats into the database,
as well as updating relevant google sheets such as stats or standings.

The most important thing to know about Kotlin: It can use ANY Java library.
RabbitMQ doesn't provide a kotlin library- we instead use the Java one, and it
just...works!

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




