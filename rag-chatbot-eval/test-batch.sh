#!/bin/bash

questions=(
  "What are the fees for the premium checking account?"
  "What types of personal loans do you offer?"
  "What is the APR for auto loans?"
  "Can I get travel insurance with my account?"
)

for q in "${questions[@]}"; do
  echo "Testing: $q"
  curl -X POST http://localhost:8080/chat \
    -H "Content-Type: application/json" \
    -d "{\"question\": \"$q\"}" | jq '.metrics'
  echo "---"
  sleep 2
done