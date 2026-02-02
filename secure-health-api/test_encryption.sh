#!/bin/bash

echo "1. Deleting old data..."
curl -X DELETE http://localhost:8080/api/patients/1 2>/dev/null

echo -e "\n2. Creating new patient..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1980-05-15",
    "email": "john.doe@example.com",
    "ssn": "123-45-6789",
    "medicalHistory": "Hypertension",
    "phoneNumber": "555-123-4567"
  }')

echo $RESPONSE | jq .

ID=$(echo $RESPONSE | jq -r .id)

echo -e "\n3. Searching by email..."
curl -s "http://localhost:8080/api/patients/search?email=john.doe@example.com" | jq .

echo -e "\n4. Getting full patient details..."
curl -s http://localhost:8080/api/patients/$ID | jq .

echo -e "\n5. Checking database..."
PGCONTAINER=$(podman ps --filter ancestor=postgres --format "{{.Names}}" | head -n 1)
podman exec -it $PGCONTAINER psql -U quarkus -d quarkus -c "SELECT id, firstname, substring(email, 1, 60) as email_preview FROM patients WHERE id=$ID;"

echo -e "\nDone!"