# Follower Maze Server

This is my attempt at writing a quick TCP server in Scala to handle a
concurrent stream of social media notifications. A fun project, indeed!
:)

## Build

### Two ways to run:

Run the prebuilt JAR:
```
./notification-server.sh
```

Build & run with SBT
```
sbt run \
  [--eventPort $EVENT_PORT] \
  [--clientPort $CLIENT_PORT] \
  [--sortWindow $SORT_WINDOW]
```

### Tests:
```
sbt test
```
