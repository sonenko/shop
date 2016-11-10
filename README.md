## About (DO NOT USE! it is in development)
Simple Shopping Basket that able to scale in minutes :)

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
// receive user id
curl "http://localhost:7777/api/shoppingbasket" -i
// show basket state
curl "http://localhost:7777/api/shoppingbasket" --cookie "user-session-id=88d6b5b0-8593-4b26-8298-62a1aa41d4c1" -i
// add product to basket
curl -XPOST "http://localhost:7777/api/shoppingbasket" --cookie "user-session-id=88d6b5b0-8593-4b26-8298-62a1aa41d4c1" -d '
{"productId": "b41623a7-a5f4-4fb3-8e0e-b18f94b9f184", "count": 3}
' -i -H "Content-Type: application/json"
// remove product
curl -XDELETE "http://localhost:7777/api/shoppingbasket" --cookie "user-session-id=88d6b5b0-8593-4b26-8298-62a1aa41d4c1" -d '
{"productId": "b41623a7-a5f4-4fb3-8e0e-b18f94b9f184", "count": 1}
' -i -H "Content-Type: application/json"
```


#### Products endpoints
```
// List products
curl "http://localhost:7777/api/products" | python -m json.tool
```

#### Admin endpoints
```
// list baskets ids
curl "http://localhost:7777/api/admin/baskets" --user admin:pwd -i
// show basket state
curl "http://localhost:7777/api/admin/baskets/88d6b5b0-8593-4b26-8298-62a1aa41d4c1" --user admin:pwd -i
// delete basket
curl -XDELETE "http://localhost:7777/api/admin/baskets/88d6b5b0-8593-4b26-8298-62a1aa41d4c1" --user admin:pwd -i
```