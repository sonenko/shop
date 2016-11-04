## About (DO NOT USE! it is in development)
Simple shop that can scale in seconds

#### Run application in dev mode
simply run Main cass from IDE, or execute next commands from terminal
```
sbt
run
```

#### Run app in prod mode
```
cd  run
./make.sh
./start.sh
```

#### Few curl endpoint
```
// List baskets
curl "http://localhost:7777/api/shoppingbasket" -i
```