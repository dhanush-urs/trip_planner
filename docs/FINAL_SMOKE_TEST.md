# TripForge — Final Smoke Test

Zero-state test sequence. Run after `make reset && make up`.

## Setup

```bash
make reset
make up
# Wait 3-4 minutes
make health
```

## Step 1: Register

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke@tripforge.com","password":"Smoke@12345","firstName":"Smoke","lastName":"Test"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('OK' if d['success'] else 'FAIL:', d.get('message'))"
# Expected: OK: Registration successful
```

## Step 2: Login + Save Token

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke@tripforge.com","password":"Smoke@12345"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
echo "Token: ${TOKEN:0:20}..."
```

## Step 3: Create Trip

```bash
TRIP=$(curl -s -X POST http://localhost:8080/api/trip/create \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"destination":"Goa","startDate":"2025-09-01","endDate":"2025-09-05","totalBudget":50000,"travelers":2,"interests":["beaches","food"],"hotelPreference":"STANDARD"}')

TRIP_ID=$(echo $TRIP | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['tripId'])")
echo "Trip ID: $TRIP_ID"
echo $TRIP | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
print('providerMode:', d.get('providerMode'))
print('hotel:', d.get('selectedHotel',{}).get('name') if d.get('selectedHotel') else 'None')
print('itinerary days:', len(d.get('itinerary',[])))
print('budget total:', d.get('budgetBreakdown',{}).get('totalEstimated') if d.get('budgetBreakdown') else 'None')
print('split perPerson:', d.get('splitResult',{}).get('perPersonAmount') if d.get('splitResult') else 'None')
"
```

**Expected (no API keys):**
```
providerMode: FALLBACK
hotel: <CSV hotel name>
itinerary days: 4
budget total: ~39900.00
split perPerson: ~19950.00
```

## Step 4: Change Hotel

```bash
HOTEL_ID=$(echo $TRIP | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print(d.get('selectedHotel',{}).get('id','') if d.get('selectedHotel') else '')")

curl -s -X POST http://localhost:8080/api/hotels/change \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"tripId\":$TRIP_ID,\"currentHotelId\":$HOTEL_ID,\"reason\":\"CHEAPER\",\"destination\":\"Goa\",\"budget\":50000,\"durationDays\":4,\"travelers\":2}" \
  | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print('Alternatives:', len(d) if isinstance(d,list) else 0)"
# Expected: Alternatives: 1-4
```

## Step 5: Equal Split with Named Participants

```bash
curl -s -X POST http://localhost:8080/api/split/equal \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"tripId\":$TRIP_ID,\"totalAmount\":39900,\"currencyCode\":\"INR\",\"participants\":[{\"participantId\":1,\"participantName\":\"Smoke\"},{\"participantId\":2,\"participantName\":\"Test\"}]}" \
  | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print('mode:', d['splitMode']); [print(p['name'], p['amount']) for p in d['participants']]"
# Expected: mode: EQUAL, Smoke 19950.00, Test 19950.00
```

## Step 6: Custom Percentage Split

```bash
curl -s -X POST http://localhost:8080/api/split/custom-percentage \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"tripId\":$TRIP_ID,\"totalAmount\":39900,\"currencyCode\":\"INR\",\"participants\":[{\"participantId\":1,\"participantName\":\"Smoke\",\"percentage\":60},{\"participantId\":2,\"participantName\":\"Test\",\"percentage\":40}]}" \
  | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; [print(p['name'], p['amount'], str(p['percentage'])+'%') for p in d['participants']]"
# Expected: Smoke 23940.00 60.0%, Test 15960.00 40.0%
```

## Step 7: Custom Amount Split

```bash
curl -s -X POST http://localhost:8080/api/split/custom-amount \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"tripId\":$TRIP_ID,\"totalAmount\":39900,\"currencyCode\":\"INR\",\"participants\":[{\"participantId\":1,\"participantName\":\"Smoke\",\"amount\":25000},{\"participantId\":2,\"participantName\":\"Test\",\"amount\":14900}]}" \
  | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; [print(p['name'], p['amount']) for p in d['participants']]"
# Expected: Smoke 25000.00, Test 14900.00
```

## Step 8: Payment Summary

```bash
curl -s http://localhost:8080/api/payments/trip/$TRIP_ID \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print('status:', d.get('status'), '| total:', d.get('totalAmount'))"
# Expected: status: UNPAID | total: ~39900.00
```

## Step 9: Trip History

```bash
USER_ID=$(curl -s http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

curl -s http://localhost:8080/api/trip/user/$USER_ID \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; trips=json.load(sys.stdin)['data']; print('Trips:', len(trips)); [print(' -', t['destination'], t['status']) for t in trips]"
# Expected: Trips: 1, - Goa PLANNED
```

## Step 10: Trip Details

```bash
curl -s http://localhost:8080/api/trip/$TRIP_ID \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print('hotel:', d.get('selectedHotel',{}).get('name') if d.get('selectedHotel') else 'None')"
# Expected: hotel: <hotel name> (not None — Phase 9A fix)
```

## Degraded Mode Notes

When no API keys are configured:
- `providerMode: FALLBACK` — expected
- `aiEnriched: false` — expected
- `paymentAvailable: false` or payment summary shows `NOT_INITIALIZED` — expected
- All endpoints still return 200 with valid data
