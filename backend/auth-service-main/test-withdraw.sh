#!/bin/bash

# Test script for withdraw connection request endpoint

TOKEN="eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6IlVTRVIiLCJpcEFkZHJlc3MiOiIxMDQuMjMuMTc1LjY5IiwidXNlckFnZW50IjoiTW96aWxsYS81LjAgKFdpbmRvd3MgTlQgMTAuMDsgV2luNjQ7IHg2NCkgQXBwbGVXZWJLaXQvNTM3LjM2IChLSFRNTCwgbGlrZSBHZWNrbykgQ2hyb21lLzE0My4wLjAuMCBTYWZhcmkvNTM3LjM2Iiwic2Vzc2lvbklkIjoiYzI0ODIyOTItYWUzMC00ZGEyLTg5ZDUtMjhlMDVmYmJlZjc1IiwicmVtZW1iZXJNZSI6dHJ1ZSwidHlwZSI6ImFjY2VzcyIsInVzZXJJZCI6IjAxNDA4MGJhLWY4NzAtNGY2Ny1hMGRiLTAwOGIwMzA4NzNmNSIsImRldmljZUlkIjoiNGUzZGI3YjUtYmZlMC0zNzQ4LWJlNWYtZmJiZmRkYjc0ZDJiIiwic3ViIjoicHVoZW1vc29AZGVuaXBsLm5ldCIsImlhdCI6MTc2NTg2ODk1OCwiZXhwIjoxNzY4NDYwOTU4LCJpc3MiOiJhdXRoLXNlcnZpY2UiLCJqdGkiOiJhYWNlZmNhNy0wN2NhLTQxZjEtYjk4OS0zZTQxNjgzN2ZhMzcifQ.9ajtaiwwbYyOoSS5ELY_1uJu3oParAf4cz-TgASMuQFbd3z2DaXAVEBZ0bWePLhUQPhMvrqmiINxdgI1ZRXwlw"
BASE_URL="https://api.mapmytimes.com"

echo "=== Testing Withdraw Connection Request ==="
echo ""

# Step 1: Get outgoing requests
echo "1. Getting outgoing connection requests..."
REQUESTS=$(curl -s "$BASE_URL/api/v1/user/connections/requests/outgoing" \
  -H "Authorization: Bearer $TOKEN")

echo "$REQUESTS" | jq . 2>/dev/null || echo "$REQUESTS"
echo ""

# Step 2: Extract first request ID
REQUEST_ID=$(echo "$REQUESTS" | jq -r '.data[0].requestId // empty' 2>/dev/null)

if [ -z "$REQUEST_ID" ] || [ "$REQUEST_ID" = "null" ]; then
  echo "❌ No pending requests found to withdraw"
  echo ""
  echo "To test withdrawal:"
  echo "1. First send a connection request"
  echo "2. Then use the requestId to withdraw it"
  echo ""
  echo "Example withdraw command:"
  echo "curl --location '$BASE_URL/api/v1/user/connections/withdraw?requestId=YOUR_REQUEST_ID' \\"
  echo "  --header 'Authorization: Bearer YOUR_TOKEN'"
  exit 0
fi

echo "2. Found request ID: $REQUEST_ID"
echo ""

# Step 3: Withdraw
echo "3. Withdrawing connection request..."
RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/user/connections/withdraw?requestId=$REQUEST_ID" \
  -H "Authorization: Bearer $TOKEN")

echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
echo ""

# Check if successful
SUCCESS=$(echo "$RESPONSE" | jq -r '.success // false' 2>/dev/null)
if [ "$SUCCESS" = "true" ]; then
  echo "✅ Withdrawal successful!"
else
  echo "❌ Withdrawal failed"
fi
