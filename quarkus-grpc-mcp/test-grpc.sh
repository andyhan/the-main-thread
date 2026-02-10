grpcurl -plaintext localhost:9000 list

grpcurl -plaintext \
  -d '{"name":"Alice"}' \
  localhost:9000 greeting.GreetingService/SayHello