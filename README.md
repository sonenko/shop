## About (DO NOT USE! it is in development)
Simple shop that can scale in seconds

#### Run application in dev mode
simply run Main cass from IDE, or execute next commands from terminal
```
sbt
run
```

#### Run app in prod mode (change `local.conf` in `run` folder)
```
cd run
./make.sh
./run.sh
```

#### Run tests
```
sbt
test
```

## curl endpoints examples
#### Basket rest endpoints
```
// List baskets
curl "http://localhost:7777/api/shoppingbasket" -i
```

#### Products endpoints
```
// List products
curl "http://localhost:7777/api/products" | python -m json.tool
```